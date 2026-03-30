package com.dbmmap.benchmark;

import com.dbmmap.benchmark.entity.UserEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.openjdk.jmh.annotations.*;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class HibernateQueryBenchmark extends BenchmarkBase {

    private SessionFactory sessionFactory;
    private Session session;

    @Setup(Level.Trial)
    public void setup() throws SQLException {
        initDatabase();

        Configuration config = new Configuration();
        config.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        config.setProperty("hibernate.connection.url", "jdbc:h2:mem:benchmark;DB_CLOSE_DELAY=-1");
        config.setProperty("hibernate.connection.username", "sa");
        config.setProperty("hibernate.connection.password", "");
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        config.setProperty("hibernate.hbm2ddl.auto", "none");
        config.setProperty("hibernate.show_sql", "false");
        config.addAnnotatedClass(UserEntity.class);

        sessionFactory = config.buildSessionFactory();
        session = sessionFactory.openSession();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (session != null) session.close();
        if (sessionFactory != null) sessionFactory.close();
    }

    @Benchmark
    public UserEntity singleQuery() {
        return session.get(UserEntity.class, 1L);
    }

    @Benchmark
    @SuppressWarnings("unchecked")
    public List<UserEntity> queryList() {
        return session.createQuery("FROM UserEntity", UserEntity.class)
            .setMaxResults(100)
            .list();
    }
}