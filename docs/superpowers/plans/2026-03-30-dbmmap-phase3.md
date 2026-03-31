# jrdb Phase 3 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan.

**Goal:** 实现jrdb SQL构建能力 - 字符串构建器、模板引擎、DSL构建器

**Architecture:** SqlBuilder链式API；SqlTemplate模板解析（支持#{}和${}占位符）；SqlDsl流式API构建SELECT/INSERT/UPDATE/DELETE。

**Tech Stack:** Java 8+, Maven, JUnit 5

---

## 文件结构

```
jrdb-core/src/main/java/com/jrdb/core/sql/
├── SqlBuilder.java              # 字符串构建器
├── SqlTemplate.java             # 模板引擎
├── SqlDsl.java                  # DSL入口
├── dsl/
│   ├── SelectBuilder.java       # SELECT构建器
│   ├── InsertBuilder.java       # INSERT构建器
│   ├── UpdateBuilder.java       # UPDATE构建器
│   └── DeleteBuilder.java       # DELETE构建器
└── SqlSegment.java              # SQL片段

jrdb-core/src/test/java/com/jrdb/core/sql/
├── SqlBuilderTest.java
├── SqlTemplateTest.java
└── SqlDslTest.java
```

---

## Task 1: SqlBuilder字符串构建器

**Files:**
- `jrdb-core/src/main/java/com/jrdb/core/sql/SqlBuilder.java`
- `jrdb-core/src/test/java/com/jrdb/core/sql/SqlBuilderTest.java`

**SqlBuilder API:**
```java
SqlBuilder builder = SqlBuilder.create()
    .append("SELECT * FROM users")
    .append("WHERE id = ?", 1)
    .append("AND status = ?", "active")
    .appendIf(condition, "AND deleted = false");

String sql = builder.build();
Object[] params = builder.getParams();
```

**实现要求:**
- append(String sql, Object... params) - 链式添加SQL片段和参数
- appendIf(boolean condition, String sql, Object... params) - 条件添加
- build() - 返回完整SQL字符串
- getParams() - 返回参数数组
- reset() - 重置构建器
- 支持StringBuilder内部实现

---

## Task 2: SqlTemplate模板引擎

**Files:**
- `jrdb-core/src/main/java/com/jrdb/core/sql/SqlTemplate.java`
- `jrdb-core/src/main/java/com/jrdb/core/sql/BoundSql.java`
- `jrdb-core/src/test/java/com/jrdb/core/sql/SqlTemplateTest.java`

**模板语法:**
- `#{param}` - 参数占位符，生成?并收集参数
- `${param}` - 字符串替换，直接替换值
- 支持嵌套属性: `#{user.name}`

**BoundSql:**
```java
public class BoundSql {
    private final String sql;
    private final Object[] params;
    // getters
}
```

**SqlTemplate API:**
```java
String template = "SELECT * FROM users WHERE id = #{id} AND name LIKE #{name}";
Map<String, Object> params = new HashMap<>();
params.put("id", 1);
params.put("name", "%张%");

BoundSql boundSql = SqlTemplate.process(template, params);
// sql: "SELECT * FROM users WHERE id = ? AND name LIKE ?"
// params: [1, "%张%"]
```

---

## Task 3: SelectBuilder DSL

**Files:**
- `jrdb-core/src/main/java/com/jrdb/core/sql/dsl/SelectBuilder.java`
- `jrdb-core/src/test/java/com/jrdb/core/sql/SelectBuilderTest.java`

**API:**
```java
BoundSql sql = SqlDsl.select("id", "name", "age")
    .from("users")
    .where("status = ?", "active")
    .whereIf(condition, "age > ?", 18)
    .orderBy("create_time DESC")
    .limit(10)
    .offset(0)
    .build();
```

**实现要求:**
- select(String... columns) - 选择列
- from(String table) - 指定表
- join/leftJoin/rightJoin(String table, String on) - 连接
- where(String condition, Object... params) - WHERE条件
- whereIf(boolean condition, String sql, Object... params)
- groupBy(String... columns)
- having(String condition, Object... params)
- orderBy(String... orders)
- limit(int limit) / offset(int offset)
- build() - 返回BoundSql

---

## Task 4: InsertBuilder DSL

**Files:**
- `jrdb-core/src/main/java/com/jrdb/core/sql/dsl/InsertBuilder.java`
- `jrdb-core/src/test/java/com/jrdb/core/sql/InsertBuilderTest.java`

**API:**
```java
BoundSql sql = SqlDsl.insertInto("users")
    .values("name", "张三")
    .values("age", 25)
    .values("status", "active")
    .build();
// INSERT INTO users (name, age, status) VALUES (?, ?, ?)
```

**批量插入:**
```java
BoundSql sql = SqlDsl.insertInto("users")
    .columns("name", "age", "status")
    .values("张三", 25, "active")
    .values("李四", 30, "active")
    .buildBatch();
// INSERT INTO users (name, age, status) VALUES (?, ?, ?), (?, ?, ?)
```

---

## Task 5: UpdateBuilder DSL

**Files:**
- `jrdb-core/src/main/java/com/jrdb/core/sql/dsl/UpdateBuilder.java`
- `jrdb-core/src/test/java/com/jrdb/core/sql/UpdateBuilderTest.java`

**API:**
```java
BoundSql sql = SqlDsl.update("users")
    .set("name", "李四")
    .set("age", 31)
    .where("id = ?", 1)
    .build();
// UPDATE users SET name = ?, age = ? WHERE id = ?
```

---

## Task 6: DeleteBuilder DSL

**Files:**
- `jrdb-core/src/main/java/com/jrdb/core/sql/dsl/DeleteBuilder.java`
- `jrdb-core/src/test/java/com/jrdb/core/sql/DeleteBuilderTest.java`

**API:**
```java
BoundSql sql = SqlDsl.deleteFrom("users")
    .where("id = ?", 1)
    .build();
// DELETE FROM users WHERE id = ?
```

---

## Task 7: SqlDsl入口和SqlSegment

**Files:**
- `jrdb-core/src/main/java/com/jrdb/core/sql/SqlDsl.java`
- `jrdb-core/src/main/java/com/jrdb/core/sql/SqlSegment.java`
- `jrdb-core/src/test/java/com/jrdb/core/sql/SqlDslTest.java`

**SqlDsl:**
```java
public class SqlDsl {
    public static SelectBuilder select(String... columns) { ... }
    public static InsertBuilder insertInto(String table) { ... }
    public static UpdateBuilder update(String table) { ... }
    public static DeleteBuilder deleteFrom(String table) { ... }
}
```

**SqlSegment:**
```java
public class SqlSegment {
    private final String sql;
    private final Object[] params;
    // 用于存储SQL片段和参数
}
```

---

## 执行顺序

1. 创建sql和dsl目录
2. Task 1: SqlBuilder
3. Task 2: SqlTemplate + BoundSql
4. Task 3: SelectBuilder
5. Task 4: InsertBuilder
6. Task 5: UpdateBuilder
7. Task 6: DeleteBuilder
8. Task 7: SqlDsl + SqlSegment
9. 运行全部测试

---

## 自检清单

- [ ] 所有测试通过
- [ ] API设计简洁易用
- [ ] 支持参数化查询（防SQL注入）
- [ ] 支持条件构建
- [ ] 无placeholder代码