package com.dbmmap.core.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Interface for mapping between entity objects and database rows.
 * <p>
 * Implementations provide bidirectional mapping between Java entity
 * objects and database result sets/rows. Implementations are typically
 * generated dynamically using ASM bytecode generation.
 * </p>
 *
 * @param <T> the entity type being mapped
 * @author dbmmap
 * @since 1.0.0
 */
public interface EntityMapper<T> {

    /**
     * Maps a ResultSet row to an entity object.
     * <p>
     * Reads data from the current row of the ResultSet and creates
     * a new entity instance with the mapped values.
     * </p>
     *
     * @param rs the ResultSet positioned at the row to map
     * @return the mapped entity object
     * @throws SQLException if a database access error occurs
     */
    T mapFromResultSet(ResultSet rs) throws SQLException;

    /**
     * Maps an entity object to an array of column values.
     * <p>
     * Extracts field values from the entity for writing to a database row.
     * The returned array contains values in the order of the defined fields.
     * </p>
     *
     * @param entity the entity to map
     * @return array of values for database columns
     */
    Object[] mapToRow(T entity);
}