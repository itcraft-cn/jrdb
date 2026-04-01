# jrdb

**J**ava **R**apid **D**ata**B**ase - High-performance Java database framework

Built on off-heap memory + ASM dynamic mapping, query performance leads MyBatis by **6-10x**, write performance leads by **14%**.

## Name Origin

- **J**ava - Designed for Java platform
- **R**apid - Extreme query performance
- **D**ata**B**ase - Database operation framework

**Positioning: Balanced read/write ORM framework, leading query performance, competitive write performance**

## Modules

| Module | Description |
|--------|-------------|
| **jrdb-jdbc** | Synchronous JDBC implementation, pursuing extreme performance |
| **jrdb-r2dbc** | Reactive R2DBC implementation, integrated into reactive ecosystem |
| **jrdb-common** | Common module |
| **jrdb-test** | Unit tests and JMH benchmarks |
| **jrdb-spring-boot-starter** | Spring Boot auto-configuration |

## Features

### jrdb-jdbc (Synchronous)

- **Off-heap Memory Storage** - Uses Unsafe for direct off-heap memory operations, avoiding GC pressure
- **ASM Dynamic Mapping** - Runtime bytecode generation, entity mapping performance close to native JDBC
- **RowHandler Streaming** - Zero object creation, suitable for ETL/data export scenarios
- **Multi-level Cache** - Supports L1(Statement cache)/L2(Result cache)
- **Spring Boot Integration** - Provides Starter auto-configuration

### jrdb-r2dbc (Reactive)

- **Reactive API** - Returns `CompletableFuture`, integrated into reactive ecosystem
- **Connection Pool Support** - Based on r2dbc-pool, efficient connection reuse
- **ASM Entity Mapping** - Same mapping performance as JDBC version
- **Batch Operation Support** - Batch insert/update/delete

## Performance Benchmarks

### jrdb-jdbc vs MyBatis/Hibernate

#### Query Performance (NoCache, Single Thread)

| Framework | Single Query ops/s | Batch Query ops/s |
|-----------|--------------------|-------------------|
| **jrdb_asm** | **2,178,743** (+8.3x) | **228,209** (+10.0x) |
| **jrdb** | **1,583,364** (+6.0x) | **156,855** (+6.8x) |
| myBatis | 261,793 | 22,906 |
| hibernate | 185,584 | 23,709 |

### jrdb-jdbc vs jrdb-r2dbc

| Method | Throughput (ops/s) | Description |
|--------|--------------------|-------------|
| jdbc_raw | ~14,000 | Synchronous blocking, direct call |
| jdbc_asm | ~14,000 | ASM mapping, close to native |
| r2dbc_raw | ~4,000 | Reactive, framework overhead |
| r2dbc_asm | ~3,500 | Reactive + ASM mapping |

**Conclusion: R2DBC is ~3.5x slower than JDBC, which is inevitable overhead of reactive frameworks.**

**Use Cases:**
- **jrdb-jdbc**: Synchronous scenarios, pursuing extreme performance
- **jrdb-r2dbc**: Async non-blocking scenarios, high concurrency, IO-intensive, works with Spring WebFlux

## Quick Start

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

JrdbConfig config = JrdbConfig.builder().build();
SessionFactory factory = new SessionFactory(config, dataSource);

try (Session session = factory.openSession()) {
    QueryResult result = session.query("SELECT * FROM users WHERE id = ?", 1L);
    if (result.next()) {
        long id = result.getLong(1);
    }
    
    User user = session.queryForObject("SELECT * FROM users WHERE id = ?", User.class, 1L);
    List<User> users = session.queryForList("SELECT * FROM users", User.class);
    
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

ConnectionPool pool = ConnectionPoolConfiguration.builder(connectionFactory)
    .maxSize(10)
    .build();

R2dbcSession session = new R2dbcSessionImpl(pool);

CompletableFuture<User> user = session.queryOne("SELECT * FROM users WHERE id = $1", User.class, 1L);
CompletableFuture<List<User>> users = session.queryList("SELECT * FROM users", User.class);

CompletableFuture<Integer> rows = session.update("UPDATE users SET name = $1 WHERE id = $2", "Bob", 1L);

CompletableFuture<Long> id = session.insertWithKey("INSERT INTO users (name) VALUES ($1)", Long.class, "Alice");

CompletableFuture<int[]> results = session.batch("UPDATE users SET name = $1 WHERE id = $2", paramsList);

session.close();
pool.dispose();
```

## Spring Boot Integration

### jrdb-jdbc Spring Boot

```xml
<dependency>
    <groupId>cn.itcraft.jrdb</groupId>
    <artifactId>jrdb-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver

jrdb:
  jdbc:
    enabled: true
    l1-cache-enabled: true
    l2-cache-enabled: false
```

```java
@Service
public class UserService {

    @Autowired
    private SessionFactory sessionFactory;

    public User getUser(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.queryForObject("SELECT * FROM users WHERE id = ?", User.class, id);
        }
    }
}
```

### jrdb-r2dbc Spring Boot

```xml
<dependency>
    <groupId>cn.itcraft.jrdb</groupId>
    <artifactId>jrdb-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

