package cn.itcraft.jrdb.jdbc.storage;

import cn.itcraft.jrdb.jdbc.memory.MemoryBlock;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 行式存储器，用于管理堆外内存中的行式数据存储。
 * 
 * <p>该类提供基于行的方式访问堆外内存中的数据，支持多种数据类型的读写操作。
 * 每行数据按照列的定义顺序紧凑存储，支持安全访问和快速访问两种模式。</p>
 * 
 * <p>安全访问方法（如 {@link #setInt}、{@link #getInt} 等）会进行边界检查，
 * 确保访问的行索引和列索引在有效范围内。快速访问方法（如 {@link #setIntFast}、
 * {@link #getIntFast} 等）不进行边界检查，适用于性能敏感场景，需要调用方
 * 预先计算偏移量。</p>
 * 
 * @author jrdb
 * @see MemoryBlock
 * @see ColumnMeta
 */
public class RowStore {

    /**
     * 底层内存块，用于实际的堆外内存读写操作。
     */
    private final MemoryBlock block;

    /**
     * 列元数据列表，定义了每列的类型、大小等信息。
     */
    private final List<ColumnMeta> columns;

    /**
     * 单行数据的字节大小。
     */
    private final int rowSize;

    /**
     * 各列在行内的字节偏移量数组。
     * 数组索引对应列索引，值为该列起始位置相对于行首的偏移量。
     */
    private final int[] columnOffsets;

    /**
     * 构造行式存储器实例。
     * 
     * <p>构造时会根据列元数据计算每列的偏移量和单行数据的总大小。</p>
     * 
     * @param block 底层内存块，用于存储实际数据，不能为 null
     * @param columns 列元数据列表，定义了表结构，不能为 null
     * @throws NullPointerException 如果 block 或 columns 为 null
     */
    public RowStore(MemoryBlock block, List<ColumnMeta> columns) {
        if (block == null) {
            throw new NullPointerException("MemoryBlock cannot be null");
        }
        if (columns == null) {
            throw new NullPointerException("columns cannot be null");
        }
        this.block = block;
        this.columns = columns;
        int size = columns.size();
        this.columnOffsets = new int[size];
        int offset = 0;
        for (int i = 0; i < size; i++) {
            columnOffsets[i] = offset;
            offset += columns.get(i).getSize();
        }
        this.rowSize = offset;
    }

    /**
     * 设置指定行和列位置的整数值。
     * 
     * <p>该方法会进行边界检查，确保行索引和列索引在有效范围内。</p>
     * 
     * @param rowIndex 行索引，从 0 开始
     * @param value 要设置的整数值
     * @param columnIndex 列索引，从 0 开始
     * @throws IndexOutOfBoundsException 如果行索引或列索引超出边界
     */
    public void setInt(int rowIndex, int value, int columnIndex) {
        validateBounds(rowIndex, columnIndex);
        int offset = getOffset(rowIndex, columnIndex);
        block.writeInt(offset, value);
    }

    /**
     * 获取指定行和列位置的整数值。
     * 
     * <p>该方法会进行边界检查，确保行索引和列索引在有效范围内。</p>
     * 
     * @param rowIndex 行索引，从 0 开始
     * @param columnIndex 列索引，从 0 开始
     * @return 该位置的整数值
     * @throws IndexOutOfBoundsException 如果行索引或列索引超出边界
     */
    public int getInt(int rowIndex, int columnIndex) {
        validateBounds(rowIndex, columnIndex);
        int offset = getOffset(rowIndex, columnIndex);
        return block.readInt(offset);
    }

    /**
     * 设置指定行和列位置的长整数值。
     * 
     * <p>该方法会进行边界检查，确保行索引和列索引在有效范围内。</p>
     * 
     * @param rowIndex 行索引，从 0 开始
     * @param value 要设置的长整数值
     * @param columnIndex 列索引，从 0 开始
     * @throws IndexOutOfBoundsException 如果行索引或列索引超出边界
     */
    public void setLong(int rowIndex, long value, int columnIndex) {
        validateBounds(rowIndex, columnIndex);
        int offset = getOffset(rowIndex, columnIndex);
        block.writeLong(offset, value);
    }

    /**
     * 获取指定行和列位置的长整数值。
     * 
     * <p>该方法会进行边界检查，确保行索引和列索引在有效范围内。</p>
     * 
     * @param rowIndex 行索引，从 0 开始
     * @param columnIndex 列索引，从 0 开始
     * @return 该位置的长整数值
     * @throws IndexOutOfBoundsException 如果行索引或列索引超出边界
     */
    public long getLong(int rowIndex, int columnIndex) {
        validateBounds(rowIndex, columnIndex);
        int offset = getOffset(rowIndex, columnIndex);
        return block.readLong(offset);
    }

    /**
     * 设置指定行和列位置的双精度浮点数值。
     * 
     * <p>该方法会将双精度浮点数转换为长整数的位表示形式进行存储。
     * 会进行边界检查，确保行索引和列索引在有效范围内。</p>
     * 
     * @param rowIndex 行索引，从 0 开始
     * @param value 要设置的双精度浮点数值
     * @param columnIndex 列索引，从 0 开始
     * @throws IndexOutOfBoundsException 如果行索引或列索引超出边界
     */
    public void setDouble(int rowIndex, double value, int columnIndex) {
        validateBounds(rowIndex, columnIndex);
        int offset = getOffset(rowIndex, columnIndex);
        block.writeLong(offset, Double.doubleToLongBits(value));
    }

    /**
     * 获取指定行和列位置的双精度浮点数值。
     * 
     * <p>该方法会将存储的长整数位表示形式转换回双精度浮点数。
     * 会进行边界检查，确保行索引和列索引在有效范围内。</p>
     * 
     * @param rowIndex 行索引，从 0 开始
     * @param columnIndex 列索引，从 0 开始
     * @return 该位置的双精度浮点数值
     * @throws IndexOutOfBoundsException 如果行索引或列索引超出边界
     */
    public double getDouble(int rowIndex, int columnIndex) {
        validateBounds(rowIndex, columnIndex);
        int offset = getOffset(rowIndex, columnIndex);
        return Double.longBitsToDouble(block.readLong(offset));
    }

    /**
     * 设置指定行和列位置的字符串值。
     * 
     * <p>字符串以 UTF-8 编码存储，存储格式为：前 4 字节为字符串长度，
     * 后跟实际的字节数据。如果字符串为 null，则长度字段存储为 0。
     * 字符串长度受列最大大小限制，超出部分会被截断。</p>
     * 
     * <p>该方法会进行边界检查，确保行索引和列索引在有效范围内。</p>
     * 
     * @param rowIndex 行索引，从 0 开始
     * @param value 要设置的字符串值，可以为 null
     * @param columnIndex 列索引，从 0 开始
     * @throws IndexOutOfBoundsException 如果行索引或列索引超出边界
     */
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

    /**
     * 获取指定行和列位置的字符串值。
     * 
     * <p>字符串以 UTF-8 编码存储，存储格式为：前 4 字节为字符串长度，
     * 后跟实际的字节数据。如果长度字段为 0，则返回 null。</p>
     * 
     * @param rowIndex 行索引，从 0 开始
     * @param columnIndex 列索引，从 0 开始
     * @return 该位置的字符串值，如果长度为 0 则返回 null
     */
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

    /**
     * 获取指定行在内存块中的起始字节偏移量。
     * 
     * @param rowIndex 行索引，从 0 开始
     * @return 该行起始位置的字节偏移量
     */
    public final int getRowOffset(int rowIndex) {
        return rowIndex * rowSize;
    }

    /**
     * 获取指定列在行内的字节偏移量。
     * 
     * @param columnIndex 列索引，从 0 开始
     * @return 该列在行内的字节偏移量
     */
    public final int getColumnOffset(int columnIndex) {
        return columnOffsets[columnIndex];
    }

    /**
     * 获取单行数据的字节大小。
     * 
     * @return 单行数据的字节大小
     */
    public final int getRowSizeValue() {
        return rowSize;
    }

    /**
     * 快速设置指定位置的整数值，不进行边界检查。
     * 
     * <p>该方法不进行边界检查，适用于高性能场景。调用方需要确保偏移量的正确性，
     * 可以通过 {@link #getRowOffset} 和 {@link #getColumnOffset} 预先计算偏移量。</p>
     * 
     * @param rowOffset 行偏移量，通过 {@link #getRowOffset} 获取
     * @param colOffset 列偏移量，通过 {@link #getColumnOffset} 获取
     * @param value 要设置的整数值
     */
    public void setIntFast(int rowOffset, int colOffset, int value) {
        block.writeIntFast(rowOffset + colOffset, value);
    }

    /**
     * 快速设置指定位置的长整数值，不进行边界检查。
     * 
     * <p>该方法不进行边界检查，适用于高性能场景。调用方需要确保偏移量的正确性，
     * 可以通过 {@link #getRowOffset} 和 {@link #getColumnOffset} 预先计算偏移量。</p>
     * 
     * @param rowOffset 行偏移量，通过 {@link #getRowOffset} 获取
     * @param colOffset 列偏移量，通过 {@link #getColumnOffset} 获取
     * @param value 要设置的长整数值
     */
    public void setLongFast(int rowOffset, int colOffset, long value) {
        block.writeLongFast(rowOffset + colOffset, value);
    }

    /**
     * 快速设置指定位置的双精度浮点数值，不进行边界检查。
     * 
     * <p>该方法会将双精度浮点数转换为长整数的位表示形式进行存储。
     * 不进行边界检查，适用于高性能场景。调用方需要确保偏移量的正确性，
     * 可以通过 {@link #getRowOffset} 和 {@link #getColumnOffset} 预先计算偏移量。</p>
     * 
     * @param rowOffset 行偏移量，通过 {@link #getRowOffset} 获取
     * @param colOffset 列偏移量，通过 {@link #getColumnOffset} 获取
     * @param value 要设置的双精度浮点数值
     */
    public void setDoubleFast(int rowOffset, int colOffset, double value) {
        block.writeLongFast(rowOffset + colOffset, Double.doubleToLongBits(value));
    }

    /**
     * 快速设置指定位置的字符串值，不进行边界检查。
     * 
     * <p>字符串以 UTF-8 编码存储，存储格式为：前 4 字节为字符串长度，
     * 后跟实际的字节数据。如果字符串为 null，则长度字段存储为 0。
     * 字符串长度受 maxSize 参数限制，超出部分会被截断。</p>
     * 
     * <p>该方法不进行边界检查，适用于高性能场景。调用方需要确保偏移量的正确性，
     * 可以通过 {@link #getRowOffset} 和 {@link #getColumnOffset} 预先计算偏移量。</p>
     * 
     * @param rowOffset 行偏移量，通过 {@link #getRowOffset} 获取
     * @param colOffset 列偏移量，通过 {@link #getColumnOffset} 获取
     * @param value 要设置的字符串值，可以为 null
     * @param maxSize 该列的最大字节大小，用于限制字符串长度
     */
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

    /**
     * 快速获取指定位置的整数值，不进行边界检查。
     * 
     * <p>该方法不进行边界检查，适用于高性能场景。调用方需要确保偏移量的正确性，
     * 可以通过 {@link #getRowOffset} 和 {@link #getColumnOffset} 预先计算偏移量。</p>
     * 
     * @param rowOffset 行偏移量，通过 {@link #getRowOffset} 获取
     * @param colOffset 列偏移量，通过 {@link #getColumnOffset} 获取
     * @return 该位置的整数值
     */
    public int getIntFast(int rowOffset, int colOffset) {
        return block.readIntFast(rowOffset + colOffset);
    }

    /**
     * 快速获取指定位置的长整数值，不进行边界检查。
     * 
     * <p>该方法不进行边界检查，适用于高性能场景。调用方需要确保偏移量的正确性，
     * 可以通过 {@link #getRowOffset} 和 {@link #getColumnOffset} 预先计算偏移量。</p>
     * 
     * @param rowOffset 行偏移量，通过 {@link #getRowOffset} 获取
     * @param colOffset 列偏移量，通过 {@link #getColumnOffset} 获取
     * @return 该位置的长整数值
     */
    public long getLongFast(int rowOffset, int colOffset) {
        return block.readLongFast(rowOffset + colOffset);
    }

    /**
     * 快速获取指定位置的字符串值，不进行边界检查。
     * 
     * <p>字符串以 UTF-8 编码存储，存储格式为：前 4 字节为字符串长度，
     * 后跟实际的字节数据。如果长度字段为 0，则返回 null。</p>
     * 
     * <p>该方法不进行边界检查，适用于高性能场景。调用方需要确保偏移量的正确性，
     * 可以通过 {@link #getRowOffset} 和 {@link #getColumnOffset} 预先计算偏移量。</p>
     * 
     * @param rowOffset 行偏移量，通过 {@link #getRowOffset} 获取
     * @param colOffset 列偏移量，通过 {@link #getColumnOffset} 获取
     * @return 该位置的字符串值，如果长度为 0 则返回 null
     */
    public String getStringFast(int rowOffset, int colOffset) {
        int len = block.readIntFast(rowOffset + colOffset);
        if (len == 0) {
            return null;
        }
        byte[] bytes = new byte[len];
        block.readBytesFast(rowOffset + colOffset + 4, bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 验证行索引和列索引是否在有效范围内。
     * 
     * @param rowIndex 行索引
     * @param columnIndex 列索引
     * @throws IndexOutOfBoundsException 如果行索引或列索引超出边界
     */
    private void validateBounds(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= getMaxRows()) {
            throw new IndexOutOfBoundsException("rowIndex out of bounds: " + rowIndex + ", maxRows: " + getMaxRows());
        }
        if (columnIndex < 0 || columnIndex >= columns.size()) {
            throw new IndexOutOfBoundsException(
                    "columnIndex out of bounds: " + columnIndex + ", columnCount: " + columns.size());
        }
    }

    /**
     * 计算指定行和列位置的字节偏移量。
     * 
     * @param rowIndex 行索引
     * @param columnIndex 列索引
     * @return 该位置在内存块中的字节偏移量
     */
    private int getOffset(int rowIndex, int columnIndex) {
        return rowIndex * rowSize + columnOffsets[columnIndex];
    }

    /**
     * 获取单行数据的字节大小。
     * 
     * @return 单行数据的字节大小
     */
    public int getRowSize() {
        return rowSize;
    }

    /**
     * 获取列的总数。
     * 
     * @return 列的总数
     */
    public int getColumnCount() {
        return columns.size();
    }

    /**
     * 获取指定列的元数据信息。
     * 
     * @param columnIndex 列索引，从 0 开始
     * @return 该列的元数据信息
     * @throws IndexOutOfBoundsException 如果列索引超出边界
     */
    public ColumnMeta getColumnMeta(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= columns.size()) {
            throw new IndexOutOfBoundsException(
                    "columnIndex out of bounds: " + columnIndex + ", columnCount: " + columns.size());
        }
        return columns.get(columnIndex);
    }

    /**
     * 获取该存储器能容纳的最大行数。
     * 
     * <p>最大行数由底层内存块大小和单行数据大小共同决定。</p>
     * 
     * @return 最大行数
     */
    public int getMaxRows() {
        return block.getSize() / rowSize;
    }
}