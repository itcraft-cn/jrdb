package cn.itcraft.jrdb.r2dbc.mapping;

import java.util.ArrayList;
import java.util.List;

public class R2dbcMappingConfig {

    private final Class<?> entityClass;
    private final String tableName;
    private final List<R2dbcMappingField> fields = new ArrayList<>();

    public R2dbcMappingConfig(Class<?> entityClass, String tableName) {
        this.entityClass = entityClass;
        this.tableName = tableName;
    }

    public R2dbcMappingConfig idField(String fieldName, Class<?> fieldType) {
        return field(fieldName, fieldType);
    }

    public R2dbcMappingConfig field(String fieldName, Class<?> fieldType) {
        String columnName = camelToSnake(fieldName);
        fields.add(new R2dbcMappingField(fieldName, columnName, fieldType));
        return this;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getTableName() {
        return tableName;
    }

    public List<R2dbcMappingField> getFields() {
        return fields;
    }

    private String camelToSnake(String str) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('_');
            }
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }
}