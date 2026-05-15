# jrdb 事务API改进设计

## 问题分析

### 当前设计缺陷

**Session和Transaction分离，不共享Connection！**

```java
// 当前代码 - 无法在同一事务中执行
Session session = sessionFactory.openSession();  // Connection A
Transaction tx = transactionManager.beginTransaction();  // Connection B

// session和tx使用不同的Connection，无法在同一事务中！
session.update("UPDATE users SET age = 30 WHERE id = 1");
// tx.commit() 不会提交session的操作，因为Connection不同！
```

**问题根源**：
- Session有自己的Connection（每次openSession从DataSource获取）
- Transaction有自己的Connection（beginTransaction从DataSource获取）
- 两者完全独立，无法协同工作

**用户影响**：
- 无法在事务中执行多个操作（update + insert）
- 无法在事务中使用Annotation/AutoCrud DAO
- 需要手动管理Connection，失去了jrdb的便捷性

## 解决方案对比

### 方案1：Session添加事务API（推荐）

**设计**：
```java
public interface Session {
    // 现有方法
    <T> T queryForObject(String sql, Class<T> type, Object... params);
    
    // 新增事务方法
    void beginTransaction() throws SQLException;
    void commit() throws SQLException;
    void rollback() throws SQLException;
    boolean isInTransaction();
}

// 使用示例
try (Session session = sessionFactory.openSession()) {
    session.beginTransaction();
    try {
        session.update("UPDATE accounts SET balance = balance - 100 WHERE id = 1");
        session.update("UPDATE accounts SET balance = balance + 100 WHERE id = 2");
        session.commit();
    } catch (Exception e) {
        session.rollback();
        throw e;
    }
}
```

**优点**：
- API简洁，Session即事务边界
- 学习曲线低，类似JDBC Connection
- 与Annotation/AutoCrud DAO无缝集成

**缺点**：
- Session接口变大（4个新方法）
- 需要手动commit/rollback

### 方案2：SessionFactory提供函数式事务API（推荐）

**设计**：
```java
public class SessionFactory {
    // 新增方法
    public <T> T executeInTransaction(Function<Session, T> function);
    public void runInTransaction(Consumer<Session> consumer);
}

// 使用示例
User user = sessionFactory.executeInTransaction(session -> {
    session.update("UPDATE accounts SET balance = balance - 100 WHERE id = 1");
    session.update("UPDATE accounts SET balance = balance + 100 WHERE id = 2");
    UserDao userDao = sessionFactory.getDaoRegistry().getDao(UserDao.class, session);
    return userDao.findById(1L);
});
```

**优点**：
- 自动commit/rollback，无需手动管理
- 函数式API，代码简洁
- 与Spring @Transactional理念一致

**缺点**：
- Lambda表达式，部分用户不熟悉

### 方案3：Session绑定到Transaction

**设计**：
```java
public class SessionFactory {
    public Session openSession(Transaction transaction);
}

Transaction tx = transactionManager.beginTransaction();
Session session = sessionFactory.openSession(tx);
session.update(...);
tx.commit();
```

**优点**：
- 保持现有TransactionManager API
- Session和Transaction明确绑定

**缺点**：
- 需要两个对象（Transaction + Session）
- API复杂度高

### 方案4：Session支持Connection绑定

**设计**：
```java
public class SessionFactory {
    public Session openSession(Connection connection);
}

Connection conn = dataSource.getConnection();
conn.setAutoCommit(false);
Session session = sessionFactory.openSession(conn);
session.update(...);
conn.commit();
```

**优点**：
- 最底层控制，完全自主管理
- 可与外部框架集成（如Spring事务）

**缺点**：
- 需要手动管理Connection生命周期
- 对用户要求高

### 方案5：混合方案（最佳）

**同时提供方案1 + 方案2 + 方案4**：

