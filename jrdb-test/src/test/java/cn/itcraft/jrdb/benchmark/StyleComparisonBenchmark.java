package cn.itcraft.jrdb.benchmark;

import cn.itcraft.jrdb.jdbc.annotation.Delete;
import cn.itcraft.jrdb.jdbc.annotation.HibernateStyle;
import cn.itcraft.jrdb.jdbc.annotation.Insert;
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

import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class StyleComparisonBenchmark {

    private SessionFactory sessionFactory;
    private JdbcDataSource dataSource;
    private Session session;
    private DaoRegistry daoRegistry;

    private JdbcStyleDao jdbcStyleDao;
    private MyBatisStyleDao myBatisStyleDao;
    private HibernateStyleDao hibernateStyleDao;

    @Setup(Level.Trial)
    public void setupTrial() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:benchmark_style;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS benchmark_users (id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100), age INT)");
        stmt.close();
        conn.close();

        JrdbConfig config = JrdbConfig.builder().build();
        sessionFactory = new SessionFactory(config, dataSource);
    }

    @Setup(Level.Iteration)
    public void setupIteration() throws Exception {
        session = sessionFactory.openSession();
        daoRegistry = sessionFactory.getDaoRegistry();

        jdbcStyleDao = daoRegistry.getDao(JdbcStyleDao.class, session);
        myBatisStyleDao = daoRegistry.getDao(MyBatisStyleDao.class, session);
        hibernateStyleDao = daoRegistry.getDao(HibernateStyleDao.class, session);

        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("DELETE FROM benchmark_users");
        stmt.execute("INSERT INTO benchmark_users VALUES (1, 'Alice', 25)");
        stmt.execute("INSERT INTO benchmark_users VALUES (2, 'Bob', 30)");
        stmt.close();
        conn.close();
    }

    @TearDown(Level.Iteration)
    public void tearDownIteration() throws Exception {
        if (session != null) {
            session.close();
        }
    }

    @TearDown(Level.Trial)
    public void tearDownTrial() throws Exception {
        if (sessionFactory != null) {
            sessionFactory.close();
        }

        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("DROP TABLE IF EXISTS benchmark_users");
        stmt.close();
        conn.close();
    }

    @Benchmark
    public Object jdbcStyle_selectById() {
        return jdbcStyleDao.findById(1L);
    }

    @Benchmark
    public Object myBatisStyle_selectById() {
        return myBatisStyleDao.findById(1L);
    }

    @Benchmark
    public Object hibernateStyle_selectById() {
        return hibernateStyleDao.findById(1L);
    }

    @Benchmark
    public int jdbcStyle_update() {
        return jdbcStyleDao.updateAge(28, 1L);
    }

    @Benchmark
    public int myBatisStyle_update() {
        return myBatisStyleDao.updateAge(28, 1L);
    }

    @Benchmark
    public int hibernateStyle_update() {
        return hibernateStyleDao.updateAge(28, 1L);
    }

    @Benchmark
    public int jdbcStyle_delete() {
        int result = jdbcStyleDao.delete(2L);
        if (result > 0) {
            try {
                Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement();
                stmt.execute("INSERT INTO benchmark_users VALUES (2, 'Bob', 30)");
                stmt.close();
                conn.close();
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    @Benchmark
    public int myBatisStyle_delete() {
        int result = myBatisStyleDao.delete(2L);
        if (result > 0) {
            try {
                Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement();
                stmt.execute("INSERT INTO benchmark_users VALUES (2, 'Bob', 30)");
                stmt.close();
                conn.close();
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    @Benchmark
    public int hibernateStyle_delete() {
        int result = hibernateStyleDao.delete(2L);
        if (result > 0) {
            try {
                Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement();
                stmt.execute("INSERT INTO benchmark_users VALUES (2, 'Bob', 30)");
                stmt.close();
                conn.close();
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    @JdbcStyle
    public interface JdbcStyleDao {
        @Select("SELECT * FROM benchmark_users WHERE id = ?")
        BenchmarkUser findById(Long id);

        @Update("UPDATE benchmark_users SET age = ? WHERE id = ?")
        int updateAge(Integer age, Long id);

        @Delete("DELETE FROM benchmark_users WHERE id = ?")
        int delete(Long id);
    }

    @MyBatisStyle
    public interface MyBatisStyleDao {
        @Select("SELECT * FROM benchmark_users WHERE id = #{id}")
        BenchmarkUser findById(Long id);

        @Update("UPDATE benchmark_users SET age = #{age} WHERE id = #{id}")
        int updateAge(Integer age, Long id);

        @Delete("DELETE FROM benchmark_users WHERE id = #{id}")
        int delete(Long id);
    }

    @HibernateStyle
    public interface HibernateStyleDao {
        @Select("SELECT * FROM benchmark_users WHERE id = :id")
        BenchmarkUser findById(Long id);

        @Update("UPDATE benchmark_users SET age = :age WHERE id = :id")
        int updateAge(Integer age, Long id);

        @Delete("DELETE FROM benchmark_users WHERE id = :id")
        int delete(Long id);
    }

    static class BenchmarkUser {
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