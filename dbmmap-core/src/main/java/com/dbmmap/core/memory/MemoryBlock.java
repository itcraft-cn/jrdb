package com.dbmmap.core.memory;

import sun.misc.Unsafe;

public class MemoryBlock {
    
    private final long address;
    private final int size;
    private final int slotId;
    private final Unsafe unsafe;
    
    MemoryBlock(long address, int size, int slotId, Unsafe unsafe) {
        this.address = address;
        this.size = size;
        this.slotId = slotId;
        this.unsafe = unsafe;
    }
    
    public long getAddress() { return address; }
    public int getSize() { return size; }
    public int getSlotId() { return slotId; }
    
    public byte readByte(int offset) {
        checkOffset(offset, 1);
        return unsafe.getByte(address + offset);
    }
    
    public void writeByte(int offset, byte value) {
        checkOffset(offset, 1);
        unsafe.putByte(address + offset, value);
    }
    
    public int readInt(int offset) {
        checkOffset(offset, 4);
        return unsafe.getInt(address + offset);
    }
    
    public void writeInt(int offset, int value) {
        checkOffset(offset, 4);
        unsafe.putInt(address + offset, value);
    }
    
    public long readLong(int offset) {
        checkOffset(offset, 8);
        return unsafe.getLong(address + offset);
    }
    
    public void writeLong(int offset, long value) {
        checkOffset(offset, 8);
        unsafe.putLong(address + offset, value);
    }
    
    public void readBytes(int offset, byte[] dest) {
        checkOffset(offset, dest.length);
        unsafe.copyMemory(null, address + offset, dest, Unsafe.ARRAY_BYTE_BASE_OFFSET, dest.length);
    }
    
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