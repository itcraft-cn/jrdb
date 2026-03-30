# cache 模块需求文档

## 功能需求

### FR-CAC-001: L1 Statement 缓存

**需求描述：** 系统需要支持 PreparedStatement 的缓存复用。

**验收标准：**
- 支持按 SQL 缓存 PreparedStatement
- 支持 LRU 淘汰策略
- 支持配置缓存容量

**优先级：** 高

### FR-CAC-002: L2 结果缓存

**需求描述：** 系统需要支持查询结果的缓存。

**验收标准：**
- 支持按 SQL + 参数缓存结果
- 支持配置缓存容量
- 支持配置过期时间
- 支持手动失效缓存

**优先级：** 中

### FR-CAC-003: 缓存级别控制

**需求描述：** 系统需要支持多级缓存配置。

**验收标准：**
- 支持 NONE（无缓存）
- 支持 L1（Statement 缓存）
- 支持 L2（结果缓存，含 L1）

**优先级：** 高

### FR-CAC-004: 缓存失效

**需求描述：** 系统需要在数据变更时失效缓存。

**验收标准：**
- 更新操作后自动失效 L2 缓存
- 支持手动失效所有缓存

**优先级：** 高

## 非功能需求

### NFR-CAC-001: 缓存命中率

- L1 Statement 缓存命中率 > 90%（重复查询场景）
- L2 结果缓存命中率 > 80%（相同查询场景）

### NFR-CAC-002: 内存控制

- L2 缓存应有容量限制
- 避免内存溢出

## 使用场景

```java
// 场景1：配置 L2 缓存
DbMmapConfig config = DbMmapConfig.builder()
    .cacheLevel(CacheLevel.L2)
    .resultCacheSize(1000)
    .resultCacheExpireSeconds(300)
    .build();

// 场景2：查询缓存
QueryResult result1 = session.query(sql);  // 执行查询
QueryResult result2 = session.query(sql);  // 命中缓存

// 场景3：手动失效
session.update(updateSql);       // 自动失效 L2 缓存
session.invalidateResultCache(); // 手动失效
session.clearCache();            // 清理所有缓存
```