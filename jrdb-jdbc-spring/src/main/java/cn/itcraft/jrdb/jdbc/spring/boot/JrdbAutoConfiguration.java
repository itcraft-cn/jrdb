package cn.itcraft.jrdb.jdbc.spring.boot;

import cn.itcraft.jrdb.jdbc.spring.JrdbSpringTransactionTemplate;
import cn.itcraft.jrdb.jdbc.spring.JrdbTransactionTemplate;
import cn.itcraft.jrdb.jdbc.session.SessionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * jrdb Spring Boot自动配置。
 *
 * <p>根据条件自动装配jrdb事务模板：
 * <ul>
 *   <li>JrdbTransactionTemplate - 函数式事务API（无需Spring事务管理器）</li>
 *   <li>JrdbSpringTransactionTemplate - Spring事务集成（需要PlatformTransactionManager）</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * @Autowired
 * private JrdbTransactionTemplate transactionTemplate;  // 自动注入
 * }</pre>
 */
@Configuration
@ConditionalOnClass({SessionFactory.class, JrdbTransactionTemplate.class})
public class JrdbAutoConfiguration {

    /**
     * 创建jrdb事务模板（函数式API）。
     *
     * <p>仅需要SessionFactory，无需Spring事务管理器。
     *
     * @param sessionFactory jrdb会话工厂
     * @return JrdbTransactionTemplate实例
     */
    @Bean
    @ConditionalOnBean(SessionFactory.class)
    @ConditionalOnMissingBean(JrdbTransactionTemplate.class)
    public JrdbTransactionTemplate jrdbTransactionTemplate(SessionFactory sessionFactory) {
        return new JrdbTransactionTemplate(sessionFactory);
    }

    /**
     * 创建Spring事务集成模板。
     *
     * <p>需要SessionFactory和PlatformTransactionManager，
     * 与Spring声明式事务管理集成。
     *
     * @param sessionFactory      jrdb会话工厂
     * @param transactionManager Spring事务管理器
     * @return JrdbSpringTransactionTemplate实例
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