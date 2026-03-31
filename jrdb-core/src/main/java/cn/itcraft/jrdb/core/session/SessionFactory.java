package cn.itcraft.jrdb.core.session;

import cn.itcraft.jrdb.core.config.JrdbConfig;
import cn.itcraft.jrdb.core.memory.MemoryPool;
import cn.itcraft.jrdb.core.memory.MemoryPoolConfig;

import javax.sql.DataSource;

public class SessionFactory {
    
    private final JrdbConfig config;
    private final MemoryPool memoryPool;
    private final DataSource dataSource;
    private volatile boolean closed;
    
    public SessionFactory(JrdbConfig config) {
        this(config, null);
    }
    
    public SessionFactory(JrdbConfig config, DataSource dataSource) {
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