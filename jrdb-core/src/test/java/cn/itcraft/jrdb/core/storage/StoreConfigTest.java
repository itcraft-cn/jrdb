package cn.itcraft.jrdb.core.storage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StoreConfigTest {

    @Test
    void shouldCreateWithDefaults() {
        StoreConfig config = StoreConfig.builder().build();
        assertEquals(StoreType.ROW, config.getStoreType());
    }

    @Test
    void shouldCreateWithColumnStore() {
        StoreConfig config = StoreConfig.builder()
                .storeType(StoreType.COLUMN)
                .build();
        assertEquals(StoreType.COLUMN, config.getStoreType());
    }
}