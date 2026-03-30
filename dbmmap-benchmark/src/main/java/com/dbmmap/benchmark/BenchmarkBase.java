package com.dbmmap.benchmark;

import org.h2.jdbcx.JdbcDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class BenchmarkBase {

    protected static JdbcDataSource dataSource;
    protected static final int DATA_SIZE = 10000;

    public static void initDatabase() throws SQLException {
        dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:mem:benchmark;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "name VARCHAR(100), " +
                "age INT, " +
                "email VARCHAR(200), " +
                "status VARCHAR(20))");

            stmt.execute("INSERT INTO users (name, age, email, status) " +
                "SELECT 'user_' || x, 20 + (x % 50), 'user_' || x || '@test.com', 'active' " +
                "FROM SYSTEM_RANGE(1, " + DATA_SIZE + ")");
        }
    }

    protected static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    protected static void closeConnection(Connection conn) {
        if (conn != null) {
            try { conn.close(); } catch (SQLException e) {}
        }
    }
}