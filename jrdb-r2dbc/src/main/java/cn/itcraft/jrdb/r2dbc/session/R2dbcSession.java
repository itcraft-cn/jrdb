package cn.itcraft.jrdb.r2dbc.session;

import io.r2dbc.spi.Row;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * R2DBC响应式会话接口，提供非阻塞的数据库操作API。
 *
 * <p>所有数据库操作返回CompletableFuture，支持响应式编程模型。
 *
 * @author Helly Guo
 * @since 1.2.0
 */
public interface R2dbcSession extends AutoCloseable {

    /**
     * 查询单个对象。
     *
     * @param sql SQL语句
     * @param typeClass 结果类型
     * @param params 参数
     * @return 异步结果
     */
    <T> CompletableFuture<T> queryOne(String sql, Class<T> typeClass, Object... params);

    /**
     * 查询列表。
     *
     * @param sql SQL语句
     * @param typeClass 结果类型
     * @param params 参数
     * @return 异步结果列表
     */
    <T> CompletableFuture<List<T>> queryList(String sql, Class<T> typeClass, Object... params);

    /**
     * 查询单个对象（使用自定义映射器）。
     *
     * @param sql SQL语句
     * @param mapper 行映射器
     * @param params 参数
     * @return 异步结果
     */
    <T> CompletableFuture<T> queryOne(String sql, RowMapper<T> mapper, Object... params);

    /**
     * 查询列表（使用自定义映射器）。
     *
     * @param sql SQL语句
     * @param mapper 行映射器
     * @param params 参数
     * @return 异步结果列表
     */
    <T> CompletableFuture<List<T>> queryList(String sql, RowMapper<T> mapper, Object... params);

    /**
     * 执行更新操作。
     *
     * @param sql SQL语句
     * @param params 参数
     * @return 异步影响的行数
     */
    CompletableFuture<Integer> update(String sql, Object... params);

    /**
     * 执行插入操作。
     *
     * @param sql SQL语句
     * @param params 参数
     * @return 异步是否成功
     */
    CompletableFuture<Boolean> insert(String sql, Object... params);

    /**
     * 执行插入并返回主键。
     *
     * @param sql SQL语句
     * @param keyType 主键类型
     * @param params 参数
     * @return 异步主键值
     */
    <T> CompletableFuture<T> insertWithKey(String sql, Class<T> keyType, Object... params);

    /**
     * 执行删除操作。
     *
     * @param sql SQL语句
     * @param params 参数
     * @return 异步影响的行数
     */
    CompletableFuture<Integer> delete(String sql, Object... params);

    /**
     * 执行批量操作。
     *
     * @param sql SQL语句
     * @param paramsList 参数列表
     * @return 异步影响行数数组
     */
    CompletableFuture<int[]> batch(String sql, List<Object[]> paramsList);

    /**
     * 检查会话是否已关闭。
     *
     * @return 是否已关闭
     */
    boolean isClosed();

    /**
     * 关闭会话。
     */
    void close();

    /**
     * 行映射器函数式接口。
     */
    @FunctionalInterface
    interface RowMapper<T> {
        /**
         * 将行映射为对象。
         *
         * @param row R2DBC行对象
         * @return 映射后的对象
         */
        T map(Row row);
    }
}
