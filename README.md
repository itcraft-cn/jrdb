# dbmmap

高性能数据库内存映射框架，基于堆外内存实现，追求极致查询性能。

## 特性

- **堆外内存存储** - 使用 Unsafe 直接操作堆外内存，避免 GC 压力
- **ASM 动态映射** - 运行时生成字节码，实体映射性能接近原生 JDBC
- **RowHandler 流式处理** - 零对象创建，适合 ETL/数据导出场景
- **多级缓存** - 支持 L1(Statement缓存)/L2(结果缓存)
- **Spring Boot 集成** - 提供 Starter 自动配置

## 性能基准

### 查询性能 (NoCache, 4线程)

#### 单条查询

| 框架 | ops/s | vs MyBatis | vs Hibernate |
|------|-------|------------|--------------|
| **dbmmap_asm** | **2.56M** | +117% | +174% |
| dbmmap | 2.16M | +82% | +130% |
| MyBatis | 1.18M | baseline | +26% |
| Hibernate | 937K | -21% | baseline |

#### 批量查询

| 框架 | ops/s | vs MyBatis | vs Hibernate |
|------|-------|------------|--------------|
| **dbmmap_asm** | **702K** | +550% | +688% |
| dbmmap | 482K | +347% | +442% |
| MyBatis | 108K | baseline | +21% |
| Hibernate | 89K | -18% | baseline |

### 写入性能 (NoCache, 4线程)

| 操作 | dbmmap_raw | myBatis | hibernate | jdbc |
|------|------------|---------|-----------|------|
| 单条Insert | 630K | **677K** | 416K | 661K |
| 批量Insert(100条) | 7.18K | **7.56K** | 6.29K | 7.54K |
| 批量Update(100条) | **5.02K** | 4.36K | 4.41K | 4.46K |
| 批量Delete(100条) | 4.93K | **5.77K** | 74* | 4.62K |

*Hibernate batchDelete 极慢是因为使用 get+delete 循环

### 定位总结

| 场景 | dbmmap 优势 | 说明 |
|------|-------------|------|
| **SELECT 单条** | ✅ **2.2x MyBatis** | 堆外内存 + ASM 映射 |
| **SELECT 批量** | ✅ **6.5x MyBatis** | 列式存储 + 零拷贝 |
| **RowHandler** | ✅ **1.9x raw** | ThreadLocal buffer 复用 |
| **INSERT** | ⚪ 0.93x MyBatis | 纯 JDBC 封装 |
| **UPDATE/DELETE** | ⚪ 0.85x MyBatis | 纯 JDBC 封装 |

**定位：读优化型框架，适合读多写少场景**

### CUD 优化空间

当前 CUD 性能与 MyBatis 接近，未做特别优化。如需进一步提升：

1. **PreparedStatement 缓存** - 目前仅 CacheLevel.L1 支持
2. **批量写入合并** - write-behind 模式
3. **异步提交** - 后台线程批量 flush
4. **实体脏检查** - 只更新变更字段

## 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>cn.itcraft.dbmmap</groupId>
    <artifactId>dbmmap-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 基本使用

```java
// 1. 创建内存池
MemoryPoolConfig poolConfig = MemoryPoolConfig.builder()
    .preAllocMemSize(64 * 1024 * 1024)  // 64MB
    .queryCacheSize(4 * 1024 * 1024)     // 4MB
    .build();
MemoryPool memoryPool = new MemoryPool(poolConfig);

// 2. 创建配置
DbMmapConfig config = DbMmapConfig.builder()
    .cacheLevel(CacheLevel.L1)           // L1缓存
    .statementCacheSize(256)
    .build();

// 3. 创建 Session
Session session = new SessionImpl(memoryPool, config, dataSource);

// 4. 执行查询
try (QueryResult result = session.query("SELECT id, name, age FROM users WHERE id = ?", 1L)) {
    if (result.next()) {
        long id = result.getLong("ID");
        String name = result.getString("NAME");
        int age = result.getInt("AGE");
    }
}
```

### ASM 实体映射

```java
// 1. 注册 Mapper
TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();
MapperRegistry mapperRegistry = new MapperRegistry(typeHandlerRegistry);
EntityMapper<User> mapper = mapperRegistry.getMapper(User.class);

// 2. 使用 Mapper 映射
try (ResultSet rs = preparedStatement.executeQuery()) {
    if (rs.next()) {
        User user = mapper.mapFromResultSet(rs);
    }
}

// 3. 批量映射
List<User> users = new ArrayList<>();
try (ResultSet rs = preparedStatement.executeQuery()) {
    while (rs.next()) {
        users.add(mapper.mapFromResultSet(rs));
    }
}
```

## 批量操作

### 批量 Insert/Update/Delete

