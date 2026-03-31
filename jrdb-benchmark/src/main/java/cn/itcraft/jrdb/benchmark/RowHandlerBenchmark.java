package cn.itcraft.jrdb.benchmark;

import cn.itcraft.jrdb.core.config.CacheLevel;
import cn.itcraft.jrdb.core.config.JrdbConfig;
import cn.itcraft.jrdb.core.mapping.MapperRegistry;
import cn.itcraft.jrdb.core.memory.MemoryPool;
import cn.itcraft.jrdb.core.memory.MemoryPoolConfig;
import cn.itcraft.jrdb.core.query.QueryResult;
import cn.itcraft.jrdb.core.session.SessionImpl;
import cn.itcraft.jrdb.core.type.TypeHandlerRegistry;
import org.h2.jdbcx.JdbcDataSource;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 20, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class RowHandlerBenchmark {

    private static final int DATA_SIZE = 1000;

    @State(Scope.Benchmark)
    public static class SharedState {
        DataSource dataSource;
        MemoryPool memoryPool;
        MapperRegistry mapperRegistry;

        @Setup(Level.Trial)
        public void setup() throws SQLException {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setUrl("jdbc:h2:mem:rowhandlerbench;DB_CLOSE_DELAY=-1");
            ds.setUser("sa");
            ds.setPassword("");
            dataSource = ds;

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                        "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                        "name VARCHAR(100), " +
                        "age INT, " +
                        "email VARCHAR(200), " +
                        "score DOUBLE)");
                stmt.execute("INSERT INTO users (name, age, email, score) " +
                        "SELECT 'user_' || x, 20 + (x % 50), 'user_' || x || '@test.com', 80 + (x % 20) " +
                        "FROM SYSTEM_RANGE(1, " + DATA_SIZE + ")");
            }

            MemoryPoolConfig poolConfig = MemoryPoolConfig.builder()
                    .preAllocMemSize(64 * 1024 * 1024)
                    .queryCacheSize(4 * 1024 * 1024)
                    .build();
            memoryPool = new MemoryPool(poolConfig);
            mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (memoryPool != null) memoryPool.destroy();
        }
    }

    @State(Scope.Thread)
    public static class JrdbState {
        SessionImpl session;

        @Setup(Level.Trial)
        public void setup(SharedState shared) {
            JrdbConfig config = JrdbConfig.builder()
                    .cacheLevel(CacheLevel.NONE)
                    .build();
            session = new SessionImpl(shared.memoryPool, config, shared.dataSource, shared.mapperRegistry);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (session != null) session.close();
        }
    }

    @State(Scope.Thread)
    public static class JdbcState {
        DataSource dataSource;
        Connection connection;
        PreparedStatement queryPs;

        @Setup(Level.Trial)
        public void setup(SharedState shared) throws SQLException {
            this.dataSource = shared.dataSource;
            this.connection = shared.dataSource.getConnection();
            this.queryPs = connection.prepareStatement("SELECT id, name, age, email, score FROM users LIMIT 100");
        }

        @TearDown(Level.Trial)
        public void tearDown() throws SQLException {
            if (queryPs != null) queryPs.close();
            if (connection != null) connection.close();
        }
    }

    @Benchmark
    public void jrdb_rowHandler(JrdbState state, Blackhole bh) {
        state.session.queryWithHandler("SELECT id, name, age, email, score FROM users LIMIT 100", (rawRow, meta) -> {
            bh.consume(rawRow);
        });
    }

    @Benchmark
    public void jrdb_rawQuery(JrdbState state, Blackhole bh) {
        try (QueryResult result = state.session.query("SELECT id, name, age, email, score FROM users LIMIT 100")) {
            while (result != null && result.next()) {
                bh.consume(result.getLong("ID"));
                bh.consume(result.getString("NAME"));
                bh.consume(result.getInt("AGE"));
            }
        }
    }

    @Benchmark
    public void jdbc_manual(JdbcState state, Blackhole bh) throws SQLException {
        try (ResultSet rs = state.queryPs.executeQuery()) {
            while (rs.next()) {
                bh.consume(rs.getLong("id"));
                bh.consume(rs.getString("name"));
                bh.consume(rs.getInt("age"));
            }
        }
    }
}