```java
// 方式1：手动控制（适合需要精细控制的场景）
try (Session session = sessionFactory.openSession()) {
    session.beginTransaction();
    session.update(...);
    session.commit();
}

// 方式2：自动管理（适合大多数场景）
sessionFactory.executeInTransaction(session -> {
    session.update(...);
    UserDao userDao = sessionFactory.getDaoRegistry().getDao(UserDao.class, session);
    userDao.updateAge(30, 1L);
});

// 方式3：底层控制（适合与外部框架集成）
Connection conn = dataSource.getConnection();
conn.setAutoCommit(false);
Session session = sessionFactory.openSession(conn);
session.update(...);
conn.commit();  // 外部管理事务
```

## 推荐方案：方案1 + Spring独立模块

### 设计理念

- **jrdb-jdbc核心模块**：仅提供Session手动事务API，保持简洁、零依赖
- **jrdb-jdbc-spring模块**：单独封装Spring集成，提供函数式API和@Transactional支持

### 模块职责划分

| 模块 | 职责 | 依赖 |
|------|------|------|
| **jrdb-jdbc** | Session事务API（手动控制） | 零依赖 |
| **jrdb-jdbc-spring** | 函数式API + Spring集成 | spring-context, spring-tx |

### API设计

#### jrdb-jdbc：Session接口扩展（方案1）

```java
public interface Session {
    // 现有方法保持不变...
    
    // ===== 新增事务API =====
    
    /**
     * 开始事务。
     * 调用此方法后，Session将使用单一Connection，
     * 所有操作在同一事务中执行，直到commit或rollback。
     * 
     * 如果已在事务中，抛出IllegalStateException。
     */
    void beginTransaction() throws SQLException;
    
    /**
     * 提交事务。
     * 如果未在事务中，抛出IllegalStateException。
     */
    void commit() throws SQLException;
    
    /**
     * 回滚事务。
     * 如果未在事务中，抛出IllegalStateException。
     */
    void rollback() throws SQLException;
    
    /**
     * 检查是否在事务中。
     */
    boolean isInTransaction();
    
    /**
     * 设置事务隔离级别。
     * 仅在beginTransaction之前调用有效。
     */
    void setTransactionIsolation(int level) throws SQLException;
}
```

#### jrdb-jdbc-spring：函数式API

```java
package cn.itcraft.jrdb.jdbc.spring;

/**
 * jrdb事务模板，提供函数式API。
 * 
 * 使用示例：
 * <pre>{@code
 * @Autowired
 * private JrdbTransactionTemplate transactionTemplate;
 * 
 * public void transfer() {
 *     transactionTemplate.execute(session -> {
 *         session.update("UPDATE accounts...");
 *         AccountDao dao = daoRegistry.getDao(AccountDao.class, session);
 *         dao.updateAccount(account);
 *         return null;
 *     });
 * }
 * }</pre>
 */
public class JrdbTransactionTemplate {
    
    private final SessionFactory sessionFactory;
    
    public JrdbTransactionTemplate(SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory);
    }
    
    /**
     * 在事务中执行操作，自动提交或回滚。
     */
    public <T> T execute(Function<Session, T> action) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            try {
                T result = action.apply(session);
                session.commit();
                return result;
            } catch (Exception e) {
                session.rollback();
                throw new JrdbTransactionException("Transaction failed", e);
            }
        } catch (SQLException e) {
            throw new JrdbTransactionException("Transaction error", e);
        }
    }
    
    /**
     * 在事务中执行操作（无返回值）。
     */
    public void executeWithoutResult(Consumer<Session> action) {
        execute(session -> {
            action.accept(session);
            return null;
        });
    }
    
    /**
     * 在事务中执行操作，返回Optional。
     */
    public <T> Optional<T> executeOptional(Function<Session, Optional<T>> action) {
        return execute(action);
    }
}

/**
 * jrdb事务异常。
 */
public class JrdbTransactionException extends RuntimeException {
    public JrdbTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

#### jrdb-jdbc-spring：Spring @Transactional集成

```java
package cn.itcraft.jrdb.jdbc.spring;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Spring事务管理器集成。
 * 
 * 使用示例：
 * <pre>{@code
 * @Autowired
 * private JrdbSpringTransactionTemplate transactionTemplate;
 * 
 * @Transactional  // Spring管理事务
 * public void transfer() {
 *     transactionTemplate.execute(session -> {
 *         session.update(...);
 *         // Spring自动commit/rollback
 *     });
 * }
 * }</pre>
 */
