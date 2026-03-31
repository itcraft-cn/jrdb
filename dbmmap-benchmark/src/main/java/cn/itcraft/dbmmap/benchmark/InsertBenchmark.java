package cn.itcraft.dbmmap.benchmark;

import cn.itcraft.dbmmap.benchmark.entity.User;
import cn.itcraft.dbmmap.benchmark.entity.UserEntity;
import cn.itcraft.dbmmap.core.config.CacheLevel;
import cn.itcraft.dbmmap.core.config.DbMmapConfig;
import cn.itcraft.dbmmap.core.mapping.EntityMapper;
import cn.itcraft.dbmmap.core.mapping.MapperRegistry;
import cn.itcraft.dbmmap.core.memory.MemoryPool;
import cn.itcraft.dbmmap.core.memory.MemoryPoolConfig;
import cn.itcraft.dbmmap.core.session.SessionImpl;
import cn.itcraft.dbmmap.core.type.TypeHandlerRegistry;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 20, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class InsertBenchmark {

    @State(Scope.Benchmark)
    public static class SharedState {
        DataSource dataSource;
        MemoryPool memoryPool;
        MapperRegistry mapperRegistry;
        AtomicLong idGenerator = new AtomicLong(0);

        @Setup(Level.Trial)
        public void setup() throws SQLException {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setUrl("jdbc:h2:mem:insertbench;DB_CLOSE_DELAY=-1");
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
                        "status VARCHAR(20))");
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

        public long nextId() {
            return idGenerator.incrementAndGet();
        }
    }

    @State(Scope.Thread)
    public static class DbMmapRawState {
        SessionImpl session;
        DataSource dataSource;

        @Setup(Level.Trial)
        public void setup(SharedState shared) {
            DbMmapConfig config = DbMmapConfig.builder()
                    .cacheLevel(CacheLevel.NONE)
                    .build();
            session = new SessionImpl(shared.memoryPool, config, shared.dataSource);
            dataSource = shared.dataSource;
        }

        @Setup(Level.Iteration)
        public void setupIteration() throws SQLException {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM users");
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (session != null) session.close();
        }
    }

    @State(Scope.Thread)
    public static class DbMmapAsmState {
        EntityMapper<User> mapper;
        Connection connection;
        PreparedStatement insertPs;

        @Setup(Level.Trial)
        public void setup(SharedState shared) throws SQLException {
            mapper = shared.mapperRegistry.getMapper(User.class);
            connection = shared.dataSource.getConnection();
            insertPs = connection.prepareStatement(
                    "INSERT INTO users (name, age, email, status) VALUES (?, ?, ?, ?)");
        }

        @Setup(Level.Iteration)
        public void setupIteration() throws SQLException {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DELETE FROM users");
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() throws SQLException {
            if (insertPs != null) insertPs.close();
            if (connection != null) connection.close();
        }
    }

    @State(Scope.Thread)
    public static class MyBatisState {
        SqlSession session;
        DataSource dataSource;

        @Setup(Level.Trial)
        public void setup(SharedState shared) {
            org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
            config.setEnvironment(new Environment("benchmark", new JdbcTransactionFactory(), shared.dataSource));
            config.setCacheEnabled(false);
            config.setLocalCacheScope(org.apache.ibatis.session.LocalCacheScope.STATEMENT);
            config.addMapper(UserMapper.class);
            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);
            session = factory.openSession();
            dataSource = shared.dataSource;
        }

        @Setup(Level.Iteration)
        public void setupIteration() throws SQLException {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM users");
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (session != null) session.close();
        }
    }

    @State(Scope.Thread)
    public static class HibernateState {
        Session session;
        DataSource dataSource;

        @Setup(Level.Trial)
        public void setup(SharedState shared) {
            org.hibernate.cfg.Configuration config = new org.hibernate.cfg.Configuration();
            config.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
            config.setProperty("hibernate.connection.url", "jdbc:h2:mem:insertbench;DB_CLOSE_DELAY=-1");
            config.setProperty("hibernate.connection.username", "sa");
            config.setProperty("hibernate.connection.password", "");
            config.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            config.setProperty("hibernate.cache.use_second_level_cache", "false");
            config.setProperty("hibernate.cache.use_query_cache", "false");
            config.setProperty("hibernate.show_sql", "false");
            config.addAnnotatedClass(UserEntity.class);
            SessionFactory factory = config.buildSessionFactory();
            session = factory.openSession();
            dataSource = shared.dataSource;
        }

        @Setup(Level.Iteration)
        public void setupIteration() throws SQLException {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM users");
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (session != null) session.close();
        }
    }

    @State(Scope.Thread)
    public static class JdbcState {
        Connection connection;
        PreparedStatement insertPs;

        @Setup(Level.Trial)
        public void setup(SharedState shared) throws SQLException {
            connection = shared.dataSource.getConnection();
            insertPs = connection.prepareStatement(
                    "INSERT INTO users (name, age, email, status) VALUES (?, ?, ?, ?)");
        }

        @Setup(Level.Iteration)
        public void setupIteration() throws SQLException {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DELETE FROM users");
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() throws SQLException {
            if (insertPs != null) insertPs.close();
            if (connection != null) connection.close();
        }
    }

    public interface UserMapper {
        @Insert("INSERT INTO users (name, age, email, status) VALUES (#{name}, #{age}, #{email}, #{status})")
        int insert(User user);
    }

    @Benchmark
    public int dbmmap_raw_insert(DbMmapRawState state, SharedState shared) {
        long id = shared.nextId();
        return state.session.update(
                "INSERT INTO users (name, age, email, status) VALUES (?, ?, ?, ?)",
                "user_" + id, 25, "user_" + id + "@test.com", "active");
    }

    @Benchmark
    public int dbmmap_asm_insert(DbMmapAsmState state, SharedState shared) throws SQLException {
        long id = shared.nextId();
        User user = new User(null, "user_" + id, 25, "user_" + id + "@test.com", "active");
        
        state.insertPs.setString(1, user.getName());
        state.insertPs.setInt(2, user.getAge());
        state.insertPs.setString(3, user.getEmail());
        state.insertPs.setString(4, user.getStatus());
        
        return state.insertPs.executeUpdate();
    }

    @Benchmark
    public int myBatis_insert(MyBatisState state, SharedState shared) {
        long id = shared.nextId();
        User user = new User(null, "user_" + id, 25, "user_" + id + "@test.com", "active");
        return state.session.insert("cn.itcraft.dbmmap.benchmark.InsertBenchmark$UserMapper.insert", user);
    }

    @Benchmark
    public int hibernate_insert(HibernateState state, SharedState shared) {
        long id = shared.nextId();
        UserEntity user = new UserEntity();
        user.setName("user_" + id);
        user.setAge(25);
        user.setEmail("user_" + id + "@test.com");
        user.setStatus("active");
        
        state.session.beginTransaction();
        state.session.save(user);
        state.session.getTransaction().commit();
        state.session.clear();
        
        return 1;
    }

    @Benchmark
    public int jdbc_manual_insert(JdbcState state, SharedState shared) throws SQLException {
        long id = shared.nextId();
        state.insertPs.setString(1, "user_" + id);
        state.insertPs.setInt(2, 25);
        state.insertPs.setString(3, "user_" + id + "@test.com");
        state.insertPs.setString(4, "active");
        return state.insertPs.executeUpdate();
    }
}