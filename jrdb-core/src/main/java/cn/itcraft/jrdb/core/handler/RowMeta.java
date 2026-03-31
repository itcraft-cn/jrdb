package cn.itcraft.jrdb.core.handler;

import cn.itcraft.jrdb.core.storage.ColumnMeta;

import java.util.List;

/**
 * 行元信息，描述原始行数据的结构。
 *
 * <p>RowMeta包含列定义和偏移量信息，用于解析RowHandler中的原始字节数组。
 *
 * <p>数据格式：
 * <table border="1">
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
}