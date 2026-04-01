package cn.itcraft.jrdb.jdbc.storage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StoreTypeTest {

    @Test
    void shouldHaveAllStoreTypes() {
        StoreType[] values = StoreType.values();
        assertEquals(3, values.length);
        assertEquals(StoreType.ROW, StoreType.valueOf("ROW"));
        assertEquals(StoreType.COLUMN, StoreType.valueOf("COLUMN"));
        assertEquals(StoreType.HYBRID, StoreType.valueOf("HYBRID"));
    }
}