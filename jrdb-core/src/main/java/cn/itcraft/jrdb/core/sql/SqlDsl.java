package cn.itcraft.jrdb.core.sql;

import cn.itcraft.jrdb.core.sql.dsl.*;

/**
 * SQL DSL工具类
 *
 * <p>提供静态方法构建各类SQL语句，支持SELECT、INSERT、UPDATE、DELETE操作。
 * 采用流式API设计，支持链式调用。
 *
 * <p>使用示例：
 * <pre>{@code
 * // SELECT查询
 * SqlDsl.select("id", "name").from("user").where("id = ?", 1);
 *
 * // INSERT插入
 * SqlDsl.insertInto("user").columns("name", "age").values("张三", 25);
 *
 * // UPDATE更新
 * SqlDsl.update("user").set("name", "李四").where("id = ?", 1);
 *
 * // DELETE删除
 * SqlDsl.deleteFrom("user").where("id = ?", 1);
 * }</pre>
 */
public class SqlDsl {
    /**
     * 创建SELECT查询构建器，指定查询列
     *
     * @param columns 查询列名数组
     * @return SELECT构建器
     */
    public static SelectBuilder select(String... columns) {
        return new SelectBuilder(columns);
    }
    
    /**
     * 创建SELECT查询构建器，查询所有列（SELECT *）
     *
     * @return SELECT构建器
     */
    public static SelectBuilder selectAll() {
        return new SelectBuilder();
    }
    
    /**
     * 创建INSERT语句构建器
     *
     * @param table 目标表名
     * @return INSERT构建器
     */
    public static InsertBuilder insertInto(String table) {
        return new InsertBuilder(table);
    }
    
    /**
     * 创建UPDATE语句构建器
     *
     * @param table 目标表名
     * @return UPDATE构建器
     */
    public static UpdateBuilder update(String table) {
        return new UpdateBuilder(table);
    }
    
    /**
     * 创建DELETE语句构建器
     *
     * @param table 目标表名
     * @return DELETE构建器
     */
    public static DeleteBuilder deleteFrom(String table) {
        return new DeleteBuilder(table);
    }
}