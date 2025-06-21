package com.focustime.controller;

import com.focustime.service.AuthService;
import com.focustime.util.SceneManager;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Region;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleSignup() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Error", "Semua field wajib diisi.");
            return;
        }

        if (!password.equals(confirm)) {
            showAlert(Alert.AlertType.WARNING, "Password Tidak Cocok", "Password dan konfirmasi tidak sama.");
            return;
        }

        boolean registered = authService.registerUser(username, password);

        if (registered) {
            showAlert(Alert.AlertType.INFORMATION, "Pendaftaran Berhasil", "Silakan login dengan akun baru.");
            SceneManager.switchScene("login.fxml", "Login", "login.css", usernameField);
        } else {
            showAlert(Alert.AlertType.ERROR, "Pendaftaran Gagal", "Username sudah digunakan.");
        }
    }

    @FXML
    private void goToLogin() {
        SceneManager.switchScene("login.fxml", "Login", "login.css", usernameField);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }
}
