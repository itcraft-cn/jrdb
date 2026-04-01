package cn.itcraft.jrdb.jdbc.query;

/**
 * 查询结果接口，提供遍历和访问查询数据的方法。
 *
 * <p>QueryResult以列式存储在堆外内存中，支持高效的随机访问和遍历。
 * 使用完毕后必须调用{@link #close()}释放资源。
 *
 * <p>使用示例：
 * <pre>{@code
 * try (QueryResult result = session.query("SELECT * FROM users")) {
 *     while (result.next()) {
 *         long id = result.getLong("ID");
 *         String name = result.getString("NAME");
 *         int age = result.getInt("AGE");
 *     }
 * }
 * }</pre>
 *
 * <p>列名匹配规则：
 * <ul>
 *   <li>不区分大小写</li>
 *   <li>支持列别名</li>
 * </ul>
 *
 * @see cn.itcraft.jrdb.jdbc.session.Session#query(String)
 */
public interface QueryResult extends AutoCloseable {

    /**
     * 获取结果集行数。
     *
     * @return 行数
     */
    int getRowCount();

    /**
     * 获取结果集列数。
     *
     * @return 列数
     */
    int getColumnCount();

    /**
     * 获取指定索引的列名。
     *
     * @param columnIndex 列索引（从1开始）
     * @return 列名
     */
    String getColumnName(int columnIndex);

    /**
     * 获取当前行指定列名的int值。
     *
     * @param columnName 列名（不区分大小写）
     * @return int值
     * @throws IllegalArgumentException 列名不存在
     */
    int getInt(String columnName);

    /**
     * 获取当前行指定列名的String值。
     *
     * @param columnName 列名（不区分大小写）
     * @return String值，可能为null
     */
    String getString(String columnName);

    /**
     * 获取当前行指定列名的long值。
     *
     * @param columnName 列名（不区分大小写）
     * @return long值
     */
    long getLong(String columnName);

    /**
     * 获取当前行指定列名的double值。
     *
     * @param columnName 列名（不区分大小写）
     * @return double值
     */
    double getDouble(String columnName);

    /**
     * 获取当前行指定列索引的int值。
     *
     * @param columnIndex 列索引（从1开始）
     * @return int值
     */
    int getInt(int columnIndex);

    /**
     * 获取当前行指定列索引的String值。
     *
     * @param columnIndex 列索引（从1开始）
     * @return String值
     */
    String getString(int columnIndex);

    /**
     * 获取当前行指定列索引的long值。
     *
     * @param columnIndex 列索引（从1开始）
     * @return long值
     */
    long getLong(int columnIndex);

    /**
     * 获取当前行指定列索引的double值。
     *
     * @param columnIndex 列索引（从1开始）
     * @return double值
     */
    double getDouble(int columnIndex);

    /**
     * 移动到下一行。
     *
     * @return true表示有下一行，false表示遍历结束
     */
    boolean next();

    /**
     * 重置游标到第一行之前。
     *
     * <p>允许重新遍历结果集。
     */
    void reset();

    /**
     * 释放内存资源。
     *
     * <p>将内存归还给内存池，但结果集对象仍可使用。
     */
    void release();

    /**
     * 关闭结果集，释放所有资源。
     *
     * <p>关闭后结果集不可再用。
     */
    void close();
}