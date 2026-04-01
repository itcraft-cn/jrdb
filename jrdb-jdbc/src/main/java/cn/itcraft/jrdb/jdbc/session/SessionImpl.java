package cn.itcraft.jrdb.jdbc.session;

import cn.itcraft.jrdb.jdbc.cache.CachedQueryResult;
import cn.itcraft.jrdb.jdbc.cache.CachedQueryResultAdapter;
import cn.itcraft.jrdb.jdbc.cache.QueryResultCache;
import cn.itcraft.jrdb.jdbc.config.JrdbConfig;
import cn.itcraft.jrdb.jdbc.executor.ErrorOverflowHandler;
import cn.itcraft.jrdb.jdbc.executor.OverflowHandler;
import cn.itcraft.jrdb.jdbc.executor.TruncateOverflowHandler;
import cn.itcraft.jrdb.jdbc.handler.RowHandler;
import cn.itcraft.jrdb.jdbc.handler.RowMeta;
import cn.itcraft.jrdb.jdbc.mapping.EntityMapper;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.memory.MemoryBlock;
import cn.itcraft.jrdb.jdbc.memory.MemoryPool;
import cn.itcraft.jrdb.jdbc.query.QueryResult;
import cn.itcraft.jrdb.jdbc.query.QueryResultImpl;
import cn.itcraft.jrdb.jdbc.storage.ColumnMeta;
import cn.itcraft.jrdb.jdbc.storage.RowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Session接口的默认实现类。
 *
 * <p>SessionImpl是jrdb框架的核心会话实现，负责管理数据库连接、执行SQL查询、
 * 处理结果集映射以及缓存管理。该实现采用堆外内存存储查询结果，通过零拷贝技术
 * 实现高性能数据访问。
 *
 * <p>主要特性：
 * <ul>
 *   <li>基于堆外内存的结果集存储，零GC压力</li>
 *   <li>多级缓存支持（L1语句缓存、L2结果缓存）</li>
 *   <li>流式行处理支持，适合大数据量场景</li>
 *   <li>批处理操作优化</li>
 * </ul>
 *
 * <p>线程安全性：此类不是线程安全的，每个线程应使用独立的Session实例。
 *
 * @see Session
 * @see MemoryPool
 * @see QueryResult
 */
public class SessionImpl implements Session {

    /**
     * 日志记录器，用于记录警告和错误信息。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionImpl.class);

    /**
     * INT类型列的字节大小（4字节）。
     */
    private static final int INT_COLUMN_SIZE = 4;

    /**
     * LONG类型列的字节大小（8字节）。
     */
    private static final int LONG_COLUMN_SIZE = 8;

    /**
     * DOUBLE类型列的字节大小（8字节）。
     */
    private static final int DOUBLE_COLUMN_SIZE = 8;

    /**
     * BOOLEAN类型列的字节大小（1字节）。
     */
    private static final int BOOLEAN_COLUMN_SIZE = 1;

    /**
     * SMALLINT类型列的字节大小（2字节）。
     */
    private static final int SMALLINT_COLUMN_SIZE = 2;

    /**
     * 字符串类型列的默认字节大小（64字节）。
     */
    private static final int STRING_COLUMN_SIZE = 64;

    /**
     * DECIMAL类型列的默认字节大小（32字节）。
     */
    private static final int DECIMAL_COLUMN_SIZE = 32;

    /**
     * 行缓冲区的默认大小（1024字节）。
     */
    private static final int DEFAULT_ROW_BUFFER_SIZE = 1024;

    /**
     * 批量操作的默认批次大小。
     */
    private static final int DEFAULT_BATCH_SIZE = 1000;

    /**
     * 内存池，用于分配堆外内存块。
     */
    private final MemoryPool memoryPool;

    /**
     * jrdb配置对象，包含缓存级别、缓存大小等配置信息。
     */
    private final JrdbConfig config;

    /**
     * 数据源，用于获取数据库连接。
     */
    private final DataSource dataSource;

    /**
     * PreparedStatement缓存，以SQL语句为键缓存编译后的语句对象。
     * 当缓存级别为NONE时为null。
     */
    private final Map<String, PreparedStatement> statementCache;

    /**
     * 查询元数据缓存，以SQL语句为键缓存列元数据信息。
     * 当缓存级别为NONE时为null。
     */
    private final Map<String, QueryMeta> metaCache;

    /**
     * 查询结果缓存，用于缓存完整的查询结果集。
     * 当缓存级别不为L2时为null。
     */
    private final QueryResultCache resultCache;

