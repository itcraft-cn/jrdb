package cn.itcraft.jrdb.jdbc.dao;

import java.util.Objects;

public class AutoCrudMeta {
    private final Class<?> entityClass;
    private final String primaryKey;
    private final String tableName;

    public AutoCrudMeta(Class<?> entityClass, String primaryKey, String tableName) {
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass cannot be null");
        this.primaryKey = Objects.requireNonNull(primaryKey, "primaryKey cannot be null");
        this.tableName = Objects.requireNonNull(tableName, "tableName cannot be null");
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public String getTableName() {
        return tableName;
    }
}