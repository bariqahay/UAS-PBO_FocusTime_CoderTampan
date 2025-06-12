package com.focustime;

import java.sql.Connection;
import java.sql.SQLException;
import com.focustime.util.DBConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {
    

    @Override
    public void start(Stage stage) {
        try {
            // Load FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/timer.fxml"));
            Parent root = loader.load();

            // Get VBox from fx:id
            VBox rootVBox = (VBox) loader.getNamespace().get("rootVBox");
            if (rootVBox != null) {
                rootVBox.prefWidthProperty().bind(stage.widthProperty());
                rootVBox.prefHeightProperty().bind(stage.heightProperty());
            }

            // Create scene and add CSS
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/timer.css").toExternalForm());

            // Setup stage with responsive sizing
            stage.setTitle("FocusTime - Focus Timer");
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading Timer UI: " + e.getMessage());
        }
    }

    
    public static void main(String[] args) {
        // Test database connection
        try (Connection conn = DBConnection.connect()) {
            System.out.println("✓ Connected to database successfully!");
        } catch (SQLException e) {
            System.err.println("✗ Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Launch JavaFX application
        launch(args);
    }
}