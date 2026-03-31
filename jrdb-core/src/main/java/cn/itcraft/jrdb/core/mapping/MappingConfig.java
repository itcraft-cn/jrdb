package cn.itcraft.jrdb.core.mapping;

import cn.itcraft.jrdb.core.type.TypeHandler;
import cn.itcraft.jrdb.core.type.TypeHandlerRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for mapping an entity class to a database table.
 * <p>
 * Contains all metadata needed to map between a Java entity class
 * and a database table, including field mappings, naming strategy,
 * and type handlers.
 * </p>
 *
 * @author jrdb
 * @since 1.0.0
 */
public class MappingConfig {

    private final Class<?> entityClass;
    private final String tableName;
    private final TypeHandlerRegistry typeHandlerRegistry;
    private NameStrategy nameStrategy;

    private final List<MappingField> fields = new ArrayList<>();
    private final Map<String, MappingField> fieldNameMap = new HashMap<>();
    private final Map<String, MappingField> columnNameMap = new HashMap<>();

    /**
     * Constructs a MappingConfig with the specified properties.
     *
     * @param entityClass the entity class to map
     * @param tableName the database table name
     * @param typeHandlerRegistry the registry for type handlers
     * @throws IllegalArgumentException if any required parameter is null
     */
    public MappingConfig(Class<?> entityClass, String tableName, TypeHandlerRegistry typeHandlerRegistry) {
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass cannot be null");
        this.tableName = Objects.requireNonNull(tableName, "tableName cannot be null");
        this.typeHandlerRegistry = Objects.requireNonNull(typeHandlerRegistry, "typeHandlerRegistry cannot be null");
        this.nameStrategy = new DefaultNameStrategy();
    }

    /**
     * Gets the entity class.
     *
     * @return the entity class
     */
    public Class<?> getEntityClass() {
        return entityClass;
    }

    /**
     * Gets the table name.
     *
     * @return the database table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Gets the type handler registry.
     *
     * @return the type handler registry
     */
    public TypeHandlerRegistry getTypeHandlerRegistry() {
        return typeHandlerRegistry;
    }

    /**
     * Gets the naming strategy.
     *
     * @return the naming strategy
     */
    public NameStrategy getNameStrategy() {
        return nameStrategy;
    }

    /**
     * Sets the naming strategy.
     *
     * @param nameStrategy the naming strategy
     * @throws IllegalArgumentException if nameStrategy is null
     */
    public void setNameStrategy(NameStrategy nameStrategy) {
        this.nameStrategy = Objects.requireNonNull(nameStrategy, "nameStrategy cannot be null");
    }

    /**
     * Gets all field mappings.
     *
     * @return an unmodifiable list of field mappings
     */
    public List<MappingField> getFields() {
        return Collections.unmodifiableList(fields);
    }

    /**
     * Adds a field mapping with explicit column name.
     * <p>
     * Builder-style method that returns this instance for chaining.
     * </p>
     *
     * @param fieldName the Java field name
     * @param columnName the database column name
     * @param javaType the Java type
     * @return this MappingConfig instance
     * @throws IllegalArgumentException if any parameter is null
     */
    public MappingConfig field(String fieldName, String columnName, Class<?> javaType) {
        MappingField field = new MappingField(fieldName, columnName, javaType);
        addField(field);
        return this;
    }

    /**
     * Adds a field mapping with auto-converted column name.
     * <p>
     * The column name is automatically converted from the field name
     * using the configured naming strategy (e.g., userId -> user_id).
     * </p>
     *
     * @param fieldName the Java field name
     * @param javaType the Java type
     * @return this MappingConfig instance
     * @throws IllegalArgumentException if any parameter is null
     */
    public MappingConfig field(String fieldName, Class<?> javaType) {
        String columnName = nameStrategy.fieldNameToColumnName(fieldName);
        return field(fieldName, columnName, javaType);
    }

    /**
     * Adds an id field mapping with auto-converted column name.
     * <p>
     * Marks the field as the primary key/id field.
     * </p>
     *
     * @param fieldName the Java field name
     * @param javaType the Java type
     * @return this MappingConfig instance
     */
    public MappingConfig idField(String fieldName, Class<?> javaType) {
        String columnName = nameStrategy.fieldNameToColumnName(fieldName);
        return idField(fieldName, columnName, javaType);
    }

    /**
     * Adds an id field mapping with explicit column name.
     * <p>
     * Marks the field as the primary key/id field.
     * </p>
     *
     * @param fieldName the Java field name
     * @param columnName the database column name
     * @param javaType the Java type
     * @return this MappingConfig instance
     */
    public MappingConfig idField(String fieldName, String columnName, Class<?> javaType) {
        MappingField field = new MappingField(fieldName, columnName, javaType).id();
        addField(field);
        return this;
    }

    /**
     * Gets a field mapping by field name.
     *
     * @param fieldName the Java field name
     * @return the MappingField, or null if not found
     */
    public MappingField getFieldByFieldName(String fieldName) {
        return fieldNameMap.get(fieldName);
    }

    /**
     * Gets a field mapping by column name.
     *
     * @param columnName the database column name
     * @return the MappingField, or null if not found
     */
    public MappingField getFieldByColumnName(String columnName) {
        return columnNameMap.get(columnName);
    }

    /**
     * Sets an existing field as the id field by field name.
     *
     * @param fieldName the field name to mark as id
     * @throws IllegalArgumentException if the field is not found
     */
    public void setIdField(String fieldName) {
        MappingField field = fieldNameMap.get(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Field not found: " + fieldName);
        }
        field.id();
    }

    /**
     * Gets the type handler for a field from the registry.
     *
     * @param field the mapping field
     * @return the type handler, or null if not found
     */
    public TypeHandler<?> getTypeHandler(MappingField field) {
        if (field.getTypeHandler() != null) {
            return field.getTypeHandler();
        }
        return typeHandlerRegistry.getTypeHandler(field.getJavaType());
    }

    private void addField(MappingField field) {
        fields.add(field);
        fieldNameMap.put(field.getFieldName(), field);
        columnNameMap.put(field.getColumnName(), field);
    }

    @Override
    public String toString() {
        return "MappingConfig{" +
                "entityClass=" + entityClass.getSimpleName() +
                ", tableName='" + tableName + '\'' +
                ", fields=" + fields.size() +
                '}';
    }
}