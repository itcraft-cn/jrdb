package com.dbmmap.core.memory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MemoryPoolTest {

    private MemoryPool pool;

    @BeforeEach
    void setUp() {
        MemoryPoolConfig config = MemoryPoolConfig.builder()
                .preAllocMemSize(1024)
                .queryCacheSize(256)
                .build();
        pool = new MemoryPool(config);
    }

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.destroy();
        }
    }

    @Test
    void shouldAllocateMemoryBlock() {
        MemoryBlock block = pool.allocate();
        assertNotNull(block);
        assertTrue(block.getAddress() > 0);
        assertEquals(256, block.getSize());
        assertEquals(0, block.getSlotId());
    }

    @Test
    void shouldAllocateDifferentSlots() {
        MemoryBlock block1 = pool.allocate();
        MemoryBlock block2 = pool.allocate();
        assertNotEquals(block1.getSlotId(), block2.getSlotId());
        assertNotEquals(block1.getAddress(), block2.getAddress());
    }

    @Test
    void shouldReleaseBlock() {
        MemoryBlock block = pool.allocate();
        int slotId = block.getSlotId();
        pool.release(block);
        
        MemoryBlock newBlock = pool.allocate();
        assertEquals(slotId, newBlock.getSlotId());
    }

    @Test
    void shouldFailWhenAllSlotsUsed() {
        for (int i = 0; i < 4; i++) {
            pool.allocate();
        }
        assertThrows(IllegalStateException.class, () -> pool.allocate());
    }

    @Test
    void shouldTrackStatistics() {
        assertEquals(0, pool.getUsedSlotCount());
        assertEquals(4, pool.getTotalSlotCount());
        assertEquals(1024, pool.getTotalMemory());
        
        pool.allocate();
        assertEquals(1, pool.getUsedSlotCount());
    }

    @Test
    void shouldWriteAndReadByte() {
        MemoryBlock block = pool.allocate();
        block.writeByte(0, (byte) 42);
        assertEquals(42, block.readByte(0));
    }

    @Test
    void shouldWriteAndReadInt() {
        MemoryBlock block = pool.allocate();
        block.writeInt(0, 12345678);
        assertEquals(12345678, block.readInt(0));
    }

    @Test
    void shouldWriteAndReadLong() {
        MemoryBlock block = pool.allocate();
        block.writeLong(0, 9876543210L);
        assertEquals(9876543210L, block.readLong(0));
    }

    @Test
    void shouldWriteAndReadBytes() {
        MemoryBlock block = pool.allocate();
        byte[] data = {1, 2, 3, 4, 5};
        block.writeBytes(0, data);
        
        byte[] read = new byte[5];
        block.readBytes(0, read);
        assertArrayEquals(data, read);
    }

    @Test
    void shouldCheckIndexBounds() {
        MemoryBlock block = pool.allocate();
        assertThrows(IndexOutOfBoundsException.class, () -> block.readByte(256));
        assertThrows(IndexOutOfBoundsException.class, () -> block.writeInt(254, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> block.readByte(-1));
    }
}