package cn.itcraft.jrdb.jdbc.session;

import cn.itcraft.jrdb.jdbc.config.JrdbConfig;
import cn.itcraft.jrdb.jdbc.memory.MemoryPool;
import cn.itcraft.jrdb.jdbc.memory.MemoryPoolConfig;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.type.TypeHandlerRegistry;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Session Transaction Test")
class SessionTransactionTest {

    private JdbcDataSource dataSource;
    private SessionImpl session;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:transaction_test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Connection conn = dataSource.getConnection();
        conn.createStatement().execute("CREATE TABLE IF NOT EXISTS users (id BIGINT PRIMARY KEY, name VARCHAR(100), age INT)");
        conn.createStatement().execute("INSERT INTO users VALUES (1, 'Alice', 30)");
        conn.close();

        MemoryPoolConfig poolConfig = MemoryPoolConfig.builder().build();
        MemoryPool memoryPool = new MemoryPool(poolConfig);
        JrdbConfig config = JrdbConfig.builder().build();
        MapperRegistry mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());

        session = new SessionImpl(memoryPool, config, dataSource, mapperRegistry);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (session != null && !session.isClosed()) {
            session.close();
        }

        Connection conn = dataSource.getConnection();
        conn.createStatement().execute("DROP TABLE IF EXISTS users");
        conn.close();
    }

    @Test
    @DisplayName("beginTransaction should start transaction")
    void testBeginTransaction() throws Exception {
        assertFalse(session.isInTransaction());

        session.beginTransaction();
        assertTrue(session.isInTransaction());

        session.update("UPDATE users SET age = 35 WHERE id = 1");
        session.commit();

        assertFalse(session.isInTransaction());
    }

    @Test
    @DisplayName("beginTransaction should throw IllegalStateException when already in transaction")
    void testBeginTransactionTwice() throws Exception {
        session.beginTransaction();
        assertTrue(session.isInTransaction());

        assertThrows(IllegalStateException.class, () -> session.beginTransaction());
        session.rollback();
    }

    @Test
    @DisplayName("commit should commit transaction")
    void testCommit() throws Exception {
        session.beginTransaction();
        session.update("UPDATE users SET age = 35 WHERE id = 1");
        session.commit();

        assertFalse(session.isInTransaction());

        Connection conn = dataSource.getConnection();
        assertTrue(conn.getAutoCommit());
        ResultSet rs = conn.createStatement().executeQuery("SELECT age FROM users WHERE id = 1");
        assertTrue(rs.next());
        assertEquals(35, rs.getInt("age"));
        conn.close();
    }

    @Test
    @DisplayName("commit should throw IllegalStateException when not in transaction")
    void testCommitWithoutTransaction() throws Exception {
        assertFalse(session.isInTransaction());
        assertThrows(IllegalStateException.class, () -> session.commit());
    }

    @Test
    @DisplayName("rollback should rollback transaction")
    void testRollback() throws Exception {
        session.beginTransaction();
        session.update("UPDATE users SET age = 40 WHERE id = 1");
        session.rollback();

        assertFalse(session.isInTransaction());

        Connection conn = dataSource.getConnection();
        assertTrue(conn.getAutoCommit());
        ResultSet rs = conn.createStatement().executeQuery("SELECT age FROM users WHERE id = 1");
        assertTrue(rs.next());
        assertEquals(30, rs.getInt("age"));
        conn.close();
    }

    @Test
    @DisplayName("rollback should throw IllegalStateException when not in transaction")
    void testRollbackWithoutTransaction() throws Exception {
        assertFalse(session.isInTransaction());
        assertThrows(IllegalStateException.class, () -> session.rollback());
    }

    @Test
    @DisplayName("close should automatically rollback uncommitted transaction")
    void testCloseAutoRollback() throws Exception {
        session.beginTransaction();
        session.update("UPDATE users SET age = 50 WHERE id = 1");
        session.close();

        Connection conn = dataSource.getConnection();
        ResultSet rs = conn.createStatement().executeQuery("SELECT age FROM users WHERE id = 1");
        assertTrue(rs.next());
        assertEquals(30, rs.getInt("age"));
        conn.close();
    }

    @Test
    @DisplayName("transaction should ensure atomicity - commit persists, rollback reverts")
    void testTransactionAtomicity() throws Exception {
        session.beginTransaction();
        session.update("UPDATE users SET age = 60 WHERE id = 1");
        session.commit();

        Connection conn = dataSource.getConnection();
        ResultSet rs = conn.createStatement().executeQuery("SELECT age FROM users WHERE id = 1");
        assertTrue(rs.next());
        assertEquals(60, rs.getInt("age"));
        conn.close();

        session.beginTransaction();
        session.update("UPDATE users SET age = 70 WHERE id = 1");
        session.rollback();

        conn = dataSource.getConnection();
        rs = conn.createStatement().executeQuery("SELECT age FROM users WHERE id = 1");
        assertTrue(rs.next());
        assertEquals(60, rs.getInt("age"));
        conn.close();
    }

    @Test
    @DisplayName("setTransactionIsolation should set isolation level")
    void testSetTransactionIsolation() throws Exception {
        session.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        session.beginTransaction();

        Connection conn = dataSource.getConnection();
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, conn.getTransactionIsolation());
        conn.close();

        session.commit();
    }

    @Test
    @DisplayName("setTransactionIsolation should throw IllegalStateException after beginTransaction")
    void testSetTransactionIsolationAfterBegin() throws Exception {
        session.beginTransaction();
        assertThrows(IllegalStateException.class, 
            () -> session.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE));
        session.rollback();
    }

    @Test
    @DisplayName("isClosed should return true after close")
    void testIsClosed() throws Exception {
        assertFalse(session.isClosed());
        session.close();
        assertTrue(session.isClosed());
    }

    @Test
    @DisplayName("beginTransaction should throw IllegalStateException when session is closed")
    void testBeginTransactionOnClosedSession() throws Exception {
        session.close();
        assertTrue(session.isClosed());
        assertThrows(IllegalStateException.class, () -> session.beginTransaction());
    }
}