```yaml
spring:
  r2dbc:
    url: r2dbc:mysql://localhost:3306/mydb
    username: root
    password: password

jrdb:
  r2dbc:
    enabled: true
    initial-pool-size: 5
    max-pool-size: 10
    max-life-time-minutes: 30
    max-idle-time-minutes: 10
```

```java
@Service
public class UserService {

    @Autowired
    private R2dbcTemplate r2dbcTemplate;

    public CompletableFuture<User> getUser(Long id) {
        return r2dbcTemplate.queryOne("SELECT * FROM users WHERE id = $1", User.class, id);
    }

    public CompletableFuture<List<User>> getAllUsers() {
        return r2dbcTemplate.queryList("SELECT * FROM users", User.class);
    }

    public CompletableFuture<Integer> updateUser(Long id, String name) {
        return r2dbcTemplate.update("UPDATE users SET name = $1 WHERE id = $2", name, id);
    }

    public CompletableFuture<Long> createUser(String name) {
        return r2dbcTemplate.insertWithKey("INSERT INTO users (name) VALUES ($1)", Long.class, name);
    }

    public CompletableFuture<int[]> batchUpdateNames(List<Object[]> params) {
        return r2dbcTemplate.batch("UPDATE users SET name = $1 WHERE id = $2", params);
    }
}
```

## API Reference

### jrdb-jdbc API

| Method | Return | Description |
|--------|--------|-------------|
| `query(sql, params)` | `QueryResult` | Raw API, returns off-heap memory result |
| `queryForObject(sql, entityClass, params)` | `<T> T` | ASM API, returns single entity |
| `queryForList(sql, entityClass, params)` | `<T> List<T>` | ASM API, returns entity list |
| `queryWithHandler(sql, handler, params)` | `void` | Streaming API, flow processing |
| `insert(sql, keyType, params)` | `<T> T` | Insert returning primary key |
| `update(sql, params)` | `int` | Update returning affected rows |
| `delete(sql, params)` | `int` | Delete returning affected rows |

### jrdb-r2dbc API

| Method | Return | Description |
|--------|--------|-------------|
| `queryOne(sql, typeClass, params)` | `CompletableFuture<T>` | Query single value |
| `queryList(sql, typeClass, params)` | `CompletableFuture<List<T>>` | Query list |
| `queryOne(sql, mapper, params)` | `CompletableFuture<T>` | Custom mapping query single |
| `queryList(sql, mapper, params)` | `CompletableFuture<List<T>>` | Custom mapping query list |
| `update(sql, params)` | `CompletableFuture<Integer>` | Update |
| `insert(sql, params)` | `CompletableFuture<Boolean>` | Insert |
| `insertWithKey(sql, keyType, params)` | `CompletableFuture<T>` | Insert returning primary key |
| `delete(sql, params)` | `CompletableFuture<Integer>` | Delete |
| `batch(sql, paramsList)` | `CompletableFuture<int[]>` | Batch operations |

## Build & Test

```bash
mvn clean compile

mvn test

mvn test -pl jrdb-jdbc
mvn test -pl jrdb-r2dbc

cd jrdb-test
mvn clean package -DskipTests
java -cp "target/jrdb-test-1.0.0-SNAPSHOT.jar:target/dependency/*" \
     org.openjdk.jmh.Main MappingPerformanceBenchmark -wi 3 -i 3 -t 2 -f 1
```

## License

MIT License