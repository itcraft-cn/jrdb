package cn.itcraft.jrdb.core.executor;

import cn.itcraft.jrdb.core.sql.BoundSql;
import cn.itcraft.jrdb.core.query.QueryResult;

import java.sql.SQLException;

/**
 * Query executor interface for executing SQL queries.
 */
public interface QueryExecutor {

    /**
     * Executes a query and returns results.
     *
     * @param boundSql the bound SQL with parameters
     * @return the query result
     * @throws SQLException if execution fails
     */
    QueryResult query(BoundSql boundSql) throws SQLException;

    /**
     * Executes an update statement.
     *
     * @param boundSql the bound SQL with parameters
     * @return the number of affected rows
     * @throws SQLException if execution fails
     */
    int update(BoundSql boundSql) throws SQLException;

    /**
     * Executes batch updates.
     *
     * @param boundSqls the array of bound SQLs
     * @return array of affected rows for each statement
     * @throws SQLException if execution fails
     */
    int[] batchUpdate(BoundSql... boundSqls) throws SQLException;
}