public class JrdbSpringTransactionTemplate {
    
    private final SessionFactory sessionFactory;
    private final PlatformTransactionManager transactionManager;
    
    public JrdbSpringTransactionTemplate(
            SessionFactory sessionFactory,
            PlatformTransactionManager transactionManager) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory);
        this.transactionManager = Objects.requireNonNull(transactionManager);
    }
    
    /**
     * 在Spring管理的事务中执行。
     * 使用Spring TransactionTemplate管理事务边界。
     */
    public <T> T executeInSpringTransaction(Function<Session, T> action) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        return txTemplate.execute(status -> {
            try (Session session = sessionFactory.openSession()) {
                session.beginTransaction();
                try {
                    T result = action.apply(session);
                    session.commit();
                    return result;
                } catch (Exception e) {
                    session.rollback();
                    status.setRollbackOnly();
                    throw new JrdbTransactionException("Transaction failed", e);
                }
            } catch (SQLException e) {
                status.setRollbackOnly();
                throw new JrdbTransactionException("Transaction error", e);
            }
        });
    }
    
    /**
     * 使用Spring TransactionDefinition配置事务。
     */
    public <T> T executeInSpringTransaction(
            TransactionDefinition definition,
            Function<Session, T> action) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager, definition);
        return txTemplate.execute(status -> {
            try (Session session = sessionFactory.openSession()) {
                session.setTransactionIsolation(definition.getIsolationLevel());
                session.beginTransaction();
                try {
                    T result = action.apply(session);
                    session.commit();
                    return result;
                } catch (Exception e) {
                    session.rollback();
                    status.setRollbackOnly();
                    throw new JrdbTransactionException("Transaction failed", e);
                }
            } catch (SQLException e) {
                status.setRollbackOnly();
                throw new JrdbTransactionException("Transaction error", e);
            }
        });
    }
}
```

#### SessionImpl实现变更

```java
public class SessionImpl implements Session {
    private DataSource dataSource;
    private Connection connection;
    private boolean inTransaction = false;
    private boolean connectionFromExternal = false;  // 区分外部Connection
    
    // 现有实现保持不变...
    
    @Override
    public void beginTransaction() throws SQLException {
        if (inTransaction) {
            throw new IllegalStateException("Already in transaction");
        }
        Connection conn = getConnection();
        if (conn.getAutoCommit()) {
            conn.setAutoCommit(false);
        }
        inTransaction = true;
    }
    
    @Override
    public void commit() throws SQLException {
        if (!inTransaction) {
            throw new IllegalStateException("Not in transaction");
        }
        getConnection().commit();
        inTransaction = false;
        if (!connectionFromExternal) {
            getConnection().setAutoCommit(true);
        }
    }
    
    @Override
    public void rollback() throws SQLException {
        if (!inTransaction) {
            throw new IllegalStateException("Not in transaction");
        }
        getConnection().rollback();
        inTransaction = false;
        if (!connectionFromExternal) {
            getConnection().setAutoCommit(true);
        }
    }
    
    @Override
    public boolean isInTransaction() {
        return inTransaction;
    }
    
