package cn.itcraft.jrdb.core.storage;

import cn.itcraft.jrdb.core.memory.MemoryBlock;
import cn.itcraft.jrdb.core.memory.MemoryPool;
import cn.itcraft.jrdb.core.memory.MemoryPoolConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ColumnStoreTest {

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
    void shouldStoreAndRetrieveIntColumn() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("id", Types.INTEGER, 4),
                new ColumnMeta("value", Types.INTEGER, 4)
        );
        int rowCount = 3;
        ColumnStore store = new ColumnStore(block, columns, rowCount);

        int[] ids = {1, 2, 3};
        int[] values = {10, 20, 30};
        
        store.setIntColumn(0, ids);
        store.setIntColumn(1, values);

        assertArrayEquals(ids, store.getIntColumn(0));
        assertArrayEquals(values, store.getIntColumn(1));
    }

    @Test
    void shouldAggregateSum() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("value", Types.INTEGER, 4)
        );
        int rowCount = 5;
        ColumnStore store = new ColumnStore(block, columns, rowCount);

        int[] values = {10, 20, 30, 40, 50};
        store.setIntColumn(0, values);

        assertEquals(150L, store.sumIntColumn(0));
    }

    @Test
    void shouldAggregateAvg() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("value", Types.INTEGER, 4)
        );
        int rowCount = 5;
        ColumnStore store = new ColumnStore(block, columns, rowCount);

        int[] values = {10, 20, 30, 40, 50};
        store.setIntColumn(0, values);

        assertEquals(30.0, store.avgIntColumn(0), 0.001);
    }

    @Test
    void shouldAggregateMaxMin() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("value", Types.INTEGER, 4)
        );
        int rowCount = 5;
        ColumnStore store = new ColumnStore(block, columns, rowCount);

        int[] values = {10, 50, 30, 5, 25};
        store.setIntColumn(0, values);

        assertEquals(50, store.maxIntColumn(0));
        assertEquals(5, store.minIntColumn(0));
    }

    @Test
    void shouldHandleMultipleColumns() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("id", Types.INTEGER, 4),
                new ColumnMeta("age", Types.INTEGER, 4),
                new ColumnMeta("score", Types.INTEGER, 4)
        );
        int rowCount = 3;
        ColumnStore store = new ColumnStore(block, columns, rowCount);

        int[] ids = {1, 2, 3};
        int[] ages = {25, 30, 35};
        int[] scores = {100, 90, 80};

        store.setIntColumn(0, ids);
        store.setIntColumn(1, ages);
        store.setIntColumn(2, scores);

        assertArrayEquals(ids, store.getIntColumn(0));
        assertArrayEquals(ages, store.getIntColumn(1));
        assertArrayEquals(scores, store.getIntColumn(2));

        assertEquals(6, store.sumIntColumn(0));
        assertEquals(90, store.avgIntColumn(2), 0.001);
    }

    @Test
    void shouldCalculateColumnOffsets() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("a", Types.INTEGER, 4),
                new ColumnMeta("b", Types.INTEGER, 4),
                new ColumnMeta("c", Types.INTEGER, 4)
        );
        int rowCount = 10;
        ColumnStore store = new ColumnStore(block, columns, rowCount);

        int[] offsets = store.calculateColumnOffsets();
        assertEquals(0, offsets[0]);
        assertEquals(40, offsets[1]);
        assertEquals(80, offsets[2]);
    }

    @Test
    void shouldGetRowCount() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("id", Types.INTEGER, 4)
        );
        int rowCount = 100;
        ColumnStore store = new ColumnStore(block, columns, rowCount);

        assertEquals(100, store.getRowCount());
    }

    @Test
    void shouldGetColumnCount() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("a", Types.INTEGER, 4),
                new ColumnMeta("b", Types.INTEGER, 4),
                new ColumnMeta("c", Types.INTEGER, 4)
        );
        int rowCount = 10;
        ColumnStore store = new ColumnStore(block, columns, rowCount);

        assertEquals(3, store.getColumnCount());
    }

    @Test
    void shouldThrowOnNullBlock() {
        List<ColumnMeta> columns = Arrays.asList(new ColumnMeta("id", Types.INTEGER, 4));
        assertThrows(NullPointerException.class, () -> new ColumnStore(null, columns, 10));
    }

    @Test
    void shouldThrowOnNullColumns() {
        assertThrows(NullPointerException.class, () -> new ColumnStore(block, null, 10));
    }

    @Test
    void shouldGetColumnMeta() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("id", Types.INTEGER, 4),
                new ColumnMeta("name", Types.VARCHAR, 20)
        );
        int rowCount = 5;
        ColumnStore store = new ColumnStore(block, columns, rowCount);

        ColumnMeta meta0 = store.getColumnMeta(0);
        assertEquals("id", meta0.getName());
        assertEquals(4, meta0.getSize());

        ColumnMeta meta1 = store.getColumnMeta(1);
        assertEquals("name", meta1.getName());
        assertEquals(20, meta1.getSize());
    }
}