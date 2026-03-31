package cn.itcraft.dbmmap.core.session;

import cn.itcraft.dbmmap.core.config.DbMmapConfig;
import cn.itcraft.dbmmap.core.memory.MemoryPool;
import cn.itcraft.dbmmap.core.memory.MemoryPoolConfig;

import javax.sql.DataSource;

public class SessionFactory {
    
    private final DbMmapConfig config;
    private final MemoryPool memoryPool;
    private final DataSource dataSource;
    private volatile boolean closed;
    
    public SessionFactory(DbMmapConfig config) {
        this(config, null);
    }
    
    public SessionFactory(DbMmapConfig config, DataSource dataSource) {
        if (config == null) {
            throw new NullPointerException("config cannot be null");
        }
        this.config = config;
        this.dataSource = dataSource;
        MemoryPoolConfig poolConfig = config.getMemoryPoolConfig();
        this.memoryPool = new MemoryPool(poolConfig);
        this.closed = false;
    }
    
    public Session openSession() {
        if (closed) {
            throw new IllegalStateException("SessionFactory is closed");
        }
        if (dataSource == null) {
            throw new IllegalStateException("DataSource not configured");
        }
        return new SessionImpl(memoryPool, config, dataSource);
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
    
    public MemoryPool getMemoryPool() {
        return memoryPool;
    }
}