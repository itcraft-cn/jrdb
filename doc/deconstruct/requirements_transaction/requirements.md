# transaction 模块需求文档

## 功能需求

### FR-TRX-001: 事务控制

**需求描述：** 系统需要支持显式的事务控制。

**验收标准：**
- 支持开始事务
- 支持提交事务
- 支持回滚事务
- 支持关闭事务

**优先级：** 高

### FR-TRX-002: 自动事务管理

**需求描述：** 系统需要支持自动化的事务管理。

**验收标准：**
- 支持回调式事务
- 成功时自动提交
- 异常时自动回滚

**优先级：** 高

### FR-TRX-003: 懒加载连接

**需求描述：** 事务开始时不立即获取数据库连接。

**验收标准：**
- 首次 getConnection 时才建立连接
- 减少不必要的连接占用

**优先级：** 中

### FR-TRX-004: 资源自动清理

**需求描述：** 事务关闭时自动清理资源。

**验收标准：**
- 未提交时自动回滚
- 自动关闭连接
- 支持多次调用 close

**优先级：** 高

## 非功能需求

### NFR-TRX-001: 异常处理

- 任何异常都应触发回滚
- 异常信息清晰易懂

### NFR-TRX-002: 资源安全

- 防止连接泄漏
- 支持嵌套 try-with-resources

## 使用场景

```java
// 场景1：显式事务
try (Transaction tx = tm.beginTransaction()) {
    Connection conn = tx.getConnection();
    // 执行操作
    tx.commit();
}

// 场景2：自动事务
Long id = tm.executeInTransaction(tx -> {
    Connection conn = tx.getConnection();
    // 执行操作
    return generatedId;
});

// 场景3：回调式事务
tm.runInTransaction(tx -> {
    Connection conn = tx.getConnection();
    // 执行操作
});
```