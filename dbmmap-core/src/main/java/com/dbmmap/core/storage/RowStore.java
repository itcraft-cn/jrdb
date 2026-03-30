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
        if (block == null) {
            throw new NullPointerException("MemoryBlock cannot be null");
        }
        if (columns == null) {
            throw new NullPointerException("columns cannot be null");
        }
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
        validateBounds(rowIndex, columnIndex);
        int offset = getOffset(rowIndex, columnIndex);
        block.writeInt(offset, value);
    }
    
    public int getInt(int rowIndex, int columnIndex) {
        validateBounds(rowIndex, columnIndex);
        int offset = getOffset(rowIndex, columnIndex);
        return block.readInt(offset);
    }
    
    public void setLong(int rowIndex, long value, int columnIndex) {
        validateBounds(rowIndex, columnIndex);
        int offset = getOffset(rowIndex, columnIndex);
        block.writeLong(offset, value);
    }
    
    public long getLong(int rowIndex, int columnIndex) {
        validateBounds(rowIndex, columnIndex);
        int offset = getOffset(rowIndex, columnIndex);
        return block.readLong(offset);
    }
    
    public void setDouble(int rowIndex, double value, int columnIndex) {
        validateBounds(rowIndex, columnIndex);
        int offset = getOffset(rowIndex, columnIndex);
        block.writeLong(offset, Double.doubleToLongBits(value));
    }
    
    public double getDouble(int rowIndex, int columnIndex) {
        validateBounds(rowIndex, columnIndex);
        int offset = getOffset(rowIndex, columnIndex);
        return Double.longBitsToDouble(block.readLong(offset));
    }
    
    public void setString(int rowIndex, String value, int columnIndex) {
        validateBounds(rowIndex, columnIndex);
        int offset = getOffset(rowIndex, columnIndex);
        int maxSize = columns.get(columnIndex).getSize();
        if (value == null) {
            block.writeInt(offset, 0);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(bytes.length, maxSize - 4);
        block.writeInt(offset, len);
        block.writeBytes(offset + 4, bytes);
    }
    
    public String getString(int rowIndex, int columnIndex) {
        int offset = rowIndex * rowSize + columnOffsets[columnIndex];
        int len = block.readInt(offset);
        if (len == 0) {
            return null;
        }
        byte[] bytes = new byte[len];
        block.readBytes(offset + 4, bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    public final int getRowOffset(int rowIndex) {
        return rowIndex * rowSize;
    }
    
    public final int getColumnOffset(int columnIndex) {
        return columnOffsets[columnIndex];
    }
    
    public final int getRowSizeValue() {
        return rowSize;
    }
    
    public void setIntFast(int rowOffset, int colOffset, int value) {
        block.writeIntFast(rowOffset + colOffset, value);
    }
    
    public void setLongFast(int rowOffset, int colOffset, long value) {
        block.writeLongFast(rowOffset + colOffset, value);
    }
    
    public void setDoubleFast(int rowOffset, int colOffset, double value) {
        block.writeLongFast(rowOffset + colOffset, Double.doubleToLongBits(value));
    }
    
    public void setStringFast(int rowOffset, int colOffset, String value, int maxSize) {
        if (value == null) {
            block.writeIntFast(rowOffset + colOffset, 0);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(bytes.length, maxSize - 4);
        block.writeIntFast(rowOffset + colOffset, len);
        block.writeBytesFast(rowOffset + colOffset + 4, bytes);
    }
    
    public int getIntFast(int rowOffset, int colOffset) {
        return block.readIntFast(rowOffset + colOffset);
    }
    
    public long getLongFast(int rowOffset, int colOffset) {
        return block.readLongFast(rowOffset + colOffset);
    }
    
    public String getStringFast(int rowOffset, int colOffset) {
        int len = block.readIntFast(rowOffset + colOffset);
        if (len == 0) {
            return null;
        }
        byte[] bytes = new byte[len];
        block.readBytesFast(rowOffset + colOffset + 4, bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    private void validateBounds(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= getMaxRows()) {
            throw new IndexOutOfBoundsException("rowIndex out of bounds: " + rowIndex + ", maxRows: " + getMaxRows());
        }
        if (columnIndex < 0 || columnIndex >= columns.size()) {
            throw new IndexOutOfBoundsException("columnIndex out of bounds: " + columnIndex + ", columnCount: " + columns.size());
        }
    }
    
    private int getOffset(int rowIndex, int columnIndex) {
        return rowIndex * rowSize + columnOffsets[columnIndex];
    }
    
    public int getRowSize() { return rowSize; }
    
    public int getColumnCount() { return columns.size(); }
    
    public ColumnMeta getColumnMeta(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= columns.size()) {
            throw new IndexOutOfBoundsException("columnIndex out of bounds: " + columnIndex + ", columnCount: " + columns.size());
        }
        return columns.get(columnIndex);
    }
    
    public int getMaxRows() {
        return block.getSize() / rowSize;
    }
}