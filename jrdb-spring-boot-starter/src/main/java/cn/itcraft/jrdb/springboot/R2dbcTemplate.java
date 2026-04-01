package cn.itcraft.jrdb.springboot;

import cn.itcraft.jrdb.r2dbc.mapping.R2dbcEntityMapper;
import cn.itcraft.jrdb.r2dbc.mapping.R2dbcMapperRegistry;
import cn.itcraft.jrdb.r2dbc.session.R2dbcSession;
import io.r2dbc.spi.Row;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class R2dbcTemplate {

    private final R2dbcSession session;
    private final R2dbcMapperRegistry mapperRegistry;

    public R2dbcTemplate(R2dbcSession session, R2dbcMapperRegistry mapperRegistry) {
        this.session = session;
        this.mapperRegistry = mapperRegistry;
    }

    public <T> CompletableFuture<T> queryOne(String sql, Class<T> typeClass, Object... params) {
        return session.queryOne(sql, typeClass, params);
    }

    public <T> CompletableFuture<List<T>> queryList(String sql, Class<T> typeClass, Object... params) {
        return session.queryList(sql, typeClass, params);
    }

    public <T> CompletableFuture<T> queryOne(String sql, R2dbcSession.RowMapper<T> mapper, Object... params) {
        return session.queryOne(sql, mapper, params);
    }

    public <T> CompletableFuture<List<T>> queryList(String sql, R2dbcSession.RowMapper<T> mapper, Object... params) {
        return session.queryList(sql, mapper, params);
    }

    public CompletableFuture<Integer> update(String sql, Object... params) {
        return session.update(sql, params);
    }

    public CompletableFuture<Boolean> insert(String sql, Object... params) {
        return session.insert(sql, params);
    }

    public <T> CompletableFuture<T> insertWithKey(String sql, Class<T> keyType, Object... params) {
        return session.insertWithKey(sql, keyType, params);
    }

    public CompletableFuture<Integer> delete(String sql, Object... params) {
        return session.delete(sql, params);
    }

    public CompletableFuture<int[]> batch(String sql, List<Object[]> paramsList) {
        return session.batch(sql, paramsList);
    }

    public R2dbcSession getSession() {
        return session;
    }

    public R2dbcMapperRegistry getMapperRegistry() {
        return mapperRegistry;
    }

    public <T> R2dbcEntityMapper<T> getMapper(Class<T> entityType) {
        return mapperRegistry.getMapper(entityType);
    }
}