# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

**jrdb** (Java Rapid Database) - 高性能 Java 数据库操作框架

- **核心特性**: 基于堆外内存 (Off-Heap Memory) + ASM 动态字节码生成
- **性能优势**: 查询性能领先 MyBatis **6-10 倍**，写入性能领先 **14%**
- **定位**: 读优化型 ORM 框架，适合读多写少、大数据量场景

## 模块结构

```
jrdb/
├── jrdb-jdbc/                 # 同步 JDBC 实现（核心模块）
│   ├── memory/                # 堆外内存管理 (MemoryPool, SlotAllocator, Unsafe)
│   ├── storage/               # 存储格式 (RowStore 行存/ColumnStore 列存)
│   ├── mapping/               # ASM 实体映射 (MapperRegistry, EntityMapper)
│   ├── session/               # 会话管理 (Session, SessionFactory)
│   ├── cache/                 # 缓存 (L1 Statement/L2 Result)
│   ├── sql/                   # SQL DSL (SqlDsl, SqlTemplate)
│   ├── type/                  # 类型处理器 (TypeHandlerRegistry)
│   └── transaction/           # 事务管理
├── jrdb-r2dbc/                # 响应式 R2DBC 实现
├── jrdb-spring-boot-starter/  # Spring Boot 自动配置
└── jrdb-test/                 # 单元测试 + JMH 基准测试
```

## 构建命令

```bash
# 编译项目
mvn clean compile

# 编译指定模块
mvn compile -pl jrdb-jdbc
mvn compile -pl jrdb-r2dbc

# 打包（跳过测试）
mvn package -DskipTests

# 安装到本地仓库
mvn install -DskipTests
```

## 测试命令

```bash
# 运行所有测试
mvn test

# 运行指定模块测试
mvn test -pl jrdb-jdbc
mvn test -pl jrdb-r2dbc

# 运行单个测试类
mvn test -Dtest=SessionImplTest

# 运行单个测试方法
mvn test -Dtest=SessionImplTest#testQuery

# 运行匹配模式的测试
mvn test -Dtest=*MapperTest
```

## 基准测试命令

```bash
# 编译 benchmark
cd jrdb-test && mvn clean package -DskipTests

# 运行 JMH 基准测试
java -cp "target/jrdb-test-1.0.0-SNAPSHOT.jar:target/dependency/*" \
     org.openjdk.jmh.Main MappingPerformanceBenchmark -wi 3 -i 3 -t 2 -f 1

# 运行框架对比测试
java -cp "target/jrdb-test-1.0.0-SNAPSHOT.jar:target/dependency/*" \
     org.openjdk.jmh.Main FrameworkComparisonBenchmark -wi 10 -i 5 -t 4 -f 3
```

## 核心 API

### jrdb-jdbc (同步)

```java
// 初始化
JrdbConfig config = JrdbConfig.builder().build();
SessionFactory factory = new SessionFactory(config, dataSource);

try (Session session = factory.openSession()) {
    // 1. ASM API (推荐) - 直接返回实体
    User user = session.queryForObject("SELECT * FROM users WHERE id = ?", User.class, 1L);
    List<User> users = session.queryForList("SELECT * FROM users", User.class);
    
    // 2. Streaming API (推荐) - 零对象创建，ETL 场景
    session.queryWithHandler("SELECT * FROM users", rowHandler -> {
        while (rowHandler.next()) {
            // 处理数据
        }
    });
    
    // 3. Raw API (底层) - 原始查询结果
    QueryResult result = session.query("SELECT * FROM users WHERE id = ?", 1L);
    if (result.next()) {
        long id = result.getLong("id");
    }
    
    // 4. 写入操作
    Long id = session.insert("INSERT INTO users (name) VALUES (?)", Long.class, "Alice");
    int rows = session.update("UPDATE users SET name = ? WHERE id = ?", "Bob", 1L);
    int deleted = session.delete("DELETE FROM users WHERE id = ?", 1L);
}
```

### jrdb-r2dbc (响应式)

