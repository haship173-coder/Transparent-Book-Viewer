package transparent.controller;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import transparent.model.Content;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog used to capture or edit metadata for a piece of content.
 */
public class ContentMetadataDialog extends Dialog<Content> {
    private final TextField titleField = new TextField();
    private final TextField authorField = new TextField();
    private final TextField categoryField = new TextField();
    private final TextField tagsField = new TextField();
    private final TextArea descriptionArea = new TextArea();
    private final TextField typeField = new TextField();

    public ContentMetadataDialog(Content content, String heading) {
        setTitle("Content metadata");
        setHeaderText(heading);
        DialogPane pane = getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        titleField.setText(content.getTitle());
        authorField.setText(valueOrEmpty(content.getAuthor()));
        categoryField.setText(valueOrEmpty(content.getCategory()));
        tagsField.setText(String.join(", ", content.getTags()));
        descriptionArea.setText(valueOrEmpty(content.getDescription()));
        descriptionArea.setPrefRowCount(4);
        typeField.setText(valueOrEmpty(content.getFileType()));
        typeField.setEditable(false);

        Label pathValue = new Label(valueOrEmpty(content.getFilePath()));
        pathValue.setWrapText(true);

        int row = 0;
        grid.addRow(row++, new Label("Title"), titleField);
        grid.addRow(row++, new Label("Author"), authorField);
        grid.addRow(row++, new Label("Category"), categoryField);
        grid.addRow(row++, new Label("Tags"), tagsField);
        grid.addRow(row++, new Label("File type"), typeField);
        grid.addRow(row++, new Label("File path"), pathValue);
        grid.addRow(row, new Label("Description"), descriptionArea);
        GridPane.setHgrow(titleField, Priority.ALWAYS);
        GridPane.setHgrow(authorField, Priority.ALWAYS);
        GridPane.setHgrow(categoryField, Priority.ALWAYS);
        GridPane.setHgrow(tagsField, Priority.ALWAYS);
        GridPane.setHgrow(descriptionArea, Priority.ALWAYS);
        GridPane.setVgrow(descriptionArea, Priority.ALWAYS);

        pane.setContent(grid);

        Node okButton = pane.lookupButton(ButtonType.OK);
        okButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> titleField.getText().trim().isEmpty(),
                titleField.textProperty()));

        setResultConverter(button -> {
            if (button.getButtonData() == ButtonData.OK_DONE) {
                Content updated = new Content(content);
                updated.setTitle(titleField.getText().trim());
                updated.setAuthor(valueOrNull(authorField.getText()));
                updated.setCategory(valueOrNull(categoryField.getText()));
                updated.setTags(parseTags(tagsField.getText()));
                updated.setDescription(valueOrNull(descriptionArea.getText()));
                return updated;
            }
            return null;
        });
    }

    private List<String> parseTags(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] parts = text.split(",");
        List<String> tags = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                tags.add(trimmed);
            }
        }
        return tags;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String valueOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
