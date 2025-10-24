package transparent.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import transparent.model.Content;
import transparent.service.FavouriteService;
import transparent.service.HistoryService;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Controller for the reader view.  Displays images and text files directly
 * inside the application and falls back to launching the system viewer for
 * unsupported formats such as PDF.  Reading progress and favourites are saved
 * when the window closes.
 */
public class ReaderController {
    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private Label infoLabel;
    @FXML private Button closeButton;
    @FXML private Button favouriteButton;
    @FXML private Button openExternalButton;
    @FXML private ScrollPane contentScroll;
    @FXML private StackPane contentContainer;
    @FXML private ImageView imageView;
    @FXML private TextArea textArea;

    private final HistoryService historyService = new HistoryService();
    private final FavouriteService favouriteService = new FavouriteService();
    private Content content;
    private int currentPage = 1;

    @FXML
    private void initialize() {
        textArea.setWrapText(true);
        openExternalButton.setDisable(true);
        openExternalButton.setOnAction(e -> openExternally());
        closeButton.setOnAction(e -> handleClose());
        favouriteButton.setOnAction(e -> toggleFavourite());
    }

    public void setContent(Content content) {
        this.content = content;
        titleLabel.setText(content.getTitle());
        statusLabel.setText(buildStatusLine());
        loadContent();
        updateFavouriteButton();
    }

    private void loadContent() {
        if (content == null) {
            return;
        }
        Path path = Path.of(content.getFilePath());
        if (!Files.exists(path)) {
            showMessage("Không tìm thấy tệp: " + content.getFilePath());
            return;
        }
        String type = content.getFileType() == null ? "" : content.getFileType().toLowerCase(Locale.ROOT);
        switch (type) {
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
                showImage(path);
                break;
            case "txt":
            case "md":
                showTextFile(path);
                break;
            case "epub":
                showEpub(path);
                break;
            case "pdf":
                showUnsupported("PDF không thể hiển thị trực tiếp. Chọn \"Mở bằng ứng dụng ngoài\" để xem.");
                break;
            default:
                showUnsupported("Định dạng " + type + " chưa được hỗ trợ. Vui lòng mở bằng ứng dụng ngoài.");
        }
    }

    private void showImage(Path path) {
        imageView.setImage(new Image(path.toUri().toString(), true));
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(760);
        imageView.setFitHeight(520);
        imageView.setVisible(true);
        textArea.setVisible(false);
        infoLabel.setText("");
        openExternalButton.setDisable(true);
    }

    private void showTextFile(Path path) {
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            textArea.setText(text);
            textArea.setVisible(true);
            imageView.setVisible(false);
            infoLabel.setText("");
            openExternalButton.setDisable(true);
        } catch (IOException e) {
            showMessage("Không thể đọc tệp văn bản: " + e.getMessage());
        }
    }

    private void showEpub(Path path) {
        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            ZipEntry entry = zipFile.stream()
                    .filter(e -> e.getName().toLowerCase(Locale.ROOT).endsWith(".xhtml")
                            || e.getName().toLowerCase(Locale.ROOT).endsWith(".html"))
                    .findFirst()
                    .orElse(null);
            if (entry == null) {
                showUnsupported("Không tìm thấy nội dung EPUB hợp lệ.");
                return;
            }
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    zipFile.getInputStream(entry), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
            }
            String raw = builder.toString();
            String text = raw.replaceAll("<[^>]+>", " ")
                    .replaceAll("&nbsp;", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            textArea.setText(text);
            textArea.setVisible(true);
            imageView.setVisible(false);
            infoLabel.setText("(Đang hiển thị chương đầu tiên trong EPUB)");
            openExternalButton.setDisable(true);
        } catch (IOException e) {
            showMessage("Không thể đọc EPUB: " + e.getMessage());
        }
    }

    private void showUnsupported(String message) {
        imageView.setVisible(false);
        textArea.setVisible(false);
        infoLabel.setText(message);
        openExternalButton.setDisable(!Desktop.isDesktopSupported());
    }

    private void showMessage(String message) {
        imageView.setVisible(false);
        textArea.setVisible(false);
        infoLabel.setText(message);
        openExternalButton.setDisable(true);
    }

    private void handleClose() {
        saveProgress();
        closeButton.getScene().getWindow().hide();
    }

    private void saveProgress() {
        if (CurrentUser.get() == null || content == null) {
            return;
        }
        historyService.saveProgress(CurrentUser.get().getUserID(), content.getContentID(), currentPage);
    }

    private void toggleFavourite() {
        if (CurrentUser.get() == null || content == null) {
            return;
        }
        boolean nowFavourite = favouriteService.toggleFavourite(CurrentUser.get().getUserID(), content.getContentID());
        favouriteButton.setText(nowFavourite ? "Bỏ yêu thích" : "Thêm yêu thích");
    }

    private void updateFavouriteButton() {
        if (CurrentUser.get() == null || content == null) {
            favouriteButton.setDisable(true);
            return;
        }
        favouriteButton.setDisable(false);
        boolean favourite = favouriteService.isFavourite(CurrentUser.get().getUserID(), content.getContentID());
        favouriteButton.setText(favourite ? "Bỏ yêu thích" : "Thêm yêu thích");
    }

    private void openExternally() {
        if (content == null || !Desktop.isDesktopSupported()) {
            return;
        }
        try {
            Desktop.getDesktop().open(Path.of(content.getFilePath()).toFile());
        } catch (IOException e) {
            showMessage("Không thể mở tệp: " + e.getMessage());
        }
    }

    private String buildStatusLine() {
        if (content == null) {
            return "";
        }
        String type = content.getFileType() == null ? "" : content.getFileType();
        String size = formatSize(content.getSizeBytes());
        String added = content.getDayAdded() == null
                ? ""
                : DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(content.getDayAdded());
        return String.format("%s • %s • Thêm: %s", type, size, added);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.ROOT, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
