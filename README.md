# jrdb

**J**ava **R**apid **D**ata**B**ase - Java 高性能数据库操作框架

基于堆外内存 + ASM 动态映射实现，查询性能领先 MyBatis **6-10倍**，写入性能领先 **14%**。

## 名称由来

- **J**ava - 专为 Java 平台设计
- **R**apid - 极致的查询性能
- **D**ata**B**ase - 数据库操作框架

**定位：读写均衡型 ORM 框架，查询性能领先，写入不弱于主流框架**

## 特性

- **堆外内存存储** - 使用 Unsafe 直接操作堆外内存，避免 GC 压力
- **ASM 动态映射** - 运行时生成字节码，实体映射性能接近原生 JDBC
- **RowHandler 流式处理** - 零对象创建，适合 ETL/数据导出场景
- **多级缓存** - 支持 L1(Statement缓存)/L2(结果缓存)
- **Spring Boot 集成** - 提供 Starter 自动配置

## 性能基准

### 查询性能 (NoCache, 单线程)

#### 单条查询

| 框架 | ops/s | vs MyBatis | vs Hibernate |
|------|-------|------------|--------------|
| **jrdb_asm** | **2,178,743** | **+8.3x** | +11.7x |
| **jrdb** | **1,583,364** | **+6.0x** | +8.5x |
| myBatis | 261,793 | baseline | - |
| hibernate | 185,584 | - | baseline |

#### 批量查询

| 框架 | ops/s | vs MyBatis | vs Hibernate |
|------|-------|------------|--------------|
| **jrdb_asm** | **228,209** | **+10.0x** | +9.6x |
| **jrdb** | **156,855** | **+6.8x** | +6.6x |
| myBatis | 22,906 | baseline | - |
| hibernate | 23,709 | - | baseline |

### 写入性能 (L1 Cache, 单线程)

#### 单条 Insert

| 框架 | ops/s | vs MyBatis | vs JDBC |
|------|-------|------------|---------|
| jdbc_manual | 808,158 | +32% | baseline |
| **jrdb_asm** | **791,949** | **+29%** | -2% |
| **jrdb_raw** | **700,300** | **+14%** | -13% |
| myBatis | 611,578 | baseline | - |
| hibernate | 368,968 | -40% | - |

#### 批量操作 (100条/批)

| 操作 | jrdb_raw | JDBC | MyBatis | Hibernate |
|------|----------|------|---------|-----------|
| batchInsert | 6,039 | 6,718 | 8,350 | 6,432 |
| batchUpdate | **5,450** | 5,702 | 5,109 | 3,142 |
| batchDelete | 5,633 | 5,830 | 5,982 | 4,158 |

### 定位总结

| 场景 | jrdb 优势 | 说明 |
|------|-------------|------|
| **SELECT 单条** | ✅ **6-8x MyBatis** | 堆外内存 + ASM 映射 |
| **SELECT 批量** | ✅ **7-10x MyBatis** | 零拷贝 + 紧凑存储 |
| **INSERT** | ✅ **+14% MyBatis** | PS复用 + 类型优化 |
| **UPDATE/DELETE** | ✅ **≈MyBatis** | JDBC 批量操作 |

**定位：读写均衡型框架，查询性能领先，写入不弱于主流 ORM**

## 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>cn.itcraft.jrdb</groupId>
    <artifactId>jrdb-core</artifactId>
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
JrdbConfig config = JrdbConfig.builder()
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

### 单条 Insert（返回主键）

```java
// 插入并返回生成的主键
Long id = session.insert(
    "INSERT INTO users (name, age, email) VALUES (?, ?, ?)",
    "Alice", 25, "alice@test.com"
);
```

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

### 性能对比 (100条/批, 单线程)

| 操作 | jrdb_raw | JDBC | MyBatis | Hibernate |
|------|----------|------|---------|-----------|
| batchInsert | 6,039 | 6,718 | 8,350 | 6,432 |
| batchUpdate | **5,450** | 5,702 | 5,109 | 3,142 |
| batchDelete | 5,633 | 5,830 | 5,982 | 4,158 |

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
| jrdb_rowHandler | **226K** | +93% | 零对象创建 |
| jrdb_rawQuery | 117K | baseline | QueryResult对象 |
| jdbc_manual | 330K | +182% | 原生JDBC |

### 使用方法

```java
// 定义 RowHandler
session.queryWithHandler("SELECT id, name, age FROM users", (rawRow, meta) -> {
    // rawRow: 原始字节数组，按列顺序存储
    // meta: 行元信息，包含列定义和偏移量
    
    // 使用 RowMeta 便捷方法提取数据
    long id = meta.extractLong(rawRow, 0);
    String name = meta.extractString(rawRow, 1);
    int age = meta.extractInt(rawRow, 2);
    
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

### 工具方法 (RowMeta API)

```java
session.queryWithHandler("SELECT id, name, age FROM users", (rawRow, meta) -> {
    // 使用 RowMeta 提供的便捷方法
    long id = meta.extractLong(rawRow, 0);
    String name = meta.extractString(rawRow, 1);
    int age = meta.extractInt(rawRow, 2);
    double salary = meta.extractDouble(rawRow, 3);
    boolean active = meta.extractBoolean(rawRow, 4);
    
    // 或根据列类型自动提取
    Object value = meta.extractValue(rawRow, 0);
});
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
    ColumnMeta col = meta.getColumn(colIndex);
    
    switch (col.getSqlType()) {
        case Types.INTEGER:
        case Types.TINYINT:
        case Types.SMALLINT:
            sb.append(meta.extractInt(rawRow, colIndex));
            break;
        case Types.BIGINT:
            sb.append(meta.extractLong(rawRow, colIndex));
            break;
        case Types.DOUBLE:
        case Types.FLOAT:
            sb.append(meta.extractDouble(rawRow, colIndex));
            break;
        case Types.VARCHAR:
        case Types.CHAR:
            String str = meta.extractString(rawRow, colIndex);
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
    <groupId>cn.itcraft.jrdb</groupId>
    <artifactId>jrdb-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

```yaml
jrdb:
  enabled: true
  overflow-strategy: ERROR  # ERROR 或 TRUNCATE
  memory-pool:
    pre-alloc-mem-size: 64MB
    query-cache-size: 4MB
  cache:
    level: L1
    statement-cache-size: 256
    result-cache-size: 1000
    result-cache-expire-seconds: 300
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
| jrdb-core | 核心实现：内存管理、查询执行、实体映射 |
| jrdb-benchmark | JMH 性能基准测试 |
| jrdb-spring-boot-starter | Spring Boot 自动配置 |

## 构建 & 测试

```bash
# 编译
mvn clean compile

# 运行测试
mvn test

# 运行基准测试
cd jrdb-benchmark
mvn exec:exec -Dexec.executable="java" \
    -Dexec.args="-cp %classpath org.openjdk.jmh.Main NoCacheComparisonBenchmark -wi 10 -i 5 -t 4 -f 3"
```

## License

MIT License