package cn.itcraft.jrdb.jdbc.cache;

import cn.itcraft.jrdb.jdbc.query.QueryResult;

import java.util.Map;

/**
 * 缓存查询结果适配器，将 {@link CachedQueryResult} 适配为 {@link QueryResult} 接口。
 *
 * <p>该类实现了游标模式，支持逐行遍历缓存中的查询结果。
 * 所有列名查找均不区分大小写。
 *
 * @author jrdb
 * @see CachedQueryResult
 * @see QueryResult
 */
public class CachedQueryResultAdapter implements QueryResult {

    private final CachedQueryResult cached;
    private int currentRow = -1;

    /**
     * 构造缓存查询结果适配器。
     *
     * @param cached 缓存的查询结果
     */
    public CachedQueryResultAdapter(CachedQueryResult cached) {
        this.cached = cached;
    }

    /**
     * 获取结果集的总行数。
     *
     * @return 总行数
     */
    @Override
    public int getRowCount() {
        return cached.getRowCount();
    }

    /**
     * 获取结果集的列数。
     *
     * @return 列数
     */
    @Override
    public int getColumnCount() {
        return cached.getColumnNames().size();
    }

    /**
     * 根据列索引获取列名。
     *
     * @param columnIndex 列索引（从0开始）
     * @return 列名
     */
    @Override
    public String getColumnName(int columnIndex) {
        return cached.getColumnNames().get(columnIndex);
    }

    /**
     * 根据列名获取当前行的整型值。
     *
     * <p>如果当前行不存在或值为null，返回0。
     * 列名查找不区分大小写。
     *
     * @param columnName 列名
     * @return 整型值，若不存在则返回0
     */
    @Override
    public int getInt(String columnName) {
        Map<String, Object> row = cached.getRow(currentRow);
        if (row == null) {
            return 0;
        }
        Object val = row.get(columnName.toUpperCase());
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }

    /**
     * 根据列名获取当前行的字符串值。
     *
     * <p>如果当前行不存在或值为null，返回null。
     * 列名查找不区分大小写。
     *
     * @param columnName 列名
     * @return 字符串值，若不存在则返回null
     */
    @Override
    public String getString(String columnName) {
        Map<String, Object> row = cached.getRow(currentRow);
        if (row == null) {
            return null;
        }
        Object val = row.get(columnName.toUpperCase());
        return val != null ? val.toString() : null;
    }

    /**
     * 根据列名获取当前行的长整型值。
     *
     * <p>如果当前行不存在或值为null，返回0。
     * 列名查找不区分大小写。
     *
     * @param columnName 列名
     * @return 长整型值，若不存在则返回0
     */
    @Override
    public long getLong(String columnName) {
        Map<String, Object> row = cached.getRow(currentRow);
        if (row == null) {
            return 0;
        }
        Object val = row.get(columnName.toUpperCase());
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return 0;
    }

    /**
     * 根据列名获取当前行的双精度浮点值。
     *
     * <p>如果当前行不存在或值为null，返回0。
     * 列名查找不区分大小写。
     *
     * @param columnName 列名
     * @return 双精度浮点值，若不存在则返回0
     */
    @Override
    public double getDouble(String columnName) {
        Map<String, Object> row = cached.getRow(currentRow);
        if (row == null) {
            return 0;
        }
        Object val = row.get(columnName.toUpperCase());
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return 0;
    }

    /**
     * 根据列索引获取当前行的整型值。
     *
     * @param columnIndex 列索引（从0开始）
     * @return 整型值
     */
    @Override
    public int getInt(int columnIndex) {
        String colName = cached.getColumnNames().get(columnIndex);
        return getInt(colName);
    }

    /**
     * 根据列索引获取当前行的字符串值。
     *
     * @param columnIndex 列索引（从0开始）
     * @return 字符串值
     */
    @Override
    public String getString(int columnIndex) {
        String colName = cached.getColumnNames().get(columnIndex);
        return getString(colName);
    }

    /**
     * 根据列索引获取当前行的长整型值。
     *
     * @param columnIndex 列索引（从0开始）
     * @return 长整型值
     */
    @Override
    public long getLong(int columnIndex) {
        String colName = cached.getColumnNames().get(columnIndex);
        return getLong(colName);
    }

    /**
     * 根据列索引获取当前行的双精度浮点值。
     *
     * @param columnIndex 列索引（从0开始）
     * @return 双精度浮点值
     */
    @Override
    public double getDouble(int columnIndex) {
        String colName = cached.getColumnNames().get(columnIndex);
        return getDouble(colName);
    }

    /**
     * 将游标移动到下一行。
     *
     * @return 如果存在下一行返回true，否则返回false
     */
    @Override
    public boolean next() {
        currentRow++;
        return currentRow < cached.getRowCount();
    }

    /**
     * 重置游标位置到初始状态（第一行之前）。
     */
    @Override
    public void reset() {
        currentRow = -1;
    }

    /**
     * 释放资源。当前实现为空操作。
     */
    @Override
    public void release() {
    }

    /**
     * 关闭结果集并重置游标位置。
     */
    @Override
    public void close() {
        currentRow = -1;
    }
}