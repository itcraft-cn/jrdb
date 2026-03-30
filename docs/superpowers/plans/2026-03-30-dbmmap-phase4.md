# dbmmap Phase 4 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan.

**Goal:** 实现dbmmap高级特性 - 列存视图、事务API、查询执行器、性能优化

**Architecture:** ColumnStore按列重排数据，提供OLAP聚合能力；Transaction独立事务管理API；QueryExecutor执行SQL并填充RowStore；性能优化包括内存对齐、批量操作。

**Tech Stack:** Java 8+, sun.misc.Unsafe, JDBC, Maven, JUnit 5

---

## 文件结构

```
dbmmap-core/src/main/java/com/dbmmap/core/
├── storage/
│   ├── ColumnStore.java         # 列存储实现
│   └── HybridView.java          # 混合视图切换
├── transaction/
│   ├── Transaction.java         # 事务接口
│   ├── TransactionImpl.java     # 事务实现
│   └── TransactionManager.java  # 事务管理器
├── executor/
│   ├── QueryExecutor.java       # 查询执行器
│   └── JdbcQueryExecutor.java   # JDBC实现
└── config/
    └── JdbcConfig.java          # JDBC配置
```

---

## Task 1: ColumnStore列存储

**Files:**
- `dbmmap-core/src/main/java/com/dbmmap/core/storage/ColumnStore.java`
- `dbmmap-core/src/test/java/com/dbmmap/core/storage/ColumnStoreTest.java`

**ColumnStore.java:**
```java
package com.dbmmap.core.storage;

import com.dbmmap.core.memory.MemoryBlock;
import java.util.List;

/**
 * 列存储实现，按列组织数据
 * 适合OLAP场景，聚合计算高效
 */
public class ColumnStore {
    
    private final MemoryBlock block;
    private final List<ColumnMeta> columns;
    private final int[] columnOffsets;
    private final int rowCount;
    
    public ColumnStore(MemoryBlock block, List<ColumnMeta> columns, int rowCount) {
        this.block = block;
        this.columns = columns;
        this.rowCount = rowCount;
        this.columnOffsets = calculateColumnOffsets();
    }
    
    /**
     * 计算每列的起始偏移量
     * 列存格式: [col1_all_rows][col2_all_rows][col3_all_rows]...
     */
    private int[] calculateColumnOffsets() {
        int[] offsets = new int[columns.size()];
        int offset = 0;
        for (int i = 0; i < columns.size(); i++) {
            offsets[i] = offset;
            offset += columns.get(i).getSize() * rowCount;
        }
        return offsets;
    }
    
    /**
     * 获取指定列的所有值
     */
    public int[] getIntColumn(int columnIndex) {
        ColumnMeta meta = columns.get(columnIndex);
        int[] values = new int[rowCount];
        int baseOffset = columnOffsets[columnIndex];
        for (int row = 0; row < rowCount; row++) {
            values[row] = block.readInt(baseOffset + row * meta.getSize());
        }
        return values;
    }
    
    /**
     * 设置指定列的所有值
     */
    public void setIntColumn(int columnIndex, int[] values) {
        ColumnMeta meta = columns.get(columnIndex);
        int baseOffset = columnOffsets[columnIndex];
        for (int row = 0; row < Math.min(values.length, rowCount); row++) {
            block.writeInt(baseOffset + row * meta.getSize(), values[row]);
        }
    }
    
    /**
     * 聚合: 求和
     */
    public long sumIntColumn(int columnIndex) {
        long sum = 0;
        int[] values = getIntColumn(columnIndex);
        for (int v : values) sum += v;
        return sum;
    }
    
    /**
     * 聚合: 平均值
     */
    public double avgIntColumn(int columnIndex) {
        return rowCount > 0 ? (double) sumIntColumn(columnIndex) / rowCount : 0;
    }
    
    /**
     * 聚合: 最大值
     */
    public int maxIntColumn(int columnIndex) {
        int[] values = getIntColumn(columnIndex);
        int max = Integer.MIN_VALUE;
        for (int v : values) if (v > max) max = v;
        return max;
    }
    
    /**
     * 聚合: 最小值
     */
    public int minIntColumn(int columnIndex) {
        int[] values = getIntColumn(columnIndex);
        int min = Integer.MAX_VALUE;
        for (int v : values) if (v < min) min = v;
        return min;
    }
    
    public int getRowCount() { return rowCount; }
    public int getColumnCount() { return columns.size(); }
    public ColumnMeta getColumnMeta(int index) { return columns.get(index); }
}
```

