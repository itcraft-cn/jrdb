package cn.itcraft.jrdb.jdbc.memory;

/**
 * 内存池配置类
 * 
 * <p>用于配置内存池的核心参数，包括预分配内存大小和查询缓存大小。
 * 使用Builder模式构建，支持参数校验。
 * 
 * <p>示例用法：
 * <pre>{@code
 * MemoryPoolConfig config = MemoryPoolConfig.builder()
 *     .preAllocMemSize(512 * 1024 * 1024)
 *     .queryCacheSize(32 * 1024 * 1024)
 *     .build();
 * }</pre>
 */
public class MemoryPoolConfig {
    
    /**
     * 默认预分配内存大小
     * 
     * <p>值为 256MB (256 * 1024 * 1024 字节)
     */
    public static final long DEFAULT_PRE_ALLOC_MEM_SIZE = 256L * 1024 * 1024;
    
    /**
     * 默认查询缓存大小
     * 
     * <p>值为 16MB (16 * 1024 * 1024 字节)
     */
    public static final long DEFAULT_QUERY_CACHE_SIZE = 16L * 1024 * 1024;
    
    /**
     * 预分配内存大小（字节）
     */
    private final long preAllocMemSize;
    
    /**
     * 查询缓存大小（字节）
     */
    private final long queryCacheSize;
    
    /**
     * 私有构造方法，通过Builder构建实例
     * 
     * @param builder 构建器实例
     */
    private MemoryPoolConfig(Builder builder) {
        this.preAllocMemSize = builder.preAllocMemSize;
        this.queryCacheSize = builder.queryCacheSize;
        validate();
    }
    
    /**
     * 校验配置参数的合法性
     * 
     * <p>校验规则：
     * <ul>
     *   <li>预分配内存大小必须为正数</li>
     *   <li>查询缓存大小必须为正数</li>
     *   <li>查询缓存大小不能超过预分配内存大小</li>
     * </ul>
     * 
     * @throws IllegalArgumentException 当参数不满足校验规则时抛出
     */
    private void validate() {
        if (preAllocMemSize <= 0) {
            throw new IllegalArgumentException("preAllocMemSize must be positive");
        }
        if (queryCacheSize <= 0) {
            throw new IllegalArgumentException("queryCacheSize must be positive");
        }
        if (queryCacheSize > preAllocMemSize) {
            throw new IllegalArgumentException("queryCacheSize cannot exceed preAllocMemSize");
        }
    }
    
    /**
     * 获取预分配内存大小
     * 
     * @return 预分配内存大小（字节）
     */
    public long getPreAllocMemSize() { return preAllocMemSize; }
    
    /**
     * 获取查询缓存大小
     * 
     * @return 查询缓存大小（字节）
     */
    public long getQueryCacheSize() { return queryCacheSize; }
    
    /**
     * 获取内存槽位数量
     * 
     * <p>计算公式：preAllocMemSize / queryCacheSize
     * 
     * @return 槽位数量
     */
    public int getSlotCount() { return (int) (preAllocMemSize / queryCacheSize); }
    
    /**
     * 创建Builder实例
     * 
     * @return 新的Builder实例
     */
    public static Builder builder() { return new Builder(); }
    
    /**
     * 内存池配置构建器
     * 
     * <p>提供链式调用方式构建MemoryPoolConfig实例。
     */
    public static class Builder {
        
        /**
         * 预分配内存大小，默认值为 {@link MemoryPoolConfig#DEFAULT_PRE_ALLOC_MEM_SIZE}
         */
        private long preAllocMemSize = DEFAULT_PRE_ALLOC_MEM_SIZE;
        
        /**
         * 查询缓存大小，默认值为 {@link MemoryPoolConfig#DEFAULT_QUERY_CACHE_SIZE}
         */
        private long queryCacheSize = DEFAULT_QUERY_CACHE_SIZE;
        
        /**
         * 设置预分配内存大小
         * 
         * @param preAllocMemSize 预分配内存大小（字节）
         * @return 当前Builder实例，支持链式调用
         */
        public Builder preAllocMemSize(long preAllocMemSize) {
            this.preAllocMemSize = preAllocMemSize;
            return this;
        }
        
        /**
         * 设置查询缓存大小
         * 
         * @param queryCacheSize 查询缓存大小（字节）
         * @return 当前Builder实例，支持链式调用
         */
        public Builder queryCacheSize(long queryCacheSize) {
            this.queryCacheSize = queryCacheSize;
            return this;
        }
        
        /**
         * 构建MemoryPoolConfig实例
         * 
         * @return 新的MemoryPoolConfig实例
         * @throws IllegalArgumentException 当参数校验失败时抛出
         */
        public MemoryPoolConfig build() { return new MemoryPoolConfig(this); }
    }
}