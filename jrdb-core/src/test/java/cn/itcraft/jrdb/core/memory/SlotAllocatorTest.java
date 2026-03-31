package cn.itcraft.jrdb.core.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SlotAllocatorTest {

    private SlotAllocator allocator;

    @BeforeEach
    void setUp() {
        allocator = new SlotAllocator(16);
    }

    @Test
    void shouldAllocateSlot() {
        int slot = allocator.allocate();
        assertTrue(slot >= 0 && slot < 16);
    }

    @Test
    void shouldAllocateDifferentSlots() {
        int slot1 = allocator.allocate();
        int slot2 = allocator.allocate();
        assertNotEquals(slot1, slot2);
    }

    @Test
    void shouldReleaseSlot() {
        int slot = allocator.allocate();
        allocator.release(slot);
        int newSlot = allocator.allocate();
        assertEquals(slot, newSlot);
    }

    @Test
    void shouldFailWhenAllSlotsUsed() {
        for (int i = 0; i < 16; i++) {
            allocator.allocate();
        }
        assertThrows(IllegalStateException.class, () -> allocator.allocate());
    }

    @Test
    void shouldTrackUsedCount() {
        assertEquals(0, allocator.getUsedCount());
        allocator.allocate();
        assertEquals(1, allocator.getUsedCount());
        allocator.allocate();
        assertEquals(2, allocator.getUsedCount());
    }

    @Test
    void shouldCheckSlotStatus() {
        int slot = allocator.allocate();
        assertTrue(allocator.isUsed(slot));
        allocator.release(slot);
        assertFalse(allocator.isUsed(slot));
    }

    @Test
    void shouldValidateSlotIndexOnRelease() {
        assertThrows(IllegalArgumentException.class, () -> allocator.release(-1));
        assertThrows(IllegalArgumentException.class, () -> allocator.release(16));
    }

    @Test
    void shouldFailToReleaseUnusedSlot() {
        assertThrows(IllegalStateException.class, () -> allocator.release(0));
    }
}