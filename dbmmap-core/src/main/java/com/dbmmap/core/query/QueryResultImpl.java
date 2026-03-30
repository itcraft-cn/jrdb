package com.dbmmap.core.query;

import com.dbmmap.core.memory.MemoryBlock;
import com.dbmmap.core.memory.MemoryPool;
import com.dbmmap.core.storage.RowStore;

import java.util.HashMap;
import java.util.Map;

public class QueryResultImpl implements QueryResult {
    
    private final RowStore store;
    private final int rowCount;
    private final MemoryBlock block;
    private final MemoryPool pool;
    private final Map<String, Integer> columnNameIndex;
    private int currentRow;
    private boolean closed;
    
    public QueryResultImpl(RowStore store, int rowCount, MemoryBlock block, MemoryPool pool) {
        if (store == null) {
            throw new NullPointerException("store cannot be null");
        }
        this.store = store;
        this.rowCount = rowCount;
        this.block = block;
        this.pool = pool;
        this.columnNameIndex = new HashMap<>();
        for (int i = 0; i < store.getColumnCount(); i++) {
            columnNameIndex.put(store.getColumnMeta(i).getName(), i);
        }
        this.currentRow = -1;
        this.closed = false;
    }
    
    @Override
    public int getRowCount() { return rowCount; }
    
    @Override
    public int getColumnCount() { return store.getColumnCount(); }
    
    @Override
    public String getColumnName(int columnIndex) {
        return store.getColumnMeta(columnIndex).getName();
    }
    
    private int getColumnIndex(String columnName) {
        Integer index = columnNameIndex.get(columnName);
        if (index == null) {
            throw new IllegalArgumentException("Unknown column name: " + columnName);
        }
        return index;
    }
    
    @Override
    public int getInt(String columnName) {
        return getInt(getColumnIndex(columnName));
    }
    
    @Override
    public String getString(String columnName) {
        return getString(getColumnIndex(columnName));
    }
    
    @Override
    public long getLong(String columnName) {
        return getLong(getColumnIndex(columnName));
    }
    
    @Override
    public double getDouble(String columnName) {
        return getDouble(getColumnIndex(columnName));
    }
    
    @Override
    public int getInt(int columnIndex) {
        return store.getInt(currentRow, columnIndex);
    }
    
    @Override
    public String getString(int columnIndex) {
        return store.getString(currentRow, columnIndex);
    }
    
    @Override
    public long getLong(int columnIndex) {
        return store.getLong(currentRow, columnIndex);
    }
    
    @Override
    public double getDouble(int columnIndex) {
        return store.getDouble(currentRow, columnIndex);
    }
    
    @Override
    public boolean next() {
        currentRow++;
        return currentRow < rowCount;
    }
    
    @Override
    public void reset() {
        currentRow = -1;
    }
    
    @Override
    public void release() {
        if (pool != null && block != null) {
            pool.release(block);
        }
    }
    
    @Override
    public void close() {
        if (closed) {
            return;
        }
        release();
        closed = true;
    }
}