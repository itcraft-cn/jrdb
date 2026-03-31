package cn.itcraft.jrdb.core.sql.dsl;

import cn.itcraft.jrdb.core.sql.BoundSql;
import java.util.*;

/**
 * UPDATE语句构建器。
 * <p>
 * 用于以链式调用的方式构建UPDATE SQL语句，支持SET子句和WHERE条件。
 * </p>
 */
public class UpdateBuilder {
    private final String table;
    private final List<String> setClauses = new ArrayList<>();
    private final List<Object> setParams = new ArrayList<>();
    private final List<String> whereClauses = new ArrayList<>();
    private final List<Object> whereParams = new ArrayList<>();

    /**
     * 构造函数。
     *
     * @param table 目标表名
     */
    public UpdateBuilder(String table) {
        this.table = table;
    }

    /**
     * 添加SET子句，设置指定列的值。
     * <p>
     * 会自动生成占位符"?"，值将被添加到参数列表中。
     * </p>
     *
     * @param column 列名
     * @param value   列值
     * @return 当前构建器实例，支持链式调用
     */
    public UpdateBuilder set(String column, Object value) {
        setClauses.add(column + " = ?");
        setParams.add(value);
        return this;
    }

    /**
     * 添加原始SET子句。
     * <p>
     * 直接使用提供的SQL片段，不添加占位符。
     * </p>
     *
     * @param clause SET子句SQL片段，例如 "count = count + 1"
     * @return 当前构建器实例，支持链式调用
     */
    public UpdateBuilder setRaw(String clause) {
        setClauses.add(clause);
        return this;
    }

    /**
     * 添加WHERE条件。
     * <p>
     * 多次调用会使用AND连接各个条件。
     * </p>
     *
     * @param condition 条件表达式，可以包含"?"占位符
     * @param params    条件参数值
     * @return 当前构建器实例，支持链式调用
     */
    public UpdateBuilder where(String condition, Object... params) {
        whereClauses.add(condition);
        whereParams.addAll(Arrays.asList(params));
        return this;
    }

    /**
     * 条件性添加WHERE条件。
     * <p>
     * 仅当条件为true时才添加WHERE子句。
     * </p>
     *
     * @param condition 是否添加条件的判断标志
     * @param clause    条件表达式，可以包含"?"占位符
     * @param params    条件参数值
     * @return 当前构建器实例，支持链式调用
     */
    public UpdateBuilder whereIf(boolean condition, String clause, Object... params) {
        if (condition) {
            where(clause, params);
        }
        return this;
    }

    /**
     * 构建最终的SQL语句。
     * <p>
     * 生成UPDATE语句及其参数列表。
     * </p>
     *
     * @return 包含SQL语句和参数的BoundSql对象
     */
    public BoundSql build() {
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(table).append(" SET ");
        sql.append(String.join(", ", setClauses));

        List<Object> allParams = new ArrayList<>(setParams);

        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
            allParams.addAll(whereParams);
        }

        return new BoundSql(sql.toString(), allParams.toArray());
    }
}