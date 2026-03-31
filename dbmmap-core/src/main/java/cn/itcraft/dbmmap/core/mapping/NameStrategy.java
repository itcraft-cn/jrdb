package cn.itcraft.dbmmap.core.mapping;

/**
 * Strategy interface for converting between column names and field names.
 * <p>
 * Implementations define how database column names map to Java field names
 * and vice versa. Common strategies include underscore-to-camelCase conversion.
 * </p>
 *
 * @author dbmmap
 * @since 1.0.0
 */
public interface NameStrategy {

    /**
     * Converts a database column name to a Java field name.
     * <p>
     * Example: "user_id" -> "userId"
     * </p>
     *
     * @param columnName the database column name, may be null
     * @return the corresponding Java field name, or null if input is null
     */
    String columnNameToFieldName(String columnName);

    /**
     * Converts a Java field name to a database column name.
     * <p>
     * Example: "userId" -> "user_id"
     * </p>
     *
     * @param fieldName the Java field name, may be null
     * @return the corresponding database column name, or null if input is null
     */
    String fieldNameToColumnName(String fieldName);

    /**
     * Gets the unique name of this naming strategy.
     * <p>
     * Used for identification and configuration purposes.
     * </p>
     *
     * @return the strategy name
     */
    String getName();
}