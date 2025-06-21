package com.focustime.service;

import com.focustime.model.UserModel;
import com.focustime.util.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;

public class AuthService implements Authenticator {

    private Connection getConnection() throws SQLException {
        return DBConnection.connect();  // pake koneksi PostgreSQL lo
    }

    @Override
    public UserModel login(String username, String password) {
        try (Connection conn = getConnection()) {
            String query = "SELECT id, username, password_hash, created_at, updated_at FROM users WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");

                if (BCrypt.checkpw(password, storedHash)) {
                    return new UserModel(
                        rs.getInt("id"),
                        rs.getString("username"),
                        storedHash,
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean registerUser(String username, String password) {
        try (Connection conn = getConnection()) {
            // cek username
            String checkQuery = "SELECT id FROM users WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) return false;

            String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());

            String insertQuery = "INSERT INTO users (username, password_hash, created_at, updated_at) VALUES (?, ?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, username);
            insertStmt.setString(2, passwordHash);
            insertStmt.setTimestamp(3, now);
            insertStmt.setTimestamp(4, now);

            int rows = insertStmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
