package cn.itcraft.jrdb.benchmark;

import cn.itcraft.jrdb.benchmark.entity.User;
import cn.itcraft.jrdb.benchmark.entity.UserEntity;
import cn.itcraft.jrdb.core.config.CacheLevel;
import cn.itcraft.jrdb.core.config.JrdbConfig;
import cn.itcraft.jrdb.core.mapping.EntityMapper;
import cn.itcraft.jrdb.core.mapping.MapperRegistry;
import cn.itcraft.jrdb.core.memory.MemoryPool;
import cn.itcraft.jrdb.core.memory.MemoryPoolConfig;
import cn.itcraft.jrdb.core.session.SessionImpl;
import cn.itcraft.jrdb.core.type.TypeHandlerRegistry;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 20, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class BatchOperationBenchmark {

    private static final int BATCH_SIZE = 100;

    @State(Scope.Benchmark)
    public static class SharedState {
        DataSource dataSource;
        MemoryPool memoryPool;
        MapperRegistry mapperRegistry;

        @Setup(Level.Trial)
        public void setup() throws SQLException {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setUrl("jdbc:h2:mem:batchbench;DB_CLOSE_DELAY=-1");
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

        public void cleanupData() throws SQLException {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM users");
            }
        }

        public void insertTestData(int count) throws SQLException {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO users (name, age, email, status) VALUES (?, ?, ?, ?)")) {
                for (int i = 0; i < count; i++) {
                    ps.setString(1, "user_" + i);
                    ps.setInt(2, 20 + (i % 50));
                    ps.setString(3, "user_" + i + "@test.com");
                    ps.setString(4, "active");
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }

        public List<User> createUsers(int count) {
            List<User> users = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                users.add(new User(null, "user_" + i, 20 + (i % 50), "user_" + i + "@test.com", "active"));
            }
            return users;
        }

        public List<Object[]> createParams(int count) {
            List<Object[]> params = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                params.add(new Object[]{"user_" + i, 20 + (i % 50), "user_" + i + "@test.com", "active"});
            }
            return params;
        }
    }

    @State(Scope.Thread)
    public static class JrdbState {
        SessionImpl session;
        DataSource dataSource;

        @Setup(Level.Trial)
        public void setup(SharedState shared) {
            JrdbConfig config = JrdbConfig.builder()
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
    public static class JrdbAsmState {
        EntityMapper<User> mapper;
        SessionImpl session;
        DataSource dataSource;

        @Setup(Level.Trial)
        public void setup(SharedState shared) {
            mapper = shared.mapperRegistry.getMapper(User.class);
            JrdbConfig config = JrdbConfig.builder()
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
            config.setProperty("hibernate.connection.url", "jdbc:h2:mem:batchbench;DB_CLOSE_DELAY=-1");
            config.setProperty("hibernate.connection.username", "sa");
            config.setProperty("hibernate.connection.password", "");
            config.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            config.setProperty("hibernate.cache.use_second_level_cache", "false");
            config.setProperty("hibernate.cache.use_query_cache", "false");
            config.setProperty("hibernate.show_sql", "false");
            config.setProperty("hibernate.jdbc.batch_size", String.valueOf(BATCH_SIZE));
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
        PreparedStatement updatePs;
        PreparedStatement deletePs;
        DataSource dataSource;

        @Setup(Level.Trial)
        public void setup(SharedState shared) throws SQLException {
            connection = shared.dataSource.getConnection();
            insertPs = connection.prepareStatement(
                    "INSERT INTO users (name, age, email, status) VALUES (?, ?, ?, ?)");
            updatePs = connection.prepareStatement(
                    "UPDATE users SET name = ?, age = ? WHERE id = ?");
            deletePs = connection.prepareStatement(
                    "DELETE FROM users WHERE id = ?");
            dataSource = shared.dataSource;
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
            if (updatePs != null) updatePs.close();
            if (deletePs != null) deletePs.close();
            if (connection != null) connection.close();
        }
    }

    public interface UserMapper {
        @Insert("INSERT INTO users (name, age, email, status) VALUES (#{name}, #{age}, #{email}, #{status})")
        int insert(User user);

        @Update("UPDATE users SET name = #{name}, age = #{age} WHERE id = #{id}")
        int update(User user);

        @Delete("DELETE FROM users WHERE id = #{id}")
        int delete(Long id);
    }

    // ==================== Batch Insert ====================

    @Benchmark
    public int[] jrdb_raw_batchInsert(JrdbState state, SharedState shared) {
        List<Object[]> params = shared.createParams(BATCH_SIZE);
        return state.session.batchExecute(
                "INSERT INTO users (name, age, email, status) VALUES (?, ?, ?, ?)", params);
    }

    @Benchmark
    public int[] jrdb_asm_batchInsert(JrdbAsmState state, SharedState shared) {
        List<User> users = shared.createUsers(BATCH_SIZE);
        List<Object[]> paramsList = new ArrayList<>(BATCH_SIZE);
        for (User user : users) {
            Object[] allParams = state.mapper.mapToRow(user);
            paramsList.add(Arrays.copyOfRange(allParams, 1, 5));
        }
        return state.session.batchExecute(
                "INSERT INTO users (name, age, email, status) VALUES (?, ?, ?, ?)", paramsList);
    }

    @Benchmark
    public int myBatis_batchInsert(MyBatisState state, SharedState shared) {
        List<User> users = shared.createUsers(BATCH_SIZE);
        int count = 0;
        for (User user : users) {
            count += state.session.insert("cn.itcraft.jrdb.benchmark.BatchOperationBenchmark$UserMapper.insert", user);
        }
        state.session.commit();
        return count;
    }

    @Benchmark
    public int hibernate_batchInsert(HibernateState state, SharedState shared) {
        List<UserEntity> entities = new ArrayList<>(BATCH_SIZE);
        for (int i = 0; i < BATCH_SIZE; i++) {
            UserEntity user = new UserEntity();
            user.setName("user_" + i);
            user.setAge(20 + (i % 50));
            user.setEmail("user_" + i + "@test.com");
            user.setStatus("active");
            entities.add(user);
        }
        state.session.beginTransaction();
        for (UserEntity entity : entities) {
            state.session.save(entity);
        }
        state.session.getTransaction().commit();
        state.session.clear();
        return BATCH_SIZE;
    }

    @Benchmark
    public int[] jdbc_batchInsert(JdbcState state, SharedState shared) throws SQLException {
        for (int i = 0; i < BATCH_SIZE; i++) {
            state.insertPs.setString(1, "user_" + i);
            state.insertPs.setInt(2, 20 + (i % 50));
            state.insertPs.setString(3, "user_" + i + "@test.com");
            state.insertPs.setString(4, "active");
            state.insertPs.addBatch();
        }
        return state.insertPs.executeBatch();
    }

    // ==================== Batch Update ====================

    @Benchmark
    public int[] jrdb_raw_batchUpdate(JrdbState state, SharedState shared) throws SQLException {
        shared.insertTestData(BATCH_SIZE);
        
        List<Object[]> params = new ArrayList<>(BATCH_SIZE);
        for (int i = 1; i <= BATCH_SIZE; i++) {
            params.add(new Object[]{"updated_" + i, 30 + i, (long) i});
        }
        return state.session.batchExecute(
                "UPDATE users SET name = ?, age = ? WHERE id = ?", params);
    }

    @Benchmark
    public int[] jrdb_asm_batchUpdate(JrdbAsmState state, SharedState shared) throws SQLException {
        shared.insertTestData(BATCH_SIZE);
        
        List<User> users = new ArrayList<>(BATCH_SIZE);
        for (int i = 1; i <= BATCH_SIZE; i++) {
            users.add(new User((long) i, "updated_" + i, 30 + i, null, null));
        }
        List<Object[]> paramsList = new ArrayList<>(BATCH_SIZE);
        for (User user : users) {
            paramsList.add(new Object[]{user.getName(), user.getAge(), user.getId()});
        }
        return state.session.batchExecute(
                "UPDATE users SET name = ?, age = ? WHERE id = ?", paramsList);
    }

    @Benchmark
    public int myBatis_batchUpdate(MyBatisState state, SharedState shared) throws SQLException {
        shared.insertTestData(BATCH_SIZE);
        
        int count = 0;
        for (int i = 1; i <= BATCH_SIZE; i++) {
            User user = new User((long) i, "updated_" + i, 30 + i, null, null);
            count += state.session.update("cn.itcraft.jrdb.benchmark.BatchOperationBenchmark$UserMapper.update", user);
        }
        state.session.commit();
        return count;
    }

    @Benchmark
    public int hibernate_batchUpdate(HibernateState state, SharedState shared) throws SQLException {
        shared.insertTestData(BATCH_SIZE);
        
        state.session.beginTransaction();
        for (int i = 1; i <= BATCH_SIZE; i++) {
            UserEntity user = state.session.get(UserEntity.class, (long) i);
            if (user != null) {
                user.setName("updated_" + i);
                user.setAge(30 + i);
                state.session.update(user);
            }
        }
        state.session.getTransaction().commit();
        state.session.clear();
        return BATCH_SIZE;
    }

    @Benchmark
    public int[] jdbc_batchUpdate(JdbcState state, SharedState shared) throws SQLException {
        shared.insertTestData(BATCH_SIZE);
        
        for (int i = 1; i <= BATCH_SIZE; i++) {
            state.updatePs.setString(1, "updated_" + i);
            state.updatePs.setInt(2, 30 + i);
            state.updatePs.setLong(3, i);
            state.updatePs.addBatch();
        }
        return state.updatePs.executeBatch();
    }

    // ==================== Batch Delete ====================

    @Benchmark
    public int[] jrdb_raw_batchDelete(JrdbState state, SharedState shared) throws SQLException {
        shared.insertTestData(BATCH_SIZE);
        
        List<Object[]> params = new ArrayList<>(BATCH_SIZE);
        for (int i = 1; i <= BATCH_SIZE; i++) {
            params.add(new Object[]{(long) i});
        }
        return state.session.batchExecute("DELETE FROM users WHERE id = ?", params);
    }

    @Benchmark
    public int[] jrdb_asm_batchDelete(JrdbAsmState state, SharedState shared) throws SQLException {
        shared.insertTestData(BATCH_SIZE);
        
        List<User> users = new ArrayList<>(BATCH_SIZE);
        for (int i = 1; i <= BATCH_SIZE; i++) {
            users.add(new User((long) i, null, null, null, null));
        }
        List<Object[]> paramsList = new ArrayList<>(BATCH_SIZE);
        for (User user : users) {
            paramsList.add(new Object[]{user.getId()});
        }
        return state.session.batchExecute("DELETE FROM users WHERE id = ?", paramsList);
    }

    @Benchmark
    public int myBatis_batchDelete(MyBatisState state, SharedState shared) throws SQLException {
        shared.insertTestData(BATCH_SIZE);
        
        int count = 0;
        for (int i = 1; i <= BATCH_SIZE; i++) {
            count += state.session.delete("cn.itcraft.jrdb.benchmark.BatchOperationBenchmark$UserMapper.delete", (long) i);
        }
        state.session.commit();
        return count;
    }

    @Benchmark
    public int hibernate_batchDelete(HibernateState state, SharedState shared) throws SQLException {
        shared.insertTestData(BATCH_SIZE);
        
        state.session.beginTransaction();
        for (int i = 1; i <= BATCH_SIZE; i++) {
            UserEntity user = state.session.get(UserEntity.class, (long) i);
            if (user != null) {
                state.session.delete(user);
            }
        }
        state.session.getTransaction().commit();
        state.session.clear();
        return BATCH_SIZE;
    }

    @Benchmark
    public int[] jdbc_batchDelete(JdbcState state, SharedState shared) throws SQLException {
        shared.insertTestData(BATCH_SIZE);
        
        for (int i = 1; i <= BATCH_SIZE; i++) {
            state.deletePs.setLong(1, i);
            state.deletePs.addBatch();
        }
        return state.deletePs.executeBatch();
    }
}