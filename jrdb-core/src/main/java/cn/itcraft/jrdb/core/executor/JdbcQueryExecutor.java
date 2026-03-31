package cn.itcraft.jrdb.core.executor;

import cn.itcraft.jrdb.core.config.JrdbConfig;
import cn.itcraft.jrdb.core.memory.MemoryBlock;
import cn.itcraft.jrdb.core.memory.MemoryPool;
import cn.itcraft.jrdb.core.query.QueryResult;
import cn.itcraft.jrdb.core.query.QueryResultImpl;
import cn.itcraft.jrdb.core.sql.BoundSql;
import cn.itcraft.jrdb.core.storage.ColumnMeta;
import cn.itcraft.jrdb.core.storage.RowStore;
import cn.itcraft.jrdb.core.transaction.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of QueryExecutor.
 */
public class JdbcQueryExecutor implements QueryExecutor {

    private final MemoryPool pool;
    private final JrdbConfig config;
    private Transaction transaction;
    private int maxRows;

    public JdbcQueryExecutor(MemoryPool pool, JrdbConfig config) {
        if (pool == null) {
            throw new NullPointerException("pool cannot be null");
        }
        if (config == null) {
            throw new NullPointerException("config cannot be null");
        }
        this.pool = pool;
        this.config = config;
        this.maxRows = Integer.MAX_VALUE;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    @Override
    public QueryResult query(BoundSql boundSql) throws SQLException {
        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(boundSql.getSql());
             ResultSet rs = stmt.executeQuery()) {
            setParameters(stmt, boundSql.getParams());

            List<ColumnMeta> columns = buildColumnMeta(rs.getMetaData());
            List<Object[]> rows = readRows(rs, columns, maxRows);

            int rowSize = calculateRowSize(columns);
            MemoryBlock block = pool.allocate();
            RowStore store = new RowStore(block, columns);
            fillRowStore(store, rows, columns);

            return new QueryResultImpl(store, rows.size(), block, pool);
        }
    }

    @Override
    public int update(BoundSql boundSql) throws SQLException {
        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(boundSql.getSql())) {
            setParameters(stmt, boundSql.getParams());
            return stmt.executeUpdate();
        }
    }

    @Override
    public int[] batchUpdate(BoundSql... boundSqls) throws SQLException {
        Connection conn = getConnection();
        int[] results = new int[boundSqls.length];
        for (int i = 0; i < boundSqls.length; i++) {
            try (PreparedStatement stmt = conn.prepareStatement(boundSqls[i].getSql())) {
                setParameters(stmt, boundSqls[i].getParams());
                results[i] = stmt.executeUpdate();
            }
        }
        return results;
    }

    private Connection getConnection() throws SQLException {
        if (transaction == null) {
            throw new SQLException("No transaction bound to executor");
        }
        return transaction.getConnection();
    }

    private void setParameters(PreparedStatement stmt, Object[] params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
        }
    }

    private List<ColumnMeta> buildColumnMeta(ResultSetMetaData metaData) throws SQLException {
        List<ColumnMeta> columns = new ArrayList<>();
        int count = metaData.getColumnCount();
        for (int i = 1; i <= count; i++) {
            String name = metaData.getColumnName(i);
            int sqlType = metaData.getColumnType(i);
            int size = estimateSize(sqlType, metaData.getColumnDisplaySize(i));
            columns.add(new ColumnMeta(name, sqlType, size));
        }
        return columns;
    }

    private int estimateSize(int sqlType, int displaySize) {
        switch (sqlType) {
            case Types.INTEGER:
                return 4;
            case Types.BIGINT:
            case Types.DOUBLE:
            case Types.FLOAT:
                return 8;
            case Types.BOOLEAN:
                return 1;
            case Types.VARCHAR:
            case Types.CHAR:
                return Math.min(displaySize * 2 + 4, 256);
            default:
                return Math.min(displaySize + 4, 256);
        }
    }

    private int calculateRowSize(List<ColumnMeta> columns) {
        int size = 0;
        for (ColumnMeta col : columns) {
            size += col.getSize();
        }
        return size;
    }

    private List<Object[]> readRows(ResultSet rs, List<ColumnMeta> columns, int max) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        int count = 0;
        while (rs.next() && count < max) {
            Object[] row = new Object[columns.size()];
            for (int i = 0; i < columns.size(); i++) {
                int sqlType = columns.get(i).getSqlType();
                int sqlIndex = i + 1;
                switch (sqlType) {
                    case Types.INTEGER:
                        row[i] = rs.getInt(sqlIndex);
                        break;
                    case Types.BIGINT:
                        row[i] = rs.getLong(sqlIndex);
                        break;
                    case Types.DOUBLE:
                    case Types.FLOAT:
                        row[i] = rs.getDouble(sqlIndex);
                        break;
                    case Types.VARCHAR:
                    case Types.CHAR:
                    default:
                        row[i] = rs.getString(sqlIndex);
                        break;
                }
            }
            rows.add(row);
            count++;
        }
        return rows;
    }

    private void fillRowStore(RowStore store, List<Object[]> rows, List<ColumnMeta> columns) {
        for (int row = 0; row < rows.size(); row++) {
            Object[] rowData = rows.get(row);
            for (int col = 0; col < columns.size(); col++) {
                ColumnMeta meta = columns.get(col);
                Object value = rowData[col];
                switch (meta.getSqlType()) {
                    case Types.INTEGER:
                        store.setInt(row, value != null ? (Integer) value : 0, col);
                        break;
                    case Types.BIGINT:
                        store.setLong(row, value != null ? (Long) value : 0L, col);
                        break;
                    case Types.DOUBLE:
                    case Types.FLOAT:
                        store.setDouble(row, value != null ? (Double) value : 0.0, col);
                        break;
                    case Types.VARCHAR:
                    case Types.CHAR:
                        store.setString(row, value != null ? (String) value : null, col);
                        break;
                    default:
                        store.setString(row, value != null ? value.toString() : null, col);
                }
            }
        }
    }
}
