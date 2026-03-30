package com.dbmmap.benchmark;

import com.dbmmap.core.config.DbMmapConfig;
import com.dbmmap.core.memory.MemoryPool;
import com.dbmmap.core.memory.MemoryPoolConfig;
import com.dbmmap.core.query.QueryResult;
import com.dbmmap.core.session.SessionImpl;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class DbMmapQueryBenchmark extends BenchmarkBase {

    private SessionImpl session;
    private MemoryPool memoryPool;

    @Setup(Level.Trial)
    public void setup() throws SQLException {
        initDatabase();

        MemoryPoolConfig poolConfig = MemoryPoolConfig.builder()
            .preAllocMemSize(64 * 1024 * 1024)
            .queryCacheSize(4 * 1024 * 1024)
            .build();
        
        DbMmapConfig config = DbMmapConfig.builder()
            .memoryPoolConfig(poolConfig)
            .build();

        memoryPool = new MemoryPool(poolConfig);
        DataSource ds = dataSource;
        session = new SessionImpl(memoryPool, config, ds);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (session != null) session.close();
        if (memoryPool != null) memoryPool.destroy();
    }

    @Benchmark
    public void singleQuery(Blackhole bh) {
        try (QueryResult result = session.query("SELECT id, name, age, email, status FROM users WHERE id = ?", 1L)) {
            if (result != null && result.next()) {
                bh.consume(result.getLong("id"));
                bh.consume(result.getString("name"));
                bh.consume(result.getInt("age"));
            }
        }
    }

    @Benchmark
    public void queryAll(Blackhole bh) {
        try (QueryResult result = session.query("SELECT id, name, age, email, status FROM users")) {
            if (result != null) {
                while (result.next()) {
                    bh.consume(result.getLong("id"));
                }
            }
        }
    }
}