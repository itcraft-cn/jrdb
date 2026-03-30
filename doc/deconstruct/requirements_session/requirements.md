# session 模块需求文档

## 功能需求

### FR-SES-001: 会话工厂管理

**需求描述：** 系统需要提供会话工厂，管理全局资源并创建会话实例。

**验收标准：**
- 支持创建 SessionFactory 实例
- 支持配置 DataSource
- 支持创建多个 Session 实例
- 支持关闭 SessionFactory 并释放资源

**优先级：** 高

### FR-SES-002: 查询操作

**需求描述：** 系统需要支持 SQL 查询操作。

**验收标准：**
- 支持执行无参数查询
- 支持执行带参数查询
- 返回 QueryResult 结果集
- 支持回调式查询（自动关闭结果）

**优先级：** 高

### FR-SES-003: 更新操作

**需求描述：** 系统需要支持 SQL 更新操作。

**验收标准：**
- 支持执行无参数更新
- 支持执行带参数更新
- 返回影响行数
- 支持批量更新

**优先级：** 高

### FR-SES-004: 缓存管理

**需求描述：** 系统需要支持多级缓存。

**验收标准：**
- 支持 NONE（无缓存）
- 支持 L1（Statement 缓存）
- 支持 L2（结果缓存）
- 支持手动清理缓存

**优先级：** 中

### FR-SES-005: 会话生命周期

**需求描述：** 系统需要管理会话的生命周期。

**验收标准：**
- 支持关闭会话
- 关闭时自动清理缓存
- 关闭时自动释放数据库连接
- 支持 try-with-resources 语法

**优先级：** 高

## 非功能需求

### NFR-SES-001: 性能要求

- 查询响应时间应与 JDBC 直接访问相当
- Statement 缓存命中率 > 90%（重复查询场景）

### NFR-SES-002: 资源管理

- Session 关闭后必须释放所有资源
- 避免连接泄漏

### NFR-SES-003: 异常处理

- 数据库异常应包装为 RuntimeException
- 提供清晰的错误信息

## 使用场景

```java
// 场景1：基本查询
try (Session session = factory.openSession()) {
    QueryResult result = session.query("SELECT * FROM user WHERE id = ?", 1);
    while (result.next()) {
        long id = result.getLong("id");
        String name = result.getString("name");
    }
    result.close();
}

// 场景2：回调式查询
User user = session.query(sql, rs -> {
    if (rs.next()) return new User(rs.getLong("id"), rs.getString("name"));
    return null;
});

// 场景3：更新操作
int affected = session.update("UPDATE user SET name = ? WHERE id = ?", "NewName", 1);

// 场景4：批量更新
int[] results = session.batchUpdate(sql1, sql2, sql3);
```