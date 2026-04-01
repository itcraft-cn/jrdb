package cn.itcraft.jrdb.jdbc.config;

import cn.itcraft.jrdb.jdbc.memory.MemoryPoolConfig;

/**
 * JRDB框架的全局配置类。
 * 
 * <p>该类使用Builder模式构建，包含内存池配置、溢出策略、缓存级别、
 * SQL语句缓存大小、结果集缓存大小及过期时间等核心配置项。</p>
 * 
 * <p>配置项在构建时会进行有效性验证，确保配置值的合法性。</p>
 * 
 * @author itcraft
 * @see MemoryPoolConfig
 * @see OverflowStrategy
 * @see CacheLevel
 */
public class JrdbConfig {
    
    /**
     * 内存池配置，用于管理堆外内存的分配与使用。
     */
    private final MemoryPoolConfig memoryPoolConfig;
    
    /**
     * 溢出策略，定义当查询结果超出预设限制时的处理方式。
     */
    private final OverflowStrategy overflowStrategy;
    
    /**
     * 缓存级别，控制是否启用一级缓存或二级缓存。
     */
    private final CacheLevel cacheLevel;
    
    /**
     * SQL语句缓存大小，单位为条数。
     */
    private final int statementCacheSize;
    
    /**
     * 结果集缓存大小，单位为条数。
     */
    private final int resultCacheSize;
    
    /**
     * 结果集缓存过期时间，单位为秒。
     */
    private final int resultCacheExpireSeconds;

    /**
     * 私有构造方法，通过Builder构建配置实例。
     *
     * @param builder 构建器实例
     */
    private JrdbConfig(Builder builder) {
        this.memoryPoolConfig = builder.memoryPoolConfig;
        this.overflowStrategy = builder.overflowStrategy;
        this.cacheLevel = builder.cacheLevel;
        this.statementCacheSize = builder.statementCacheSize;
        this.resultCacheSize = builder.resultCacheSize;
        this.resultCacheExpireSeconds = builder.resultCacheExpireSeconds;
        validate();
    }
    
    /**
     * 验证配置项的有效性。
     * 
     * <p>检查以下内容：</p>
     * <ul>
     *   <li>内存池配置不能为空</li>
     *   <li>SQL语句缓存大小必须非负</li>
     *   <li>结果集缓存大小必须非负</li>
     * </ul>
     * 
     * @throws NullPointerException 当内存池配置为空时抛出
     * @throws IllegalArgumentException 当缓存大小配置为负数时抛出
     */
    private void validate() {
        if (memoryPoolConfig == null) {
            throw new NullPointerException("memoryPoolConfig cannot be null");
        }
        if (statementCacheSize < 0) {
            throw new IllegalArgumentException("statementCacheSize must be non-negative");
        }
        if (resultCacheSize < 0) {
            throw new IllegalArgumentException("resultCacheSize must be non-negative");
        }
    }
    
    /**
     * 获取内存池配置。
     * 
     * @return 内存池配置实例
     */
    public MemoryPoolConfig getMemoryPoolConfig() { return memoryPoolConfig; }
    
    /**
     * 获取溢出策略。
     * 
     * @return 溢出策略枚举值
     */
    public OverflowStrategy getOverflowStrategy() { return overflowStrategy; }
    
    /**
     * 获取缓存级别。
     * 
     * @return 缓存级别枚举值
     */
    public CacheLevel getCacheLevel() { return cacheLevel; }
    
    /**
     * 获取SQL语句缓存大小。
     * 
     * @return SQL语句缓存大小，单位为条数
     */
    public int getStatementCacheSize() { return statementCacheSize; }
    
    /**
     * 获取结果集缓存大小。
     * 
     * @return 结果集缓存大小，单位为条数
     */
    public int getResultCacheSize() { return resultCacheSize; }
    
    /**
     * 获取结果集缓存过期时间。
     * 
     * @return 结果集缓存过期时间，单位为秒
     */
    public int getResultCacheExpireSeconds() { return resultCacheExpireSeconds; }
    
    /**
     * 判断是否启用一级缓存。
     * 
     * <p>当缓存级别为L1或L2时返回true。</p>
     * 
     * @return 如果启用一级缓存返回true，否则返回false
     */
    public boolean isL1CacheEnabled() {
        return cacheLevel == CacheLevel.L1 || cacheLevel == CacheLevel.L2;
    }
    
