package com.dbmmap.core.memory;

import sun.misc.Unsafe;

/**
 * Represents a contiguous block of off-heap memory allocated from a MemoryPool.
 * Provides thread-safe read/write operations for primitive types and byte arrays.
 */
public class MemoryBlock {
    
    private final long address;
    private final int size;
    private final int slotId;
    private final Unsafe unsafe;
    
    /**
     * Creates a new MemoryBlock instance.
     *
     * @param address the base memory address of this block
     * @param size the size of this memory block in bytes
     * @param slotId the slot identifier assigned by the pool
     * @param unsafe the Unsafe instance for memory operations
     * @throws IllegalArgumentException if unsafe is null
     */
    MemoryBlock(long address, int size, int slotId, Unsafe unsafe) {
        if (unsafe == null) {
            throw new IllegalArgumentException("Unsafe instance cannot be null");
        }
        this.address = address;
        this.size = size;
        this.slotId = slotId;
        this.unsafe = unsafe;
    }
    
    /** @return the base memory address of this block */
    public long getAddress() { return address; }
    
    /** @return the size of this memory block in bytes */
    public int getSize() { return size; }
    
    /** @return the slot identifier assigned by the pool */
    public int getSlotId() { return slotId; }
    
    /**
     * Reads a byte value from the specified offset.
     * @param offset the byte offset within this block
     * @return the byte value at the specified offset
     * @throws IndexOutOfBoundsException if offset is out of bounds
     */
    public byte readByte(int offset) {
        checkOffset(offset, 1);
        return unsafe.getByte(address + offset);
    }
    
    /**
     * Writes a byte value at the specified offset.
     * @param offset the byte offset within this block
     * @param value the byte value to write
     * @throws IndexOutOfBoundsException if offset is out of bounds
     */
    public void writeByte(int offset, byte value) {
        checkOffset(offset, 1);
        unsafe.putByte(address + offset, value);
    }
    
    /**
     * Reads an int value from the specified offset.
     * @param offset the byte offset within this block
     * @return the int value at the specified offset
     * @throws IndexOutOfBoundsException if offset or offset+4 is out of bounds
     */
    public int readInt(int offset) {
        checkOffset(offset, 4);
        return unsafe.getInt(address + offset);
    }
    
    /**
     * Writes an int value at the specified offset.
     * @param offset the byte offset within this block
     * @param value the int value to write
     * @throws IndexOutOfBoundsException if offset or offset+4 is out of bounds
     */
    public void writeInt(int offset, int value) {
        checkOffset(offset, 4);
        unsafe.putInt(address + offset, value);
    }
    
    /**
     * Reads a long value from the specified offset.
     * @param offset the byte offset within this block
     * @return the long value at the specified offset
     * @throws IndexOutOfBoundsException if offset or offset+8 is out of bounds
     */
    public long readLong(int offset) {
        checkOffset(offset, 8);
        return unsafe.getLong(address + offset);
    }
    
    /**
     * Writes a long value at the specified offset.
     * @param offset the byte offset within this block
     * @param value the long value to write
     * @throws IndexOutOfBoundsException if offset or offset+8 is out of bounds
     */
    public void writeLong(int offset, long value) {
        checkOffset(offset, 8);
        unsafe.putLong(address + offset, value);
    }
    
    /**
     * Reads bytes into the destination array from the specified offset.
     * @param offset the byte offset within this block
     * @param dest the destination byte array
     * @throws IndexOutOfBoundsException if offset+dest.length exceeds block size
     */
    public void readBytes(int offset, byte[] dest) {
        checkOffset(offset, dest.length);
        unsafe.copyMemory(null, address + offset, dest, Unsafe.ARRAY_BYTE_BASE_OFFSET, dest.length);
    }
    
    /**
     * Writes bytes from the source array at the specified offset.
     * @param offset the byte offset within this block
     * @param src the source byte array
     * @throws IndexOutOfBoundsException if offset+src.length exceeds block size
     */
    public void writeBytes(int offset, byte[] src) {
        checkOffset(offset, src.length);
        unsafe.copyMemory(src, Unsafe.ARRAY_BYTE_BASE_OFFSET, null, address + offset, src.length);
    }
    
    private void checkOffset(int offset, int length) {
        if (offset < 0 || offset + length > size) {
            throw new IndexOutOfBoundsException(
                "offset=" + offset + ", length=" + length + ", size=" + size);
        }
    }
}