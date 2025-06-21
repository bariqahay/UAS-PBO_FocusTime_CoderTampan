package com.focustime.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class SceneManager {

    public static void switchScene(String fxmlFilename, String title, String cssFilename, Control sourceControl) {
        try {
            URL fxmlUrl = SceneManager.class.getResource("/fxml/" + fxmlFilename);
            if (fxmlUrl == null) throw new IOException("FXML not found: " + fxmlFilename);

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root);

            if (cssFilename != null && !cssFilename.isBlank()) {
                URL cssUrl = SceneManager.class.getResource("/css/" + cssFilename);
                if (cssUrl == null) throw new IOException("CSS not found: " + cssFilename);
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            Stage stage = (Stage) sourceControl.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (IOException e) {
            System.err.println("Gagal memuat halaman: " + fxmlFilename);
            e.printStackTrace();
        }
    }
}
