package cn.itcraft.jrdb.jdbc.handler;

import cn.itcraft.jrdb.jdbc.config.CacheLevel;
import cn.itcraft.jrdb.jdbc.config.JrdbConfig;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.memory.MemoryPool;
import cn.itcraft.jrdb.jdbc.memory.MemoryPoolConfig;
import cn.itcraft.jrdb.jdbc.session.SessionImpl;
import cn.itcraft.jrdb.jdbc.type.TypeHandlerRegistry;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RowHandlerTest {

    private MemoryPool memoryPool;
    private SessionImpl session;
    private JdbcDataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setUrl("jdbc:h2:mem:rowhandler;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        dataSource = ds;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS users");
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "name VARCHAR(100), " +
                    "age INT, " +
                    "score DOUBLE)");
            stmt.execute("INSERT INTO users (name, age, score) VALUES ('Alice', 25, 95.5)");
            stmt.execute("INSERT INTO users (name, age, score) VALUES ('Bob', 30, 88.0)");
            stmt.execute("INSERT INTO users (name, age, score) VALUES ('Charlie', 35, 92.3)");
        }

        MemoryPoolConfig poolConfig = MemoryPoolConfig.builder()
                .preAllocMemSize(64 * 1024 * 1024)
                .queryCacheSize(4 * 1024 * 1024)
                .build();
        memoryPool = new MemoryPool(poolConfig);

        JrdbConfig config = JrdbConfig.builder()
                .cacheLevel(CacheLevel.NONE)
                .build();
        MapperRegistry mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        session = new SessionImpl(memoryPool, config, dataSource, mapperRegistry);
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
    void testBasicRowHandler() {
        List<byte[]> rows = new ArrayList<>();
        List<RowMeta> metas = new ArrayList<>();

        session.queryWithHandler("SELECT id, name, age, score FROM users", (rawRow, meta) -> {
            rows.add(rawRow.clone());
            metas.add(meta);
        });

        assertEquals(3, rows.size());
        assertEquals(3, metas.size());

        RowMeta meta = metas.get(0);
        assertEquals(4, meta.getColumnCount());
        assertEquals("ID", meta.getColumn(0).getName());
        assertEquals("NAME", meta.getColumn(1).getName());
        assertEquals("AGE", meta.getColumn(2).getName());
        assertEquals("SCORE", meta.getColumn(3).getName());
    }

    @Test
    void testRowHandlerWithParams() {
        List<byte[]> rows = new ArrayList<>();

        session.queryWithHandler("SELECT id, name, age FROM users WHERE age > ?", (rawRow, meta) -> {
            rows.add(rawRow.clone());
        }, 28);

        assertEquals(2, rows.size());
    }

    @Test
    void testRawRowSize() {
        int[] rowSize = new int[1];

        session.queryWithHandler("SELECT id, name, age, score FROM users", (rawRow, meta) -> {
            rowSize[0] = rawRow.length;
        });

        assertTrue(rowSize[0] > 0);
    }

    @Test
    void testExtractIntFromRawRow() {
        List<Integer> ages = new ArrayList<>();

        session.queryWithHandler("SELECT age FROM users ORDER BY age", (rawRow, meta) -> {
            int age = ((rawRow[0] & 0xFF) << 24) |
                    ((rawRow[1] & 0xFF) << 16) |
                    ((rawRow[2] & 0xFF) << 8) |
                    (rawRow[3] & 0xFF);
            ages.add(age);
        });

        assertEquals(3, ages.size());
        assertEquals(25, ages.get(0));
        assertEquals(30, ages.get(1));
        assertEquals(35, ages.get(2));
    }

    @Test
    void testExtractLongFromRawRow() {
        List<Long> ids = new ArrayList<>();

        session.queryWithHandler("SELECT id FROM users ORDER BY id", (rawRow, meta) -> {
            long id = ((long) (rawRow[0] & 0xFF) << 56) |
                    ((long) (rawRow[1] & 0xFF) << 48) |
                    ((long) (rawRow[2] & 0xFF) << 40) |
                    ((long) (rawRow[3] & 0xFF) << 32) |
                    ((long) (rawRow[4] & 0xFF) << 24) |
                    ((long) (rawRow[5] & 0xFF) << 16) |
                    ((long) (rawRow[6] & 0xFF) << 8) |
                    ((long) (rawRow[7] & 0xFF));
            ids.add(id);
        });

        assertEquals(3, ids.size());
        assertEquals(1L, ids.get(0));
        assertEquals(2L, ids.get(1));
        assertEquals(3L, ids.get(2));
    }

    @Test
    void testNullHandler() {
        assertThrows(NullPointerException.class, () -> {
            session.queryWithHandler("SELECT * FROM users", null);
        });
    }

    @Test
    void testClosedSession() {
        session.close();
        assertThrows(IllegalStateException.class, () -> {
            session.queryWithHandler("SELECT * FROM users", (rawRow, meta) -> {});
        });
    }
}