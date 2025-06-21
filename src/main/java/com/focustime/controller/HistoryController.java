package com.focustime.controller;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import com.focustime.model.HistoryModel;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class HistoryController implements Initializable {

    @FXML private TableView<HistoryModel> historyTable;
    @FXML private TableColumn<HistoryModel, String> colCategory;
    @FXML private TableColumn<HistoryModel, Integer> colDuration;
    @FXML private TableColumn<HistoryModel, LocalDateTime> colTimestamp;
    @FXML private TableColumn<HistoryModel, String> colNote;

    @FXML private BarChart<String, Number> barChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    private final ObservableList<HistoryModel> sessionList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup tabel
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("durationMinutes"));
        colTimestamp.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colNote.setCellValueFactory(new PropertyValueFactory<>("note"));

        loadSessionHistory();
        loadBarChartData();
    }

    private void loadSessionHistory() {
        sessionList.clear();
        String sql = "SELECT category, duration_minutes, timestamp, note FROM study_sessions ORDER BY timestamp DESC";

        try (Connection conn = DBConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String category = rs.getString("category");
                int duration = rs.getInt("duration_minutes");
                Timestamp timestamp = rs.getTimestamp("timestamp");
                String note = rs.getString("note");

                sessionList.add(new HistoryModel(
                    category, duration, timestamp.toLocalDateTime(), note
                ));
            }

            historyTable.setItems(sessionList);

        } catch (Exception e) {
            System.err.println("Gagal load histori sesi: " + e.getMessage());
        }
    }

    private void loadBarChartData() {
        Map<String, Integer> totalDurationsByDate = new TreeMap<>();

        for (HistoryModel session : sessionList) {
            String date = session.getTimestamp().toLocalDate().toString();
            totalDurationsByDate.put(
                date,
                totalDurationsByDate.getOrDefault(date, 0) + session.getDurationMinutes()
            );
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Durasi Belajar");

        for (Map.Entry<String, Integer> entry : totalDurationsByDate.entrySet()) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(entry.getKey(), entry.getValue());

            // Tooltip per batang
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

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/timer.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            
            // PENTING: Load CSS untuk timer
            String timerCss = getClass().getResource("/css/timer.css").toExternalForm();
            scene.getStylesheets().add(timerCss);
            
            Stage stage = (Stage) historyTable.getScene().getWindow();
            stage.setScene(scene);
            
        } catch (IOException e) {
            System.err.println("Gagal kembali ke halaman utama: " + e.getMessage());
        }
    }
}