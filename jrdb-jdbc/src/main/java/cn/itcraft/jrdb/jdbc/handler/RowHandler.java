package cn.itcraft.jrdb.jdbc.handler;

/**
 * 行处理器接口，用于流式处理查询结果。
 *
 * <p>RowHandler以原始字节数组形式接收每一行数据，
 * 避免创建中间对象，适合以下场景：
 * <ul>
 *   <li>数据库导出CSV/JSON</li>
 *   <li>ETL数据转换</li>
 *   <li>报表生成</li>
 *   <li>数据同步</li>
 * </ul>
 *
 * <p>性能优势：
 * <ul>
 *   <li>零对象创建，GC压力极低</li>
 *   <li>比QueryResult快约2倍</li>
 *   <li>ThreadLocal复用byte[] buffer</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * session.queryWithHandler("SELECT * FROM users", (rawRow, meta) -> {
 *     int age = extractInt(rawRow, meta.getColumnOffset(2));
 *     long id = extractLong(rawRow, meta.getColumnOffset(0));
 *     outputStream.write(rawRow, 0, meta.getRowSize());
 * });
 * }</pre>
 *
 * @see RowMeta
 */
public interface RowHandler {

    /**
     * 处理一行数据。
     *
     * <p>每次调用传入一行的原始字节数据。byte[]会被复用，
     * 如需保留数据请复制到新数组。
     *
     * @param rawRow 原始行数据，按列顺序存储，格式见{@link RowMeta}
     * @param meta   行元信息，包含列定义和偏移量
     */
    void onNextRow(byte[] rawRow, RowMeta meta);
}