package cn.itcraft.dbmmap.core.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CachedQueryResult {

    private final List<String> columnNames;
    private final List<Map<String, Object>> rows;

    public CachedQueryResult(List<String> columnNames, List<Map<String, Object>> rows) {
        this.columnNames = columnNames;
        this.rows = rows;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public int getRowCount() {
        return rows.size();
    }

    public Map<String, Object> getRow(int index) {
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        return rows.get(index);
    }

    public static class Builder {
        private final List<String> columnNames = new ArrayList<>();
        private final List<Map<String, Object>> rows = new ArrayList<>();

        public Builder addColumnName(String name) {
            columnNames.add(name);
            return this;
        }

        public Builder addRow(Map<String, Object> row) {
            rows.add(row);
            return this;
        }

        public Builder newRow() {
            rows.add(new HashMap<>());
            return this;
        }

        public Builder put(String column, Object value) {
            if (!rows.isEmpty()) {
                rows.get(rows.size() - 1).put(column.toUpperCase(), value);
            }
            return this;
        }

        public CachedQueryResult build() {
            return new CachedQueryResult(columnNames, rows);
        }
    }
}
