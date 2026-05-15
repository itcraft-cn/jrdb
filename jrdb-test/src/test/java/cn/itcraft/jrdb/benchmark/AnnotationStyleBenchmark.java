package cn.itcraft.jrdb.benchmark;

import cn.itcraft.jrdb.benchmark.entity.User;
import cn.itcraft.jrdb.jdbc.annotation.AutoCrud;
import cn.itcraft.jrdb.jdbc.annotation.MyBatisStyle;
import cn.itcraft.jrdb.jdbc.annotation.Select;
import cn.itcraft.jrdb.jdbc.config.JrdbConfig;
import cn.itcraft.jrdb.jdbc.dao.DaoRegistry;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.memory.MemoryPool;
import cn.itcraft.jrdb.jdbc.memory.MemoryPoolConfig;
import cn.itcraft.jrdb.jdbc.session.Session;
import cn.itcraft.jrdb.jdbc.session.SessionFactory;
import cn.itcraft.jrdb.jdbc.type.TypeHandlerRegistry;
import org.h2.jdbcx.JdbcDataSource;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class AnnotationStyleBenchmark {

    private static final int DATA_SIZE = 1000;

    public static class BenchmarkUser {
        private Long id;
        private String name;
        private Integer age;
        private String email;
        private String status;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    @State(Scope.Benchmark)
    public static class SharedState {
        DataSource dataSource;
        MemoryPool memoryPool;
        MapperRegistry mapperRegistry;
        SessionFactory sessionFactory;
        DaoRegistry daoRegistry;

        @Setup(Level.Trial)
        public void setup() throws SQLException {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setUrl("jdbc:h2:mem:annotation_benchmark;DB_CLOSE_DELAY=-1");
            ds.setUser("sa");
            ds.setPassword("");
            dataSource = ds;

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS users");
                stmt.execute("CREATE TABLE users (id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100), age INT, email VARCHAR(200), status VARCHAR(20))");
                for (int i = 0; i < DATA_SIZE; i++) {
                    stmt.execute("INSERT INTO users (name, age, email, status) VALUES ('user_" + i + "', " + (20 + i % 50) + ", 'user_" + i + "@test.com', 'active')");
                }
            }

            MemoryPoolConfig poolConfig = MemoryPoolConfig.builder()
                .preAllocMemSize(64 * 1024 * 1024)
                .queryCacheSize(4 * 1024 * 1024)
                .build();
            memoryPool = new MemoryPool(poolConfig);

            TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();
            mapperRegistry = new MapperRegistry(typeHandlerRegistry);

            JrdbConfig config = JrdbConfig.builder().build();
            sessionFactory = new SessionFactory(config, dataSource);
            daoRegistry = sessionFactory.getDaoRegistry();
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (memoryPool != null) memoryPool.destroy();
            if (sessionFactory != null) sessionFactory.close();
        }
    }

    @State(Scope.Thread)
    public static class JrdbAsmState {
        Session session;

        @Setup(Level.Iteration)
        public void setup(SharedState shared) {
            session = shared.sessionFactory.openSession();
        }

        @TearDown(Level.Iteration)
        public void tearDown() {
            if (session != null) session.close();
        }
    }

    @State(Scope.Thread)
    public static class MyBatisStyleState {
        Session session;
        MyBatisStyleDao dao;

        @Setup(Level.Iteration)
        public void setup(SharedState shared) {
            session = shared.sessionFactory.openSession();
            dao = shared.daoRegistry.getDao(MyBatisStyleDao.class, session);
        }

        @TearDown(Level.Iteration)
        public void tearDown() {
            if (session != null) session.close();
        }
    }

    @State(Scope.Thread)
    public static class AutoCrudState {
        Session session;
        AutoCrudDao dao;

        @Setup(Level.Iteration)
        public void setup(SharedState shared) {
            session = shared.sessionFactory.openSession();
            dao = shared.daoRegistry.getDao(AutoCrudDao.class, session);
        }

        @TearDown(Level.Iteration)
        public void tearDown() {
            if (session != null) session.close();
        }
    }

    @MyBatisStyle
    public interface MyBatisStyleDao {
        @Select("SELECT * FROM users WHERE id = #{id}")
        User selectById(Long id);
    }

    @AutoCrud(entity = BenchmarkUser.class, tableName = "users", primaryKey = "id")
    public interface AutoCrudDao {
        int insert(BenchmarkUser entity);
        int update(BenchmarkUser entity);
        int delete(Long id);
        BenchmarkUser selectById(Long id);
    }

    @Benchmark
    public void jrdbAsm_selectById(JrdbAsmState state, Blackhole bh) {
        User user = state.session.queryForObject("SELECT id, name, age, email, status FROM users WHERE id = ?", User.class, 1L);
        bh.consume(user);
    }

    @Benchmark
    public void myBatisStyle_selectById(MyBatisStyleState state, Blackhole bh) {
        User user = state.dao.selectById(1L);
        bh.consume(user);
    }

    @Benchmark
    public void autoCrud_selectById(AutoCrudState state, Blackhole bh) {
        BenchmarkUser user = state.dao.selectById(1L);
        bh.consume(user);
    }

    @Benchmark
    public void jrdbAsm_update(JrdbAsmState state, Blackhole bh) {
        int result = state.session.update("UPDATE users SET name = ?, age = ? WHERE id = ?", 
            "updated_" + System.nanoTime() % 10000, 30, 1L);
        bh.consume(result);
    }

    @Benchmark
    public void autoCrud_update(AutoCrudState state, Blackhole bh) {
        BenchmarkUser user = new BenchmarkUser();
        user.setId(1L);
        user.setName("updated_" + System.nanoTime() % 10000);
        user.setAge(30);
        user.setEmail("updated@test.com");
        user.setStatus("updated");
        int result = state.dao.update(user);
        bh.consume(result);
    }

    @Benchmark
    public void jrdbAsm_delete(JrdbAsmState state, Blackhole bh) {
        int result = state.session.delete("DELETE FROM users WHERE id = ?", 999L);
        bh.consume(result);
    }

    @Benchmark
    public void autoCrud_delete(AutoCrudState state, Blackhole bh) {
        int result = state.dao.delete(999L);
        bh.consume(result);
    }
}