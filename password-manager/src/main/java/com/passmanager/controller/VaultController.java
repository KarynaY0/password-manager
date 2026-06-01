package com.passmanager.controller;

import com.passmanager.MainApp;
import com.passmanager.Session;
import com.passmanager.model.PasswordEntry;
import com.passmanager.service.VaultService;
import com.passmanager.util.PasswordGenerator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public class VaultController {

    @FXML private Label     lblUsername;
    @FXML private Label     lblCount;
    @FXML private TextField searchField;

    @FXML private TableView<PasswordEntry>           entriesTable;
    @FXML private TableColumn<PasswordEntry, String> colTitle;
    @FXML private TableColumn<PasswordEntry, String> colUrl;
    @FXML private TableColumn<PasswordEntry, String> colNotes;
    @FXML private TableColumn<PasswordEntry, String> colActions;

    private VaultService vault;
    private ObservableList<PasswordEntry> displayedEntries = FXCollections.observableArrayList();

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        vault = Session.get().getVaultService();
        lblUsername.setText("👤 " + Session.get().getUsername());

        setupTable();
        refreshTable("");

        // Save & encrypt vault on window close
        MainApp.getPrimaryStage().setOnCloseRequest(e -> {
            try {
                vault.saveVault();
                com.passmanager.service.FileService.encryptVaultFromDisk(
                        Session.get().getUsername(), Session.get().getMasterPassword());
            } catch (Exception ex) { ex.printStackTrace(); }
        });
    }

    private void setupTable() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colUrl.setCellValueFactory(new PropertyValueFactory<>("url"));
        colNotes.setCellValueFactory(cell ->
                new SimpleStringProperty(truncate(cell.getValue().getNotes(), 40)));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnReveal = new Button("👁 Reveal");
            private final Button btnEdit   = new Button("✏ Edit");
            private final Button btnDelete = new Button("🗑");
            private final HBox   box       = new HBox(6, btnReveal, btnEdit, btnDelete);

            {
                btnReveal.setOnAction(e -> revealPassword(getTableRow().getItem()));
                btnEdit.setOnAction(e   -> showEditDialog(getTableRow().getItem()));
                btnDelete.setOnAction(e -> deleteEntry(getTableRow().getItem()));

                btnReveal.getStyleClass().add("action-btn");
                btnEdit.getStyleClass().add("action-btn");
                btnDelete.getStyleClass().addAll("action-btn", "btn-danger-small");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getTableRow().getItem() == null ? null : box);
            }
        });

        entriesTable.setItems(displayedEntries);
    }

    private void refreshTable(String query) {
        List<PasswordEntry> results = vault.search(query);
        displayedEntries.setAll(results);
        lblCount.setText(results.size() + " entr" + (results.size() == 1 ? "y" : "ies"));
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    @FXML public void handleSearch() { refreshTable(searchField.getText()); }
    @FXML public void focusSearch()  { searchField.requestFocus(); }

    @FXML public void onRowDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2 && !entriesTable.getSelectionModel().isEmpty()) {
            revealPassword(entriesTable.getSelectionModel().getSelectedItem());
        }
    }

    // ── Add dialog ────────────────────────────────────────────────────────────

    @FXML public void showAddDialog() {
        Dialog<ButtonType> dlg = buildDialog("Add New Password Entry");
        GridPane grid = buildEntryForm(null);
        dlg.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dlg.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            TextField tfTitle = (TextField) grid.lookup("#title");
            TextField tfPass  = (TextField) grid.lookup("#password");
            TextField tfUrl   = (TextField) grid.lookup("#url");
            TextArea  taNote  = (TextArea)  grid.lookup("#notes");

            try {
                vault.addEntry(tfTitle.getText(), tfPass.getText(),
                        tfUrl.getText(), taNote.getText());
                vault.saveVault();
                refreshTable(searchField.getText());
                showInfo("Entry '" + tfTitle.getText() + "' added successfully.");
            } catch (Exception ex) {
                showError("Could not add entry: " + ex.getMessage());
            }
        }
    }

    // ── Edit dialog ───────────────────────────────────────────────────────────

    private void showEditDialog(PasswordEntry entry) {
        if (entry == null) return;
        Dialog<ButtonType> dlg = buildDialog("Edit Entry: " + entry.getTitle());
        GridPane grid = buildEntryForm(entry);
        dlg.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dlg.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            TextField tfPass = (TextField) grid.lookup("#password");
            TextField tfUrl  = (TextField) grid.lookup("#url");
            TextArea  taNote = (TextArea)  grid.lookup("#notes");

            try {
                vault.updateEntry(entry.getTitle(),
                        tfPass.getText().isBlank() ? null : tfPass.getText(),
                        tfUrl.getText(),
                        taNote.getText());
                vault.saveVault();
                refreshTable(searchField.getText());
                showInfo("Entry updated successfully.");
            } catch (Exception ex) {
                showError("Could not update entry: " + ex.getMessage());
            }
        }
    }

    // ── Reveal dialog ─────────────────────────────────────────────────────────

    private void revealPassword(PasswordEntry entry) {
        if (entry == null) return;
        try {
            String plain = vault.revealPassword(entry.getTitle());

            Dialog<ButtonType> dlg = new Dialog<>();
            dlg.setTitle("Revealed Password");
            dlg.setHeaderText("🔓 Password for: " + entry.getTitle());

            TextField tfPass = new TextField(plain);
            tfPass.setEditable(false);
            tfPass.setStyle("-fx-font-family: monospace; -fx-font-size: 14;");

            Button btnCopy = new Button("📋 Copy to Clipboard");
            btnCopy.setOnAction(e -> {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(plain);
                Clipboard.getSystemClipboard().setContent(cc);
                btnCopy.setText("✅ Copied!");
            });

            VBox content = new VBox(10,
                    new Label("Password:"), tfPass,
                    new Label("URL: " + entry.getUrl()),
                    new Label("Notes: " + entry.getNotes()),
                    btnCopy);
            content.setPadding(new Insets(16));

            dlg.getDialogPane().setContent(content);
            dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dlg.showAndWait();
        } catch (Exception ex) {
            showError("Could not reveal password: " + ex.getMessage());
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    private void deleteEntry(PasswordEntry entry) {
        if (entry == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete \"" + entry.getTitle() + "\"?");
        confirm.setContentText("This action cannot be undone.");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            vault.deleteEntry(entry.getTitle());
            try { vault.saveVault(); } catch (Exception ex) { ex.printStackTrace(); }
            refreshTable(searchField.getText());
        }
    }

    // ── Generator dialog ──────────────────────────────────────────────────────

    @FXML public void showGeneratorDialog() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Random Password Generator");

        Spinner<Integer> lengthSpinner = new Spinner<>(8, 64, 20);
        lengthSpinner.setEditable(true);

        TextField tfGenerated = new TextField();
        tfGenerated.setEditable(false);
        tfGenerated.setStyle("-fx-font-family: monospace; -fx-font-size: 13;");

        Button btnGen = new Button("🎲 Generate");
        Button btnCopy = new Button("📋 Copy");

        btnGen.setOnAction(e -> {
            String pw = PasswordGenerator.generate(lengthSpinner.getValue());
            tfGenerated.setText(pw);
        });

        btnCopy.setOnAction(e -> {
            if (!tfGenerated.getText().isEmpty()) {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(tfGenerated.getText());
                Clipboard.getSystemClipboard().setContent(cc);
                btnCopy.setText("✅ Copied!");
            }
        });

        // Generate immediately
        tfGenerated.setText(PasswordGenerator.generate(20));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.add(new Label("Length:"),    0, 0); grid.add(lengthSpinner, 1, 0);
        grid.add(btnGen,                  0, 1, 2, 1);
        grid.add(new Label("Generated:"), 0, 2); grid.add(tfGenerated, 1, 2);
        grid.add(btnCopy,                 1, 3);

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        dlg.showAndWait();
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @FXML public void handleLogout() {
        try {
            vault.saveVault(); // save in-memory entries to encrypted file
            com.passmanager.service.FileService.encryptVaultFromDisk(
                    Session.get().getUsername(), Session.get().getMasterPassword());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Session.get().clear();
        try { MainApp.loadScene("login"); } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────

    private Dialog<ButtonType> buildDialog(String title) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(title);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Stage) dlg.getDialogPane().getScene().getWindow()).setAlwaysOnTop(true);
        return dlg;
    }

    private GridPane buildEntryForm(PasswordEntry existing) {
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(16));

        TextField tfTitle    = new TextField(existing != null ? existing.getTitle()   : "");
        TextField tfPassword = new TextField();
        TextField tfUrl      = new TextField(existing != null ? existing.getUrl()     : "");
        TextArea  taNote     = new TextArea (existing != null ? existing.getNotes()   : "");
        taNote.setPrefRowCount(3);

        // IDs for lookup
        tfTitle.setId("title");
        tfPassword.setId("password");
        tfUrl.setId("url");
        taNote.setId("notes");

        if (existing != null) {
            tfTitle.setEditable(false); // title is the key, don't allow renaming
            tfPassword.setPromptText("Leave blank to keep current password");
        }

        // Mini generator button next to password field
        Button btnGen = new Button("🎲");
        btnGen.setTooltip(new Tooltip("Generate a random password"));
        btnGen.setOnAction(e -> tfPassword.setText(PasswordGenerator.generate(20)));

        HBox passRow = new HBox(6, tfPassword, btnGen);
        HBox.setHgrow(tfPassword, Priority.ALWAYS);

        int row = 0;
        grid.add(new Label("Title *"),    0, row); grid.add(tfTitle,  1, row++);
        grid.add(new Label("Password" + (existing == null ? " *" : "")), 0, row);
        grid.add(passRow, 1, row++);
        grid.add(new Label("URL / App"),  0, row); grid.add(tfUrl,    1, row++);
        grid.add(new Label("Notes"),      0, row); grid.add(taNote,   1, row);

        ColumnConstraints c1 = new ColumnConstraints(100);
        ColumnConstraints c2 = new ColumnConstraints(300);
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        return grid;
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error"); a.setContentText(msg); a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Success"); a.setContentText(msg); a.showAndWait();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}