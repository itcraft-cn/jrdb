package cn.itcraft.jrdb.jdbc.sql.dsl;

import cn.itcraft.jrdb.jdbc.sql.BoundSql;
import java.util.*;

/**
 * DELETE语句构建器。
 * <p>
 * 用于以链式调用的方式构建DELETE SQL语句，支持WHERE条件。
 * </p>
 */
public class DeleteBuilder {
    private final String table;
    private final List<String> whereClauses = new ArrayList<>();
    private final List<Object> whereParams = new ArrayList<>();

    /**
     * 构造函数。
     *
     * @param table 目标表名
     */
    public DeleteBuilder(String table) {
        this.table = table;
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
    public DeleteBuilder where(String condition, Object... params) {
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
    public DeleteBuilder whereIf(boolean condition, String clause, Object... params) {
        if (condition) {
            where(clause, params);
        }
        return this;
    }

    /**
     * 构建最终的SQL语句。
     * <p>
     * 生成DELETE语句及其参数列表。
     * </p>
     *
     * @return 包含SQL语句和参数的BoundSql对象
     */
    public BoundSql build() {
        StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(table);

        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
        }

        return new BoundSql(sql.toString(), whereParams.toArray());
    }
}