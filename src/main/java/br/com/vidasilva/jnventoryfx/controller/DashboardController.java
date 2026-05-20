package br.com.vidasilva.jnventoryfx.controller;

import br.com.vidasilva.jnventoryfx.model.AuditLog;
import br.com.vidasilva.jnventoryfx.model.CarPart;
import br.com.vidasilva.jnventoryfx.model.Supplier;
import br.com.vidasilva.jnventoryfx.model.User;
import br.com.vidasilva.jnventoryfx.model.UserRole;
import br.com.vidasilva.jnventoryfx.security.AuthorizationService;
import br.com.vidasilva.jnventoryfx.security.Permission;
import br.com.vidasilva.jnventoryfx.service.AuditService;
import br.com.vidasilva.jnventoryfx.service.InventoryService;
import br.com.vidasilva.jnventoryfx.service.Session;
import br.com.vidasilva.jnventoryfx.service.SupplierService;
import br.com.vidasilva.jnventoryfx.service.UserService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;

public class DashboardController {

    @FXML private Label currentUserLabel;
    @FXML private Label lowStockSummaryLabel;

    @FXML private Tab suppliersTab;
    @FXML private Tab warehouseTab;
    @FXML private Tab usersTab;
    @FXML private Tab auditTab;

    @FXML private VBox partRegistrationPane;
    @FXML private VBox salePane;
    @FXML private VBox supplierRegistrationPane;
    @FXML private VBox warehouseUpdatePane;
    @FXML private VBox userRegistrationPane;

    @FXML private TableView<CarPart> partsTable;
    @FXML private TableColumn<CarPart, Integer> partIdColumn;
    @FXML private TableColumn<CarPart, String> partNameColumn;
    @FXML private TableColumn<CarPart, String> partManufacturerColumn;
    @FXML private TableColumn<CarPart, String> partVehiclesColumn;
    @FXML private TableColumn<CarPart, String> partSupplierColumn;
    @FXML private TableColumn<CarPart, Double> partPriceColumn;
    @FXML private TableColumn<CarPart, Integer> partQuantityColumn;
    @FXML private TableColumn<CarPart, String> partAddressColumn;
    @FXML private TableColumn<CarPart, Integer> partMaxCapacityColumn;
    @FXML private TableColumn<CarPart, Integer> partLowLevelColumn;
    @FXML private TableColumn<CarPart, String> partStatusColumn;

    @FXML private TextField partNameField;
    @FXML private TextField partManufacturerField;
    @FXML private TextField partVehiclesField;
    @FXML private ComboBox<Supplier> partSupplierComboBox;
    @FXML private TextField partPriceField;
    @FXML private TextField partQuantityField;
    @FXML private TextField partWarehouseAddressField;
    @FXML private TextField partMaxCapacityField;
    @FXML private TextField partLowLevelField;

    @FXML private TextField salePartIdField;
    @FXML private TextField saleQuantityField;
    @FXML private TextField searchField;

    @FXML private TableView<Supplier> suppliersTable;
    @FXML private TableColumn<Supplier, Integer> supplierIdColumn;
    @FXML private TableColumn<Supplier, String> supplierNameColumn;
    @FXML private TableColumn<Supplier, String> supplierPhoneColumn;
    @FXML private TableColumn<Supplier, String> supplierEmailColumn;
    @FXML private TableColumn<Supplier, String> supplierAddressColumn;
    @FXML private TableColumn<Supplier, String> supplierNotesColumn;

    @FXML private TextField supplierNameField;
    @FXML private TextField supplierPhoneField;
    @FXML private TextField supplierEmailField;
    @FXML private TextField supplierAddressField;
    @FXML private TextField supplierNotesField;

    @FXML private TextField warehousePartIdField;
    @FXML private TextField warehouseAddressField;
    @FXML private TextField warehouseMaxCapacityField;
    @FXML private TextField warehouseLowLevelField;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> userNameColumn;
    @FXML private TableColumn<User, String> userEmailColumn;
    @FXML private TableColumn<User, UserRole> userRoleColumn;

    @FXML private TextField newUsernameField;
    @FXML private TextField newUserEmailField;
    @FXML private ComboBox<UserRole> newUserRoleComboBox;

