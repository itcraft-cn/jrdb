package cn.itcraft.jrdb.core.memory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MemoryPoolConfigTest {

    @Test
    void shouldCreateWithDefaults() {
        MemoryPoolConfig config = MemoryPoolConfig.builder().build();
        assertEquals(256L * 1024 * 1024, config.getPreAllocMemSize());
        assertEquals(16L * 1024 * 1024, config.getQueryCacheSize());
    }

    @Test
    void shouldCreateWithCustomValues() {
        MemoryPoolConfig config = MemoryPoolConfig.builder()
                .preAllocMemSize(512L * 1024 * 1024)
                .queryCacheSize(32L * 1024 * 1024)
                .build();
        assertEquals(512L * 1024 * 1024, config.getPreAllocMemSize());
        assertEquals(32L * 1024 * 1024, config.getQueryCacheSize());
    }

    @Test
    void shouldCalculateSlotCount() {
        MemoryPoolConfig config = MemoryPoolConfig.builder()
                .preAllocMemSize(256L * 1024 * 1024)
                .queryCacheSize(16L * 1024 * 1024)
                .build();
        assertEquals(16, config.getSlotCount());
    }

    @Test
    void shouldValidatePreAllocMemSize() {
        assertThrows(IllegalArgumentException.class, () ->
            MemoryPoolConfig.builder().preAllocMemSize(-1).build()
        );
    }

    @Test
    void shouldValidateQueryCacheSize() {
        assertThrows(IllegalArgumentException.class, () ->
            MemoryPoolConfig.builder().queryCacheSize(0).build()
        );
    }

    @Test
    void shouldValidateQueryCacheSizeNotExceedPreAlloc() {
        assertThrows(IllegalArgumentException.class, () ->
            MemoryPoolConfig.builder()
                .preAllocMemSize(16L * 1024 * 1024)
                .queryCacheSize(32L * 1024 * 1024)
                .build()
        );
    }
}