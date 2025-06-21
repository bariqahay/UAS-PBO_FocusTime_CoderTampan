package com.focustime.controller;

import com.focustime.model.CategoryModel;
import com.focustime.model.SessionModel;
import com.focustime.model.TimerModel;
import com.focustime.session.CurrentUser;

import java.awt.Toolkit;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

public class TimerController implements Initializable {
    @FXML private Label timeLabel, statusLabel, labelTodayTotal, labelWeekTotal;
    @FXML private Button btn25, btn45, btn60, btnStart, btnPause, btnStop, btnReset;
    @FXML private Button btnAddCategory, btnRemoveCategory, btnHistory;
    @FXML private ProgressBar progressBar;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<Integer> hourCombo, minuteCombo, secondCombo;

    private TimerModel timerModel;
    private CategoryModel categoryModel;
    private MediaPlayer notificationSound;
    private final SimpleIntegerProperty todayMinutes = new SimpleIntegerProperty(0);
    private final SimpleIntegerProperty weekMinutes = new SimpleIntegerProperty(0);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (CurrentUser.get() == null) {
            System.err.println("ERROR: No user logged in! Redirecting or aborting...");
            return;
        }

        int userId = CurrentUser.get().getId();
        timerModel = new TimerModel();
        categoryModel = new CategoryModel(userId);
        timerModel.setDuration(25);

        setupCategoryComboBox();
        setupBindings();
        setupNotificationSound();
        setupTimeDropdowns();

