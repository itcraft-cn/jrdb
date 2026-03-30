package com.dbmmap.core.storage;

import com.dbmmap.core.memory.MemoryBlock;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class RowStore {
    
    private final MemoryBlock block;
    private final List<ColumnMeta> columns;
    private final int rowSize;
    private final int[] columnOffsets;
    
    public RowStore(MemoryBlock block, List<ColumnMeta> columns) {
        this.block = block;
        this.columns = columns;
        this.columnOffsets = new int[columns.size()];
        int offset = 0;
        for (int i = 0; i < columns.size(); i++) {
            columnOffsets[i] = offset;
            offset += columns.get(i).getSize();
        }
        this.rowSize = offset;
    }
    
    public void setInt(int rowIndex, int value, int columnIndex) {
        int offset = getOffset(rowIndex, columnIndex);
        block.writeInt(offset, value);
    }
    
    public int getInt(int rowIndex, int columnIndex) {
        int offset = getOffset(rowIndex, columnIndex);
        return block.readInt(offset);
    }
    
    public void setLong(int rowIndex, long value, int columnIndex) {
        int offset = getOffset(rowIndex, columnIndex);
        block.writeLong(offset, value);
    }
    
    public long getLong(int rowIndex, int columnIndex) {
        int offset = getOffset(rowIndex, columnIndex);
        return block.readLong(offset);
    }
    
    public void setDouble(int rowIndex, double value, int columnIndex) {
        int offset = getOffset(rowIndex, columnIndex);
        block.writeLong(offset, Double.doubleToLongBits(value));
    }
    
    public double getDouble(int rowIndex, int columnIndex) {
        int offset = getOffset(rowIndex, columnIndex);
        return Double.longBitsToDouble(block.readLong(offset));
    }
    
    public void setString(int rowIndex, String value, int columnIndex) {
        int offset = getOffset(rowIndex, columnIndex);
        int maxSize = columns.get(columnIndex).getSize();
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(bytes.length, maxSize - 4);
        block.writeInt(offset, len);
        block.writeBytes(offset + 4, bytes);
    }
    
    public String getString(int rowIndex, int columnIndex) {
        int offset = getOffset(rowIndex, columnIndex);
        int len = block.readInt(offset);
        byte[] bytes = new byte[len];
        block.readBytes(offset + 4, bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    private int getOffset(int rowIndex, int columnIndex) {
        return rowIndex * rowSize + columnOffsets[columnIndex];
    }
    
    public int getRowSize() { return rowSize; }
    
    public int getColumnCount() { return columns.size(); }
    
    public ColumnMeta getColumnMeta(int columnIndex) {
        return columns.get(columnIndex);
    }
    
    public int getMaxRows() {
        return block.getSize() / rowSize;
    }
}