package cn.itcraft.jrdb.jdbc.session;

import cn.itcraft.jrdb.jdbc.config.JrdbConfig;
import cn.itcraft.jrdb.jdbc.config.OverflowStrategy;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.memory.MemoryPool;
import cn.itcraft.jrdb.jdbc.memory.MemoryPoolConfig;
import cn.itcraft.jrdb.jdbc.query.QueryResult;
import cn.itcraft.jrdb.jdbc.type.TypeHandlerRegistry;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class LargeDataTest {

    private MemoryPool memoryPool;
    private SessionImpl session;
    private JdbcDataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setUrl("jdbc:h2:mem:largetest;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        dataSource = ds;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS large_table (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "name VARCHAR(100), " +
                    "\"value\" INT)");
        }

        MemoryPoolConfig poolConfig = MemoryPoolConfig.builder()
                .preAllocMemSize(1 * 1024 * 1024)
                .queryCacheSize(64 * 1024)
                .build();
        memoryPool = new MemoryPool(poolConfig);
    }

    @AfterEach
    void tearDown() {
        if (session != null) {
            session.close();
        }
        if (memoryPool != null) {
            memoryPool.destroy();
        }
    }

    @Test
    void testLargeDataThrowsError() throws Exception {
        JrdbConfig config = JrdbConfig.builder()
                .overflowStrategy(OverflowStrategy.ERROR)
                .build();
        MapperRegistry mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        session = new SessionImpl(memoryPool, config, dataSource, mapperRegistry);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO large_table (name, \"value\") " +
                    "SELECT 'name_' || x, x FROM SYSTEM_RANGE(1, 100000)");
        }

        assertThrows(RuntimeException.class, () -> {
            session.query("SELECT * FROM large_table");
        });
    }

    @Test
    void testLargeDataTruncated() throws Exception {
        JrdbConfig config = JrdbConfig.builder()
                .overflowStrategy(OverflowStrategy.TRUNCATE)
                .build();
        MapperRegistry mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        session = new SessionImpl(memoryPool, config, dataSource, mapperRegistry);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO large_table (name, \"value\") " +
                    "SELECT 'name_' || x, x FROM SYSTEM_RANGE(1, 100000)");
        }

        try (QueryResult result = session.query("SELECT * FROM large_table")) {
            int count = 0;
            while (result.next()) {
                count++;
            }
            assertTrue(count > 0 && count < 100000, 
                    "Result should be truncated: count=" + count);
        }
    }
}