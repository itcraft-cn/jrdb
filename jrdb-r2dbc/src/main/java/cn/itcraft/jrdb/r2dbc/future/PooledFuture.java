package cn.itcraft.jrdb.r2dbc.future;

import cn.itcraft.frogspawn.ObjectCreator;
import cn.itcraft.frogspawn.ObjectsMemoryPool;
import cn.itcraft.frogspawn.ObjectsMemoryPoolFactory;
import cn.itcraft.frogspawn.Resettable;
import cn.itcraft.frogspawn.strategy.FetchFailStrategy;
import cn.itcraft.frogspawn.strategy.FetchStrategy;
import cn.itcraft.frogspawn.strategy.PoolStrategy;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PooledFuture implements Resettable {

    private static final int STATE_PENDING = 0;
    private static final int STATE_SUCCESS = 1;
    private static final int STATE_FAILURE = 2;
    private static final int STATE_RETURNED = 3;

    private int state = STATE_PENDING;
    private Object result;
    private Throwable exception;
    private int markedId;

    public static PooledFuture fetch() {
        return new PooledFuture();
    }

    @Override
    public void reset() {
        state = STATE_PENDING;
        result = null;
        exception = null;
    }

    @Override
    public boolean isInvalid() {
        return false;
    }

    @Override
    public int getMarkedId() {
        return markedId;
    }

    @Override
    public void markId(int id) {
        this.markedId = id;
    }

    public boolean complete(Object value) {
        if (state != STATE_PENDING) {
            return false;
        }
        this.result = value;
        this.state = STATE_SUCCESS;
        return true;
    }

    public boolean completeExceptionally(Throwable ex) {
        if (state != STATE_PENDING) {
            return false;
        }
        this.exception = ex;
        this.state = STATE_FAILURE;
        return true;
    }

    @SuppressWarnings("unchecked")
    public <T> T get() throws ExecutionException, InterruptedException {
        while (state == STATE_PENDING) {
            Thread.yield();
        }
        if (state == STATE_FAILURE) {
            throw new ExecutionException(exception);
        }
        return (T) result;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (state == STATE_PENDING) {
            if (System.nanoTime() > deadline) {
                throw new TimeoutException();
            }
            Thread.yield();
        }
        if (state == STATE_FAILURE) {
            throw new ExecutionException(exception);
        }
        return (T) result;
    }

    public boolean isDone() {
        return state == STATE_SUCCESS || state == STATE_FAILURE;
    }

    public boolean isCompletedExceptionally() {
        return state == STATE_FAILURE;
    }
}