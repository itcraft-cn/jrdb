package cn.itcraft.jrdb.jdbc.integration;

import cn.itcraft.jrdb.jdbc.annotation.JdbcStyle;
import cn.itcraft.jrdb.jdbc.annotation.Select;
import cn.itcraft.jrdb.jdbc.annotation.Update;
import cn.itcraft.jrdb.jdbc.annotation.Insert;
import cn.itcraft.jrdb.jdbc.annotation.Delete;
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

@DisplayName("Jdbc Style Integration Test")
class JdbcStyleIntegrationTest {

    private SessionFactory sessionFactory;
    private JdbcDataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test_jdbc_style;DB_CLOSE_DELAY=-1");
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
    @DisplayName("Jdbc style DAO should query single entity with ? placeholder")
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
    @DisplayName("Jdbc style DAO should query with parameter binding")
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

    @Test
    @DisplayName("Jdbc style DAO should insert entity")
    void insertEntity() throws Exception {
        Session session = sessionFactory.openSession();
        DaoRegistry daoRegistry = sessionFactory.getDaoRegistry();
        UserDao userDao = daoRegistry.getDao(UserDao.class, session);

        try {
            int result = userDao.insert(3L, "Charlie", 28);
            assertEquals(1, result);

            UserEntity user = userDao.findById(3L);
            assertNotNull(user);
            assertEquals("Charlie", user.getName());
        } finally {
            session.close();
        }
    }

    @Test
    @DisplayName("Jdbc style DAO should update entity")
    void updateEntity() throws Exception {
        Session session = sessionFactory.openSession();
        DaoRegistry daoRegistry = sessionFactory.getDaoRegistry();
        UserDao userDao = daoRegistry.getDao(UserDao.class, session);

        try {
            int rows = userDao.updateAge(35, 1L);
            assertEquals(1, rows);

            UserEntity user = userDao.findById(1L);
            assertEquals(35, user.getAge());
        } finally {
            session.close();
        }
    }

    @Test
    @DisplayName("Jdbc style DAO should delete entity")
    void deleteEntity() throws Exception {
        Session session = sessionFactory.openSession();
        DaoRegistry daoRegistry = sessionFactory.getDaoRegistry();
        UserDao userDao = daoRegistry.getDao(UserDao.class, session);

        try {
            int rows = userDao.delete(2L);
            assertEquals(1, rows);

            UserEntity user = userDao.findById(2L);
            assertNull(user);
        } finally {
            session.close();
        }
    }

    @JdbcStyle
    public interface UserDao {
        @Select("SELECT * FROM users WHERE id = ?")
        UserEntity findById(Long id);

        @Select("SELECT * FROM users WHERE name = ?")
        UserEntity findByName(String name);

        @Insert("INSERT INTO users (id, name, age) VALUES (?, ?, ?)")
        int insert(Long id, String name, Integer age);

        @Update("UPDATE users SET age = ? WHERE id = ?")
        int updateAge(Integer age, Long id);

        @Delete("DELETE FROM users WHERE id = ?")
        int delete(Long id);
    }
}