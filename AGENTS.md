# jrdb - Java Rapid DataBase

高性能 Java 数据库操作框架，基于堆外内存 + ASM 动态映射实现。

## 模块结构

```
jrdb/
├── jrdb-jdbc/              # 同步 JDBC 实现（追求极致性能）
│   ├── cache/              # 缓存实现
│   ├── config/             # 配置类
│   ├── executor/           # 查询执行器
│   ├── handler/            # RowHandler 流式处理
│   ├── mapping/            # ASM 实体映射
│   ├── memory/             # 堆外内存管理
│   ├── query/              # 查询结果
│   ├── session/            # 会话管理
│   ├── sql/                # SQL 构建
│   ├── storage/            # 存储
│   ├── transaction/        # 事务
│   └── type/               # 类型处理
├── jrdb-r2dbc/             # 响应式 R2DBC 实现
│   ├── config/             # 配置类
│   ├── executor/           # 查询执行器
│   ├── mapping/            # ASM 实体映射
│   └── session/            # 会话管理
├── jrdb-common/            # 公共模块
├── jrdb-benchmark/         # JMH 基准测试
└── jrdb-spring-boot-starter/  # Spring Boot 集成
```

## Build Commands

```bash
# 编译项目
mvn compile

# 编译指定模块
mvn compile -pl jrdb-jdbc
mvn compile -pl jrdb-r2dbc

# 清理并编译
mvn clean compile

# 打包（跳过测试）
mvn package -DskipTests

# 安装到本地仓库
mvn install -DskipTests
```

## Test Commands

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

## Benchmark Commands

```bash
# 编译 benchmark
cd jrdb-benchmark && mvn clean package -DskipTests

# 运行 JDBC vs R2DBC 对比 benchmark
java -cp "target/jrdb-benchmark-1.0.0-SNAPSHOT.jar:target/dependency/*" \
     org.openjdk.jmh.Main MappingPerformanceBenchmark -wi 5 -i 3 -t 2 -f 1

# 运行框架对比 benchmark
java -cp "target/jrdb-benchmark-1.0.0-SNAPSHOT.jar:target/dependency/*" \
     org.openjdk.jmh.Main FrameworkComparisonBenchmark -wi 10 -i 5 -t 4 -f 3
```

## API 层次

### jrdb-jdbc (同步)

1. **ASM API** (主推) - `queryForObject`/`queryForList`，直接返回实体
2. **Streaming API** (主推) - `queryWithHandler`，零对象创建，ETL 场景
3. **Raw API** (底层能力) - `query` 返回 QueryResult，精细控制

### jrdb-r2dbc (响应式)

1. **queryOne** - 查询单个值，返回 `CompletableFuture<T>`
2. **queryList** - 查询列表，返回 `CompletableFuture<List<T>>`
3. **update/insert/delete** - 更新操作，返回 `CompletableFuture<Integer>`
4. **batch** - 批量操作，返回 `CompletableFuture<int[]>`

## 性能参考

### jrdb-jdbc vs MyBatis/Hibernate

- 单条查询：**+6-8x MyBatis**
- 批量查询：**+7-10x MyBatis**
- 插入操作：**+14% MyBatis**

### jrdb-jdbc vs jrdb-r2dbc

- JDBC：~14,000 ops/s
- R2DBC：~4,000 ops/s (差距 3.5x)

**结论：R2DBC 慢于 JDBC 是响应式框架的必然开销，价值在于高并发非阻塞场景。**

## Code Style Guidelines

### Imports

- 分组导入：先 `java.*`，再 `javax.*`，然后第三方库，最后项目内包
- 不使用 `import xxx.*`，导入具体类
- 删除未使用的 import

### Formatting

- 4 空格缩进，不使用 Tab
- 花括号与语句在同一行：`public void method() {`
- 最大行长度 120 字符
- 方法间空一行

### Naming Conventions

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | PascalCase | `SessionImpl`, `MemoryPool` |
| 方法名 | camelCase | `queryForObject`, `getMemoryPool` |
| 变量名 | camelCase | `memoryPool`, `dataSource` |
| 常量 | UPPER_SNAKE | `DEFAULT_ROW_BUFFER_SIZE` |
| 布尔方法 | is/has 前缀 | `isL1CacheEnabled`, `hasMapper` |

### Comments

- 类注释：说明职责、特性、使用示例
- 方法注释：说明参数、返回值、异常
- **禁止行尾注释**
- 核心代码注释放在代码块上方

### Error Handling

- 使用 `Objects.requireNonNull()` 验证参数
- 抛出描述性异常：`IllegalArgumentException`, `IllegalStateException`
- 异常消息包含参数名："config cannot be null"
- 异常日志三参数：`log.error("msg: {}", e.getMessage(), e)`

### Resource Management

- 使用 try-with-resources 管理资源
- 实现了 `AutoCloseable` 的类必须关闭

## Key Classes

### jrdb-jdbc

| 类 | 职责 |
|---|------|
| `Session` | 核心会话接口，提供查询/更新 API |
| `SessionFactory` | 创建 Session，管理 MapperRegistry |
| `MemoryPool` | 堆外内存池管理 |
| `MapperRegistry` | ASM 映射器注册表 |
| `EntityMapper<T>` | 实体映射器接口 |

### jrdb-r2dbc

| 类 | 职责 |
|---|------|
| `R2dbcSession` | 响应式会话接口，返回 CompletableFuture |
| `R2dbcExecutor` | 查询执行器，基于 Reactor |
| `R2dbcMapperRegistry` | ASM 映射器注册表 |
| `R2dbcEntityMapper<T>` | 实体映射器接口 |

## Git Conventions

- 使用简体中文交互
- 遵循约定式提交规范（Conventional Commits）
- 提交类型：`feat`/`fix`/`refactor`/`docs`/`test`
- 示例：`feat: 添加R2DBC响应式支持`
- 仅以当前 `user.name` 提交，不推送到远端

## Documentation

- 编码规范：`/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/spec.java.md`
- 构建工具：`/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/ci.java.md`
- 环境信息：执行 `skill /java-env` 获取