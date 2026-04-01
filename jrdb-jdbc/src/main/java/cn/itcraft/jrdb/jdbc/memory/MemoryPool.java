package cn.itcraft.jrdb.jdbc.memory;

import sun.misc.Unsafe;

/**
 * 堆外内存池管理器。
 * 
 * <p>该类负责管理固定大小的堆外内存块池，提供线程安全的内存块分配和释放功能。
 * 内存池在初始化时预分配所有内存，运行时仅进行槽位管理，避免频繁的内存分配释放操作。
 * 
 * <p>主要特点：
 * <ul>
 *   <li>基于 Unsafe 的堆外内存分配，避免 GC 压力</li>
 *   <li>固定大小的内存块（槽位），便于管理和复用</li>
 *   <li>线程安全的分配和释放操作</li>
 *   <li>支持销毁池并释放所有堆外内存</li>
 * </ul>
 * 
 * <p>适用场景：读多写少的高性能数据库查询缓存
 * 
 * @see MemoryBlock
 * @see MemoryPoolConfig
 * @see SlotAllocator
 */
public class MemoryPool {
    
    /**
     * 内存池基地址。
     * 指向堆外内存块的起始位置，所有槽位地址均基于此地址计算。
     */
    private final long baseAddress;
    
    /**
     * 每个槽位的大小（字节）。
     * 所有槽位大小相同，由配置决定。
     */
    private final int slotSize;
    
    /**
     * 槽位总数。
     * 内存池包含的固定大小槽位数量。
     */
    private final int slotCount;
    
    /**
     * 槽位分配器。
     * 负责管理槽位的分配和回收，跟踪已使用和空闲槽位。
     */
    private final SlotAllocator slotAllocator;
    
    /**
     * Unsafe 实例。
     * 用于直接操作堆外内存。
     */
    private final Unsafe unsafe;
    
    /**
     * 销毁标志。
     * 标识内存池是否已被销毁，volatile 保证线程可见性。
     */
    private volatile boolean destroyed;
    
    /**
     * 使用指定配置创建内存池。
     * 
     * <p>创建过程：
     * <ol>
     *   <li>从配置中读取槽位大小和数量</li>
     *   <li>初始化槽位分配器</li>
     *   <li>预分配堆外内存</li>
     * </ol>
     * 
     * @param config 内存池配置，包含槽位大小、数量等信息
     * @throws NullPointerException 如果配置为 null
     * @throws OutOfMemoryError 如果无法分配足够的堆外内存
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
     * 从内存池分配一个内存块。
     * 
     * <p>分配过程：
     * <ol>
     *   <li>检查内存池是否已销毁</li>
     *   <li>从分配器获取一个空闲槽位</li>
     *   <li>计算槽位对应的内存地址</li>
     *   <li>创建并返回 MemoryBlock 实例</li>
     * </ol>
     * 
     * @return 新分配的内存块实例
     * @throws IllegalStateException 如果内存池已销毁或没有可用槽位
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
     * 释放内存块回内存池。
     * 
     * <p>释放过程：
     * <ol>
     *   <li>检查内存池状态和块是否为 null</li>
     *   <li>将槽位标记为可用</li>
     * </ol>
     * 
     * <p>注意：释放后内存块不应再被使用，但不会自动清零
     * 
     * @param block 要释放的内存块，可以为 null（空安全）
     */
    public synchronized void release(MemoryBlock block) {
        if (destroyed || block == null) {
            return;
        }
        
        slotAllocator.release(block.getSlotId());
    }
    
    /**
     * 销毁内存池并释放所有堆外内存。
     * 
     * <p>销毁过程：
     * <ol>
     *   <li>设置销毁标志，防止后续操作</li>
     *   <li>释放所有堆外内存</li>
     * </ol>
     * 
     * <p>警告：销毁后内存池不可再使用，所有已分配的内存块将失效
     */
    public synchronized void destroy() {
        if (destroyed) {
            return;
        }
        
        destroyed = true;
        
        unsafe.freeMemory(baseAddress);
    }
    
    /**
     * 获取当前已使用的槽位数量。
     * 
     * @return 已使用的槽位数量
     */
    public synchronized int getUsedSlotCount() { 
        return slotAllocator.getUsedCount(); 
    }
    
    /**
     * 获取内存池的槽位总数。
     * 
     * @return 槽位总数
     */
    public int getTotalSlotCount() { 
        return slotCount; 
    }
    
    /**
     * 获取内存池的总内存大小（字节）。
     * 
     * @return 总内存大小（字节）
     */
    public long getTotalMemory() { 
        return (long) slotCount * slotSize; 
    }
    
    /**
     * 获取每个槽位的大小（字节）。
     * 
     * @return 槽位大小（字节）
     */
    public int getSlotSize() { 
        return slotSize; 
    }
    
    /**
     * 检查内存池是否已被销毁。
     * 
     * @return 如果已销毁返回 true，否则返回 false
     */
    public boolean isDestroyed() { 
        return destroyed; 
    }
}