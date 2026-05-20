package br.com.vidasilva.jnventoryfx.controller;

import br.com.vidasilva.jnventoryfx.model.User;
import br.com.vidasilva.jnventoryfx.service.Session;
import br.com.vidasilva.jnventoryfx.service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
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

        if (user.isMustChangePassword() && !forcePasswordChange(user)) {
            return;
        }

        Session.setCurrentUser(user);

        if (rememberMe) {
            System.out.println("Remember me selected for: " + email);
        }

        openDashboard();
    }

    @FXML
    private void handleForgotPassword() {
        Dialog<ButtonType> emailDialog = new Dialog<>();
        emailDialog.setTitle("Password Recovery");
        emailDialog.setHeaderText("Enter your account email to receive a reset code.");
        emailDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField emailField = new TextField();
        emailField.setText(signInEmailField.getText());
        emailField.setPromptText("you@example.com");

        GridPane emailGrid = new GridPane();
        emailGrid.setHgap(8);
        emailGrid.setVgap(8);
        emailGrid.add(new Label("Email"), 0, 0);
        emailGrid.add(emailField, 1, 0);
        emailDialog.getDialogPane().setContent(emailGrid);

        ButtonType emailResult = emailDialog.showAndWait().orElse(ButtonType.CANCEL);

        if (emailResult == ButtonType.CANCEL) {
            return;
        }

        String email = emailField.getText();

        try {
            UserService.PasswordResetRequestResult result = userService.requestPasswordReset(email);
            showPasswordResetRequestInfo(result);
            showPasswordResetCompletionDialog(email);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError("Password Recovery Failed", exception.getMessage());
        }
    }

    @FXML
    private void handleSignUp() {
        String username = signUpUsernameField.getText();
        String email = signUpEmailField.getText();
        String password = signUpPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        try {
            boolean success = userService.registerUser(username, email, password, confirmPassword);

            if (success) {
                showInfo("Account Created", "Your account has been created as a cashier user.");
                clearSignUpFields();
            } else {
                showError("Sign Up Failed", "Please check your information and try again.");
            }
        } catch (IllegalArgumentException exception) {
            showError("Sign Up Failed", exception.getMessage());
        }
    }


    private void showPasswordResetRequestInfo(UserService.PasswordResetRequestResult result) {
        String message = "If that email belongs to an account, a password reset code has been sent.";

        if (result.developmentOutboxFile() != null) {
            message += "\n\nSMTP is not configured, so the demo email was written to:\n" + result.developmentOutboxFile();
        }

        showInfo("Password Reset Code Sent", message);
    }

    private void showPasswordResetCompletionDialog(String email) {
        while (true) {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Reset Password");
            dialog.setHeaderText("Enter the emailed reset code and choose a new password.");
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            TextField codeField = new TextField();
            PasswordField newPasswordField = new PasswordField();
            PasswordField confirmPasswordField = new PasswordField();
            codeField.setPromptText("8-digit reset code");
            newPasswordField.setPromptText("New password");
            confirmPasswordField.setPromptText("Confirm new password");

            GridPane gridPane = new GridPane();
            gridPane.setHgap(8);
            gridPane.setVgap(8);
            gridPane.add(new Label("Reset code"), 0, 0);
            gridPane.add(codeField, 1, 0);
            gridPane.add(new Label("New password"), 0, 1);
            gridPane.add(newPasswordField, 1, 1);
            gridPane.add(new Label("Confirm password"), 0, 2);
            gridPane.add(confirmPasswordField, 1, 2);
            gridPane.add(new Label("Use at least 8 characters with at least one letter and one number."), 0, 3, 2, 1);
            dialog.getDialogPane().setContent(gridPane);

            ButtonType result = dialog.showAndWait().orElse(ButtonType.CANCEL);

            if (result == ButtonType.CANCEL) {
                return;
            }

            try {
                boolean changed = userService.resetPasswordWithRecoveryCode(
                        email,
                        codeField.getText(),
                        newPasswordField.getText(),
                        confirmPasswordField.getText()
                );

                if (changed) {
                    signInEmailField.setText(email);
                    signInPasswordField.clear();
                    showInfo("Password Updated", "Your password was changed. You can now sign in with the new password.");
                    return;
                }

                showError("Password Not Changed", "Please check the reset code and password fields.");
            } catch (IllegalArgumentException exception) {
                showError("Password Not Changed", exception.getMessage());
            }
        }
    }

    private boolean forcePasswordChange(User user) {
        while (true) {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Change Temporary Password");
            dialog.setHeaderText("This account is using a temporary password.");
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            PasswordField newPasswordField = new PasswordField();
            PasswordField confirmPasswordField = new PasswordField();
            newPasswordField.setPromptText("New password");
            confirmPasswordField.setPromptText("Confirm new password");

            GridPane gridPane = new GridPane();
            gridPane.setHgap(8);
            gridPane.setVgap(8);
            gridPane.add(new Label("New password"), 0, 0);
            gridPane.add(newPasswordField, 1, 0);
            gridPane.add(new Label("Confirm password"), 0, 1);
            gridPane.add(confirmPasswordField, 1, 1);
            gridPane.add(new Label("Use at least 8 characters with at least one letter and one number. Do not reuse the temporary password."), 0, 2, 2, 1);
            dialog.getDialogPane().setContent(gridPane);

            ButtonType result = dialog.showAndWait().orElse(ButtonType.CANCEL);

            if (result == ButtonType.CANCEL) {
                showInfo("Password Change Required", "You must change the temporary password before opening the dashboard.");
                return false;
            }

            try {
                boolean changed = userService.changePasswordAfterFirstLogin(
                        user,
                        newPasswordField.getText(),
                        confirmPasswordField.getText()
                );

                if (changed) {
                    showInfo("Password Updated", "Your password was changed. You can now use the dashboard.");
                    return true;
                }

                showError("Password Not Changed", "Please check the password and try again.");
            } catch (IllegalArgumentException exception) {
                showError("Password Not Changed", exception.getMessage());
            }
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
