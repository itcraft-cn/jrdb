package cn.itcraft.jrdb.jdbc.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 查询结果缓存
 *
 * <p>基于 Guava Cache 实现的查询结果缓存，用于缓存数据库查询结果以提升查询性能。</p>
 *
 * <p>主要特性：</p>
 * <ul>
 *   <li>基于 LRU 算法的最大容量限制</li>
 *   <li>写入后过期策略</li>
 *   <li>线程安全</li>
 * </ul>
 *
 * <p>缓存键由 SQL 语句和参数组合生成，确保相同查询条件的请求可以命中缓存。</p>
 *
 * @author jrdb
 * @see CachedQueryResult
 * @see Cache
 */
public class QueryResultCache {

    /**
     * 底层缓存实例
     *
     * <p>使用 Guava Cache 实现，支持最大容量限制和过期时间配置。</p>
     */
    private final Cache<String, CachedQueryResult> cache;

    /**
     * 构造查询结果缓存实例
     *
     * @param maxSize 缓存最大容量，当缓存条目数超过此值时，采用 LRU 算法淘汰旧条目
     * @param expireSeconds 写入后过期时间（秒），缓存条目在写入后经过此时间将自动失效
     */
    public QueryResultCache(int maxSize, int expireSeconds) {
        this.cache = CacheBuilder.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
            .build();
    }

    /**
     * 从缓存中获取查询结果
     *
     * <p>根据 SQL 语句和参数生成缓存键，尝试从缓存中获取对应的查询结果。
     * 如果缓存中不存在，返回 {@code null}。</p>
     *
     * @param sql SQL 查询语句
     * @param params 查询参数数组，可为 null 或空数组
     * @return 缓存的查询结果，如果不存在则返回 {@code null}
     */
    public CachedQueryResult get(String sql, Object[] params) {
        String key = buildKey(sql, params);
        return cache.getIfPresent(key);
    }

    /**
     * 将查询结果存入缓存
     *
     * <p>根据 SQL 语句和参数生成缓存键，将查询结果存入缓存。
     * 如果缓存键已存在，将覆盖原有值。</p>
     *
     * @param sql SQL 查询语句
     * @param params 查询参数数组，可为 null 或空数组
     * @param result 要缓存的查询结果
     */
    public void put(String sql, Object[] params, CachedQueryResult result) {
        String key = buildKey(sql, params);
        cache.put(key, result);
    }

    /**
     * 使指定查询的缓存失效
     *
     * <p>根据 SQL 语句和参数生成缓存键，从缓存中移除对应的查询结果。</p>
     *
     * @param sql SQL 查询语句
     * @param params 查询参数数组，可为 null 或空数组
     */
    public void invalidate(String sql, Object[] params) {
        String key = buildKey(sql, params);
        cache.invalidate(key);
    }

    /**
     * 使所有缓存失效
     *
     * <p>清空缓存中的所有条目，通常在数据发生批量变更时调用。</p>
     */
    public void invalidateAll() {
        cache.invalidateAll();
    }

    /**
     * 获取当前缓存大小
     *
     * @return 缓存中的条目数量
     */
    public long size() {
        return cache.size();
    }

    /**
     * 构建缓存键
     *
     * <p>根据 SQL 语句和参数生成唯一的缓存键。键的生成规则如下：</p>
     * <ul>
     *   <li>无参数时，直接使用 SQL 语句作为键</li>
     *   <li>有参数时，使用 "SQL:参数哈希值" 格式作为键</li>
     * </ul>
     *
     * @param sql SQL 查询语句
     * @param params 查询参数数组，可为 null 或空数组
     * @return 生成的缓存键
     */
    private String buildKey(String sql, Object[] params) {
        if (params == null || params.length == 0) {
            return sql;
        }
        return new StringBuilder(sql.length() + 12).append(sql).append(':').append(Arrays.hashCode(params)).toString();
    }
}