package com.focustime.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DBConnection {
    private static final HikariDataSource ds;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://ep-snowy-silence-a8l9kwog-pooler.eastus2.azure.neon.tech/focustimes?sslmode=require");
        config.setUsername("focustimes_owner");
        config.setPassword("npg_0wWpgkYRiG2N");
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(2000); // 2 detik max wait
        config.setIdleTimeout(600000); // 10 menit
        ds = new HikariDataSource(config);
    }

    public static Connection connect() throws SQLException {
        return ds.getConnection();
    }
}
