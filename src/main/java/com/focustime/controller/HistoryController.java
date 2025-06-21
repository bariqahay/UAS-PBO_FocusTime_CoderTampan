package com.focustime.controller;

import com.focustime.model.HistoryModel;
import com.focustime.session.CurrentUser;
import com.focustime.util.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

public class HistoryController implements Initializable {

    @FXML private TableView<HistoryModel> historyTable;
    @FXML private TableColumn<HistoryModel, String> colCategory;
    @FXML private TableColumn<HistoryModel, Integer> colDuration;
    @FXML private TableColumn<HistoryModel, LocalDateTime> colTimestamp;
    @FXML private TableColumn<HistoryModel, String> colNote;
    @FXML private Label labelTodayTotal;
    @FXML private Label labelWeekTotal;

    @FXML private BarChart<String, Number> barChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    private final ObservableList<HistoryModel> sessionList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Mapping kolom table ke getter model
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("durationMinutes"));
        colTimestamp.setCellValueFactory(new PropertyValueFactory<>("createdAt"));  // disesuaikan dengan getter
        colNote.setCellValueFactory(new PropertyValueFactory<>("note"));

        loadSessionHistory();
        loadBarChartData();
        loadSummaryLabels();
    }

    private void loadSessionHistory() {
        sessionList.clear();
        String sql = """
            SELECT category, duration_minutes, created_at, note 
            FROM study_sessions 
            WHERE user_id = ? 
            ORDER BY created_at DESC
        """;

        try (Connection conn = DBConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, CurrentUser.get().getId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String category = rs.getString("category");
                int duration = rs.getInt("duration_minutes");
                Timestamp ts = rs.getTimestamp("created_at");
                String note = rs.getString("note");

                sessionList.add(new HistoryModel(category, duration, ts.toLocalDateTime(), note));
            }

            historyTable.setItems(sessionList);

        } catch (Exception e) {
            System.err.println("❌ Gagal load histori sesi: " + e.getMessage());
        }
    }

    private void loadBarChartData() {
        Map<String, Integer> totalDurationsByDate = new TreeMap<>();

        for (HistoryModel session : sessionList) {
            String date = session.getCreatedAt().toLocalDate().toString();
            totalDurationsByDate.merge(date, session.getDurationMinutes(), Integer::sum);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Durasi Belajar");

        for (Map.Entry<String, Integer> entry : totalDurationsByDate.entrySet()) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(entry.getKey(), entry.getValue());
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    Tooltip tooltip = new Tooltip(entry.getValue() + " menit pada " + entry.getKey());
                    Tooltip.install(newNode, tooltip);
                }
            });
            series.getData().add(data);
        }

        barChart.getData().clear();
        barChart.getData().add(series);
    }

    private void loadSummaryLabels() {
        int todayMinutes = 0;
        int weekMinutes = 0;

        String sql = """
            SELECT date, total_minutes 
            FROM session_summary 
            WHERE user_id = ? AND date >= CURRENT_DATE - INTERVAL '6 days'
        """;

        try (Connection conn = DBConnection.connect();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, CurrentUser.get().getId());
            ResultSet rs = stmt.executeQuery();

            LocalDate today = LocalDate.now();
            LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);

            while (rs.next()) {
                LocalDate date = rs.getDate("date").toLocalDate();
                int minutes = rs.getInt("total_minutes");

                if (date.equals(today)) todayMinutes += minutes;
                if (!date.isBefore(startOfWeek)) weekMinutes += minutes;
            }

            labelTodayTotal.setText("Total Hari Ini: " + todayMinutes + " menit");
            labelWeekTotal.setText("Total Minggu Ini: " + weekMinutes + " menit");

        } catch (SQLException e) {
            System.err.println("❌ Gagal load ringkasan: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/timer.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/timer.css").toExternalForm());

            Stage stage = (Stage) historyTable.getScene().getWindow();
            stage.setScene(scene);

        } catch (IOException e) {
            System.err.println("❌ Gagal kembali ke halaman utama: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        HistoryModel selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Tidak Ada Data Terpilih", "Silakan pilih baris histori yang ingin dihapus.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi Hapus");
        confirm.setHeaderText("Yakin ingin menghapus histori ini?");
        confirm.setContentText("Kategori: " + selected.getCategory() + "\nDurasi: " + selected.getDurationMinutes() + " menit\nWaktu: " + selected.getCreatedAt());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteSessionFromDB(selected);
                loadSessionHistory();  // Refresh table
                loadBarChartData();   // Refresh chart
                loadSummaryLabels();  // Refresh label ringkasan
            }
        });
    }

    private void deleteSessionFromDB(HistoryModel session) {
        String sql = "DELETE FROM study_sessions WHERE user_id = ? AND created_at = ?";

        try (Connection conn = DBConnection.connect();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, CurrentUser.get().getId());
            stmt.setTimestamp(2, Timestamp.valueOf(session.getCreatedAt()));
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Gagal menghapus sesi: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
