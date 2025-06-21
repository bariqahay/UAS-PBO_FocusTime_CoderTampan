package com.focustime.controller;

import com.focustime.model.TimerModel;
import com.focustime.model.CategoryModel;
import com.focustime.model.SessionModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TimerController implements Initializable {

    @FXML private Label timeLabel;
    @FXML private Label statusLabel;
    @FXML private Button btn25, btn45, btn60;
    @FXML private Button btnStart, btnPause, btnStop, btnReset;
    @FXML private TextField timeInputField;
    @FXML private ProgressBar progressBar;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Button btnAddCategory, btnRemoveCategory;
    @FXML private ComboBox<Integer> hourCombo;
    @FXML private ComboBox<Integer> minuteCombo;
    @FXML private ComboBox<Integer> secondCombo;

    private TimerModel timerModel;
    private CategoryModel categoryModel;
    private MediaPlayer notificationSound;
    private LocalDateTime sessionStartTime;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        timerModel = new TimerModel();
        categoryModel = new CategoryModel();
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
        hourCombo.getItems().addAll(IntStream.rangeClosed(0, 3).boxed().collect(Collectors.toList()));
        minuteCombo.getItems().addAll(IntStream.rangeClosed(0, 59).boxed().collect(Collectors.toList()));
        secondCombo.getItems().addAll(IntStream.rangeClosed(0, 59).boxed().collect(Collectors.toList()));

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
        categoryComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
            }
        });
        categoryComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(String object) { return object != null ? object : ""; }
            @Override public String fromString(String string) { return string; }
        });
    }

    private void setupBindings() {
        timeLabel.textProperty().bind(timerModel.timeDisplayProperty());
        statusLabel.textProperty().bind(
            Bindings.createStringBinding(() -> {
                String cat = categoryModel.getSelectedCategory();
                String catText = cat != null ? " - " + cat : "";
                if (timerModel.isRunning()) {
                    return (timerModel.isPaused() ? "Paused" : "Focusing...") + catText;
                } else {
                    return (timerModel.getRemainingSeconds() == timerModel.getTotalSeconds() ?
                            "Ready to Focus" : "Session Stopped") + catText;
                }
            }, timerModel.isRunningProperty(), timerModel.isPausedProperty(),
               timerModel.remainingSecondsProperty(), categoryModel.selectedCategoryProperty())
        );
        btnStart.disableProperty().bind(timerModel.isRunningProperty().and(timerModel.isPausedProperty().not()));
        btnPause.disableProperty().bind(timerModel.isRunningProperty().not());
        btnStop.disableProperty().bind(timerModel.isRunningProperty().not());
        btnRemoveCategory.disableProperty().bind(
            Bindings.createBooleanBinding(() -> categoryModel.getCategories().size() <= 1,
                categoryModel.getCategories())
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
            URL soundURL = getClass().getResource("/audio/session_end.mp3");
            if (soundURL != null) {
                Media media = new Media(soundURL.toString());
                notificationSound = new MediaPlayer(media);
                notificationSound.setVolume(0.7);
            } else {
                System.out.println("Sound file not found.");
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
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        } catch (Exception e) {
            System.out.println("Notification error: " + e.getMessage());
            try { java.awt.Toolkit.getDefaultToolkit().beep(); } catch (Exception ignored) {}
        }
    }

    private void onTimerFinished() {
        Platform.runLater(() -> {
            playNotificationSound();

            String category = categoryModel.getSelectedCategory();
            int durationMinutes = timerModel.getTotalSeconds() / 60;
            String note = "";

            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Sesi Selesai");
            dialog.setHeaderText("Sesi " + category + " selesai (" + durationMinutes + " menit)");
            dialog.setContentText("Catatan singkat:");
            dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/dialog-styles.css").toExternalForm()
            );
            dialog.getDialogPane().getStyleClass().add("custom-dialog");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) note = result.get().trim();

            SessionModel session = new SessionModel();
            session.saveSession(category, durationMinutes, note);
            showInfoAlert("Sesi Dicatat", "Sesi telah dicatat ke database.");
            btnPause.setText("Pause");
        });
    }

    @FXML private void startTimer() {
        sessionStartTime = LocalDateTime.now();
        timerModel.start();
    }

    @FXML private void setCustomTimeDropdown() {
        int h = hourCombo.getValue() != null ? hourCombo.getValue() : 0;
        int m = minuteCombo.getValue() != null ? minuteCombo.getValue() : 0;
        int s = secondCombo.getValue() != null ? secondCombo.getValue() : 0;

        int totalMinutes = h * 60 + m + (s > 0 ? 1 : 0);

        if (totalMinutes <= 0 || totalMinutes > 180) {
            showWarningAlert("Invalid Time", "Durasi total harus antara 1–180 menit.");
            return;
        }

        if (!timerModel.isRunning()) {
            timerModel.setDuration(totalMinutes);
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
    @FXML private void setDuration25() { if (!timerModel.isRunning()) timerModel.setDuration(25); }
    @FXML private void setDuration45() { if (!timerModel.isRunning()) timerModel.setDuration(45); }
    @FXML private void setDuration60() { if (!timerModel.isRunning()) timerModel.setDuration(60); }

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
            System.err.println("Gagal membuka histori: " + e.getMessage());
        }
    }

    @FXML private void addCategory() {
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

    @FXML private void removeCategory() {
        String selected = categoryComboBox.getValue();
        if (selected == null) {
            showWarningAlert("No Selection", "Please select a category to remove.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Category");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("Remove category '" + selected + "'?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (categoryModel.removeCategory(selected)) {
                if (!categoryModel.getCategories().isEmpty())
                    categoryComboBox.setValue(categoryModel.getCategories().get(0));
                showInfoAlert("Success", "Category removed successfully!");
            } else {
                showWarningAlert("Cannot Remove", "Cannot remove selected or last category.");
            }
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