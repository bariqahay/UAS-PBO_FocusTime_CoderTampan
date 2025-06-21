package com.focustime.controller;

import com.focustime.model.UserModel;
import com.focustime.service.AuthService;
import com.focustime.session.CurrentUser;
import com.focustime.util.SceneManager;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Region;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Username dan password tidak boleh kosong");
            return;
        }

        AuthService authService = new AuthService();
        UserModel user = authService.loginUser(username, password);

        if (user != null) {
            // Simpan user login kalau perlu
            // Session.setCurrentUser(user);
            CurrentUser.login(user);
            SceneManager.switchScene("timer.fxml", "Focus Timer", "timer.css", usernameField);
        } else {
            showAlert("Login Gagal", "Username atau password salah");
        }
    }

    @FXML
    private void goToSignup() {
        SceneManager.switchScene("signup.fxml", "Daftar Akun", "signup.css", usernameField);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
