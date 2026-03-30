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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SessionImpl implements Session {
    
    private final MemoryPool memoryPool;
    private final DbMmapConfig config;
    private final DataSource dataSource;
    private Connection connection;
    private final Map<String, PreparedStatement> statementCache;
    private final Map<String, QueryMeta> metaCache;
    private final boolean useStatementCache;
    private final boolean useMetaCache;
    private volatile boolean closed;
    
    private static class QueryMeta {
        final List<ColumnMeta> columns;
        final int rowSize;
        final int maxRows;
        
        QueryMeta(List<ColumnMeta> columns, int slotSize) {
            this.columns = columns;
            this.rowSize = columns.stream().mapToInt(ColumnMeta::getSize).sum();
            this.maxRows = slotSize / Math.max(rowSize, 1);
        }
    }
    
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
        this.useStatementCache = config.isStatementCacheEnabled();
        this.useMetaCache = config.isQueryMetaCacheEnabled();
        
        if (useStatementCache) {
            this.statementCache = createLruCache(config.getStatementCacheSize());
        } else {
            this.statementCache = null;
        }
        
        if (useMetaCache) {
            this.metaCache = createLruCache(config.getStatementCacheSize());
        } else {
            this.metaCache = null;
        }
        
        this.closed = false;
    }
    
    private <K, V> Map<K, V> createLruCache(int maxSize) {
        return new LinkedHashMap<K, V>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxSize;
            }
        };
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
            PreparedStatement ps = getOrCreateStatement(sql, conn);
            setParameters(ps, params);
            
            ResultSet rs = ps.executeQuery();
            QueryMeta meta = getOrCreateMeta(sql, rs);
            
            MemoryBlock block = memoryPool.allocate();
            RowStore store = new RowStore(block, meta.columns);
            
            int rowCount = 0;
            final int maxRows = meta.maxRows;
            final int colCount = meta.columns.size();
            final int rowSize = store.getRowSizeValue();
            
            while (rs.next() && rowCount < maxRows) {
                int rowOffset = rowCount * rowSize;
                for (int i = 0; i < colCount; i++) {
                    ColumnMeta cm = meta.columns.get(i);
                    setColumnValueUltraFast(store, rs, rowOffset, store.getColumnOffset(i), i + 1, cm.getSqlType(), cm.getSize());
                }
                rowCount++;
            }
            
            rs.close();
            
            if (!useStatementCache) {
                ps.close();
            }
            
            return new QueryResultImpl(store, rowCount, block, memoryPool);
            
        } catch (SQLException e) {
            throw new RuntimeException("Query execution failed: " + sql, e);
        }
    }
    
    private PreparedStatement getOrCreateStatement(String sql, Connection conn) throws SQLException {
        if (!useStatementCache) {
            return conn.prepareStatement(sql);
        }
        
        PreparedStatement ps = statementCache.get(sql);
        if (ps == null || ps.isClosed()) {
            ps = conn.prepareStatement(sql);
            statementCache.put(sql, ps);
        } else {
            ps.clearParameters();
        }
        return ps;
    }
    
    private QueryMeta getOrCreateMeta(String sql, ResultSet rs) throws SQLException {
        if (!useMetaCache) {
            return createQueryMeta(rs);
        }
        
        QueryMeta meta = metaCache.get(sql);
        if (meta == null) {
            meta = createQueryMeta(rs);
            metaCache.put(sql, meta);
        }
        return meta;
    }
    
    private QueryMeta createQueryMeta(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        List<ColumnMeta> columns = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            String name = metaData.getColumnLabel(i);
            int sqlType = metaData.getColumnType(i);
            int size = getColumnSize(sqlType);
            columns.add(new ColumnMeta(name, sqlType, size));
        }
        return new QueryMeta(columns, memoryPool.getSlotSize());
    }
    
    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = dataSource.getConnection();
        }
        return connection;
    }
    
    private void setParameters(PreparedStatement ps, Object[] params) throws SQLException {
        final int len = params.length;
        for (int i = 0; i < len; i++) {
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
    
    private void setColumnValueUltraFast(RowStore store, ResultSet rs, int rowOffset, int colOffset, int rsColIndex, int sqlType, int maxSize) 
            throws SQLException {
        switch (sqlType) {
            case Types.INTEGER:
            case Types.TINYINT:
            case Types.SMALLINT:
                store.setIntFast(rowOffset, colOffset, rs.getInt(rsColIndex));
                break;
            case Types.BIGINT:
                store.setLongFast(rowOffset, colOffset, rs.getLong(rsColIndex));
                break;
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
            case Types.CLOB:
                store.setStringFast(rowOffset, colOffset, rs.getString(rsColIndex), maxSize);
                break;
            case Types.DOUBLE:
            case Types.FLOAT:
                store.setDoubleFast(rowOffset, colOffset, rs.getDouble(rsColIndex));
                break;
            case Types.BOOLEAN:
                store.setIntFast(rowOffset, colOffset, rs.getBoolean(rsColIndex) ? 1 : 0);
                break;
            case Types.DATE:
            case Types.TIMESTAMP:
                java.sql.Timestamp ts = rs.getTimestamp(rsColIndex);
                store.setLongFast(rowOffset, colOffset, ts != null ? ts.getTime() : 0L);
                break;
            case Types.DECIMAL:
            case Types.NUMERIC:
                java.math.BigDecimal bd = rs.getBigDecimal(rsColIndex);
                store.setDoubleFast(rowOffset, colOffset, bd != null ? bd.doubleValue() : 0.0);
                break;
            default:
                store.setStringFast(rowOffset, colOffset, rs.getString(rsColIndex), maxSize);
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
    
    public void clearCache() {
        if (statementCache != null) {
            for (PreparedStatement ps : statementCache.values()) {
                try { ps.close(); } catch (SQLException e) { }
            }
            statementCache.clear();
        }
        if (metaCache != null) {
            metaCache.clear();
        }
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
        clearCache();
        if (connection != null) {
            try { connection.close(); } catch (SQLException e) { }
        }
        closed = true;
    }
}