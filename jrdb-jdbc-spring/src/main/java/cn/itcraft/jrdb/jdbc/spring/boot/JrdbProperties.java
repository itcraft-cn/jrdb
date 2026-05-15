package cn.itcraft.jrdb.jdbc.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * jrdb配置属性。
 *
 * <p>可通过application.yml或application.properties配置：
 * <pre>{@code
 * jrdb.jdbc.transaction.default-isolation-level=2
 * }</pre>
 */
@ConfigurationProperties(prefix = "jrdb.jdbc.transaction")
public class JrdbProperties {

    /**
     * 默认事务隔离级别（可选）。
     *
     * <p>使用JDBC Connection常量值：
     * <ul>
     *   <li>0 - TRANSACTION_NONE</li>
     *   <li>1 - TRANSACTION_READ_UNCOMMITTED</li>
     *   <li>2 - TRANSACTION_READ_COMMITTED</li>
     *   <li>4 - TRANSACTION_REPEATABLE_READ</li>
     *   <li>8 - TRANSACTION_SERIALIZABLE</li>
     * </ul>
     */
    private Integer defaultIsolationLevel;

    /**
     * 是否启用Spring事务集成（默认true）。
     *
     * <p>设为false则不自动装配JrdbSpringTransactionTemplate。
     */
    private boolean springTransactionEnabled = true;

    public Integer getDefaultIsolationLevel() {
        return defaultIsolationLevel;
    }

    public void setDefaultIsolationLevel(Integer defaultIsolationLevel) {
        this.defaultIsolationLevel = defaultIsolationLevel;
    }

    public boolean isSpringTransactionEnabled() {
        return springTransactionEnabled;
    }

    public void setSpringTransactionEnabled(boolean springTransactionEnabled) {
        this.springTransactionEnabled = springTransactionEnabled;
    }
}