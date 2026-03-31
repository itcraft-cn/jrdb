package cn.itcraft.jrdb.core.session;

import cn.itcraft.jrdb.core.handler.RowHandler;
import cn.itcraft.jrdb.core.mapping.EntityMapper;
import cn.itcraft.jrdb.core.memory.MemoryPool;
import cn.itcraft.jrdb.core.query.QueryResult;

import java.util.List;
import java.util.function.Function;

/**
 * 数据库会话接口，提供查询和更新操作的核心API。
 *
 * <p>Session是jrdb框架的主要入口点，支持：
 * <ul>
 *   <li>SQL查询 - 返回QueryResult对象</li>
 *   <li>实体查询 - 通过ASM映射返回实体对象</li>
 *   <li>流式查询 - 通过RowHandler处理原始字节数据</li>
 *   <li>更新操作 - INSERT/UPDATE/DELETE</li>
 *   <li>批量操作 - 批量执行同一SQL</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * try (Session session = sessionFactory.openSession()) {
 *     // 原始查询
 *     QueryResult result = session.query("SELECT * FROM users WHERE id = ?", 1L);
 *     
 *     // ASM实体查询
 *     User user = session.queryForObject("SELECT * FROM users WHERE id = ?", User.class, 1L);
 *     List<User> users = session.queryForList("SELECT * FROM users", User.class);
 * }
 * }</pre>
 *
 * @see QueryResult
 * @see RowHandler
 * @see EntityMapper
 */
public interface Session extends AutoCloseable {

    /**
     * 执行无参数查询。
     *
     * @param sql SQL语句
     * @return 查询结果，使用后需要关闭
     */
    QueryResult query(String sql);

    /**
     * 执行带参数查询。
     *
     * @param sql    SQL语句，使用?作为参数占位符
     * @param params 参数值数组
     * @return 查询结果，使用后需要关闭
     */
    QueryResult query(String sql, Object... params);

    /**
     * 执行查询并使用handler处理结果。
     *
     * <p>自动管理QueryResult的关闭。
     *
     * @param sql     SQL语句
     * @param handler 结果处理函数
     * @param <T>     返回类型
     * @return handler处理后的结果
     */
    <T> T query(String sql, Function<QueryResult, T> handler);

    /**
     * 查询单个实体对象（ASM映射）。
     *
     * <p>使用ASM动态生成的映射器将ResultSet转换为实体对象。
     * 如果查询结果为空，返回null。
     *
     * @param sql         SQL语句
     * @param entityClass 实体类
     * @param params      参数值数组
     * @param <T>         实体类型
     * @return 实体对象，如果无结果则返回null
     */
    <T> T queryForObject(String sql, Class<T> entityClass, Object... params);

    /**
     * 查询实体列表（ASM映射）。
     *
     * <p>使用ASM动态生成的映射器将ResultSet转换为实体列表。
     *
     * @param sql         SQL语句
     * @param entityClass 实体类
     * @param params      参数值数组
     * @param <T>         实体类型
     * @return 实体列表，不会返回null
     */
    <T> List<T> queryForList(String sql, Class<T> entityClass, Object... params);

    /**
     * 使用RowHandler流式处理查询结果。
     *
     * <p>适用于ETL、数据导出等需要零对象创建的场景。
     * 每行数据以原始字节数组形式传递，避免对象创建开销。
     *
     * @param sql     SQL语句
     * @param handler 行处理器
     */
    void queryWithHandler(String sql, RowHandler handler);

    /**
     * 使用RowHandler流式处理带参数的查询结果。
     *
     * @param sql     SQL语句
     * @param handler 行处理器
     * @param params  参数值数组
     */
    void queryWithHandler(String sql, RowHandler handler, Object... params);

    /**
     * 执行无参数更新操作。
     *
     * @param sql SQL语句（INSERT/UPDATE/DELETE）
     * @return 影响的行数
     */
    int update(String sql);

    /**
     * 执行带参数更新操作。
     *
     * @param sql    SQL语句
     * @param params 参数值数组
     * @return 影响的行数
     */
    int update(String sql, Object... params);

    /**
     * 批量执行多条不同的SQL语句。
     *
     * @param sqls SQL语句数组
     * @return 每条SQL影响的行数数组
     */
    int[] batchUpdate(String... sqls);

    /**
     * 批量执行同一SQL语句（Raw方式）。
     *
     * <p>适用于批量INSERT/UPDATE/DELETE操作。
     *
     * @param sql        SQL语句
     * @param paramsList 参数列表，每个元素对应一次执行
     * @return 每次执行影响的行数数组
     */
    int[] batchExecute(String sql, List<Object[]> paramsList);

    /**
     * 批量执行同一SQL语句（ASM映射方式）。
     *
     * <p>自动将实体映射为SQL参数。
     *
     * @param sql      SQL语句
     * @param entities 实体列表
     * @param mapper   实体映射器
     * @param <T>      实体类型
     * @return 每次执行影响的行数数组
     */
    <T> int[] batchExecute(String sql, List<T> entities, EntityMapper<T> mapper);

    /**
     * 获取关联的内存池。
     *
     * @return 内存池实例
     */
    MemoryPool getMemoryPool();

    /**
     * 检查会话是否已关闭。
     *
     * @return true表示已关闭
     */
    boolean isClosed();

    /**
     * 关闭会话，释放资源。
     *
     * <p>关闭后会话不可再用。
     */
    void close();
}