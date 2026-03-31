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
 * 实体类与数据库表的映射配置。
 *
 * <p>包含Java实体类与数据库表之间映射所需的所有元数据，
 * 包括字段映射、命名策略和类型处理器等。</p>
 *
 * <p>该类采用构建器模式，支持链式调用配置字段映射：</p>
 * <pre>{@code
 * MappingConfig config = new MappingConfig(User.class, "t_user", typeHandlerRegistry)
 *     .idField("id", Long.class)
 *     .field("userName", String.class)
 *     .field("createTime", LocalDateTime.class);
 * }</pre>
 *
 * @author jrdb
 * @since 1.0.0
 */
public class MappingConfig {

    /**
     * 实体类。
     */
    private final Class<?> entityClass;

    /**
     * 数据库表名。
     */
    private final String tableName;

    /**
     * 类型处理器注册表。
     */
    private final TypeHandlerRegistry typeHandlerRegistry;

    /**
     * 命名策略，用于字段名与列名之间的转换。
     */
    private NameStrategy nameStrategy;

    /**
     * 字段映射列表，按添加顺序存储。
     */
    private final List<MappingField> fields = new ArrayList<>();

    /**
     * 字段名到映射字段的映射表。
     */
    private final Map<String, MappingField> fieldNameMap = new HashMap<>();

    /**
     * 列名到映射字段的映射表。
     */
    private final Map<String, MappingField> columnNameMap = new HashMap<>();

    /**
     * 构造映射配置。
     *
     * @param entityClass 实体类，不能为null
     * @param tableName 数据库表名，不能为null
     * @param typeHandlerRegistry 类型处理器注册表，不能为null
     * @throws IllegalArgumentException 如果任一必需参数为null
     */
    public MappingConfig(Class<?> entityClass, String tableName, TypeHandlerRegistry typeHandlerRegistry) {
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass cannot be null");
        this.tableName = Objects.requireNonNull(tableName, "tableName cannot be null");
        this.typeHandlerRegistry = Objects.requireNonNull(typeHandlerRegistry, "typeHandlerRegistry cannot be null");
        this.nameStrategy = new DefaultNameStrategy();
    }

    /**
     * 获取实体类。
     *
     * @return 实体类
     */
    public Class<?> getEntityClass() {
        return entityClass;
    }

    /**
     * 获取数据库表名。
     *
     * @return 数据库表名
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * 获取类型处理器注册表。
     *
     * @return 类型处理器注册表
     */
    public TypeHandlerRegistry getTypeHandlerRegistry() {
        return typeHandlerRegistry;
    }

    /**
     * 获取命名策略。
     *
     * @return 命名策略
     */
    public NameStrategy getNameStrategy() {
        return nameStrategy;
    }

    /**
     * 设置命名策略。
     *
     * @param nameStrategy 命名策略，不能为null
     * @throws IllegalArgumentException 如果命名策略为null
     */
    public void setNameStrategy(NameStrategy nameStrategy) {
        this.nameStrategy = Objects.requireNonNull(nameStrategy, "nameStrategy cannot be null");
    }

    /**
     * 获取所有字段映射。
     *
     * <p>返回不可修改的列表，保证外部无法直接修改内部字段列表。</p>
     *
     * @return 字段映射列表（不可修改）
     */
    public List<MappingField> getFields() {
        return Collections.unmodifiableList(fields);
    }

    /**
     * 添加字段映射（指定列名）。
     *
     * <p>构建器风格方法，返回当前实例以支持链式调用。</p>
     *
     * @param fieldName Java字段名，不能为null
     * @param columnName 数据库列名，不能为null
     * @param javaType Java类型，不能为null
     * @return 当前映射配置实例
     * @throws IllegalArgumentException 如果任一参数为null
     */
    public MappingConfig field(String fieldName, String columnName, Class<?> javaType) {
        MappingField field = new MappingField(fieldName, columnName, javaType);
        addField(field);
        return this;
    }

    /**
     * 添加字段映射（自动转换列名）。
     *
     * <p>根据配置的命名策略自动将字段名转换为列名
     * （例如：userId -> user_id）。</p>
     *
     * @param fieldName Java字段名，不能为null
     * @param javaType Java类型，不能为null
     * @return 当前映射配置实例
     * @throws IllegalArgumentException 如果任一参数为null
     */
    public MappingConfig field(String fieldName, Class<?> javaType) {
        String columnName = nameStrategy.fieldNameToColumnName(fieldName);
        return field(fieldName, columnName, javaType);
    }

    /**
     * 添加主键字段映射（自动转换列名）。
     *
     * <p>将字段标记为主键/ID字段，并根据命名策略自动转换列名。</p>
     *
     * @param fieldName Java字段名
     * @param javaType Java类型
     * @return 当前映射配置实例
     */
    public MappingConfig idField(String fieldName, Class<?> javaType) {
        String columnName = nameStrategy.fieldNameToColumnName(fieldName);
        return idField(fieldName, columnName, javaType);
    }

    /**
     * 添加主键字段映射（指定列名）。
     *
     * <p>将字段标记为主键/ID字段。</p>
     *
     * @param fieldName Java字段名
     * @param columnName 数据库列名
     * @param javaType Java类型
     * @return 当前映射配置实例
     */
    public MappingConfig idField(String fieldName, String columnName, Class<?> javaType) {
        MappingField field = new MappingField(fieldName, columnName, javaType).id();
        addField(field);
        return this;
    }

    /**
     * 根据字段名获取字段映射。
     *
     * @param fieldName Java字段名
     * @return 字段映射，如果未找到则返回null
     */
    public MappingField getFieldByFieldName(String fieldName) {
        return fieldNameMap.get(fieldName);
    }

    /**
     * 根据列名获取字段映射。
     *
     * @param columnName 数据库列名
     * @return 字段映射，如果未找到则返回null
     */
    public MappingField getFieldByColumnName(String columnName) {
        return columnNameMap.get(columnName);
    }

    /**
     * 将已存在的字段设置为主键字段。
     *
     * @param fieldName 要标记为主键的字段名
     * @throws IllegalArgumentException 如果字段未找到
     */
    public void setIdField(String fieldName) {
        MappingField field = fieldNameMap.get(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Field not found: " + fieldName);
        }
        field.id();
    }

    /**
     * 从注册表获取字段的类型处理器。
     *
     * <p>优先返回字段上设置的类型处理器，若未设置则从注册表中
     * 根据Java类型查找对应的类型处理器。</p>
     *
     * @param field 映射字段
     * @return 类型处理器，如果未找到则返回null
     */
    public TypeHandler<?> getTypeHandler(MappingField field) {
        if (field.getTypeHandler() != null) {
            return field.getTypeHandler();
        }
        return typeHandlerRegistry.getTypeHandler(field.getJavaType());
    }

    /**
     * 添加字段映射到内部集合。
     *
     * <p>将字段添加到列表，并同时维护字段名和列名的映射表。</p>
     *
     * @param field 要添加的字段映射
     */
    private void addField(MappingField field) {
        fields.add(field);
        fieldNameMap.put(field.getFieldName(), field);
        columnNameMap.put(field.getColumnName(), field);
    }

    /**
     * 返回映射配置的字符串表示。
     *
     * @return 包含实体类名、表名和字段数量的字符串
     */
    @Override
    public String toString() {
        return "MappingConfig{" +
                "entityClass=" + entityClass.getSimpleName() +
                ", tableName='" + tableName + '\'' +
                ", fields=" + fields.size() +
                '}';
    }
}