```java
// Raw 方式 - 直接传参
List<Object[]> paramsList = new ArrayList<>();
for (int i = 0; i < 100; i++) {
    paramsList.add(new Object[]{"user_" + i, 20 + i, "user_" + i + "@test.com", "active"});
}
int[] results = session.batchExecute(
    "INSERT INTO users (name, age, email, status) VALUES (?, ?, ?, ?)", 
    paramsList
);

// ASM 方式 - 实体映射
List<User> users = new ArrayList<>();
for (int i = 0; i < 100; i++) {
    users.add(new User(null, "user_" + i, 20 + i, "user_" + i + "@test.com", "active"));
}
EntityMapper<User> mapper = mapperRegistry.getMapper(User.class);
int[] results = session.batchExecute(
    "INSERT INTO users (name, age, email, status) VALUES (?, ?, ?, ?)",
    users, mapper
);

// 批量更新
List<Object[]> updateParams = new ArrayList<>();
for (long id : ids) {
    updateParams.add(new Object[]{"updated", 30, id});
}
session.batchExecute("UPDATE users SET name = ?, age = ? WHERE id = ?", updateParams);

// 批量删除
List<Object[]> deleteParams = new ArrayList<>();
for (long id : ids) {
    deleteParams.add(new Object[]{id});
}
session.batchExecute("DELETE FROM users WHERE id = ?", deleteParams);
```

### 性能对比 (100条/批, 4线程)

| 操作 | dbmmap_raw | myBatis | hibernate | jdbc |
|------|------------|---------|-----------|------|
| batchInsert | 7.18K | **7.56K** | 6.29K | 7.54K |
| batchUpdate | **5.02K** | 4.36K | 4.41K | 4.46K |
| batchDelete | 4.93K | **5.77K** | 0.07K* | 4.62K |

*Hibernate batchDelete 极慢是因为使用 get+delete 循环

## RowHandler 流式处理

### 使用场景

RowHandler 适合**流式处理**场景，避免创建中间对象：

| 场景 | 传统方式 | RowHandler方式 |
|------|----------|----------------|
| 数据库导出CSV | ResultSet→Object→String→write | ResultSet→raw→直接write |
| ETL 数据转换 | 多次对象创建/转换 | 零对象创建 |
| 报表生成 | List<Object>占用内存 | 流式处理，内存恒定 |
| 数据同步 | 高GC压力 | 极低GC压力 |

### 性能对比

| 方式 | ops/s | vs rawQuery | 说明 |
|------|-------|-------------|------|
| dbmmap_rowHandler | **226K** | +93% | 零对象创建 |
| dbmmap_rawQuery | 117K | baseline | QueryResult对象 |
| jdbc_manual | 330K | +182% | 原生JDBC |

### 使用方法

```java
// 定义 RowHandler
session.queryWithHandler("SELECT id, name, age FROM users", (rawRow, meta) -> {
    // rawRow: 原始字节数组，按列顺序存储
    // meta: 行元信息，包含列定义和偏移量
    
    // 提取 int (4字节)
    int age = ((rawRow[meta.getColumnOffset(2)] & 0xFF) << 24) |
              ((rawRow[meta.getColumnOffset(2) + 1] & 0xFF) << 16) |
              ((rawRow[meta.getColumnOffset(2) + 2] & 0xFF) << 8) |
              (rawRow[meta.getColumnOffset(2) + 3] & 0xFF);
    
    // 提取 long (8字节)
    long id = extractLong(rawRow, meta.getColumnOffset(0));
    
    // 提取 String (长度前缀 + UTF8字节)
    String name = extractString(rawRow, meta.getColumnOffset(1));
    
    // 直接写入输出流，零拷贝
    outputStream.write(rawRow, 0, meta.getRowSize());
});

// 带参数查询
session.queryWithHandler(
    "SELECT id, name, age FROM users WHERE age > ?", 
    (rawRow, meta) -> { /* 处理 */ },
    18  // 参数
);
```

### 工具方法

```java
// 提取 int
public static int extractInt(byte[] buf, int offset) {
    return ((buf[offset] & 0xFF) << 24) |
           ((buf[offset + 1] & 0xFF) << 16) |
           ((buf[offset + 2] & 0xFF) << 8) |
           (buf[offset + 3] & 0xFF);
}

// 提取 long
public static long extractLong(byte[] buf, int offset) {
    return ((long) (buf[offset] & 0xFF) << 56) |
           ((long) (buf[offset + 1] & 0xFF) << 48) |
           ((long) (buf[offset + 2] & 0xFF) << 40) |
           ((long) (buf[offset + 3] & 0xFF) << 32) |
           ((long) (buf[offset + 4] & 0xFF) << 24) |
           ((long) (buf[offset + 5] & 0xFF) << 16) |
           ((long) (buf[offset + 6] & 0xFF) << 8) |
           ((long) (buf[offset + 7] & 0xFF));
}

// 提取 double
public static double extractDouble(byte[] buf, int offset) {
    return Double.longBitsToDouble(extractLong(buf, offset));
}

// 提取 String (长度前缀格式)
public static String extractString(byte[] buf, int offset) {
    int len = extractInt(buf, offset);
    if (len == 0) return null;
    return new String(buf, offset + 4, len, StandardCharsets.UTF_8);
}
```

