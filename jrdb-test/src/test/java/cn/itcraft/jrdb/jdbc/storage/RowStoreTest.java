package cn.itcraft.jrdb.jdbc.storage;

import cn.itcraft.jrdb.jdbc.memory.MemoryBlock;
import cn.itcraft.jrdb.jdbc.memory.MemoryPool;
import cn.itcraft.jrdb.jdbc.memory.MemoryPoolConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RowStoreTest {

    private MemoryPool pool;
    private MemoryBlock block;

    @BeforeEach
    void setUp() {
        MemoryPoolConfig config = MemoryPoolConfig.builder()
                .preAllocMemSize(1024 * 1024)
                .queryCacheSize(64 * 1024)
                .build();
        pool = new MemoryPool(config);
        block = pool.allocate();
    }

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.destroy();
        }
    }

    @Test
    void shouldWriteAndReadRow() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("id", java.sql.Types.INTEGER, 4),
                new ColumnMeta("name", java.sql.Types.VARCHAR, 20)
        );
        RowStore store = new RowStore(block, columns);

        store.setInt(0, 123, 0);
        store.setString(0, "test", 1);

        assertEquals(123, store.getInt(0, 0));
        assertEquals("test", store.getString(0, 1));
    }

    @Test
    void shouldWriteMultipleRows() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("id", java.sql.Types.INTEGER, 4)
        );
        RowStore store = new RowStore(block, columns);

        store.setInt(0, 100, 0);
        store.setInt(1, 200, 0);
        store.setInt(2, 300, 0);

        assertEquals(100, store.getInt(0, 0));
        assertEquals(200, store.getInt(1, 0));
        assertEquals(300, store.getInt(2, 0));
    }

    @Test
    void shouldCalculateRowSize() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("id", java.sql.Types.INTEGER, 4),
                new ColumnMeta("name", java.sql.Types.VARCHAR, 20),
                new ColumnMeta("value", java.sql.Types.BIGINT, 8)
        );
        RowStore store = new RowStore(block, columns);

        assertEquals(32, store.getRowSize());
    }

    @Test
    void shouldGetColumnCount() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("id", java.sql.Types.INTEGER, 4),
                new ColumnMeta("name", java.sql.Types.VARCHAR, 20)
        );
        RowStore store = new RowStore(block, columns);

        assertEquals(2, store.getColumnCount());
    }

    @Test
    void shouldGetColumnMeta() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("id", java.sql.Types.INTEGER, 4),
                new ColumnMeta("name", java.sql.Types.VARCHAR, 20)
        );
        RowStore store = new RowStore(block, columns);

        ColumnMeta meta0 = store.getColumnMeta(0);
        assertEquals("id", meta0.getName());
        assertEquals(4, meta0.getSize());

        ColumnMeta meta1 = store.getColumnMeta(1);
        assertEquals("name", meta1.getName());
        assertEquals(20, meta1.getSize());
    }

    @Test
    void shouldHandleLongAndDouble() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("id", java.sql.Types.BIGINT, 8),
                new ColumnMeta("value", java.sql.Types.DOUBLE, 8)
        );
        RowStore store = new RowStore(block, columns);

        store.setLong(0, 123456789L, 0);
        store.setDouble(0, 3.14159, 1);

        assertEquals(123456789L, store.getLong(0, 0));
        assertEquals(3.14159, store.getDouble(0, 1), 0.00001);
    }

    @Test
    void shouldGetMaxRows() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("id", java.sql.Types.INTEGER, 4)
        );
        RowStore store = new RowStore(block, columns);

        assertTrue(store.getMaxRows() > 0);
        assertEquals(block.getSize() / 4, store.getMaxRows());
    }
}