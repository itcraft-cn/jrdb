package com.dbmmap.core.storage;

import com.dbmmap.core.memory.MemoryBlock;

import java.util.List;

/**
 * Column-oriented storage that organizes data by columns rather than rows.
 * Format: [col1_all_rows][col2_all_rows]...
 * Optimized for analytical queries and aggregations.
 * <p>
 * Supports both column-oriented and row-oriented memory layouts:
 * - Column format: [col0: all rows][col1: all rows]...
 * - Row format: [row0: all cols][row1: all cols]...
 */
public class ColumnStore {

    private final MemoryBlock block;
    private final List<ColumnMeta> columns;
    private final int rowCount;
    private final int[] columnOffsets;
    private final boolean rowFormatMode;
    private final int rowSize;

    /**
     * Creates a new ColumnStore instance (column format mode).
     *
     * @param block    the memory block to store data
     * @param columns  the column metadata list
     * @param rowCount the number of rows to store
     * @throws NullPointerException if block or columns is null
     */
    public ColumnStore(MemoryBlock block, List<ColumnMeta> columns, int rowCount) {
        this(block, columns, rowCount, false);
    }

    /**
     * Creates a new ColumnStore instance with specified format mode.
     *
     * @param block         the memory block to store data
     * @param columns       the column metadata list
     * @param rowCount      the number of rows to store
     * @param rowFormatMode if true, reads from row-formatted memory
     * @throws NullPointerException if block or columns is null
     */
    ColumnStore(MemoryBlock block, List<ColumnMeta> columns, int rowCount, boolean rowFormatMode) {
        if (block == null) {
            throw new NullPointerException("MemoryBlock cannot be null");
        }
        if (columns == null) {
            throw new NullPointerException("columns cannot be null");
        }
        this.block = block;
        this.columns = columns;
        this.rowCount = rowCount;
        this.rowFormatMode = rowFormatMode;
        this.rowSize = calculateRowSize();
        this.columnOffsets = calculateColumnOffsets();
    }

    private int calculateRowSize() {
        int size = 0;
        for (ColumnMeta col : columns) {
            size += col.getSize();
        }
        return size;
    }

    /**
     * Calculates the starting offset for each column.
     * In column format: [col0: rowCount * size0][col1: rowCount * size1]...
     * In row format: column offset within each row
     *
     * @return array of column offsets
     */
    public int[] calculateColumnOffsets() {
        int size = columns.size();
        int[] offsets = new int[size];
        int offset = 0;
        if (rowFormatMode) {
            for (int i = 0; i < size; i++) {
                offsets[i] = offset;
                offset += columns.get(i).getSize();
            }
        } else {
            for (int i = 0; i < size; i++) {
                offsets[i] = offset;
                offset += rowCount * columns.get(i).getSize();
            }
        }
        return offsets;
    }

    /**
     * Gets all values of an integer column.
     *
     * @param columnIndex the column index
     * @return array of integer values
     * @throws IndexOutOfBoundsException if columnIndex is invalid
     */
    public int[] getIntColumn(int columnIndex) {
        validateColumnIndex(columnIndex);
        int[] values = new int[rowCount];
        int colOffset = columnOffsets[columnIndex];
        int colSize = columns.get(columnIndex).getSize();

        if (rowFormatMode) {
            for (int i = 0; i < rowCount; i++) {
                values[i] = block.readInt(i * rowSize + colOffset);
            }
        } else {
            for (int i = 0; i < rowCount; i++) {
                values[i] = block.readInt(colOffset + i * colSize);
            }
        }
        return values;
    }

    /**
     * Sets all values of an integer column.
     * In row format mode, writes to row-formatted memory.
     *
     * @param columnIndex the column index
     * @param values      the values to set
     * @throws IndexOutOfBoundsException if columnIndex is invalid
     * @throws IllegalArgumentException  if values length doesn't match rowCount
     */
    public void setIntColumn(int columnIndex, int[] values) {
        validateColumnIndex(columnIndex);
        if (values == null) {
            throw new NullPointerException("values cannot be null");
        }
        if (values.length != rowCount) {
            throw new IllegalArgumentException("values length must match rowCount: " + rowCount);
        }
        int colOffset = columnOffsets[columnIndex];
        int colSize = columns.get(columnIndex).getSize();

        if (rowFormatMode) {
            for (int i = 0; i < rowCount; i++) {
                block.writeInt(i * rowSize + colOffset, values[i]);
            }
        } else {
            for (int i = 0; i < rowCount; i++) {
                block.writeInt(colOffset + i * colSize, values[i]);
            }
        }
    }

    /**
     * Calculates the sum of an integer column.
     *
     * @param columnIndex the column index
     * @return the sum of all values
     * @throws IndexOutOfBoundsException if columnIndex is invalid
     */
    public long sumIntColumn(int columnIndex) {
        int[] values = getIntColumn(columnIndex);
        long sum = 0;
        for (int v : values) {
            sum += v;
        }
        return sum;
    }

    /**
     * Calculates the average of an integer column.
     *
     * @param columnIndex the column index
     * @return the average of all values
     * @throws IndexOutOfBoundsException if columnIndex is invalid
     */
    public double avgIntColumn(int columnIndex) {
        long sum = sumIntColumn(columnIndex);
        return (double) sum / rowCount;
    }

    /**
     * Finds the maximum value of an integer column.
     *
     * @param columnIndex the column index
     * @return the maximum value
     * @throws IndexOutOfBoundsException if columnIndex is invalid
     */
    public int maxIntColumn(int columnIndex) {
        int[] values = getIntColumn(columnIndex);
        int length = values.length;
        int max = values[0];
        for (int i = 1; i < length; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }
        return max;
    }

    /**
     * Finds the minimum value of an integer column.
     *
     * @param columnIndex the column index
     * @return the minimum value
     * @throws IndexOutOfBoundsException if columnIndex is invalid
     */
    public int minIntColumn(int columnIndex) {
        int[] values = getIntColumn(columnIndex);
        int length = values.length;
        int min = values[0];
        for (int i = 1; i < length; i++) {
            if (values[i] < min) {
                min = values[i];
            }
        }
        return min;
    }

    private void validateColumnIndex(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= columns.size()) {
            throw new IndexOutOfBoundsException(
                    "columnIndex out of bounds: " + columnIndex + ", columnCount: " + columns.size());
        }
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getColumnCount() {
        return columns.size();
    }

    public ColumnMeta getColumnMeta(int columnIndex) {
        validateColumnIndex(columnIndex);
        return columns.get(columnIndex);
    }
}
