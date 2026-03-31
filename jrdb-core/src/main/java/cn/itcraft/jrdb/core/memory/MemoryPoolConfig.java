package cn.itcraft.jrdb.core.memory;

public class MemoryPoolConfig {
    
    public static final long DEFAULT_PRE_ALLOC_MEM_SIZE = 256L * 1024 * 1024;
    public static final long DEFAULT_QUERY_CACHE_SIZE = 16L * 1024 * 1024;
    
    private final long preAllocMemSize;
    private final long queryCacheSize;
    
    private MemoryPoolConfig(Builder builder) {
        this.preAllocMemSize = builder.preAllocMemSize;
        this.queryCacheSize = builder.queryCacheSize;
        validate();
    }
    
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
    
    public long getPreAllocMemSize() { return preAllocMemSize; }
    public long getQueryCacheSize() { return queryCacheSize; }
    public int getSlotCount() { return (int) (preAllocMemSize / queryCacheSize); }
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private long preAllocMemSize = DEFAULT_PRE_ALLOC_MEM_SIZE;
        private long queryCacheSize = DEFAULT_QUERY_CACHE_SIZE;
        
        public Builder preAllocMemSize(long preAllocMemSize) {
            this.preAllocMemSize = preAllocMemSize;
            return this;
        }
        
        public Builder queryCacheSize(long queryCacheSize) {
            this.queryCacheSize = queryCacheSize;
            return this;
        }
        
        public MemoryPoolConfig build() { return new MemoryPoolConfig(this); }
    }
}