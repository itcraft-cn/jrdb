package cn.itcraft.jrdb.benchmark;

import cn.itcraft.jrdb.jdbc.mapping.EntityMapper;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.type.TypeHandlerRegistry;
import cn.itcraft.jrdb.r2dbc.mapping.R2dbcEntityMapper;
import cn.itcraft.jrdb.r2dbc.mapping.R2dbcMapperRegistry;
import cn.itcraft.jrdb.r2dbc.session.R2dbcSession;
import cn.itcraft.jrdb.r2dbc.session.R2dbcSessionImpl;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 20, timeUnit = TimeUnit.MILLISECONDS)
@Threads(4)
@Fork(3)
@State(Scope.Benchmark)
public class MappingPerformanceBenchmark {

    private static final int DATA_SIZE = 1000;
    private static final int OPS_PER_INVOCATION = 100;
    private static final int POOL_SIZE = 16;

    private static JdbcDataSource jdbcDataSource;
    private static MapperRegistry jdbcMapperRegistry;

    private static ConnectionPool r2dbcConnectionPool;
    private static R2dbcMapperRegistry r2dbcMapperRegistry;
    private static R2dbcSession sharedR2dbcSession;

    @Setup(Level.Trial)
    public static void setup() throws Exception {
        setupJdbc();
        setupR2dbc();
    }

    private static void setupJdbc() throws Exception {
        jdbcDataSource = new JdbcDataSource();
        jdbcDataSource.setUrl("jdbc:h2:mem:benchmark_jdbc;DB_CLOSE_DELAY=-1");
        jdbcDataSource.setUser("sa");
        jdbcDataSource.setPassword("");

        try (Connection conn = jdbcDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS users");
            stmt.execute("CREATE TABLE users (id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100), age INT, email VARCHAR(200))");
            for (int i = 0; i < DATA_SIZE; i++) {
                stmt.execute("INSERT INTO users (name, age, email) VALUES ('user_" + i + "', " + (20 + i % 50) + ", 'user_" + i + "@test.com')");
            }
        }

        jdbcMapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
    }

    private static void setupR2dbc() throws Exception {
        ConnectionFactory h2Factory = H2ConnectionFactory.inMemory("benchmark_r2dbc");

        ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(h2Factory)
            .initialSize(POOL_SIZE / 2)
            .maxSize(POOL_SIZE)
            .maxLifeTime(Duration.ofMinutes(30))
            .maxIdleTime(Duration.ofMinutes(10))
            .build();

        r2dbcConnectionPool = new ConnectionPool(poolConfig);
        r2dbcMapperRegistry = new R2dbcMapperRegistry();

        initR2dbcData();

        sharedR2dbcSession = new R2dbcSessionImpl(r2dbcConnectionPool);
    }

    private static void initR2dbcData() throws Exception {
        R2dbcSession session = new R2dbcSessionImpl(r2dbcConnectionPool);
        
        session.update("DROP TABLE IF EXISTS users").get();
        session.update("CREATE TABLE users (id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100), age INT, email VARCHAR(200))").get();
        
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < DATA_SIZE; i++) {
            futures.add(session.update("INSERT INTO users (name, age, email) VALUES ('user_" + i + "', " + (20 + i % 50) + ", 'user_" + i + "@test.com')"));
        }
        for (CompletableFuture<Integer> future : futures) {
            future.get();
        }
        
        session.close();
    }

    @TearDown(Level.Trial)
    public static void tearDown() {
        if (sharedR2dbcSession != null) {
            sharedR2dbcSession.close();
        }
        if (r2dbcConnectionPool != null) {
            r2dbcConnectionPool.dispose();
        }
    }

    @Benchmark
    public void jdbcRaw(Blackhole bh) throws Exception {
        try (Connection conn = jdbcDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name, age, email FROM users")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setName(rs.getString("name"));
                user.setAge(rs.getInt("age"));
                user.setEmail(rs.getString("email"));
                bh.consume(user);
            }
        }
    }

    @Benchmark
    public void jdbcAsm(Blackhole bh) throws Exception {
        EntityMapper<User> mapper = jdbcMapperRegistry.getMapper(User.class);
        try (Connection conn = jdbcDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name, age, email FROM users")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User user = mapper.mapFromResultSet(rs);
                bh.consume(user);
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(OPS_PER_INVOCATION)
    public void r2dbcRaw(Blackhole bh) throws Exception {
        List<CompletableFuture<List<User>>> futures = new ArrayList<>(OPS_PER_INVOCATION);
        
        for (int i = 0; i < OPS_PER_INVOCATION; i++) {
            futures.add(sharedR2dbcSession.queryList("SELECT id, name, age, email FROM users", row -> {
                User user = new User();
                user.setId(row.get("id", Long.class));
                user.setName(row.get("name", String.class));
                user.setAge(row.get("age", Integer.class));
                user.setEmail(row.get("email", String.class));
                return user;
            }));
        }
        
        for (CompletableFuture<List<User>> future : futures) {
            for (User user : future.get()) {
                bh.consume(user);
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(OPS_PER_INVOCATION)
    public void r2dbcAsm(Blackhole bh) throws Exception {
        R2dbcEntityMapper<User> mapper = r2dbcMapperRegistry.getMapper(User.class);
        List<CompletableFuture<List<User>>> futures = new ArrayList<>(OPS_PER_INVOCATION);
        
        for (int i = 0; i < OPS_PER_INVOCATION; i++) {
            futures.add(sharedR2dbcSession.queryList("SELECT id, name, age, email FROM users", mapper::mapFromRow));
        }
        
        for (CompletableFuture<List<User>> future : futures) {
            for (User user : future.get()) {
                bh.consume(user);
            }
        }
    }

    public static class User {
        private Long id;
        private String name;
        private Integer age;
        private String email;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}