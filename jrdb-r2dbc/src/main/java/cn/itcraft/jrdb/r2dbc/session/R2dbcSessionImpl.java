package cn.itcraft.jrdb.r2dbc.session;

import cn.itcraft.jrdb.r2dbc.executor.R2dbcExecutor;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class R2dbcSessionImpl implements R2dbcSession {

    private final R2dbcExecutor executor;
    private volatile boolean closed;

    public R2dbcSessionImpl(ConnectionFactory connectionFactory) {
        this.executor = new R2dbcExecutor(connectionFactory);
        this.closed = false;
    }

    @Override
    public <T> CompletableFuture<T> queryOne(String sql, Class<T> typeClass, Object... params) {
        checkClosed();
        return executor.queryOne(sql, row -> row.get(0, typeClass), params);
    }

    @Override
    public <T> CompletableFuture<List<T>> queryList(String sql, Class<T> typeClass, Object... params) {
        checkClosed();
        return executor.queryList(sql, row -> row.get(0, typeClass), params);
    }

    @Override
    public <T> CompletableFuture<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        checkClosed();
        return executor.queryOne(sql, mapper::map, params);
    }

    @Override
    public <T> CompletableFuture<List<T>> queryList(String sql, RowMapper<T> mapper, Object... params) {
        checkClosed();
        return executor.queryList(sql, mapper::map, params);
    }

    @Override
    public CompletableFuture<Integer> update(String sql, Object... params) {
        checkClosed();
        return executor.update(sql, params);
    }

    @Override
    public CompletableFuture<Boolean> insert(String sql, Object... params) {
        checkClosed();
        return executor.insert(sql, params);
    }

    @Override
    public <T> CompletableFuture<T> insertWithKey(String sql, Class<T> keyType, Object... params) {
        checkClosed();
        return executor.insertWithKey(sql, keyType, params);
    }

    @Override
    public CompletableFuture<Integer> delete(String sql, Object... params) {
        checkClosed();
        return executor.update(sql, params);
    }

    @Override
    public CompletableFuture<int[]> batch(String sql, List<Object[]> paramsList) {
        checkClosed();
        return executor.batch(sql, paramsList);
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        closed = true;
        executor.shutdown();
    }
}