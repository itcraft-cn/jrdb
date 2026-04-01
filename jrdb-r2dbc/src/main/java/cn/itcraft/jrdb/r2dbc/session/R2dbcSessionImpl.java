package cn.itcraft.jrdb.r2dbc.session;

import cn.itcraft.jrdb.r2dbc.config.R2dbcConfig;
import cn.itcraft.jrdb.r2dbc.mapping.R2dbcMapperRegistry;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Statement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class R2dbcSessionImpl implements R2dbcSession {

    private final ConnectionFactory connectionFactory;
    private final R2dbcConfig config;
    private final R2dbcMapperRegistry mapperRegistry;
    private volatile boolean closed;

    public R2dbcSessionImpl(ConnectionFactory connectionFactory, R2dbcConfig config, R2dbcMapperRegistry mapperRegistry) {
        this.connectionFactory = connectionFactory;
        this.config = config;
        this.mapperRegistry = mapperRegistry;
        this.closed = false;
    }

    @Override
    public <T> Mono<T> queryForObject(String sql, Class<T> typeClass, Object... params) {
        if (closed) {
            return Mono.error(new IllegalStateException("Session is closed"));
        }
        return Mono.usingWhen(
            connectionFactory.create(),
            conn -> {
                Statement stmt = conn.createStatement(sql);
                bindParameters(stmt, params);
                return Mono.from(stmt.execute())
                    .flatMap(result -> Mono.from(result.map((row, metadata) -> row.get(0, typeClass))));
            },
            Connection::close
        );
    }

    @Override
    public <T> Flux<T> queryForList(String sql, Class<T> typeClass, Object... params) {
        if (closed) {
            return Flux.error(new IllegalStateException("Session is closed"));
        }
        return Flux.usingWhen(
            connectionFactory.create(),
            conn -> {
                Statement stmt = conn.createStatement(sql);
                bindParameters(stmt, params);
                return Flux.from(stmt.execute())
                    .flatMap(result -> result.map((row, metadata) -> row.get(0, typeClass)));
            },
            Connection::close
        );
    }

    @Override
    public Mono<Integer> update(String sql, Object... params) {
        if (closed) {
            return Mono.error(new IllegalStateException("Session is closed"));
        }
        return Mono.usingWhen(
            connectionFactory.create(),
            conn -> {
                Statement stmt = conn.createStatement(sql);
                bindParameters(stmt, params);
                return Mono.from(stmt.execute())
                    .flatMap(result -> Mono.from(result.getRowsUpdated()).map(Long::intValue));
            },
            Connection::close
        );
    }

    @Override
    public Mono<Boolean> insert(String sql, Object... params) {
        return update(sql, params).map(rows -> rows > 0);
    }

    @Override
    public <T> Mono<T> insert(String sql, Class<T> keyType, Object... params) {
        if (closed) {
            return Mono.error(new IllegalStateException("Session is closed"));
        }
        return Mono.usingWhen(
            connectionFactory.create(),
            conn -> {
                Statement stmt = conn.createStatement(sql).returnGeneratedValues();
                bindParameters(stmt, params);
                return Mono.from(stmt.execute())
                    .flatMap(result -> Mono.from(result.map((row, metadata) -> row.get(0, keyType))));
            },
            Connection::close
        );
    }

    @Override
    public Mono<Integer> delete(String sql, Object... params) {
        return update(sql, params);
    }

    @Override
    public Mono<int[]> batchUpdate(String sql, List<Object[]> paramsList) {
        if (closed) {
            return Mono.error(new IllegalStateException("Session is closed"));
        }
        return Mono.usingWhen(
            connectionFactory.create(),
            conn -> {
                io.r2dbc.spi.Batch batch = conn.createBatch();
                for (Object[] params : paramsList) {
                    batch.add(sql);
                }
                return Flux.from(batch.execute())
                    .flatMap(result -> Mono.from(result.getRowsUpdated()).map(Long::intValue))
                    .collectList()
                    .<int[]>map(list -> list.stream().mapToInt(Integer::intValue).toArray());
            },
            Connection::close
        );
    }

    @Override
    public <T> Flux<T> batchInsert(String sql, Class<T> keyType, List<Object[]> paramsList) {
        return Flux.error(new UnsupportedOperationException("Not implemented yet"));
    }

    @Override
    public Mono<Integer> batchInsert(String sql, List<Object[]> paramsList) {
        return batchUpdate(sql, paramsList)
            .map(arr -> {
                int count = 0;
                for (int i : arr) {
                    if (i > 0) count++;
                }
                return count;
            });
    }

    @Override
    public Mono<int[]> batchDelete(String sql, List<Object[]> paramsList) {
        return batchUpdate(sql, paramsList);
    }

    private void bindParameters(Statement stmt, Object... params) {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                stmt.bind(i, params[i]);
            }
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        closed = true;
    }
}