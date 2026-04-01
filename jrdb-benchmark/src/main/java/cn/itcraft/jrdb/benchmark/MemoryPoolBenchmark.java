package cn.itcraft.jrdb.benchmark;

import cn.itcraft.jrdb.jdbc.memory.MemoryBlock;
import cn.itcraft.jrdb.jdbc.memory.MemoryPool;
import cn.itcraft.jrdb.jdbc.memory.MemoryPoolConfig;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class MemoryPoolBenchmark {

    private MemoryPool pooledPool;
    private MemoryPool nonPooledPool;

    @Setup(Level.Trial)
    public void setup() {
        MemoryPoolConfig pooledConfig = MemoryPoolConfig.builder()
            .preAllocMemSize(64 * 1024 * 1024)
            .queryCacheSize(4 * 1024 * 1024)
            .build();

        MemoryPoolConfig nonPooledConfig = MemoryPoolConfig.builder()
            .preAllocMemSize(64 * 1024 * 1024)
            .queryCacheSize(64 * 1024)
            .build();

        pooledPool = new MemoryPool(pooledConfig);
        nonPooledPool = new MemoryPool(nonPooledConfig);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (pooledPool != null) pooledPool.destroy();
        if (nonPooledPool != null) nonPooledPool.destroy();
    }

    @Benchmark
    public void pooledAllocateRelease(Blackhole bh) {
        MemoryBlock block = pooledPool.allocate();
        bh.consume(block);
        pooledPool.release(block);
    }

    @Benchmark
    public void pooledAllocateWriteRelease(Blackhole bh) {
        MemoryBlock block = pooledPool.allocate();
        block.writeInt(0, 12345);
        block.writeLong(4, 987654321L);
        bh.consume(block.readInt(0));
        bh.consume(block.readLong(4));
        pooledPool.release(block);
    }

    @Benchmark
    public void smallSlotAllocateRelease(Blackhole bh) {
        MemoryBlock block = nonPooledPool.allocate();
        bh.consume(block);
        nonPooledPool.release(block);
    }

    @Benchmark
    public void pooledSequentialAccess(Blackhole bh) {
        MemoryBlock block = pooledPool.allocate();
        for (int i = 0; i < 1000; i += 8) {
            block.writeLong(i, i);
            bh.consume(block.readLong(i));
        }
        pooledPool.release(block);
    }

    @Benchmark
    public void pooledBatchAllocate(Blackhole bh) {
        MemoryBlock[] blocks = new MemoryBlock[16];
        for (int i = 0; i < 16; i++) {
            blocks[i] = pooledPool.allocate();
            blocks[i].writeInt(0, i);
        }
        for (int i = 0; i < 16; i++) {
            bh.consume(blocks[i].readInt(0));
            pooledPool.release(blocks[i]);
        }
    }
}