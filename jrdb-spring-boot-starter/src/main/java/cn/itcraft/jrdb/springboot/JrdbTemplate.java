package cn.itcraft.jrdb.springboot;

import cn.itcraft.jrdb.jdbc.handler.RowHandler;
import cn.itcraft.jrdb.jdbc.mapping.EntityMapper;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.query.QueryResult;
import cn.itcraft.jrdb.jdbc.session.Session;

import java.util.List;
import java.util.function.Function;

/**
 * jrdb Spring 集成的核心模板类。
 *
 * <p>提供了对 Session 的简单封装，方便在 Spring 环境中使用。
 *
 * <p>使用示例：
 * <pre>{@code
 * @Autowired
 * private JrdbTemplate jrdbTemplate;
 *
 * public User getUser(Long id) {
 *     return jrdbTemplate.queryForObject("SELECT * FROM users WHERE id = ?", User.class, id);
 * }
 * }</pre>
 */
public class JrdbTemplate {

    private final Session session;
    private final MapperRegistry mapperRegistry;

    public JrdbTemplate(Session session, MapperRegistry mapperRegistry) {
        this.session = session;
        this.mapperRegistry = mapperRegistry;
    }

    // ==================== Raw API ====================

    /**
     * 执行查询，返回 QueryResult。
     */
    public QueryResult query(String sql, Object... params) {
        return session.query(sql, params);
    }

    /**
     * 执行查询并使用 handler 处理结果。
     */
    public <T> T query(String sql, Function<QueryResult, T> handler) {
        return session.query(sql, handler);
    }

    // ==================== ASM API (推荐) ====================

    /**
     * 查询单个实体对象。
     */
    public <T> T queryForObject(String sql, Class<T> entityType, Object... params) {
        return session.queryForObject(sql, entityType, params);
    }

    /**
     * 查询实体列表。
     */
    public <T> List<T> queryForList(String sql, Class<T> entityType, Object... params) {
        return session.queryForList(sql, entityType, params);
    }

    // ==================== Streaming API (推荐) ====================

    /**
     * 流式处理查询结果。
     */
    public void queryWithHandler(String sql, RowHandler handler, Object... params) {
        session.queryWithHandler(sql, handler, params);
    }

    // ==================== Update ====================

    /**
     * 执行更新操作。
     */
    public int update(String sql, Object... params) {
        return session.update(sql, params);
    }

    // ==================== Batch ====================

    /**
     * 批量执行同一 SQL。
     */
    public int[] batchUpdate(String sql, List<Object[]> paramsList) {
        return session.batchUpdate(sql, paramsList);
    }

    /**
     * 批量执行同一 SQL（实体映射方式）。
     */
    public <T> int[] batchUpdate(String sql, List<T> entities, EntityMapper<T> mapper) {
        return session.batchUpdate(sql, entities, mapper);
    }

    // ==================== Accessors ====================

    /**
     * 获取底层 Session。
     */
    public Session getSession() {
        return session;
    }

    /**
     * 获取 MapperRegistry。
     */
    public MapperRegistry getMapperRegistry() {
        return mapperRegistry;
    }

    /**
     * 获取实体映射器。
     */
    public <T> EntityMapper<T> getMapper(Class<T> entityType) {
        return mapperRegistry.getMapper(entityType);
    }
}