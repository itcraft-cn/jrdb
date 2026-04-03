# jrdb TODO List

## 缓存优化

### 缓存失效策略优化

**问题**: 当前缓存失效采用一刀切模式，任何更新/插入/删除操作都会清空全部 L2 缓存。

**现状**:
```java
// SessionImpl.java - 任何写操作都调用
if (resultCache != null) {
    resultCache.invalidateAll();  // 一刀切清空
}
```

**问题场景**:
1. 更新 `user` 表，缓存了 `order` 表的查询结果也被误删
2. 批量操作场景性能差 - 循环内多次更新，每次都清空全部缓存
3. 高并发写场景下 L2 缓存命中率极低

**改进方案**:

| 方案 | 描述 | 优先级 |
|------|------|--------|
| SQL 解析识别表名 | 解析 SQL 得到目标表，只失效该表相关缓存 | P1 |
| 表级失效 API | 提供 `invalidateTable(String tableName)` 方法 | P2 |
| 手动精细控制 | 框架只自动失效，用户可额外调用 API 精细管理 | P2 |
| 缓存标签 | 为缓存 key 添加表标签，支持按标签失效 | P3 |

**实现示例 (方案 1)**:
```java
// 解析 SQL 得到表名
String tableName = extractTableName(sql);

// 只失效与该表相关的缓存 key
resultCache.invalidateByTable(tableName);
```

**涉及文件**:
- `jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/session/SessionImpl.java`
- `jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/cache/QueryResultCache.java`

---

## 代码结构优化

### SessionImpl 类拆分

**问题**: `SessionImpl.java` 达到 1477 行，违反单一职责原则。

**建议拆分**:
- `StatementCacheManager` - PreparedStatement 缓存管理
- `QueryMetaManager` - 元数据缓存管理
- `ResultCacheManager` - 结果缓存管理
- `BatchExecutor` - 批量操作执行器
- `RowDataReader` - 行数据读取器

**优先级**: P2

---

### MapperClassGenerator 方法拆分

**问题**: `generateFieldMapping` 方法过长，包含 8 种基本类型 + String + 其他对象类型的处理。

**建议拆分**:
- `generateIntMapping()`
- `generateLongMapping()`
- `generateStringMapping()`
- `generateObjectMapping()`

**优先级**: P2

---

## 线程安全修复

### ThreadLocal 内存泄漏

**文件**: `JrdbTransactionManager.java`

**问题**: `connectionHolder` 只 set 未确保 remove，线程池场景下可能泄漏。

**修复**: 在 `closeConnection()` 的 finally 块中调用 `connectionHolder.remove()`。

**优先级**: P0

---

### 缓存 key 哈希冲突

**文件**: `QueryResultCache.java`

**问题**: `Arrays.hashCode()` 可能冲突，导致不同参数命中相同缓存。

**修复**: 改用 `Arrays.deepToString(params)` 作为 key 的一部分。

**优先级**: P0

---

### 空 catch 块吞掉异常

**文件**: `JrdbTransactionManager.java:68-69`

**问题**: catch 块为空，异常被吞掉。

**修复**: 添加日志记录。

**优先级**: P1

---

## 事务完善

### 事务传播机制

**问题**: `TransactionManager` 不支持事务传播（REQUIRED/REQUIRES_NEW 等）。

**优先级**: P2

---

### Savepoint 支持

**问题**: `JrdbTransactionManager` 的 Savepoint 方法为空实现。

**优先级**: P2

---

## 资源管理

### TransactionImpl 实现 AutoCloseable

**问题**: 无法使用 try-with-resources 语法。

**优先级**: P1

---

### ConnectionPool 正确销毁

**文件**: `R2dbcSessionImpl.java`

**问题**: 外部传入的 ConnectionPool 可能未被销毁。

**优先级**: P1

---

## 性能优化

### switch 分支预计算

**文件**: `SessionImpl.java:setColumnValueUltraFast`

**问题**: 热点代码路径，每次处理列值都需要执行 switch 分支。

**建议**: 预先计算每列的 `ValueWriter` 函数式接口。

**优先级**: P2

---

### 异常捕获控制流程

**文件**: `SessionImpl.java:getColumnValue`

**问题**: 用异常捕获判断类型，批量查询场景成本高。

**建议**: 使用 `ColumnMeta` 预先知道类型。

**优先级**: P2

---

## 日志完善

**问题**: 多处异常日志未输出堆栈。

**建议**: 统一使用三参数形式：`log.error("msg: {}", e.getMessage(), e)`

**优先级**: P1

---

**最后更新**: 2026-04-03
