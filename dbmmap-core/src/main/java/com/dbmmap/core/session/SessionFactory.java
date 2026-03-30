package com.dbmmap.core.session;

import com.dbmmap.core.config.DbMmapConfig;
import com.dbmmap.core.memory.MemoryPool;
import com.dbmmap.core.memory.MemoryPoolConfig;

public class SessionFactory {
    
    private final DbMmapConfig config;
    private final MemoryPool memoryPool;
    private volatile boolean closed;
    
    public SessionFactory(DbMmapConfig config) {
        if (config == null) {
            throw new NullPointerException("config cannot be null");
        }
        this.config = config;
        MemoryPoolConfig poolConfig = config.getMemoryPoolConfig();
        this.memoryPool = new MemoryPool(poolConfig);
        this.closed = false;
    }
    
    public Session openSession() {
        if (closed) {
            throw new IllegalStateException("SessionFactory is closed");
        }
        return new SessionImpl(memoryPool, config);
    }
    
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        memoryPool.destroy();
    }
    
    public boolean isClosed() {
        return closed;
    }
}