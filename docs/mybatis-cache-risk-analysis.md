# MyBatis SESSION 级别缓存风险分析

## 问题背景

MyBatis 默认开启 SESSION 级别一级缓存，该设计在业务系统中存在严重风险。

## 一级缓存原理

```java
// MyBatis 一级缓存在 BaseExecutor 中实现
// key = CacheKey(ms.id, params, rowBounds, sql)
// value = 查询结果对象

// SESSION 级别：同一个 SqlSession 内共享缓存
// STATEMENT 级别：每次查询后立即清空缓存
```

## SESSION 级别缓存行为

```java
SqlSession session = factory.openSession();

// 第一次查询
User u1 = session.selectOne("getById", 1);  // 执行 SQL，缓存结果

// 第二次查询（相同参数）
User u2 = session.selectOne("getById", 1);  // 直接返回缓存对象
System.out.println(u1 == u2);  // true - 同一个 Java 对象引用

// 即使数据库被外部修改
otherSystem.updateUser(1, "Bob");

// 第三次查询 - 缓存未失效，仍返回旧数据
User u3 = session.selectOne("getById", 1);  // 仍是 Alice（脏数据！）
```

## 风险场景

### 1. 脏读风险

```
时间线：
T1: 事务 A 查询 user(id=1) -> 缓存 {id:1, name:Alice}
T2: 事务 B 修改 user(id=1) -> name=Bob
T3: 事务 A 再次查询 user(id=1) -> 返回缓存中的 Alice（脏数据！）
```

**影响：**
- 无法感知其他事务的修改
- 业务决策基于过期数据
- 数据一致性被破坏

### 2. 内存泄漏风险

```java
// 长事务场景
SqlSession session = factory.openSession();
for (int i = 0; i < 100000; i++) {
    session.selectOne("getById", i);  // 缓存 10 万个对象
}
// OutOfMemoryError!
```

**影响：**
- 批量处理场景内存暴涨
- 长事务占用大量内存
- OOM 导致服务崩溃

### 3. 结果不可预测

```java
// 同一个 SqlSession 内
User u1 = session.selectOne("getById", 1);
session.update("updateName", 1, "Bob");  // 更新操作
User u2 = session.selectOne("getById", 1);  // 返回什么？

// 答案：取决于 MyBatis 内部实现
// update 操作会清空缓存，所以 u2 会重新查询
// 但这个行为开发者难以预测
```

## 真实案例

### 案例1：库存超卖

```
场景：电商秒杀系统

1. 查询库存 -> 100 件（缓存）
2. 其他用户下单 -> 库存变为 99 件
3. 再次查询库存 -> 返回缓存中的 100 件
4. 判断库存充足，继续下单 -> 超卖！
```

### 案例2：状态同步失败

```
场景：分布式系统

服务A: 查询订单状态 -> PENDING（缓存）
服务B: 更新订单状态 -> PAID
服务A: 再次查询订单状态 -> PENDING（缓存）
服务A: 执行业务逻辑，认为订单未支付 -> 业务错误！
```

## 性能测试对比

在 JMH 基准测试中，SESSION 级别缓存会产生误导性结果：

```
# SESSION 级别
MyBatis_singleQuery: 12,398,851 ops/s  # 直接返回内存对象，无 IO

# STATEMENT 级别
MyBatis_singleQuery: 448,711 ops/s     # 每次执行 SQL

# 差距 27 倍！
```

**SESSION 级别测试的不是数据库性能，而是 HashMap.get() 性能。**

## 业界最佳实践

### 推荐配置

```xml
<!-- mybatis-config.xml -->
<configuration>
    <settings>
        <!-- 关闭一级缓存 -->
        <setting name="localCacheScope" value="STATEMENT"/>
        
        <!-- 关闭二级缓存（使用 Redis/Ehcache 替代） -->
        <setting name="cacheEnabled" value="false"/>
    </settings>
</configuration>
```

### Spring Boot 配置

```yaml
# application.yml
mybatis:
  configuration:
    local-cache-scope: statement
    cache-enabled: false
```

### 各级别对比

| 级别 | 缓存范围 | 脏读风险 | 内存风险 | 适用场景 |
|------|----------|----------|----------|----------|
| SESSION | SqlSession 内 | ⚠️ 高 | ⚠️ 高 | 不推荐 |
| STATEMENT | 无 | ✅ 无 | ✅ 无 | 推荐默认 |
| 二级缓存 | 跨 SqlSession | ⚠️ 中 | ⚠️ 中 | 需配合分布式缓存 |

### 分布式缓存方案

```java
// 使用 Redis 替代 MyBatis 缓存
@Cacheable(value = "user", key = "#id")
public User getUserById(Long id) {
    return userMapper.getById(id);
}

// 配合 TTL 和缓存失效策略
@CacheEvict(value = "user", key = "#user.id")
public void updateUser(User user) {
    userMapper.update(user);
}
```

## jrdb 缓存设计对比

jrdb 的 L2 缓存设计避免了这些问题：

```java
// jrdb L2 缓存特性
JrdbConfig config = JrdbConfig.builder()
    .cacheLevel(CacheLevel.L2)
    .resultCacheSize(1000)           // 限制缓存大小
    .resultCacheExpireSeconds(300)   // TTL 自动过期
    .build();

// 基于SQL + 参数的缓存 key
// 不会出现 "同一个对象" 问题
// 有 TTL，不会永久缓存脏数据
```

**jrdb L2 缓存优势：**

| 特性 | MyBatis SESSION | jrdb L2 |
|------|-----------------|---------|
| TTL 过期 | ❌ 无 | ✅ 有 |
| 缓存大小限制 | ❌ 无 | ✅ 有 |
| 跨 Session 共享 | ❌ 否 | ✅ 是 |
| 脏数据风险 | ⚠️ 高 | ✅ 低（TTL 兜底） |

## 结论

**MyBatis SESSION 级别缓存不适合业务系统使用：**

1. **脏读风险高** - 无法感知其他事务的修改
2. **内存泄漏风险** - 长事务场景 OOM
3. **行为不可预测** - 开发者难以理解缓存何时生效/失效

**推荐方案：**

```yaml
# 必须配置
mybatis.configuration.local-cache-scope: statement

# 分布式缓存用 Redis 替代
spring.cache.type: redis
```

## 参考资料

- [MyBatis 官方文档 - 缓存](https://mybatis.org/mybatis-3/sqlmap-xml.html#cache)
- [MyBatis 一级缓存详解](https://tech.meituan.com/2018/01/07/mybatis-cache.html)
- [为什么 MyBatis 一级缓存是个坑](https://mp.weixin.qq.com/s/uMUFjKJHpOgUfu5XQpmBqQ)