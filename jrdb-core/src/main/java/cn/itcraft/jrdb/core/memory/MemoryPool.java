package cn.itcraft.jrdb.core.memory;

import sun.misc.Unsafe;

/**
 * Manages a pool of fixed-size memory blocks allocated off-heap.
 * Provides thread-safe allocation and release of memory blocks.
 */
public class MemoryPool {
    
    private final long baseAddress;
    private final int slotSize;
    private final int slotCount;
    private final SlotAllocator slotAllocator;
    private final Unsafe unsafe;
    private volatile boolean destroyed;
    
    /**
     * Creates a new MemoryPool with the specified configuration.
     * @param config the pool configuration
     * @throws NullPointerException if config is null
     */
    public MemoryPool(MemoryPoolConfig config) {
        this.slotSize = (int) config.getQueryCacheSize();
        this.slotCount = config.getSlotCount();
        this.slotAllocator = new SlotAllocator(slotCount);
        this.unsafe = UnsafeProvider.getUnsafe();
        this.baseAddress = unsafe.allocateMemory(config.getPreAllocMemSize());
        this.destroyed = false;
    }
    
    /**
     * Allocates a memory block from the pool.
     * @return a new MemoryBlock instance
     * @throws IllegalStateException if pool is destroyed or no slots available
     */
    public synchronized MemoryBlock allocate() {
        if (destroyed) {
            throw new IllegalStateException("MemoryPool has been destroyed");
        }
        int slotId = slotAllocator.allocate();
        long address = baseAddress + (long) slotId * slotSize;
        return new MemoryBlock(address, slotSize, slotId, unsafe);
    }
    
    /**
     * Releases a memory block back to the pool.
     * @param block the block to release (null-safe)
     */
    public synchronized void release(MemoryBlock block) {
        if (destroyed || block == null) {
            return;
        }
        slotAllocator.release(block.getSlotId());
    }
    
    /**
     * Destroys this pool and frees all off-heap memory.
     * After calling this method, the pool cannot be used.
     */
    public synchronized void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        unsafe.freeMemory(baseAddress);
    }
    
    /** @return the number of currently used slots */
    public synchronized int getUsedSlotCount() { 
        return slotAllocator.getUsedCount(); 
    }
    
    /** @return the total number of slots in this pool */
    public int getTotalSlotCount() { 
        return slotCount; 
    }
    
    /** @return the total memory size in bytes */
    public long getTotalMemory() { 
        return (long) slotCount * slotSize; 
    }
    
    /** @return the size of each slot in bytes */
    public int getSlotSize() { 
        return slotSize; 
    }
    
    /** @return true if this pool has been destroyed */
    public boolean isDestroyed() { 
        return destroyed; 
    }
}