package cn.itcraft.jrdb.springboot.autoconfigure;

import cn.itcraft.jrdb.core.config.CacheLevel;
import cn.itcraft.jrdb.core.config.OverflowStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jrdb")
public class JrdbProperties {

    private boolean enabled = true;

    private OverflowStrategy overflowStrategy = OverflowStrategy.ERROR;

    private MemoryPool memoryPool = new MemoryPool();

    private Cache cache = new Cache();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public OverflowStrategy getOverflowStrategy() { return overflowStrategy; }
    public void setOverflowStrategy(OverflowStrategy overflowStrategy) { this.overflowStrategy = overflowStrategy; }

    public MemoryPool getMemoryPool() { return memoryPool; }
    public void setMemoryPool(MemoryPool memoryPool) { this.memoryPool = memoryPool; }

    public Cache getCache() { return cache; }
    public void setCache(Cache cache) { this.cache = cache; }

    public static class MemoryPool {
        private long preAllocMemSize = 64 * 1024 * 1024;
        private long queryCacheSize = 4 * 1024 * 1024;

        public long getPreAllocMemSize() { return preAllocMemSize; }
        public void setPreAllocMemSize(long preAllocMemSize) { this.preAllocMemSize = preAllocMemSize; }

        public long getQueryCacheSize() { return queryCacheSize; }
        public void setQueryCacheSize(long queryCacheSize) { this.queryCacheSize = queryCacheSize; }
    }

    public static class Cache {
        private CacheLevel level = CacheLevel.L1;
        private int statementCacheSize = 256;
        private int resultCacheSize = 1000;
        private int resultCacheExpireSeconds = 300;

        public CacheLevel getLevel() { return level; }
        public void setLevel(CacheLevel level) { this.level = level; }

        public int getStatementCacheSize() { return statementCacheSize; }
        public void setStatementCacheSize(int statementCacheSize) { this.statementCacheSize = statementCacheSize; }

        public int getResultCacheSize() { return resultCacheSize; }
        public void setResultCacheSize(int resultCacheSize) { this.resultCacheSize = resultCacheSize; }

        public int getResultCacheExpireSeconds() { return resultCacheExpireSeconds; }
        public void setResultCacheExpireSeconds(int resultCacheExpireSeconds) { this.resultCacheExpireSeconds = resultCacheExpireSeconds; }
    }
}