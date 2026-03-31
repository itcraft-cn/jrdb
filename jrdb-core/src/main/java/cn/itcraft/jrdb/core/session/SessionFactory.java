package cn.itcraft.jrdb.core.session;

import cn.itcraft.jrdb.core.config.JrdbConfig;
import cn.itcraft.jrdb.core.mapping.MapperRegistry;
import cn.itcraft.jrdb.core.memory.MemoryPool;
import cn.itcraft.jrdb.core.memory.MemoryPoolConfig;
import cn.itcraft.jrdb.core.type.TypeHandlerRegistry;

import javax.sql.DataSource;

/**
 * 会话工厂，负责创建和管理数据库会话。
 *
 * <p>{@code SessionFactory} 是 jrdb 框架的核心入口点，负责：
 * <ul>
 *   <li>管理内存池的生命周期</li>
 *   <li>管理实体映射器注册表</li>
 *   <li>创建 {@link Session} 实例</li>
 *   <li>持有数据源引用</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * JrdbConfig config = JrdbConfig.builder().build();
 * DataSource dataSource = ...;
 * SessionFactory factory = new SessionFactory(config, dataSource);
 * try (Session session = factory.openSession()) {
 *     User user = session.queryForObject("SELECT * FROM users WHERE id = ?", User.class, 1L);
 * }
 * factory.close();
 * }</pre>
 *
 * <p>该类是线程安全的，可在多线程环境中共享使用。
 *
 * @author jrdb
 * @see Session
 * @see JrdbConfig
 * @see MemoryPool
 * @see MapperRegistry
 */
public class SessionFactory {

    private final JrdbConfig config;
    private final MemoryPool memoryPool;
    private final DataSource dataSource;
    private final MapperRegistry mapperRegistry;
    private volatile boolean closed;

    /**
     * 使用指定配置构造会话工厂（不绑定数据源）。
     *
     * <p>此构造函数创建的工厂无法打开会话，需要后续通过其他方式配置数据源。
     *
     * @param config jrdb 配置，不能为null
     * @throws NullPointerException 如果 config 为 null
     */
    public SessionFactory(JrdbConfig config) {
        this(config, null);
    }

    /**
     * 使用指定配置和数据源构造会话工厂。
     *
     * @param config     jrdb 配置，不能为null
     * @param dataSource 数据源，可为null但会导致 openSession 抛出异常
     * @throws NullPointerException 如果 config 为 null
     */
    public SessionFactory(JrdbConfig config, DataSource dataSource) {
        if (config == null) {
            throw new NullPointerException("config cannot be null");
        }
        this.config = config;
        this.dataSource = dataSource;
        MemoryPoolConfig poolConfig = config.getMemoryPoolConfig();
        this.memoryPool = new MemoryPool(poolConfig);
        this.mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        this.closed = false;
    }

    /**
     * 打开一个新的数据库会话。
     *
     * <p>每次调用都会创建一个新的 {@link Session} 实例。
     * 调用者负责在使用完毕后关闭会话。
     *
     * @return 新的会话实例
     * @throws IllegalStateException 如果工厂已关闭或未配置数据源
     */
    public Session openSession() {
        if (closed) {
            throw new IllegalStateException("SessionFactory is closed");
        }
        if (dataSource == null) {
            throw new IllegalStateException("DataSource not configured");
        }
        return new SessionImpl(memoryPool, config, dataSource, mapperRegistry);
    }

    /**
     * 获取实体映射器注册表。
     *
     * <p>可通过此方法注册自定义映射配置。
     *
     * @return 映射器注册表实例
     */
    public MapperRegistry getMapperRegistry() {
        return mapperRegistry;
    }

    /**
     * 关闭会话工厂并释放所有资源。
     *
     * <p>关闭后，内存池将被销毁，后续调用 {@link #openSession()} 将抛出异常。
     * 此方法是线程安全的，多次调用不会产生副作用。
     */
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        memoryPool.destroy();
    }

    /**
     * 检查会话工厂是否已关闭。
     *
     * @return 如果已关闭返回true，否则返回false
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * 获取与此工厂关联的内存池。
     *
     * @return 内存池实例
     */
    public MemoryPool getMemoryPool() {
        return memoryPool;
    }
}