package cn.itcraft.jrdb.jdbc.sql.dsl;

import cn.itcraft.jrdb.jdbc.sql.BoundSql;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SELECT语句构建器。
 * <p>
 * 用于以链式调用的方式构建SELECT SQL语句，支持JOIN、WHERE、GROUP BY、HAVING、ORDER BY、LIMIT、OFFSET等子句。
 * </p>
 */
public class SelectBuilder {
    private String[] columns;
    private String table;
    private String alias;
    private final List<String> joins = new ArrayList<>();
    private final List<String> whereClauses = new ArrayList<>();
    private final List<Object> whereParams = new ArrayList<>();
    private String groupBy;
    private String having;
    private final List<Object> havingParams = new ArrayList<>();
    private String orderBy;
    private Integer limit;
    private Integer offset;

    /**
     * 构造函数。
     *
     * @param columns 要查询的列名数组，如果为空则查询所有列
     */
    public SelectBuilder(String... columns) {
        this.columns = columns;
    }

    /**
     * 设置FROM子句，指定查询的表名。
     *
     * @param table 表名
     * @return 当前构建器实例，支持链式调用
     */
    public SelectBuilder from(String table) {
        this.table = table;
        return this;
    }

    /**
     * 设置FROM子句，指定查询的表名和别名。
     *
     * @param table 表名
     * @param alias 表别名
     * @return 当前构建器实例，支持链式调用
     */
    public SelectBuilder from(String table, String alias) {
        this.table = table;
        this.alias = alias;
        return this;
    }

    /**
     * 添加INNER JOIN子句。
     *
     * @param table 要连接的表名
     * @param on    连接条件
     * @return 当前构建器实例，支持链式调用
     */
    public SelectBuilder join(String table, String on) {
        joins.add("JOIN " + table + " ON " + on);
        return this;
    }

    /**
     * 添加LEFT JOIN子句。
     *
     * @param table 要连接的表名
     * @param on    连接条件
     * @return 当前构建器实例，支持链式调用
     */
    public SelectBuilder leftJoin(String table, String on) {
        joins.add("LEFT JOIN " + table + " ON " + on);
        return this;
    }

    /**
     * 添加RIGHT JOIN子句。
     *
     * @param table 要连接的表名
     * @param on    连接条件
     * @return 当前构建器实例，支持链式调用
     */
    public SelectBuilder rightJoin(String table, String on) {
        joins.add("RIGHT JOIN " + table + " ON " + on);
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
    public SelectBuilder where(String condition, Object... params) {
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
    public SelectBuilder whereIf(boolean condition, String clause, Object... params) {
        if (condition) {
            where(clause, params);
        }
        return this;
    }

    /**
     * 设置GROUP BY子句。
     *
     * @param columns 分组列名数组
     * @return 当前构建器实例，支持链式调用
     */
    public SelectBuilder groupBy(String... columns) {
        this.groupBy = String.join(", ", columns);
        return this;
    }

    /**
     * 设置HAVING子句。
     *
     * @param condition HAVING条件表达式，可以包含"?"占位符
     * @param params    条件参数值
     * @return 当前构建器实例，支持链式调用
     */
    public SelectBuilder having(String condition, Object... params) {
        this.having = condition;
        havingParams.addAll(Arrays.asList(params));
        return this;
    }

    /**
     * 设置ORDER BY子句。
     *
     * @param orders 排序表达式数组，例如 "id DESC", "name ASC"
     * @return 当前构建器实例，支持链式调用
     */
    public SelectBuilder orderBy(String... orders) {
        this.orderBy = String.join(", ", orders);
        return this;
    }

    /**
     * 设置LIMIT子句，限制返回的记录数。
     *
     * @param limit 最大返回记录数
     * @return 当前构建器实例，支持链式调用
     */
    public SelectBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * 设置OFFSET子句，指定返回记录的起始偏移量。
     *
     * @param offset 偏移量
     * @return 当前构建器实例，支持链式调用
     */
    public SelectBuilder offset(int offset) {
        this.offset = offset;
        return this;
    }

    /**
     * 构建最终的SQL语句。
     * <p>
     * 生成SELECT语句及其参数列表。
     * </p>
     *
     * @return 包含SQL语句和参数的BoundSql对象
     */
    public BoundSql build() {
        StringBuilder sql = new StringBuilder("SELECT ");
        List<Object> allParams = new ArrayList<>();

        sql.append(columns == null || columns.length == 0 ? "*" : String.join(", ", columns));

        sql.append(" FROM ").append(table);
        if (alias != null) sql.append(" ").append(alias);

        for (String join : joins) {
            sql.append(" ").append(join);
        }

        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
            allParams.addAll(whereParams);
        }

        if (groupBy != null) {
            sql.append(" GROUP BY ").append(groupBy);
        }

        if (having != null) {
            sql.append(" HAVING ").append(having);
            allParams.addAll(havingParams);
        }

        if (orderBy != null) {
            sql.append(" ORDER BY ").append(orderBy);
        }

        if (limit != null) {
            sql.append(" LIMIT ").append(limit);
        }

        if (offset != null) {
            sql.append(" OFFSET ").append(offset);
        }

        return new BoundSql(sql.toString(), allParams.toArray());
    }
}