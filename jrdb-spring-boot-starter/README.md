# jrdb-spring-boot-starter

Spring Boot集成模块，提供自动配置、Repository支持和ASM实体映射。

## 依赖

```xml
<dependency>
    <groupId>cn.itcraft.jrdb</groupId>
    <artifactId>jrdb-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 配置

```yaml
jrdb:
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

## 使用方式一：JrdbTemplate (类似JdbcTemplate)

```java
@Service
public class UserService {

    @Autowired
    private JrdbTemplate template;

    // 直接查询
    public User getUserById(Long id) {
        return template.queryForObject(
            "SELECT id, name, age, email FROM users WHERE id = ?", 
            User.class, id);
    }

    // 列表查询
    public List<User> getUsersByAge(int minAge) {
        return template.queryForList(
            "SELECT id, name, age, email FROM users WHERE age > ?", 
            User.class, minAge);
    }

    // 更新
    public int updateName(Long id, String name) {
        return template.update("UPDATE users SET name = ? WHERE id = ?", name, id);
    }
}
```

## 使用方式二：Repository接口 (类似MyBatis Mapper)

### 1. 定义实体类

```java
package com.example.entity;

public class User {
    private Long id;
    private String name;
    private Integer age;
    private String email;

    // getter/setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

### 2. 定义Repository接口

```java
package com.example.repository;

import cn.itcraft.jrdb.springboot.repository.JrdbRepository;
import cn.itcraft.jrdb.springboot.repository.Query;
import cn.itcraft.jrdb.springboot.repository.Update;
import com.example.entity.User;
import java.util.List;

@JrdbRepository
public interface UserRepository {

    // 返回实体对象 (ASM自动映射)
    @Query("SELECT id, name, age, email FROM users WHERE id = ?")
    User findById(Long id);

    // 返回实体列表
    @Query("SELECT id, name, age, email FROM users WHERE age > ?")
    List<User> findByAgeGreaterThan(int age);

    // 返回单个值
    @Query("SELECT COUNT(*) FROM users")
    long count();

    // 更新操作
    @Update("UPDATE users SET name = ? WHERE id = ?")
    int updateName(String name, Long id);

    @Update("DELETE FROM users WHERE id = ?")
    int deleteById(Long id);
}
```

### 3. 启用Repository扫描

```java
@SpringBootApplication
@JrdbRepositoryScan("com.example.repository")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 4. 使用Repository

```java
@Service
public class OrderService {

    @Autowired
    private UserRepository userRepository;

    public User getUser(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getAdults() {
        return userRepository.findByAgeGreaterThan(18);
    }

    public void changeName(Long id, String name) {
        userRepository.updateName(name, id);
    }
}
```

## 使用方式三：直接注入Session (底层API)

```java
@Service
public class RawService {

    @Autowired
    private Session session;

    public void rawQuery() {
        try (QueryResult result = session.query(
            "SELECT id, name FROM users WHERE age > ?", 18)) {
            while (result != null && result.next()) {
                long id = result.getLong("ID");
                String name = result.getString("NAME");
            }
        }
    }
}
```

## 事务管理

```java
@Configuration
public class JrdbConfig {

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new JrdbTransactionManager(dataSource);
    }
}
```

```java
@Service
public class OrderService {

    @Autowired
    private JrdbTemplate template;

    @Transactional
    public void placeOrder(Order order) {
        template.update("INSERT INTO orders ...", ...);
        template.update("UPDATE inventory SET stock = stock - ? ...", ...);
    }
}
```

## ASM实体映射原理

jrdb使用ASM动态生成EntityMapper实现类：

1. **首次查询**时，MapperRegistry通过反射分析实体类字段
2. **ASM生成**专用的Mapper字节码（比反射快10倍）
3. **自动映射** ResultSet到实体对象

```java
// 自动生成的映射逻辑（伪代码）
public User mapFromResultSet(ResultSet rs) {
    User user = new User();
    user.setId(rs.getLong("id"));
    user.setName(rs.getString("name"));
    user.setAge(rs.getInt("age"));
    return user;
}
```

## 缓存级别

| 级别 | 说明 | 适用场景 |
|------|------|----------|
| NONE | 无缓存 | 数据频繁变化 |
| L1 | PreparedStatement + Meta缓存 | 默认值，适合大部分场景 |
| L2 | L1 + 结果缓存(Guava Cache) | 读多写少，查询结果可缓存 |

## 对比MyBatis/Hibernate

| 特性 | jrdb | MyBatis | Hibernate |
|------|--------|---------|-----------|
| 初始化 | **自动** | XML/注解 | 注解 |
| Repository | ✅支持 | ✅支持 | ✅支持 |
| 实体映射 | **ASM动态生成** | 反射 | 反射 |
| 事务管理 | ✅支持 | ✅支持 | ✅支持 |
| 性能 | **最高** | 中等 | 较低 |
| 学习曲线 | **低** | 中 | 高 |

## 性能数据

NoCache模式（真实DB查询）:
- 单条查询: jrdb 1.72M > Hibernate 562K > MyBatis 382K ops/s
- 批量查询: jrdb 174K > MyBatis 43K > Hibernate 24K ops/s