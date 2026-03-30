package com.dbmmap.core.config;

import com.dbmmap.core.memory.MemoryPoolConfig;

public class DbMmapConfig {
    
    private final MemoryPoolConfig memoryPoolConfig;
    private final OverflowStrategy overflowStrategy;
    
    private DbMmapConfig(Builder builder) {
        this.memoryPoolConfig = builder.memoryPoolConfig;
        this.overflowStrategy = builder.overflowStrategy;
        validate();
    }
    
    private void validate() {
        if (memoryPoolConfig == null) {
            throw new NullPointerException("memoryPoolConfig cannot be null");
        }
    }
    
    public MemoryPoolConfig getMemoryPoolConfig() { return memoryPoolConfig; }
    public OverflowStrategy getOverflowStrategy() { return overflowStrategy; }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private MemoryPoolConfig memoryPoolConfig = MemoryPoolConfig.builder().build();
        private OverflowStrategy overflowStrategy = OverflowStrategy.ERROR;
        
        public Builder memoryPoolConfig(MemoryPoolConfig memoryPoolConfig) {
            this.memoryPoolConfig = memoryPoolConfig;
            return this;
        }
        
        public Builder overflowStrategy(OverflowStrategy overflowStrategy) {
            this.overflowStrategy = overflowStrategy;
            return this;
        }
        
        public DbMmapConfig build() { return new DbMmapConfig(this); }
    }
}