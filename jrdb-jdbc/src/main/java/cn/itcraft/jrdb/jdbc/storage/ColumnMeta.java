package cn.itcraft.jrdb.jdbc.storage;

/**
 * 列元数据信息
 *
 * <p>用于描述数据库表中的列属性，包括列名、SQL类型和字段大小等信息。
 * 该类是不可变的，所有字段在构造时初始化。</p>
 *
 * @author jrdb
 * @since 1.0
 */
public class ColumnMeta {

    /**
     * 列名称
     */
    private final String name;

    /**
     * SQL类型，使用 {@link java.sql.Types} 中定义的常量
     */
    private final int sqlType;

    /**
     * 字段大小（长度或精度）
     */
    private final int size;

    /**
     * 构造列元数据对象
     *
     * @param name    列名称
     * @param sqlType SQL类型，使用 {@link java.sql.Types} 中定义的常量
     * @param size    字段大小（长度或精度）
     */
    public ColumnMeta(String name, int sqlType, int size) {
        this.name = name;
        this.sqlType = sqlType;
        this.size = size;
    }

    /**
     * 获取列名称
     *
     * @return 列名称
     */
    public String getName() { return name; }

    /**
     * 获取SQL类型
     *
     * @return SQL类型，对应 {@link java.sql.Types} 中的常量值
     */
    public int getSqlType() { return sqlType; }

    /**
     * 获取字段大小
     *
     * @return 字段大小（长度或精度）
     */
    public int getSize() { return size; }

    @Override
    public String toString() {
        return "ColumnMeta{" +
                "name='" + name + '\'' +
                ", sqlType=" + sqlType +
                ", size=" + size +
                '}';
    }
}