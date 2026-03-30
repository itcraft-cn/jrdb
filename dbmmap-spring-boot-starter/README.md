# dbmmap-spring-boot-starter

Spring Boot集成模块，提供自动配置和Repository支持。

## 依赖

```xml
<dependency>
    <groupId>com.dbmmap</groupId>
    <artifactId>dbmmap-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 配置

```yaml
# application.yml
dbmmap:
  enabled: true
  memory-pool:
    pre-alloc-mem-size: 67108864    # 64MB
    query-cache-size: 4194304       # 4MB
  cache:
    level: L1                       # NONE/L1/L2
    statement-cache-size: 256
    result-cache-size: 1000         # L2专用
    result-cache-expire-seconds: 300 # L2专用
```

## 使用方式一：直接注入Session

```java
@Service
public class UserService {

    @Autowired
    private Session session;

    public User getUserById(Long id) {
        try (QueryResult result = session.query(
            "SELECT id, name, age FROM users WHERE id = ?", id)) {
            if (result != null && result.next()) {
                User user = new User();
                user.setId(result.getLong("ID"));
                user.setName(result.getString("NAME"));
                user.setAge(result.getInt("AGE"));
                return user;
            }
            return null;
        }
    }

    public void updateName(Long id, String name) {
        session.update("UPDATE users SET name = ? WHERE id = ?", name, id);
    }
}
```

## 使用方式二：Repository接口

### 1. 定义Repository接口

```java
package com.example.repository;

import com.dbmmap.springboot.repository.DbMmapRepository;
import com.dbmmap.springboot.repository.Query;
import com.dbmmap.springboot.repository.Update;

@DbMmapRepository
public interface UserRepository {

    @Query("SELECT id, name, age FROM users WHERE id = ?")
    long findNameById(Long id);

    @Query("SELECT COUNT(*) FROM users WHERE age > ?")
    int countByAgeGreaterThan(int age);

    @Update("UPDATE users SET name = ? WHERE id = ?")
    int updateName(String name, Long id);

    @Update("DELETE FROM users WHERE id = ?")
    int deleteById(Long id);
}
```

### 2. 启用Repository扫描

```java
@SpringBootApplication
@DbMmapRepositoryScan("com.example.repository")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 使用Repository

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public long getUserName(Long id) {
        return userRepository.findNameById(id);
    }

    public void changeName(Long id, String name) {
        userRepository.updateName(name, id);
    }
}
```

## 事务管理

```java
@Configuration
public class DbMmapConfig {

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DbMmapTransactionManager(dataSource);
    }
}
```

```java
@Service
public class OrderService {

    @Autowired
    private Session session;

    @Transactional
    public void placeOrder(Order order) {
        session.update("INSERT INTO orders ...", ...);
        session.update("UPDATE inventory SET stock = stock - ? ...", ...);
        // 自动提交或回滚
    }
}
```

## 缓存级别说明

| 级别 | 说明 | 适用场景 |
|------|------|----------|
| NONE | 无缓存 | 数据频繁变化的场景 |
| L1 | Statement + Meta缓存 | 大部分场景，默认值 |
| L2 | L1 + 结果缓存 | 读多写少，查询结果可缓存 |

## 对比MyBatis/Hibernate

| 特性 | dbmmap | MyBatis | Hibernate |
|------|--------|---------|-----------|
| 初始化 | 自动 | XML/注解 | 注解 |
| 查询API | JDBC风格 | Mapper接口 | ORM |
| 实体映射 | 手动 | 自动 | 自动 |
| 性能 | 最高 | 中等 | 较低 |
| 学习曲线 | 低 | 中 | 高 |