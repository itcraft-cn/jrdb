package cn.itcraft.jrdb.jdbc.sql;

/**
 * 绑定SQL对象
 *
 * <p>封装SQL语句及其对应的参数，是不可变对象。
 * 用于表示一个完整的可执行SQL语句。
 *
 * <p>使用示例：
 * <pre>{@code
 * BoundSql boundSql = new BoundSql("SELECT * FROM user WHERE id = ?", new Object[]{1});
 * String sql = boundSql.getSql();
 * Object[] params = boundSql.getParams();
 * }</pre>
 */
public class BoundSql {
    private final String sql;
    private final Object[] params;
    
    /**
     * 构造绑定SQL对象
     *
     * @param sql SQL语句
     * @param params 参数数组，为null时自动转换为空数组
     */
    public BoundSql(String sql, Object[] params) {
        this.sql = sql;
        this.params = params != null ? params : new Object[0];
    }
    
    /**
     * 获取SQL语句
     *
     * @return SQL语句字符串
     */
    public String getSql() { 
        return sql; 
    }
    
    /**
     * 获取参数数组
     *
     * @return 参数数组
     */
    public Object[] getParams() { 
        return params; 
    }
}