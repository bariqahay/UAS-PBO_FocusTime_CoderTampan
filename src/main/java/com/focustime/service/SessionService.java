package com.focustime.service;

import com.focustime.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SessionService implements SessionSaver {

    private Connection getConnection() throws SQLException {
        return DBConnection.connect();  // PostgreSQL connection
    }

    @Override
    public boolean save(int userId, String category, int duration, String note) {
        String sql = "INSERT INTO study_sessions (user_id, category, duration_minutes, note, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setString(2, category);
            stmt.setInt(3, duration);
            stmt.setString(4, note);
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                updateDailySummary(userId, duration);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void updateDailySummary(int userId, int minutes) {
        String sql = """
            INSERT INTO session_summary (user_id, date, total_minutes)
            VALUES (?, ?, ?)
            ON CONFLICT (user_id, date)
            DO UPDATE SET total_minutes = session_summary.total_minutes + EXCLUDED.total_minutes;
            """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
            stmt.setInt(3, minutes);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