    /**
     * 查询执行策略，根据缓存级别选择直接查询或缓存查询。
     */
    private final QueryAction queryAction;

    /**
     * 实体映射器注册表，用于获取ASM生成的映射器。
     */
    private final MapperRegistry mapperRegistry;

    /**
     * 溢出处理器，根据配置决定超限时的行为。
     */
    private final OverflowHandler overflowHandler;

    /**
     * 线程本地行缓冲区，用于流式行处理时存储单行数据。
     */
    private final ThreadLocal<byte[]> rowBuffer;

    /**
     * 当前会话的数据库连接。
     */
    private Connection connection;

    /**
     * 会话关闭标志，true表示会话已关闭。
     */
    private volatile boolean closed;

    /**
     * 构造一个新的SessionImpl实例。
     *
     * <p>根据配置的缓存级别初始化相应的缓存组件：
     * <ul>
     *   <li>NONE：不使用任何缓存</li>
     *   <li>L1：使用语句缓存和元数据缓存</li>
     *   <li>L2：在L1基础上增加结果缓存</li>
     * </ul>
     *
     * @param memoryPool     内存池，用于分配堆外内存，不能为null
     * @param config         jrdb配置对象，不能为null
     * @param dataSource     数据源，用于获取数据库连接，不能为null
     * @param mapperRegistry 实体映射器注册表，不能为null
     * @throws NullPointerException 如果任一参数为null
     */
    public SessionImpl(MemoryPool memoryPool, JrdbConfig config, 
                       DataSource dataSource, MapperRegistry mapperRegistry) {
        if (memoryPool == null) {
            throw new NullPointerException("memoryPool cannot be null");
        }
        if (config == null) {
            throw new NullPointerException("config cannot be null");
        }
        if (dataSource == null) {
            throw new NullPointerException("dataSource cannot be null");
        }
        if (mapperRegistry == null) {
            throw new NullPointerException("mapperRegistry cannot be null");
        }
        this.memoryPool = memoryPool;
        this.config = config;
        this.dataSource = dataSource;
        this.mapperRegistry = mapperRegistry;

        if (config.isL1CacheEnabled()) {
            this.statementCache = createLruCache(config.getStatementCacheSize());
            this.metaCache = createLruCache(config.getStatementCacheSize());
        } else {
            this.statementCache = null;
            this.metaCache = null;
        }

        if (config.isL2CacheEnabled()) {
            this.resultCache = new QueryResultCache(
                    config.getResultCacheSize(),
                    config.getResultCacheExpireSeconds()
            );
            this.queryAction = this::cachedQuery;
        } else {
            this.resultCache = null;
            this.queryAction = this::directQuery;
        }

        this.overflowHandler = createOverflowHandler(config);

        this.rowBuffer = ThreadLocal.withInitial(() -> new byte[DEFAULT_ROW_BUFFER_SIZE]);
        this.closed = false;
    }

    private OverflowHandler createOverflowHandler(JrdbConfig config) {
        switch (config.getOverflowStrategy()) {
            case TRUNCATE:
                return new TruncateOverflowHandler();
            case ERROR:
            default:
                return new ErrorOverflowHandler();
        }
    }

    /**
     * 创建一个LRU（最近最少使用）缓存。
     *
     * <p>使用LinkedHashMap的accessOrder模式实现LRU淘汰策略，
     * 当缓存大小超过maxSize时，自动移除最久未访问的条目。
     *
     * @param <K>     缓存键类型
     * @param <V>     缓存值类型
     * @param maxSize 缓存最大容量
     * @return 线程安全的LRU缓存Map
     */
    private <K, V> Map<K, V> createLruCache(int maxSize) {
        return Collections.synchronizedMap(new LinkedHashMap<K, V>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxSize;
            }
        });
    }

