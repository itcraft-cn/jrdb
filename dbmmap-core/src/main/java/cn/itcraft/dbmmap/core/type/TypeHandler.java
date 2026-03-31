package cn.itcraft.dbmmap.core.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Type handler for converting between Java types and JDBC types.
 *
 * @param <T> the Java type handled by this handler
 */
public interface TypeHandler<T> {

    /**
     * Gets a result from a ResultSet by column name.
     *
     * @param rs the ResultSet
     * @param columnName the column name
     * @return the result value, may be null
     * @throws SQLException if a database access error occurs
     */
    T getResult(ResultSet rs, String columnName) throws SQLException;

    /**
     * Gets a result from a ResultSet by column index.
     *
     * @param rs the ResultSet
     * @param columnIndex the column index (1-based)
     * @return the result value, may be null
     * @throws SQLException if a database access error occurs
     */
    T getResult(ResultSet rs, int columnIndex) throws SQLException;

    /**
     * Sets a parameter on a PreparedStatement.
     *
     * @param ps the PreparedStatement
     * @param i the parameter index (1-based)
     * @param parameter the parameter value, may be null
     * @throws SQLException if a database access error occurs
     */
    void setParameter(PreparedStatement ps, int i, T parameter) throws SQLException;

    /**
     * Gets the Java type handled by this handler.
     *
     * @return the Java type
     */
    Class<T> getJavaType();
}