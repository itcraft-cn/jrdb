# jrdb

**J**ava **R**apid **D**ata**B**ase - Java 高性能数据库操作框架

基于堆外内存 + ASM 动态映射实现，查询性能领先 MyBatis **6-10倍**，写入性能领先 **14%**。

## 名称由来

- **J**ava - 专为 Java 平台设计
- **R**apid - 极致的查询性能
- **D**ata**B**ase - 数据库操作框架

**定位：读写均衡型 ORM 框架，查询性能领先，写入不弱于主流框架**

## 模块说明

| 模块 | 说明 |
|------|------|
| **jrdb-jdbc** | 同步 JDBC 实现，追求极致性能 |
| **jrdb-r2dbc** | 响应式 R2DBC 实现，融入响应式生态 |
| **jrdb-common** | 公共模块 |
| **jrdb-benchmark** | JMH 性能基准测试 |
| **jrdb-spring-boot-starter** | Spring Boot 自动配置 |

## 特性

### jrdb-jdbc (同步)

- **堆外内存存储** - 使用 Unsafe 直接操作堆外内存，避免 GC 压力
- **ASM 动态映射** - 运行时生成字节码，实体映射性能接近原生 JDBC
- **RowHandler 流式处理** - 零对象创建，适合 ETL/数据导出场景
- **多级缓存** - 支持 L1(Statement缓存)/L2(结果缓存)
- **Spring Boot 集成** - 提供 Starter 自动配置

### jrdb-r2dbc (响应式)

- **响应式 API** - 返回 `CompletableFuture`，融入响应式生态
- **连接池支持** - 基于 r2dbc-pool，高效连接复用
- **ASM 实体映射** - 与 JDBC 版本相同的映射性能
- **批量操作支持** - 批量插入/更新/删除

## 性能基准

### jrdb-jdbc vs MyBatis/Hibernate

#### 查询性能 (NoCache, 单线程)

| 框架 | 单条查询 ops/s | 批量查询 ops/s |
|------|---------------|---------------|
| **jrdb_asm** | **2,178,743** (+8.3x) | **228,209** (+10.0x) |
| **jrdb** | **1,583,364** (+6.0x) | **156,855** (+6.8x) |
| myBatis | 261,793 | 22,906 |
| hibernate | 185,584 | 23,709 |

### jrdb-jdbc vs jrdb-r2dbc

| 方式 | 吞吐量 (ops/s) | 说明 |
|------|---------------|------|
| jdbc_raw | ~14,000 | 同步阻塞，直接调用 |
| jdbc_asm | ~14,000 | ASM 映射，接近原生 |
| r2dbc_raw | ~4,000 | 响应式，框架开销 |
| r2dbc_asm | ~3,500 | 响应式 + ASM 映射 |

**结论：R2DBC 比 JDBC 慢约 3.5x，这是响应式框架的必然开销。**

**适用场景：**
- **jrdb-jdbc**：同步场景，追求极致性能
- **jrdb-r2dbc**：异步非阻塞场景，高并发、IO 密集型，与 Spring WebFlux 配合

## 快速开始

### jrdb-jdbc

```xml
<dependency>
    <groupId>cn.itcraft.jrdb</groupId>
    <artifactId>jrdb-jdbc</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
import cn.itcraft.jrdb.jdbc.config.JrdbConfig;
import cn.itcraft.jrdb.jdbc.session.Session;
import cn.itcraft.jrdb.jdbc.session.SessionFactory;

// 创建 SessionFactory
JrdbConfig config = JrdbConfig.builder().build();
SessionFactory factory = new SessionFactory(config, dataSource);

try (Session session = factory.openSession()) {
    // Raw API - 返回 QueryResult
    try (QueryResult result = session.query("SELECT * FROM users WHERE id = ?", 1L)) {
        if (result.next()) {
            long id = result.getLong(1);
        }
    }
    
    // ASM API - 直接返回实体
    User user = session.queryForObject("SELECT * FROM users WHERE id = ?", User.class, 1L);
    List<User> users = session.queryForList("SELECT * FROM users", User.class);
    
    // 插入返回主键
    Long id = session.insert("INSERT INTO users (name) VALUES (?)", Long.class, "Alice");
}
```

### jrdb-r2dbc

```xml
<dependency>
    <groupId>cn.itcraft.jrdb</groupId>
    <artifactId>jrdb-r2dbc</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
import cn.itcraft.jrdb.r2dbc.session.R2dbcSession;
import cn.itcraft.jrdb.r2dbc.session.R2dbcSessionImpl;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import java.util.concurrent.CompletableFuture;

// 创建连接池
ConnectionPool pool = ConnectionPoolConfiguration.builder(connectionFactory)
    .maxSize(10)
    .build();

// 创建 Session
R2dbcSession session = new R2dbcSessionImpl(pool);

// 查询 - 返回 CompletableFuture
CompletableFuture<User> user = session.queryOne("SELECT * FROM users WHERE id = $1", User.class, 1L);
CompletableFuture<List<User>> users = session.queryList("SELECT * FROM users", User.class);

// 更新
CompletableFuture<Integer> rows = session.update("UPDATE users SET name = $1 WHERE id = $2", "Bob", 1L);

// 插入返回主键
CompletableFuture<Long> id = session.insertWithKey("INSERT INTO users (name) VALUES ($1)", Long.class, "Alice");

// 批量操作
CompletableFuture<int[]> results = session.batch("UPDATE users SET name = $1 WHERE id = $2", paramsList);

// 关闭
session.close();
pool.dispose();
```

## API 对比

### jrdb-jdbc API

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `query(sql, params)` | `QueryResult` | Raw API，返回堆外内存结果 |
| `queryForObject(sql, entityClass, params)` | `<T> T` | ASM API，返回单个实体 |
| `queryForList(sql, entityClass, params)` | `<T> List<T>` | ASM API，返回实体列表 |
| `queryWithHandler(sql, handler, params)` | `void` | Streaming API，流式处理 |
| `insert(sql, keyType, params)` | `<T> T` | 插入返回主键 |
| `update(sql, params)` | `int` | 更新返回影响行数 |
| `delete(sql, params)` | `int` | 删除返回影响行数 |

### jrdb-r2dbc API

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `queryOne(sql, typeClass, params)` | `CompletableFuture<T>` | 查询单个值 |
| `queryList(sql, typeClass, params)` | `CompletableFuture<List<T>>` | 查询列表 |
| `queryOne(sql, mapper, params)` | `CompletableFuture<T>` | 自定义映射查询单个 |
| `queryList(sql, mapper, params)` | `CompletableFuture<List<T>>` | 自定义映射查询列表 |
| `update(sql, params)` | `CompletableFuture<Integer>` | 更新 |
| `insert(sql, params)` | `CompletableFuture<Boolean>` | 插入 |
| `insertWithKey(sql, keyType, params)` | `CompletableFuture<T>` | 插入返回主键 |
| `delete(sql, params)` | `CompletableFuture<Integer>` | 删除 |
| `batch(sql, paramsList)` | `CompletableFuture<int[]>` | 批量操作 |

## 构建 & 测试

```bash
# 编译
mvn clean compile

# 运行所有测试
mvn test

# 运行指定模块测试
mvn test -pl jrdb-jdbc
mvn test -pl jrdb-r2dbc

# 运行基准测试
cd jrdb-benchmark
mvn clean package -DskipTests
java -cp "target/jrdb-benchmark-1.0.0-SNAPSHOT.jar:target/dependency/*" \
     org.openjdk.jmh.Main MappingPerformanceBenchmark -wi 3 -i 3 -t 2 -f 1
```

## License

MIT License