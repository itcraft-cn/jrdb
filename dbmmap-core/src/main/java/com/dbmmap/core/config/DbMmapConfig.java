package com.dbmmap.core.config;

import com.dbmmap.core.memory.MemoryPoolConfig;

public class DbMmapConfig {
    
    private final MemoryPoolConfig memoryPoolConfig;
    private final OverflowStrategy overflowStrategy;
    private final boolean statementCacheEnabled;
    private final boolean queryMetaCacheEnabled;
    private final int statementCacheSize;
    
    private DbMmapConfig(Builder builder) {
        this.memoryPoolConfig = builder.memoryPoolConfig;
        this.overflowStrategy = builder.overflowStrategy;
        this.statementCacheEnabled = builder.statementCacheEnabled;
        this.queryMetaCacheEnabled = builder.queryMetaCacheEnabled;
        this.statementCacheSize = builder.statementCacheSize;
        validate();
    }
    
    private void validate() {
        if (memoryPoolConfig == null) {
            throw new NullPointerException("memoryPoolConfig cannot be null");
        }
        if (statementCacheSize < 0) {
            throw new IllegalArgumentException("statementCacheSize must be non-negative");
        }
    }
    
    public MemoryPoolConfig getMemoryPoolConfig() { return memoryPoolConfig; }
    public OverflowStrategy getOverflowStrategy() { return overflowStrategy; }
    public boolean isStatementCacheEnabled() { return statementCacheEnabled; }
    public boolean isQueryMetaCacheEnabled() { return queryMetaCacheEnabled; }
    public int getStatementCacheSize() { return statementCacheSize; }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private MemoryPoolConfig memoryPoolConfig = MemoryPoolConfig.builder().build();
        private OverflowStrategy overflowStrategy = OverflowStrategy.ERROR;
        private boolean statementCacheEnabled = true;
        private boolean queryMetaCacheEnabled = true;
        private int statementCacheSize = 256;
        
        public Builder memoryPoolConfig(MemoryPoolConfig memoryPoolConfig) {
            this.memoryPoolConfig = memoryPoolConfig;
            return this;
        }
        
        public Builder overflowStrategy(OverflowStrategy overflowStrategy) {
            this.overflowStrategy = overflowStrategy;
            return this;
        }
        
        public Builder statementCacheEnabled(boolean statementCacheEnabled) {
            this.statementCacheEnabled = statementCacheEnabled;
            return this;
        }
        
        public Builder queryMetaCacheEnabled(boolean queryMetaCacheEnabled) {
            this.queryMetaCacheEnabled = queryMetaCacheEnabled;
            return this;
        }
        
        public Builder statementCacheSize(int statementCacheSize) {
            this.statementCacheSize = statementCacheSize;
            return this;
        }
        
        public DbMmapConfig build() { return new DbMmapConfig(this); }
    }
}