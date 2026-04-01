package cn.itcraft.jrdb.jdbc.handler;

import cn.itcraft.jrdb.jdbc.storage.ColumnMeta;

import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.util.List;

/**
 * 行元信息，描述原始行数据的结构。
 *
 * <p>RowMeta包含列定义和偏移量信息，用于解析RowHandler中的原始字节数组。
 *
 * <p>数据格式：
 * <table border="1">
 * <caption>列数据存储格式</caption>
 *   <tr><th>列类型</th><th>存储大小</th><th>格式</th></tr>
 *   <tr><td>INT/TINYINT/SMALLINT</td><td>4字节</td><td>大端序</td></tr>
 *   <tr><td>BIGINT</td><td>8字节</td><td>大端序</td></tr>
 *   <tr><td>DOUBLE/FLOAT</td><td>8字节</td><td>IEEE 754</td></tr>
 *   <tr><td>BOOLEAN</td><td>1字节</td><td>0/1</td></tr>
 *   <tr><td>TIMESTAMP/DATE</td><td>8字节</td><td>epoch millis</td></tr>
 *   <tr><td>VARCHAR</td><td>4+N字节</td><td>长度(4B)+UTF8字节</td></tr>
 * </table>
 *
 * <p>使用示例：
 * <pre>{@code
 * RowMeta meta = ...;
 * for (int i = 0; i < meta.getColumnCount(); i++) {
 *     int offset = meta.getColumnOffset(i);
 *     ColumnMeta col = meta.getColumn(i);
 *     // 根据 col.getSqlType() 解析 rawRow[offset...]
 * }
 * }</pre>
 *
 * <p>便捷方法示例：
 * <pre>{@code
 * session.queryWithHandler("SELECT id, name, age FROM users", (rawRow, meta) -> {
 *     long id = meta.extractLong(rawRow, 0);
 *     String name = meta.extractString(rawRow, 1);
 *     int age = meta.extractInt(rawRow, 2);
 * });
 * }</pre>
 */
public class RowMeta {

    private final List<ColumnMeta> columns;

    private final int rowSize;

    private final int[] columnOffsets;

    /**
     * 构造行元信息。
     *
     * <p>根据列定义计算每列在原始字节数组中的偏移量。
     *
     * @param columns 列元信息列表
     */
    public RowMeta(List<ColumnMeta> columns) {
        this.columns = columns;
        this.columnOffsets = new int[columns.size()];
        int offset = 0;
        for (int i = 0; i < columns.size(); i++) {
            columnOffsets[i] = offset;
            offset += columns.get(i).getSize();
        }
        this.rowSize = offset;
    }

    /**
     * 获取所有列元信息。
     *
     * @return 列列表
     */
    public List<ColumnMeta> getColumns() {
        return columns;
    }

    /**
     * 获取单行原始数据大小。
     *
     * @return 字节数
     */
    public int getRowSize() {
        return rowSize;
    }

    /**
     * 获取所有列偏移量。
     *
     * @return 偏移量数组，索引对应列索引
     */
    public int[] getColumnOffsets() {
        return columnOffsets;
    }

    /**
     * 获取列数。
     *
     * @return 列数
     */
    public int getColumnCount() {
        return columns.size();
    }

    /**
     * 获取指定索引的列元信息。
     *
     * @param index 列索引（从0开始）
     * @return 列元信息
     */
    public ColumnMeta getColumn(int index) {
        return columns.get(index);
    }

    /**
     * 获取指定列在原始字节数组中的起始偏移量。
     *
     * @param index 列索引（从0开始）
     * @return 字节偏移量
     */
    public int getColumnOffset(int index) {
        return columnOffsets[index];
    }

    /**
     * 从原始行数据中提取int值。
     *
     * <p>适用于 INT、TINYINT、SMALLINT 类型列。
     *
     * @param rawRow 原始行数据
     * @param columnIndex 列索引（从0开始）
     * @return int值
     */
    public int extractInt(byte[] rawRow, int columnIndex) {
        int offset = columnOffsets[columnIndex];
        return ((rawRow[offset] & 0xFF) << 24)
                | ((rawRow[offset + 1] & 0xFF) << 16)
                | ((rawRow[offset + 2] & 0xFF) << 8)
                | (rawRow[offset + 3] & 0xFF);
    }