    @Override
    public void close() throws SQLException {
        if (inTransaction) {
            rollback();  // 自动回滚未提交的事务
        }
        if (!connectionFromExternal && connection != null) {
            connection.close();
        }
        // 外部Connection由外部管理，Session不关闭
    }
}
```

### 使用示例

#### 示例1：jrdb-jdbc手动事务控制

```java
try (Session session = sessionFactory.openSession()) {
    session.beginTransaction();
    try {
        // 同一事务中的多个操作
        session.update("UPDATE accounts SET balance = ? WHERE id = ?", 100, 1L);
        session.update("UPDATE accounts SET balance = ? WHERE id = ?", 200, 2L);
        
        // 使用Annotation DAO
        AccountDao accountDao = daoRegistry.getDao(AccountDao.class, session);
        accountDao.updateAccount(account);
        
        session.commit();
    } catch (Exception e) {
        session.rollback();
        throw e;
    }
}
```

#### 示例2：jrdb-jdbc-spring函数式API

```java
@Service
public class AccountService {
    
    @Autowired
    private JrdbTransactionTemplate transactionTemplate;
    
    @Autowired
    private DaoRegistry daoRegistry;
    
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        transactionTemplate.execute(session -> {
            session.update("UPDATE accounts SET balance = balance - ? WHERE id = ?", 
                amount, fromId);
            
            AccountDao dao = daoRegistry.getDao(AccountDao.class, session);
            Account account = dao.selectById(toId);
            account.setBalance(account.getBalance().add(amount));
            dao.update(account);
            
            return null;  // 自动commit，异常自动rollback
        });
    }
    
    public Account createAccount(String name, BigDecimal balance) {
        return transactionTemplate.execute(session -> {
            AccountDao dao = daoRegistry.getDao(AccountDao.class, session);
            Account account = new Account();
            account.setName(name);
            account.setBalance(balance);
            dao.insert(account);
            return dao.selectById(account.getId());
        });
    }
}
```

#### 示例3：jrdb-jdbc-spring + Spring @Transactional

```java
@Service
public class AccountService {
    
    @Autowired
    private JrdbSpringTransactionTemplate transactionTemplate;
    
    @Autowired
    private DaoRegistry daoRegistry;
    
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void transferWithSpringTx(Long fromId, Long toId, BigDecimal amount) {
        transactionTemplate.executeInSpringTransaction(session -> {
            session.update("UPDATE accounts SET balance = balance - ? WHERE id = ?", 
                amount, fromId);
            session.update("UPDATE accounts SET balance = balance + ? WHERE id = ?", 
                amount, toId);
            return null;
            // Spring管理事务边界，自动commit/rollback
        });
    }
    
    @Transactional(readOnly = true)
    public Account getAccount(Long id) {
        return transactionTemplate.executeInSpringTransaction(session -> {
            AccountDao dao = daoRegistry.getDao(AccountDao.class, session);
            return dao.selectById(id);
        });
    }
}
```

### 模块结构

```
jrdb/
├── jrdb-jdbc/                    # 核心模块（零依赖）
│   ├── session/
│   │   ├── Session.java          # 新增4个事务方法
│   │   ├── SessionImpl.java      # 实现事务状态管理
│   │   └── SessionFactory.java   # 保持不变
│   └── transaction/              # 移除或标记废弃
│       ├── Transaction.java      # @Deprecated("Use Session.beginTransaction()")
│       ├── TransactionManager.java  # @Deprecated
│       └── ...
├── jrdb-jdbc-spring/             # Spring集成模块（新增）
│   ├── JrdbTransactionTemplate.java      # 函数式API
│   ├── JrdbSpringTransactionTemplate.java  # Spring事务集成
│   ├── JrdbTransactionException.java     # 事务异常
│   ├── JrdbAutoConfiguration.java        # Spring Boot自动配置
│   └── JrdbProperties.java               # 配置属性
├── jrdb-spring-boot-starter/     # Spring Boot Starter（更新）
│   └── pom.xml                   # 添加jrdb-jdbc-spring依赖
└── jrdb-test/                    # 测试模块
    ├── SessionTransactionTest.java       # jrdb-jdbc事务测试
    ├── JrdbTransactionTemplateTest.java  # jrdb-jdbc-spring测试
    └── SpringTransactionIntegrationTest.java  # Spring集成测试
