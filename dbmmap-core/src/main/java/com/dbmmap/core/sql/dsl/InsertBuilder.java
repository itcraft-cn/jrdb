package com.dbmmap.core.sql.dsl;

import com.dbmmap.core.sql.BoundSql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InsertBuilder {
    private final String table;
    private final List<String> columns = new ArrayList<>();
    private final List<List<Object>> rowsValues = new ArrayList<>();

    public InsertBuilder(String table) {
        this.table = table;
    }

    public InsertBuilder columns(String... cols) {
        columns.addAll(Arrays.asList(cols));
        return this;
    }

    public InsertBuilder values(Object... vals) {
        rowsValues.add(Arrays.asList(vals));
        return this;
    }

    public BoundSql build() {
        if (rowsValues.isEmpty()) {
            throw new IllegalStateException("No values to insert");
        }

        List<Object> allParams = new ArrayList<>();
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(table);

        List<String> cols = columns.isEmpty() ? null : columns;

        if (cols != null) {
            sql.append(" (").append(String.join(", ", cols)).append(")");
        }

        sql.append(" VALUES ");

        String placeholder = "(" + String.join(", ", Collections.nCopies(
                cols != null ? cols.size() : rowsValues.get(0).size(), "?")) + ")";

        sql.append(String.join(", ", Collections.nCopies(rowsValues.size(), placeholder)));

        for (List<Object> row : rowsValues) {
            allParams.addAll(row);
        }

        return new BoundSql(sql.toString(), allParams.toArray());
    }
}
