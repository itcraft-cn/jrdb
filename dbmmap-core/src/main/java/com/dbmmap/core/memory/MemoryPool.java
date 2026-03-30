package com.dbmmap.core.memory;

import sun.misc.Unsafe;

public class MemoryPool {
    
    private final long baseAddress;
    private final int slotSize;
    private final int slotCount;
    private final SlotAllocator slotAllocator;
    private final Unsafe unsafe;
    private volatile boolean destroyed;
    
    public MemoryPool(MemoryPoolConfig config) {
        this.slotSize = (int) config.getQueryCacheSize();
        this.slotCount = config.getSlotCount();
        this.slotAllocator = new SlotAllocator(slotCount);
        this.unsafe = UnsafeProvider.getUnsafe();
        this.baseAddress = unsafe.allocateMemory(config.getPreAllocMemSize());
        this.destroyed = false;
    }
    
    public synchronized MemoryBlock allocate() {
        if (destroyed) {
            throw new IllegalStateException("MemoryPool has been destroyed");
        }
        int slotId = slotAllocator.allocate();
        long address = baseAddress + (long) slotId * slotSize;
        return new MemoryBlock(address, slotSize, slotId, unsafe);
    }
    
    public synchronized void release(MemoryBlock block) {
        if (destroyed || block == null) {
            return;
        }
        slotAllocator.release(block.getSlotId());
    }
    
    public synchronized void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        unsafe.freeMemory(baseAddress);
    }
    
    public int getUsedSlotCount() { 
        return slotAllocator.getUsedCount(); 
    }
    
    public int getTotalSlotCount() { 
        return slotCount; 
    }
    
    public long getTotalMemory() { 
        return (long) slotCount * slotSize; 
    }
    
    public int getSlotSize() { 
        return slotSize; 
    }
    
    public boolean isDestroyed() { 
        return destroyed; 
    }
}