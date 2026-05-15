# jrdb 注解 SQL 混合模式设计文档

## 概述

为 jrdb 添加注解 SQL 能力，实现混合模式（MyBatis Style + Hibernate Style + Jdbc Style），降低 CRUD 编码强度。

## 设计目标

1. 提供 MyBatis 风格的注解 SQL（`@Select/@Update/@Insert/@Delete`）
2. 提供 Hibernate 风格的实体查询（HQL-like）
3. 提供 Jdbc 风格的直接 SQL（默认）
4. 支持 AutoCrud 自动生成基础 CRUD
5. 统一到 DAO 接口，使用动态代理实现

## 架构设计

### 整体架构

```
┌─────────────────────────────────────────┐
│  用户层：DAO 接口定义（注解驱动）          │
├─────────────────────────────────────────┤
│  代理层：JDK Proxy/ASM 代理（胶水）       │
├─────────────────────────────────────────┤
│  元数据层：DaoMetaCache（稳态缓存）       │
│  ├─ Style 配置                           │
│  ├─ 方法注解映射                          │
│  └─ AutoCrud 配置                        │
├─────────────────────────────────────────┤
│  固化层：Style 执行器（核心逻辑）          │
│  ├─ MyBatisStyleExecutor                 │
│  ├─ HibernateStyleExecutor               │
│  └─ JdbcStyleExecutor                    │
├─────────────────────────────────────────┤
│  基础层：Session API（现有）               │
└─────────────────────────────────────────┘
```

### 数据流

```
UserDao.findById(1L)
    → JDK Proxy/ASM代理拦截方法调用
    → 查询 DaoMetaCache（已缓存，无解析开销）
    → 获取 Style + SqlTemplate
    → 调用对应 StyleExecutor 执行
    → 返回结果
```

## 核心组件

### 注解定义

#### Style 注解（类/方法级别）

- `@MyBatisStyle` - MyBatis 风格（#{param} 参数绑定）
- `@HibernateStyle` - Hibernate 风格（from Entity where...）
- `@JdbcStyle` - JDBC 风格（? 占位符）

**优先级：方法 > 类 > 配置**

#### 方法注解

- `@Select` - 查询注解
- `@Update` - 更新注解
- `@Insert` - 插入注解
- `@Delete` - 删除注解
- `@Query` - Hibernate 风格查询注解

#### AutoCrud 注解

```java
@AutoCrud(entity = User.class, primaryKey = "id")
interface UserDao {}
```

自动生成：
- `insert(entity)`
- `update(entity)`
- `delete(id)`
- `selectById(id)`
- `selectAll()`

### 元数据缓存

```java
class DaoMeta {
    SqlParamBindStyle classStyle;          // 类级别 Style
    Map<Method, MethodMeta> methodMetas;   // 方法元数据映射
    AutoCrudMeta autoCrud;                 // AutoCrud 配置（可选）
}

class MethodMeta {
    SqlParamBindStyle methodStyle;         // 方法级别 Style（优先级更高）
    String sql;                            // SQL 模板
    List<ParamMeta> params;                // 参数元数据
}
```

### Style 执行器

#### MyBatisStyleExecutor

- 将 `#{param}` 替换为 `?`
- 按参数名绑定参数值

#### HibernateStyleExecutor

- 将 `from Entity where...` 转换为 `SELECT * FROM table WHERE...`
- 自动推断表名（CamelCase → snake_case）

#### JdbcStyleExecutor

- 直接使用 `?` 占位符
- 按参数位置绑定参数值

### DaoRegistry

- 管理 DAO 实例
- 创建代理（当前使用 JDK Proxy，后续优化为 ASM）
- 提供 `getDao(daoInterface, session)` 方法

### DaoMetaCache

- 解析 DAO 接口注解
- 缓存元数据（稳态，运行周期内不变）
- 提供快速查询

## 配置扩展

### JrdbConfig

```java
JrdbConfig config = JrdbConfig.builder()
    .defaultStyle(SqlParamBindStyle.JDBC)  // 默认 Style
    .build();
```

### SessionFactory

```java
SessionFactory factory = new SessionFactory(config, dataSource);
DaoRegistry daoRegistry = factory.getDaoRegistry();
```

## 使用示例

### MyBatis 风格

```java
@MyBatisStyle
public interface UserDao {
    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);
    
    @Update("UPDATE users SET name = #{name} WHERE id = #{id}")
    int updateName(Long id, String name);
}

// 使用
try (Session session = factory.openSession()) {
    UserDao userDao = daoRegistry.getDao(UserDao.class, session);
    User user = userDao.findById(1L);
}
```

### Hibernate 风格

```java
@HibernateStyle
public interface UserDao {
    @Query("from User where id = ?")
    User findById(Long id);
    
    @Query("from User")
    List<User> findAll();
}
```

### Jdbc 风格

```java
@JdbcStyle
public interface UserDao {
    @Select("SELECT * FROM users WHERE id = ?")
    User findById(Long id);
}
```

### AutoCrud

```java
@AutoCrud(entity = User.class, primaryKey = "id")
public interface UserDao {
    // 自动生成 insert/update/delete/selectById/selectAll
}
```

### 混合模式

```java
@MyBatisStyle  // 类级别默认 MyBatis
public interface UserDao {
    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);  // MyBatis Style
    
    @JdbcStyle  // 方法级别覆盖
    @Select("SELECT * FROM users WHERE name = ?")
    User findByJdbcStyle(String name);  // Jdbc Style
}
```

## 性能考虑

1. **元数据缓存**：DAO 接口注解首次解析后缓存，后续调用无解析开销
2. **代理实现**：当前使用 JDK Proxy，后续优化为 ASM 字节码生成
3. **执行器固化**：Style 执行器逻辑固化，ASM 仅完成胶水黏合

## 测试策略

1. **单元测试**：每个组件独立测试
2. **集成测试**：H2 数据库真实读写验证
3. **混合模式测试**：方法级别覆盖类级别 Style

## 后续优化

1. ASM 字节码生成替换 JDK Proxy
2. AutoCrud 完整实现（当前仅支持注解解析）
3. 复杂参数绑定（如 Map/List 嵌套）
4. Spring Boot Starter 集成