**Test cases:**
- shouldStoreAndRetrieveIntColumn
- shouldAggregateSum
- shouldAggregateAvg
- shouldAggregateMaxMin
- shouldHandleMultipleColumns

---

## Task 2: HybridView混合视图

**Files:**
- `dbmmap-core/src/main/java/com/dbmmap/core/storage/HybridView.java`
- `dbmmap-core/src/test/java/com/dbmmap/core/storage/HybridViewTest.java`

**HybridView.java:**
```java
package com.dbmmap.core.storage;

import com.dbmmap.core.memory.MemoryBlock;
import java.util.List;

/**
 * 混合视图，支持行存和列存切换
 */
public class HybridView {
    
    private final MemoryBlock block;
    private final List<ColumnMeta> columns;
    private final int rowCount;
    private final RowStore rowStore;
    private ColumnStore columnStore;
    private boolean isColumnMode;
    
    public HybridView(MemoryBlock block, List<ColumnMeta> columns, int rowCount) {
        this.block = block;
        this.columns = columns;
        this.rowCount = rowCount;
        this.rowStore = new RowStore(block, columns);
        this.isColumnMode = false;
    }
    
    /**
     * 切换到列存视图
     */
    public ColumnStore toColumnView() {
        if (columnStore == null) {
            // 需要重排数据，创建新的ColumnStore
            columnStore = new ColumnStore(block, columns, rowCount);
            transposeRowToColumn();
        }
        isColumnMode = true;
        return columnStore;
    }
    
    /**
     * 切换到行存视图
     */
    public RowStore toRowView() {
        isColumnMode = false;
        return rowStore;
    }
    
    /**
     * 行存转列存
     */
    private void transposeRowToColumn() {
        int rowSize = rowStore.getRowSize();
        // 创建临时缓冲区存储转置后的数据
        // 这里简化处理，实际需要更复杂的内存操作
    }
    
    public boolean isColumnMode() { return isColumnMode; }
    public RowStore getRowStore() { return rowStore; }
    public ColumnStore getColumnStore() { return columnStore; }
}
```

---

## Task 3: Transaction事务API

**Files:**
- `dbmmap-core/src/main/java/com/dbmmap/core/transaction/Transaction.java`
- `dbmmap-core/src/main/java/com/dbmmap/core/transaction/TransactionImpl.java`
- `dbmmap-core/src/main/java/com/dbmmap/core/transaction/TransactionManager.java`
- `dbmmap-core/src/test/java/com/dbmmap/core/transaction/TransactionTest.java`

**Transaction.java:**
```java
package com.dbmmap.core.transaction;

import java.sql.Connection;

/**
 * 事务接口
 */
public interface Transaction {
    
    /**
     * 获取数据库连接
     */
    Connection getConnection();
    
    /**
     * 提交事务
     */
    void commit();
    
    /**
     * 回滚事务
     */
    void rollback();
    
    /**
     * 关闭事务（自动提交或回滚）
     */
    void close();
    
    /**
     * 是否已关闭
     */
    boolean isClosed();
}
```

**TransactionImpl.java:**
```java
package com.dbmmap.core.transaction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class TransactionImpl implements Transaction {
    
    private final DataSource dataSource;
    private Connection connection;
    private boolean closed;
    private boolean autoCommit;
    
    public TransactionImpl(DataSource dataSource) {
        this.dataSource = dataSource;
        this.closed = false;
        this.autoCommit = false;
    }
    
    @Override
    public Connection getConnection() {
        if (closed) {
            throw new IllegalStateException("Transaction is closed");
        }
        if (connection == null) {
            try {
                connection = dataSource.getConnection();
                connection.setAutoCommit(autoCommit);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get connection", e);
            }
        }
        return connection;
    }
    
    @Override
    public void commit() {
        if (connection != null) {
            try {
                connection.commit();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to commit", e);
            }
        }
    }
    
    @Override
    public void rollback() {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to rollback", e);
            }
        }
    }
    
    @Override
    public void close() {
        if (closed) return;
        closed = true;
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }
    
    @Override
    public boolean isClosed() {
        return closed;
    }
}
```

