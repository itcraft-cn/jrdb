package cn.itcraft.dbmmap.core.transaction;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Transaction interface for managing database transactions.
 */
public interface Transaction extends AutoCloseable {

    /**
     * Gets the database connection for this transaction.
     *
     * @return the connection
     * @throws SQLException if connection cannot be obtained
     */
    Connection getConnection() throws SQLException;

    /**
     * Commits the transaction.
     *
     * @throws SQLException if commit fails
     */
    void commit() throws SQLException;

    /**
     * Rolls back the transaction.
     *
     * @throws SQLException if rollback fails
     */
    void rollback() throws SQLException;

    /**
     * Closes the transaction, releasing resources.
     *
     * @throws SQLException if close fails
     */
    void close() throws SQLException;

    /**
     * Checks if the transaction is closed.
     *
     * @return true if closed
     */
    boolean isClosed();
}