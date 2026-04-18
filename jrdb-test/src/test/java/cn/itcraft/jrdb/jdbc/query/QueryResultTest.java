package cn.itcraft.jrdb.jdbc.query;

import cn.itcraft.jrdb.jdbc.memory.MemoryBlock;
import cn.itcraft.jrdb.jdbc.memory.MemoryPool;
import cn.itcraft.jrdb.jdbc.memory.MemoryPoolConfig;
import cn.itcraft.jrdb.jdbc.storage.ColumnMeta;
import cn.itcraft.jrdb.jdbc.storage.RowStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryResultTest {

    private MemoryPool pool;
    private MemoryBlock block;
    private RowStore store;

    @BeforeEach
    void setUp() {
        MemoryPoolConfig config = MemoryPoolConfig.builder()
                .preAllocMemSize(1024 * 1024)
                .queryCacheSize(64 * 1024)
                .build();
        pool = new MemoryPool(config);
        block = pool.allocate();
        
        List<ColumnMeta> columns = Arrays.asList(
                new ColumnMeta("id", java.sql.Types.INTEGER, 4),
                new ColumnMeta("name", java.sql.Types.VARCHAR, 20)
        );
        store = new RowStore(block, columns);
        
        store.setInt(0, 100, 0);
        store.setString(0, "Alice", 1);
        store.setInt(1, 200, 0);
        store.setString(1, "Bob", 1);
        store.setInt(2, 300, 0);
        store.setString(2, "Charlie", 1);
    }

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.destroy();
        }
    }

    @Test
    void shouldGetRowCount() {
        QueryResult result = new QueryResultImpl(store, 3, block, pool);
        assertEquals(3, result.getRowCount());
        result.close();
    }

    @Test
    void shouldGetIntByColumnName() {
        QueryResult result = new QueryResultImpl(store, 3, block, pool);
        result.reset();
        assertTrue(result.next());
        assertEquals(100, result.getInt("id"));
        result.close();
    }

    @Test
    void shouldGetStringByColumnName() {
        QueryResult result = new QueryResultImpl(store, 3, block, pool);
        result.reset();
        assertTrue(result.next());
        assertEquals("Alice", result.getString("name"));
        result.close();
    }

    @Test
    void shouldGetIntByColumnIndex() {
        QueryResult result = new QueryResultImpl(store, 3, block, pool);
        result.reset();
        assertTrue(result.next());
        assertEquals(100, result.getInt(1));
        result.close();
    }

    @Test
    void shouldGetStringByColumnIndex() {
        QueryResult result = new QueryResultImpl(store, 3, block, pool);
        result.reset();
        assertTrue(result.next());
        assertEquals("Alice", result.getString(2));
        result.close();
    }

    @Test
    void shouldIterateRows() {
        QueryResult result = new QueryResultImpl(store, 3, block, pool);
        result.reset();
        
        assertTrue(result.next());
        assertEquals(100, result.getInt("id"));
        
        assertTrue(result.next());
        assertEquals(200, result.getInt("id"));
        
        assertTrue(result.next());
        assertEquals(300, result.getInt("id"));
        
        assertFalse(result.next());
        result.close();
    }

    @Test
    void shouldCloseReleaseMemory() {
        int usedBefore = pool.getUsedSlotCount();
        QueryResult result = new QueryResultImpl(store, 3, block, pool);
        assertEquals(usedBefore, pool.getUsedSlotCount());
        
        result.close();
        assertEquals(usedBefore - 1, pool.getUsedSlotCount());
    }

    @Test
    void shouldGetColumnCount() {
        QueryResult result = new QueryResultImpl(store, 3, block, pool);
        assertEquals(2, result.getColumnCount());
        result.close();
    }

    @Test
    void shouldGetColumnName() {
        QueryResult result = new QueryResultImpl(store, 3, block, pool);
        assertEquals("id", result.getColumnName(1));
        assertEquals("name", result.getColumnName(2));
        result.close();
    }
}