        timerModel.remainingSecondsProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.intValue() == 0 && oldVal.intValue() > 0) {
                onTimerFinished();
            }
        });
    }

    private void setupTimeDropdowns() {
        hourCombo.getItems().setAll(IntStream.rangeClosed(0, 3).boxed().collect(Collectors.toList()));
        minuteCombo.getItems().setAll(IntStream.rangeClosed(0, 59).boxed().collect(Collectors.toList()));
        secondCombo.getItems().setAll(IntStream.rangeClosed(0, 59).boxed().collect(Collectors.toList()));
        hourCombo.setValue(0);
        minuteCombo.setValue(25);
        secondCombo.setValue(0);
    }

    private void setupCategoryComboBox() {
        categoryComboBox.setItems(categoryModel.getCategories());
        categoryComboBox.setValue(categoryModel.getSelectedCategory());
        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) categoryModel.setSelectedCategory(newVal);
        });
    }

    private void setupBindings() {
        timeLabel.textProperty().bind(timerModel.timeDisplayProperty());
        statusLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            String cat = categoryModel.getSelectedCategory();
            String catText = (cat != null ? " - " + cat : "");
            if (timerModel.isRunning()) {
                return (timerModel.isPaused() ? "Paused" : "Focusing...") + catText;
            } else {
                return (timerModel.getRemainingSeconds() == timerModel.getTotalSeconds() ? "Ready to Focus" : "Session Stopped") + catText;
            }
        }, timerModel.isRunningProperty(), timerModel.isPausedProperty(), timerModel.remainingSecondsProperty(), categoryModel.selectedCategoryProperty()));

        btnStart.disableProperty().bind(timerModel.isRunningProperty().and(timerModel.isPausedProperty().not()));
        btnPause.disableProperty().bind(timerModel.isRunningProperty().not());
        btnStop.disableProperty().bind(timerModel.isRunningProperty().not());

        btnRemoveCategory.disableProperty().bind(Bindings.createBooleanBinding(
                () -> categoryModel.getCategories().size() <= 1,
                (Observable) categoryModel.getCategories()));

        progressBar.progressProperty().bind(Bindings.createDoubleBinding(() -> {
            return timerModel.getTotalSeconds() == 0 ? 0.0 : 1.0 - (double) timerModel.getRemainingSeconds() / timerModel.getTotalSeconds();
        }, timerModel.remainingSecondsProperty(), timerModel.totalSecondsProperty()));

        labelTodayTotal.textProperty().bind(Bindings.concat("Total Hari Ini: ", todayMinutes.asString(), " menit"));
        labelWeekTotal.textProperty().bind(Bindings.concat("Total Minggu Ini: ", weekMinutes.asString(), " menit"));
    }

    private void setupNotificationSound() {
        try {
            URL soundURL = getClass().getResource("/audio/session_end.mp3");
            if (soundURL != null) {
                Media media = new Media(soundURL.toString());
                notificationSound = new MediaPlayer(media);
                notificationSound.setVolume(0.7);
            }
        } catch (Exception e) {
            System.out.println("Could not load notification sound: " + e.getMessage());
        }
    }

    private void playNotificationSound() {
        try {
            if (notificationSound != null) {
                notificationSound.stop();
                notificationSound.play();
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        } catch (Exception e) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void onTimerFinished() {
        Platform.runLater(() -> {
            playNotificationSound();
            String category = categoryModel.getSelectedCategory();
            int durationMinutes = Math.max(1, timerModel.getTotalSeconds() / 60);

            if (category == null || category.trim().isEmpty()) {
                showWarningAlert("Kategori Kosong", "Silakan pilih kategori sebelum memulai sesi.");
                return;
            }

            todayMinutes.set(todayMinutes.get() + durationMinutes);
            weekMinutes.set(weekMinutes.get() + durationMinutes);

            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Sesi Selesai");
            dialog.setHeaderText("Sesi " + category + " selesai (" + durationMinutes + " menit)");
            dialog.setContentText("Catatan singkat:");
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dialog-styles.css").toExternalForm());

            Optional<String> result = dialog.showAndWait();
            String note = result.map(String::trim).orElse("");

            SessionModel session = new SessionModel();
            boolean success = session.saveSession(CurrentUser.get().getId(), category, durationMinutes, note);

            if (success) {
                showInfoAlert("Sesi Dicatat", "Sesi telah dicatat ke database.");
            } else {
                showWarningAlert("Gagal", "Sesi gagal disimpan.");
            }

            btnPause.setText("Pause");
        });
    }

    @FXML private void startTimer() { timerModel.start(); }

    @FXML private void setCustomTimeDropdown() {
        int h = hourCombo.getValue() != null ? hourCombo.getValue() : 0;
        int m = minuteCombo.getValue() != null ? minuteCombo.getValue() : 0;
        int s = secondCombo.getValue() != null ? secondCombo.getValue() : 0;
        int totalSeconds = h * 3600 + m * 60 + s;

        if (totalSeconds <= 0 || totalSeconds > 180 * 60) {
            showWarningAlert("Invalid Time", "Durasi total harus antara 1–180 menit.");
            return;
        }

        if (!timerModel.isRunning()) {
            timerModel.setDurationSeconds(totalSeconds);
            updatePresetButtonStyles(-1);
        }
    }

    @FXML private void pauseTimer() {
        if (timerModel.isPaused()) {
            timerModel.resume();
            btnPause.setText("Pause");
        } else {
            timerModel.pause();
            btnPause.setText("Resume");
        }
    }

    @FXML private void stopTimer() {
        timerModel.stop();
        btnPause.setText("Pause");
    }

    @FXML private void resetTimer() {
        timerModel.reset();
        btnPause.setText("Pause");
    }

    @FXML private void setDuration25() {
        if (!timerModel.isRunning()) {
            timerModel.setDuration(25);
            updatePresetButtonStyles(25);
        }
    }

    @FXML private void setDuration45() {
        if (!timerModel.isRunning()) {
            timerModel.setDuration(45);
            updatePresetButtonStyles(45);
        }
    }

    @FXML private void setDuration60() {
        if (!timerModel.isRunning()) {
            timerModel.setDuration(60);
            updatePresetButtonStyles(60);
        }
    }

    @FXML private void openHistoryView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/history.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/history-styles.css").toExternalForm());
            Stage stage = (Stage) timeLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Histori Belajar");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML private void addCategory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add New Category");
        dialog.setHeaderText("Create a new focus category");
        dialog.setContentText("Category name:");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/timer.css").toExternalForm());

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

    @FXML private void removeCategory() {
        String selected = categoryComboBox.getValue();
        if (selected == null) {
            showWarningAlert("No Selection", "Please select a category to remove.");
            return;
        }

        if (categoryModel.getCategories().size() <= 1) {
            showWarningAlert("Cannot Remove", "You must have at least one category.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Category");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("Remove category '" + selected + "'?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (selected.equals(categoryModel.getSelectedCategory())) {
                for (String other : categoryModel.getCategories()) {
                    if (!other.equals(selected)) {
                        categoryModel.setSelectedCategory(other);
                        categoryComboBox.setValue(other);
                        break;
                    }
                }
            }

            boolean success = categoryModel.removeCategory(selected);
            if (success) {
                showInfoAlert("Success", "Category removed successfully!");
            } else {
                showWarningAlert("Failed", "Could not remove category.");
            }
        }
    }

    private void updatePresetButtonStyles(int minutes) {
        btn25.getStyleClass().remove("preset-btn-selected");
        btn45.getStyleClass().remove("preset-btn-selected");
        btn60.getStyleClass().remove("preset-btn-selected");
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
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    private void showWarningAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    public CategoryModel getCategoryModel() {
        return categoryModel;
    }
}