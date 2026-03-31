package com.dbmmap.core.session;

import com.dbmmap.core.cache.CachedQueryResult;
import com.dbmmap.core.cache.CachedQueryResultAdapter;
import com.dbmmap.core.cache.QueryResultCache;
import com.dbmmap.core.config.CacheLevel;
import com.dbmmap.core.config.DbMmapConfig;
import com.dbmmap.core.handler.RowHandler;
import com.dbmmap.core.handler.RowMeta;
import com.dbmmap.core.mapping.EntityMapper;
import com.dbmmap.core.memory.MemoryBlock;
import com.dbmmap.core.memory.MemoryPool;
import com.dbmmap.core.query.QueryResult;
import com.dbmmap.core.query.QueryResultImpl;
import com.dbmmap.core.storage.ColumnMeta;
import com.dbmmap.core.storage.RowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SessionImpl implements Session {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionImpl.class);

    private static final int INT_COLUMN_SIZE = 4;
    private static final int LONG_COLUMN_SIZE = 8;
    private static final int DOUBLE_COLUMN_SIZE = 8;
    private static final int BOOLEAN_COLUMN_SIZE = 1;
    private static final int SMALLINT_COLUMN_SIZE = 2;
    private static final int STRING_COLUMN_SIZE = 64;
    private static final int DECIMAL_COLUMN_SIZE = 32;
    private static final int DEFAULT_ROW_BUFFER_SIZE = 1024;

    private final MemoryPool memoryPool;
    private final DbMmapConfig config;
    private final DataSource dataSource;
    private final Map<String, PreparedStatement> statementCache;
    private final Map<String, QueryMeta> metaCache;
    private final QueryResultCache resultCache;
    private final CacheLevel cacheLevel;
    private final QueryAction queryAction;
    private final ThreadLocal<byte[]> rowBuffer;
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
        this.cacheLevel = config.getCacheLevel();

        if (cacheLevel == CacheLevel.NONE) {
            this.statementCache = null;
            this.metaCache = null;
        } else {
            this.statementCache = createLruCache(config.getStatementCacheSize());
            this.metaCache = createLruCache(config.getStatementCacheSize());
        }

        if (cacheLevel == CacheLevel.L2) {
            this.resultCache = new QueryResultCache(
                    config.getResultCacheSize(),
                    config.getResultCacheExpireSeconds()
            );
            this.queryAction = this::cachedQuery;
        } else {
            this.resultCache = null;
            this.queryAction = this::directQuery;
        }

        this.rowBuffer = ThreadLocal.withInitial(() -> new byte[DEFAULT_ROW_BUFFER_SIZE]);
        this.closed = false;
    }

    private <K, V> Map<K, V> createLruCache(int maxSize) {
        return Collections.synchronizedMap(new LinkedHashMap<K, V>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxSize;
            }
        });
    }

    @Override
    public QueryResult query(String sql) {
        return query(sql, new Object[0]);
    }

    @Override
    public QueryResult query(String sql, Object... params) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        return queryAction.query(sql, params);
    }

    public QueryResult cachedQuery(String sql, Object... params) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        CachedQueryResult cached = resultCache.get(sql, params);
        if (cached != null) {
            return new CachedQueryResultAdapter(cached);
        }
        QueryResult result = executeQuery(sql, params);
        cached = copyResult(result);
        resultCache.put(sql, params, cached);
        result.close();
        return new CachedQueryResultAdapter(cached);
    }

    public QueryResult directQuery(String sql, Object... params) {
        return executeQuery(sql, params);
    }

    private QueryResult executeQuery(String sql, Object... params) {
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
                    setColumnValueUltraFast(store, rs, rowOffset, store.getColumnOffset(i), i + 1, cm.getSqlType(),
                                            cm.getSize());
                }
                rowCount++;
            }

            rs.close();

            if (cacheLevel == CacheLevel.NONE) {
                ps.close();
            }

            return new QueryResultImpl(store, rowCount, block, memoryPool);

        } catch (SQLException e) {
            throw new RuntimeException("Query execution failed: " + sql, e);
        }
    }

    private CachedQueryResult copyResult(QueryResult result) {
        CachedQueryResult.Builder builder = new CachedQueryResult.Builder();

        int colCount = result.getColumnCount();
        for (int i = 0; i < colCount; i++) {
            builder.addColumnName(result.getColumnName(i));
        }

        while (result.next()) {
            builder.newRow();
            for (int i = 0; i < colCount; i++) {
                String colName = result.getColumnName(i);
                builder.put(colName, getColumnValue(result, i));
            }
        }

        return builder.build();
    }

    private Object getColumnValue(QueryResult result, int columnIndex) {
        String colName = result.getColumnName(columnIndex);
        try {
            return result.getLong(colName);
        } catch (Exception e1) {
            try {
                return result.getDouble(colName);
            } catch (Exception e2) {
                return result.getString(colName);
            }
        }
    }

    private PreparedStatement getOrCreateStatement(String sql, Connection conn) throws SQLException {
        if (cacheLevel == CacheLevel.NONE) {
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
        if (cacheLevel == CacheLevel.NONE) {
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
                return INT_COLUMN_SIZE;
            case Types.BIGINT:
            case Types.TIMESTAMP:
            case Types.DOUBLE:
                return LONG_COLUMN_SIZE;
            case Types.BOOLEAN:
            case Types.TINYINT:
                return BOOLEAN_COLUMN_SIZE;
            case Types.SMALLINT:
                return SMALLINT_COLUMN_SIZE;
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
                return STRING_COLUMN_SIZE;
            case Types.DECIMAL:
            case Types.NUMERIC:
            default:
                return DECIMAL_COLUMN_SIZE;
        }
    }

    private void setColumnValueUltraFast(RowStore store, ResultSet rs, int rowOffset, int colOffset, int rsColIndex,
                                         int sqlType, int maxSize)
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
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
            case Types.CLOB:
            default:
                store.setStringFast(rowOffset, colOffset, rs.getString(rsColIndex), maxSize);
                break;
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
    public void queryWithHandler(String sql, RowHandler handler) {
        queryWithHandler(sql, handler, new Object[0]);
    }

    @Override
    public void queryWithHandler(String sql, RowHandler handler, Object... params) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        if (handler == null) {
            throw new NullPointerException("handler cannot be null");
        }

        try {
            Connection conn = getConnection();
            PreparedStatement ps = getOrCreateStatement(sql, conn);
            setParameters(ps, params);

            ResultSet rs = ps.executeQuery();
            QueryMeta queryMeta = createQueryMeta(rs);

            RowMeta rowMeta = new RowMeta(queryMeta.columns);
            int rowSize = rowMeta.getRowSize();
            int colCount = queryMeta.columns.size();
            byte[] rawRow = getOrCreateBuffer(rowSize);

            while (rs.next()) {
                readRowToBytes(rs, rawRow, queryMeta.columns, colCount);
                handler.onNextRow(rawRow, rowMeta);
            }

            rs.close();
            if (cacheLevel == CacheLevel.NONE) {
                ps.close();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Query with handler failed: " + sql, e);
        }
    }

    private byte[] getOrCreateBuffer(int requiredSize) {
        byte[] buffer = rowBuffer.get();
        if (buffer.length < requiredSize) {
            buffer = new byte[requiredSize];
            rowBuffer.set(buffer);
        }
        return buffer;
    }

    private void readRowToBytes(ResultSet rs, byte[] rawRow, List<ColumnMeta> columns, int colCount) throws SQLException {
        int offset = 0;
        for (int i = 0; i < colCount; i++) {
            ColumnMeta cm = columns.get(i);
            int rsColIndex = i + 1;
            readColumnToBytes(rs, rawRow, offset, rsColIndex, cm.getSqlType(), cm.getSize());
            offset += cm.getSize();
        }
    }

    private void readColumnToBytes(ResultSet rs, byte[] rawRow, int offset, int rsColIndex, int sqlType, int maxSize) throws SQLException {
        switch (sqlType) {
            case Types.INTEGER:
            case Types.TINYINT:
            case Types.SMALLINT:
                writeInt(rawRow, offset, rs.getInt(rsColIndex));
                break;
            case Types.BIGINT:
                writeLong(rawRow, offset, rs.getLong(rsColIndex));
                break;
            case Types.TIMESTAMP:
            case Types.DATE:
                java.sql.Timestamp ts = rs.getTimestamp(rsColIndex);
                writeLong(rawRow, offset, ts != null ? ts.getTime() : 0L);
                break;
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.DECIMAL:
            case Types.NUMERIC:
                writeDouble(rawRow, offset, rs.getDouble(rsColIndex));
                break;
            case Types.BOOLEAN:
                rawRow[offset] = (byte) (rs.getBoolean(rsColIndex) ? 1 : 0);
                break;
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
            case Types.CLOB:
            default:
                String str = rs.getString(rsColIndex);
                if (str != null) {
                    byte[] strBytes = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    int len = Math.min(strBytes.length, maxSize - 4);
                    writeInt(rawRow, offset, len);
                    System.arraycopy(strBytes, 0, rawRow, offset + 4, len);
                } else {
                    writeInt(rawRow, offset, 0);
                }
                break;
        }
    }

    private void writeInt(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value >> 24);
        buf[offset + 1] = (byte) (value >> 16);
        buf[offset + 2] = (byte) (value >> 8);
        buf[offset + 3] = (byte) value;
    }

    private void writeLong(byte[] buf, int offset, long value) {
        buf[offset] = (byte) (value >> 56);
        buf[offset + 1] = (byte) (value >> 48);
        buf[offset + 2] = (byte) (value >> 40);
        buf[offset + 3] = (byte) (value >> 32);
        buf[offset + 4] = (byte) (value >> 24);
        buf[offset + 5] = (byte) (value >> 16);
        buf[offset + 6] = (byte) (value >> 8);
        buf[offset + 7] = (byte) value;
    }

    private void writeDouble(byte[] buf, int offset, double value) {
        writeLong(buf, offset, Double.doubleToLongBits(value));
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
    public int[] batchExecute(String sql, List<Object[]> paramsList) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        if (paramsList == null || paramsList.isEmpty()) {
            return new int[0];
        }

        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            for (Object[] params : paramsList) {
                setParameters(ps, params);
                ps.addBatch();
            }

            int[] results = ps.executeBatch();
            ps.close();
            return results;

        } catch (SQLException e) {
            throw new RuntimeException("Batch execution failed: " + sql, e);
        }
    }

    @Override
    public <T> int[] batchExecute(String sql, List<T> entities, EntityMapper<T> mapper) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        if (entities == null || entities.isEmpty()) {
            return new int[0];
        }
        if (mapper == null) {
            throw new NullPointerException("mapper cannot be null");
        }

        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            for (T entity : entities) {
                Object[] params = mapper.mapToRow(entity);
                setParameters(ps, params);
                ps.addBatch();
            }

            int[] results = ps.executeBatch();
            ps.close();
            return results;

        } catch (SQLException e) {
            throw new RuntimeException("Batch execution failed: " + sql, e);
        }
    }

    public void clearCache() {
        if (statementCache != null) {
            for (PreparedStatement ps : statementCache.values()) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    LOGGER.warn("Failed to close PreparedStatement: {}", e.getMessage());
                }
            }
            statementCache.clear();
        }
        if (metaCache != null) {
            metaCache.clear();
        }
        if (resultCache != null) {
            resultCache.invalidateAll();
        }
    }

    public void invalidateResultCache() {
        if (resultCache != null) {
            resultCache.invalidateAll();
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
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.warn("Failed to close connection: {}", e.getMessage());
            }
        }
        closed = true;
    }

    @FunctionalInterface
    private interface QueryAction {
        QueryResult query(String sql, Object... params);
    }

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
}
