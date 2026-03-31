package cn.itcraft.dbmmap.core.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class QueryResultCache {
    
    private final Cache<String, CachedQueryResult> cache;
    
    public QueryResultCache(int maxSize, int expireSeconds) {
        this.cache = CacheBuilder.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
            .build();
    }
    
    public CachedQueryResult get(String sql, Object[] params) {
        String key = buildKey(sql, params);
        return cache.getIfPresent(key);
    }
    
    public void put(String sql, Object[] params, CachedQueryResult result) {
        String key = buildKey(sql, params);
        cache.put(key, result);
    }
    
    public void invalidate(String sql, Object[] params) {
        String key = buildKey(sql, params);
        cache.invalidate(key);
    }
    
    public void invalidateAll() {
        cache.invalidateAll();
    }
    
    public long size() {
        return cache.size();
    }
    
    private String buildKey(String sql, Object[] params) {
        if (params == null || params.length == 0) {
            return sql;
        }
        return new StringBuilder(sql.length() + 12).append(sql).append(':').append(Arrays.hashCode(params)).toString();
    }
}