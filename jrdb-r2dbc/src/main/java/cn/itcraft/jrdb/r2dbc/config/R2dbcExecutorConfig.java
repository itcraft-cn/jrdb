package cn.itcraft.jrdb.r2dbc.config;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class R2dbcExecutorConfig {

    private int corePoolSize = Runtime.getRuntime().availableProcessors();
    private int maxPoolSize = Runtime.getRuntime().availableProcessors() * 2;
    private int queueCapacity = 1000;
    private RejectionPolicy rejectionPolicy = RejectionPolicy.CALLER_RUNS;
    private String threadNamePrefix = "jrdb-r2dbc-";

    public enum RejectionPolicy {
        ABORT,
        CALLER_RUNS,
        DISCARD,
        DISCARD_OLDEST
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public R2dbcExecutorConfig corePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
        return this;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public R2dbcExecutorConfig maxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
        return this;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public R2dbcExecutorConfig queueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
        return this;
    }

    public RejectionPolicy getRejectionPolicy() {
        return rejectionPolicy;
    }

    public R2dbcExecutorConfig rejectionPolicy(RejectionPolicy rejectionPolicy) {
        this.rejectionPolicy = rejectionPolicy;
        return this;
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    public R2dbcExecutorConfig threadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
        return this;
    }

    public ThreadFactory createThreadFactory() {
        return new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, threadNamePrefix + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };
    }
}