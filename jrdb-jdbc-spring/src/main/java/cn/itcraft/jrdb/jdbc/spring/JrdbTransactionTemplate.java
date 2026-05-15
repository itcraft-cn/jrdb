package cn.itcraft.jrdb.jdbc.spring;

import cn.itcraft.jrdb.jdbc.session.Session;
import cn.itcraft.jrdb.jdbc.session.SessionFactory;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * jrdb事务模板，提供函数式API。
 *
 * <p>自动管理事务边界：
 * <ul>
 *   <li>自动beginTransaction</li>
 *   <li>成功自动commit</li>
 *   <li>异常自动rollback</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * @Autowired
 * private JrdbTransactionTemplate transactionTemplate;
 *
 * public void transfer(Long fromId, Long toId, BigDecimal amount) {
 *     transactionTemplate.execute(session -> {
 *         session.update("UPDATE accounts SET balance = balance - ? WHERE id = ?", amount, fromId);
 *         session.update("UPDATE accounts SET balance = balance + ? WHERE id = ?", amount, toId);
 *         return null;
 *     });
 * }
 * }</pre>
 */
public class JrdbTransactionTemplate {

    private final SessionFactory sessionFactory;

    /**
     * 创建事务模板。
     *
     * @param sessionFactory 会话工厂，不能为null
     * @throws NullPointerException 如果sessionFactory为null
     */
    public JrdbTransactionTemplate(SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory cannot be null");
    }

    /**
     * 在事务中执行操作，自动提交或回滚。
     *
     * <p>执行流程：
     * <ol>
     *   <li>打开Session</li>
     *   <li>beginTransaction</li>
     *   <li>执行action</li>
     *   <li>成功commit，异常rollback</li>
     * </ol>
     *
     * @param action 要执行的操作
     * @param <T>    返回类型
     * @return action的返回值
     * @throws JrdbTransactionException 如果事务执行失败
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
        } catch (Exception e) {
            if (e instanceof JrdbTransactionException) {
                throw e;
            }
            throw new JrdbTransactionException("Transaction error", e);
        }
    }

    /**
     * 在事务中执行操作（无返回值）。
     *
     * @param action 要执行的操作
     * @throws JrdbTransactionException 如果事务执行失败
     */
    public void executeWithoutResult(Consumer<Session> action) {
        execute(session -> {
            action.accept(session);
            return null;
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
}