    /**
     * 从原始行数据中提取long值。
     *
     * <p>适用于 BIGINT、TIMESTAMP、DATE 类型列。
     *
     * @param rawRow 原始行数据
     * @param columnIndex 列索引（从0开始）
     * @return long值
     */
    public long extractLong(byte[] rawRow, int columnIndex) {
        int offset = columnOffsets[columnIndex];
        return ((long) (rawRow[offset] & 0xFF) << 56)
                | ((long) (rawRow[offset + 1] & 0xFF) << 48)
                | ((long) (rawRow[offset + 2] & 0xFF) << 40)
                | ((long) (rawRow[offset + 3] & 0xFF) << 32)
                | ((long) (rawRow[offset + 4] & 0xFF) << 24)
                | ((long) (rawRow[offset + 5] & 0xFF) << 16)
                | ((long) (rawRow[offset + 6] & 0xFF) << 8)
                | ((long) (rawRow[offset + 7] & 0xFF));
    }

    /**
     * 从原始行数据中提取double值。
     *
     * <p>适用于 DOUBLE、FLOAT、DECIMAL、NUMERIC 类型列。
     *
     * @param rawRow 原始行数据
     * @param columnIndex 列索引（从0开始）
     * @return double值
     */
    public double extractDouble(byte[] rawRow, int columnIndex) {
        long bits = extractLong(rawRow, columnIndex);
        return Double.longBitsToDouble(bits);
    }

    /**
     * 从原始行数据中提取boolean值。
     *
     * <p>适用于 BOOLEAN 类型列。
     *
     * @param rawRow 原始行数据
     * @param columnIndex 列索引（从0开始）
     * @return boolean值
     */
    public boolean extractBoolean(byte[] rawRow, int columnIndex) {
        int offset = columnOffsets[columnIndex];
        return rawRow[offset] != 0;
    }

    /**
     * 从原始行数据中提取String值。
     *
     * <p>适用于 VARCHAR、CHAR、LONGVARCHAR 类型列。
     * 格式为4字节长度前缀 + UTF-8字节。
     *
     * @param rawRow 原始行数据
     * @param columnIndex 列索引（从0开始）
     * @return String值，可能为空字符串
     */
    public String extractString(byte[] rawRow, int columnIndex) {
        int offset = columnOffsets[columnIndex];
        int len = ((rawRow[offset] & 0xFF) << 24)
                | ((rawRow[offset + 1] & 0xFF) << 16)
                | ((rawRow[offset + 2] & 0xFF) << 8)
                | (rawRow[offset + 3] & 0xFF);
        if (len == 0) {
            return "";
        }
        return new String(rawRow, offset + 4, len, StandardCharsets.UTF_8);
    }

    /**
     * 从原始行数据中根据列类型自动提取值。
     *
     * <p>根据列的SQL类型自动选择对应的提取方法。
     *
     * @param rawRow 原始行数据
     * @param columnIndex 列索引（从0开始）
     * @return 提取的值，类型取决于列类型
     */
    public Object extractValue(byte[] rawRow, int columnIndex) {
        ColumnMeta col = columns.get(columnIndex);
        int sqlType = col.getSqlType();
        switch (sqlType) {
            case Types.INTEGER:
            case Types.TINYINT:
            case Types.SMALLINT:
                return extractInt(rawRow, columnIndex);
            case Types.BIGINT:
                return extractLong(rawRow, columnIndex);
            case Types.TIMESTAMP:
            case Types.DATE:
                return new java.sql.Timestamp(extractLong(rawRow, columnIndex));
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.DECIMAL:
            case Types.NUMERIC:
                return extractDouble(rawRow, columnIndex);
            case Types.BOOLEAN:
                return extractBoolean(rawRow, columnIndex);
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
            default:
                return extractString(rawRow, columnIndex);
        }
    }
}