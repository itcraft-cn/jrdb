# sql 模块需求文档

## 功能需求

### FR-SQL-001: SELECT 语句构建

**需求描述：** 系统需要支持 SELECT 语句的流式构建。

**验收标准：**
- 支持指定查询列
- 支持 FROM 子句
- 支持 JOIN 子句
- 支持 WHERE 条件
- 支持 GROUP BY / HAVING
- 支持 ORDER BY / LIMIT / OFFSET

**优先级：** 高

### FR-SQL-002: INSERT 语句构建

**需求描述：** 系统需要支持 INSERT 语句的流式构建。

**验收标准：**
- 支持指定表名
- 支持指定列名
- 支持指定值

**优先级：** 高

### FR-SQL-003: UPDATE 语句构建

**需求描述：** 系统需要支持 UPDATE 语句的流式构建。

**验收标准：**
- 支持指定表名
- 支持 SET 子句
- 支持 WHERE 条件

**优先级：** 高

### FR-SQL-004: DELETE 语句构建

**需求描述：** 系统需要支持 DELETE 语句的流式构建。

**验收标准：**
- 支持指定表名
- 支持 WHERE 条件

**优先级：** 高

### FR-SQL-005: SQL 模板处理

**需求描述：** 系统需要支持模板式 SQL 处理。

**验收标准：**
- 支持 `${}` 直接替换
- 支持 `#{}` 参数占位符转换
- 支持嵌套属性访问

**优先级：** 中

### FR-SQL-006: 条件拼接

**需求描述：** 系统需要支持条件化的 SQL 拼接。

**验收标准：**
- 支持 `whereIf` 条件拼接
- 支持 `appendIf` 条件拼接

**优先级：** 中

## 非功能需求

### NFR-SQL-001: SQL 注入防护

- 所有参数必须使用占位符
- 禁止直接拼接用户输入

### NFR-SQL-002: API 易用性

- Builder 方法应返回 this 支持链式调用
- 方法命名应直观易懂

## 使用场景

```java
// 场景1：SELECT 构建
BoundSql sql = SqlDsl.select("id", "name")
    .from("user")
    .where("status = ?", "active")
    .whereIf(name != null, "name LIKE ?", name)
    .orderBy("id DESC")
    .limit(10)
    .build();

// 场景2：INSERT 构建
BoundSql sql = SqlDsl.insertInto("user")
    .columns("id", "name")
    .values(1L, "Alice")
    .build();

// 场景3：UPDATE 构建
BoundSql sql = SqlDsl.update("user")
    .set("name", "Bob")
    .where("id = ?", 1L)
    .build();

// 场景4：模板处理
BoundSql sql = SqlTemplate.process(
    "SELECT * FROM ${table} WHERE id = #{id}",
    Map.of("table", "user", "id", 1L));
```