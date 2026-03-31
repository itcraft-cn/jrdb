package com.dbmmap.core.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class JdbcHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcHelper.class);

    /**
     * 获得连接
     */
    static Connection openLink(DataSource dataSource) {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            LOGGER.warn("Failed to get connection: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get connection", e);
        }
    }

    /**
     * 关闭连接
     */
    static void closeLink(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOGGER.warn("Failed to close connection: {}", e.getMessage(), e);
            }
        }
    }

    private static void fillArgs(PreparedStatement stat, Object[] args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            stat.setObject(i + 1, args[i]);
        }
    }

    private static void execAndCommit(Connection conn, PreparedStatement stat, List<Integer> batchResult) throws
                                                                                                          SQLException {
        Arrays.stream(stat.executeBatch())
              .forEach(batchResult::add);
        conn.commit();
    }

    /**
     * batch insert,update,delete
     */
    public static List<Integer> batchModify(Connection conn, String sql, List<Object[]> argsList, int commitSize) {
        try {
            List<Integer> batchResult = new ArrayList<>(argsList.size());
            conn.setAutoCommit(false);
            PreparedStatement stat = conn.prepareStatement(sql);
            int count = 0;
            for (Object[] args : argsList) {
                fillArgs(stat, args);
                stat.addBatch();
                count++;
                if (count % commitSize == 0) {
                    execAndCommit(conn, stat, batchResult);
                }
            }
            execAndCommit(conn, stat, batchResult);
            return batchResult;
        } catch (SQLException e) {
            rollback(conn);
            LOGGER.warn("Batch modify failed: {}", e.getMessage(), e);
            throw new RuntimeException("Batch modify failed", e);
        }
    }

    private static void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException e) {
            LOGGER.warn("Failed to rollback transaction: {}", e.getMessage(), e);
        }
    }

    private JdbcHelper() {
    }
}