package com.dbmmap.springboot.transaction;

import com.dbmmap.core.session.Session;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DbMmapTransactionManager implements PlatformTransactionManager {

    private final DataSource dataSource;
    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    public DbMmapTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
        try {
            Connection conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            connectionHolder.set(conn);
            return new DbMmapTransactionStatus(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get transaction", e);
        }
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                conn.commit();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to commit transaction", e);
            } finally {
                closeConnection();
            }
        }
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to rollback transaction", e);
            } finally {
                closeConnection();
            }
        }
    }

    private void closeConnection() {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException e) {
            }
            connectionHolder.remove();
        }
    }

    Connection getCurrentConnection() {
        return connectionHolder.get();
    }

    private static class DbMmapTransactionStatus implements TransactionStatus {
        private final Connection connection;
        private boolean completed = false;

        DbMmapTransactionStatus(Connection connection) {
            this.connection = connection;
        }

        @Override
        public boolean isNewTransaction() {
            return true;
        }

        @Override
        public boolean hasSavepoint() {
            return false;
        }

        @Override
        public void setRollbackOnly() {
            completed = true;
        }

        @Override
        public boolean isRollbackOnly() {
            return completed;
        }

        @Override
        public void flush() {
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }

        @Override
        public Object createSavepoint() throws TransactionException {
            return null;
        }

        @Override
        public void rollbackToSavepoint(Object savepoint) throws TransactionException {
        }

        @Override
        public void releaseSavepoint(Object savepoint) throws TransactionException {
        }
    }
}