package cn.itcraft.jrdb.benchmark;

import cn.itcraft.jrdb.jdbc.config.JrdbConfig;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.memory.MemoryPool;
import cn.itcraft.jrdb.jdbc.memory.MemoryPoolConfig;
import cn.itcraft.jrdb.jdbc.query.QueryResult;
import cn.itcraft.jrdb.jdbc.session.SessionImpl;
import cn.itcraft.jrdb.jdbc.type.TypeHandlerRegistry;
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
public class JrdbQueryBenchmark extends BenchmarkBase {

    private SessionImpl session;
    private MemoryPool memoryPool;
    private MapperRegistry mapperRegistry;

    @Setup(Level.Trial)
    public void setup() throws SQLException {
        initDatabase();

        MemoryPoolConfig poolConfig = MemoryPoolConfig.builder()
            .preAllocMemSize(64 * 1024 * 1024)
            .queryCacheSize(4 * 1024 * 1024)
            .build();
        
        JrdbConfig config = JrdbConfig.builder()
            .memoryPoolConfig(poolConfig)
            .build();

        memoryPool = new MemoryPool(poolConfig);
        mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        DataSource ds = dataSource;
        session = new SessionImpl(memoryPool, config, ds, mapperRegistry);
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