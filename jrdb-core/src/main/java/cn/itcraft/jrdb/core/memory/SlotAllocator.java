package cn.itcraft.jrdb.core.memory;

/**
 * 槽位分配器
 * 
 * <p>使用位图管理槽位的分配与释放，支持高效的槽位查找和回收操作。
 * 槽位状态通过 long 数组存储，每个 bit 对应一个槽位的使用状态。
 * 
 * <p>该类是线程安全的，所有公共方法均使用 synchronized 保证并发访问安全。
 */
public class SlotAllocator {
    
    /**
     * 位图数组，用于记录槽位的使用状态
     * 
     * <p>每个 long 类型有 64 位，可表示 64 个槽位的状态。
     * 位为 1 表示槽位已使用，为 0 表示槽位空闲。
     */
    private final long[] slots;
    
    /**
     * 槽位总数
     */
    private final int slotCount;
    
    /**
     * 已使用的槽位数量
     */
    private int usedCount;
    
    /**
     * 下一个可能空闲的槽位索引
     * 
     * <p>用于优化分配时的搜索起点，避免每次都从头开始搜索。
     */
    private int nextFreeSlot;

    /**
     * 构造槽位分配器
     * 
     * @param slotCount 槽位总数，必须大于 0
     * @throws IllegalArgumentException 如果 slotCount 小于等于 0
     */
    public SlotAllocator(int slotCount) {
        if (slotCount <= 0) {
            throw new IllegalArgumentException("slotCount must be positive");
        }
        this.slotCount = slotCount;
        this.slots = new long[(slotCount + 63) / 64];
        this.usedCount = 0;
        this.nextFreeSlot = 0;
    }

    /**
     * 分配一个空闲槽位
     * 
     * <p>从 nextFreeSlot 开始搜索，找到第一个空闲槽位并标记为已使用。
     * 如果所有槽位都已使用，则抛出异常。
     * 
     * @return 分配的槽位索引
     * @throws IllegalStateException 如果所有槽位都已使用
     */
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

    /**
     * 释放指定槽位
     * 
     * <p>将指定槽位标记为空闲状态，并更新下一个空闲槽位索引。
     * 如果槽位未被使用，则抛出异常。
     * 
     * @param slot 要释放的槽位索引
     * @throws IllegalArgumentException 如果槽位索引超出范围
     * @throws IllegalStateException 如果槽位未被使用
     */
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

    /**
     * 检查指定槽位是否已使用
     * 
     * @param slot 槽位索引
     * @return 如果槽位已使用返回 true，否则返回 false
     * @throws IllegalArgumentException 如果槽位索引超出范围
     */
    public synchronized boolean isUsed(int slot) {
        validateSlotIndex(slot);
        return (slots[slot / 64] & (1L << (slot % 64))) != 0;
    }

    /**
     * 获取已使用的槽位数量
     * 
     * @return 已使用的槽位数量
     */
    public synchronized int getUsedCount() {
        return usedCount;
    }

    /**
     * 获取槽位总数
     * 
     * @return 槽位总数
     */
    public int getSlotCount() {
        return slotCount;
    }

    /**
     * 标记指定槽位为已使用
     * 
     * @param slot 槽位索引
     */
    private void markUsed(int slot) {
        slots[slot / 64] |= (1L << (slot % 64));
    }

    /**
     * 标记指定槽位为未使用
     * 
     * @param slot 槽位索引
     */
    private void markUnused(int slot) {
        slots[slot / 64] &= ~(1L << (slot % 64));
    }

    /**
     * 验证槽位索引是否有效
     * 
     * @param slot 槽位索引
     * @throws IllegalArgumentException 如果槽位索引超出范围
     */
    private void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= slotCount) {
            throw new IllegalArgumentException("Slot index out of range: " + slot);
        }
    }
}