package cn.itcraft.jrdb.r2dbc.session;

import cn.itcraft.jrdb.r2dbc.future.PooledFuture;

import java.util.List;

public interface R2dbcSession extends AutoCloseable {

    PooledFuture queryForObject(String sql, Class<?> typeClass, Object... params);

    PooledFuture queryForList(String sql, Class<?> typeClass, Object... params);

    <T> PooledFuture queryOne(String sql, R2dbcRowMapper<T> mapper, Object... params);

    <T> PooledFuture queryList(String sql, R2dbcRowMapper<T> mapper, Object... params);

    PooledFuture update(String sql, Object... params);

    PooledFuture insert(String sql, Object... params);

    PooledFuture insertWithKey(String sql, Class<?> keyType, Object... params);

    PooledFuture delete(String sql, Object... params);

    PooledFuture batchUpdate(String sql, List<Object[]> paramsList);

    PooledFuture batchInsert(String sql, List<Object[]> paramsList);

    boolean isClosed();

    void close();

    @FunctionalInterface
    interface R2dbcRowMapper<T> {
        T map(io.r2dbc.spi.Row row);
    }
}