package com.focustime;

import java.sql.Connection;
import java.sql.SQLException;

import com.focustime.util.DBConnection;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // Load login.fxml as initial screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/login.css").toExternalForm());

            stage.setTitle("FocusTime - Login");
            stage.setScene(scene);

            // ✅ Biar window langsung maksimal tapi masih ada border & bisa di-resize
            stage.setResizable(true);
            stage.setMaximized(true);

            stage.show();

        } catch (Exception e) {
            System.err.println("❌ Failed to load login UI: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        // Test DB connection before launching app
        try (Connection conn = DBConnection.connect()) {
            System.out.println("✅ Connected to database successfully!");
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }

        // Launch JavaFX app
        launch(args);
    }
}