/**
     * {@inheritDoc}
     *
     * <p>执行带参数的SQL查询。根据缓存级别选择直接查询或缓存查询策略。
     * 查询结果存储在堆外内存中，支持零拷贝访问。
     *
     * @param sql    要执行的SQL查询语句
     * @param params SQL参数，按顺序替换SQL中的占位符
     * @return 查询结果，包含结果集数据和元数据
     * @throws IllegalStateException 如果会话已关闭
     */
    @Override
    public QueryResult query(String sql, Object... params) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        return queryAction.query(sql, params);
    }

    @Override
    public <T> T queryForObject(String sql, Class<T> entityClass, Object... params) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        if (entityClass == null) {
            throw new NullPointerException("entityClass cannot be null");
        }

        EntityMapper<T> mapper = mapperRegistry.getMapper(entityClass);

        try {
            Connection conn = getConnection();
            PreparedStatement ps = getOrCreateStatement(sql, conn);
            setParameters(ps, params);

            ResultSet rs = ps.executeQuery();
            T result = null;
            if (rs.next()) {
                result = mapper.mapFromResultSet(rs);
            }
            rs.close();

            if (!config.isL1CacheEnabled()) {
                ps.close();
            }

            return result;

        } catch (SQLException e) {
            throw new RuntimeException("Query for object failed: " + sql, e);
        }
    }

    @Override
    public <T> List<T> queryForList(String sql, Class<T> entityClass, Object... params) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        if (entityClass == null) {
            throw new NullPointerException("entityClass cannot be null");
        }

        EntityMapper<T> mapper = mapperRegistry.getMapper(entityClass);
        List<T> results = new ArrayList<>();

        try {
            Connection conn = getConnection();
            PreparedStatement ps = getOrCreateStatement(sql, conn);
            setParameters(ps, params);

            ResultSet rs = ps.executeQuery();
            QueryMeta meta = createQueryMeta(rs);
            int maxRows = meta.maxRows;
            int rowCount = 0;

            while (rs.next() && overflowHandler.shouldContinue(rowCount, maxRows)) {
                results.add(mapper.mapFromResultSet(rs));
                rowCount++;
            }

            if (rowCount >= maxRows) {
                overflowHandler.onOverflow(maxRows);
            }

            rs.close();

            if (!config.isL1CacheEnabled()) {
                ps.close();
            }

            return results;

        } catch (SQLException e) {
            throw new RuntimeException("Query for list failed: " + sql, e);
        }
    }

    /**
     * 执行带缓存的查询。
     *
     * <p>首先检查结果缓存中是否存在相同的查询结果，如果存在则直接返回缓存结果。
     * 否则执行数据库查询，将结果复制到缓存后返回。
     *
     * <p>缓存查询的特点：
     * <ul>
     *   <li>缓存命中时无需访问数据库，响应极快</li>
     *   <li>缓存未命中时会执行查询并将结果复制到堆内存</li>
     *   <li>适合读多写少、数据变化不频繁的场景</li>
     * </ul>
     *
     * @param sql    要执行的SQL查询语句
     * @param params SQL参数数组
     * @return 查询结果，可能是缓存结果或新查询结果
     * @throws IllegalStateException 如果会话已关闭
     */
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

    /**
     * 执行直接查询，不使用结果缓存。
     *
     * <p>直接执行SQL查询并将结果存储在堆外内存中，适合实时性要求高、
     * 数据变化频繁的场景。
     *
     * @param sql    要执行的SQL查询语句
     * @param params SQL参数数组
     * @return 查询结果，存储在堆外内存中
     */
    public QueryResult directQuery(String sql, Object... params) {
        return executeQuery(sql, params);
    }

    /**
     * 执行SQL查询的核心方法。
     *
     * <p>查询执行流程：
     * <ol>
     *   <li>获取或创建数据库连接</li>
     *   <li>获取或创建PreparedStatement</li>
     *   <li>设置查询参数</li>
     *   <li>执行查询获取ResultSet</li>
     *   <li>从内存池分配堆外内存块</li>
     *   <li>逐行读取数据并存储到堆外内存</li>
     *   <li>关闭ResultSet，根据缓存级别决定是否关闭Statement</li>
     * </ol>
     *
     * <p>数据存储采用紧凑的二进制格式，每列按固定大小存储，
     * 便于通过偏移量直接定位，无需反序列化。
     *
     * @param sql    要执行的SQL查询语句
     * @param params SQL参数数组
     * @return 查询结果，包含堆外内存存储的结果集
     * @throws RuntimeException 如果SQL执行失败
     */
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

            while (rs.next() && overflowHandler.shouldContinue(rowCount, maxRows)) {
                int rowOffset = rowCount * rowSize;
                for (int i = 0; i < colCount; i++) {
                    ColumnMeta cm = meta.columns.get(i);
                    setColumnValueUltraFast(store, rs, rowOffset, store.getColumnOffset(i), i + 1, cm.getSqlType(),
                                            cm.getSize());
                }
                rowCount++;
            }

            if (rowCount >= maxRows) {
                overflowHandler.onOverflow(maxRows);
            }

            rs.close();

            if (!config.isL1CacheEnabled()) {
                ps.close();
            }

            return new QueryResultImpl(store, rowCount, block, memoryPool);

        } catch (SQLException e) {
            throw new RuntimeException("Query execution failed: " + sql, e);
        }
    }

    /**
     * 将QueryResult复制为CachedQueryResult。
     *
     * <p>将堆外内存中的查询结果复制到堆内存中，以便进行缓存。
     * 复制过程会遍历所有行和列，将数据转换为可序列化的格式。
     *
     * @param result 源查询结果
     * @return 可缓存的查询结果对象
     */
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

    /**
     * 从QueryResult中获取指定列的值。
     *
     * <p>按优先级尝试获取不同类型的值：首先尝试Long，失败后尝试Double，
     * 最后使用String。这种方式可以处理大多数常见数据类型。
     *
     * @param result      查询结果对象
     * @param columnIndex 列索引（从0开始）
     * @return 列值对象
     */
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

    /**
     * 获取或创建PreparedStatement。
     *
     * <p>根据缓存级别采取不同策略：
     * <ul>
     *   <li>NONE：每次创建新的Statement</li>
     *   <li>L1/L2：从缓存获取，若不存在或已关闭则创建新的并缓存</li>
     * </ul>
     *
     * <p>重用Statement可以避免SQL解析和编译开销，显著提升性能。
     *
     * @param sql  SQL语句
     * @param conn 数据库连接
     * @return PreparedStatement实例
     * @throws SQLException 如果创建Statement失败
     */
    private PreparedStatement getOrCreateStatement(String sql, Connection conn) throws SQLException {
        if (!config.isL1CacheEnabled()) {
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

    /**
     * 获取或创建查询元数据。
     *
     * <p>元数据包含列信息、行大小、最大行数等。缓存元数据可以
     * 避免重复解析ResultSet元数据，提升查询性能。
     *
     * @param sql SQL语句，用作缓存键
     * @param rs  结果集，用于提取元数据
     * @return 查询元数据对象
     * @throws SQLException 如果提取元数据失败
     */
    private QueryMeta getOrCreateMeta(String sql, ResultSet rs) throws SQLException {
        if (!config.isL1CacheEnabled()) {
            return createQueryMeta(rs);
        }

        QueryMeta meta = metaCache.get(sql);
        if (meta == null) {
            meta = createQueryMeta(rs);
            metaCache.put(sql, meta);
        }
        return meta;
    }

    /**
     * 从ResultSet创建查询元数据。
     *
     * <p>提取每列的名称、SQL类型和存储大小，构建ColumnMeta列表。
     * 同时计算单行大小和内存槽可容纳的最大行数。
     *
     * @param rs 结果集
     * @return 查询元数据对象
     * @throws SQLException 如果提取元数据失败
     */
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

    /**
     * 获取数据库连接。
     *
     * <p>采用懒加载方式，首次查询时才获取连接。
     * 如果连接不存在或已关闭，则从数据源获取新连接。
     *
     * @return 数据库连接
     * @throws SQLException 如果获取连接失败
     */
    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = dataSource.getConnection();
        }
        return connection;
    }

    /**
     * 设置PreparedStatement的参数。
     *
     * <p>按顺序设置参数，索引从1开始（JDBC规范）。
     *
     * @param PreparedStatement对象
     * @param params              参数数组
     * @throws SQLException 如果设置参数失败
     */
    private void setParameters(PreparedStatement ps, Object[] params) throws SQLException {
        final int len = params.length;
        for (int i = 0; i < len; i++) {
            Object param = params[i];
            int idx = i + 1;
            if (param == null) {
                ps.setNull(idx, java.sql.Types.NULL);
            } else if (param instanceof String) {
                ps.setString(idx, (String) param);
            } else if (param instanceof Integer) {
                ps.setInt(idx, (Integer) param);
            } else if (param instanceof Long) {
                ps.setLong(idx, (Long) param);
            } else if (param instanceof Double) {
                ps.setDouble(idx, (Double) param);
            } else if (param instanceof Float) {
                ps.setFloat(idx, (Float) param);
            } else if (param instanceof Boolean) {
                ps.setBoolean(idx, (Boolean) param);
            } else if (param instanceof java.sql.Date) {
                ps.setDate(idx, (java.sql.Date) param);
            } else if (param instanceof java.sql.Timestamp) {
                ps.setTimestamp(idx, (java.sql.Timestamp) param);
            } else if (param instanceof java.util.Date) {
                ps.setTimestamp(idx, new java.sql.Timestamp(((java.util.Date) param).getTime()));
            } else if (param instanceof byte[]) {
                ps.setBytes(idx, (byte[]) param);
            } else {
                ps.setObject(idx, param);
            }
        }
    }

    /**
     * 根据SQL类型获取列的存储大小。
     *
     * <p>不同SQL类型对应不同的存储空间：
     * <ul>
     *   <li>INTEGER/DATE/FLOAT/REAL：4字节</li>
     *   <li>BIGINT/TIMESTAMP/DOUBLE：8字节</li>
     *   <li>BOOLEAN/TINYINT：1字节</li>
     *   <li>SMALLINT：2字节</li>
     *   <li>VARCHAR/CHAR/LONGVARCHAR：64字节</li>
     *   <li>DECIMAL/NUMERIC及其他：32字节</li>
     * </ul>
     *
     * @param sqlType SQL类型常量（来自java.sql.Types）
     * @return 存储大小（字节数）
     */
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

    /**
     * 将ResultSet中的列值快速写入RowStore。
     *
     * <p>这是核心数据写入方法，采用类型分支优化性能。
     * 根据SQL类型选择对应的方法直接写入堆外内存，避免装箱开销。
     *
     * <p>写入规则：
     * <ul>
     *   <li>整型系列：使用setIntFast写入</li>
     *   <li>BIGINT：使用setLongFast写入</li>
     *   <li>浮点系列：使用setDoubleFast写入</li>
     *   <li>BOOLEAN：转换为0/1后使用setIntFast写入</li>
     *   <li>时间类型：转换为时间戳毫秒值后使用setLongFast写入</li>
     *   <li>精度类型：转换为double后使用setDoubleFast写入</li>
     *   <li>字符串类型：使用setStringFast写入，UTF-8编码</li>
     * </ul>
     *
     * @param store      行存储对象
     * @param rs         结果集
     * @param rowOffset  行在内存块中的偏移量
     * @param colOffset  列在行中的偏移量
     * @param rsColIndex ResultSet中的列索引（从1开始）
     * @param sqlType    SQL类型常量
     * @param maxSize    最大存储大小
     * @throws SQLException 如果读取ResultSet失败
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>执行查询并使用函数式处理器处理结果。
     * 处理完成后自动关闭结果集，确保资源释放。
     *
     * @param <T>     返回类型
     * @param sql     SQL查询语句
     * @param handler 结果处理函数
     * @return 处理器返回的结果
     * @throws IllegalStateException 如果会话已关闭
     */
    @Override
    public <T> T query(String sql, Function<QueryResult, T> handler) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        QueryResult result = query(sql, new Object[0]);
        if (result == null) {
            return null;
        }
        try {
            return handler.apply(result);
        } finally {
            result.close();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>执行带参数的流式行处理查询。
     *
     * <p>流式处理特点：
     * <ul>
     *   <li>逐行处理，无需一次性加载全部数据到内存</li>
     *   <li>使用线程本地缓冲区，减少内存分配</li>
     *   <li>适合大数据量导出、ETL等场景</li>
     * </ul>
     *
     * <p>处理流程：
     * <ol>
     *   <li>获取连接和PreparedStatement</li>
     *   <li>执行查询获取ResultSet</li>
     *   <li>创建行元数据</li>
     *   <li>逐行读取并调用处理器</li>
     *   <li>关闭ResultSet</li>
     * </ol>
     *
     * @param sql     SQL查询语句
     * @param handler 行处理器，每行调用一次
     * @param params  SQL参数数组
     * @throws IllegalStateException 如果会话已关闭
     * @throws NullPointerException  如果handler为null
     */
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
            int maxRows = queryMeta.maxRows;
            int rowCount = 0;
            byte[] rawRow = getOrCreateBuffer(rowSize);

            while (rs.next() && overflowHandler.shouldContinue(rowCount, maxRows)) {
                readRowToBytes(rs, rawRow, queryMeta.columns, colCount);
                handler.onNextRow(rawRow, rowMeta);
                rowCount++;
            }

            if (rowCount >= maxRows) {
                overflowHandler.onOverflow(maxRows);
            }

            rs.close();
            if (!config.isL1CacheEnabled()) {
                ps.close();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Query with handler failed: " + sql, e);
        }
    }

    /**
     * 获取或创建行缓冲区。
     *
     * <p>使用ThreadLocal存储行缓冲区，避免频繁分配。
     * 如果现有缓冲区大小不足，则创建新的更大的缓冲区。
     *
     * @param requiredSize 所需缓冲区大小
     * @return 字节缓冲区
     */
    private byte[] getOrCreateBuffer(int requiredSize) {
        byte[] buffer = rowBuffer.get();
        if (buffer.length < requiredSize) {
            buffer = new byte[requiredSize];
            rowBuffer.set(buffer);
        }
        return buffer;
    }

    /**
     * 将ResultSet中的单行数据读取到字节数组。
     *
     * <p>按列顺序依次读取数据并写入字节数组，
     * 每列的偏移量根据前几列的大小累加计算。
     *
     * @param rs       结果集
     * @param rawRow   目标字节数组
     * @param columns  列元数据列表
     * @param colCount 列数量
     * @throws SQLException 如果读取数据失败
     */
    private void readRowToBytes(ResultSet rs, byte[] rawRow, List<ColumnMeta> columns, int colCount) throws
                                                                                                     SQLException {
        int offset = 0;
        for (int i = 0; i < colCount; i++) {
            ColumnMeta cm = columns.get(i);
            int rsColIndex = i + 1;
            readColumnToBytes(rs, rawRow, offset, rsColIndex, cm.getSqlType(), cm.getSize());
            offset += cm.getSize();
        }
    }

    /**
     * 将ResultSet中的单列数据读取到字节数组。
     *
     * <p>根据SQL类型选择对应的写入方式：
     * <ul>
     *   <li>整型：大端序写入4字节</li>
     *   <li>长整型：大端序写入8字节</li>
     *   <li>时间戳：转换为毫秒值后大端序写入</li>
     *   <li>浮点型：使用IEEE 754格式写入8字节</li>
     *   <li>布尔型：写入1字节（0或1）</li>
     *   <li>字符串：写入长度前缀+UTF-8字节</li>
     * </ul>
     *
     * @param rs         结果集
     * @param rawRow     目标字节数组
     * @param offset     写入起始偏移量
     * @param rsColIndex ResultSet中的列索引（从1开始）
     * @param sqlType    SQL类型常量
     * @param maxSize    最大存储大小
     * @throws SQLException 如果读取数据失败
     */
    private void readColumnToBytes(ResultSet rs, byte[] rawRow, int offset, int rsColIndex, int sqlType,
                                   int maxSize) throws SQLException {
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
                if (str == null) {
                    writeInt(rawRow, offset, 0);
                } else {
                    byte[] strBytes = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    int len = Math.min(strBytes.length, maxSize - 4);
                    writeInt(rawRow, offset, len);
                    System.arraycopy(strBytes, 0, rawRow, offset + 4, len);
                }
                break;
        }
    }

    /**
     * 将int值以大端序写入字节数组。
     *
     * <p>大端序即高位字节在前，便于网络传输和跨平台兼容。
     *
     * @param buf    目标字节数组
     * @param offset 写入起始偏移量
     * @param value  要写入的int值
     */
    private void writeInt(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value >> 24);
        buf[offset + 1] = (byte) (value >> 16);
        buf[offset + 2] = (byte) (value >> 8);
        buf[offset + 3] = (byte) value;
    }

    /**
     * 将long值以大端序写入字节数组。
     *
     * <p>大端序即高位字节在前，便于网络传输和跨平台兼容。
     *
     * @param buf    目标字节数组
     * @param offset 写入起始偏移量
     * @param value  要写入的long值
     */
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

    /**
     * 将double值写入字节数组。
     *
     * <p>先将double转换为IEEE 754格式的long位表示，
     * 然后以大端序写入8字节。
     *
     * @param buf    目标字节数组
     * @param offset 写入起始偏移量
     * @param value  要写入的double值
     */
    private void writeDouble(byte[] buf, int offset, double value) {
        writeLong(buf, offset, Double.doubleToLongBits(value));
    }

    /**
     * {@inheritDoc}
     *
     * <p>执行带参数的更新操作（INSERT/UPDATE/DELETE）。
     *
     * <p>更新操作不使用PreparedStatement缓存，每次都创建新Statement，
     * 因为更新操作通常只执行一次，缓存意义不大。
     *
     * @param sql    SQL更新语句
     * @param params SQL参数数组
     * @return 受影响的行数
     * @throws IllegalStateException 如果会话已关闭
     * @throws RuntimeException      如果执行失败
     */
    @Override
    public int update(String sql, Object... params) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        try {
            Connection conn = getConnection();
            PreparedStatement ps = getOrCreateStatement(sql, conn);
            setParameters(ps, params);
            int result = ps.executeUpdate();

            if (!config.isL1CacheEnabled()) {
                ps.close();
            }

            if (resultCache != null) {
                resultCache.invalidateAll();
            }

            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Update execution failed: " + sql, e);
        }
    }

    @Override
    public int delete(String sql, Object... params) {
        return update(sql, params);
    }

    @Override
    public <T> T insert(String sql, Class<T> keyType, Object... params) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        if (keyType == null) {
            throw new NullPointerException("keyType cannot be null");
        }
        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            setParameters(ps, params);
            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                ps.close();
                return null;
            }

            T generatedKey = null;
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    generatedKey = extractKey(rs, keyType);
                }
            }

            ps.close();

            if (resultCache != null) {
                resultCache.invalidateAll();
            }

            return generatedKey;
        } catch (SQLException e) {
            throw new RuntimeException("Insert execution failed: " + sql, e);
        }
    }

    @Override
    public boolean insert(String sql, Object... params) {
        return update(sql, params) > 0;
    }

    @SuppressWarnings("unchecked")
    private <T> T extractKey(ResultSet rs, Class<T> keyType) throws SQLException {
        Object key = rs.getObject(1);
        if (key == null) {
            return null;
        }
        if (keyType.isInstance(key)) {
            return (T) key;
        }
        if (keyType == Long.class || keyType == long.class) {
            return (T) Long.valueOf(rs.getLong(1));
        }
        if (keyType == Integer.class || keyType == int.class) {
            return (T) Integer.valueOf(rs.getInt(1));
        }
        if (keyType == String.class) {
            return (T) rs.getString(1);
        }
        return (T) key;
    }

    /**
     * {@inheritDoc}
     *
     * <p>执行参数化批量操作。
     *
     * <p>使用JDBC的addBatch/executeBatch机制，适合同一SQL不同参数的场景。
     *
     * @param sql        SQL语句（带占位符）
     * @param paramsList 参数列表，每组参数对应一次执行
     * @return 每次执行影响的行数数组
     * @throws IllegalStateException 如果会话已关闭
     * @throws RuntimeException      如果执行失败
     */
    @Override
    public int[] batchUpdate(String sql, List<Object[]> paramsList) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        if (paramsList == null || paramsList.isEmpty()) {
            return new int[0];
        }

        int totalSize = paramsList.size();
        int[] allResults = new int[totalSize];
        int resultIndex = 0;

        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            int batchCount = 0;
            for (Object[] params : paramsList) {
                setParameters(ps, params);
                ps.addBatch();
                batchCount++;

                if (batchCount >= DEFAULT_BATCH_SIZE) {
                    int[] batchResults = ps.executeBatch();
                    System.arraycopy(batchResults, 0, allResults, resultIndex, batchResults.length);
                    resultIndex += batchResults.length;
                    ps.clearBatch();
                    batchCount = 0;
                }
            }

            if (batchCount > 0) {
                int[] batchResults = ps.executeBatch();
                System.arraycopy(batchResults, 0, allResults, resultIndex, batchResults.length);
            }

            ps.close();

            if (resultCache != null) {
                resultCache.invalidateAll();
            }

            return allResults;

        } catch (SQLException e) {
            throw new RuntimeException("Batch execution failed: " + sql, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>使用实体映射器执行参数化批量操作。
     *
     * <p>将实体对象转换为参数数组后执行批处理，
     * 适合ORM场景，简化应用层代码。
     *
     * @param <T>      实体类型
     * @param sql      SQL语句（带占位符）
     * @param entities 实体对象列表
     * @param mapper   实体到参数的映射器
     * @return 每次执行影响的行数数组
     * @throws IllegalStateException 如果会话已关闭
     * @throws NullPointerException  如果mapper为null
     * @throws RuntimeException      如果执行失败
     */
    @Override
    public <T> int[] batchUpdate(String sql, List<T> entities, EntityMapper<T> mapper) {
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

            if (resultCache != null) {
                resultCache.invalidateAll();
            }

            return results;

        } catch (SQLException e) {
            throw new RuntimeException("Batch execution failed: " + sql, e);
        }
    }

    @Override
    public int[] batchDelete(String sql, List<Object[]> paramsList) {
        return batchUpdate(sql, paramsList);
    }

    @Override
    public <T> List<T> batchInsert(String sql, Class<T> keyType, List<Object[]> paramsList) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        if (keyType == null) {
            throw new NullPointerException("keyType cannot be null");
        }
        if (paramsList == null || paramsList.isEmpty()) {
            return new ArrayList<>();
        }

        List<T> keys = new ArrayList<>(paramsList.size());

        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            int batchCount = 0;
            for (Object[] params : paramsList) {
                setParameters(ps, params);
                ps.addBatch();
                batchCount++;

                if (batchCount >= DEFAULT_BATCH_SIZE) {
                    ps.executeBatch();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        while (rs.next()) {
                            keys.add(extractKey(rs, keyType));
                        }
                    }
                    ps.clearBatch();
                    batchCount = 0;
                }
            }

            if (batchCount > 0) {
                ps.executeBatch();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    while (rs.next()) {
                        keys.add(extractKey(rs, keyType));
                    }
                }
            }

            ps.close();

            if (resultCache != null) {
                resultCache.invalidateAll();
            }

            return keys;

        } catch (SQLException e) {
            throw new RuntimeException("Batch insert failed: " + sql, e);
        }
    }

    @Override
    public int batchInsert(String sql, List<Object[]> paramsList) {
        if (paramsList == null || paramsList.isEmpty()) {
            return 0;
        }
        int[] results = batchUpdate(sql, paramsList);
        int successCount = 0;
        for (int r : results) {
            if (r > 0) {
                successCount++;
            }
        }
        return successCount;
    }

    /**
     * 清空所有缓存。
     *
     * <p>清空操作包括：
     * <ul>
     *   <li>关闭并清空PreparedStatement缓存</li>
     *   <li>清空查询元数据缓存</li>
     *   <li>清空结果缓存</li>
     * </ul>
     *
     * <p>通常在数据变更后调用，确保后续查询获取最新数据。
     */
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

    /**
     * 使结果缓存失效。
     *
     * <p>仅清空L2结果缓存，不影响语句缓存和元数据缓存。
     * 用于数据变更后需要重新查询的场景。
     */
    public void invalidateResultCache() {
        if (resultCache != null) {
            resultCache.invalidateAll();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return 内存池实例
     */
    @Override
    public MemoryPool getMemoryPool() {
        return memoryPool;
    }

    /**
     * {@inheritDoc}
     *
     * @return 会话是否已关闭
     */
    @Override
    public boolean isClosed() {
        return closed;
    }

    /**
     * {@inheritDoc}
     *
     * <p>关闭会话并释放所有资源：
     * <ol>
     *   <li>清空所有缓存</li>
     *   <li>关闭数据库连接</li>
     *   <li>设置关闭标志</li>
     * </ol>
     *
     * <p>关闭后的会话不能再使用，重复调用close()是安全的。
     */
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

    /**
     * 查询执行策略接口。
     *
     * <p>用于根据缓存级别选择不同的查询执行方式：
     * <ul>
     *   <li>直接查询：每次都执行数据库查询</li>
     *   <li>缓存查询：先查缓存，未命中再查数据库</li>
     * </ul>
     */
    @FunctionalInterface
    private interface QueryAction {
        /**
         * 执行查询。
         *
         * @param sql    SQL语句
         * @param params 参数数组
         * @return 查询结果
         */
        QueryResult query(String sql, Object... params);
    }

    /**
     * 查询元数据内部类。
     *
     * <p>封装查询结果的元数据信息，包括：
     * <ul>
     *   <li>列元数据列表</li>
     *   <li>单行大小</li>
     *   <li>内存槽可容纳的最大行数</li>
     * </ul>
     */
    private static class QueryMeta {
        /**
         * 列元数据列表，按列顺序存储。
         */
        final List<ColumnMeta> columns;

        /**
         * 单行的字节大小，等于所有列大小之和。
         */
        final int rowSize;

        /**
         * 单个内存槽可容纳的最大行数。
         * 由内存槽大小除以单行大小计算得出。
         */
        final int maxRows;

        /**
         * 构造查询元数据。
         *
         * @param columns  列元数据列表
         * @param slotSize 内存槽大小
         */
        QueryMeta(List<ColumnMeta> columns, int slotSize) {
            this.columns = columns;
            this.rowSize = columns.stream().mapToInt(ColumnMeta::getSize).sum();
            this.maxRows = slotSize / Math.max(rowSize, 1);
        }
    }
}
