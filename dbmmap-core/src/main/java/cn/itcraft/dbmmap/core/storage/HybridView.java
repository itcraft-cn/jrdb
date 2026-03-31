package cn.itcraft.dbmmap.core.storage;

import cn.itcraft.dbmmap.core.memory.MemoryBlock;

import java.util.List;

/**
 * Hybrid view that allows switching between row and column storage modes.
 * Data is stored in memory block and can be accessed via RowStore or ColumnStore.
 */
public class HybridView {

    private final MemoryBlock block;
    private final List<ColumnMeta> columns;
    private final int rowCount;
    private boolean columnMode;

    /**
     * Creates a new HybridView instance starting in row mode.
     *
     * @param block the memory block to store data
     * @param columns the column metadata list
     * @param rowCount the number of rows to store
     * @throws NullPointerException if block or columns is null
     */
    public HybridView(MemoryBlock block, List<ColumnMeta> columns, int rowCount) {
        if (block == null) {
            throw new NullPointerException("MemoryBlock cannot be null");
        }
        if (columns == null) {
            throw new NullPointerException("columns cannot be null");
        }
        this.block = block;
        this.columns = columns;
        this.rowCount = rowCount;
        this.columnMode = false;
    }

    /**
     * Switches to column mode and returns a ColumnStore view.
     * The ColumnStore reads from row-formatted memory.
     *
     * @return the ColumnStore instance
     */
    public ColumnStore toColumnView() {
        columnMode = true;
        return new ColumnStore(block, columns, rowCount, true);
    }

    /**
     * Switches to row mode and returns a RowStore view.
     *
     * @return the RowStore instance
     */
    public RowStore toRowView() {
        columnMode = false;
        return new RowStore(block, columns);
    }

    /**
     * Checks if the current mode is column mode.
     *
     * @return true if in column mode, false if in row mode
     */
    public boolean isColumnMode() {
        return columnMode;
    }

    public int getColumnCount() { return columns.size(); }

    public int getRowCount() { return rowCount; }
}