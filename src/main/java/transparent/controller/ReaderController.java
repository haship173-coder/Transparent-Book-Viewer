package transparent.controller;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import transparent.model.Content;
import transparent.model.HistoryRecord;
import transparent.service.FavouriteService;
import transparent.service.HistoryService;
import transparent.ui.ThemeManager;
import transparent.ui.ThemeManager.Theme;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controller for the reader view.  Responsible for rendering the selected
 * content (EPUB, PDF, images, plain text) and persisting reading progress.
 */
public class ReaderController {
    private enum ReaderMode { TEXT, EPUB, PDF, IMAGE, UNSUPPORTED }

    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button closeButton;
    @FXML private Button favouriteButton;
    @FXML private Button openExternalButton;
    @FXML private Label pageLabel;
    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private ScrollPane contentScroll;
    @FXML private StackPane contentHolder;

    private final HistoryService historyService = new HistoryService();
    private final FavouriteService favouriteService = new FavouriteService();
    private final ThemeManager themeManager = ThemeManager.getInstance();
    private final WebView webView = new WebView();
    private final ImageView imageView = new ImageView();

    private Content content;
    private ReaderMode mode = ReaderMode.TEXT;
    private int currentPage = 0;
    private int totalPages = 1;
    private List<String> epubPages = new ArrayList<>();
    private List<Image> pdfPages = new ArrayList<>();

