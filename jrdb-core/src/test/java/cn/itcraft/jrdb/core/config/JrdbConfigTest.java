package cn.itcraft.jrdb.core.config;

import cn.itcraft.jrdb.core.memory.MemoryPoolConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JrdbConfigTest {

    @Test
    void shouldCreateWithDefaults() {
        JrdbConfig config = JrdbConfig.builder().build();
        assertNotNull(config.getMemoryPoolConfig());
        assertEquals(OverflowStrategy.ERROR, config.getOverflowStrategy());
    }

    @Test
    void shouldCreateWithCustomValues() {
        MemoryPoolConfig poolConfig = MemoryPoolConfig.builder()
                .preAllocMemSize(512L * 1024 * 1024)
                .queryCacheSize(32L * 1024 * 1024)
                .build();
        
        JrdbConfig config = JrdbConfig.builder()
                .memoryPoolConfig(poolConfig)
                .overflowStrategy(OverflowStrategy.TRUNCATE)
                .build();
        
        assertEquals(poolConfig, config.getMemoryPoolConfig());
        assertEquals(OverflowStrategy.TRUNCATE, config.getOverflowStrategy());
    }

    @Test
    void shouldValidateMemoryPoolConfig() {
        assertThrows(NullPointerException.class, () ->
            JrdbConfig.builder().memoryPoolConfig(null).build()
        );
    }
}