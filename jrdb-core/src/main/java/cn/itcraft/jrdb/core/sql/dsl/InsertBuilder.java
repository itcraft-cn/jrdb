package cn.itcraft.jrdb.core.sql.dsl;

import cn.itcraft.jrdb.core.sql.BoundSql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * INSERT语句构建器。
 * <p>
 * 用于以链式调用的方式构建INSERT SQL语句，支持批量插入多行数据。
 * </p>
 */
public class InsertBuilder {
    private final String table;
    private final List<String> columns = new ArrayList<>();
    private final List<List<Object>> rowsValues = new ArrayList<>();

    /**
     * 构造函数。
     *
     * @param table 目标表名
     */
    public InsertBuilder(String table) {
        this.table = table;
    }

    /**
     * 设置要插入的列名。
     *
     * @param cols 列名数组
     * @return 当前构建器实例，支持链式调用
     */
    public InsertBuilder columns(String... cols) {
        columns.addAll(Arrays.asList(cols));
        return this;
    }

    /**
     * 添加一行要插入的值。
     * <p>
     * 可以多次调用此方法来插入多行数据。
     * </p>
     *
     * @param vals 一行数据的值数组
     * @return 当前构建器实例，支持链式调用
     */
    public InsertBuilder values(Object... vals) {
        rowsValues.add(Arrays.asList(vals));
        return this;
    }

    /**
     * 构建最终的SQL语句。
     * <p>
     * 生成INSERT语句及其参数列表。
     * </p>
     *
     * @return 包含SQL语句和参数的BoundSql对象
     * @throws IllegalStateException 如果没有设置要插入的值
     */
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