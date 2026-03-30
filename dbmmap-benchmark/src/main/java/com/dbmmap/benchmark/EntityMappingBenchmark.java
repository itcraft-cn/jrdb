package com.dbmmap.benchmark;

import com.dbmmap.benchmark.entity.User;
import com.dbmmap.benchmark.entity.UserEntity;
import com.dbmmap.core.config.CacheLevel;
import com.dbmmap.core.config.DbMmapConfig;
import com.dbmmap.core.mapping.EntityMapper;
import com.dbmmap.core.mapping.MapperRegistry;
import com.dbmmap.core.memory.MemoryPool;
import com.dbmmap.core.memory.MemoryPoolConfig;
import com.dbmmap.core.query.QueryResult;
import com.dbmmap.core.session.SessionImpl;
import com.dbmmap.core.type.TypeHandlerRegistry;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
@Fork(1)
public class EntityMappingBenchmark {

    private static final int DATA_SIZE = 1000;

    @State(Scope.Benchmark)
    public static class SharedState {
        DataSource dataSource;
        MemoryPool memoryPool;
        MapperRegistry mapperRegistry;

        @Setup(Level.Trial)
        public void setup() throws SQLException {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setUrl("jdbc:h2:mem:entitymap;DB_CLOSE_DELAY=-1");
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

            TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();
            mapperRegistry = new MapperRegistry(typeHandlerRegistry);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (memoryPool != null) memoryPool.destroy();
        }
    }

    @State(Scope.Thread)
    public static class DbMmapRawState {
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
    public static class DbMmapAsmState {
        EntityMapper<User> mapper;

        @Setup(Level.Trial)
        public void setup(SharedState shared) {
            mapper = shared.mapperRegistry.getMapper(User.class);
        }
    }

    @State(Scope.Thread)
    public static class MyBatisState {
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
    public static class HibernateState {
        Session session;

        @Setup(Level.Trial)
        public void setup(SharedState shared) {
            org.hibernate.cfg.Configuration config = new org.hibernate.cfg.Configuration();
            config.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
            config.setProperty("hibernate.connection.url", "jdbc:h2:mem:entitymap;DB_CLOSE_DELAY=-1");
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

    @State(Scope.Thread)
    public static class JdbcState {
        DataSource dataSource;

        @Setup(Level.Trial)
        public void setup(SharedState shared) {
            this.dataSource = shared.dataSource;
        }
    }

    public interface UserMapper {
        @Select("SELECT id, name, age, email, status FROM users WHERE id = #{id}")
        User getById(Long id);

        @Select("SELECT id, name, age, email, status FROM users LIMIT 100")
        List<User> getAll();
    }

    @Benchmark
    public void dbmmap_raw_singleQuery(DbMmapRawState state, Blackhole bh) {
        try (QueryResult result = state.session.query("SELECT id, name, age, email, status FROM users WHERE id = ?", 1L)) {
            if (result != null && result.next()) {
                User user = new User();
                user.setId(result.getLong("ID"));
                user.setName(result.getString("NAME"));
                user.setAge(result.getInt("AGE"));
                user.setEmail(result.getString("EMAIL"));
                user.setStatus(result.getString("STATUS"));
                bh.consume(user);
            }
        }
    }

    @Benchmark
    public User dbmmap_asm_singleQuery(SharedState shared, DbMmapAsmState state) throws SQLException {
        try (Connection conn = shared.dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name, age, email, status FROM users WHERE id = ?")) {
            ps.setLong(1, 1L);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return state.mapper.mapFromResultSet(rs);
                }
                return null;
            }
        }
    }

    @Benchmark
    public User myBatis_singleQuery(MyBatisState state) {
        return state.session.selectOne("com.dbmmap.benchmark.EntityMappingBenchmark$UserMapper.getById", 1L);
    }

    @Benchmark
    public UserEntity hibernate_singleQuery(HibernateState state) {
        state.session.clear();
        return state.session.get(UserEntity.class, 1L);
    }

    @Benchmark
    public User jdbc_manual_singleQuery(JdbcState state) throws SQLException {
        try (Connection conn = state.dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name, age, email, status FROM users WHERE id = ?")) {
            ps.setLong(1, 1L);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setName(rs.getString("name"));
                    user.setAge(rs.getInt("age"));
                    user.setEmail(rs.getString("email"));
                    user.setStatus(rs.getString("status"));
                    return user;
                }
                return null;
            }
        }
    }

    @Benchmark
    public void dbmmap_raw_queryList(DbMmapRawState state, Blackhole bh) {
        List<User> users = new ArrayList<>(100);
        try (QueryResult result = state.session.query("SELECT id, name, age, email, status FROM users LIMIT 100")) {
            while (result != null && result.next()) {
                User user = new User();
                user.setId(result.getLong("ID"));
                user.setName(result.getString("NAME"));
                user.setAge(result.getInt("AGE"));
                user.setEmail(result.getString("EMAIL"));
                user.setStatus(result.getString("STATUS"));
                users.add(user);
            }
        }
        bh.consume(users);
    }

    @Benchmark
    public List<User> dbmmap_asm_queryList(SharedState shared, DbMmapAsmState state) throws SQLException {
        List<User> users = new ArrayList<>(100);
        try (Connection conn = shared.dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name, age, email, status FROM users LIMIT 100");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(state.mapper.mapFromResultSet(rs));
            }
        }
        return users;
    }

    @Benchmark
    @SuppressWarnings("unchecked")
    public List<User> myBatis_queryList(MyBatisState state) {
        return state.session.selectList("com.dbmmap.benchmark.EntityMappingBenchmark$UserMapper.getAll");
    }

    @Benchmark
    @SuppressWarnings("unchecked")
    public List<UserEntity> hibernate_queryList(HibernateState state) {
        state.session.clear();
        return state.session.createQuery("FROM UserEntity", UserEntity.class)
            .setMaxResults(100)
            .list();
    }

    @Benchmark
    public List<User> jdbc_manual_queryList(JdbcState state) throws SQLException {
        List<User> users = new ArrayList<>(100);
        try (Connection conn = state.dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name, age, email, status FROM users LIMIT 100");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setName(rs.getString("name"));
                user.setAge(rs.getInt("age"));
                user.setEmail(rs.getString("email"));
                user.setStatus(rs.getString("status"));
                users.add(user);
            }
        }
        return users;
    }
}