package com.focustime.controller;

import com.focustime.model.TimerModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.beans.binding.Bindings;

import java.net.URL;
import java.util.ResourceBundle;

public class TimerController implements Initializable {
    
    @FXML private Label timeLabel;
    @FXML private Label statusLabel;
    @FXML private Button btn25, btn45, btn60, btnCustom, btnSetTime;
    @FXML private Button btnStart, btnPause, btnStop, btnReset;
    @FXML private Spinner<Integer> customSpinner;
    @FXML private TextField timeInputField;
    @FXML private ProgressBar progressBar;
    
    private TimerModel timerModel;
    private MediaPlayer notificationSound;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize model
        timerModel = new TimerModel();
        timerModel.setDuration(25); // Default 25 minutes
        
        // Setup spinner properly
        setupSpinner();
        
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
    
    private void setupBindings() {
        // Bind time display
        timeLabel.textProperty().bind(timerModel.timeDisplayProperty());
        
        // Bind status label
        statusLabel.textProperty().bind(
            Bindings.createStringBinding(() -> {
                if (timerModel.isRunning()) {
                    return timerModel.isPaused() ? "Paused" : "Focusing...";
                } else {
                    return timerModel.getRemainingSeconds() == timerModel.getTotalSeconds() 
                        ? "Ready to Focus" : "Session Stopped";
                }
            }, timerModel.isRunningProperty(), timerModel.isPausedProperty(), 
               timerModel.remainingSecondsProperty())
        );
        
        // Bind button states
        btnStart.disableProperty().bind(timerModel.isRunningProperty().and(timerModel.isPausedProperty().not()));
        btnPause.disableProperty().bind(timerModel.isRunningProperty().not());
        btnStop.disableProperty().bind(timerModel.isRunningProperty().not());
        
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
    
    @FXML
    private void startTimer() {
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
        
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Invalid Time Format");
        alert.setHeaderText("Please enter time in HH:MM:SS or MM:SS format");
        alert.setContentText("Example: 01:30:00 or 25:00\nMaximum: 03:00:00 (180 minutes)");
        alert.showAndWait();
        return false;
    }
    
    private void onTimerFinished() {
        Platform.runLater(() -> {
            // Play notification sound
            playNotificationSound();
            
            // Show completion dialog
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Session Complete");
            alert.setHeaderText("Focus Session Finished!");
            alert.setContentText("Great job! You've completed your focus session.");
            alert.showAndWait();
            
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
}