# jrdb - Java Rapid DataBase

高性能 Java 数据库操作框架，基于堆外内存 + ASM 动态映射实现，查询性能领先 MyBatis 2-6 倍。

**定位：读优化型 ORM 框架，适合读多写少场景**

**API 层次：**
1. **ASM API** (主推) - `queryForObject`/`queryForList`，直接返回实体
2. **Streaming API** (主推) - `queryWithHandler`，零对象创建，ETL 场景
3. **Raw API** (底层能力) - `query` 返回 QueryResult，精细控制

## Build Commands

```bash
# 编译项目
mvn compile

# 编译指定模块
mvn compile -pl jrdb-core

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
mvn test -pl jrdb-core

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

# 运行指定 benchmark（快速验证）
java -cp "target/jrdb-benchmark-1.0.0-SNAPSHOT.jar:target/dependency/*" \
     org.openjdk.jmh.Main NoCacheComparisonBenchmark -wi 3 -i 3 -t 1 -f 1

# 运行标准 benchmark
java -cp "target/jrdb-benchmark-1.0.0-SNAPSHOT.jar:target/dependency/*" \
     org.openjdk.jmh.Main NoCacheComparisonBenchmark -wi 10 -i 5 -t 4 -f 3
```

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

### Types & Generics

- 字段不可变时标记为 `final`
- 正确使用泛型：`EntityMapper<T>`, `List<User>`
- 高并发计数使用 `LongAdder` 而非 `AtomicLong`
- 避免使用 `Map` 传参

### Error Handling

- 使用 `Objects.requireNonNull()` 验证参数
- 抛出描述性异常：`IllegalArgumentException`, `IllegalStateException`
- 异常消息包含参数名："config cannot be null"
- 异常日志三参数：`log.error("msg: {}", e.getMessage(), e)`

### Thread Safety

- 静态可变变量使用 `ConcurrentHashMap`
- `SimpleDateFormat` 等非线程安全类使用 `ThreadLocal`
- 共享资源访问必须同步
- 循环退出使用 `volatile boolean` 标志位

### Resource Management

- 使用 try-with-resources 管理资源
- 实现了 `AutoCloseable` 的类必须关闭

## Project Structure

```
jrdb/
├── jrdb-core/              # 核心模块
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
├── jrdb-benchmark/         # JMH 基准测试
└── jrdb-spring-boot-starter/  # Spring Boot 集成
```

## Key Classes

| 类 | 职责 |
|---|------|
| `Session` | 核心会话接口，提供查询/更新 API |
| `SessionFactory` | 创建 Session，管理 MapperRegistry |
| `MemoryPool` | 堆外内存池管理 |
| `MapperRegistry` | ASM 映射器注册表 |
| `EntityMapper<T>` | 实体映射器接口 |

## Git Conventions

- 使用简体中文交互
- 遵循约定式提交规范（Conventional Commits）
- 提交类型：`feat`/`fix`/`refactor`/`docs`/`test`
- 示例：`feat: 添加ASM实体查询API`
- 仅以当前 `user.name` 提交，不推送到远端

## Documentation

- 编码规范：`/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/spec.java.md`
- 构建工具：`/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/ci.java.md`
- 环境信息：执行 `skill /java-env` 获取