package cn.itcraft.dbmmap.benchmark;

import cn.itcraft.dbmmap.benchmark.entity.User;
import cn.itcraft.dbmmap.benchmark.entity.UserEntity;
import cn.itcraft.dbmmap.core.config.CacheLevel;
import cn.itcraft.dbmmap.core.config.DbMmapConfig;
import cn.itcraft.dbmmap.core.mapping.EntityMapper;
import cn.itcraft.dbmmap.core.mapping.MapperRegistry;
import cn.itcraft.dbmmap.core.memory.MemoryPool;
import cn.itcraft.dbmmap.core.memory.MemoryPoolConfig;
import cn.itcraft.dbmmap.core.query.QueryResult;
import cn.itcraft.dbmmap.core.session.SessionImpl;
import cn.itcraft.dbmmap.core.type.TypeHandlerRegistry;
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
public class NoCacheComparisonBenchmark {

    private static final int DATA_SIZE = 1000;

    @State(Scope.Benchmark)
    public static class SharedState {
        DataSource dataSource;
        MemoryPool memoryPool;
        MapperRegistry mapperRegistry;

        @Setup(Level.Trial)
        public void setup() throws SQLException {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setUrl("jdbc:h2:mem:nocache;DB_CLOSE_DELAY=-1");
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
    public static class DbMmapState {
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
        Connection connection;
        PreparedStatement singleQueryPs;
        PreparedStatement queryListPs;

        @Setup(Level.Trial)
        public void setup(SharedState shared) throws SQLException {
            mapper = shared.mapperRegistry.getMapper(User.class);
            connection = shared.dataSource.getConnection();
            singleQueryPs = connection.prepareStatement("SELECT id, name, age, email, status FROM users WHERE id = ?");
            queryListPs = connection.prepareStatement("SELECT id, name, age, email, status FROM users LIMIT 100");
        }

        @TearDown(Level.Trial)
        public void tearDown() throws SQLException {
            if (singleQueryPs != null) singleQueryPs.close();
            if (queryListPs != null) queryListPs.close();
            if (connection != null) connection.close();
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
            config.setProperty("hibernate.connection.url", "jdbc:h2:mem:nocache;DB_CLOSE_DELAY=-1");
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
    public void dbmmap_singleQuery(DbMmapState state, Blackhole bh) {
        try (QueryResult result = state.session.query("SELECT id, name, age, email, status FROM users WHERE id = ?", 1L)) {
            if (result != null && result.next()) {
                bh.consume(result.getLong("ID"));
                bh.consume(result.getString("NAME"));
                bh.consume(result.getInt("AGE"));
            }
        }
    }

    @Benchmark
    public User dbmmap_asm_singleQuery(DbMmapAsmState state) throws SQLException {
        state.singleQueryPs.setLong(1, 1L);
        try (ResultSet rs = state.singleQueryPs.executeQuery()) {
            if (rs.next()) {
                return state.mapper.mapFromResultSet(rs);
            }
            return null;
        }
    }

    @Benchmark
    public User myBatis_singleQuery(MyBatisState state) {
        return state.session.selectOne("cn.itcraft.dbmmap.benchmark.NoCacheComparisonBenchmark$UserMapper.getById", 1L);
    }

    @Benchmark
    public UserEntity hibernate_singleQuery(HibernateState state) {
        state.session.clear();
        return state.session.get(UserEntity.class, 1L);
    }

    @Benchmark
    public void dbmmap_queryList(DbMmapState state, Blackhole bh) {
        try (QueryResult result = state.session.query("SELECT id, name, age, email, status FROM users LIMIT 100")) {
            if (result != null) {
                while (result.next()) {
                    bh.consume(result.getLong("ID"));
                }
            }
        }
    }

    @Benchmark
    public List<User> dbmmap_asm_queryList(DbMmapAsmState state) throws SQLException {
        List<User> users = new ArrayList<>(100);
        try (ResultSet rs = state.queryListPs.executeQuery()) {
            while (rs.next()) {
                users.add(state.mapper.mapFromResultSet(rs));
            }
        }
        return users;
    }

    @Benchmark
    @SuppressWarnings("unchecked")
    public List<User> myBatis_queryList(MyBatisState state) {
        return state.session.selectList("cn.itcraft.dbmmap.benchmark.NoCacheComparisonBenchmark$UserMapper.getAll");
    }

    @Benchmark
    @SuppressWarnings("unchecked")
    public List<UserEntity> hibernate_queryList(HibernateState state) {
        state.session.clear();
        return state.session.createQuery("FROM UserEntity", UserEntity.class)
            .setMaxResults(100)
            .list();
    }
}