package com.focustime.controller;

import com.focustime.model.*;
import com.focustime.service.*;
import com.focustime.session.CurrentUser;
import com.focustime.util.DBConnection;
import com.focustime.util.NotificationPlayer;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class TimerController extends BaseController implements Initializable {

    @FXML private Label timeLabel, statusLabel, labelTodayTotal, labelWeekTotal;
    @FXML private Button btnStart, btnPause, btnStop;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<Integer> hourCombo, minuteCombo, secondCombo;
    @FXML private ProgressBar progressBar;
    @FXML private Label warningLabel;

    private CategoryModel categoryModel;
    private final SessionSaver sessionSaver = new SessionService();
    private final NotificationPlayer notifier = new NotificationPlayer("/audio/session_end.mp3");
    private final TimerModel timerModel = TimerModelSingleton.getInstance(); // yang baru

    private final SimpleIntegerProperty todayMinutes = new SimpleIntegerProperty(0);
    private final SimpleIntegerProperty weekMinutes = new SimpleIntegerProperty(0);



    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (CurrentUser.get() == null) {
            System.err.println("No user. Abort.");
            return;
        }

        categoryModel = new CategoryModel(CurrentUser.get().getId());
        timerModel.setDuration(25);

        categoryComboBox.setItems(categoryModel.getCategories());
        categoryComboBox.getSelectionModel().select(categoryModel.getSelectedCategory());

        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            categoryModel.setSelectedCategory(newVal);
        });

        loadSummaryMinutes(); // ⬅ tambahin ini
        bindUI();
        setupTimerListener();
        initDurationCombos();
        setupButtonStateListeners();
    }


    private void initDurationCombos() {
        for (int i = 0; i <= 12; i++) hourCombo.getItems().add(i);
        hourCombo.setValue(0);

        for (int i = 0; i < 60; i++) {
            minuteCombo.getItems().add(i);
            secondCombo.getItems().add(i);
        }
        minuteCombo.setValue(0);
        secondCombo.setValue(0);
    }

    private void bindUI() {
        timeLabel.textProperty().bind(timerModel.timeDisplayProperty());

        statusLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            String cat = categoryModel.getSelectedCategory();
            return timerModel.isRunning()
                ? (timerModel.isPaused() ? "Paused" : "Focusing") + (cat != null ? " - " + cat : "")
                : "Ready to Focus";
        }, timerModel.isRunningProperty(), timerModel.isPausedProperty()));

        labelTodayTotal.textProperty().bind(Bindings.concat("Hari ini: ", todayMinutes.asString(), " menit"));
        labelWeekTotal.textProperty().bind(Bindings.concat("Minggu ini: ", weekMinutes.asString(), " menit"));

        progressBar.progressProperty().bind(Bindings.createDoubleBinding(() ->
            timerModel.getTotalSeconds() == 0 ? 0 :
            1.0 - (double) timerModel.getRemainingSeconds() / timerModel.getTotalSeconds(),
            timerModel.remainingSecondsProperty(), timerModel.totalSecondsProperty()));
    }

    private void setupTimerListener() {
        timerModel.remainingSecondsProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.intValue() == 0 && oldVal.intValue() > 0) {
                onSessionFinished();
            }
        });
    }

    private void setupButtonStateListeners() {
        timerModel.isRunningProperty().addListener((obs, oldVal, isRunning) -> {
            btnPause.setDisable(!isRunning);
            btnStop.setDisable(!isRunning);
            btnStart.setDisable(isRunning);
        });

        // Optional: disable pause & stop on start
        btnPause.setDisable(true);
        btnStop.setDisable(true);
    }

    @FXML private void setDuration25() { timerModel.setDuration(25); }
    @FXML private void setDuration45() { timerModel.setDuration(45); }
    @FXML private void setDuration60() { timerModel.setDuration(60); }


    @FXML
    private void setCustomTimeDropdown() {
        int h = hourCombo.getValue() != null ? hourCombo.getValue() : 0;
        int m = minuteCombo.getValue() != null ? minuteCombo.getValue() : 0;
        int s = secondCombo.getValue() != null ? secondCombo.getValue() : 0;

        int totalSeconds = h * 3600 + m * 60 + s;
        if (totalSeconds < 60) {
            warningLabel.setText("Minimal 1 menit ya bro!");
            warningLabel.setVisible(true);
            return;
        }

        warningLabel.setVisible(false);
        timerModel.setDurationSeconds(totalSeconds);
    }

    @FXML
    private void addCategory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Tambah Kategori");
        dialog.setHeaderText("Kategori Baru");
        dialog.setContentText("Nama kategori:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(category -> {
            boolean success = categoryModel.addCategory(category);
            if (success) {
                categoryComboBox.getItems().add(category);
                categoryComboBox.getSelectionModel().select(category);
            } else {
                showWarning("Gagal", "Kategori sudah ada atau tidak valid.");
            }
        });
    }

    @FXML
    private void removeCategory() {
        String selected = categoryComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Gagal", "Kategori belum dipilih.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Hapus Kategori");
        confirm.setHeaderText("Yakin mau hapus kategori \"" + selected + "\"?");
        confirm.setContentText("Data sesi terkait tidak akan terhapus.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        boolean removed = categoryModel.removeCategory(selected);
        if (removed) {
            categoryComboBox.getItems().remove(selected);
            if (!categoryComboBox.getItems().isEmpty()) {
                categoryComboBox.getSelectionModel().selectFirst();
            } else {
                categoryComboBox.getSelectionModel().clearSelection();
            }
        } else {
            showWarning("Gagal", "Kategori tidak bisa dihapus.\nMungkin tinggal satu atau sedang dipakai.");
        }
    }

        private void loadSummaryMinutes() {
        int today = 0;
        int week = 0;

        String sql = """
            SELECT date, total_minutes 
            FROM session_summary 
            WHERE user_id = ? AND date >= CURRENT_DATE - INTERVAL '6 days'
        """;

        try (var conn = DBConnection.connect();
            var stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, CurrentUser.get().getId());
            var rs = stmt.executeQuery();

            var todayDate = java.time.LocalDate.now();
            var startOfWeek = todayDate.with(java.time.DayOfWeek.MONDAY);

            while (rs.next()) {
                var date = rs.getDate("date").toLocalDate();
                int minutes = rs.getInt("total_minutes");

                if (date.equals(todayDate)) today += minutes;
                if (!date.isBefore(startOfWeek)) week += minutes;
            }

            todayMinutes.set(today);
            weekMinutes.set(week);

        } catch (Exception e) {
            System.err.println("Gagal load summary: " + e.getMessage());
        }
    }

    private void onSessionFinished() {
        Platform.runLater(() -> {
            notifier.play();

            String category = categoryModel.getSelectedCategory();
            if (category == null || category.isBlank()) {
                showWarning("Kategori kosong", "Silakan pilih kategori.");
                return;
            }

            int durationSeconds = timerModel.getTotalSeconds();
            int remainingSeconds = timerModel.getRemainingSeconds();
            int actualSeconds = durationSeconds - remainingSeconds;

            if (actualSeconds < 45) {
                showWarning("Terlalu Singkat", "Durasi sesi < 45 detik, tidak disimpan.");
                return;
            }

            int minutes = Math.max(1, actualSeconds / 60);  // Pastiin minimal 1 menit ke DB
            int seconds = actualSeconds % 60;

            todayMinutes.set(todayMinutes.get() + minutes);
            weekMinutes.set(weekMinutes.get() + minutes);

            String timeInfo = minutes > 0
                ? (seconds > 0 ? minutes + " menit " + seconds + " detik" : minutes + " menit")
                : seconds + " detik";

            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Sesi selesai");
            dialog.setHeaderText("Sesi " + category + " (" + timeInfo + ")");
            dialog.setContentText("Catatan:");

            Optional<String> result = dialog.showAndWait();
            String note = result.map(String::trim).orElse("");

            boolean saved = sessionSaver.save(CurrentUser.get().getId(), category, minutes, note);
            if (saved && sessionSaver instanceof SessionService s) {
                s.updateDailySummary(CurrentUser.get().getId(), minutes);
                showInfo("Tersimpan", "Sesi telah dicatat.");
            }

            btnPause.setText("Pause");
        });
    }



    // Timer Controls
    @FXML private void startTimer() { timerModel.start(); }

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
    private void openHistoryView() {
        switchScene("history.fxml", "Riwayat Fokus", "history-styles.css", timeLabel);
    }

    @FXML
    private void logout() {
        CurrentUser.logout();
        switchScene("login.fxml", "Login", "login.css", timeLabel);
    }
}
