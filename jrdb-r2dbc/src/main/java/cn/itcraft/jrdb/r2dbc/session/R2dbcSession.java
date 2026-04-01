package cn.itcraft.jrdb.r2dbc.session;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface R2dbcSession extends AutoCloseable {

    <T> Mono<T> queryForObject(String sql, Class<T> typeClass, Object... params);

    <T> Flux<T> queryForList(String sql, Class<T> typeClass, Object... params);

    Mono<Integer> update(String sql, Object... params);

    Mono<Boolean> insert(String sql, Object... params);

    <T> Mono<T> insert(String sql, Class<T> keyType, Object... params);

    Mono<Integer> delete(String sql, Object... params);

    Mono<int[]> batchUpdate(String sql, List<Object[]> paramsList);

    <T> Flux<T> batchInsert(String sql, Class<T> keyType, List<Object[]> paramsList);

    Mono<Integer> batchInsert(String sql, List<Object[]> paramsList);

    Mono<int[]> batchDelete(String sql, List<Object[]> paramsList);

    boolean isClosed();

    void close();
}