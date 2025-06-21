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

            // Ambil stage dari komponen sumber
            Stage stage = (Stage) sourceControl.getScene().getWindow();

            // ✅ Simpan status window sebelum ganti scene
            boolean wasMaximized = stage.isMaximized();
            double x = stage.getX();
            double y = stage.getY();
            double width = stage.getWidth();
            double height = stage.getHeight();

            // Ganti scene
            stage.setScene(scene);
            stage.setTitle(title);

            // ✅ Restore status window
            stage.setX(x);
            stage.setY(y);
            stage.setWidth(width);
            stage.setHeight(height);
            stage.setMaximized(wasMaximized);

            // Optional: biar tetap bisa resize
            stage.setResizable(true);

        } catch (IOException e) {
            System.err.println("Gagal memuat halaman: " + fxmlFilename);
            e.printStackTrace();
        }
    }
}