```

### jrdb-jdbc-spring模块设计

#### pom.xml

```xml
<project>
    <parent>
        <groupId>cn.itcraft</groupId>
        <artifactId>jrdb</artifactId>
        <version>1.2.0-dev</version>
    </parent>
    
    <artifactId>jrdb-jdbc-spring</artifactId>
    <name>jrdb-jdbc-spring</name>
    <description>jrdb JDBC Spring Integration</description>
    
    <dependencies>
        <!-- jrdb核心 -->
        <dependency>
            <groupId>cn.itcraft</groupId>
            <artifactId>jrdb-jdbc</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <!-- Spring依赖 -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
            <version>${spring.version}</version>
            <scope>provided</scope>
        </dependency>
        
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-tx</artifactId>
            <version>${spring.version}</version>
            <scope>provided</scope>
        </dependency>
        
        <!-- 可选：Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
            <version>${spring-boot.version}</version>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

#### JrdbAutoConfiguration.java

```java
package cn.itcraft.jrdb.jdbc.spring.boot;

import cn.itcraft.jrdb.jdbc.spring.JrdbTransactionTemplate;
import cn.itcraft.jrdb.jdbc.spring.JrdbSpringTransactionTemplate;
import cn.itcraft.jrdb.jdbc.session.SessionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@ConditionalOnClass({SessionFactory.class, JrdbTransactionTemplate.class})
public class JrdbAutoConfiguration {
    
    /**
     * 函数式事务模板（无Spring事务管理）。
     */
    @Bean
    @ConditionalOnBean(SessionFactory.class)
    @ConditionalOnMissingBean(JrdbTransactionTemplate.class)
    public JrdbTransactionTemplate jrdbTransactionTemplate(SessionFactory sessionFactory) {
        return new JrdbTransactionTemplate(sessionFactory);
    }
    
    /**
     * Spring事务集成模板（需要PlatformTransactionManager）。
     */
    @Bean
    @ConditionalOnBean({SessionFactory.class, PlatformTransactionManager.class})
    @ConditionalOnMissingBean(JrdbSpringTransactionTemplate.class)
    public JrdbSpringTransactionTemplate jrdbSpringTransactionTemplate(
            SessionFactory sessionFactory,
            PlatformTransactionManager transactionManager) {
        return new JrdbSpringTransactionTemplate(sessionFactory, transactionManager);
    }
}
```

#### JrdbProperties.java

```java
package cn.itcraft.jrdb.jdbc.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jrdb.jdbc.transaction")
public class JrdbProperties {
    
    /**
     * 默认事务隔离级别（可选）。
     */
    private Integer defaultIsolationLevel;
    
    /**
     * 是否启用Spring事务集成（默认true）。
     */
    private boolean springTransactionEnabled = true;
    
    // getters and setters
}
```

### SessionImpl实现变更

```java
public class SessionImpl implements Session {
    private DataSource dataSource;
    private Connection connection;
    private boolean inTransaction = false;
    
    @Override
    public void beginTransaction() throws SQLException {
        if (inTransaction) {
            throw new IllegalStateException("Already in transaction");
        }
        Connection conn = getConnection();
        if (conn.getAutoCommit()) {
            conn.setAutoCommit(false);
        }
        inTransaction = true;
    }
    
    @Override
    public void commit() throws SQLException {
        if (!inTransaction) {
            throw new IllegalStateException("Not in transaction");
        }
        getConnection().commit();
        inTransaction = false;
        getConnection().setAutoCommit(true);  // 恢复自动提交
    }
    
    @Override
    public void rollback() throws SQLException {
        if (!inTransaction) {
            throw new IllegalStateException("Not in transaction");
        }
        getConnection().rollback();
        inTransaction = false;
        getConnection().setAutoCommit(true);  // 恢复自动提交
    }
    
    @Override
    public boolean isInTransaction() {
        return inTransaction;
    }
    
    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        if (inTransaction) {
            throw new IllegalStateException("Cannot set isolation level after beginTransaction");
        }
        getConnection().setTransactionIsolation(level);
    }
    
    @Override
    public void close() throws SQLException {
        if (inTransaction) {
            rollback();  // 自动回滚未提交的事务
        }
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
```

