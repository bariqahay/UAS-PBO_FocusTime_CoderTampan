package com.focustime.service;

import com.focustime.model.UserModel;
import com.focustime.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    public UserModel loginUser(String username, String plainPassword) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DBConnection.connect();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (BCrypt.checkpw(plainPassword, storedHash)) {
                    return new UserModel(
                        rs.getInt("id"),
                        rs.getString("username")
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("Login failed: " + e.getMessage());
        }
        return null;
    }

    public boolean registerUser(String username, String plainPassword) {
    String checkSql = "SELECT id FROM users WHERE username = ?";
    String insertSql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

    try (Connection conn = DBConnection.connect();
         PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

        // Cek apakah username sudah ada
        checkStmt.setString(1, username);
        ResultSet rs = checkStmt.executeQuery();
        if (rs.next()) {
            return false; // Username sudah dipakai
        }

        // Hash password dan simpan user baru
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());

        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, username);
            insertStmt.setString(2, hashedPassword);
            insertStmt.executeUpdate();
            return true;
        }

    } catch (SQLException e) {
        System.err.println("Register failed: " + e.getMessage());
        return false;
    }
}
}
