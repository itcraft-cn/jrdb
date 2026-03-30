package com.dbmmap.core.transaction;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * Manages transactions with begin, execute, and callback patterns.
 */
public class TransactionManager {

    private final DataSource dataSource;

    /**
     * Creates a new TransactionManager.
     *
     * @param dataSource the data source for connections
     * @throws NullPointerException if dataSource is null
     */
    public TransactionManager(DataSource dataSource) {
        if (dataSource == null) {
            throw new NullPointerException("dataSource cannot be null");
        }
        this.dataSource = dataSource;
    }

    /**
     * Begins a new transaction.
     *
     * @return a new Transaction instance
     */
    public Transaction beginTransaction() {
        return new TransactionImpl(dataSource);
    }

    /**
     * Executes a function within a transaction.
     * Automatically commits on success or rolls back on exception.
     *
     * @param function the function to execute
     * @param <T> the return type
     * @return the function result
     * @throws SQLException if database operation fails
     */
    public <T> T executeInTransaction(Function<Transaction, T> function) throws SQLException {
        Transaction tx = beginTransaction();
        try {
            T result = function.apply(tx);
            tx.commit();
            return result;
        } catch (Exception e) {
            tx.rollback();
            if (e instanceof SQLException) {
                throw (SQLException) e;
            }
            throw new SQLException("Transaction failed", e);
        }
    }

    /**
     * Executes a callback within a transaction.
     * Automatically commits on success or rolls back on exception.
     *
     * @param callback the callback to execute
     * @throws SQLException if database operation fails
     */
    public void runInTransaction(TransactionCallback callback) throws SQLException {
        Transaction tx = beginTransaction();
        try {
            callback.execute(tx);
            tx.commit();
        } catch (SQLException e) {
            tx.rollback();
            throw e;
        } catch (Exception e) {
            tx.rollback();
            throw new SQLException("Transaction failed", e);
        }
    }
}