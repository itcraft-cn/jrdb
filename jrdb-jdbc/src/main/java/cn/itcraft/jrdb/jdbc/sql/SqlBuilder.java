package cn.itcraft.jrdb.jdbc.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SQL构建器
 *
 * <p>用于逐步构建SQL语句及其参数，支持链式调用。
 * 内部维护SQL字符串和参数列表，适用于动态SQL场景。
 *
 * <p>使用示例：
 * <pre>{@code
 * SqlBuilder builder = SqlBuilder.create()
 *     .append("SELECT * FROM user")
 *     .append("WHERE id = ?", 1)
 *     .appendIf(status != null, "AND status = ?", status);
 * String sql = builder.build();
 * Object[] params = builder.getParams();
 * }</pre>
 */
public class SqlBuilder {
    private final StringBuilder sql = new StringBuilder();
    private final List<Object> params = new ArrayList<>();
    
    /**
     * 创建SQL构建器实例
     *
     * @return 新的SqlBuilder实例
     */
    public static SqlBuilder create() { 
        return new SqlBuilder(); 
    }
    
    /**
     * 追加SQL片段及其参数
     *
     * <p>自动处理片段间的空格分隔。如果当前SQL非空且片段不以空格开头，
     * 则自动添加空格分隔。
     *
     * @param fragment SQL片段
     * @param params 该片段对应的参数
     * @return 当前构建器实例，支持链式调用
     */
    public SqlBuilder append(String fragment, Object... params) {
        if (sql.length() > 0 && !fragment.startsWith(" ")) {
            sql.append(" ");
        }
        sql.append(fragment);
        this.params.addAll(Arrays.asList(params));
        return this;
    }
    
    /**
     * 条件性追加SQL片段
     *
     * <p>仅当条件为true时才追加片段，适用于动态SQL构建。
     *
     * @param condition 追加条件
     * @param fragment SQL片段
     * @param params 该片段对应的参数
     * @return 当前构建器实例，支持链式调用
     */
    public SqlBuilder appendIf(boolean condition, String fragment, Object... params) {
        if (condition) {
            append(fragment, params);
        }
        return this;
    }
    
    /**
     * 构建并返回SQL字符串
     *
     * @return 完整的SQL语句
     */
    public String build() { 
        return sql.toString(); 
    }
    
    /**
     * 获取参数数组
     *
     * @return 参数数组
     */
    public Object[] getParams() { 
        return params.toArray(); 
    }
    
    /**
     * 重置构建器状态
     *
     * <p>清空SQL字符串和参数列表，使构建器可复用。
     */
    public void reset() { 
        sql.setLength(0); 
        params.clear(); 
    }
    
    /**
     * 判断SQL是否为空
     *
     * @return SQL为空返回true，否则返回false
     */
    public boolean isEmpty() { 
        return sql.length() == 0; 
    }
}