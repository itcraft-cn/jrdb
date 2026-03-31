# query 模块需求文档

## 功能需求

### FR-QRY-001: 结果集遍历

**需求描述：** 系统需要支持游标式遍历查询结果。

**验收标准：**
- 支持 `next()` 方法移动游标
- 支持 `reset()` 方法重置游标
- 游标初始位置在第一行之前

**优先级：** 高

### FR-QRY-002: 列数据访问

**需求描述：** 系统需要支持按名称或索引访问列数据。

**验收标准：**
- 支持按列名获取数据
- 支持按列索引获取数据
- 支持常见数据类型（int, long, String, double）
- 列名查找大小写不敏感

**优先级：** 高

### FR-QRY-003: 元数据查询

**需求描述：** 系统需要提供结果集的元数据。

**验收标准：**
- 支持获取行数
- 支持获取列数
- 支持获取列名

**优先级：** 中

### FR-QRY-004: 资源释放

**需求描述：** 系统需要支持结果集资源的释放。

**验收标准：**
- 支持 `release()` 方法释放内存
- 支持 `close()` 方法关闭结果集
- 支持 try-with-resources 语法

**优先级：** 高

## 非功能需求

### NFR-QRY-001: 性能要求

- 数据访问应直接从内存读取，无额外拷贝
- 列名查找应为 O(1) 时间复杂度

### NFR-QRY-002: 内存管理

- 关闭结果集后，内存块应返回内存池
- 避免内存泄漏

## 使用场景

```java
// 场景1：基本遍历
QueryResult result = session.query("SELECT id, name FROM user");
while (result.next()) {
    long id = result.getLong("id");
    String name = result.getString("name");
}

// 场景2：按索引访问
while (result.next()) {
    long id = result.getLong(0);
    String name = result.getString(1);
}

// 场景3：重置游标
result.reset();
while (result.next()) {
    // 再次遍历
}

// 场景4：自动关闭
try (QueryResult result = session.query(sql)) {
    while (result.next()) {
        // ...
    }
}
```