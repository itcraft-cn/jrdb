package cn.itcraft.jrdb.core.config;

import cn.itcraft.jrdb.core.memory.MemoryPoolConfig;

public class JrdbConfig {
    
    private final MemoryPoolConfig memoryPoolConfig;
    private final OverflowStrategy overflowStrategy;
    private final CacheLevel cacheLevel;
    private final int statementCacheSize;
    private final int resultCacheSize;
    private final int resultCacheExpireSeconds;
    
    private JrdbConfig(Builder builder) {
        this.memoryPoolConfig = builder.memoryPoolConfig;
        this.overflowStrategy = builder.overflowStrategy;
        this.cacheLevel = builder.cacheLevel;
        this.statementCacheSize = builder.statementCacheSize;
        this.resultCacheSize = builder.resultCacheSize;
        this.resultCacheExpireSeconds = builder.resultCacheExpireSeconds;
        validate();
    }
    
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
    
    public MemoryPoolConfig getMemoryPoolConfig() { return memoryPoolConfig; }
    public OverflowStrategy getOverflowStrategy() { return overflowStrategy; }
    public CacheLevel getCacheLevel() { return cacheLevel; }
    public int getStatementCacheSize() { return statementCacheSize; }
    public int getResultCacheSize() { return resultCacheSize; }
    public int getResultCacheExpireSeconds() { return resultCacheExpireSeconds; }
    
    public boolean isL1CacheEnabled() {
        return cacheLevel == CacheLevel.L1 || cacheLevel == CacheLevel.L2;
    }
    
    public boolean isL2CacheEnabled() {
        return cacheLevel == CacheLevel.L2;
    }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private MemoryPoolConfig memoryPoolConfig = MemoryPoolConfig.builder().build();
        private OverflowStrategy overflowStrategy = OverflowStrategy.ERROR;
        private CacheLevel cacheLevel = CacheLevel.L1;
        private int statementCacheSize = 256;
        private int resultCacheSize = 1000;
        private int resultCacheExpireSeconds = 300;
        
        public Builder memoryPoolConfig(MemoryPoolConfig memoryPoolConfig) {
            this.memoryPoolConfig = memoryPoolConfig;
            return this;
        }
        
        public Builder overflowStrategy(OverflowStrategy overflowStrategy) {
            this.overflowStrategy = overflowStrategy;
            return this;
        }
        
        public Builder cacheLevel(CacheLevel cacheLevel) {
            this.cacheLevel = cacheLevel;
            return this;
        }
        
        public Builder statementCacheSize(int statementCacheSize) {
            this.statementCacheSize = statementCacheSize;
            return this;
        }
        
        public Builder resultCacheSize(int resultCacheSize) {
            this.resultCacheSize = resultCacheSize;
            return this;
        }
        
        public Builder resultCacheExpireSeconds(int resultCacheExpireSeconds) {
            this.resultCacheExpireSeconds = resultCacheExpireSeconds;
            return this;
        }
        
        public JrdbConfig build() { return new JrdbConfig(this); }
    }
}