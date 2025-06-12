package com.focustime.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:postgresql://ep-snowy-silence-a8l9kwog-pooler.eastus2.azure.neon.tech/focustimes?sslmode=require";
    private static final String USER = "focustimes_owner";
    private static final String PASSWORD = "npg_0wWpgkYRiG2N";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}