package com.dbmmap.benchmark;

import com.dbmmap.core.config.DbMmapConfig;
import com.dbmmap.core.memory.MemoryPool;
import com.dbmmap.core.memory.MemoryPoolConfig;
import com.dbmmap.core.query.QueryResult;
import com.dbmmap.core.session.SessionImpl;
import org.h2.jdbcx.JdbcDataSource;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class CacheBenchmark {

    private SessionImpl sessionWithCache;
    private SessionImpl sessionNoCache;
    private SessionImpl sessionNoMetaCache;
    private SessionImpl sessionNoStatementCache;
    private MemoryPool memoryPool;
    private DataSource dataSource;

    @Setup(Level.Trial)
    public void setup() throws SQLException {
        dataSource = createDataSource();
        initDatabase();
        memoryPool = createMemoryPool();

        DbMmapConfig configWithCache = DbMmapConfig.builder()
            .statementCacheEnabled(true)
            .queryMetaCacheEnabled(true)
            .statementCacheSize(256)
            .build();

        DbMmapConfig configNoCache = DbMmapConfig.builder()
            .statementCacheEnabled(false)
            .queryMetaCacheEnabled(false)
            .build();

        DbMmapConfig configNoMetaCache = DbMmapConfig.builder()
            .statementCacheEnabled(true)
            .queryMetaCacheEnabled(false)
            .build();

        DbMmapConfig configNoStatementCache = DbMmapConfig.builder()
            .statementCacheEnabled(false)
            .queryMetaCacheEnabled(true)
            .build();

        sessionWithCache = new SessionImpl(memoryPool, configWithCache, dataSource);
        sessionNoCache = new SessionImpl(memoryPool, configNoCache, dataSource);
        sessionNoMetaCache = new SessionImpl(memoryPool, configNoMetaCache, dataSource);
        sessionNoStatementCache = new SessionImpl(memoryPool, configNoStatementCache, dataSource);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (sessionWithCache != null) sessionWithCache.close();
        if (sessionNoCache != null) sessionNoCache.close();
        if (sessionNoMetaCache != null) sessionNoMetaCache.close();
        if (sessionNoStatementCache != null) sessionNoStatementCache.close();
        if (memoryPool != null) memoryPool.destroy();
    }

    private DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setUrl("jdbc:h2:mem:cachebench;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private MemoryPool createMemoryPool() {
        MemoryPoolConfig poolConfig = MemoryPoolConfig.builder()
            .preAllocMemSize(64 * 1024 * 1024)
            .queryCacheSize(4 * 1024 * 1024)
            .build();
        return new MemoryPool(poolConfig);
    }

    private void initDatabase() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS test_data (" +
                "id BIGINT PRIMARY KEY, " +
                "name VARCHAR(100), " +
                "val INT)");
            stmt.execute("INSERT INTO test_data VALUES (1, 'test', 100)");
        }
    }

    @Benchmark
    public void withFullCache(Blackhole bh) {
        try (QueryResult result = sessionWithCache.query("SELECT id, name, val FROM test_data WHERE id = ?", 1L)) {
            if (result != null && result.next()) {
                bh.consume(result.getLong("id"));
                bh.consume(result.getString("name"));
                bh.consume(result.getInt("val"));
            }
        }
    }

    @Benchmark
    public void noCache(Blackhole bh) {
        try (QueryResult result = sessionNoCache.query("SELECT id, name, val FROM test_data WHERE id = ?", 1L)) {
            if (result != null && result.next()) {
                bh.consume(result.getLong("id"));
                bh.consume(result.getString("name"));
                bh.consume(result.getInt("val"));
            }
        }
    }

    @Benchmark
    public void noMetaCache(Blackhole bh) {
        try (QueryResult result = sessionNoMetaCache.query("SELECT id, name, val FROM test_data WHERE id = ?", 1L)) {
            if (result != null && result.next()) {
                bh.consume(result.getLong("id"));
                bh.consume(result.getString("name"));
                bh.consume(result.getInt("val"));
            }
        }
    }

    @Benchmark
    public void noStatementCache(Blackhole bh) {
        try (QueryResult result = sessionNoStatementCache.query("SELECT id, name, val FROM test_data WHERE id = ?", 1L)) {
            if (result != null && result.next()) {
                bh.consume(result.getLong("id"));
                bh.consume(result.getString("name"));
                bh.consume(result.getInt("val"));
            }
        }
    }
}