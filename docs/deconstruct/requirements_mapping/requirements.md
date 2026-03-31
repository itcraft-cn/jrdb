# mapping 模块需求文档

## 功能需求

### FR-MAP-001: 实体映射器管理

**需求描述：** 系统需要管理实体类与数据库表的映射关系。

**验收标准：**
- 支持自动配置实体映射
- 支持手动配置映射关系
- 支持缓存已创建的映射器

**优先级：** 高

### FR-MAP-002: ASM 字节码生成

**需求描述：** 系统需要动态生成映射器实现类。

**验收标准：**
- 支持生成 mapFromResultSet 方法
- 支持生成 mapToRow 方法
- 生成的代码直接调用 setter/getter

**优先级：** 高

### FR-MAP-003: 命名策略

**需求描述：** 系统需要支持字段名到列名的自动转换。

**验收标准：**
- 支持驼峰转下划线（userId -> user_id）
- 支持自定义命名策略

**优先级：** 中

### FR-MAP-004: 主键标识

**需求描述：** 系统需要支持标识主键字段。

**验收标准：**
- 支持标记字段为主键
- 主键字段在映射中有特殊标记

**优先级：** 中

## 非功能需求

### NFR-MAP-001: 性能要求

- 映射器生成应在首次使用时完成
- 生成的映射器性能应接近手写代码

### NFR-MAP-002: 类型安全

- 映射配置时应验证字段类型
- 不支持的类型应有明确错误提示

## 使用场景

```java
// 场景1：自动配置
MapperRegistry registry = new MapperRegistry(typeHandlerRegistry);
EntityMapper<User> mapper = registry.getMapper(User.class);

// 场景2：手动配置
MappingConfig config = new MappingConfig(User.class, "t_user", typeHandlerRegistry)
    .idField("id", Long.class)
    .field("userName", String.class);
registry.register(config);

// 场景3：使用映射器
ResultSet rs = stmt.executeQuery("SELECT * FROM user");
User user = mapper.mapFromResultSet(rs);

Object[] params = mapper.mapToRow(user);
```