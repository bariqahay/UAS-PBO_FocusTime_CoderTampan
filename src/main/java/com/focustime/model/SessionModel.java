package com.focustime.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

import com.focustime.util.DBConnection;

public class SessionModel {

    public void saveSession(String category, int durationMinutes, String note) {
        String sql = "INSERT INTO study_sessions (category, duration_minutes, timestamp, note) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, category);
            stmt.setInt(2, durationMinutes);
            stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            stmt.setString(4, note);
            stmt.executeUpdate();

            System.out.println("✅ Session saved successfully.");

        } catch (Exception e) {
            System.err.println("Failed to save session: " + e.getMessage());
        }
    }
}