**TransactionManager.java:**
```java
package com.dbmmap.core.transaction;

import javax.sql.DataSource;
import java.util.function.Function;

/**
 * 事务管理器
 */
public class TransactionManager {
    
    private final DataSource dataSource;
    
    public TransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * 开始新事务
     */
    public Transaction beginTransaction() {
        return new TransactionImpl(dataSource);
    }
    
    /**
     * 在事务中执行操作
     */
    public <T> T executeInTransaction(Function<Transaction, T> action) {
        Transaction tx = beginTransaction();
        try {
            T result = action.apply(tx);
            tx.commit();
            return result;
        } catch (Exception e) {
            tx.rollback();
            throw new RuntimeException("Transaction failed", e);
        } finally {
            tx.close();
        }
    }
    
    /**
     * 在事务中执行无返回值操作
     */
    public void runInTransaction(TransactionCallback callback) {
        executeInTransaction(tx -> {
            callback.execute(tx);
            return null;
        });
    }
    
    @FunctionalInterface
    public interface TransactionCallback {
        void execute(Transaction tx);
    }
}
```

---

## Task 4: QueryExecutor查询执行器

**Files:**
- `dbmmap-core/src/main/java/com/dbmmap/core/executor/QueryExecutor.java`
- `dbmmap-core/src/main/java/com/dbmmap/core/executor/JdbcQueryExecutor.java`
- `dbmmap-core/src/test/java/com/dbmmap/core/executor/QueryExecutorTest.java`

**QueryExecutor.java:**
```java
package com.dbmmap.core.executor;

import com.dbmmap.core.query.QueryResult;
import com.dbmmap.core.sql.BoundSql;

/**
 * 查询执行器接口
 */
public interface QueryExecutor {
    
    /**
     * 执行查询
     */
    QueryResult query(BoundSql sql);
    
    /**
     * 执行更新
     */
    int update(BoundSql sql);
    
    /**
     * 执行批量更新
     */
    int[] batchUpdate(BoundSql... sqls);
}
```

