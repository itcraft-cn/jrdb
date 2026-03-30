package com.dbmmap.benchmark;

import com.dbmmap.benchmark.entity.User;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.openjdk.jmh.annotations.*;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class MyBatisQueryBenchmark extends BenchmarkBase {

    private SqlSessionFactory sqlSessionFactory;
    private SqlSession session;

    @Setup(Level.Trial)
    public void setup() throws SQLException {
        initDatabase();

        Configuration config = new Configuration();
        config.setEnvironment(new Environment("benchmark",
            new JdbcTransactionFactory(), dataSource));
        config.setCacheEnabled(false);
        config.setLocalCacheScope(org.apache.ibatis.session.LocalCacheScope.STATEMENT);

        config.addMapper(UserMapper.class);

        sqlSessionFactory = new SqlSessionFactoryBuilder().build(config);
        session = sqlSessionFactory.openSession();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (session != null) session.close();
    }

    @Benchmark
    public User singleQuery() {
        return session.selectOne("com.dbmmap.benchmark.MyBatisQueryBenchmark$UserMapper.getById", 1L);
    }

    @Benchmark
    public List<User> queryList() {
        return session.selectList("com.dbmmap.benchmark.MyBatisQueryBenchmark$UserMapper.getAll");
    }

    public interface UserMapper {
        @Select("SELECT id, name, age, email, status FROM users WHERE id = #{id}")
        User getById(Long id);

        @Select("SELECT id, name, age, email, status FROM users LIMIT 100")
        List<User> getAll();
    }
}