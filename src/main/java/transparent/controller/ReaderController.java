package transparent.controller;

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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import transparent.model.Content;
import transparent.model.HistoryRecord;
import transparent.service.FavouriteService;
import transparent.service.HistoryService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;

/**
 * Controller for the reader view.  Responsible for rendering the selected
 * content (EPUB, PDF, images, plain text) and persisting reading progress.
 */
public class ReaderController {
    private enum ReaderMode { TEXT, EPUB, PDF, IMAGE }

    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button closeButton;
    @FXML private Button favouriteButton;
    @FXML private Label pageLabel;
    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private ScrollPane contentScroll;
    @FXML private StackPane contentHolder;

    private final HistoryService historyService = new HistoryService();
    private final FavouriteService favouriteService = new FavouriteService();
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
        configureWebView();
        imageView.setPreserveRatio(true);
        contentScroll.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            if (mode == ReaderMode.IMAGE) {
                imageView.setFitWidth(Math.max(200, newBounds.getWidth() - 40));
            } else if (mode == ReaderMode.TEXT || mode == ReaderMode.EPUB) {
                webView.setPrefWidth(Math.max(200, newBounds.getWidth() - 40));
            }
        });
    }

    private void configureWebView() {
        webView.setContextMenuEnabled(false);
        webView.getEngine().setUserStyleSheetLocation(getClass().getResource("/webview-reader.css") != null
                ? getClass().getResource("/webview-reader.css").toExternalForm()
                : null);
    }

    /**
     * Populate the reader with the selected content.
     *
     * @param content the content to display
     */
    public void setContent(Content content) {
        this.content = content;
        titleLabel.setText(content.getTitle());
        statusLabel.setText("");
        loadContent();
        restoreProgress();
        updateFavouriteState();
    }

    private void loadContent() {
        if (content == null) {
            return;
        }
        Path path = Path.of(content.getFilePath());
        if (!Files.exists(path)) {
            showError("File missing", "Cannot find file: " + path);
            return;
        }
        String type = content.getFileType() == null ? "" : content.getFileType().toUpperCase();
        try {
            switch (type) {
                case "EPUB" -> loadEpub(path);
                case "PDF" -> loadPdf(path);
                case "PNG", "JPG", "JPEG", "GIF", "BMP" -> loadImage(path);
                case "TXT", "TEXT" -> loadText(path);
                default -> {
                    loadText(path);
                    statusLabel.setText("Unknown file type, attempting plain text rendering");
                }
            }
        } catch (IOException ex) {
            showError("Unable to open file", ex.getMessage());
        }
    }

    private void loadText(Path path) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        mode = ReaderMode.TEXT;
        totalPages = 1;
        currentPage = 0;
        String html = "<html><head><style>body{font-family:'Segoe UI',sans-serif;font-size:16px;line-height:1.6;padding:24px;}" +
                "pre{white-space:pre-wrap;word-wrap:break-word;}</style></head><body><pre>" +
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
            case TEXT -> setContentNode(webView);
            case IMAGE -> setContentNode(imageView);
        }
        updateNavigationState();
        persistProgress();
    }

    private void updateNavigationState() {
        pageLabel.setText("Page " + (currentPage + 1) + " / " + totalPages);
        prevButton.setDisable(currentPage <= 0);
        nextButton.setDisable(currentPage >= totalPages - 1);
    }

    private void restoreProgress() {
        if (CurrentUser.get() == null) {
            return;
        }
        HistoryRecord record = historyService.getLatestEntry(CurrentUser.get().getUserID(), content.getContentID());
        if (record == null) {
            displayCurrentPage();
            return;
        }
        int page = Math.max(1, record.getPageNumber());
        currentPage = Math.min(page - 1, totalPages - 1);
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
        favouriteService.toggleFavourite(CurrentUser.get().getUserID(), content.getContentID());
        boolean favourite = favouriteService.isFavourite(CurrentUser.get().getUserID(), content.getContentID());
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

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Transparent");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
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