    /**
     * 判断是否启用二级缓存。
     * 
     * <p>当缓存级别为L2时返回true。</p>
     * 
     * @return 如果启用二级缓存返回true，否则返回false
     */
    public boolean isL2CacheEnabled() {
        return cacheLevel == CacheLevel.L2;
    }
    
    /**
     * 创建新的Builder实例。
     * 
     * @return 新的Builder实例
     */
    public static Builder builder() { return new Builder(); }
    
    /**
     * JrdbConfig的构建器类。
     * 
     * <p>提供链式调用的方式构建JrdbConfig实例，所有配置项均有默认值：</p>
     * <ul>
     *   <li>memoryPoolConfig: 默认内存池配置</li>
     *   <li>overflowStrategy: ERROR（抛出异常）</li>
     *   <li>cacheLevel: L1（启用一级缓存）</li>
     *   <li>statementCacheSize: 256</li>
     *   <li>resultCacheSize: 1000</li>
     *   <li>resultCacheExpireSeconds: 300</li>
     * </ul>
     */
    public static class Builder {
        
        /**
         * 内存池配置，默认使用默认参数构建的内存池配置。
         */
        private MemoryPoolConfig memoryPoolConfig = MemoryPoolConfig.builder().build();
        
        /**
         * 溢出策略，默认为ERROR（抛出异常）。
         */
        private OverflowStrategy overflowStrategy = OverflowStrategy.ERROR;
        
        /**
         * 缓存级别，默认为L1（启用一级缓存）。
         */
        private CacheLevel cacheLevel = CacheLevel.L1;
        
        /**
         * SQL语句缓存大小，默认为256条。
         */
        private int statementCacheSize = 256;
        
        /**
         * 结果集缓存大小，默认为1000条。
         */
        private int resultCacheSize = 1000;
        
        /**
         * 结果集缓存过期时间，默认为300秒。
         */
        private int resultCacheExpireSeconds = 300;
        
        /**
         * 设置内存池配置。
         * 
         * @param memoryPoolConfig 内存池配置实例
         * @return 当前Builder实例
         */
        public Builder memoryPoolConfig(MemoryPoolConfig memoryPoolConfig) {
            this.memoryPoolConfig = memoryPoolConfig;
            return this;
        }
        
        /**
         * 设置溢出策略。
         * 
         * @param overflowStrategy 溢出策略枚举值
         * @return 当前Builder实例
         */
        public Builder overflowStrategy(OverflowStrategy overflowStrategy) {
            this.overflowStrategy = overflowStrategy;
            return this;
        }
        
        /**
         * 设置缓存级别。
         * 
         * @param cacheLevel 缓存级别枚举值
         * @return 当前Builder实例
         */
        public Builder cacheLevel(CacheLevel cacheLevel) {
            this.cacheLevel = cacheLevel;
            return this;
        }
        
        /**
         * 设置SQL语句缓存大小。
         * 
         * @param statementCacheSize SQL语句缓存大小，单位为条数，必须非负
         * @return 当前Builder实例
         */
        public Builder statementCacheSize(int statementCacheSize) {
            this.statementCacheSize = statementCacheSize;
            return this;
        }
        
        /**
         * 设置结果集缓存大小。
         * 
         * @param resultCacheSize 结果集缓存大小，单位为条数，必须非负
         * @return 当前Builder实例
         */
        public Builder resultCacheSize(int resultCacheSize) {
            this.resultCacheSize = resultCacheSize;
            return this;
        }
        
        /**
         * 设置结果集缓存过期时间。
         * 
         * @param resultCacheExpireSeconds 结果集缓存过期时间，单位为秒
         * @return 当前Builder实例
         */
        public Builder resultCacheExpireSeconds(int resultCacheExpireSeconds) {
            this.resultCacheExpireSeconds = resultCacheExpireSeconds;
            return this;
        }
        
        /**
         * 构建JrdbConfig实例。
         * 
         * <p>构建时会自动验证配置项的有效性。</p>
         * 
         * @return 新构建的JrdbConfig实例
         * @throws NullPointerException 当内存池配置为空时抛出
         * @throws IllegalArgumentException 当缓存大小配置为负数时抛出
         */
        public JrdbConfig build() { return new JrdbConfig(this); }
    }
}