    @FXML private TableView<AuditLog> auditTable;
    @FXML private TableColumn<AuditLog, String> auditTimeColumn;
    @FXML private TableColumn<AuditLog, String> auditActorColumn;
    @FXML private TableColumn<AuditLog, String> auditRoleColumn;
    @FXML private TableColumn<AuditLog, String> auditActionColumn;
    @FXML private TableColumn<AuditLog, String> auditTargetColumn;
    @FXML private TableColumn<AuditLog, String> auditStatusColumn;
    @FXML private TableColumn<AuditLog, String> auditDetailsColumn;

    private final InventoryService inventoryService = new InventoryService();
    private final SupplierService supplierService = new SupplierService();
    private final UserService userService = new UserService();
    private final AuditService auditService = new AuditService();

    @FXML
    private void initialize() {
        configurePartTable();
        configureSupplierTable();
        configureUserTable();
        configureAuditTable();
        configureComboBoxes();
        configureSelectionListeners();

        partsTable.setItems(inventoryService.getParts());
        suppliersTable.setItems(supplierService.getSuppliers());
        usersTable.setItems(userService.getUsers());
        if (AuthorizationService.currentUserCan(Permission.VIEW_AUDIT_LOGS)) {
            auditTable.setItems(auditService.getRecentLogs());
        } else {
            auditTable.setItems(FXCollections.observableArrayList());
        }

        applyCurrentUserPermissions();
        updateLowStockSummary();
    }

