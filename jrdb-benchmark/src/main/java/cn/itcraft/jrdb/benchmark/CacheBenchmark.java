package cn.itcraft.jrdb.benchmark;

import cn.itcraft.jrdb.core.config.CacheLevel;
import cn.itcraft.jrdb.core.config.JrdbConfig;
import cn.itcraft.jrdb.core.memory.MemoryPool;
import cn.itcraft.jrdb.core.memory.MemoryPoolConfig;
import cn.itcraft.jrdb.core.query.QueryResult;
import cn.itcraft.jrdb.core.session.SessionImpl;
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
@Measurement(iterations = 5, time = 20, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class CacheBenchmark {

    private SessionImpl sessionNone;
    private SessionImpl sessionL1;
    private SessionImpl sessionL2;
    private MemoryPool memoryPool;
    private DataSource dataSource;

    @Setup(Level.Trial)
    public void setup() throws SQLException {
        dataSource = createDataSource();
        initDatabase();
        memoryPool = createMemoryPool();

        JrdbConfig configNone = JrdbConfig.builder()
            .cacheLevel(CacheLevel.NONE)
            .build();

        JrdbConfig configL1 = JrdbConfig.builder()
            .cacheLevel(CacheLevel.L1)
            .statementCacheSize(256)
            .build();

        JrdbConfig configL2 = JrdbConfig.builder()
            .cacheLevel(CacheLevel.L2)
            .statementCacheSize(256)
            .resultCacheSize(1000)
            .resultCacheExpireSeconds(300)
            .build();

        sessionNone = new SessionImpl(memoryPool, configNone, dataSource);
        sessionL1 = new SessionImpl(memoryPool, configL1, dataSource);
        sessionL2 = new SessionImpl(memoryPool, configL2, dataSource);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (sessionNone != null) sessionNone.close();
        if (sessionL1 != null) sessionL1.close();
        if (sessionL2 != null) sessionL2.close();
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
    public void cacheNone(Blackhole bh) {
        try (QueryResult result = sessionNone.query("SELECT id, name, val FROM test_data WHERE id = ?", 1L)) {
            if (result != null && result.next()) {
                bh.consume(result.getLong("ID"));
                bh.consume(result.getString("NAME"));
                bh.consume(result.getInt("VAL"));
            }
        }
    }

    @Benchmark
    public void cacheL1(Blackhole bh) {
        try (QueryResult result = sessionL1.query("SELECT id, name, val FROM test_data WHERE id = ?", 1L)) {
            if (result != null && result.next()) {
                bh.consume(result.getLong("ID"));
                bh.consume(result.getString("NAME"));
                bh.consume(result.getInt("VAL"));
            }
        }
    }

    @Benchmark
    public void cacheL2(Blackhole bh) {
        try (QueryResult result = sessionL2.query("SELECT id, name, val FROM test_data WHERE id = ?", 1L)) {
            if (result != null && result.next()) {
                bh.consume(result.getLong("ID"));
                bh.consume(result.getString("NAME"));
                bh.consume(result.getInt("VAL"));
            }
        }
    }
}