**JdbcQueryExecutor.java:**
```java
package com.dbmmap.core.executor;

import com.dbmmap.core.config.DbMmapConfig;
import com.dbmmap.core.memory.MemoryBlock;
import com.dbmmap.core.memory.MemoryPool;
import com.dbmmap.core.query.QueryResult;
import com.dbmmap.core.query.QueryResultImpl;
import com.dbmmap.core.sql.BoundSql;
import com.dbmmap.core.storage.ColumnMeta;
import com.dbmmap.core.storage.RowStore;
import com.dbmmap.core.transaction.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcQueryExecutor implements QueryExecutor {
    
    private final MemoryPool memoryPool;
    private final DbMmapConfig config;
    private Transaction transaction;
    
    public JdbcQueryExecutor(MemoryPool memoryPool, DbMmapConfig config) {
        this.memoryPool = memoryPool;
        this.config = config;
    }
    
    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }
    
    @Override
    public QueryResult query(BoundSql sql) {
        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql.getSql());
            setParameters(ps, sql.getParams());
            
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // 构建列元数据
            List<ColumnMeta> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                String name = metaData.getColumnLabel(i);
                int sqlType = metaData.getColumnType(i);
                int size = getColumnSize(sqlType);
                columns.add(new ColumnMeta(name, sqlType, size));
            }
            
            // 计算行大小和最大行数
            int rowSize = columns.stream().mapToInt(ColumnMeta::getSize).sum();
            int maxRows = memoryPool.getSlotSize() / rowSize;
            
            // 分配内存
            MemoryBlock block = memoryPool.allocate();
            RowStore store = new RowStore(block, columns);
            
            // 填充数据
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
            throw new RuntimeException("Query execution failed", e);
        }
    }
    
    @Override
    public int update(BoundSql sql) {
        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql.getSql());
            setParameters(ps, sql.getParams());
            int result = ps.executeUpdate();
            ps.close();
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Update execution failed", e);
        }
    }
    
    @Override
    public int[] batchUpdate(BoundSql... sqls) {
        try {
            Connection conn = getConnection();
            int[] results = new int[sqls.length];
            for (int i = 0; i < sqls.length; i++) {
                PreparedStatement ps = conn.prepareStatement(sqls[i].getSql());
                setParameters(ps, sqls[i].getParams());
                results[i] = ps.executeUpdate();
                ps.close();
            }
            return results;
        } catch (SQLException e) {
            throw new RuntimeException("Batch update failed", e);
        }
    }
    
    private Connection getConnection() {
        if (transaction != null) {
            return transaction.getConnection();
        }
        throw new IllegalStateException("No transaction available");
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
                return 4;
            case Types.BIGINT:
            case Types.TIMESTAMP:
                return 8;
            case Types.BOOLEAN:
            case Types.TINYINT:
                return 1;
            case Types.SMALLINT:
                return 2;
            case Types.FLOAT:
            case Types.REAL:
                return 4;
            case Types.DOUBLE:
                return 8;
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
                return 64;  // Default string size
            default:
                return 32;
        }
    }
    
    private void setColumnValue(RowStore store, ResultSet rs, int row, int col, ColumnMeta meta) 
            throws SQLException {
        int sqlType = meta.getSqlType();
        switch (sqlType) {
            case Types.INTEGER:
                store.setRow(row, rs.getInt(col + 1), col);
                break;
            case Types.BIGINT:
                store.setRow(row, rs.getLong(col + 1), col);
                break;
            case Types.VARCHAR:
            case Types.CHAR:
                store.setRow(row, rs.getString(col + 1), col);
                break;
            case Types.DOUBLE:
            case Types.FLOAT:
                store.setRow(row, rs.getDouble(col + 1), col);
                break;
            case Types.BOOLEAN:
                store.setRow(row, rs.getBoolean(col + 1) ? 1 : 0, col);
                break;
            default:
                store.setRow(row, rs.getObject(col + 1), col);
        }
    }
}
```

---

## Task 5: JdbcConfig配置

**Files:**
- `dbmmap-core/src/main/java/com/dbmmap/core/config/JdbcConfig.java`
- `dbmmap-core/src/test/java/com/dbmmap/core/config/JdbcConfigTest.java`

**JdbcConfig.java:**
```java
package com.dbmmap.core.config;

import javax.sql.DataSource;

/**
 * JDBC配置
 */
public class JdbcConfig {
    
    private final DataSource dataSource;
    private final int maxPoolSize;
    private final int minPoolSize;
    private final long connectionTimeout;
    
    private JdbcConfig(Builder builder) {
        this.dataSource = builder.dataSource;
        this.maxPoolSize = builder.maxPoolSize;
        this.minPoolSize = builder.minPoolSize;
        this.connectionTimeout = builder.connectionTimeout;
    }
    
    public DataSource getDataSource() { return dataSource; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public int getMinPoolSize() { return minPoolSize; }
    public long getConnectionTimeout() { return connectionTimeout; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private DataSource dataSource;
        private int maxPoolSize = 10;
        private int minPoolSize = 1;
        private long connectionTimeout = 30000;
        
        public Builder dataSource(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }
        
        public Builder maxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }
        
        public Builder minPoolSize(int minPoolSize) {
            this.minPoolSize = minPoolSize;
            return this;
        }
        
        public Builder connectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }
        
        public JdbcConfig build() {
            if (dataSource == null) {
                throw new IllegalArgumentException("dataSource is required");
            }
            return new JdbcConfig(this);
        }
    }
}
```

---

## 执行顺序

1. 创建transaction和executor目录
2. Task 1: ColumnStore
3. Task 2: HybridView
4. Task 3: Transaction API
5. Task 4: QueryExecutor
6. Task 5: JdbcConfig
7. 运行全部测试

---

## 自检清单

- [ ] 所有测试通过
- [ ] ColumnStore聚合功能正确
- [ ] Transaction支持commit/rollback
- [ ] QueryExecutor能执行SQL并填充RowStore
- [ ] 无placeholder代码