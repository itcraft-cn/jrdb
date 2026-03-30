package com.dbmmap.benchmark;

import com.dbmmap.benchmark.entity.User;
import com.dbmmap.benchmark.entity.UserEntity;
import com.dbmmap.core.config.CacheLevel;
import com.dbmmap.core.config.DbMmapConfig;
import com.dbmmap.core.memory.MemoryPool;
import com.dbmmap.core.memory.MemoryPoolConfig;
import com.dbmmap.core.query.QueryResult;
import com.dbmmap.core.session.SessionImpl;
import org.apache.ibatis.annotations.Select;
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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 20, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class FrameworkComparisonBenchmark {

    private static final int DATA_SIZE = 1000;

    @State(Scope.Benchmark)
    public static class SharedState {
        DataSource dataSource;
        MemoryPool memoryPool;

        @Setup(Level.Trial)
        public void setup() throws SQLException {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setUrl("jdbc:h2:mem:framework;DB_CLOSE_DELAY=-1");
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
                stmt.execute("INSERT INTO users (name, age, email, status) " +
                    "SELECT 'user_' || x, 20 + (x % 50), 'user_' || x || '@test.com', 'active' " +
                    "FROM SYSTEM_RANGE(1, " + DATA_SIZE + ")");
            }

            MemoryPoolConfig poolConfig = MemoryPoolConfig.builder()
                .preAllocMemSize(64 * 1024 * 1024)
                .queryCacheSize(4 * 1024 * 1024)
                .build();
            memoryPool = new MemoryPool(poolConfig);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (memoryPool != null) memoryPool.destroy();
        }
    }

    @State(Scope.Thread)
    public static class DbMmapNoneState {
        SessionImpl session;

        @Setup(Level.Trial)
        public void setup(SharedState shared) {
            DbMmapConfig config = DbMmapConfig.builder()
                .cacheLevel(CacheLevel.NONE)
                .build();
            session = new SessionImpl(shared.memoryPool, config, shared.dataSource);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (session != null) session.close();
        }
    }

    @State(Scope.Thread)
    public static class DbMmapL1State {
        SessionImpl session;

        @Setup(Level.Trial)
        public void setup(SharedState shared) {
            DbMmapConfig config = DbMmapConfig.builder()
                .cacheLevel(CacheLevel.L1)
                .statementCacheSize(256)
                .build();
            session = new SessionImpl(shared.memoryPool, config, shared.dataSource);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (session != null) session.close();
        }
    }

    @State(Scope.Thread)
    public static class DbMmapL2State {
        SessionImpl session;

        @Setup(Level.Trial)
        public void setup(SharedState shared) {
            DbMmapConfig config = DbMmapConfig.builder()
                .cacheLevel(CacheLevel.L2)
                .statementCacheSize(256)
                .resultCacheSize(1000)
                .resultCacheExpireSeconds(300)
                .build();
            session = new SessionImpl(shared.memoryPool, config, shared.dataSource);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (session != null) session.close();
        }
    }

    @State(Scope.Thread)
    public static class MyBatisCachedState {
        SqlSession session;

        @Setup(Level.Trial)
        public void setup(SharedState shared) {
            org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
            config.setEnvironment(new Environment("benchmark", new JdbcTransactionFactory(), shared.dataSource));
            config.setCacheEnabled(true);
            config.setLocalCacheScope(org.apache.ibatis.session.LocalCacheScope.SESSION);
            config.addMapper(UserMapper.class);
            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);
            session = factory.openSession();
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (session != null) session.close();
        }
    }

    @State(Scope.Thread)
    public static class MyBatisNoCacheState {
        SqlSession session;

        @Setup(Level.Trial)
        public void setup(SharedState shared) {
            org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
            config.setEnvironment(new Environment("benchmark", new JdbcTransactionFactory(), shared.dataSource));
            config.setCacheEnabled(false);
            config.setLocalCacheScope(org.apache.ibatis.session.LocalCacheScope.STATEMENT);
            config.addMapper(UserMapper.class);
            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);
            session = factory.openSession();
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (session != null) session.close();
        }
    }

    @State(Scope.Thread)
    public static class HibernateCachedState {
        Session session;

        @Setup(Level.Trial)
        public void setup(SharedState shared) {
            org.hibernate.cfg.Configuration config = new org.hibernate.cfg.Configuration();
            config.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
            config.setProperty("hibernate.connection.url", "jdbc:h2:mem:framework;DB_CLOSE_DELAY=-1");
            config.setProperty("hibernate.connection.username", "sa");
            config.setProperty("hibernate.connection.password", "");
            config.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            config.setProperty("hibernate.cache.use_second_level_cache", "true");
            config.setProperty("hibernate.cache.use_query_cache", "true");
            config.setProperty("hibernate.show_sql", "false");
            config.addAnnotatedClass(UserEntity.class);
            SessionFactory factory = config.buildSessionFactory();
            session = factory.openSession();
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (session != null) session.close();
        }
    }

    @State(Scope.Thread)
    public static class HibernateNoCacheState {
        Session session;

        @Setup(Level.Trial)
        public void setup(SharedState shared) {
            org.hibernate.cfg.Configuration config = new org.hibernate.cfg.Configuration();
            config.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
            config.setProperty("hibernate.connection.url", "jdbc:h2:mem:framework;DB_CLOSE_DELAY=-1");
            config.setProperty("hibernate.connection.username", "sa");
            config.setProperty("hibernate.connection.password", "");
            config.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            config.setProperty("hibernate.cache.use_second_level_cache", "false");
            config.setProperty("hibernate.cache.use_query_cache", "false");
            config.setProperty("hibernate.show_sql", "false");
            config.addAnnotatedClass(UserEntity.class);
            SessionFactory factory = config.buildSessionFactory();
            session = factory.openSession();
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (session != null) session.close();
        }
    }

    public interface UserMapper {
        @Select("SELECT id, name, age, email, status FROM users WHERE id = #{id}")
        User getById(Long id);

        @Select("SELECT id, name, age, email, status FROM users LIMIT 100")
        List<User> getAll();
    }

    @Benchmark
    public void dbmmap_none_singleQuery(DbMmapNoneState state, Blackhole bh) {
        try (QueryResult result = state.session.query("SELECT id, name, age, email, status FROM users WHERE id = ?", 1L)) {
            if (result != null && result.next()) {
                bh.consume(result.getLong("ID"));
                bh.consume(result.getString("NAME"));
                bh.consume(result.getInt("AGE"));
            }
        }
    }

    @Benchmark
    public void dbmmap_L1_singleQuery(DbMmapL1State state, Blackhole bh) {
        try (QueryResult result = state.session.query("SELECT id, name, age, email, status FROM users WHERE id = ?", 1L)) {
            if (result != null && result.next()) {
                bh.consume(result.getLong("ID"));
                bh.consume(result.getString("NAME"));
                bh.consume(result.getInt("AGE"));
            }
        }
    }

    @Benchmark
    public void dbmmap_L2_singleQuery(DbMmapL2State state, Blackhole bh) {
        try (QueryResult result = state.session.query("SELECT id, name, age, email, status FROM users WHERE id = ?", 1L)) {
            if (result != null && result.next()) {
                bh.consume(result.getLong("ID"));
                bh.consume(result.getString("NAME"));
                bh.consume(result.getInt("AGE"));
            }
        }
    }

    @Benchmark
    public User myBatis_cached_singleQuery(MyBatisCachedState state) {
        return state.session.selectOne("com.dbmmap.benchmark.FrameworkComparisonBenchmark$UserMapper.getById", 1L);
    }

    @Benchmark
    public User myBatis_noCache_singleQuery(MyBatisNoCacheState state) {
        return state.session.selectOne("com.dbmmap.benchmark.FrameworkComparisonBenchmark$UserMapper.getById", 1L);
    }

    @Benchmark
    public UserEntity hibernate_cached_singleQuery(HibernateCachedState state) {
        return state.session.get(UserEntity.class, 1L);
    }

    @Benchmark
    public UserEntity hibernate_noCache_singleQuery(HibernateNoCacheState state) {
        state.session.clear();
        return state.session.get(UserEntity.class, 1L);
    }

    @Benchmark
    public void dbmmap_none_queryList(DbMmapNoneState state, Blackhole bh) {
        try (QueryResult result = state.session.query("SELECT id, name, age, email, status FROM users LIMIT 100")) {
            if (result != null) {
                while (result.next()) {
                    bh.consume(result.getLong("ID"));
                }
            }
        }
    }

    @Benchmark
    public void dbmmap_L1_queryList(DbMmapL1State state, Blackhole bh) {
        try (QueryResult result = state.session.query("SELECT id, name, age, email, status FROM users LIMIT 100")) {
            if (result != null) {
                while (result.next()) {
                    bh.consume(result.getLong("ID"));
                }
            }
        }
    }

    @Benchmark
    public void dbmmap_L2_queryList(DbMmapL2State state, Blackhole bh) {
        try (QueryResult result = state.session.query("SELECT id, name, age, email, status FROM users LIMIT 100")) {
            if (result != null) {
                while (result.next()) {
                    bh.consume(result.getLong("ID"));
                }
            }
        }
    }

    @Benchmark
    @SuppressWarnings("unchecked")
    public List<User> myBatis_cached_queryList(MyBatisCachedState state) {
        return state.session.selectList("com.dbmmap.benchmark.FrameworkComparisonBenchmark$UserMapper.getAll");
    }

    @Benchmark
    @SuppressWarnings("unchecked")
    public List<User> myBatis_noCache_queryList(MyBatisNoCacheState state) {
        return state.session.selectList("com.dbmmap.benchmark.FrameworkComparisonBenchmark$UserMapper.getAll");
    }

    @Benchmark
    @SuppressWarnings("unchecked")
    public List<UserEntity> hibernate_cached_queryList(HibernateCachedState state) {
        return state.session.createQuery("FROM UserEntity", UserEntity.class)
            .setMaxResults(100)
            .list();
    }

    @Benchmark
    @SuppressWarnings("unchecked")
    public List<UserEntity> hibernate_noCache_queryList(HibernateNoCacheState state) {
        state.session.clear();
        return state.session.createQuery("FROM UserEntity", UserEntity.class)
            .setMaxResults(100)
            .list();
    }
}