# dbmmap 设计文档

> 数据库内存映射框架 - 高性能堆外内存存储

## 目标

实现一套基于堆外内存的数据库映射框架，绕过ORM对象开销，提供行存/列存混合模式，追求极致查询性能。

## 架构概览

```
┌─────────────────────────────────────────────────────────┐
│                      dbmmap                             │
├─────────────────────────────────────────────────────────┤
│  API Layer                                              │
│  ├── Query API (getRecord/getInt/getColumn/迭代器)      │
│  ├── Write API (SQL透传)                                │
│  ├── Transaction API                                    │
│  └── Session (资源管理)                                 │
├─────────────────────────────────────────────────────────┤
│  Mapping Layer                                          │
│  ├── Mapper接口 (XML/注解/API配置)                       │
│  ├── TypeHandler (类型转换扩展点)                        │
│  ├── NameStrategy (命名规则扩展点)                        │
│  └── ASM动态生成 (POJO映射)                              │
├─────────────────────────────────────────────────────────┤
│  SQL Builder Layer                                      │
│  ├── String Builder (简单拼接)                           │
│  ├── Template Engine (类似MyBatis)                      │
│  └── DSL Builder (流式API)                              │
├─────────────────────────────────────────────────────────┤
│  Memory Pool Layer                                      │
│  ├── MemoryPool (总控)                                  │
│  │   ├── preAllocMemSize (总预分配)                      │
│  │   └── queryCacheSize (单查询槽位)                     │
│  ├── SlotAllocator (栈式+位图)                          │
│  └── DirectBuffer管理                                   │
├─────────────────────────────────────────────────────────┤
│  Storage Layer                                          │
│  ├── RowStore (行存)                                    │
│  ├── ColumnStore (列存)                                │
│  └── HybridView (混合视图切换)                          │
├─────────────────────────────────────────────────────────┤
│  JDBC Layer                                             │
│  └── DataSource适配 (HikariCP/Druid/内置)              │
└─────────────────────────────────────────────────────────┘
```

## 模块划分

| 模块 | 职责 |
|------|------|
| dbmmap-core | 核心：内存池、存储、查询API |
| dbmmap-mapping | 映射：ASM、Mapper、TypeHandler |
| dbmmap-sql | SQL构建：Builder、Template、DSL |
| dbmmap-transaction | 事务：独立事务API |
| dbmmap-config | 配置：XML解析、配置对象 |

## 核心设计

### 1. 内存池设计

**配置参数：**
- `preAllocMemSize`: 总预分配内存大小
- `queryCacheSize`: 单查询槽位大小
- 关系: `preAllocMemSize = queryCacheSize * n` (n为槽位数量)

**内存管理：**
```
MemoryPool
├── long[] slots (位图标记: 0=unused, 1=used)
├── long baseAddress (Unsafe基地址)
├── allocate() → long address + int slotId
└── release(int slotId) → 标记unused
```

**分配策略：** 简单栈式分配 + 位图标记

### 2. 数据加载策略

**Cache限制：**
- 设置cache大小
- 最多存储 `cache/record_size` 条数据
- 分页同样受此限制

**超限行为（可配置）：**
- 默认：抛异常，让用户缩小查询范围
- 可选：截断加载，只加载前N条，警告用户

### 3. 存储格式

**行存（RowStore）：**
- 格式: `[row1][row2][row3]...`
- 每行紧凑排列
- 适合OLTP场景，单行读取快

**列存（ColumnStore）：**
- 格式: `[col1_all][col2_all]...`
- 按列重排
- 适合OLAP场景，聚合计算快

**混合模式：**
- 默认行存
- 提供 `toColumnView()` 切换到列存视图

### 4. 查询生命周期

**必须显式释放内存：**

```java
// 方式1: try-with-resources
try (QueryResult result = session.query(sql)) {
    result.getInt("id");
} // 自动release

// 方式2: 显式释放
QueryResult result = session.query(sql);
result.getInt("id");
result.release();

// 方式3: 回调
session.query(sql, result -> {
    result.getInt("id");
    return null;
}); // 自动release
```

### 5. 类型映射

**Phase 1: JDBC基础类型**
- int, long, short, byte, float, double
- boolean
- String
- Date, Time, Timestamp
- byte[]

**Phase 2: 扩展常用类型**
- 枚举
- BigDecimal
- Java 8时间API: LocalDate, LocalDateTime, LocalTime, Instant
- Optional

**Phase 3: 自定义类型**
- 提供TypeHandler扩展接口
- 用户可注册自定义类型转换器

### 6. POJO映射

**映射方式（多方式并存）：**
1. **XML配置** - 类似MyBatis的Mapper XML
2. **注解配置** - `@Table`, `@Column`, `@Id`等
3. **API配置** - `map(User.class).field("id", "user_id")...`

**命名规则（可配置，多态）：**
- 默认：下划线转驼峰 (`user_id` → `userId`)
- 可自定义实现

**实现方式：** ASM动态生成映射代码

### 7. SQL构建

**四种方式并存：**
1. **字符串构建器** - 简单StringBuilder封装，链式拼接
2. **模板引擎** - 类似MyBatis的XML或注解模板
3. **DSL构建器** - 流式API构建SQL，类型安全
4. **原生SQL** - 直接传递SQL字符串

### 8. 事务管理

**独立事务API：**
- 显式调用：`session.beginTransaction()`, `tx.commit()`, `tx.rollback()`
- 方便Spring等框架集成
- 不做连接池管理，透传JDBC

### 9. 写操作处理

**Insert/Update/Delete：**
- 透传JDBC执行
- 不做对象管理
- 不做内存管理
- 支持SQL拼接能力

## 非功能性需求

### 性能
- 堆外内存避免大对象触发Full GC
- 零拷贝（JDBC Type4不支持，数据需拷贝）
- 栈式内存分配，O(1)复杂度

### 可扩展性
- TypeHandler扩展点
- NameStrategy扩展点
- Mapper配置多方式

### 可用性
- 多种资源释放方式
- 清晰的错误提示
- 完善的日志

## 技术栈

- Java 8+
- sun.misc.Unsafe (堆外内存)
- ASM (动态代码生成)
- JDBC (数据库访问)
- Maven (构建)

## 项目结构

```
dbmmap/
├── dbmmap-core/           # 核心模块
│   ├── memory/            # 内存池
│   ├── storage/           # 存储层
│   ├── session/           # 会话管理
│   └── query/             # 查询API
├── dbmmap-mapping/        # 映射模块
│   ├── mapper/            # Mapper接口
│   ├── type/              # 类型处理
│   ├── name/              # 命名策略
│   └── asm/               # ASM生成
├── dbmmap-sql/            # SQL构建模块
│   ├── builder/           # 字符串构建
│   ├── template/          # 模板引擎
│   └── dsl/               # DSL构建
├── dbmmap-transaction/    # 事务模块
│   └── tx/                # 事务API
├── dbmmap-config/         # 配置模块
│   ├── xml/               # XML解析
│   └── config/            # 配置对象
└── dbmmap-all/            # 聚合模块
```

## 里程碑

**Phase 1: 核心基础设施**
- 内存池管理
- 基础存储（行存）
- 基础查询API
- JDBC连接

**Phase 2: 映射能力**
- 类型处理
- POJO映射
- 命名策略

**Phase 3: SQL构建**
- 字符串构建器
- 模板引擎
- DSL构建

**Phase 4: 高级特性**
- 列存视图
- 事务API
- 性能优化