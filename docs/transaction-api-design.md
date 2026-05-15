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

## 推荐方案：方案5（混合）

### API设计

#### Session接口扩展

```java
public interface Session {
    // 现有方法保持不变...
    
    // ===== 新增事务API =====
    
    /**
     * 开始事务。
     * 调用此方法后，Session将使用单一Connection，
     * 所有操作在同一事务中执行，直到commit或rollback。
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
}
```

#### SessionFactory扩展

```java
public class SessionFactory {
    // 现有方法保持不变...
    
    // ===== 新增事务API =====
    
    /**
     * 在事务中执行操作，自动提交或回滚。
     */
    public <T> T executeInTransaction(Function<Session, T> function) {
        try (Session session = openSession()) {
            session.beginTransaction();
            try {
                T result = function.apply(session);
                session.commit();
                return result;
            } catch (Exception e) {
                session.rollback();
                throw new RuntimeException("Transaction failed", e);
            }
        }
    }
    
    /**
     * 在事务中执行操作（无返回值），自动提交或回滚。
     */
    public void runInTransaction(Consumer<Session> consumer) {
        executeInTransaction(session -> {
            consumer.accept(session);
            return null;
        });
    }
    
    /**
     * 使用外部Connection创建Session（用于与外部事务管理集成）。
     */
    public Session openSession(Connection connection) {
        // Session绑定到外部Connection，不自动关闭
        return new SessionImpl(memoryPool, config, connection, mapperRegistry);
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

#### 示例1：手动事务控制

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

#### 示例2：自动事务管理（推荐）

```java
sessionFactory.executeInTransaction(session -> {
    // 多个操作自动在同一事务中
    session.update("UPDATE accounts SET balance = ? WHERE id = ?", 100, 1L);
    
    AccountDao accountDao = sessionFactory.getDaoRegistry().getDao(AccountDao.class, session);
    accountDao.updateAccount(account);
    
    return null;  // 或返回结果
});
```

#### 示例3：与Spring @Transactional集成

```java
@Service
public class AccountService {
    
    @Autowired
    private SessionFactory sessionFactory;
    
    @Transactional  // Spring管理事务
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        // Spring提供Connection并管理事务
        Connection conn = TransactionSynchronizationManager.getCurrentTransactionConnection();
        
        // jrdb使用外部Connection，不管理事务
        Session session = sessionFactory.openSession(conn);
        session.update("UPDATE accounts SET balance = balance - ? WHERE id = ?", amount, fromId);
        session.update("UPDATE accounts SET balance = balance + ? WHERE id = ?", amount, toId);
        // 不需要commit，Spring会自动管理
    }
}
```

### 与Spring Boot集成设计

```java
@Configuration
public class JrdbTransactionConfiguration {
    
    @Bean
    public JrdbTransactionTemplate transactionTemplate(SessionFactory sessionFactory) {
        return new JrdbTransactionTemplate(sessionFactory);
    }
}

public class JrdbTransactionTemplate {
    private final SessionFactory sessionFactory;
    
    public <T> T execute(Function<Session, T> function) {
        return sessionFactory.executeInTransaction(function);
    }
}

// 使用示例
@Autowired
private JrdbTransactionTemplate transactionTemplate;

public void transfer() {
    transactionTemplate.execute(session -> {
        session.update(...);
        return null;
    });
}
```

## 实现计划

### Phase 1：Session事务API
1. Session接口添加4个事务方法
2. SessionImpl实现事务状态管理
3. 单元测试：SessionTransactionTest

### Phase 2：SessionFactory函数式API
1. SessionFactory添加executeInTransaction/runInTransaction
2. SessionFactory添加openSession(Connection)
3. 单元测试：SessionFactoryTransactionTest

### Phase 3：与Annotation/AutoCrud集成
1. 确保DAO在事务Session中正常工作
2. 集成测试：TransactionWithDaoTest

### Phase 4：Spring Boot集成
1. JrdbTransactionTemplate
2. 与Spring @Transactional集成（使用PlatformTransactionManager）
3. 集成测试：SpringTransactionTest

### Phase 5：文档更新
1. README添加事务API示例
2. CHANGELOG记录事务功能
3. 与MyBatis/Hibernate事务对比文档

## 性能考虑

**事务性能开销**：
- beginTransaction：仅设置autoCommit=false，开销极小
- commit/rollback：JDBC原生操作，开销相同
- executeInTransaction：Lambda调用，开销极小

**预期性能**：
- 事务模式下查询性能不变（~2M ops/s）
- 事务模式下更新性能不变（~350K ops/s）

## 总结

**推荐方案**：方案5（混合），提供3种事务使用方式：
1. **手动控制**：session.beginTransaction/commit/rollback
2. **自动管理**：sessionFactory.executeInTransaction
3. **外部集成**：sessionFactory.openSession(Connection)

**核心价值**：
- 与Annotation/AutoCrud无缝集成
- 函数式API简洁易用
- 支持Spring @Transactional集成
- 性能无损失