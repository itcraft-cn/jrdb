package cn.itcraft.dbmmap.core.config;

import cn.itcraft.dbmmap.core.memory.MemoryPoolConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DbMmapConfigTest {

    @Test
    void shouldCreateWithDefaults() {
        DbMmapConfig config = DbMmapConfig.builder().build();
        assertNotNull(config.getMemoryPoolConfig());
        assertEquals(OverflowStrategy.ERROR, config.getOverflowStrategy());
    }

    @Test
    void shouldCreateWithCustomValues() {
        MemoryPoolConfig poolConfig = MemoryPoolConfig.builder()
                .preAllocMemSize(512L * 1024 * 1024)
                .queryCacheSize(32L * 1024 * 1024)
                .build();
        
        DbMmapConfig config = DbMmapConfig.builder()
                .memoryPoolConfig(poolConfig)
                .overflowStrategy(OverflowStrategy.TRUNCATE)
                .build();
        
        assertEquals(poolConfig, config.getMemoryPoolConfig());
        assertEquals(OverflowStrategy.TRUNCATE, config.getOverflowStrategy());
    }

    @Test
    void shouldValidateMemoryPoolConfig() {
        assertThrows(NullPointerException.class, () ->
            DbMmapConfig.builder().memoryPoolConfig(null).build()
        );
    }
}