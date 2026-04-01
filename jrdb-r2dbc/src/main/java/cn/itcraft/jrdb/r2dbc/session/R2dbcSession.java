package cn.itcraft.jrdb.r2dbc.session;

import io.r2dbc.spi.Row;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface R2dbcSession extends AutoCloseable {

    <T> CompletableFuture<T> queryOne(String sql, Class<T> typeClass, Object... params);

    <T> CompletableFuture<List<T>> queryList(String sql, Class<T> typeClass, Object... params);

    <T> CompletableFuture<T> queryOne(String sql, RowMapper<T> mapper, Object... params);

    <T> CompletableFuture<List<T>> queryList(String sql, RowMapper<T> mapper, Object... params);

    CompletableFuture<Integer> update(String sql, Object... params);

    CompletableFuture<Boolean> insert(String sql, Object... params);

    <T> CompletableFuture<T> insertWithKey(String sql, Class<T> keyType, Object... params);

    CompletableFuture<Integer> delete(String sql, Object... params);

    CompletableFuture<int[]> batch(String sql, List<Object[]> paramsList);

    boolean isClosed();

    void close();

    @FunctionalInterface
    interface RowMapper<T> {
        T map(Row row);
    }
}