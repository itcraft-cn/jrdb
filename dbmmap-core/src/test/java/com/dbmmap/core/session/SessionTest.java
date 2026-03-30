package com.dbmmap.core.session;

import com.dbmmap.core.config.DbMmapConfig;
import com.dbmmap.core.memory.MemoryPool;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionTest {

    private SessionFactory factory;
    private JdbcDataSource dataSource;

    @BeforeEach
    void setUp() {
        dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        
        DbMmapConfig config = DbMmapConfig.builder()
                .memoryPoolConfig(DbMmapConfig.builder().build().getMemoryPoolConfig())
                .build();
        factory = new SessionFactory(config, dataSource);
    }

    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void shouldOpenSession() {
        Session session = factory.openSession();
        assertNotNull(session);
        assertFalse(session.isClosed());
        session.close();
    }

    @Test
    void shouldCloseSession() {
        Session session = factory.openSession();
        assertFalse(session.isClosed());
        session.close();
        assertTrue(session.isClosed());
    }

    @Test
    void shouldGetMemoryPool() {
        Session session = factory.openSession();
        MemoryPool pool = session.getMemoryPool();
        assertNotNull(pool);
        session.close();
    }

    @Test
    void shouldReleaseAllQueryResultsOnClose() {
        Session session = factory.openSession();
        MemoryPool pool = session.getMemoryPool();
        int usedBefore = pool.getUsedSlotCount();
        
        session.close();
        
        assertTrue(session.isClosed());
        assertEquals(usedBefore, pool.getUsedSlotCount());
    }

    @Test
    void shouldCloseFactory() {
        assertFalse(factory.isClosed());
        factory.close();
        assertTrue(factory.isClosed());
    }

    @Test
    void shouldNotOpenSessionAfterFactoryClosed() {
        factory.close();
        assertThrows(IllegalStateException.class, () -> factory.openSession());
    }
}