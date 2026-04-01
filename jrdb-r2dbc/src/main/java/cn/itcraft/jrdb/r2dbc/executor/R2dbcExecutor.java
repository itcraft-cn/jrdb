package cn.itcraft.jrdb.r2dbc.executor;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.Statement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class R2dbcExecutor {

    private static final int DEFAULT_POOL_SIZE = 10;
    private static final Duration DEFAULT_MAX_LIFE_TIME = Duration.ofMinutes(30);
    private static final Duration DEFAULT_MAX_IDLE_TIME = Duration.ofMinutes(10);

    private final ConnectionPool connectionPool;
    private volatile boolean closed;

    public R2dbcExecutor(ConnectionFactory connectionFactory) {
        this.connectionPool = createPool(connectionFactory);
    }

    public R2dbcExecutor(ConnectionFactory connectionFactory, int poolSize) {
        this.connectionPool = createPool(connectionFactory, poolSize);
    }

    private ConnectionPool createPool(ConnectionFactory connectionFactory) {
        return createPool(connectionFactory, DEFAULT_POOL_SIZE);
    }

    private ConnectionPool createPool(ConnectionFactory connectionFactory, int poolSize) {
        ConnectionPoolConfiguration config = ConnectionPoolConfiguration.builder(connectionFactory)
            .initialSize(poolSize / 2)
            .maxSize(poolSize)
            .maxLifeTime(DEFAULT_MAX_LIFE_TIME)
            .maxIdleTime(DEFAULT_MAX_IDLE_TIME)
            .build();
        return new ConnectionPool(config);
    }

    public <T> CompletableFuture<T> queryOne(String sql, Function<Row, T> mapper, Object... params) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Mono.from(connectionPool.create())
            .flatMap(conn -> executeQueryOne(conn, sql, mapper, params))
            .subscribe(future::complete, future::completeExceptionally);
        return future;
    }

    public <T> CompletableFuture<List<T>> queryList(String sql, Function<Row, T> mapper, Object... params) {
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        Mono.from(connectionPool.create())
            .flatMapMany(conn -> executeQueryList(conn, sql, mapper, params))
            .collectList()
            .subscribe(future::complete, future::completeExceptionally);
        return future;
    }

    public CompletableFuture<Integer> update(String sql, Object... params) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        Mono.from(connectionPool.create())
            .flatMap(conn -> executeUpdate(conn, sql, params))
            .subscribe(future::complete, future::completeExceptionally);
        return future;
    }

    public CompletableFuture<Boolean> insert(String sql, Object... params) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Mono.from(connectionPool.create())
            .flatMap(conn -> executeInsert(conn, sql, params))
            .subscribe(future::complete, future::completeExceptionally);
        return future;
    }

    public <T> CompletableFuture<T> insertWithKey(String sql, Class<T> keyType, Object... params) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Mono.from(connectionPool.create())
            .flatMap(conn -> executeInsertWithKey(conn, sql, keyType, params))
            .subscribe(future::complete, future::completeExceptionally);
        return future;
    }

    public CompletableFuture<int[]> batch(String sql, List<Object[]> paramsList) {
        CompletableFuture<int[]> future = new CompletableFuture<>();
        Mono.from(connectionPool.create())
            .flatMap(conn -> executeBatch(conn, sql, paramsList))
            .subscribe(future::complete, future::completeExceptionally);
        return future;
    }

    private <T> Mono<T> executeQueryOne(Connection conn, String sql, Function<Row, T> mapper, Object[] params) {
        Statement stmt = conn.createStatement(sql);
        bindParams(stmt, params);
        return Mono.from(stmt.execute())
            .flatMap(result -> Mono.from(result.map((row, meta) -> mapper.apply(row))))
            .doFinally(signal -> closeConnection(conn));
    }

    private <T> Flux<T> executeQueryList(Connection conn, String sql, Function<Row, T> mapper, Object[] params) {
        Statement stmt = conn.createStatement(sql);
        bindParams(stmt, params);
        return Flux.from(stmt.execute())
            .flatMap(result -> result.map((row, meta) -> mapper.apply(row)))
            .doFinally(signal -> closeConnection(conn));
    }

    private Mono<Integer> executeUpdate(Connection conn, String sql, Object[] params) {
        Statement stmt = conn.createStatement(sql);
        bindParams(stmt, params);
        return Mono.from(stmt.execute())
            .flatMap(result -> Mono.from(result.getRowsUpdated()))
            .map(Long::intValue)
            .doFinally(signal -> closeConnection(conn));
    }

    private Mono<Boolean> executeInsert(Connection conn, String sql, Object[] params) {
        Statement stmt = conn.createStatement(sql);
        bindParams(stmt, params);
        return Mono.from(stmt.execute())
            .flatMap(result -> Mono.from(result.getRowsUpdated()))
            .map(rows -> rows > 0)
            .doFinally(signal -> closeConnection(conn));
    }

    @SuppressWarnings("unchecked")
    private <T> Mono<T> executeInsertWithKey(Connection conn, String sql, Class<T> keyType, Object[] params) {
        Statement stmt = conn.createStatement(sql).returnGeneratedValues();
        bindParams(stmt, params);
        return Mono.from(stmt.execute())
            .flatMap(result -> Mono.from(result.map((row, meta) -> (T) row.get(0, keyType))))
            .doFinally(signal -> closeConnection(conn));
    }

    private Mono<int[]> executeBatch(Connection conn, String sql, List<Object[]> paramsList) {
        return Flux.fromIterable(paramsList)
            .flatMap(params -> {
                Statement stmt = conn.createStatement(sql);
                bindParams(stmt, params);
                return Mono.from(stmt.execute())
                    .flatMap(result -> Mono.from(result.getRowsUpdated()).map(Long::intValue));
            })
            .collectList()
            .map(list -> list.stream().mapToInt(Integer::intValue).toArray())
            .doFinally(signal -> closeConnection(conn));
    }

    private void bindParams(Statement stmt, Object[] params) {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                stmt.bind(i, params[i]);
            }
        }
    }

    private void closeConnection(Connection conn) {
        Mono.from(conn.close()).subscribe();
    }

    public void shutdown() {
        if (!closed) {
            closed = true;
            connectionPool.dispose();
        }
    }
}