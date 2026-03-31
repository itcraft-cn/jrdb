package cn.itcraft.dbmmap.core.cache;

import cn.itcraft.dbmmap.core.query.QueryResult;

import java.util.Map;

public class CachedQueryResultAdapter implements QueryResult {

    private final CachedQueryResult cached;
    private int currentRow = -1;

    public CachedQueryResultAdapter(CachedQueryResult cached) {
        this.cached = cached;
    }

    @Override
    public int getRowCount() {
        return cached.getRowCount();
    }

    @Override
    public int getColumnCount() {
        return cached.getColumnNames().size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return cached.getColumnNames().get(columnIndex);
    }

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

    @Override
    public String getString(String columnName) {
        Map<String, Object> row = cached.getRow(currentRow);
        if (row == null) {
            return null;
        }
        Object val = row.get(columnName.toUpperCase());
        return val != null ? val.toString() : null;
    }

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

    @Override
    public int getInt(int columnIndex) {
        String colName = cached.getColumnNames().get(columnIndex);
        return getInt(colName);
    }

    @Override
    public String getString(int columnIndex) {
        String colName = cached.getColumnNames().get(columnIndex);
        return getString(colName);
    }

    @Override
    public long getLong(int columnIndex) {
        String colName = cached.getColumnNames().get(columnIndex);
        return getLong(colName);
    }

    @Override
    public double getDouble(int columnIndex) {
        String colName = cached.getColumnNames().get(columnIndex);
        return getDouble(colName);
    }

    @Override
    public boolean next() {
        currentRow++;
        return currentRow < cached.getRowCount();
    }

    @Override
    public void reset() {
        currentRow = -1;
    }

    @Override
    public void release() {
    }

    @Override
    public void close() {
        currentRow = -1;
    }
}
