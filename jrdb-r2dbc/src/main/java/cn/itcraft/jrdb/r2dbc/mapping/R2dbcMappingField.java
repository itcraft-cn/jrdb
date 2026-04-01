package cn.itcraft.jrdb.r2dbc.mapping;

public class R2dbcMappingField {

    private final String fieldName;
    private final String columnName;
    private final Class<?> javaType;

    public R2dbcMappingField(String fieldName, String columnName, Class<?> javaType) {
        this.fieldName = fieldName;
        this.columnName = columnName;
        this.javaType = javaType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getColumnName() {
        return columnName;
    }

    public Class<?> getJavaType() {
        return javaType;
    }
}