### 数据格式

RowHandler 返回的 `byte[]` 按列顺序存储：

| 列类型 | 存储大小 | 格式 |
|--------|----------|------|
| INT/TINYINT/SMALLINT | 4字节 | 大端序 |
| BIGINT | 8字节 | 大端序 |
| DOUBLE/FLOAT/DECIMAL | 8字节 | IEEE 754 |
| BOOLEAN | 1字节 | 0/1 |
| TIMESTAMP/DATE | 8字节 | epoch millis |
| VARCHAR | 4+N字节 | 长度(4B)+UTF8字节 |

### 导出示例：CSV导出

```java
public void exportToCsv(Session session, String sql, OutputStream out) throws IOException {
    StringBuilder sb = new StringBuilder();
    
    session.queryWithHandler(sql, (rawRow, meta) -> {
        sb.setLength(0);
        int colCount = meta.getColumnCount();
        
        for (int i = 0; i < colCount; i++) {
            if (i > 0) sb.append(',');
            appendValue(sb, rawRow, meta, i);
        }
        sb.append('\n');
        
        try {
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    });
}

private void appendValue(StringBuilder sb, byte[] rawRow, RowMeta meta, int colIndex) {
    int offset = meta.getColumnOffset(colIndex);
    ColumnMeta col = meta.getColumn(colIndex);
    
    switch (col.getSqlType()) {
        case Types.INTEGER:
        case Types.TINYINT:
        case Types.SMALLINT:
            sb.append(extractInt(rawRow, offset));
            break;
        case Types.BIGINT:
            sb.append(extractLong(rawRow, offset));
            break;
        case Types.DOUBLE:
        case Types.FLOAT:
            sb.append(extractDouble(rawRow, offset));
            break;
        case Types.VARCHAR:
        case Types.CHAR:
            String str = extractString(rawRow, offset);
            if (str != null) {
                sb.append('"').append(str.replace("\"", "\"\"")).append('"');
            }
            break;
        // ... 其他类型
    }
}
```

## Spring Boot 集成

```xml
<dependency>
    <groupId>cn.itcraft.dbmmap</groupId>
    <artifactId>dbmmap-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

```yaml
dbmmap:
  memory-pool:
    pre-alloc-mem-size: 64MB
    query-cache-size: 4MB
  cache-level: L1
  statement-cache-size: 256
```

```java
@Autowired
private Session session;

public List<User> getUsers() {
    return session.query("SELECT * FROM users", result -> {
        List<User> users = new ArrayList<>();
        while (result.next()) {
            users.add(new User(
                result.getLong("ID"),
                result.getString("NAME"),
                result.getInt("AGE")
            ));
        }
        return users;
    });
}
```

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                        Session API                           │
├─────────────────────────────────────────────────────────────┤
│  query()         queryWithHandler()         update()        │
└────────┬──────────────────┬──────────────────────┬──────────┘
         │                  │                      │
         ▼                  ▼                      ▼
┌────────────────┐  ┌────────────────┐  ┌────────────────────┐
│  QueryResult   │  │  RowHandler    │  │  PreparedStatement │
│  (对象模式)     │  │  (流式模式)    │  │  (更新操作)        │
└────────┬───────┘  └────────┬───────┘  └────────────────────┘
         │                   │
         ▼                   ▼
┌─────────────────────────────────────────────────────────────┐
│                      MemoryPool                              │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐        │
│  │ Block 0 │  │ Block 1 │  │ Block 2 │  │ Block N │        │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘        │
│                   堆外内存 (Off-Heap)                        │
└─────────────────────────────────────────────────────────────┘
```

## 模块说明

| 模块 | 说明 |
|------|------|
| dbmmap-core | 核心实现：内存管理、查询执行、实体映射 |
| dbmmap-benchmark | JMH 性能基准测试 |
| dbmmap-spring-boot-starter | Spring Boot 自动配置 |

## 构建 & 测试

```bash
# 编译
mvn clean compile

# 运行测试
mvn test

# 运行基准测试
cd dbmmap-benchmark
mvn exec:exec -Dexec.executable="java" \
    -Dexec.args="-cp %classpath org.openjdk.jmh.Main NoCacheComparisonBenchmark -wi 10 -i 5 -t 4 -f 3"
```

## License

MIT License