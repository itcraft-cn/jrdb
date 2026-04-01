package cn.itcraft.jrdb.r2dbc.session;

import cn.itcraft.jrdb.r2dbc.config.R2dbcConfig;
import cn.itcraft.jrdb.r2dbc.executor.R2dbcExecutor;
import cn.itcraft.jrdb.r2dbc.future.PooledFuture;
import cn.itcraft.jrdb.r2dbc.mapping.R2dbcMapperRegistry;
import io.r2dbc.spi.ConnectionFactory;

import java.util.List;

public class R2dbcSessionImpl implements R2dbcSession {

    private final R2dbcExecutor executor;
    private final R2dbcMapperRegistry mapperRegistry;
    private volatile boolean closed;

    public R2dbcSessionImpl(ConnectionFactory connectionFactory, R2dbcConfig config, R2dbcMapperRegistry mapperRegistry) {
        this.executor = new R2dbcExecutor(connectionFactory, config.getExecutorConfig());
        this.mapperRegistry = mapperRegistry;
        this.closed = false;
    }

    @Override
    public PooledFuture queryForObject(String sql, Class<?> typeClass, Object... params) {
        checkClosed();
        return executor.executeQueryOne(sql, row -> row.get(0, typeClass), params);
    }

    @Override
    public PooledFuture queryForList(String sql, Class<?> typeClass, Object... params) {
        checkClosed();
        return executor.executeQuery(sql, row -> row.get(0, typeClass), params);
    }

    @Override
    public <T> PooledFuture queryOne(String sql, R2dbcRowMapper<T> mapper, Object... params) {
        checkClosed();
        return executor.executeQueryOne(sql, mapper::map, params);
    }

    @Override
    public <T> PooledFuture queryList(String sql, R2dbcRowMapper<T> mapper, Object... params) {
        checkClosed();
        return executor.executeQuery(sql, mapper::map, params);
    }

    @Override
    public PooledFuture update(String sql, Object... params) {
        checkClosed();
        return executor.executeUpdate(sql, params);
    }

    @Override
    public PooledFuture insert(String sql, Object... params) {
        checkClosed();
        return executor.executeUpdateBoolean(sql, params);
    }

    @Override
    public PooledFuture insertWithKey(String sql, Class<?> keyType, Object... params) {
        checkClosed();
        return executor.executeInsertWithKey(sql, keyType, params);
    }

    @Override
    public PooledFuture delete(String sql, Object... params) {
        checkClosed();
        return executor.executeUpdate(sql, params);
    }

    @Override
    public PooledFuture batchUpdate(String sql, List<Object[]> paramsList) {
        checkClosed();
        return executor.executeBatch(sql, paramsList);
    }

    @Override
    public PooledFuture batchInsert(String sql, List<Object[]> paramsList) {
        checkClosed();
        return executor.executeBatchInsert(sql, paramsList);
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