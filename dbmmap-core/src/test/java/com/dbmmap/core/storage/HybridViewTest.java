package com.dbmmap.core.storage;

import com.dbmmap.core.memory.MemoryBlock;
import com.dbmmap.core.memory.MemoryPool;
import com.dbmmap.core.memory.MemoryPoolConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HybridViewTest {

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
    void shouldStartInRowMode() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("id", Types.INTEGER, 4),
                new ColumnMeta("value", Types.INTEGER, 4)
        );
        int rowCount = 3;
        HybridView view = new HybridView(block, columns, rowCount);

        assertFalse(view.isColumnMode());
    }

    @Test
    void shouldSwitchToColumnMode() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("id", Types.INTEGER, 4),
                new ColumnMeta("value", Types.INTEGER, 4)
        );
        int rowCount = 3;
        HybridView view = new HybridView(block, columns, rowCount);

        ColumnStore columnStore = view.toColumnView();

        assertNotNull(columnStore);
        assertTrue(view.isColumnMode());
        assertEquals(2, columnStore.getColumnCount());
        assertEquals(3, columnStore.getRowCount());
    }

    @Test
    void shouldReturnRowStore() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("id", Types.INTEGER, 4),
                new ColumnMeta("value", Types.INTEGER, 4)
        );
        int rowCount = 3;
        HybridView view = new HybridView(block, columns, rowCount);

        RowStore rowStore = view.toRowView();

        assertNotNull(rowStore);
        assertFalse(view.isColumnMode());
        assertEquals(2, rowStore.getColumnCount());
    }

    @Test
    void shouldPersistDataAcrossModeSwitch() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("id", Types.INTEGER, 4),
                new ColumnMeta("value", Types.INTEGER, 4)
        );
        int rowCount = 3;
        HybridView view = new HybridView(block, columns, rowCount);

        RowStore rowStore = view.toRowView();
        rowStore.setInt(0, 1, 0);
        rowStore.setInt(0, 10, 1);
        rowStore.setInt(1, 2, 0);
        rowStore.setInt(1, 20, 1);
        rowStore.setInt(2, 3, 0);
        rowStore.setInt(2, 30, 1);

        ColumnStore columnStore = view.toColumnView();
        assertArrayEquals(new int[]{1, 2, 3}, columnStore.getIntColumn(0));
        assertArrayEquals(new int[]{10, 20, 30}, columnStore.getIntColumn(1));
    }

    @Test
    void shouldThrowOnNullBlock() {
        List<ColumnMeta> columns = Arrays.asList(new ColumnMeta("id", Types.INTEGER, 4));
        assertThrows(NullPointerException.class, () -> new HybridView(null, columns, 10));
    }

    @Test
    void shouldThrowOnNullColumns() {
        assertThrows(NullPointerException.class, () -> new HybridView(block, null, 10));
    }

    @Test
    void shouldGetColumnCount() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("a", Types.INTEGER, 4),
                new ColumnMeta("b", Types.INTEGER, 4)
        );
        HybridView view = new HybridView(block, columns, 10);

        assertEquals(2, view.getColumnCount());
    }

    @Test
    void shouldGetRowCount() {
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("a", Types.INTEGER, 4)
        );
        HybridView view = new HybridView(block, columns, 50);

        assertEquals(50, view.getRowCount());
    }
}