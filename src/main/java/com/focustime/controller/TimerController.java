package com.focustime.controller;

import com.focustime.model.TimerModel;
import com.focustime.model.CategoryModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.beans.binding.Bindings;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class TimerController implements Initializable {
    
    @FXML private Label timeLabel;
    @FXML private Label statusLabel;
    @FXML private Button btn25, btn45, btn60, btnCustom, btnSetTime;
    @FXML private Button btnStart, btnPause, btnStop, btnReset;
    @FXML private Spinner<Integer> customSpinner;
    @FXML private TextField timeInputField;
    @FXML private ProgressBar progressBar;
    
    // Category components
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Button btnAddCategory;
    @FXML private Button btnRemoveCategory;
    
    private TimerModel timerModel;
    private CategoryModel categoryModel;
    private MediaPlayer notificationSound;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize models
        timerModel = new TimerModel();
        categoryModel = new CategoryModel();
        timerModel.setDuration(25); // Default 25 minutes
        
        // Setup components
        setupSpinner();
        setupCategoryComboBox();
        
        // Bind UI to model properties
        setupBindings();
        
        // Setup notification sound
        setupNotificationSound();
        
        // Listen for timer completion
        timerModel.remainingSecondsProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.intValue() == 0 && oldVal.intValue() > 0) {
                onTimerFinished();
            }
        });
    }
    
    private void setupSpinner() {
        // Create proper SpinnerValueFactory
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 180, 25);
        customSpinner.setValueFactory(valueFactory);
        customSpinner.setEditable(true);
    }
    
    private void setupCategoryComboBox() {
        // Bind ComboBox items to category model
        categoryComboBox.setItems(categoryModel.getCategories());
        
        // Set initial selection
        categoryComboBox.setValue(categoryModel.getSelectedCategory());
        
        // Bind selected category to model
        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                categoryModel.setSelectedCategory(newVal);
            }
        });
        
        // Custom cell factory for better display
        categoryComboBox.setCellFactory(listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }
            }
        });
        
        // Custom string converter
        categoryComboBox.setConverter(new StringConverter<String>() {
            @Override
            public String toString(String object) {
                return object != null ? object : "";
            }
            
            @Override
            public String fromString(String string) {
                return string;
            }
        });
    }
    
    private void setupBindings() {
        // Bind time display
        timeLabel.textProperty().bind(timerModel.timeDisplayProperty());
        
        // Bind status label with category info
        statusLabel.textProperty().bind(
            Bindings.createStringBinding(() -> {
                String category = categoryModel.getSelectedCategory();
                String categoryText = category != null ? " - " + category : "";
                
                if (timerModel.isRunning()) {
                    return (timerModel.isPaused() ? "Paused" : "Focusing...") + categoryText;
                } else {
                    String baseStatus = timerModel.getRemainingSeconds() == timerModel.getTotalSeconds() 
                        ? "Ready to Focus" : "Session Stopped";
                    return baseStatus + categoryText;
                }
            }, timerModel.isRunningProperty(), timerModel.isPausedProperty(), 
               timerModel.remainingSecondsProperty(), categoryModel.selectedCategoryProperty())
        );
        
        // Bind button states
        btnStart.disableProperty().bind(timerModel.isRunningProperty().and(timerModel.isPausedProperty().not()));
        btnPause.disableProperty().bind(timerModel.isRunningProperty().not());
        btnStop.disableProperty().bind(timerModel.isRunningProperty().not());
        
        // Category button bindings
        btnRemoveCategory.disableProperty().bind(
            Bindings.createBooleanBinding(() -> 
                categoryModel.getCategories().size() <= 1,
                categoryModel.getCategories()
            )
        );
        
        // Bind progress bar
        progressBar.progressProperty().bind(
            Bindings.createDoubleBinding(() -> {
                if (timerModel.getTotalSeconds() == 0) return 0.0;
                return 1.0 - (double) timerModel.getRemainingSeconds() / timerModel.getTotalSeconds();
            }, timerModel.remainingSecondsProperty(), timerModel.totalSecondsProperty())
        );
        
        // Bind preset button styles (highlight selected duration)
        timerModel.totalSecondsProperty().addListener((obs, oldVal, newVal) -> {
            updatePresetButtonStyles(newVal.intValue() / 60);
        });
    }
    
    private void setupNotificationSound() {
        try {
            // You can replace this with a custom sound file
            // For now, using system beep as placeholder
            // URL soundUrl = getClass().getResource("/sounds/notification.wav");
            // if (soundUrl != null) {
            //     Media sound = new Media(soundUrl.toString());
            //     notificationSound = new MediaPlayer(sound);
            // }
        } catch (Exception e) {
            System.out.println("Could not load notification sound: " + e.getMessage());
        }
    }
    
    // Category Actions
    @FXML
    private void addCategory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add New Category");
        dialog.setHeaderText("Create a new focus category");
        dialog.setContentText("Category name:");
        
        // Style the dialog
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/timer.css").toExternalForm()
        );
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(categoryName -> {
            if (categoryModel.addCategory(categoryName)) {
                categoryComboBox.setValue(categoryName);
                showInfoAlert("Success", "Category '" + categoryName + "' added successfully!");
            } else {
                showWarningAlert("Category Exists", "Category '" + categoryName + "' already exists or is invalid.");
            }
        });
    }
    
    @FXML
    private void removeCategory() {
        String selectedCategory = categoryComboBox.getValue();
        if (selectedCategory == null) {
            showWarningAlert("No Selection", "Please select a category to remove.");
            return;
        }
        
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Remove Category");
        confirmDialog.setHeaderText("Are you sure?");
        confirmDialog.setContentText("Remove category '" + selectedCategory + "'?");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (categoryModel.removeCategory(selectedCategory)) {
                // Select first available category
                if (!categoryModel.getCategories().isEmpty()) {
                    categoryComboBox.setValue(categoryModel.getCategories().get(0));
                }
                showInfoAlert("Success", "Category removed successfully!");
            } else {
                showWarningAlert("Cannot Remove", "Cannot remove the currently selected category or the last remaining category.");
            }
        }
    }
    
    // Timer Duration Actions (existing methods)
    @FXML
    private void setDuration25() {
        if (!timerModel.isRunning()) {
            timerModel.setDuration(25);
        }
    }
    
    @FXML
    private void setDuration45() {
        if (!timerModel.isRunning()) {
            timerModel.setDuration(45);
        }
    }
    
    @FXML
    private void setDuration60() {
        if (!timerModel.isRunning()) {
            timerModel.setDuration(60);
        }
    }
    
    @FXML
    private void setCustomDuration() {
        if (!timerModel.isRunning()) {
            int minutes = customSpinner.getValue();
            timerModel.setDuration(minutes);
        }
    }
    
    // Timer Control Actions (existing methods)
    @FXML
    private void startTimer() {
        // Log the session start with category (for future database integration)
        String category = categoryModel.getSelectedCategory();
        System.out.println("Starting timer session - Category: " + category + ", Duration: " + (timerModel.getTotalSeconds() / 60) + " minutes");
        
        timerModel.start();
    }
    
    @FXML
    private void pauseTimer() {
        if (timerModel.isPaused()) {
            timerModel.resume();
            btnPause.setText("Pause");
        } else {
            timerModel.pause();
            btnPause.setText("Resume");
        }
    }
    
    @FXML
    private void stopTimer() {
        timerModel.stop();
        btnPause.setText("Pause");
    }
    
    @FXML
    private void resetTimer() {
        timerModel.reset();
        btnPause.setText("Pause");
    }
    
    @FXML
    private void setTimeFromInput() {
        if (!timerModel.isRunning()) {
            String timeInput = timeInputField.getText().trim();
            if (parseAndSetTime(timeInput)) {
                timeInputField.clear();
            }
        }
    }
    
    // Helper methods (existing methods)
    private boolean parseAndSetTime(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            if (parts.length == 3) {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = Integer.parseInt(parts[2]);
                
                int totalMinutes = hours * 60 + minutes + (seconds > 0 ? 1 : 0);
                if (totalMinutes > 0 && totalMinutes <= 180) {
                    timerModel.setDuration(totalMinutes);
                    return true;
                }
            } else if (parts.length == 2) {
                int minutes = Integer.parseInt(parts[0]);
                int seconds = Integer.parseInt(parts[1]);
                
                int totalMinutes = minutes + (seconds > 0 ? 1 : 0);
                if (totalMinutes > 0 && totalMinutes <= 180) {
                    timerModel.setDuration(totalMinutes);
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            // Invalid format
        }
        
        showWarningAlert("Invalid Time Format", 
            "Please enter time in HH:MM:SS or MM:SS format\n" +
            "Example: 01:30:00 or 25:00\n" +
            "Maximum: 03:00:00 (180 minutes)");
        return false;
    }
    
    private void onTimerFinished() {
        Platform.runLater(() -> {
            // Play notification sound
            playNotificationSound();
            
            // Show completion dialog with category info
            String category = categoryModel.getSelectedCategory();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Session Complete");
            alert.setHeaderText("Focus Session Finished!");
            alert.setContentText("Great job! You've completed your " + category + " session.");
            alert.showAndWait();
            
            // Log completion (for future database integration)
            System.out.println("Session completed - Category: " + category + ", Duration: " + (timerModel.getTotalSeconds() / 60) + " minutes");
            
            // Reset button text
            btnPause.setText("Pause");
        });
    }
    
    private void playNotificationSound() {
        try {
            if (notificationSound != null) {
                notificationSound.stop();
                notificationSound.play();
            } else {
                // Fallback to system beep
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        } catch (Exception e) {
            System.out.println("Could not play notification sound: " + e.getMessage());
            // Fallback beep
            try {
                java.awt.Toolkit.getDefaultToolkit().beep();
            } catch (Exception ex) {
                // Silent fallback
            }
        }
    }
    
    private void updatePresetButtonStyles(int minutes) {
        // Remove selected style from all buttons
        btn25.getStyleClass().removeAll("preset-btn-selected");
        btn45.getStyleClass().removeAll("preset-btn-selected");
        btn60.getStyleClass().removeAll("preset-btn-selected");
        
        // Add selected style to current duration
        switch (minutes) {
            case 25:
                btn25.getStyleClass().add("preset-btn-selected");
                break;
            case 45:
                btn45.getStyleClass().add("preset-btn-selected");
                break;
            case 60:
                btn60.getStyleClass().add("preset-btn-selected");
                break;
        }
    }
    
    // Utility methods for alerts
    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarningAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Getter for CategoryModel (for future use)
    public CategoryModel getCategoryModel() {
        return categoryModel;
    }
}