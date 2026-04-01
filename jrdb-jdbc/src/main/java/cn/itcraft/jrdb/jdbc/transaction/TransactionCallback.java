package cn.itcraft.jrdb.jdbc.transaction;

import java.sql.SQLException;

/**
 * Callback interface for transaction execution.
 */
public interface TransactionCallback {

    /**
     * Executes within a transaction context.
     *
     * @param transaction the transaction
     * @throws SQLException if database operation fails
     */
    void execute(Transaction transaction) throws SQLException;
}