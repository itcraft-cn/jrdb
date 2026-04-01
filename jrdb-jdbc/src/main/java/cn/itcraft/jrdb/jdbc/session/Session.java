package cn.itcraft.jrdb.jdbc.session;

import cn.itcraft.jrdb.jdbc.handler.RowHandler;
import cn.itcraft.jrdb.jdbc.mapping.EntityMapper;
import cn.itcraft.jrdb.jdbc.memory.MemoryPool;
import cn.itcraft.jrdb.jdbc.query.QueryResult;

import java.util.List;
import java.util.function.Function;

/**
 * 数据库会话接口，提供查询和更新操作的核心API。
 *
 * <p>Session是jrdb框架的主要入口点，支持：
 * <ul>
 *   <li>Raw查询 - 返回QueryResult对象</li>
 *   <li>ASM查询 - 通过动态映射返回实体对象</li>
 *   <li>流式查询 - 通过RowHandler处理原始字节数据</li>
 *   <li>更新操作 - INSERT/UPDATE/DELETE</li>
 *   <li>批量操作 - 批量执行同一SQL</li>
 * </ul>
 *
 * <p>API层次：
 * <ul>
 *   <li>ASM API (主推) - queryForObject/queryForList</li>
 *   <li>Streaming API (主推) - queryWithHandler</li>
 *   <li>Raw API (底层能力) - query</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * try (Session session = sessionFactory.openSession()) {
 *     // ASM实体查询（推荐）
 *     User user = session.queryForObject("SELECT * FROM users WHERE id = ?", User.class, 1L);
 *     List<User> users = session.queryForList("SELECT * FROM users", User.class);
 *     
 *     // 流式处理（推荐，ETL场景）
 *     session.queryWithHandler("SELECT * FROM users", (rawRow, meta) -> {
 *         // 处理原始字节数据
 *     });
 *     
 *     // Raw查询（底层能力）
 *     try (QueryResult result = session.query("SELECT * FROM users WHERE id = ?", 1L)) {
 *         if (result.next()) {
 *             String name = result.getString("NAME");
 *         }
 *     }
 * }
 * }</pre>
 *
 * @see QueryResult
 * @see RowHandler
 * @see EntityMapper
 */
public interface Session extends AutoCloseable {

    // ==================== Raw API ====================

    /**
     * 执行SQL查询，返回QueryResult对象。
     *
     * <p>QueryResult使用堆外内存存储结果，使用完毕后必须关闭。
     *
     * @param sql    SQL语句，使用?作为参数占位符
     * @param params 参数值数组（可空）
     * @return 查询结果，使用后需要关闭
     */
    QueryResult query(String sql, Object... params);

    /**
     * 执行查询并使用handler处理结果。
     *
     * <p>自动管理QueryResult的关闭，适合需要自定义结果处理的场景。
     *
     * @param sql     SQL语句
     * @param handler 结果处理函数
     * @param <T>     返回类型
     * @return handler处理后的结果
     */
    <T> T query(String sql, Function<QueryResult, T> handler);

    // ==================== ASM API (主推) ====================

    /**
     * 查询单个实体对象（ASM映射）。
     *
     * <p>使用ASM动态生成的映射器将ResultSet转换为实体对象。
     * 如果查询结果为空，返回null。
     *
     * @param sql         SQL语句
     * @param entityClass 实体类
     * @param params      参数值数组（可空）
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
     * @param params      参数值数组（可空）
     * @param <T>         实体类型
     * @return 实体列表，不会返回null
     */
    <T> List<T> queryForList(String sql, Class<T> entityClass, Object... params);

    // ==================== Streaming API (主推) ====================

    /**
     * 使用RowHandler流式处理查询结果。
     *
     * <p>适用于ETL、数据导出等需要零对象创建的场景。
     * 每行数据以原始字节数组形式传递，避免对象创建开销。
     *
     * @param sql     SQL语句
     * @param handler 行处理器
     * @param params  参数值数组（可空）
     */
    void queryWithHandler(String sql, RowHandler handler, Object... params);

    // ==================== Update ====================

    /**
     * 执行更新操作（INSERT/UPDATE/DELETE）。
     *
     * @param sql    SQL语句
     * @param params 参数值数组（可空）
     * @return 影响的行数
     */
    int update(String sql, Object... params);

    /**
     * 执行DELETE操作。
     *
     * <p>等价于 update(sql, params)，语义更清晰。
     *
     * @param sql    DELETE语句
     * @param params 参数值数组（可空）
     * @return 影响的行数
     */
    int delete(String sql, Object... params);

    // ==================== Insert ====================

    /**
     * 执行INSERT操作并返回生成的主键。
     *
     * <p>适用于单条INSERT，返回自增主键值。
     * 主键类型由调用方指定，支持 Long、Integer、String 等类型。
     *
     * @param sql        INSERT语句
     * @param keyType    主键类型
     * @param params     参数值数组（可空）
     * @param <T>        主键类型
     * @return 生成的主键值，如果没有则返回null
     */
    <T> T insert(String sql, Class<T> keyType, Object... params);

    /**
     * 执行INSERT操作，不返回主键。
     *
     * <p>等价于 update(sql, params) &gt; 0，返回是否成功。
     *
     * @param sql    INSERT语句
     * @param params 参数值数组（可空）
     * @return true表示插入成功
     */
    boolean insert(String sql, Object... params);

    // ==================== Batch ====================

    /**
     * 批量执行同一SQL语句（参数列表方式）。
     *
     * <p>适用于批量INSERT/UPDATE/DELETE操作。
     * 使用PreparedStatement的addBatch/executeBatch实现。
     *
     * @param sql        SQL语句
     * @param paramsList 参数列表，每个元素对应一次执行
     * @return 每次执行影响的行数数组
     */
    int[] batchUpdate(String sql, List<Object[]> paramsList);

    /**
     * 批量执行DELETE操作。
     *
     * <p>等价于 batchUpdate(sql, paramsList)，语义更清晰。
     *
     * @param sql        DELETE语句
     * @param paramsList 参数列表，每个元素对应一次执行
     * @return 每次执行影响的行数数组
     */
    int[] batchDelete(String sql, List<Object[]> paramsList);

    /**
     * 批量INSERT，返回生成的主键列表。
     *
     * <p>使用PreparedStatement的addBatch/executeBatch实现。
     * 主键类型由调用方指定，支持 Long、Integer、String 等类型。
     *
     * @param sql        INSERT语句
     * @param keyType    主键类型
     * @param paramsList 参数列表，每个元素对应一次执行
     * @param <T>        主键类型
     * @return 生成的主键列表
     */
    <T> List<T> batchInsert(String sql, Class<T> keyType, List<Object[]> paramsList);

    /**
     * 批量INSERT，不返回主键。
     *
     * <p>使用PreparedStatement的addBatch/executeBatch实现。
     * 返回成功插入的条数。
     *
     * @param sql        INSERT语句
     * @param paramsList 参数列表，每个元素对应一次执行
     * @return 成功插入的条数
     */
    int batchInsert(String sql, List<Object[]> paramsList);

    /**
     * 批量执行同一SQL语句（实体映射方式）。
     *
     * <p>自动将实体映射为SQL参数。
     *
     * @param sql      SQL语句
     * @param entities 实体列表
     * @param mapper   实体映射器
     * @param <T>      实体类型
     * @return 每次执行影响的行数数组
     */
    <T> int[] batchUpdate(String sql, List<T> entities, EntityMapper<T> mapper);

    // ==================== Lifecycle ====================

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