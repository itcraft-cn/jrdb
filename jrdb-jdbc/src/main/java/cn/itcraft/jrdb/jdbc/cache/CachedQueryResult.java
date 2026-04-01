package cn.itcraft.jrdb.jdbc.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缓存查询结果类
 * 
 * <p>用于封装数据库查询的结果集，包含列名列表和行数据列表。
 * 该类是不可变的，一旦创建就不能修改其内容。</p>
 * 
 * <p>通常通过 {@link Builder} 来构建实例。</p>
 * 
 * @author jrdb
 * @since 1.0
 */
public class CachedQueryResult {

    /**
     * 列名列表
     * 
     * <p>存储查询结果集中所有列的名称，顺序与查询语句中的列顺序一致。</p>
     */
    private final List<String> columnNames;

    /**
     * 行数据列表
     * 
     * <p>存储查询结果集中的所有行数据，每一行使用 Map 表示，
     * 其中键为列名（大写形式），值为对应的列值。</p>
     */
    private final List<Map<String, Object>> rows;

    /**
     * 构造方法
     * 
     * <p>创建一个包含指定列名和行数据的缓存查询结果实例。</p>
     * 
     * @param columnNames 列名列表，不能为 null
     * @param rows 行数据列表，不能为 null
     */
    public CachedQueryResult(List<String> columnNames, List<Map<String, Object>> rows) {
        this.columnNames = columnNames;
        this.rows = rows;
    }

    /**
     * 获取列名列表
     * 
     * @return 列名列表，不会返回 null
     */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /**
     * 获取所有行数据
     * 
     * @return 行数据列表，每一行都是一个列名到值的映射，不会返回 null
     */
    public List<Map<String, Object>> getRows() {
        return rows;
    }

    /**
     * 获取行数
     * 
     * @return 结果集中的行数
     */
    public int getRowCount() {
        return rows.size();
    }

    /**
     * 根据索引获取指定行
     * 
     * <p>返回指定索引位置的行数据。如果索引超出有效范围，则返回 null。</p>
     * 
     * @param index 行索引，从 0 开始
     * @return 指定行的数据映射，如果索引无效则返回 null
     */
    public Map<String, Object> getRow(int index) {
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        return rows.get(index);
    }

    /**
     * 缓存查询结果构建器
     * 
     * <p>用于逐步构建 {@link CachedQueryResult} 实例的构建器类。
     * 支持流式 API 调用风格。</p>
     * 
     * <p>使用示例：</p>
     * <pre>{@code
     * CachedQueryResult result = new CachedQueryResult.Builder()
     *     .addColumnName("ID")
     *     .addColumnName("NAME")
     *     .newRow()
     *     .put("ID", 1)
     *     .put("NAME", "张三")
     *     .newRow()
     *     .put("ID", 2)
     *     .put("NAME", "李四")
     *     .build();
     * }</pre>
     * 
     * @author jrdb
     * @since 1.0
     */
    public static class Builder {

        /**
         * 列名列表
         */
        private final List<String> columnNames = new ArrayList<>();

        /**
         * 行数据列表
         */
        private final List<Map<String, Object>> rows = new ArrayList<>();

        /**
         * 添加列名
         * 
         * @param name 列名
         * @return 当前构建器实例，支持链式调用
         */
        public Builder addColumnName(String name) {
            columnNames.add(name);
            return this;
        }

        /**
         * 添加一行数据
         * 
         * <p>将完整的一行数据添加到结果集中。</p>
         * 
         * @param row 行数据，键为列名，值为列值
         * @return 当前构建器实例，支持链式调用
         */
        public Builder addRow(Map<String, Object> row) {
            rows.add(row);
            return this;
        }

        /**
         * 创建新行
         * 
         * <p>在结果集中添加一个新的空行，后续可通过 {@link #put(String, Object)} 
         * 方法向该行填充数据。</p>
         * 
         * @return 当前构建器实例，支持链式调用
         */
        public Builder newRow() {
            rows.add(new HashMap<>());
            return this;
        }

        /**
         * 向当前行添加列值
         * 
         * <p>将指定列的值添加到当前正在构建的行中。列名会被转换为大写形式存储。
         * 如果当前没有行（即未调用过 {@link #newRow()} 或 {@link #addRow(Map)}），
         * 则此操作不会产生任何效果。</p>
         * 
         * @param column 列名
         * @param value 列值
         * @return 当前构建器实例，支持链式调用
         */
        public Builder put(String column, Object value) {
            if (!rows.isEmpty()) {
                rows.get(rows.size() - 1).put(column.toUpperCase(), value);
            }
            return this;
        }

        /**
         * 构建缓存查询结果实例
         * 
         * @return 新建的 {@link CachedQueryResult} 实例
         */
        public CachedQueryResult build() {
            return new CachedQueryResult(columnNames, rows);
        }
    }
}