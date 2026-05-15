package cn.itcraft.jrdb.benchmark;

import cn.itcraft.jrdb.jdbc.annotation.HibernateStyle;
import cn.itcraft.jrdb.jdbc.annotation.JdbcStyle;
import cn.itcraft.jrdb.jdbc.annotation.MyBatisStyle;
import cn.itcraft.jrdb.jdbc.annotation.Select;
import cn.itcraft.jrdb.jdbc.annotation.Update;
import cn.itcraft.jrdb.jdbc.config.JrdbConfig;
import cn.itcraft.jrdb.jdbc.dao.DaoRegistry;
import cn.itcraft.jrdb.jdbc.session.Session;
import cn.itcraft.jrdb.jdbc.session.SessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Fork;

import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class StyleComparisonBenchmark {

    @State(Scope.Benchmark)
    public static class SharedState {
        JdbcDataSource dataSource;
        SessionFactory sessionFactory;

        @Setup(Level.Trial)
        public void setup() throws Exception {
            dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:style_benchmark;DB_CLOSE_DELAY=-1");
            dataSource.setUser("sa");
            dataSource.setPassword("");

            Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS benchmark_users (id BIGINT PRIMARY KEY, name VARCHAR(100), age INT)");
            stmt.execute("INSERT INTO benchmark_users VALUES (1, 'Alice', 25)");
            stmt.close();
            conn.close();

            JrdbConfig config = JrdbConfig.builder().build();
            sessionFactory = new SessionFactory(config, dataSource);
        }

        @TearDown(Level.Trial)
        public void tearDown() throws Exception {
            if (sessionFactory != null) {
                sessionFactory.close();
            }

            Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            stmt.execute("DROP TABLE IF EXISTS benchmark_users");
            stmt.close();
            conn.close();
        }
    }

    @State(Scope.Thread)
    public static class ThreadState {
        Session session;
        JdbcStyleDao jdbcStyleDao;
        MyBatisStyleDao myBatisStyleDao;
        HibernateStyleDao hibernateStyleDao;
        int counter = 0;

        @Setup(Level.Iteration)
        public void setup(SharedState shared) throws Exception {
            session = shared.sessionFactory.openSession();
            DaoRegistry daoRegistry = shared.sessionFactory.getDaoRegistry();
            jdbcStyleDao = daoRegistry.getDao(JdbcStyleDao.class, session);
            myBatisStyleDao = daoRegistry.getDao(MyBatisStyleDao.class, session);
            hibernateStyleDao = daoRegistry.getDao(HibernateStyleDao.class, session);
        }

        @TearDown(Level.Iteration)
        public void tearDown() throws Exception {
            if (session != null) {
                session.close();
            }
        }
    }

    @Benchmark
    public Object jdbcStyle_selectById(ThreadState state) {
        return state.jdbcStyleDao.findById(1L);
    }

    @Benchmark
    public Object myBatisStyle_selectById(ThreadState state) {
        return state.myBatisStyleDao.findById(1L);
    }

    @Benchmark
    public Object hibernateStyle_selectById(ThreadState state) {
        return state.hibernateStyleDao.findById(1L);
    }

    @Benchmark
    public int jdbcStyle_update(ThreadState state) {
        int age = 25 + (state.counter++ % 10);
        int result = state.jdbcStyleDao.updateAge(age, 1L);
        return result;
    }

    @Benchmark
    public int myBatisStyle_update(ThreadState state) {
        int age = 25 + (state.counter++ % 10);
        int result = state.myBatisStyleDao.updateAge(age, 1L);
        return result;
    }

    @Benchmark
    public int hibernateStyle_update(ThreadState state) {
        int age = 25 + (state.counter++ % 10);
        int result = state.hibernateStyleDao.updateAge(age, 1L);
        return result;
    }

    @JdbcStyle
    public interface JdbcStyleDao {
        @Select("SELECT * FROM benchmark_users WHERE id = ?")
        BenchmarkUser findById(Long id);

        @Update("UPDATE benchmark_users SET age = ? WHERE id = ?")
        int updateAge(Integer age, Long id);
    }

    @MyBatisStyle
    public interface MyBatisStyleDao {
        @Select("SELECT * FROM benchmark_users WHERE id = #{id}")
        BenchmarkUser findById(Long id);

        @Update("UPDATE benchmark_users SET age = #{age} WHERE id = #{id}")
        int updateAge(Integer age, Long id);
    }

    @HibernateStyle
    public interface HibernateStyleDao {
        @Select("SELECT * FROM benchmark_users WHERE id = :id")
        BenchmarkUser findById(Long id);

        @Update("UPDATE benchmark_users SET age = :age WHERE id = :id")
        int updateAge(Integer age, Long id);
    }

    public static class BenchmarkUser {
        private Long id;
        private String name;
        private Integer age;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
    }
}