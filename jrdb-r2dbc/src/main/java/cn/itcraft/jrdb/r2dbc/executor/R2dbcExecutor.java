package cn.itcraft.jrdb.r2dbc.executor;

import cn.itcraft.jrdb.r2dbc.config.R2dbcExecutorConfig;
import cn.itcraft.jrdb.r2dbc.future.PooledFuture;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Statement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.*;

public class R2dbcExecutor {

    private final ConnectionFactory connectionFactory;
    private final ThreadPoolExecutor executor;

    public R2dbcExecutor(ConnectionFactory connectionFactory, R2dbcExecutorConfig config) {
        this.connectionFactory = connectionFactory;
        this.executor = new ThreadPoolExecutor(
            config.getCorePoolSize(),
            config.getMaxPoolSize(),
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(config.getQueueCapacity()),
            config.createThreadFactory(),
            (r, exec) -> r.run()
        );
    }

    private PooledFuture createFuture() {
        return PooledFuture.fetch();
    }

    public PooledFuture executeQueryOne(String sql, RowMapper<?> mapper, Object... params) {
        PooledFuture future = createFuture();
        try {
            Mono.from(connectionFactory.create())
                .flatMap(conn -> {
                    Statement stmt = conn.createStatement(sql);
                    bindParams(stmt, params);
                    return Mono.from(stmt.execute())
                        .flatMap(result -> Mono.from(result.map((row, meta) -> mapper.map(row))))
                        .doFinally(signal -> Mono.from(conn.close()).subscribe());
                })
                .subscribe(
                    value -> future.complete(value),
                    error -> future.completeExceptionally(error),
                    () -> { if (!future.isDone()) future.complete(null); }
                );
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public PooledFuture executeQuery(String sql, RowMapper<?> mapper, Object... params) {
        PooledFuture future = createFuture();
        try {
            try {
                Mono.from(connectionFactory.create())
                    .flatMapMany(conn -> {
                        Statement stmt = conn.createStatement(sql);
                        bindParams(stmt, params);
                        return Flux.from(stmt.execute())
                            .flatMap(result -> result.map((row, meta) -> mapper.map(row)))
                            .doFinally(signal -> Mono.from(conn.close()).subscribe());
                    })
                    .collectList()
                    .subscribe(
                        list -> future.complete(list),
                        error -> future.completeExceptionally(error)
                    );
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } catch (Exception e) { future.completeExceptionally(e); }
        return future;
    }

    public PooledFuture executeUpdate(String sql, Object... params) {
        PooledFuture future = createFuture();
        try {
            try {
                Mono.from(connectionFactory.create())
                    .flatMap(conn -> {
                        Statement stmt = conn.createStatement(sql);
                        bindParams(stmt, params);
                        return Mono.from(stmt.execute())
                            .flatMap(result -> Mono.from(result.getRowsUpdated()))
                            .map(Long::intValue)
                            .doFinally(signal -> Mono.from(conn.close()).subscribe());
                    })
                    .subscribe(
                        value -> future.complete(value),
                        error -> future.completeExceptionally(error)
                    );
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } catch (Exception e) { future.completeExceptionally(e); }
        return future;
    }

    public PooledFuture executeUpdateBoolean(String sql, Object... params) {
        PooledFuture future = createFuture();
        try {
            try {
                Mono.from(connectionFactory.create())
                    .flatMap(conn -> {
                        Statement stmt = conn.createStatement(sql);
                        bindParams(stmt, params);
                        return Mono.from(stmt.execute())
                            .flatMap(result -> Mono.from(result.getRowsUpdated()))
                            .map(rows -> rows > 0)
                            .doFinally(signal -> Mono.from(conn.close()).subscribe());
                    })
                    .subscribe(
                        value -> future.complete(value),
                        error -> future.completeExceptionally(error)
                    );
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } catch (Exception e) { future.completeExceptionally(e); }
        return future;
    }

    public PooledFuture executeInsertWithKey(String sql, Class<?> keyType, Object... params) {
        PooledFuture future = createFuture();
        try {
            try {
                Mono.from(connectionFactory.create())
                    .flatMap(conn -> {
                        Statement stmt = conn.createStatement(sql).returnGeneratedValues();
                        bindParams(stmt, params);
                        return Mono.from(stmt.execute())
                            .flatMap(result -> Mono.from(result.map((row, meta) -> row.get(0, keyType))))
                            .doFinally(signal -> Mono.from(conn.close()).subscribe());
                    })
                    .subscribe(
                        value -> future.complete(value),
                        error -> future.completeExceptionally(error)
                    );
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } catch (Exception e) { future.completeExceptionally(e); }
        return future;
    }

    public PooledFuture executeBatch(String sql, List<Object[]> paramsList) {
        PooledFuture future = createFuture();
        try {
            try {
                Mono.from(connectionFactory.create())
                    .flatMap(conn -> {
                        return Flux.fromIterable(paramsList)
                            .flatMap(params -> {
                                Statement stmt = conn.createStatement(sql);
                                bindParams(stmt, params);
                                return Mono.from(stmt.execute())
                                    .flatMap(result -> Mono.from(result.getRowsUpdated()).map(Long::intValue));
                            })
                            .collectList()
                            .map(list -> list.stream().mapToInt(Integer::intValue).toArray())
                            .doFinally(signal -> Mono.from(conn.close()).subscribe());
                    })
                    .subscribe(
                        value -> future.complete(value),
                        error -> future.completeExceptionally(error)
                    );
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } catch (Exception e) { future.completeExceptionally(e); }
        return future;
    }

    public PooledFuture executeBatchInsert(String sql, List<Object[]> paramsList) {
        PooledFuture future = createFuture();
        try {
            try {
                Mono.from(connectionFactory.create())
                    .flatMap(conn -> {
                        return Flux.fromIterable(paramsList)
                            .flatMap(params -> {
                                Statement stmt = conn.createStatement(sql);
                                bindParams(stmt, params);
                                return Mono.from(stmt.execute())
                                    .flatMap(result -> Mono.from(result.getRowsUpdated()).map(Long::intValue));
                            })
                            .collectList()
                            .map(list -> {
                                int count = 0;
                                for (int i : list) {
                                    if (i > 0) count++;
                                }
                                return count;
                            })
                            .doFinally(signal -> Mono.from(conn.close()).subscribe());
                    })
                    .subscribe(
                        value -> future.complete(value),
                        error -> future.completeExceptionally(error)
                    );
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } catch (Exception e) { future.completeExceptionally(e); }
        return future;
    }

    private void bindParams(Statement stmt, Object... params) {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                stmt.bind(i, params[i]);
            }
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(io.r2dbc.spi.Row row);
    }
}