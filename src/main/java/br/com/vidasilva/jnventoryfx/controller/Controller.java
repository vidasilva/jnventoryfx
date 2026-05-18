package br.com.vidasilva.jnventoryfx.controller;

import br.com.vidasilva.jnventoryfx.model.User;
import br.com.vidasilva.jnventoryfx.service.Session;
import br.com.vidasilva.jnventoryfx.service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class Controller {

    @FXML
    private TextField signInEmailField;

    @FXML
    private PasswordField signInPasswordField;

    @FXML
    private CheckBox rememberMeCheckBox;

    @FXML
    private TextField signUpUsernameField;

    @FXML
    private TextField signUpEmailField;

    @FXML
    private PasswordField signUpPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    private final UserService userService = new UserService();

    @FXML
    private void handleSignIn() {
        String email = signInEmailField.getText();
        String password = signInPasswordField.getText();
        boolean rememberMe = rememberMeCheckBox.isSelected();

        User user = userService.authenticateUser(email, password);

        if (user == null) {
            showError("Sign In Failed", "Invalid email or password.");
            return;
        }

        Session.setCurrentUser(user);

        if (rememberMe) {
            System.out.println("Remember me selected for: " + email);
        }

        openDashboard();
    }

    @FXML
    private void handleSignUp() {
        String username = signUpUsernameField.getText();
        String email = signUpEmailField.getText();
        String password = signUpPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        boolean success = userService.registerUser(username, email, password, confirmPassword);

        if (success) {
            showInfo("Account Created", "Your account has been created as a cashier user.");
            clearSignUpFields();
        } else {
            showError("Sign Up Failed", "Please check your information. The email may already exist or the passwords may not match.");
        }
    }

    private void openDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/br/com/vidasilva/jnventoryfx/view/inventory-dashboard.fxml")
            );

            Scene scene = new Scene(loader.load(), 1200, 800);
            Stage stage = (Stage) signInEmailField.getScene().getWindow();
            stage.setTitle("JnventoryFX - Auto Parts Inventory");
            stage.setMinWidth(1000);
            stage.setMinHeight(700);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException exception) {
            showError("Dashboard Error", "Could not open the inventory dashboard.");
            exception.printStackTrace();
        }
    }

    private void clearSignUpFields() {
        signUpUsernameField.clear();
        signUpEmailField.clear();
        signUpPasswordField.clear();
        confirmPasswordField.clear();
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
