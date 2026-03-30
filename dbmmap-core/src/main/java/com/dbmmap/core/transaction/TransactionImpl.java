package com.dbmmap.core.transaction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Transaction implementation with lazy connection initialization.
 */
public class TransactionImpl implements Transaction {

    private final DataSource dataSource;
    private Connection connection;
    private boolean closed;
    private boolean committed;

    /**
     * Creates a new transaction.
     *
     * @param dataSource the data source
     * @throws NullPointerException if dataSource is null
     */
    public TransactionImpl(DataSource dataSource) {
        if (dataSource == null) {
            throw new NullPointerException("dataSource cannot be null");
        }
        this.dataSource = dataSource;
        this.closed = false;
        this.committed = false;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (closed) {
            throw new SQLException("Transaction is closed");
        }
        if (connection == null) {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
        }
        return connection;
    }

    @Override
    public void commit() throws SQLException {
        if (closed) {
            return;
        }
        if (connection != null) {
            connection.commit();
            committed = true;
            connection.close();
        }
        closed = true;
    }

    @Override
    public void rollback() throws SQLException {
        if (closed) {
            return;
        }
        if (connection != null) {
            connection.rollback();
            connection.close();
        }
        closed = true;
    }

    @Override
    public void close() throws SQLException {
        if (closed) {
            return;
        }
        if (connection != null && !committed) {
            connection.rollback();
            connection.close();
        } else if (connection != null) {
            connection.close();
        }
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}