package cn.itcraft.jrdb.benchmark;

import cn.itcraft.jrdb.jdbc.config.CacheLevel;
import cn.itcraft.jrdb.jdbc.config.JrdbConfig;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.memory.MemoryPool;
import cn.itcraft.jrdb.jdbc.memory.MemoryPoolConfig;
import cn.itcraft.jrdb.jdbc.session.SessionImpl;
import cn.itcraft.jrdb.jdbc.type.TypeHandlerRegistry;
import org.h2.jdbcx.JdbcDataSource;
import org.openjdk.jmh.annotations.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 20, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
public class DirectSqlBenchmark {

    private static final int BATCH_SIZE = 100;

    @State(Scope.Benchmark)
    public static class SharedState {
        DataSource dataSource;
        MemoryPool memoryPool;
        MapperRegistry mapperRegistry;

        @Setup(Level.Trial)
        public void setup() throws SQLException {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setUrl("jdbc:h2:mem:directsql;DB_CLOSE_DELAY=-1");
            ds.setUser("sa");
            ds.setPassword("");
            dataSource = ds;

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "name VARCHAR(100), " +
                    "age INT, " +
                    "email VARCHAR(200))");
            }

            MemoryPoolConfig poolConfig = MemoryPoolConfig.builder()
                .preAllocMemSize(64 * 1024 * 1024)
                .queryCacheSize(4 * 1024 * 1024)
                .build();
            memoryPool = new MemoryPool(poolConfig);

            TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();
            mapperRegistry = new MapperRegistry(typeHandlerRegistry);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (memoryPool != null) memoryPool.destroy();
        }

        void clearTable() throws SQLException {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM users");
            }
        }
    }

    @State(Scope.Thread)
    public static class PreparedStatementState {
        SessionImpl session;
        List<Object[]> paramsList;

        @Setup(Level.Invocation)
        public void setup(SharedState shared) throws SQLException {
            shared.clearTable();
            JrdbConfig config = JrdbConfig.builder()
                .cacheLevel(CacheLevel.NONE)
                .build();
            session = new SessionImpl(shared.memoryPool, config, shared.dataSource, shared.mapperRegistry);
            paramsList = generateInsertParams();
        }

        @TearDown(Level.Invocation)
        public void tearDown() {
            if (session != null) session.close();
        }

        private List<Object[]> generateInsertParams() {
            List<Object[]> list = new ArrayList<>(BATCH_SIZE);
            for (int i = 0; i < BATCH_SIZE; i++) {
                list.add(new Object[]{"user_" + i, 20 + (i % 50), "user_" + i + "@test.com"});
            }
            return list;
        }
    }

    @State(Scope.Thread)
    public static class DirectSqlState {
        SessionImpl session;
        List<Object[]> rows;
        String[] columns = {"name", "age", "email"};

        @Setup(Level.Invocation)
        public void setup(SharedState shared) throws SQLException {
            shared.clearTable();
            JrdbConfig config = JrdbConfig.builder()
                .cacheLevel(CacheLevel.NONE)
                .build();
            session = new SessionImpl(shared.memoryPool, config, shared.dataSource, shared.mapperRegistry);
            rows = generateInsertRows();
        }

        @TearDown(Level.Invocation)
        public void tearDown() {
            if (session != null) session.close();
        }

        private List<Object[]> generateInsertRows() {
            List<Object[]> list = new ArrayList<>(BATCH_SIZE);
            for (int i = 0; i < BATCH_SIZE; i++) {
                list.add(new Object[]{"user_" + i, 20 + (i % 50), "user_" + i + "@test.com"});
            }
            return list;
        }
    }

    @State(Scope.Thread)
    public static class UpdatePreparedStatementState {
        SessionImpl session;
        List<Object[]> paramsList;

        @Setup(Level.Invocation)
        public void setup(SharedState shared) throws SQLException {
            shared.clearTable();
            insertData(shared);
            JrdbConfig config = JrdbConfig.builder()
                .cacheLevel(CacheLevel.NONE)
                .build();
            session = new SessionImpl(shared.memoryPool, config, shared.dataSource, shared.mapperRegistry);
            paramsList = generateUpdateParams();
        }

        @TearDown(Level.Invocation)
        public void tearDown() {
            if (session != null) session.close();
        }

        private void insertData(SharedState shared) throws SQLException {
            try (Connection conn = shared.dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                for (int i = 1; i <= BATCH_SIZE; i++) {
                    stmt.execute("INSERT INTO users (name, age, email) VALUES ('user_" + i + "', " + (20 + i % 50) + ", 'user_" + i + "@test.com')");
                }
            }
        }

        private List<Object[]> generateUpdateParams() {
            List<Object[]> list = new ArrayList<>(BATCH_SIZE);
            for (int i = 1; i <= BATCH_SIZE; i++) {
                list.add(new Object[]{30 + (i % 50), (long) i});
            }
            return list;
        }
    }

    @State(Scope.Thread)
    public static class UpdateDirectSqlState {
        SessionImpl session;
        List<Object[]> rows;

        @Setup(Level.Invocation)
        public void setup(SharedState shared) throws SQLException {
            shared.clearTable();
            insertData(shared);
            JrdbConfig config = JrdbConfig.builder()
                .cacheLevel(CacheLevel.NONE)
                .build();
            session = new SessionImpl(shared.memoryPool, config, shared.dataSource, shared.mapperRegistry);
            rows = generateUpdateRows();
        }

        @TearDown(Level.Invocation)
        public void tearDown() {
            if (session != null) session.close();
        }

        private void insertData(SharedState shared) throws SQLException {
            try (Connection conn = shared.dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                for (int i = 1; i <= BATCH_SIZE; i++) {
                    stmt.execute("INSERT INTO users (name, age, email) VALUES ('user_" + i + "', " + (20 + i % 50) + ", 'user_" + i + "@test.com')");
                }
            }
        }

        private List<Object[]> generateUpdateRows() {
            List<Object[]> list = new ArrayList<>(BATCH_SIZE);
            for (int i = 1; i <= BATCH_SIZE; i++) {
                list.add(new Object[]{(long) i, 30 + (i % 50)});
            }
            return list;
        }
    }

    @Benchmark
    public int[] batchInsert_PreparedStatement(PreparedStatementState state) {
        return state.session.batchUpdate(
            "INSERT INTO users (name, age, email) VALUES (?, ?, ?)",
            state.paramsList
        );
    }

    @Benchmark
    public int batchInsert_DirectSql(DirectSqlState state) {
        return state.session.batchInsertDirect("users", state.columns, state.rows);
    }

    @Benchmark
    public int[] batchUpdate_PreparedStatement(UpdatePreparedStatementState state) {
        return state.session.batchUpdate(
            "UPDATE users SET age = ? WHERE id = ?",
            state.paramsList
        );
    }

    @Benchmark
    public int batchUpdate_DirectSql(UpdateDirectSqlState state) {
        return state.session.batchUpdateDirect("users", "age", "id", state.rows);
    }
}