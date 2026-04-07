package cn.itcraft.jrdb.jdbc.sql;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

/**
 * SQL直接执行工具类，用于拼接完整SQL语句和转义参数。
 *
 * <p>提供安全的参数转义，防止SQL注入。</p>
 *
 * <p><b>使用场景：</b>当enableSqlCascade=true时，批量操作直接拼接SQL执行，
 * 省去PreparedStatement参数绑定开销，性能更高。</p>
 *
 * @author jrdb
 * @since 1.1.0
 */
public final class DirectSqlBuilder {

    private static final int DEFAULT_CAPACITY = 256;

    private DirectSqlBuilder() {
    }

    /**
     * 转义参数值，防止SQL注入。
     *
     * @param value 参数值
     * @return 转义后的字符串
     */
    public static String escape(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "1" : "0";
        }
        if (value instanceof java.util.Date) {
            return "'" + new Timestamp(((java.util.Date) value).getTime()) + "'";
        }
        if (value instanceof Date) {
            return "'" + value + "'";
        }
        if (value instanceof Timestamp) {
            return "'" + value + "'";
        }
        String str = value.toString();
        return escapeString(str);
    }

    /**
     * 转义字符串，处理单引号和特殊字符。
     */
    private static String escapeString(String str) {
        StringBuilder sb = new StringBuilder(str.length() + 16);
        sb.append('\'');
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '\'':
                    sb.append("''");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        sb.append('\'');
        return sb.toString();
    }

    /**
     * 构建批量INSERT语句。
     *
     * @param table   表名
     * @param columns 列名数组
     * @param rows    数据行列表
     * @return 完整INSERT语句
     */
    public static String buildInsert(String table, String[] columns, List<Object[]> rows) {
        StringBuilder sb = new StringBuilder(DEFAULT_CAPACITY * rows.size());
        sb.append("INSERT INTO ").append(table).append(" (");
        
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(columns[i]);
        }
        sb.append(") VALUES ");

        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) sb.append(", ");
            Object[] row = rows.get(i);
            sb.append('(');
            for (int j = 0; j < row.length; j++) {
                if (j > 0) sb.append(", ");
                sb.append(escape(row[j]));
            }
            sb.append(')');
        }

        return sb.toString();
    }

    /**
     * 构建批量UPDATE语句（基于主键）。
     *
     * @param table       表名
     * @param setColumn   要更新的列名
     * @param whereColumn 条件列名（通常为主键）
     * @param rows        数据行，每行为 [whereValue, setValue]
     * @return 完整UPDATE语句
     */
    public static String buildUpdate(String table, String setColumn, String whereColumn, List<Object[]> rows) {
        StringBuilder sb = new StringBuilder(DEFAULT_CAPACITY * rows.size());
        sb.append("UPDATE ").append(table).append(" SET ").append(setColumn).append(" = CASE ").append(whereColumn);
        
        for (Object[] row : rows) {
            sb.append(" WHEN ").append(escape(row[0])).append(" THEN ").append(escape(row[1]));
        }
        
        sb.append(" END WHERE ").append(whereColumn).append(" IN (");
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(escape(rows.get(i)[0]));
        }
        sb.append(')');
        
        return sb.toString();
    }

    /**
     * 构建批量DELETE语句。
     *
     * @param table  表名
     * @param column 条件列名
     * @param values 值列表
     * @return 完整DELETE语句
     */
    public static String buildDelete(String table, String column, List<?> values) {
        StringBuilder sb = new StringBuilder(128 + values.size() * 16);
        sb.append("DELETE FROM ").append(table).append(" WHERE ").append(column).append(" IN (");
        
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(escape(values.get(i)));
        }
        sb.append(')');
        
        return sb.toString();
    }

    /**
     * 构建单条INSERT语句。
     *
     * @param table   表名
     * @param columns 列名数组
     * @param values  值数组
     * @return 完整INSERT语句
     */
    public static String buildInsertOne(String table, String[] columns, Object[] values) {
        StringBuilder sb = new StringBuilder(DEFAULT_CAPACITY);
        sb.append("INSERT INTO ").append(table).append(" (");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(columns[i]);
        }
        sb.append(") VALUES (");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(escape(values[i]));
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * 构建单条UPDATE语句。
     *
     * @param table       表名
     * @param setColumns  要更新的列名数组
     * @param setValues   要更新的值数组
     * @param whereColumn 条件列名
     * @param whereValue  条件值
     * @return 完整UPDATE语句
     */
    public static String buildUpdateOne(String table, String[] setColumns, Object[] setValues, 
                                        String whereColumn, Object whereValue) {
        StringBuilder sb = new StringBuilder(DEFAULT_CAPACITY);
        sb.append("UPDATE ").append(table).append(" SET ");
        for (int i = 0; i < setColumns.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(setColumns[i]).append(" = ").append(escape(setValues[i]));
        }
        sb.append(" WHERE ").append(whereColumn).append(" = ").append(escape(whereValue));
        return sb.toString();
    }

    /**
     * 构建单条DELETE语句。
     *
     * @param table       表名
     * @param whereColumn 条件列名
     * @param whereValue  条件值
     * @return 完整DELETE语句
     */
    public static String buildDeleteOne(String table, String whereColumn, Object whereValue) {
        return "DELETE FROM " + table + " WHERE " + whereColumn + " = " + escape(whereValue);
    }
}