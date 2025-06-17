package com.focustime.controller;

import com.focustime.model.TimerModel;
import com.focustime.model.CategoryModel;
import com.focustime.util.DBConnection;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.beans.binding.Bindings;
import javafx.util.StringConverter;
import com.focustime.model.SessionModel;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Button btnAddCategory;
    @FXML private Button btnRemoveCategory;

    private TimerModel timerModel;
    private CategoryModel categoryModel;
    private MediaPlayer notificationSound;
    private LocalDateTime sessionStartTime;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        timerModel = new TimerModel();
        categoryModel = new CategoryModel();
        timerModel.setDuration(25);
        setupSpinner();
        setupCategoryComboBox();
        setupBindings();
        setupNotificationSound();
        timerModel.remainingSecondsProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.intValue() == 0 && oldVal.intValue() > 0) {
                onTimerFinished();
            }
        });
    }

    private void setupSpinner() {
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 180, 25);
        customSpinner.setValueFactory(valueFactory);
        customSpinner.setEditable(true);
    }

    private void setupCategoryComboBox() {
        categoryComboBox.setItems(categoryModel.getCategories());
        categoryComboBox.setValue(categoryModel.getSelectedCategory());
        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                categoryModel.setSelectedCategory(newVal);
            }
        });
        categoryComboBox.setCellFactory(listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
            }
        });
        categoryComboBox.setConverter(new StringConverter<String>() {
            @Override public String toString(String object) { return object != null ? object : ""; }
            @Override public String fromString(String string) { return string; }
        });
    }

    private void setupBindings() {
        timeLabel.textProperty().bind(timerModel.timeDisplayProperty());
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
        btnStart.disableProperty().bind(timerModel.isRunningProperty().and(timerModel.isPausedProperty().not()));
        btnPause.disableProperty().bind(timerModel.isRunningProperty().not());
        btnStop.disableProperty().bind(timerModel.isRunningProperty().not());
        btnRemoveCategory.disableProperty().bind(
            Bindings.createBooleanBinding(() -> 
                categoryModel.getCategories().size() <= 1,
                categoryModel.getCategories()
            )
        );
        progressBar.progressProperty().bind(
            Bindings.createDoubleBinding(() -> {
                if (timerModel.getTotalSeconds() == 0) return 0.0;
                return 1.0 - (double) timerModel.getRemainingSeconds() / timerModel.getTotalSeconds();
            }, timerModel.remainingSecondsProperty(), timerModel.totalSecondsProperty())
        );
        timerModel.totalSecondsProperty().addListener((obs, oldVal, newVal) -> {
            updatePresetButtonStyles(newVal.intValue() / 60);
        });
    }

    private void setupNotificationSound() {
        try {
            // Replace with your own notification if desired
        } catch (Exception e) {
            System.out.println("Could not load notification sound: " + e.getMessage());
        }
    }

    

    @FXML
    private void openHistoryView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/history.fxml"));
            Parent historyRoot = loader.load();

            Stage historyStage = new Stage();
            historyStage.setTitle("Histori Belajar");
            historyStage.setScene(new Scene(historyRoot));
            historyStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Gagal membuka histori: " + e.getMessage());
        }
    }


    @FXML
    private void addCategory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add New Category");
        dialog.setHeaderText("Create a new focus category");
        dialog.setContentText("Category name:");
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/timer.css").toExternalForm()
        );
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(categoryName -> {
            if (categoryModel.addCategory(categoryName)) {
                categoryComboBox.setValue(categoryName);
                showInfoAlert("Success", "Category '" + categoryName + "' added successfully!");
            } else {
                showWarningAlert("Category Exists", "Category already exists or invalid.");
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
                if (!categoryModel.getCategories().isEmpty()) {
                    categoryComboBox.setValue(categoryModel.getCategories().get(0));
                }
                showInfoAlert("Success", "Category removed successfully!");
            } else {
                showWarningAlert("Cannot Remove", "Cannot remove selected or last category.");
            }
        }
    }

    @FXML private void setDuration25() { if (!timerModel.isRunning()) timerModel.setDuration(25); }
    @FXML private void setDuration45() { if (!timerModel.isRunning()) timerModel.setDuration(45); }
    @FXML private void setDuration60() { if (!timerModel.isRunning()) timerModel.setDuration(60); }
    @FXML private void setCustomDuration() {
        if (!timerModel.isRunning()) {
            int minutes = customSpinner.getValue();
            timerModel.setDuration(minutes);
        }
    }

    @FXML
    private void startTimer() {
        String category = categoryModel.getSelectedCategory();
        System.out.println("Starting timer session - Category: " + category + ", Duration: " + (timerModel.getTotalSeconds() / 60) + " minutes");
        sessionStartTime = LocalDateTime.now();
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
        } catch (NumberFormatException e) {}
        showWarningAlert("Invalid Time Format", 
            "Use HH:MM:SS or MM:SS (max 03:00:00)\nExamples: 01:30:00 or 25:00");
        return false;
    }

    private void onTimerFinished() {
        Platform.runLater(() -> {
            playNotificationSound();

            String category = categoryModel.getSelectedCategory();
            int durationMinutes = timerModel.getTotalSeconds() / 60;
            String note = "";

            // Dialog input catatan singkat
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Sesi Selesai");
            dialog.setHeaderText("Sesi " + category + " selesai (" + durationMinutes + " menit)");
            dialog.setContentText("Catatan singkat:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                note = result.get().trim();
            }

            // Simpan ke database
            SessionModel session = new SessionModel();
            session.saveSession(category, durationMinutes, note);

            showInfoAlert("Sesi Dicatat", "Sesi telah dicatat ke database.");
            btnPause.setText("Pause");
        });
    }


    private void playNotificationSound() {
        try {
            if (notificationSound != null) {
                notificationSound.stop();
                notificationSound.play();
            } else {
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        } catch (Exception e) {
            System.out.println("Could not play notification sound: " + e.getMessage());
            try { java.awt.Toolkit.getDefaultToolkit().beep(); } catch (Exception ex) {}
        }
    }

    private void updatePresetButtonStyles(int minutes) {
        btn25.getStyleClass().removeAll("preset-btn-selected");
        btn45.getStyleClass().removeAll("preset-btn-selected");
        btn60.getStyleClass().removeAll("preset-btn-selected");
        switch (minutes) {
            case 25 -> btn25.getStyleClass().add("preset-btn-selected");
            case 45 -> btn45.getStyleClass().add("preset-btn-selected");
            case 60 -> btn60.getStyleClass().add("preset-btn-selected");
        }
    }

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

    public CategoryModel getCategoryModel() {
        return categoryModel;
    }
}
