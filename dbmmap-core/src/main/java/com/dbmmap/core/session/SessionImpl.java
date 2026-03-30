package com.dbmmap.core.session;

import com.dbmmap.core.config.DbMmapConfig;
import com.dbmmap.core.memory.MemoryBlock;
import com.dbmmap.core.memory.MemoryPool;
import com.dbmmap.core.query.QueryResult;
import com.dbmmap.core.query.QueryResultImpl;
import com.dbmmap.core.storage.ColumnMeta;
import com.dbmmap.core.storage.RowStore;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SessionImpl implements Session {
    
    private final MemoryPool memoryPool;
    private final DbMmapConfig config;
    private final DataSource dataSource;
    private Connection connection;
    private volatile boolean closed;
    
    public SessionImpl(MemoryPool memoryPool, DbMmapConfig config, DataSource dataSource) {
        if (memoryPool == null) {
            throw new NullPointerException("memoryPool cannot be null");
        }
        if (config == null) {
            throw new NullPointerException("config cannot be null");
        }
        if (dataSource == null) {
            throw new NullPointerException("dataSource cannot be null");
        }
        this.memoryPool = memoryPool;
        this.config = config;
        this.dataSource = dataSource;
        this.closed = false;
    }
    
    @Override
    public QueryResult query(String sql) {
        return query(sql, new Object[0]);
    }
    
    public QueryResult query(String sql, Object... params) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        
        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            setParameters(ps, params);
            
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            List<ColumnMeta> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                String name = metaData.getColumnLabel(i);
                int sqlType = metaData.getColumnType(i);
                int size = getColumnSize(sqlType);
                columns.add(new ColumnMeta(name, sqlType, size));
            }
            
            int rowSize = columns.stream().mapToInt(ColumnMeta::getSize).sum();
            int maxRows = memoryPool.getSlotSize() / Math.max(rowSize, 1);
            
            MemoryBlock block = memoryPool.allocate();
            RowStore store = new RowStore(block, columns);
            
            int rowCount = 0;
            while (rs.next() && rowCount < maxRows) {
                for (int i = 0; i < columns.size(); i++) {
                    ColumnMeta meta = columns.get(i);
                    setColumnValue(store, rs, rowCount, i, meta);
                }
                rowCount++;
            }
            
            rs.close();
            ps.close();
            
            return new QueryResultImpl(store, rowCount, block, memoryPool);
            
        } catch (SQLException e) {
            throw new RuntimeException("Query execution failed: " + sql, e);
        }
    }
    
    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = dataSource.getConnection();
        }
        return connection;
    }
    
    private void setParameters(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
    
    private int getColumnSize(int sqlType) {
        switch (sqlType) {
            case Types.INTEGER:
            case Types.DATE:
            case Types.FLOAT:
            case Types.REAL:
                return 4;
            case Types.BIGINT:
            case Types.TIMESTAMP:
            case Types.DOUBLE:
                return 8;
            case Types.BOOLEAN:
            case Types.TINYINT:
                return 1;
            case Types.SMALLINT:
                return 2;
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
                return 64;
            case Types.DECIMAL:
            case Types.NUMERIC:
                return 32;
            default:
                return 32;
        }
    }
    
    private void setColumnValue(RowStore store, ResultSet rs, int row, int col, ColumnMeta meta) 
            throws SQLException {
        int sqlType = meta.getSqlType();
        switch (sqlType) {
            case Types.INTEGER:
            case Types.TINYINT:
            case Types.SMALLINT:
                store.setInt(row, rs.getInt(col + 1), col);
                break;
            case Types.BIGINT:
                store.setLong(row, rs.getLong(col + 1), col);
                break;
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
            case Types.CLOB:
                store.setString(row, rs.getString(col + 1), col);
                break;
            case Types.DOUBLE:
            case Types.FLOAT:
                store.setDouble(row, rs.getDouble(col + 1), col);
                break;
            case Types.BOOLEAN:
                store.setInt(row, rs.getBoolean(col + 1) ? 1 : 0, col);
                break;
            case Types.DATE:
            case Types.TIMESTAMP:
                java.sql.Timestamp ts = rs.getTimestamp(col + 1);
                store.setLong(row, ts != null ? ts.getTime() : 0L, col);
                break;
            case Types.DECIMAL:
            case Types.NUMERIC:
                java.math.BigDecimal bd = rs.getBigDecimal(col + 1);
                store.setDouble(row, bd != null ? bd.doubleValue() : 0.0, col);
                break;
            default:
                String strVal = rs.getString(col + 1);
                store.setString(row, strVal, col);
        }
    }
    
    @Override
    public <T> T query(String sql, Function<QueryResult, T> handler) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        QueryResult result = query(sql);
        if (result == null) {
            return null;
        }
        try {
            return handler.apply(result);
        } finally {
            result.close();
        }
    }
    
    @Override
    public int update(String sql) {
        return update(sql, new Object[0]);
    }
    
    @Override
    public int update(String sql, Object... params) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            setParameters(ps, params);
            int result = ps.executeUpdate();
            ps.close();
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Update execution failed: " + sql, e);
        }
    }
    
    @Override
    public int[] batchUpdate(String... sqls) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        int[] results = new int[sqls.length];
        for (int i = 0; i < sqls.length; i++) {
            results[i] = update(sqls[i]);
        }
        return results;
    }
    
    @Override
    public MemoryPool getMemoryPool() {
        return memoryPool;
    }
    
    @Override
    public boolean isClosed() {
        return closed;
    }
    
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
    }
}