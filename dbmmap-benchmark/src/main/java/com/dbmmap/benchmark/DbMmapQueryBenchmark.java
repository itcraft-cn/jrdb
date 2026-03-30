package com.dbmmap.benchmark;

import com.dbmmap.core.config.DbMmapConfig;
import com.dbmmap.core.memory.MemoryPoolConfig;
import com.dbmmap.core.query.QueryResult;
import com.dbmmap.core.session.Session;
import com.dbmmap.core.session.SessionFactory;
import com.dbmmap.core.sql.BoundSql;
import com.dbmmap.core.sql.SqlDsl;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class DbMmapQueryBenchmark extends BenchmarkBase {

    private SessionFactory sessionFactory;
    private Session session;

    @Setup(Level.Trial)
    public void setup() throws SQLException {
        initDatabase();

        DbMmapConfig config = DbMmapConfig.builder()
            .memoryPoolConfig(MemoryPoolConfig.builder()
                .preAllocMemSize(64 * 1024 * 1024)
                .queryCacheSize(4 * 1024 * 1024)
                .build())
            .build();

        sessionFactory = new SessionFactory(config);
        session = sessionFactory.openSession();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (session != null) session.close();
        if (sessionFactory != null) sessionFactory.close();
    }

    @Benchmark
    public void singleQuery(Blackhole bh) {
        BoundSql sql = SqlDsl.select("id", "name", "age", "email", "status")
            .from("users")
            .where("id = ?", 1L)
            .build();

        try (QueryResult result = session.query(sql.getSql())) {
            if (result != null && result.next()) {
                bh.consume(result.getLong("id"));
                bh.consume(result.getString("name"));
                bh.consume(result.getInt("age"));
            }
        }
    }

    @Benchmark
    public void queryAll(Blackhole bh) {
        BoundSql sql = SqlDsl.select("id", "name", "age", "email", "status")
            .from("users")
            .build();

        try (QueryResult result = session.query(sql.getSql())) {
            if (result != null) {
                while (result.next()) {
                    bh.consume(result.getLong("id"));
                }
            }
        }
    }
}