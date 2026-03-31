package com.dbmmap.core.handler;

import com.dbmmap.core.storage.ColumnMeta;

import java.util.List;

public class RowMeta {

    private final List<ColumnMeta> columns;
    private final int rowSize;
    private final int[] columnOffsets;

    public RowMeta(List<ColumnMeta> columns) {
        this.columns = columns;
        this.columnOffsets = new int[columns.size()];
        int offset = 0;
        for (int i = 0; i < columns.size(); i++) {
            columnOffsets[i] = offset;
            offset += columns.get(i).getSize();
        }
        this.rowSize = offset;
    }

    public List<ColumnMeta> getColumns() {
        return columns;
    }

    public int getRowSize() {
        return rowSize;
    }

    public int[] getColumnOffsets() {
        return columnOffsets;
    }

    public int getColumnCount() {
        return columns.size();
    }

    public ColumnMeta getColumn(int index) {
        return columns.get(index);
    }

    public int getColumnOffset(int index) {
        return columnOffsets[index];
    }
}