    @FXML
    private void handleRegisterPart() {
        try {
            inventoryService.registerPart(
                    partNameField.getText(),
                    partManufacturerField.getText(),
                    partVehiclesField.getText(),
                    partSupplierComboBox.getValue(),
                    parseDouble(partPriceField, "unit price"),
                    parseInt(partQuantityField, "quantity"),
                    partWarehouseAddressField.getText(),
                    parseInt(partMaxCapacityField, "max capacity"),
                    parseInt(partLowLevelField, "low stock warning level")
            );

            clearPartForm();
            refreshInventoryView();
            showInfo("Part Registered", "The car part was added to inventory.");
        } catch (SecurityException exception) {
            showError("Access Denied", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            showError("Invalid Part", exception.getMessage());
        }
    }

    @FXML
    private void handleSellPart() {
        try {
            int partId = parseInt(salePartIdField, "part ID");
            int quantity = parseInt(saleQuantityField, "sale quantity");

            inventoryService.sellPart(partId, quantity);
            saleQuantityField.clear();
            refreshInventoryView();
            showInfo("Sale Registered", "Stock was updated for the sale.");
        } catch (SecurityException exception) {
            showError("Access Denied", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            showError("Sale Failed", exception.getMessage());
        }
    }

    @FXML
    private void handleSearchParts() {
        partsTable.setItems(inventoryService.searchParts(searchField.getText()));
        updateLowStockSummary();
    }

    @FXML
    private void handleResetSearch() {
        searchField.clear();
        partsTable.setItems(inventoryService.getParts());
        updateLowStockSummary();
    }

    @FXML
    private void handleRegisterSupplier() {
        try {
            Supplier supplier = supplierService.registerSupplier(
                    supplierNameField.getText(),
                    supplierPhoneField.getText(),
                    supplierEmailField.getText(),
                    supplierAddressField.getText(),
                    supplierNotesField.getText()
            );

            partSupplierComboBox.getSelectionModel().select(supplier);
            clearSupplierForm();
            suppliersTable.refresh();
            showInfo("Supplier Registered", "Supplier details were saved.");
        } catch (SecurityException exception) {
            showError("Access Denied", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            showError("Invalid Supplier", exception.getMessage());
        }
    }

    @FXML
    private void handleUpdateWarehouseData() {
        try {
            inventoryService.updateWarehouseData(
                    parseInt(warehousePartIdField, "part ID"),
                    warehouseAddressField.getText(),
                    parseInt(warehouseMaxCapacityField, "max capacity"),
                    parseInt(warehouseLowLevelField, "low stock warning level")
            );

            refreshInventoryView();
            showInfo("Warehouse Updated", "Location and capacity data were updated.");
        } catch (SecurityException exception) {
            showError("Access Denied", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            showError("Warehouse Update Failed", exception.getMessage());
        }
    }

    @FXML
    private void handleCreateUser() {
        UserRole role = newUserRoleComboBox.getValue();
        try {
            UserService.UserCreationResult result = userService.createUserWithTemporaryPassword(
                    newUsernameField.getText(),
                    newUserEmailField.getText(),
                    role
            );

            if (result == null) {
                showError("User Not Created", "Check username, email and role. The email may already exist.");
                return;
            }

            clearUserForm();
            usersTable.refresh();
            auditService.refreshRecentLogs();
            showTemporaryPassword(result.user().getEmail(), result.temporaryPassword());
        } catch (SecurityException exception) {
            showError("Access Denied", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            showError("User Not Created", exception.getMessage());
        }
    }

    @FXML
    private void handleResetSelectedUserPassword() {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            showError("No User Selected", "Select a user before sending a reset code.");
            return;
        }

        try {
            UserService.PasswordResetRequestResult result = userService.sendPasswordResetEmailForUser(selectedUser.getEmail());
            usersTable.refresh();
            auditService.refreshRecentLogs();
            showPasswordResetDelivery(result);
        } catch (SecurityException exception) {
            showError("Access Denied", exception.getMessage());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError("Password Reset Email Failed", exception.getMessage());
        }
    }

    @FXML
    private void handleRefreshAuditLogs() {
        try {
            auditService.refreshRecentLogs();
            auditTable.refresh();
        } catch (SecurityException exception) {
            showError("Access Denied", exception.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        Session.clear();

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/br/com/vidasilva/jnventoryfx/view/welcome-auth.fxml")
            );

            Scene scene = new Scene(loader.load(), 800, 600);
            Stage stage = (Stage) currentUserLabel.getScene().getWindow();
            stage.setTitle("JnventoryFX - Sign In");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException exception) {
            showError("Logout Error", "Could not return to the sign-in screen.");
            exception.printStackTrace();
        }
    }

    private void configurePartTable() {
        partIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        partNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        partManufacturerColumn.setCellValueFactory(new PropertyValueFactory<>("manufacturer"));
        partVehiclesColumn.setCellValueFactory(new PropertyValueFactory<>("compatibleVehicles"));
        partSupplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        partPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        partQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        partAddressColumn.setCellValueFactory(new PropertyValueFactory<>("warehouseAddressLabel"));
        partMaxCapacityColumn.setCellValueFactory(new PropertyValueFactory<>("maxCapacity"));
        partLowLevelColumn.setCellValueFactory(new PropertyValueFactory<>("lowCapacityWarningTriggerLevel"));
        partStatusColumn.setCellValueFactory(new PropertyValueFactory<>("stockStatus"));
    }

    private void configureSupplierTable() {
        supplierIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        supplierNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        supplierPhoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        supplierEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        supplierAddressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        supplierNotesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));
    }

    private void configureUserTable() {
        userNameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        userEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        userRoleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
    }

    private void configureAuditTable() {
        auditTimeColumn.setCellValueFactory(new PropertyValueFactory<>("eventTime"));
        auditActorColumn.setCellValueFactory(new PropertyValueFactory<>("actorEmail"));
        auditRoleColumn.setCellValueFactory(new PropertyValueFactory<>("actorRole"));
        auditActionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        auditTargetColumn.setCellValueFactory(new PropertyValueFactory<>("targetId"));
        auditStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        auditDetailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));
    }

    private void configureComboBoxes() {
        partSupplierComboBox.setItems(supplierService.getSuppliers());
        partSupplierComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Supplier supplier) {
                return supplier == null ? "" : supplier.getName();
            }