```java
// 初始化
ConnectionPool pool = ConnectionPoolConfiguration.builder(connectionFactory)
    .maxSize(10).build();
R2dbcSession session = new R2dbcSessionImpl(pool);

// 查询
CompletableFuture<User> user = session.queryOne("SELECT * FROM users WHERE id = $1", User.class, 1L);
CompletableFuture<List<User>> users = session.queryList("SELECT * FROM users", User.class);

// 写入
CompletableFuture<Integer> rows = session.update("UPDATE users SET name = $1 WHERE id = $2", "Bob", 1L);
CompletableFuture<Long> id = session.insertWithKey("INSERT INTO users (name) VALUES ($1)", Long.class, "Alice");
CompletableFuture<int[]> batch = session.batch("UPDATE users SET name = $1 WHERE id = $2", paramsList);
```

## 核心数据流

### 查询流程
```
SQL → Session → PreparedStatement → ResultSet 
    → MemoryPool.allocate() → RowStore写入 
    → QueryResultImpl → (可选 L2 缓存) → 返回
```

### 内存布局
```
MemoryBlock (行式存储):
┌─────────────────────────────────────┐
│ Row0: [Col0][Col1]...[ColN]         │
│ Row1: [Col0][Col1]...[ColN]         │
│ ...                                 │
└─────────────────────────────────────┘
offset = currentRow * rowSize + columnOffsets[columnIndex]
```

### ASM 映射流程
```
MapperRegistry.getMapper(entityClass)
  → 扫描字段 → 推断表名 (驼峰转下划线)
  → MapperClassGenerator(ASM 生成字节码)
  → MapperClassLoader.defineClass()
  → 缓存并返回 EntityMapper 实例
```

## 关键设计决策

| 决策 | 理由 | 代价 |
|------|------|------|
| **堆外内存** | 避免 GC 压力，突破堆大小限制 | 需手动管理释放 |
| **内存池化** | 预分配 + 槽位管理，减少运行时分配 | 内存预占用 |
| **ASM 字节码** | 避免反射，JVM 可内联优化 | 字节码生成开销 |
| **两级缓存** | L1: Statement 复用; L2: 结果缓存 | 缓存一致性 |
| **行存优先** | OLTP 场景按行访问，写入更快 | 列存需转换 |

## 性能基准 (NoCache, 4 线程)

### 查询性能 (ops/s)
| 操作 | jrdb_asm | jrdb_raw | MyBatis | Hibernate |
|------|----------|----------|---------|-----------|
| 单条查询 | **2.56M** | 2.16M | 1.18M | 937K |
| 批量查询 (100 条) | **702K** | 482K | 108K | 89K |
| RowHandler | **226K** | 117K | - | - |

### 写入性能 (ops/s)
| 操作 | jrdb_raw | MyBatis | Hibernate |
|------|----------|---------|-----------|
| 单条 Insert | 630K | **677K** | 416K |
| 批量 Insert(100 条) | 7.18K | **7.56K** | 6.29K |

## Spring Boot 配置

### jrdb-jdbc
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: password

jrdb:
  jdbc:
    enabled: true
    l1-cache-enabled: true
    l2-cache-enabled: false
```

### jrdb-r2dbc
```yaml
spring:
  r2dbc:
    url: r2dbc:mysql://localhost:3306/mydb
    username: root
    password: password

jrdb:
  r2dbc:
    enabled: true
    max-pool-size: 10
```

## 编码规范

- **语言**: 简体中文交互
- **Git**: 约定式提交 (Conventional Commits)
- **格式**: 4 空格缩进，120 字符行宽
- **命名**: 类 PascalCase, 方法/变量 camelCase, 常量 UPPER_SNAKE
- **异常**: 使用 `IllegalArgumentException`, `IllegalStateException`
- **资源**: try-with-resources 管理 `AutoCloseable`

## 外部规范

- **编码规范**: `/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/spec.java.md`
- **构建工具**: `/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/ci.java.md`
- **环境信息**: 执行 `skill /java-env` 获取

## 技术栈

- **JDK**: 1.8+
- **ASM**: 9.6 (字节码生成)
- **Guava**: 32.1.3-jre (缓存工具)
- **SLF4J**: 1.7.36 (日志)
- **Spring Boot**: 2.7.18 (自动配置)
- **R2DBC**: 1.0.0.RELEASE (响应式)
- **JMH**: 1.37 (基准测试)
- **H2**: 2.2.224 (测试数据库)

## 文档索引

- `docs/deconstruct/` - 需求分析/设计文档 (23 个文件)
- `docs/review/` - 代码走查报告
- `docs/superpowers/` - 实现计划
- `README.md` / `README_cn.md` - 用户文档
- `MEMORY.md` - 项目进度和性能总结
