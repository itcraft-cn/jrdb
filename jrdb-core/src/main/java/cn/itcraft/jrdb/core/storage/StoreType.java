package cn.itcraft.jrdb.core.storage;

/**
 * 存储类型枚举，定义数据在内存中的组织方式。
 *
 * <p>不同的存储类型适用于不同的查询场景：
 * <ul>
 *   <li>{@link #ROW} - 行式存储，适合事务型操作和点查询</li>
 *   <li>{@link #COLUMN} - 列式存储，适合分析型查询和聚合操作</li>
 *   <li>{@link #HYBRID} - 混合存储，结合行列存储的优点</li>
 * </ul>
 *
 * @author jrdb
 * @see StoreConfig
 */
public enum StoreType {

    /**
     * 行式存储。
     *
     * <p>数据按行存储，同一行的所有列值连续存放。
     * 适合事务处理（OLTP）场景，对单行插入、更新和点查询性能较好。
     */
    ROW,

    /**
     * 列式存储。
     *
     * <p>数据按列存储，同一列的所有值连续存放。
     * 适合分析处理（OLAP）场景，对列聚合、扫描和压缩效率较高。
     */
    COLUMN,

    /**
     * 混合存储。
     *
     * <p>结合行式和列式存储的优点，根据数据特征和查询模式动态选择存储方式。
     * 适合混合负载（HTAP）场景。
     */
    HYBRID
}