    @FXML
    private void initialize() {
        prevButton.setOnAction(e -> showPreviousPage());
        nextButton.setOnAction(e -> showNextPage());
        closeButton.setOnAction(e -> handleClose());
        favouriteButton.setOnAction(e -> toggleFavourite());
        openExternalButton.setOnAction(e -> openExternally());
        openExternalButton.setDisable(true);
        configureWebView();
        imageView.setPreserveRatio(true);
        contentScroll.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            if (mode == ReaderMode.IMAGE) {
                imageView.setFitWidth(Math.max(200, newBounds.getWidth() - 40));
            } else if (mode == ReaderMode.TEXT || mode == ReaderMode.EPUB || mode == ReaderMode.UNSUPPORTED) {
                webView.setPrefWidth(Math.max(200, newBounds.getWidth() - 40));
            }
        });
        themeManager.addListener(this::applyTheme);
    }

    private void configureWebView() {
        webView.setContextMenuEnabled(false);
        webView.getEngine().setUserStyleSheetLocation(getClass().getResource("/webview-reader.css") != null
                ? getClass().getResource("/webview-reader.css").toExternalForm()
                : null);
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                applyWebTheme(themeManager.getActiveTheme());
            }
        });
    }

    /**
     * Populate the reader with the selected content.
     */
    public void setContent(Content content) {
        this.content = content;
        titleLabel.setText(content.getTitle());
        statusLabel.setText("");
        loadContent();
        restoreProgress();
        updateFavouriteState();
        updateExternalButton();
    }

    private void loadContent() {
        if (content == null) {
            return;
        }
        Path path = Path.of(content.getFilePath());
        if (!Files.exists(path)) {
            showError("File missing", "Cannot find file: " + path);
            mode = ReaderMode.UNSUPPORTED;
            return;
        }
        String type = content.getFileType() == null ? "" : content.getFileType().toUpperCase(Locale.ROOT);
        try {
            boolean handled = true;
            switch (type) {
                case "EPUB" -> loadEpub(path);
                case "PDF" -> loadPdf(path);
                case "PNG", "JPG", "JPEG", "GIF", "BMP" -> loadImage(path);
                case "TXT", "TEXT" -> loadText(path);
                default -> handled = false;
            }
            if (!handled) {
                showUnsupported(path);
            }
        } catch (IOException ex) {
            showError("Unable to open file", ex.getMessage());
            showUnsupported(path);
        }
    }

    private void loadText(Path path) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        mode = ReaderMode.TEXT;
        totalPages = 1;
        currentPage = 0;
        String theme = themeManager.getActiveTheme() == Theme.DARK ? "dark" : "light";
        String html = "<html><head><style>body{font-family:'Segoe UI',sans-serif;font-size:16px;line-height:1.6;padding:24px;}" +
                "pre{white-space:pre-wrap;word-wrap:break-word;}</style></head><body data-theme='" + theme + "'><pre>" +
                escapeHtml(text) + "</pre></body></html>";
        webView.getEngine().loadContent(html);
        webView.setPrefWidth(Math.max(200, getViewportWidth() - 40));
        setContentNode(webView);
        updateNavigationState();
    }

    private void loadEpub(Path path) throws IOException {
        epubPages.clear();
        try (InputStream inputStream = Files.newInputStream(path)) {
            Book book = new EpubReader().readEpub(inputStream);
            if (book.getTitle() != null && !book.getTitle().isBlank()) {
                titleLabel.setText(book.getTitle());
            }
            for (Resource res : book.getSpine().getSpineResources()) {
                byte[] data = res.getData();
                String html = new String(data, res.getInputEncoding() != null ? res.getInputEncoding() : StandardCharsets.UTF_8);
                epubPages.add(html);
            }
        }
        if (epubPages.isEmpty()) {
            statusLabel.setText("EPUB has no readable chapters");
            epubPages.add("<p>No readable content</p>");
        }
        mode = ReaderMode.EPUB;
        totalPages = epubPages.size();
        currentPage = Math.min(currentPage, totalPages - 1);
        displayCurrentEpubPage();
    }

    private void loadPdf(Path path) throws IOException {
        pdfPages.clear();
        try (PDDocument document = PDDocument.load(path.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage bufferedImage = renderer.renderImageWithDPI(i, 150);
                Image image = SwingFXUtils.toFXImage(bufferedImage, null);
                pdfPages.add(image);
            }
        }
        if (pdfPages.isEmpty()) {
            statusLabel.setText("PDF contains no pages");
            showUnsupported(path);
            return;
        }
        mode = ReaderMode.PDF;
        totalPages = pdfPages.size();
        currentPage = Math.min(currentPage, totalPages - 1);
        displayCurrentPdfPage();
    }

    private void loadImage(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            if (bufferedImage == null) {
                throw new IOException("Unsupported image format");
            }
            Image image = SwingFXUtils.toFXImage(bufferedImage, null);
            imageView.setImage(image);
            imageView.setFitWidth(Math.max(200, getViewportWidth() - 40));
            mode = ReaderMode.IMAGE;
            totalPages = 1;
            currentPage = 0;
            setContentNode(imageView);
            updateNavigationState();
        }
    }

    private void displayCurrentEpubPage() {
        if (epubPages.isEmpty()) {
            return;
        }
        String html = epubPages.get(currentPage);
        webView.getEngine().loadContent(html);
        webView.setPrefWidth(Math.max(200, getViewportWidth() - 40));
        setContentNode(webView);
        updateNavigationState();
        applyWebTheme(themeManager.getActiveTheme());
    }

    private void displayCurrentPdfPage() {
        if (pdfPages.isEmpty()) {
            return;
        }
        imageView.setImage(pdfPages.get(currentPage));
        imageView.setFitWidth(Math.max(200, getViewportWidth() - 40));
        setContentNode(imageView);
        updateNavigationState();
    }

    private void setContentNode(Node node) {
        contentHolder.getChildren().setAll(node);
    }

    private void showPreviousPage() {
        if (currentPage <= 0) {
            return;
        }
        currentPage--;
        displayCurrentPage();
    }

    private void showNextPage() {
        if (currentPage >= totalPages - 1) {
            return;
        }
        currentPage++;
        displayCurrentPage();
    }

    private void displayCurrentPage() {
        switch (mode) {
            case EPUB -> displayCurrentEpubPage();
            case PDF -> displayCurrentPdfPage();
            case TEXT, UNSUPPORTED -> {
                setContentNode(webView);
                applyWebTheme(themeManager.getActiveTheme());
            }
            case IMAGE -> setContentNode(imageView);
        }
        updateNavigationState();
        persistProgress();
    }

    private void updateNavigationState() {
        pageLabel.setText("Page " + (currentPage + 1) + " / " + totalPages);
        boolean multiPage = totalPages > 1;
        prevButton.setDisable(!multiPage || currentPage <= 0);
        nextButton.setDisable(!multiPage || currentPage >= totalPages - 1);
    }

    private void restoreProgress() {
        if (CurrentUser.get() == null || content == null) {
            displayCurrentPage();
            return;
        }
        HistoryRecord record = historyService.getLatestEntry(CurrentUser.get().getUserID(), content.getContentID());
        if (record == null) {
            displayCurrentPage();
            return;
        }
        int page = Math.max(1, record.getPageNumber());
        currentPage = Math.min(page - 1, Math.max(totalPages - 1, 0));
        displayCurrentPage();
        statusLabel.setText("Resumed from page " + page);
    }

    private void persistProgress() {
        if (CurrentUser.get() == null || content == null) {
            return;
        }
        historyService.saveProgress(CurrentUser.get().getUserID(), content.getContentID(), currentPage + 1);
    }

    private void updateFavouriteState() {
        if (CurrentUser.get() == null || content == null) {
            favouriteButton.setDisable(true);
            return;
        }
        favouriteButton.setDisable(false);
        boolean favourite = favouriteService.isFavourite(CurrentUser.get().getUserID(), content.getContentID());
        favouriteButton.setText(favourite ? "Remove favourite" : "Add to favourites");
    }

    private void toggleFavourite() {
        if (CurrentUser.get() == null || content == null) {
            showError("Not logged in", "Please login to manage favourites.");
            return;
        }
        boolean favourite = favouriteService.toggleFavourite(CurrentUser.get().getUserID(), content.getContentID());
        favouriteButton.setText(favourite ? "Remove favourite" : "Add to favourites");
        statusLabel.setText(favourite ? "Added to favourites" : "Removed from favourites");
    }

    private double getViewportWidth() {
        double width = contentScroll.getViewportBounds().getWidth();
        if (width <= 0) {
            width = contentScroll.getWidth();
        }
        return width > 0 ? width : 800;
    }

    private void handleClose() {
        persistProgress();
        closeButton.getScene().getWindow().hide();
    }

    private void openExternally() {
        if (content == null) {
            return;
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                showError("Unavailable", "Opening files externally is not supported on this platform.");
                return;
            }
            Desktop.getDesktop().open(new File(content.getFilePath()));
            if (CurrentUser.get() != null) {
                historyService.saveProgress(CurrentUser.get().getUserID(), content.getContentID(), currentPage + 1);
            }
        } catch (IOException ex) {
            showError("Unable to open externally", ex.getMessage());
        }
    }

    private void updateExternalButton() {
        boolean supported = Desktop.isDesktopSupported()
                && content != null
                && Files.exists(Path.of(content.getFilePath()));
        openExternalButton.setDisable(!supported);
    }

    private void showUnsupported(Path path) {
        mode = ReaderMode.UNSUPPORTED;
        totalPages = 1;
        currentPage = 0;
        String theme = themeManager.getActiveTheme() == Theme.DARK ? "dark" : "light";
        String message = "<html><body data-theme='" + theme + "'><div style='font-family:sans-serif;font-size:16px;padding:24px;'>"
                + "<p><strong>Unsupported file type.</strong></p>"
                + "<p>Use the <em>Open externally</em> button to launch the file in a native application.</p>"
                + "</div></body></html>";
        webView.getEngine().loadContent(message);
        setContentNode(webView);
        updateNavigationState();
        statusLabel.setText("Unsupported format: " + path.getFileName());
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Transparent");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void applyTheme(Theme theme) {
        applyWebTheme(theme);
        Platform.runLater(this::updateExternalButton);
    }

    private void applyWebTheme(Theme theme) {
        Platform.runLater(() -> {
            try {
                webView.getEngine().executeScript(
                        "if (document && document.body){document.body.setAttribute('data-theme','" +
                                theme.name().toLowerCase(Locale.ROOT) + "');}");
            } catch (Exception ignored) {
                // WebView may not be ready yet
            }
        });
    }

    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
