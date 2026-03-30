package com.dbmmap.core.memory;

public class SlotAllocator {
    private final long[] slots;
    private final int slotCount;
    private int usedCount;
    private int nextFreeSlot;

    public SlotAllocator(int slotCount) {
        if (slotCount <= 0) {
            throw new IllegalArgumentException("slotCount must be positive");
        }
        this.slotCount = slotCount;
        this.slots = new long[(slotCount + 63) / 64];
        this.usedCount = 0;
        this.nextFreeSlot = 0;
    }

    public synchronized int allocate() {
        if (usedCount >= slotCount) {
            throw new IllegalStateException("All slots are used");
        }

        int startSlot = nextFreeSlot;
        for (int i = 0; i < slotCount; i++) {
            int slot = (startSlot + i) % slotCount;
            if (!isUsed(slot)) {
                markUsed(slot);
                usedCount++;
                nextFreeSlot = (slot + 1) % slotCount;
                return slot;
            }
        }

        throw new IllegalStateException("No available slot found");
    }

    public synchronized void release(int slot) {
        validateSlotIndex(slot);
        if (!isUsed(slot)) {
            throw new IllegalStateException("Slot " + slot + " is not used");
        }
        markUnused(slot);
        usedCount--;
        if (slot < nextFreeSlot) {
            nextFreeSlot = slot;
        }
    }

    public synchronized boolean isUsed(int slot) {
        validateSlotIndex(slot);
        return (slots[slot / 64] & (1L << (slot % 64))) != 0;
    }

    public synchronized int getUsedCount() {
        return usedCount;
    }

    public int getSlotCount() {
        return slotCount;
    }

    private void markUsed(int slot) {
        slots[slot / 64] |= (1L << (slot % 64));
    }

    private void markUnused(int slot) {
        slots[slot / 64] &= ~(1L << (slot % 64));
    }

    private void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= slotCount) {
            throw new IllegalArgumentException("Slot index out of range: " + slot);
        }
    }
}