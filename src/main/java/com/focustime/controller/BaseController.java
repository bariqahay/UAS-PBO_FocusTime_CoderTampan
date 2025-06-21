package com.focustime.controller;

import javafx.scene.control.Alert;
import javafx.scene.control.Control;
import javafx.scene.layout.Region;
import com.focustime.util.SceneManager;

public abstract class BaseController {

    protected void showInfo(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    protected void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, message);
    }

    protected void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    protected void switchScene(String fxml, String title, String css, Control node) {
        SceneManager.switchScene(fxml, title, css, node);
    }
}
