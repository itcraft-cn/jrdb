package cn.itcraft.jrdb.jdbc.spring;

import cn.itcraft.jrdb.jdbc.session.Session;
import cn.itcraft.jrdb.jdbc.session.SessionFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Spring事务管理器集成。
 *
 * <p>与Spring TransactionTemplate集成，支持Spring声明式事务管理。
 * 可与Spring @Transactional注解配合使用。
 *
 * <p>使用示例：
 * <pre>{@code
 * @Autowired
 * private JrdbSpringTransactionTemplate transactionTemplate;
 *
 * @Transactional(propagation = Propagation.REQUIRED)
 * public void transfer(Long fromId, Long toId, BigDecimal amount) {
 *     transactionTemplate.executeInSpringTransaction(session -> {
 *         session.update(...);
 *         // Spring自动管理事务边界
 *     });
 * }
 * }</pre>
 */
public class JrdbSpringTransactionTemplate {

    private final SessionFactory sessionFactory;
    private final PlatformTransactionManager transactionManager;

    /**
     * 创建Spring事务模板。
     *
     * @param sessionFactory      会话工厂，不能为null
     * @param transactionManager Spring事务管理器，不能为null
     * @throws NullPointerException 如果任一参数为null
     */
    public JrdbSpringTransactionTemplate(
            SessionFactory sessionFactory,
            PlatformTransactionManager transactionManager) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager cannot be null");
    }

    /**
     * 在Spring管理的事务中执行。
     *
     * <p>使用Spring TransactionTemplate管理事务边界。
     *
     * @param action 要执行的操作
     * @param <T>    返回类型
     * @return action的返回值
     * @throws JrdbTransactionException 如果事务执行失败
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
            } catch (Exception e) {
                status.setRollbackOnly();
                if (e instanceof JrdbTransactionException) {
                    throw e;
                }
                throw new JrdbTransactionException("Transaction error", e);
            }
        });
    }

    /**
     * 在Spring管理的事务中执行（无返回值）。
     *
     * @param action 要执行的操作
     * @throws JrdbTransactionException 如果事务执行失败
     */
    public void executeInSpringTransactionWithoutResult(Consumer<Session> action) {
        executeInSpringTransaction(session -> {
            action.accept(session);
            return null;
        });
    }

    /**
     * 使用自定义TransactionDefinition执行事务。
     *
     * @param definition 事务定义（隔离级别、传播行为等）
     * @param action     要执行的操作
     * @param <T>        返回类型
     * @return action的返回值
     * @throws JrdbTransactionException 如果事务执行失败
     */
    public <T> T executeInSpringTransaction(
            TransactionDefinition definition,
            Function<Session, T> action) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager, definition);
        return txTemplate.execute(status -> {
            try (Session session = sessionFactory.openSession()) {
                if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
                    session.setTransactionIsolation(definition.getIsolationLevel());
                }
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
            } catch (Exception e) {
                status.setRollbackOnly();
                if (e instanceof JrdbTransactionException) {
                    throw e;
                }
                throw new JrdbTransactionException("Transaction error", e);
            }
        });
    }

    /**
     * 获取会话工厂。
     *
     * @return 会话工厂实例
     */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * 获取Spring事务管理器。
     *
     * @return Spring事务管理器实例
     */
    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }
}