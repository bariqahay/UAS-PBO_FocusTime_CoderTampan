package com.focustime.service;

import com.focustime.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class SessionService implements SessionSaver {

    private Connection getConnection() throws SQLException {
        return DBConnection.connect();  // pake PostgreSQL juga
    }

    @Override
    public boolean save(int userId, String category, int duration, String note) {
        String sql = "INSERT INTO sessions (user_id, category, duration_minutes, note, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setString(2, category);
            stmt.setInt(3, duration);
            stmt.setString(4, note);
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
