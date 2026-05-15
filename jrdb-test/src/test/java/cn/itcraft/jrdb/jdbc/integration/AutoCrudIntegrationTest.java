package cn.itcraft.jrdb.jdbc.integration;

import cn.itcraft.jrdb.jdbc.annotation.AutoCrud;
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

@DisplayName("AutoCrud Integration Test")
class AutoCrudIntegrationTest {

    private SessionFactory sessionFactory;
    private JdbcDataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:autocrudtest;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS public_user_entity (id BIGINT PRIMARY KEY, name VARCHAR(100), age INT)");
        stmt.close();
        conn.close();

        JrdbConfig config = JrdbConfig.builder().build();
        sessionFactory = new SessionFactory(config, dataSource);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (sessionFactory != null) sessionFactory.close();
        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("DROP TABLE IF EXISTS public_user_entity");
        stmt.close();
        conn.close();
    }

    @Test
    @DisplayName("AutoCrud insert should work")
    void autoCrudInsert() throws Exception {
        Session session = sessionFactory.openSession();
        DaoRegistry daoRegistry = sessionFactory.getDaoRegistry();
        
        try {
            AutoCrudDao dao = daoRegistry.getDao(AutoCrudDao.class, session);
            
            PublicUserEntity entity = new PublicUserEntity();
            entity.setId(1L);
            entity.setName("Alice");
            entity.setAge(30);
            
            int result = dao.insert(entity);
            assertEquals(1, result);
            
            PublicUserEntity loaded = dao.selectById(1L);
            assertNotNull(loaded);
            assertEquals("Alice", loaded.getName());
            assertEquals(30, loaded.getAge());
        } finally {
            session.close();
        }
    }

    @Test
    @DisplayName("AutoCrud update should work")
    void autoCrudUpdate() throws Exception {
        Session session = sessionFactory.openSession();
        DaoRegistry daoRegistry = sessionFactory.getDaoRegistry();
        
        try {
            AutoCrudDao dao = daoRegistry.getDao(AutoCrudDao.class, session);
            
            PublicUserEntity entity = new PublicUserEntity();
            entity.setId(1L);
            entity.setName("Alice");
            entity.setAge(30);
            dao.insert(entity);
            
            entity.setName("Bob");
            entity.setAge(35);
            int result = dao.update(entity);
            assertEquals(1, result);
            
            PublicUserEntity loaded = dao.selectById(1L);
            assertEquals("Bob", loaded.getName());
            assertEquals(35, loaded.getAge());
        } finally {
            session.close();
        }
    }

    @Test
    @DisplayName("AutoCrud delete should work")
    void autoCrudDelete() throws Exception {
        Session session = sessionFactory.openSession();
        DaoRegistry daoRegistry = sessionFactory.getDaoRegistry();
        
        try {
            AutoCrudDao dao = daoRegistry.getDao(AutoCrudDao.class, session);
            
            PublicUserEntity entity = new PublicUserEntity();
            entity.setId(1L);
            entity.setName("Alice");
            entity.setAge(30);
            dao.insert(entity);
            
            int result = dao.delete(1L);
            assertEquals(1, result);
            
            PublicUserEntity loaded = dao.selectById(1L);
            assertNull(loaded);
        } finally {
            session.close();
        }
    }

    @Test
    @DisplayName("AutoCrud selectById should return null when not found")
    void autoCrudSelectByIdNotFound() throws Exception {
        Session session = sessionFactory.openSession();
        DaoRegistry daoRegistry = sessionFactory.getDaoRegistry();
        
        try {
            AutoCrudDao dao = daoRegistry.getDao(AutoCrudDao.class, session);
            
            PublicUserEntity loaded = dao.selectById(999L);
            assertNull(loaded);
        } finally {
            session.close();
        }
    }

    @AutoCrud(entity = PublicUserEntity.class, primaryKey = "id")
    public interface AutoCrudDao {
        int insert(PublicUserEntity entity);
        int update(PublicUserEntity entity);
        int delete(Long id);
        PublicUserEntity selectById(Long id);
    }
}