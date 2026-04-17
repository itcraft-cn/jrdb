package cn.itcraft.jrdb.jdbc.query;

import cn.itcraft.jrdb.jdbc.memory.MemoryBlock;
import cn.itcraft.jrdb.jdbc.memory.MemoryPool;
import cn.itcraft.jrdb.jdbc.storage.RowStore;

import java.util.HashMap;
import java.util.Map;

/**
 * 查询结果实现类。
 * 
 * <p>该类实现了 {@link QueryResult} 接口，提供对数据库查询结果的访问能力。
 * 基于堆外内存存储查询结果，支持按行遍历、按列名或索引获取数据。
 * 
 * <p>使用完毕后应调用 {@link #close()} 方法释放资源。
 */
public class QueryResultImpl implements QueryResult {

    /** 行存储对象，用于访问实际的行数据 */
    private final RowStore store;

    /** 结果集中的总行数 */
    private final int rowCount;

    /** 存储结果数据的内存块 */
    private final MemoryBlock block;

    /** 内存池，用于管理内存块的分配与释放 */
    private final MemoryPool pool;

    /** 列名到大写列名对应索引的映射 */
    private final Map<String, Integer> columnNameIndex;

    /** 当前行指针位置，初始值为 -1 */
    private int currentRow;

    /** 是否已关闭的标志 */
    private boolean closed;

    /**
     * 构造查询结果对象。
     * 
     * @param store    行存储对象，不能为 null
     * @param rowCount 结果集的行数
     * @param block    存储数据的内存块，可为 null（当 rowCount 为 0 时）
     * @param pool     内存池，可为 null（当无需释放内存块时）
     * @throws NullPointerException 如果 store 为 null
     */
    public QueryResultImpl(RowStore store, int rowCount, MemoryBlock block, MemoryPool pool) {
        if (store == null) {
            throw new NullPointerException("store cannot be null");
        }
        this.store = store;
        this.rowCount = rowCount;
        this.block = block;
        this.pool = pool;
        this.columnNameIndex = new HashMap<>();
        int columnCount = store.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            String name = store.getColumnMeta(i).getName();
            columnNameIndex.put(name.toUpperCase(), i);
        }
        this.currentRow = -1;
        this.closed = false;
    }

    /**
     * 获取结果集中的总行数。
     * 
     * @return 结果集的行数
     */
    @Override
    public int getRowCount() {
        return rowCount;
    }

    /**
     * 获取结果集中的列数。
     * 
     * @return 结果集的列数
     */
    @Override
    public int getColumnCount() {
        return store.getColumnCount();
    }

    /**
     * 根据列索引获取列名。
     * 
     * @param columnIndex 列索引，从 0 开始
     * @return 列名
     */
    @Override
    public String getColumnName(int columnIndex) {
        return store.getColumnMeta(columnIndex).getName();
    }

    /**
     * 根据列名获取列索引。
     * 
     * @param columnName 列名，不区分大小写
     * @return 列索引，从 0 开始
     * @throws IllegalArgumentException 如果列名不存在
     */
    private int getColumnIndex(String columnName) {
        Integer index = columnNameIndex.get(columnName.toUpperCase());
        if (index == null) {
            throw new IllegalArgumentException("Unknown column name: " + columnName);
        }
        return index;
    }

    /**
     * 根据列名获取当前行的整型值。
     * 
     * @param columnName 列名，不区分大小写
     * @return 当前行指定列的整型值
     * @throws IllegalArgumentException 如果列名不存在
     */
    @Override
    public int getInt(String columnName) {
        return getInt(getColumnIndex(columnName));
    }

    /**
     * 根据列名获取当前行的字符串值。
     * 
     * @param columnName 列名，不区分大小写
     * @return 当前行指定列的字符串值
     * @throws IllegalArgumentException 如果列名不存在
     */
    @Override
    public String getString(String columnName) {
        return getString(getColumnIndex(columnName));
    }

    /**
     * 根据列名获取当前行的长整型值。
     * 
     * @param columnName 列名，不区分大小写
     * @return 当前行指定列的长整型值
     * @throws IllegalArgumentException 如果列名不存在
     */
    @Override
    public long getLong(String columnName) {
        return getLong(getColumnIndex(columnName));
    }

    /**
     * 根据列名获取当前行的双精度浮点数值。
     * 
     * @param columnName 列名，不区分大小写
     * @return 当前行指定列的双精度浮点数值
     * @throws IllegalArgumentException 如果列名不存在
     */
    @Override
    public double getDouble(String columnName) {
        return getDouble(getColumnIndex(columnName));
    }

    /**
     * 根据列索引获取当前行的整型值。
     * 
     * @param columnIndex 列索引，从 1 开始
     * @return 当前行指定列的整型值
     */
    @Override
    public int getInt(int columnIndex) {
        return store.getInt(currentRow, columnIndex - 1);
    }

    /**
     * 根据列索引获取当前行的字符串值。
     * 
     * @param columnIndex 列索引，从 1 开始
     * @return 当前行指定列的字符串值
     */
    @Override
    public String getString(int columnIndex) {
        return store.getString(currentRow, columnIndex - 1);
    }

    /**
     * 根据列索引获取当前行的长整型值。
     * 
     * @param columnIndex 列索引，从 1 开始
     * @return 当前行指定列的长整型值
     */
    @Override
    public long getLong(int columnIndex) {
        return store.getLong(currentRow, columnIndex - 1);
    }

    /**
     * 根据列索引获取当前行的双精度浮点数值。
     * 
     * @param columnIndex 列索引，从 1 开始
     * @return 当前行指定列的双精度浮点数值
     */
    @Override
    public double getDouble(int columnIndex) {
        return store.getDouble(currentRow, columnIndex - 1);
    }

    /**
     * 移动到结果集的下一行。
     * 
     * <p>首次调用时将移动到第一行。
     * 应在每次获取数据前调用此方法。
     * 
     * @return 如果存在下一行返回 true，否则返回 false
     */
    @Override
    public boolean next() {
        currentRow++;
        return currentRow < rowCount;
    }

    /**
     * 重置行指针到初始位置。
     * 
     * <p>重置后可重新遍历结果集。
     */
    @Override
    public void reset() {
        currentRow = -1;
    }

    /**
     * 释放内存资源。
     * 
     * <p>将内存块归还给内存池。
     */
    @Override
    public void release() {
        if (pool != null && block != null) {
            pool.release(block);
        }
    }

    /**
     * 关闭结果集并释放资源。
     * 
     * <p>关闭后不应再访问结果集。
     * 重复调用是安全的，不会产生副作用。
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        release();
        closed = true;
    }
}