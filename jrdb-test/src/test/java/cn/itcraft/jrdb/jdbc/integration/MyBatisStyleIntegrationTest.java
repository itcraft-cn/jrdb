package cn.itcraft.jrdb.jdbc.integration;

import cn.itcraft.jrdb.jdbc.annotation.MyBatisStyle;
import cn.itcraft.jrdb.jdbc.annotation.Select;
import cn.itcraft.jrdb.jdbc.config.JrdbConfig;
import cn.itcraft.jrdb.jdbc.dao.DaoRegistry;
import cn.itcraft.jrdb.jdbc.session.Session;
import cn.itcraft.jrdb.jdbc.session.SessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MyBatis Style Integration Test")
class MyBatisStyleIntegrationTest {

    private SessionFactory sessionFactory;
    private JdbcDataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS users (id BIGINT PRIMARY KEY, name VARCHAR(100), age INT)");
        stmt.execute("INSERT INTO users VALUES (1, 'Alice', 30)");
        stmt.execute("INSERT INTO users VALUES (2, 'Bob', 25)");
        stmt.close();
        conn.close();

        JrdbConfig config = JrdbConfig.builder().build();
        sessionFactory = new SessionFactory(config, dataSource);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (sessionFactory != null) {
            sessionFactory.close();
        }

        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("DROP TABLE IF EXISTS users");
        stmt.close();
        conn.close();
    }

    @Test
    @DisplayName("MyBatis style DAO should query single entity")
    void querySingleEntity() throws Exception {
        Session session = sessionFactory.openSession();
        DaoRegistry daoRegistry = sessionFactory.getDaoRegistry();
        UserDao userDao = daoRegistry.getDao(UserDao.class, session);

        try {
            UserEntity user = userDao.findById(1L);
            assertNotNull(user);
            assertEquals(1L, user.getId());
            assertEquals("Alice", user.getName());
            assertEquals(30, user.getAge());
        } finally {
            session.close();
        }
    }

    @Test
    @DisplayName("MyBatis style DAO should query with parameter binding")
    void queryWithParameterBinding() throws Exception {
        Session session = sessionFactory.openSession();
        DaoRegistry daoRegistry = sessionFactory.getDaoRegistry();
        UserDao userDao = daoRegistry.getDao(UserDao.class, session);

        try {
            UserEntity user = userDao.findByName("Bob");
            assertNotNull(user);
            assertEquals(2L, user.getId());
            assertEquals("Bob", user.getName());
        } finally {
            session.close();
        }
    }

    @MyBatisStyle
    public interface UserDao {
        @Select("SELECT * FROM users WHERE id = #{id}")
        UserEntity findById(Long id);

        @Select("SELECT * FROM users WHERE name = #{name}")
        UserEntity findByName(String name);
    }

    static class User {
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