## 实现计划

### Phase 1：jrdb-jdbc核心API（P0）

**目标**：Session添加手动事务控制

**任务**：
1. Session接口添加5个事务方法（beginTransaction/commit/rollback/isInTransaction/setTransactionIsolation）
2. SessionImpl实现事务状态管理
3. 废弃Transaction/TransactionManager（标记@Deprecated）
4. 单元测试：SessionTransactionTest（10个测试场景）

**预期时间**：2小时

### Phase 2：jrdb-jdbc-spring模块创建（P0）

**目标**：创建Spring集成模块，提供函数式API

**任务**：
1. 创建jrdb-jdbc-spring模块结构
2. JrdbTransactionTemplate实现
3. JrdbTransactionException定义
4. 单元测试：JrdbTransactionTemplateTest（8个测试场景）

**预期时间**：1小时

### Phase 3：Spring @Transactional集成（P1）

**目标**：支持Spring事务管理器

**任务**：
1. JrdbSpringTransactionTemplate实现
2. JrdbAutoConfiguration自动配置
3. JrdbProperties配置属性
4. 集成测试：SpringTransactionIntegrationTest（5个测试场景）

**预期时间**：1.5小时

### Phase 4：更新jrdb-spring-boot-starter（P2）

**目标**：整合jrdb-jdbc-spring到starter

**任务**：
1. pom.xml添加jrdb-jdbc-spring依赖
2. 更新自动配置导入
3. 更新文档示例

**预期时间**：0.5小时

### Phase 5：文档更新（P2）

**目标**：更新README和CHANGELOG

**任务**：
1. README添加事务API示例
2. README_cn添加事务API示例
3. CHANGELOG记录事务功能
4. 对比文档更新：jrdb vs MyBatis事务

**预期时间**：1小时

## 性能考虑

**事务性能开销**：
- beginTransaction：仅设置autoCommit=false，开销极小
- commit/rollback：JDBC原生操作，开销相同
- executeInTransaction：Lambda调用，开销极小

**预期性能**：
- 事务模式下查询性能不变（~2M ops/s）
- 事务模式下更新性能不变（~350K ops/s）

## 总结

**设计方案**：方案1 + Spring独立模块

**核心API**：
- **jrdb-jdbc**：Session手动事务控制（beginTransaction/commit/rollback）
- **jrdb-jdbc-spring**：JrdbTransactionTemplate函数式API + JrdbSpringTransactionTemplate Spring集成

**模块职责清晰**：
| 模块 | API风格 | 适用场景 | Spring依赖 |
|------|---------|---------|-----------|
| **jrdb-jdbc** | 手动控制 | 无Spring环境、底层控制 | 无 |
| **jrdb-jdbc-spring** | 函数式 | Spring环境、便捷开发 | 有（optional） |

**核心价值**：
1. **jrdb-jdbc零依赖** - 核心模块不引入Spring依赖，保持轻量
2. **API层次清晰** - 手动控制（底层）+ 函数式（便捷）+ Spring集成（生态）
3. **与Annotation/AutoCrud无缝集成** - 事务中可使用DAO
4. **废弃旧Transaction API** - 统一到Session，避免Connection分离问题

**预期性能**：事务模式下查询/更新性能不变（~2M/~350K ops/s）

**实现时间预估**：6小时（Phase 1-5）