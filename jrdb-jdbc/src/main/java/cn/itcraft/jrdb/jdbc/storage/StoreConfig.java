package cn.itcraft.jrdb.jdbc.storage;

/**
 * 存储配置，定义数据存储的方式和参数。
 *
 * <p>使用建造者模式创建实例：
 * <pre>{@code
 * StoreConfig config = StoreConfig.builder()
 *     .storeType(StoreType.COLUMN)
 *     .build();
 * }</pre>
 *
 * @author jrdb
 * @see StoreType
 */
public class StoreConfig {

    private final StoreType storeType;

    private StoreConfig(Builder builder) {
        this.storeType = builder.storeType;
    }

    /**
     * 获取存储类型。
     *
     * @return 存储类型
     */
    public StoreType getStoreType() { return storeType; }

    /**
     * 创建新的建造者实例。
     *
     * @return 建造者实例
     */
    public static Builder builder() { return new Builder(); }

    /**
     * 存储配置建造者。
     *
     * <p>提供流式API构建 {@link StoreConfig} 实例。
     */
    public static class Builder {
        private StoreType storeType = StoreType.ROW;

        /**
         * 设置存储类型。
         *
         * @param storeType 存储类型
         * @return 当前建造者实例
         */
        public Builder storeType(StoreType storeType) {
            this.storeType = storeType;
            return this;
        }

        /**
         * 构建 {@link StoreConfig} 实例。
         *
         * @return 新的存储配置实例
         */
        public StoreConfig build() { return new StoreConfig(this); }
    }
}