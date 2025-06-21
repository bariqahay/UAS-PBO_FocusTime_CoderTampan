package com.focustime.model;

import com.focustime.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SessionModel {

    public boolean saveSession(int userId, String category, int durationMinutes, String note) {
        if (durationMinutes <= 0) {
            System.err.println("❌ Invalid session duration: must be greater than 0");
            return false;
        }

        String insertSql = "INSERT INTO study_sessions (category, duration_minutes, note, user_id) VALUES (?, ?, ?, ?)";
        String upsertSql = """
            INSERT INTO session_summary (user_id, date, total_minutes)
            VALUES (?, CURRENT_DATE, ?)
            ON CONFLICT (user_id, date) DO UPDATE
            SET total_minutes = session_summary.total_minutes + EXCLUDED.total_minutes
        """;

        try (Connection conn = DBConnection.connect()) {
            conn.setAutoCommit(false);

            try (
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                PreparedStatement upsertStmt = conn.prepareStatement(upsertSql)
            ) {
                // Insert session
                insertStmt.setString(1, category);
                insertStmt.setInt(2, durationMinutes);
                insertStmt.setString(3, note == null || note.isBlank() ? null : note);
                insertStmt.setInt(4, userId);
                insertStmt.executeUpdate();

                // Upsert summary
                upsertStmt.setInt(1, userId);
                upsertStmt.setInt(2, durationMinutes);
                upsertStmt.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("❌ Error saving session or updating summary: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
            return false;
        }
    }

}
