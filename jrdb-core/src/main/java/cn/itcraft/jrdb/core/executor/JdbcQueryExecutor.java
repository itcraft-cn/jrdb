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
 * JDBC查询执行器实现类。
 * 
 * <p>该类实现了{@link QueryExecutor}接口，基于JDBC协议执行数据库查询和更新操作。
 * 查询结果会被存储到堆外内存中，以实现高性能的数据访问。</p>
 * 
 * <p>核心功能包括：</p>
 * <ul>
 *   <li>执行SQL查询并将结果集映射到堆外内存</li>
 *   <li>执行单条更新语句</li>
 *   <li>执行批量更新语句</li>
 *   <li>支持事务管理</li>
 *   <li>支持结果集行数限制</li>
 * </ul>
 * 
 * @see QueryExecutor
 * @see MemoryPool
 * @see RowStore
 */
public class JdbcQueryExecutor implements QueryExecutor {

    /**
     * 内存池，用于分配堆外内存块。
     */
    private final MemoryPool pool;

    /**
     * JRDB配置对象，包含框架运行时配置信息。
     */
    private final JrdbConfig config;

    /**
     * 当前绑定的事务对象，用于获取数据库连接。
     */
    private Transaction transaction;

    /**
     * 查询结果集的最大行数限制，默认为{@link Integer#MAX_VALUE}表示不限制。
     */
    private int maxRows;

    /**
     * 构造JDBC查询执行器实例。
     * 
     * @param pool 内存池，不能为null，用于分配堆外内存存储查询结果
     * @param config JRDB配置对象，不能为null
     * @throws NullPointerException 如果pool或config为null
     */
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

    /**
     * 设置当前执行器绑定的事务对象。
     * 
     * <p>事务对象用于获取数据库连接，执行SQL操作时必须先设置事务。</p>
     * 
     * @param transaction 事务对象，可为null表示解除事务绑定
     */
    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    /**
     * 设置查询结果集的最大行数限制。
     * 
     * <p>当查询结果行数超过此限制时，只会返回前maxRows行数据。</p>
     * 
     * @param maxRows 最大行数，必须为正整数
     */
    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    /**
     * 执行SQL查询操作。
     * 
     * <p>该方法执行以下步骤：</p>
     * <ol>
     *   <li>从事务获取数据库连接</li>
     *   <li>创建预编译语句并设置参数</li>
     *   <li>执行查询并读取结果集</li>
     *   <li>构建列元数据信息</li>
     *   <li>将结果数据填充到堆外内存</li>
     *   <li>返回查询结果对象</li>
     * </ol>
     * 
     * @param boundSql 绑定参数的SQL对象，包含SQL语句和参数值
     * @return 查询结果对象，包含结果数据和内存块引用
     * @throws SQLException 如果数据库访问发生错误
     */
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

    /**
     * 执行单条更新语句（INSERT、UPDATE、DELETE）。
     * 
     * @param boundSql 绑定参数的SQL对象，包含SQL语句和参数值
     * @return 受影响的行数
     * @throws SQLException 如果数据库访问发生错误
     */
    @Override
    public int update(BoundSql boundSql) throws SQLException {
        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(boundSql.getSql())) {
            setParameters(stmt, boundSql.getParams());
            return stmt.executeUpdate();
        }
    }

    /**
     * 执行批量更新操作。
     * 
     * <p>该方法依次执行多条更新语句，每条语句独立执行并返回各自的受影响行数。
     * 注意：此方法不使用JDBC的批量更新特性，而是逐条执行。</p>
     * 
     * @param boundSqls 绑定参数的SQL对象数组，每个对象包含一条更新语句
     * @return 整数数组，每个元素对应一条SQL语句的受影响行数
     * @throws SQLException 如果数据库访问发生错误
     */
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

    /**
     * 从当前绑定的事务获取数据库连接。
     * 
     * @return 数据库连接对象
     * @throws SQLException 如果没有绑定事务
     */
    private Connection getConnection() throws SQLException {
        if (transaction == null) {
            throw new SQLException("No transaction bound to executor");
        }
        return transaction.getConnection();
    }

    /**
     * 为预编译语句设置参数值。
     * 
     * <p>参数按顺序设置到预编译语句中，索引从1开始。</p>
     * 
     * @param stmt 预编译语句对象
     * @param params 参数值数组，可为null表示无参数
     * @throws SQLException 如果设置参数时发生错误
     */
    private void setParameters(PreparedStatement stmt, Object[] params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
        }
    }

    /**
     * 根据结果集元数据构建列元数据列表。
     * 
     * <p>遍历结果集的所有列，提取列名、SQL类型和预估大小，
     * 构建对应的{@link ColumnMeta}对象。</p>
     * 
     * @param metaData 结果集元数据
     * @return 列元数据列表，顺序与结果集列顺序一致
     * @throws SQLException 如果读取元数据时发生错误
     */
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

    /**
     * 根据SQL类型和显示大小估算列的存储空间大小（字节）。
     * 
     * <p>估算规则：</p>
     * <ul>
     *   <li>INTEGER: 4字节</li>
     *   <li>BIGINT、DOUBLE、FLOAT: 8字节</li>
     *   <li>BOOLEAN: 1字节</li>
     *   <li>VARCHAR、CHAR: 显示大小*2+4，最大256字节</li>
     *   <li>其他类型: 显示大小+4，最大256字节</li>
     * </ul>
     * 
     * @param sqlType SQL类型常量，来自{@link java.sql.Types}
     * @param displaySize 列的显示大小
     * @return 估算的存储空间大小（字节）
     */
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

    /**
     * 计算单行数据的总存储大小。
     * 
     * <p>将所有列的大小相加得到单行数据的预估存储空间。</p>
     * 
     * @param columns 列元数据列表
     * @return 单行数据的总字节数
     */
    private int calculateRowSize(List<ColumnMeta> columns) {
        int size = 0;
        for (ColumnMeta col : columns) {
            size += col.getSize();
        }
        return size;
    }

    /**
     * 从结果集读取多行数据。
     * 
     * <p>按指定的最大行数限制从结果集读取数据，根据列的SQL类型
     * 使用对应的getter方法获取值，以提高性能并避免类型转换问题。</p>
     * 
     * @param rs JDBC结果集
     * @param columns 列元数据列表
     * @param max 最大读取行数
     * @return 行数据列表，每行为Object数组
     * @throws SQLException 如果读取结果集时发生错误
     */
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

    /**
     * 将行数据填充到行存储对象中。
     * 
     * <p>遍历所有行和列，根据列的SQL类型调用对应的setter方法
     * 将数据写入堆外内存。对于null值，使用对应类型的默认值填充。</p>
     * 
     * @param store 行存储对象，用于管理堆外内存中的行数据
     * @param rows 行数据列表
     * @param columns 列元数据列表
     */
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