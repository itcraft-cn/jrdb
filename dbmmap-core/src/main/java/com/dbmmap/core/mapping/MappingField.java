package com.dbmmap.core.mapping;

import com.dbmmap.core.type.TypeHandler;

import java.util.Objects;

/**
 * Represents a mapping between a Java field and a database column.
 * <p>
 * Contains metadata about how a specific field in an entity class maps
 * to a column in a database table, including type handling information.
 * </p>
 *
 * @author dbmmap
 * @since 1.0.0
 */
public class MappingField {

    private final String fieldName;
    private final String columnName;
    private final Class<?> javaType;
    private TypeHandler<?> typeHandler;
    private boolean id;

    /**
     * Constructs a MappingField with the specified properties.
     *
     * @param fieldName the Java field name
     * @param columnName the database column name
     * @param javaType the Java type of the field
     * @throws IllegalArgumentException if any required parameter is null
     */
    public MappingField(String fieldName, String columnName, Class<?> javaType) {
        this.fieldName = Objects.requireNonNull(fieldName, "fieldName cannot be null");
        this.columnName = Objects.requireNonNull(columnName, "columnName cannot be null");
        this.javaType = Objects.requireNonNull(javaType, "javaType cannot be null");
        this.id = false;
        this.typeHandler = null;
    }

    /**
     * Gets the Java field name.
     *
     * @return the field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Gets the database column name.
     *
     * @return the column name
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Gets the Java type of the field.
     *
     * @return the Java type
     */
    public Class<?> getJavaType() {
        return javaType;
    }

    /**
     * Gets the type handler for this field, if any.
     *
     * @return the type handler, or null if not specified
     */
    public TypeHandler<?> getTypeHandler() {
        return typeHandler;
    }

    /**
     * Sets the type handler for this field.
     * <p>
     * Builder-style method that returns this instance for chaining.
     * </p>
     *
     * @param typeHandler the type handler, may be null
     * @return this MappingField instance
     */
    public MappingField typeHandler(TypeHandler<?> typeHandler) {
        this.typeHandler = typeHandler;
        return this;
    }

    /**
     * Checks if this field is the primary key/id field.
     *
     * @return true if this is an id field
     */
    public boolean isId() {
        return id;
    }

    /**
     * Marks this field as the primary key/id field.
     * <p>
     * Builder-style method that returns this instance for chaining.
     * </p>
     *
     * @return this MappingField instance
     */
    public MappingField id() {
        this.id = true;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MappingField that = (MappingField) o;
        return Objects.equals(fieldName, that.fieldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName);
    }

    @Override
    public String toString() {
        return "MappingField{" +
                "fieldName='" + fieldName + '\'' +
                ", columnName='" + columnName + '\'' +
                ", javaType=" + javaType.getSimpleName() +
                ", id=" + id +
                '}';
    }
}