            @Override
            public Supplier fromString(String text) {
                return null;
            }
        });

        if (!supplierService.getSuppliers().isEmpty()) {
            partSupplierComboBox.getSelectionModel().selectFirst();
        }

        newUserRoleComboBox.setItems(FXCollections.observableArrayList(UserRole.values()));
        newUserRoleComboBox.getSelectionModel().select(UserRole.CASHIER);
    }

    private void configureSelectionListeners() {
        partsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldPart, selectedPart) -> {
            if (selectedPart == null) {
                return;
            }

            salePartIdField.setText(String.valueOf(selectedPart.getId()));
            warehousePartIdField.setText(String.valueOf(selectedPart.getId()));
            warehouseAddressField.setText(selectedPart.getWarehouseAddressLabel());
            warehouseMaxCapacityField.setText(String.valueOf(selectedPart.getMaxCapacity()));
            warehouseLowLevelField.setText(String.valueOf(selectedPart.getLowCapacityWarningTriggerLevel()));
        });
    }

    private void applyCurrentUserPermissions() {
        User currentUser = Session.getCurrentUser();

        if (currentUser == null) {
            currentUserLabel.setText("Signed in as Guest");
            disableProtectedControls();
            return;
        }

        UserRole role = currentUser.getRole();
        currentUserLabel.setText("Signed in as " + currentUser.getUsername() + " (" + role.getLabel() + ")");

        partRegistrationPane.setDisable(!AuthorizationService.currentUserCan(Permission.REGISTER_PART));
        salePane.setDisable(!AuthorizationService.currentUserCan(Permission.SELL_PART));

        suppliersTab.setDisable(!AuthorizationService.currentUserCan(Permission.VIEW_SUPPLIERS));
        supplierRegistrationPane.setDisable(!AuthorizationService.currentUserCan(Permission.MANAGE_SUPPLIERS));

        warehouseTab.setDisable(!AuthorizationService.currentUserCan(Permission.UPDATE_WAREHOUSE));
        warehouseUpdatePane.setDisable(!AuthorizationService.currentUserCan(Permission.UPDATE_WAREHOUSE));

        usersTab.setDisable(!AuthorizationService.currentUserCan(Permission.VIEW_USERS));
        userRegistrationPane.setDisable(!AuthorizationService.currentUserCan(Permission.MANAGE_USERS));

        auditTab.setDisable(!AuthorizationService.currentUserCan(Permission.VIEW_AUDIT_LOGS));
    }

    private void disableProtectedControls() {
        partRegistrationPane.setDisable(true);
        salePane.setDisable(true);
        suppliersTab.setDisable(true);
        supplierRegistrationPane.setDisable(true);
        warehouseTab.setDisable(true);
        warehouseUpdatePane.setDisable(true);
        usersTab.setDisable(true);
        userRegistrationPane.setDisable(true);
        auditTab.setDisable(true);
    }

    private void refreshInventoryView() {
        partsTable.refresh();
        partsTable.setItems(inventoryService.searchParts(searchField.getText()));
        updateLowStockSummary();
    }

    private void updateLowStockSummary() {
        long lowStockCount = inventoryService.getParts()
                .stream()
                .filter(part -> part.getQuantity() <= part.getLowCapacityWarningTriggerLevel())
                .count();

        lowStockSummaryLabel.setText("Low stock warnings: " + lowStockCount);
    }

    private int parseInt(TextField field, String label) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid " + label + ".");
        }
    }

    private double parseDouble(TextField field, String label) {
        try {
            return Double.parseDouble(field.getText().trim());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid " + label + ".");
        }
    }

    private void clearPartForm() {
        partNameField.clear();
        partManufacturerField.clear();
        partVehiclesField.clear();
        partPriceField.clear();
        partQuantityField.clear();
        partWarehouseAddressField.clear();
        partMaxCapacityField.clear();
        partLowLevelField.clear();
    }

    private void clearSupplierForm() {
        supplierNameField.clear();
        supplierPhoneField.clear();
        supplierEmailField.clear();
        supplierAddressField.clear();
        supplierNotesField.clear();
    }

    private void clearUserForm() {
        newUsernameField.clear();
        newUserEmailField.clear();
        newUserRoleComboBox.getSelectionModel().select(UserRole.CASHIER);
    }

    private void showTemporaryPassword(String email, String temporaryPassword) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("User Created");
        alert.setHeaderText("Temporary password for " + email);

        TextArea passwordArea = new TextArea(temporaryPassword);
        passwordArea.setEditable(false);
        passwordArea.setWrapText(true);
        passwordArea.setMaxWidth(Double.MAX_VALUE);
        passwordArea.setMaxHeight(80);

        Label warningLabel = new Label("Copy this password now. It will not be shown again, and the user must change it on first login.");
        warningLabel.setWrapText(true);

        VBox content = new VBox(8, warningLabel, passwordArea);
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }


    private void showPasswordResetDelivery(UserService.PasswordResetRequestResult result) {
        String message = "A password reset code was sent to " + result.email() + ".";

        if (result.developmentOutboxFile() != null) {
            message += "\n\nSMTP is not configured, so the demo email was written to:\n" + result.developmentOutboxFile();
        }

        showInfo("Password Reset Email", message);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
