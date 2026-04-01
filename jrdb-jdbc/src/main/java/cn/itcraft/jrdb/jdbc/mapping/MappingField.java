package cn.itcraft.jrdb.jdbc.mapping;

import cn.itcraft.jrdb.jdbc.type.TypeHandler;

import java.util.Objects;

/**
 * 表示Java字段与数据库列之间的映射关系。
 * <p>
 * 包含实体类中特定字段如何映射到数据库表中列的元数据信息，
 * 包括类型处理信息。
 * </p>
 *
 * @author jrdb
 * @since 1.0.0
 */
public class MappingField {

    private final String fieldName;
    private final String columnName;
    private final Class<?> javaType;
    private TypeHandler<?> typeHandler;
    private boolean id;

    /**
     * 使用指定属性构造映射字段。
     *
     * @param fieldName Java字段名称
     * @param columnName 数据库列名
     * @param javaType 字段的Java类型
     * @throws IllegalArgumentException 如果任何必需参数为null
     */
    public MappingField(String fieldName, String columnName, Class<?> javaType) {
        this.fieldName = Objects.requireNonNull(fieldName, "fieldName cannot be null");
        this.columnName = Objects.requireNonNull(columnName, "columnName cannot be null");
        this.javaType = Objects.requireNonNull(javaType, "javaType cannot be null");
        this.id = false;
        this.typeHandler = null;
    }

    /**
     * 获取Java字段名称。
     *
     * @return 字段名称
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * 获取数据库列名。
     *
     * @return 列名
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * 获取字段的Java类型。
     *
     * @return Java类型
     */
    public Class<?> getJavaType() {
        return javaType;
    }

    /**
     * 获取此字段的类型处理器。
     *
     * @return 类型处理器，如果未指定则返回null
     */
    public TypeHandler<?> getTypeHandler() {
        return typeHandler;
    }

    /**
     * 设置此字段的类型处理器。
     * <p>
     * 构建器风格方法，返回当前实例以支持链式调用。
     * </p>
     *
     * @param typeHandler 类型处理器，可以为null
     * @return 当前MappingField实例
     */
    public MappingField typeHandler(TypeHandler<?> typeHandler) {
        this.typeHandler = typeHandler;
        return this;
    }

    /**
     * 检查此字段是否为主键字段。
     *
     * @return 如果是主键字段则返回true
     */
    public boolean isId() {
        return id;
    }

    /**
     * 将此字段标记为主键字段。
     * <p>
     * 构建器风格方法，返回当前实例以支持链式调用。
     * </p>
     *
     * @return 当前MappingField实例
     */
    public MappingField id() {
        this.id = true;
        return this;
    }

    /**
     * 比较此映射字段与另一个对象是否相等。
     * <p>
     * 基于字段名称判断相等性。
     * </p>
     *
     * @param o 要比较的对象
     * @return 如果相等则返回true
     */
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

    /**
     * 返回此映射字段的哈希码。
     * <p>
     * 基于字段名称计算哈希码。
     * </p>
     *
     * @return 哈希码
     */
    @Override
    public int hashCode() {
        return Objects.hash(fieldName);
    }

    /**
     * 返回此映射字段的字符串表示。
     *
     * @return 包含字段名称、列名、Java类型和主键标识的字符串
     */
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