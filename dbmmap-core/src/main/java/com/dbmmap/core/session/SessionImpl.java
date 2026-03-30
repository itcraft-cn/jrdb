package com.dbmmap.core.session;

import com.dbmmap.core.config.DbMmapConfig;
import com.dbmmap.core.memory.MemoryPool;
import com.dbmmap.core.query.QueryResult;
import com.dbmmap.core.query.QueryResultImpl;

import java.util.function.Function;

public class SessionImpl implements Session {
    
    private final MemoryPool memoryPool;
    private final DbMmapConfig config;
    private volatile boolean closed;
    
    public SessionImpl(MemoryPool memoryPool, DbMmapConfig config) {
        if (memoryPool == null) {
            throw new NullPointerException("memoryPool cannot be null");
        }
        if (config == null) {
            throw new NullPointerException("config cannot be null");
        }
        this.memoryPool = memoryPool;
        this.config = config;
        this.closed = false;
    }
    
    @Override
    public QueryResult query(String sql) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        return null;
    }
    
    @Override
    public <T> T query(String sql, Function<QueryResult, T> handler) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        QueryResult result = query(sql);
        if (result == null) {
            return null;
        }
        try {
            return handler.apply(result);
        } finally {
            result.close();
        }
    }
    
    @Override
    public int update(String sql) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        return 0;
    }
    
    @Override
    public int[] batchUpdate(String... sqls) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        int[] results = new int[sqls.length];
        for (int i = 0; i < sqls.length; i++) {
            results[i] = update(sqls[i]);
        }
        return results;
    }
    
    @Override
    public MemoryPool getMemoryPool() {
        return memoryPool;
    }
    
    @Override
    public boolean isClosed() {
        return closed;
    }
    
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
    }
}