package cn.itcraft.jrdb.benchmark;

import cn.itcraft.jrdb.jdbc.mapping.EntityMapper;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.type.TypeHandlerRegistry;
import cn.itcraft.jrdb.r2dbc.config.R2dbcConfig;
import cn.itcraft.jrdb.r2dbc.config.R2dbcExecutorConfig;
import cn.itcraft.jrdb.r2dbc.future.PooledFuture;
import cn.itcraft.jrdb.r2dbc.mapping.R2dbcEntityMapper;
import cn.itcraft.jrdb.r2dbc.mapping.R2dbcMapperRegistry;
import cn.itcraft.jrdb.r2dbc.session.R2dbcSession;
import cn.itcraft.jrdb.r2dbc.session.R2dbcSessionImpl;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
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

    private static JdbcDataSource jdbcDataSource;
    private static MapperRegistry jdbcMapperRegistry;

    private static ConnectionFactory r2dbcConnectionFactory;
    private static R2dbcMapperRegistry r2dbcMapperRegistry;
    private static R2dbcConfig r2dbcConfig;

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
        r2dbcConnectionFactory = H2ConnectionFactory.inMemory("benchmark_r2dbc");

        r2dbcConfig = R2dbcConfig.builder()
            .executorConfig(new R2dbcExecutorConfig()
                .corePoolSize(4)
                .maxPoolSize(8)
                .queueCapacity(1000)
                .rejectionPolicy(R2dbcExecutorConfig.RejectionPolicy.CALLER_RUNS))
            .build();

        r2dbcMapperRegistry = new R2dbcMapperRegistry();

        initR2dbcData();
    }

    private static void initR2dbcData() throws Exception {
        R2dbcSession session = new R2dbcSessionImpl(r2dbcConnectionFactory, r2dbcConfig, r2dbcMapperRegistry);
        
        session.update("DROP TABLE IF EXISTS users").get();
        session.update("CREATE TABLE users (id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100), age INT, email VARCHAR(200))").get();
        
        List<PooledFuture> futures = new ArrayList<>();
        for (int i = 0; i < DATA_SIZE; i++) {
            futures.add(session.update("INSERT INTO users (name, age, email) VALUES ('user_" + i + "', " + (20 + i % 50) + ", 'user_" + i + "@test.com')"));
        }
        for (PooledFuture future : futures) {
            future.get();
        }
        
        session.close();
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
        R2dbcSession session = new R2dbcSessionImpl(r2dbcConnectionFactory, r2dbcConfig, r2dbcMapperRegistry);
        
        List<PooledFuture> futures = new ArrayList<>();
        for (int i = 0; i < OPS_PER_INVOCATION; i++) {
            futures.add(session.queryList("SELECT id, name, age, email FROM users", row -> {
                User user = new User();
                user.setId(row.get("id", Long.class));
                user.setName(row.get("name", String.class));
                user.setAge(row.get("age", Integer.class));
                user.setEmail(row.get("email", String.class));
                return user;
            }));
        }
        
        for (PooledFuture future : futures) {
            @SuppressWarnings("unchecked")
            List<User> users = future.get();
            for (User user : users) {
                bh.consume(user);
            }
        }
        
        session.close();
    }

    @Benchmark
    @OperationsPerInvocation(OPS_PER_INVOCATION)
    public void r2dbcAsm(Blackhole bh) throws Exception {
        R2dbcSession session = new R2dbcSessionImpl(r2dbcConnectionFactory, r2dbcConfig, r2dbcMapperRegistry);
        R2dbcEntityMapper<User> mapper = r2dbcMapperRegistry.getMapper(User.class);
        
        List<PooledFuture> futures = new ArrayList<>();
        for (int i = 0; i < OPS_PER_INVOCATION; i++) {
            futures.add(session.queryList("SELECT id, name, age, email FROM users", mapper::mapFromRow));
        }
        
        for (PooledFuture future : futures) {
            @SuppressWarnings("unchecked")
            List<User> users = future.get();
            for (User user : users) {
                bh.consume(user);
            }
        }
        
        session.close();
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