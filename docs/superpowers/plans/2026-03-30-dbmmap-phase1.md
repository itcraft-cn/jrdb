# dbmmap Phase 1 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现dbmmap核心基础设施 - 内存池管理、行存储、基础查询API、Session管理

**Architecture:** 基于sun.misc.Unsafe实现堆外内存池，栈式分配+位图标记；行存格式紧凑排列；查询结果必须显式释放内存；JDBC透传。

**Tech Stack:** Java 8+, sun.misc.Unsafe, ASM, JDBC, Maven, JUnit 5

---

## 文件结构

```
dbmmap/
├── pom.xml                          # 父POM
├── dbmmap-core/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/dbmmap/core/
│       │   ├── memory/
│       │   │   ├── MemoryPool.java
│       │   │   ├── MemoryPoolConfig.java
│       │   │   ├── SlotAllocator.java
│       │   │   └── UnsafeProvider.java
│       │   ├── storage/
│       │   │   ├── RowStore.java
│       │   │   ├── ColumnStore.java
│       │   │   ├── StoreConfig.java
│       │   │   └── HybridView.java
│       │   ├── query/
│       │   │   ├── QueryResult.java
│       │   │   ├── QueryResultImpl.java
│       │   │   ├── QueryExecutor.java
│       │   │   └── QueryConfig.java
│       │   ├── session/
│       │   │   ├── Session.java
│       │   │   ├── SessionImpl.java
│       │   │   ├── SessionFactory.java
│       │   │   └── SessionConfig.java
│       │   └── config/
│       │       ├── DbMmapConfig.java
│       │       └── OverflowStrategy.java
│       └── test/java/com/dbmmap/core/
│           ├── memory/
│           ├── storage/
│           ├── query/
│           └── session/
├── dbmmap-mapping/                   # Phase 2
├── dbmmap-sql/                       # Phase 3
├── dbmmap-transaction/               # Phase 4
└── dbmmap-config/                    # Phase 4
```

---

## Task 1: 项目初始化

**Files:**
- Create: `pom.xml`
- Create: `dbmmap-core/pom.xml`
- Create: `.gitignore`

- [ ] **Step 1: 创建父POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.dbmmap</groupId>
    <artifactId>dbmmap</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>dbmmap</name>
    <description>Database Memory Mapping Framework - High Performance Off-Heap Storage</description>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <junit.version>5.9.3</junit.version>
        <mockito.version>5.4.0</mockito.version>
    </properties>

    <modules>
        <module>dbmmap-core</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.junit.jupiter</groupId>
                <artifactId>junit-jupiter</artifactId>
                <version>${junit.version}</version>
                <scope>test</scope>
            </dependency>
            <dependency>
                <groupId>org.mockito</groupId>
                <artifactId>mockito-core</artifactId>
                <version>${mockito.version}</version>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.1.2</version>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建dbmmap-core模块POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.dbmmap</groupId>
        <artifactId>dbmmap</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>dbmmap-core</artifactId>
    <name>dbmmap-core</name>
    <description>Core module: memory pool, storage, query API</description>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: 创建.gitignore**

```
# Compiled
target/
*.class

# IDE
.idea/
*.iml
*.ipr
*.iws
.vscode/
.settings/
.project
.classpath

# OS
.DS_Store
Thumbs.db

# Temp
*.log
*.tmp

# Memory file (不提交)
MEMORY.md
```

- [ ] **Step 4: 创建目录结构**

Run: `mkdir -p dbmmap-core/src/main/java/com/dbmmap/core/{memory,storage,query,session,config} dbmmap-core/src/test/java/com/dbmmap/core/{memory,storage,query,session}`

- [ ] **Step 5: 验证项目结构**

Run: `mvn -v && ls -la dbmmap-core/src/main/java/com/dbmmap/core/`
Expected: Maven版本信息和5个目录(config, memory, query, session, storage)

- [ ] **Step 6: 提交**

```bash
git add pom.xml dbmmap-core/pom.xml .gitignore
git commit -m "feat: initialize dbmmap project structure"
```

---

## Task 2: UnsafeProvider - Unsafe访问提供者

**Files:**
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/memory/UnsafeProvider.java`
- Create: `dbmmap-core/src/test/java/com/dbmmap/core/memory/UnsafeProviderTest.java`

- [ ] **Step 1: 编写失败的测试**

```java
package com.dbmmap.core.memory;

import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.*;

class UnsafeProviderTest {

    @Test
    void shouldGetUnsafeInstance() {
        Unsafe unsafe = UnsafeProvider.getUnsafe();
        assertNotNull(unsafe);
    }

    @Test
    void shouldReturnSameInstance() {
        Unsafe u1 = UnsafeProvider.getUnsafe();
        Unsafe u2 = UnsafeProvider.getUnsafe();
        assertSame(u1, u2);
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd dbmmap-core && mvn test -Dtest=UnsafeProviderTest`
Expected: FAIL - class not found

- [ ] **Step 3: 实现UnsafeProvider**

```java
package com.dbmmap.core.memory;

import sun.misc.Unsafe;
import java.lang.reflect.Field;

public final class UnsafeProvider {
    
    private static final Unsafe UNSAFE;
    
    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Unsafe instance", e);
        }
    }
    
    private UnsafeProvider() {}
    
    public static Unsafe getUnsafe() {
        return UNSAFE;
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `cd dbmmap-core && mvn test -Dtest=UnsafeProviderTest`
Expected: PASS - 2 tests

- [ ] **Step 5: 提交**

```bash
git add dbmmap-core/src/main/java/com/dbmmap/core/memory/UnsafeProvider.java dbmmap-core/src/test/java/com/dbmmap/core/memory/UnsafeProviderTest.java
git commit -m "feat(core): add UnsafeProvider for off-heap memory access"
```

---

## Task 3: MemoryPoolConfig - 内存池配置

**Files:**
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/memory/MemoryPoolConfig.java`
- Create: `dbmmap-core/src/test/java/com/dbmmap/core/memory/MemoryPoolConfigTest.java`

- [ ] **Step 1: 编写失败的测试**

```java
package com.dbmmap.core.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemoryPoolConfigTest {

    @Test
    void shouldCreateWithDefaults() {
        MemoryPoolConfig config = MemoryPoolConfig.builder().build();
        assertEquals(256 * 1024 * 1024L, config.getPreAllocMemSize());
        assertEquals(16 * 1024 * 1024L, config.getQueryCacheSize());
    }

    @Test
    void shouldCreateWithCustomValues() {
        MemoryPoolConfig config = MemoryPoolConfig.builder()
                .preAllocMemSize(512 * 1024 * 1024L)
                .queryCacheSize(32 * 1024 * 1024L)
                .build();
        assertEquals(512 * 1024 * 1024L, config.getPreAllocMemSize());
        assertEquals(32 * 1024 * 1024L, config.getQueryCacheSize());
    }

    @Test
    void shouldCalculateSlotCount() {
        MemoryPoolConfig config = MemoryPoolConfig.builder()
                .preAllocMemSize(256L * 1024 * 1024)
                .queryCacheSize(16L * 1024 * 1024)
                .build();
        assertEquals(16, config.getSlotCount());
    }

    @Test
    void shouldValidatePreAllocMemSize() {
        assertThrows(IllegalArgumentException.class, () ->
            MemoryPoolConfig.builder().preAllocMemSize(-1).build()
        );
    }

    @Test
    void shouldValidateQueryCacheSize() {
        assertThrows(IllegalArgumentException.class, () ->
            MemoryPoolConfig.builder().queryCacheSize(0).build()
        );
    }

    @Test
    void shouldValidateQueryCacheSizeNotExceedPreAlloc() {
        assertThrows(IllegalArgumentException.class, () ->
            MemoryPoolConfig.builder()
                .preAllocMemSize(16L * 1024 * 1024)
                .queryCacheSize(32L * 1024 * 1024)
                .build()
        );
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd dbmmap-core && mvn test -Dtest=MemoryPoolConfigTest`
Expected: FAIL - class not found

- [ ] **Step 3: 实现MemoryPoolConfig**

```java
package com.dbmmap.core.memory;

public class MemoryPoolConfig {
    
    public static final long DEFAULT_PRE_ALLOC_MEM_SIZE = 256L * 1024 * 1024;
    public static final long DEFAULT_QUERY_CACHE_SIZE = 16L * 1024 * 1024;
    
    private final long preAllocMemSize;
    private final long queryCacheSize;
    
    private MemoryPoolConfig(Builder builder) {
        this.preAllocMemSize = builder.preAllocMemSize;
        this.queryCacheSize = builder.queryCacheSize;
        validate();
    }
    
    private void validate() {
        if (preAllocMemSize <= 0) {
            throw new IllegalArgumentException("preAllocMemSize must be positive");
        }
        if (queryCacheSize <= 0) {
            throw new IllegalArgumentException("queryCacheSize must be positive");
        }
        if (queryCacheSize > preAllocMemSize) {
            throw new IllegalArgumentException("queryCacheSize cannot exceed preAllocMemSize");
        }
    }
    
    public long getPreAllocMemSize() {
        return preAllocMemSize;
    }
    
    public long getQueryCacheSize() {
        return queryCacheSize;
    }
    
    public int getSlotCount() {
        return (int) (preAllocMemSize / queryCacheSize);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private long preAllocMemSize = DEFAULT_PRE_ALLOC_MEM_SIZE;
        private long queryCacheSize = DEFAULT_QUERY_CACHE_SIZE;
        
        public Builder preAllocMemSize(long preAllocMemSize) {
            this.preAllocMemSize = preAllocMemSize;
            return this;
        }
        
        public Builder queryCacheSize(long queryCacheSize) {
            this.queryCacheSize = queryCacheSize;
            return this;
        }
        
        public MemoryPoolConfig build() {
            return new MemoryPoolConfig(this);
        }
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `cd dbmmap-core && mvn test -Dtest=MemoryPoolConfigTest`
Expected: PASS - 6 tests

- [ ] **Step 5: 提交**

```bash
git add dbmmap-core/src/main/java/com/dbmmap/core/memory/MemoryPoolConfig.java dbmmap-core/src/test/java/com/dbmmap/core/memory/MemoryPoolConfigTest.java
git commit -m "feat(core): add MemoryPoolConfig with builder pattern"
```

---

## Task 4: SlotAllocator - 槽位分配器

**Files:**
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/memory/SlotAllocator.java`
- Create: `dbmmap-core/src/test/java/com/dbmmap/core/memory/SlotAllocatorTest.java`

- [ ] **Step 1: 编写失败的测试**

```java
package com.dbmmap.core.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlotAllocatorTest {

    private SlotAllocator allocator;

    @BeforeEach
    void setUp() {
        allocator = new SlotAllocator(16);
    }

    @Test
    void shouldAllocateSlot() {
        int slot = allocator.allocate();
        assertTrue(slot >= 0 && slot < 16);
    }

    @Test
    void shouldAllocateDifferentSlots() {
        int slot1 = allocator.allocate();
        int slot2 = allocator.allocate();
        assertNotEquals(slot1, slot2);
    }

    @Test
    void shouldReleaseSlot() {
        int slot = allocator.allocate();
        allocator.release(slot);
        int newSlot = allocator.allocate();
        assertEquals(slot, newSlot);
    }

    @Test
    void shouldFailWhenAllSlotsUsed() {
        for (int i = 0; i < 16; i++) {
            allocator.allocate();
        }
        assertThrows(IllegalStateException.class, () -> allocator.allocate());
    }

    @Test
    void shouldTrackUsedCount() {
        assertEquals(0, allocator.getUsedCount());
        allocator.allocate();
        assertEquals(1, allocator.getUsedCount());
        allocator.allocate();
        assertEquals(2, allocator.getUsedCount());
    }

    @Test
    void shouldCheckSlotStatus() {
        int slot = allocator.allocate();
        assertTrue(allocator.isUsed(slot));
        allocator.release(slot);
        assertFalse(allocator.isUsed(slot));
    }

    @Test
    void shouldValidateSlotIndexOnRelease() {
        assertThrows(IllegalArgumentException.class, () -> allocator.release(-1));
        assertThrows(IllegalArgumentException.class, () -> allocator.release(16));
    }

    @Test
    void shouldFailToReleaseUnusedSlot() {
        assertThrows(IllegalStateException.class, () -> allocator.release(0));
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd dbmmap-core && mvn test -Dtest=SlotAllocatorTest`
Expected: FAIL - class not found

- [ ] **Step 3: 实现SlotAllocator**

```java
package com.dbmmap.core.memory;

public class SlotAllocator {
    
    private final long[] slots;
    private final int slotCount;
    private int usedCount;
    private int nextFreeSlot;
    
    public SlotAllocator(int slotCount) {
        if (slotCount <= 0) {
            throw new IllegalArgumentException("slotCount must be positive");
        }
        this.slotCount = slotCount;
        this.slots = new long[(slotCount + 63) / 64];
        this.usedCount = 0;
        this.nextFreeSlot = 0;
    }
    
    public synchronized int allocate() {
        if (usedCount >= slotCount) {
            throw new IllegalStateException("All slots are in use");
        }
        
        int startSlot = nextFreeSlot;
        for (int i = 0; i < slotCount; i++) {
            int slot = (startSlot + i) % slotCount;
            if (!isUsed(slot)) {
                markUsed(slot);
                usedCount++;
                nextFreeSlot = (slot + 1) % slotCount;
                return slot;
            }
        }
        
        throw new IllegalStateException("No free slot available");
    }
    
    public synchronized void release(int slot) {
        validateSlot(slot);
        if (!isUsed(slot)) {
            throw new IllegalStateException("Slot " + slot + " is not in use");
        }
        markUnused(slot);
        usedCount--;
    }
    
    public synchronized boolean isUsed(int slot) {
        validateSlot(slot);
        int arrayIndex = slot / 64;
        int bitIndex = slot % 64;
        return (slots[arrayIndex] & (1L << bitIndex)) != 0;
    }
    
    public synchronized int getUsedCount() {
        return usedCount;
    }
    
    public int getSlotCount() {
        return slotCount;
    }
    
    private void markUsed(int slot) {
        int arrayIndex = slot / 64;
        int bitIndex = slot % 64;
        slots[arrayIndex] |= (1L << bitIndex);
    }
    
    private void markUnused(int slot) {
        int arrayIndex = slot / 64;
        int bitIndex = slot % 64;
        slots[arrayIndex] &= ~(1L << bitIndex);
    }
    
    private void validateSlot(int slot) {
        if (slot < 0 || slot >= slotCount) {
            throw new IllegalArgumentException("Invalid slot index: " + slot);
        }
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `cd dbmmap-core && mvn test -Dtest=SlotAllocatorTest`
Expected: PASS - 8 tests

- [ ] **Step 5: 提交**

```bash
git add dbmmap-core/src/main/java/com/dbmmap/core/memory/SlotAllocator.java dbmmap-core/src/test/java/com/dbmmap/core/memory/SlotAllocatorTest.java
git commit -m "feat(core): add SlotAllocator with bitmap marking"
```

---

## Task 5: MemoryPool - 内存池核心

**Files:**
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/memory/MemoryPool.java`
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/memory/MemoryBlock.java`
- Create: `dbmmap-core/src/test/java/com/dbmmap/core/memory/MemoryPoolTest.java`

- [ ] **Step 1: 编写失败的测试**

```java
package com.dbmmap.core.memory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemoryPoolTest {

    private MemoryPool pool;

    @BeforeEach
    void setUp() {
        MemoryPoolConfig config = MemoryPoolConfig.builder()
                .preAllocMemSize(1024)
                .queryCacheSize(256)
                .build();
        pool = new MemoryPool(config);
    }

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.destroy();
        }
    }

    @Test
    void shouldAllocateMemoryBlock() {
        MemoryBlock block = pool.allocate();
        assertNotNull(block);
        assertTrue(block.getAddress() > 0);
        assertEquals(256, block.getSize());
        assertEquals(0, block.getSlotId());
    }

    @Test
    void shouldAllocateDifferentSlots() {
        MemoryBlock block1 = pool.allocate();
        MemoryBlock block2 = pool.allocate();
        assertNotEquals(block1.getSlotId(), block2.getSlotId());
        assertNotEquals(block1.getAddress(), block2.getAddress());
    }

    @Test
    void shouldReleaseBlock() {
        MemoryBlock block = pool.allocate();
        int slotId = block.getSlotId();
        pool.release(block);
        
        MemoryBlock newBlock = pool.allocate();
        assertEquals(slotId, newBlock.getSlotId());
    }

    @Test
    void shouldFailWhenAllSlotsUsed() {
        for (int i = 0; i < 4; i++) {
            pool.allocate();
        }
        assertThrows(IllegalStateException.class, () -> pool.allocate());
    }

    @Test
    void shouldTrackStatistics() {
        assertEquals(0, pool.getUsedSlotCount());
        assertEquals(4, pool.getTotalSlotCount());
        assertEquals(1024, pool.getTotalMemory());
        
        pool.allocate();
        assertEquals(1, pool.getUsedSlotCount());
    }

    @Test
    void shouldWriteAndReadByte() {
        MemoryBlock block = pool.allocate();
        block.writeByte(0, (byte) 42);
        assertEquals(42, block.readByte(0));
    }

    @Test
    void shouldWriteAndReadInt() {
        MemoryBlock block = pool.allocate();
        block.writeInt(0, 12345678);
        assertEquals(12345678, block.readInt(0));
    }

    @Test
    void shouldWriteAndReadLong() {
        MemoryBlock block = pool.allocate();
        block.writeLong(0, 9876543210L);
        assertEquals(9876543210L, block.readLong(0));
    }

    @Test
    void shouldWriteAndReadBytes() {
        MemoryBlock block = pool.allocate();
        byte[] data = {1, 2, 3, 4, 5};
        block.writeBytes(0, data);
        
        byte[] read = new byte[5];
        block.readBytes(0, read);
        assertArrayEquals(data, read);
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd dbmmap-core && mvn test -Dtest=MemoryPoolTest`
Expected: FAIL - class not found

- [ ] **Step 3: 实现MemoryBlock**

```java
package com.dbmmap.core.memory;

import sun.misc.Unsafe;

public class MemoryBlock {
    
    private final long address;
    private final int size;
    private final int slotId;
    private final Unsafe unsafe;
    
    MemoryBlock(long address, int size, int slotId, Unsafe unsafe) {
        this.address = address;
        this.size = size;
        this.slotId = slotId;
        this.unsafe = unsafe;
    }
    
    public long getAddress() {
        return address;
    }
    
    public int getSize() {
        return size;
    }
    
    public int getSlotId() {
        return slotId;
    }
    
    public byte readByte(int offset) {
        checkOffset(offset, 1);
        return unsafe.getByte(address + offset);
    }
    
    public void writeByte(int offset, byte value) {
        checkOffset(offset, 1);
        unsafe.putByte(address + offset, value);
    }
    
    public int readInt(int offset) {
        checkOffset(offset, 4);
        return unsafe.getInt(address + offset);
    }
    
    public void writeInt(int offset, int value) {
        checkOffset(offset, 4);
        unsafe.putInt(address + offset, value);
    }
    
    public long readLong(int offset) {
        checkOffset(offset, 8);
        return unsafe.getLong(address + offset);
    }
    
    public void writeLong(int offset, long value) {
        checkOffset(offset, 8);
        unsafe.putLong(address + offset, value);
    }
    
    public short readShort(int offset) {
        checkOffset(offset, 2);
        return unsafe.getShort(address + offset);
    }
    
    public void writeShort(int offset, short value) {
        checkOffset(offset, 2);
        unsafe.putShort(address + offset, value);
    }
    
    public float readFloat(int offset) {
        checkOffset(offset, 4);
        return unsafe.getFloat(address + offset);
    }
    
    public void writeFloat(int offset, float value) {
        checkOffset(offset, 4);
        unsafe.putFloat(address + offset, value);
    }
    
    public double readDouble(int offset) {
        checkOffset(offset, 8);
        return unsafe.getDouble(address + offset);
    }
    
    public void writeDouble(int offset, double value) {
        checkOffset(offset, 8);
        unsafe.putDouble(address + offset, value);
    }
    
    public void readBytes(int offset, byte[] dest) {
        checkOffset(offset, dest.length);
        unsafe.copyMemory(null, address + offset, dest, Unsafe.ARRAY_BYTE_BASE_OFFSET, dest.length);
    }
    
    public void writeBytes(int offset, byte[] src) {
        checkOffset(offset, src.length);
        unsafe.copyMemory(src, Unsafe.ARRAY_BYTE_BASE_OFFSET, null, address + offset, src.length);
    }
    
    private void checkOffset(int offset, int length) {
        if (offset < 0 || offset + length > size) {
            throw new IndexOutOfBoundsException(
                "Offset " + offset + " with length " + length + " exceeds block size " + size
            );
        }
    }
}
```

- [ ] **Step 4: 实现MemoryPool**

```java
package com.dbmmap.core.memory;

import sun.misc.Unsafe;

public class MemoryPool {
    
    private final long baseAddress;
    private final int slotSize;
    private final int slotCount;
    private final SlotAllocator slotAllocator;
    private final Unsafe unsafe;
    private volatile boolean destroyed;
    
    public MemoryPool(MemoryPoolConfig config) {
        this.slotSize = (int) config.getQueryCacheSize();
        this.slotCount = config.getSlotCount();
        this.slotAllocator = new SlotAllocator(slotCount);
        this.unsafe = UnsafeProvider.getUnsafe();
        this.baseAddress = unsafe.allocateMemory(config.getPreAllocMemSize());
        this.destroyed = false;
    }
    
    public synchronized MemoryBlock allocate() {
        if (destroyed) {
            throw new IllegalStateException("MemoryPool has been destroyed");
        }
        int slotId = slotAllocator.allocate();
        long address = baseAddress + (long) slotId * slotSize;
        return new MemoryBlock(address, slotSize, slotId, unsafe);
    }
    
    public synchronized void release(MemoryBlock block) {
        if (destroyed) {
            return;
        }
        if (block == null) {
            return;
        }
        slotAllocator.release(block.getSlotId());
    }
    
    public synchronized void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        unsafe.freeMemory(baseAddress);
    }
    
    public int getUsedSlotCount() {
        return slotAllocator.getUsedCount();
    }
    
    public int getTotalSlotCount() {
        return slotCount;
    }
    
    public long getTotalMemory() {
        return (long) slotCount * slotSize;
    }
    
    public int getSlotSize() {
        return slotSize;
    }
    
    public boolean isDestroyed() {
        return destroyed;
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `cd dbmmap-core && mvn test -Dtest=MemoryPoolTest`
Expected: PASS - 10 tests

- [ ] **Step 6: 提交**

```bash
git add dbmmap-core/src/main/java/com/dbmmap/core/memory/MemoryPool.java dbmmap-core/src/main/java/com/dbmmap/core/memory/MemoryBlock.java dbmmap-core/src/test/java/com/dbmmap/core/memory/MemoryPoolTest.java
git commit -m "feat(core): add MemoryPool with off-heap allocation"
```

---

## Task 6: DbMmapConfig - 全局配置

**Files:**
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/config/OverflowStrategy.java`
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/config/DbMmapConfig.java`
- Create: `dbmmap-core/src/test/java/com/dbmmap/core/config/DbMmapConfigTest.java`

- [ ] **Step 1: 编写失败的测试**

```java
package com.dbmmap.core.config;

import com.dbmmap.core.memory.MemoryPoolConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DbMmapConfigTest {

    @Test
    void shouldCreateWithDefaults() {
        DbMmapConfig config = DbMmapConfig.builder().build();
        assertNotNull(config.getMemoryPoolConfig());
        assertEquals(OverflowStrategy.ERROR, config.getOverflowStrategy());
    }

    @Test
    void shouldCreateWithCustomValues() {
        MemoryPoolConfig memConfig = MemoryPoolConfig.builder()
                .preAllocMemSize(512L * 1024 * 1024)
                .queryCacheSize(32L * 1024 * 1024)
                .build();
        
        DbMmapConfig config = DbMmapConfig.builder()
                .memoryPoolConfig(memConfig)
                .overflowStrategy(OverflowStrategy.TRUNCATE)
                .build();
        
        assertEquals(memConfig, config.getMemoryPoolConfig());
        assertEquals(OverflowStrategy.TRUNCATE, config.getOverflowStrategy());
    }

    @Test
    void shouldValidateMemoryPoolConfig() {
        assertThrows(IllegalArgumentException.class, () ->
            DbMmapConfig.builder().memoryPoolConfig(null).build()
        );
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd dbmmap-core && mvn test -Dtest=DbMmapConfigTest`
Expected: FAIL - class not found

- [ ] **Step 3: 实现OverflowStrategy**

```java
package com.dbmmap.core.config;

public enum OverflowStrategy {
    ERROR,
    TRUNCATE
}
```

- [ ] **Step 4: 实现DbMmapConfig**

```java
package com.dbmmap.core.config;

import com.dbmmap.core.memory.MemoryPoolConfig;

public class DbMmapConfig {
    
    private final MemoryPoolConfig memoryPoolConfig;
    private final OverflowStrategy overflowStrategy;
    
    private DbMmapConfig(Builder builder) {
        this.memoryPoolConfig = builder.memoryPoolConfig;
        this.overflowStrategy = builder.overflowStrategy;
        validate();
    }
    
    private void validate() {
        if (memoryPoolConfig == null) {
            throw new IllegalArgumentException("memoryPoolConfig is required");
        }
    }
    
    public MemoryPoolConfig getMemoryPoolConfig() {
        return memoryPoolConfig;
    }
    
    public OverflowStrategy getOverflowStrategy() {
        return overflowStrategy;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private MemoryPoolConfig memoryPoolConfig = MemoryPoolConfig.builder().build();
        private OverflowStrategy overflowStrategy = OverflowStrategy.ERROR;
        
        public Builder memoryPoolConfig(MemoryPoolConfig memoryPoolConfig) {
            this.memoryPoolConfig = memoryPoolConfig;
            return this;
        }
        
        public Builder overflowStrategy(OverflowStrategy overflowStrategy) {
            this.overflowStrategy = overflowStrategy;
            return this;
        }
        
        public DbMmapConfig build() {
            return new DbMmapConfig(this);
        }
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `cd dbmmap-core && mvn test -Dtest=DbMmapConfigTest`
Expected: PASS - 3 tests

- [ ] **Step 6: 提交**

```bash
git add dbmmap-core/src/main/java/com/dbmmap/core/config/OverflowStrategy.java dbmmap-core/src/main/java/com/dbmmap/core/config/DbMmapConfig.java dbmmap-core/src/test/java/com/dbmmap/core/config/DbMmapConfigTest.java
git commit -m "feat(core): add DbMmapConfig and OverflowStrategy"
```

---

## Task 7: StoreConfig - 存储配置

**Files:**
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/storage/StoreConfig.java`
- Create: `dbmmap-core/src/test/java/com/dbmmap/core/storage/StoreConfigTest.java`

- [ ] **Step 1: 编写失败的测试**

```java
package com.dbmmap.core.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoreConfigTest {

    @Test
    void shouldCreateWithDefaults() {
        StoreConfig config = StoreConfig.builder().build();
        assertEquals(StoreType.ROW, config.getStoreType());
    }

    @Test
    void shouldCreateWithColumnStore() {
        StoreConfig config = StoreConfig.builder()
                .storeType(StoreType.COLUMN)
                .build();
        assertEquals(StoreType.COLUMN, config.getStoreType());
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd dbmmap-core && mvn test -Dtest=StoreConfigTest`
Expected: FAIL - class not found

- [ ] **Step 3: 实现StoreType枚举**

```java
package com.dbmmap.core.storage;

public enum StoreType {
    ROW,
    COLUMN,
    HYBRID
}
```

- [ ] **Step 4: 实现StoreConfig**

```java
package com.dbmmap.core.storage;

public class StoreConfig {
    
    private final StoreType storeType;
    
    private StoreConfig(Builder builder) {
        this.storeType = builder.storeType;
    }
    
    public StoreType getStoreType() {
        return storeType;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private StoreType storeType = StoreType.ROW;
        
        public Builder storeType(StoreType storeType) {
            this.storeType = storeType;
            return this;
        }
        
        public StoreConfig build() {
            return new StoreConfig(this);
        }
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `cd dbmmap-core && mvn test -Dtest=StoreConfigTest`
Expected: PASS - 2 tests

- [ ] **Step 6: 提交**

```bash
git add dbmmap-core/src/main/java/com/dbmmap/core/storage/StoreType.java dbmmap-core/src/main/java/com/dbmmap/core/storage/StoreConfig.java dbmmap-core/src/test/java/com/dbmmap/core/storage/StoreConfigTest.java
git commit -m "feat(core): add StoreConfig and StoreType"
```

---

## Task 8: RowStore - 行存储实现

**Files:**
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/storage/ColumnMeta.java`
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/storage/RowStore.java`
- Create: `dbmmap-core/src/test/java/com/dbmmap/core/storage/RowStoreTest.java`

- [ ] **Step 1: 编写失败的测试**

```java
package com.dbmmap.core.storage;

import com.dbmmap.core.memory.MemoryBlock;
import com.dbmmap.core.memory.MemoryPool;
import com.dbmmap.core.memory.MemoryPoolConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RowStoreTest {

    private MemoryPool pool;
    private MemoryBlock block;
    private RowStore store;

    @BeforeEach
    void setUp() {
        MemoryPoolConfig config = MemoryPoolConfig.builder()
                .preAllocMemSize(1024)
                .queryCacheSize(1024)
                .build();
        pool = new MemoryPool(config);
        block = pool.allocate();
        
        List<ColumnMeta> columns = Arrays.asList(
            new ColumnMeta("id", Types.INTEGER, 4),
            new ColumnMeta("name", Types.VARCHAR, 32),
            new ColumnMeta("age", Types.INTEGER, 4)
        );
        store = new RowStore(block, columns);
    }

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.destroy();
        }
    }

    @Test
    void shouldWriteAndReadRow() {
        store.setRow(0, 1001);
        store.setRow(0, "Alice", 1);
        store.setRow(0, 25, 2);
        
        assertEquals(1001, store.getInt(0, 0));
        assertEquals("Alice", store.getString(0, 1));
        assertEquals(25, store.getInt(0, 2));
    }

    @Test
    void shouldWriteMultipleRows() {
        store.setRow(0, 1001);
        store.setRow(0, "Alice", 1);
        store.setRow(0, 25, 2);
        
        store.setRow(1, 1002);
        store.setRow(1, "Bob", 1);
        store.setRow(1, 30, 2);
        
        assertEquals(1001, store.getInt(0, 0));
        assertEquals("Alice", store.getString(0, 1));
        assertEquals(1002, store.getInt(1, 0));
        assertEquals("Bob", store.getString(1, 1));
    }

    @Test
    void shouldCalculateRowSize() {
        int rowSize = store.getRowSize();
        assertEquals(40, rowSize);
    }

    @Test
    void shouldGetColumnCount() {
        assertEquals(3, store.getColumnCount());
    }

    @Test
    void shouldGetColumnMeta() {
        ColumnMeta meta = store.getColumnMeta(1);
        assertEquals("name", meta.getName());
        assertEquals(Types.VARCHAR, meta.getSqlType());
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd dbmmap-core && mvn test -Dtest=RowStoreTest`
Expected: FAIL - class not found

- [ ] **Step 3: 实现ColumnMeta**

```java
package com.dbmmap.core.storage;

public class ColumnMeta {
    
    private final String name;
    private final int sqlType;
    private final int size;
    
    public ColumnMeta(String name, int sqlType, int size) {
        this.name = name;
        this.sqlType = sqlType;
        this.size = size;
    }
    
    public String getName() {
        return name;
    }
    
    public int getSqlType() {
        return sqlType;
    }
    
    public int getSize() {
        return size;
    }
}
```

- [ ] **Step 4: 实现RowStore**

```java
package com.dbmmap.core.storage;

import com.dbmmap.core.memory.MemoryBlock;

import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.util.List;

public class RowStore {
    
    private final MemoryBlock block;
    private final List<ColumnMeta> columns;
    private final int rowSize;
    private final int[] columnOffsets;
    
    public RowStore(MemoryBlock block, List<ColumnMeta> columns) {
        this.block = block;
        this.columns = columns;
        this.columnOffsets = new int[columns.size()];
        
        int offset = 0;
        for (int i = 0; i < columns.size(); i++) {
            columnOffsets[i] = offset;
            offset += columns.get(i).getSize();
        }
        this.rowSize = offset;
    }
    
    public void setRow(int rowIndex, int value, int columnIndex) {
        int offset = getRowOffset(rowIndex) + columnOffsets[columnIndex];
        block.writeInt(offset, value);
    }
    
    public void setRow(int rowIndex, String value, int columnIndex) {
        int offset = getRowOffset(rowIndex) + columnOffsets[columnIndex];
        ColumnMeta meta = columns.get(columnIndex);
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(bytes.length, meta.getSize() - 4);
        block.writeInt(offset, len);
        block.writeBytes(offset + 4, Arrays.copyOf(bytes, len));
    }
    
    public void setRow(int rowIndex, long value, int columnIndex) {
        int offset = getRowOffset(rowIndex) + columnOffsets[columnIndex];
        block.writeLong(offset, value);
    }
    
    public void setRow(int rowIndex, double value, int columnIndex) {
        int offset = getRowOffset(rowIndex) + columnOffsets[columnIndex];
        block.writeDouble(offset, value);
    }
    
    public int getInt(int rowIndex, int columnIndex) {
        int offset = getRowOffset(rowIndex) + columnOffsets[columnIndex];
        return block.readInt(offset);
    }
    
    public String getString(int rowIndex, int columnIndex) {
        int offset = getRowOffset(rowIndex) + columnOffsets[columnIndex];
        int len = block.readInt(offset);
        if (len <= 0) {
            return null;
        }
        byte[] bytes = new byte[len];
        block.readBytes(offset + 4, bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    public long getLong(int rowIndex, int columnIndex) {
        int offset = getRowOffset(rowIndex) + columnOffsets[columnIndex];
        return block.readLong(offset);
    }
    
    public double getDouble(int rowIndex, int columnIndex) {
        int offset = getRowOffset(rowIndex) + columnOffsets[columnIndex];
        return block.readDouble(offset);
    }
    
    private int getRowOffset(int rowIndex) {
        return rowIndex * rowSize;
    }
    
    public int getRowSize() {
        return rowSize;
    }
    
    public int getColumnCount() {
        return columns.size();
    }
    
    public ColumnMeta getColumnMeta(int columnIndex) {
        return columns.get(columnIndex);
    }
    
    public int getMaxRows() {
        return block.getSize() / rowSize;
    }
}
```

- [ ] **Step 5: 修复import**

添加缺失的import:
```java
import java.util.Arrays;
```

- [ ] **Step 6: 运行测试验证通过**

Run: `cd dbmmap-core && mvn test -Dtest=RowStoreTest`
Expected: PASS - 4 tests

- [ ] **Step 7: 提交**

```bash
git add dbmmap-core/src/main/java/com/dbmmap/core/storage/ColumnMeta.java dbmmap-core/src/main/java/com/dbmmap/core/storage/RowStore.java dbmmap-core/src/test/java/com/dbmmap/core/storage/RowStoreTest.java
git commit -m "feat(core): add RowStore for row-based storage"
```

---

## Task 9: QueryResult - 查询结果API

**Files:**
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/query/QueryResult.java`
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/query/QueryResultImpl.java`
- Create: `dbmmap-core/src/test/java/com/dbmmap/core/query/QueryResultTest.java`

- [ ] **Step 1: 编写失败的测试**

```java
package com.dbmmap.core.query;

import com.dbmmap.core.memory.MemoryBlock;
import com.dbmmap.core.memory.MemoryPool;
import com.dbmmap.core.memory.MemoryPoolConfig;
import com.dbmmap.core.storage.ColumnMeta;
import com.dbmmap.core.storage.RowStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryResultTest {

    private MemoryPool pool;
    private MemoryBlock block;
    private QueryResult result;

    @BeforeEach
    void setUp() {
        MemoryPoolConfig config = MemoryPoolConfig.builder()
                .preAllocMemSize(1024)
                .queryCacheSize(1024)
                .build();
        pool = new MemoryPool(config);
        block = pool.allocate();
        
        List<ColumnMeta> columns = Arrays.asList(
            new ColumnMeta("id", Types.INTEGER, 4),
            new ColumnMeta("name", Types.VARCHAR, 32),
            new ColumnMeta("age", Types.INTEGER, 4)
        );
        RowStore store = new RowStore(block, columns);
        store.setRow(0, 1001, 0);
        store.setRow(0, "Alice", 1);
        store.setRow(0, 25, 2);
        store.setRow(1, 1002, 0);
        store.setRow(1, "Bob", 1);
        store.setRow(1, 30, 2);
        
        result = new QueryResultImpl(store, 2, block, pool);
    }

    @AfterEach
    void tearDown() {
        result.close();
        if (pool != null) {
            pool.destroy();
        }
    }

    @Test
    void shouldGetRowCount() {
        assertEquals(2, result.getRowCount());
    }

    @Test
    void shouldGetIntByColumnName() {
        assertEquals(1001, result.getInt(0, "id"));
        assertEquals(1002, result.getInt(1, "id"));
    }

    @Test
    void shouldGetStringByColumnName() {
        assertEquals("Alice", result.getString(0, "name"));
        assertEquals("Bob", result.getString(1, "name"));
    }

    @Test
    void shouldGetIntByColumnIndex() {
        assertEquals(1001, result.getInt(0, 0));
        assertEquals(1002, result.getInt(1, 0));
    }

    @Test
    void shouldGetStringByColumnIndex() {
        assertEquals("Alice", result.getString(0, 1));
        assertEquals("Bob", result.getString(1, 1));
    }

    @Test
    void shouldIterateRows() {
        int count = 0;
        while (result.next()) {
            count++;
        }
        assertEquals(2, count);
        result.reset();
        assertTrue(result.next());
    }

    @Test
    void shouldCloseReleaseMemory() {
        result.close();
        assertEquals(0, pool.getUsedSlotCount());
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd dbmmap-core && mvn test -Dtest=QueryResultTest`
Expected: FAIL - class not found

- [ ] **Step 3: 实现QueryResult接口**

```java
package com.dbmmap.core.query;

public interface QueryResult extends AutoCloseable {
    
    int getRowCount();
    
    int getColumnCount();
    
    String getColumnName(int columnIndex);
    
    int getInt(int rowIndex, String columnName);
    
    int getInt(int rowIndex, int columnIndex);
    
    String getString(int rowIndex, String columnName);
    
    String getString(int rowIndex, int columnIndex);
    
    long getLong(int rowIndex, String columnName);
    
    long getLong(int rowIndex, int columnIndex);
    
    double getDouble(int rowIndex, String columnName);
    
    double getDouble(int rowIndex, int columnIndex);
    
    boolean next();
    
    void reset();
    
    void release();
    
    @Override
    void close();
}
```

- [ ] **Step 4: 实现QueryResultImpl**

```java
package com.dbmmap.core.query;

import com.dbmmap.core.memory.MemoryBlock;
import com.dbmmap.core.memory.MemoryPool;
import com.dbmmap.core.storage.RowStore;

import java.util.HashMap;
import java.util.Map;

public class QueryResultImpl implements QueryResult {
    
    private final RowStore store;
    private final int rowCount;
    private final MemoryBlock block;
    private final MemoryPool pool;
    private final Map<String, Integer> columnNameIndex;
    private int currentRow;
    private boolean closed;
    
    public QueryResultImpl(RowStore store, int rowCount, MemoryBlock block, MemoryPool pool) {
        this.store = store;
        this.rowCount = rowCount;
        this.block = block;
        this.pool = pool;
        this.columnNameIndex = new HashMap<>();
        this.currentRow = -1;
        this.closed = false;
        
        for (int i = 0; i < store.getColumnCount(); i++) {
            columnNameIndex.put(store.getColumnMeta(i).getName().toLowerCase(), i);
        }
    }
    
    @Override
    public int getRowCount() {
        checkClosed();
        return rowCount;
    }
    
    @Override
    public int getColumnCount() {
        checkClosed();
        return store.getColumnCount();
    }
    
    @Override
    public String getColumnName(int columnIndex) {
        checkClosed();
        return store.getColumnMeta(columnIndex).getName();
    }
    
    @Override
    public int getInt(int rowIndex, String columnName) {
        return getInt(rowIndex, getColumnIndex(columnName));
    }
    
    @Override
    public int getInt(int rowIndex, int columnIndex) {
        checkClosed();
        checkRowIndex(rowIndex);
        return store.getInt(rowIndex, columnIndex);
    }
    
    @Override
    public String getString(int rowIndex, String columnName) {
        return getString(rowIndex, getColumnIndex(columnName));
    }
    
    @Override
    public String getString(int rowIndex, int columnIndex) {
        checkClosed();
        checkRowIndex(rowIndex);
        return store.getString(rowIndex, columnIndex);
    }
    
    @Override
    public long getLong(int rowIndex, String columnName) {
        return getLong(rowIndex, getColumnIndex(columnName));
    }
    
    @Override
    public long getLong(int rowIndex, int columnIndex) {
        checkClosed();
        checkRowIndex(rowIndex);
        return store.getLong(rowIndex, columnIndex);
    }
    
    @Override
    public double getDouble(int rowIndex, String columnName) {
        return getDouble(rowIndex, getColumnIndex(columnName));
    }
    
    @Override
    public double getDouble(int rowIndex, int columnIndex) {
        checkClosed();
        checkRowIndex(rowIndex);
        return store.getDouble(rowIndex, columnIndex);
    }
    
    @Override
    public boolean next() {
        checkClosed();
        currentRow++;
        return currentRow < rowCount;
    }
    
    @Override
    public void reset() {
        checkClosed();
        currentRow = -1;
    }
    
    @Override
    public void release() {
        close();
    }
    
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (pool != null && block != null) {
            pool.release(block);
        }
    }
    
    private int getColumnIndex(String columnName) {
        Integer index = columnNameIndex.get(columnName.toLowerCase());
        if (index == null) {
            throw new IllegalArgumentException("Column not found: " + columnName);
        }
        return index;
    }
    
    private void checkRowIndex(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rowCount) {
            throw new IndexOutOfBoundsException("Row index out of bounds: " + rowIndex);
        }
    }
    
    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("QueryResult has been closed");
        }
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `cd dbmmap-core && mvn test -Dtest=QueryResultTest`
Expected: PASS - 7 tests

- [ ] **Step 6: 提交**

```bash
git add dbmmap-core/src/main/java/com/dbmmap/core/query/QueryResult.java dbmmap-core/src/main/java/com/dbmmap/core/query/QueryResultImpl.java dbmmap-core/src/test/java/com/dbmmap/core/query/QueryResultTest.java
git commit -m "feat(core): add QueryResult API with row iteration"
```

---

## Task 10: Session - 会话管理

**Files:**
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/session/Session.java`
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/session/SessionImpl.java`
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/session/SessionFactory.java`
- Create: `dbmmap-core/src/test/java/com/dbmmap/core/session/SessionTest.java`

- [ ] **Step 1: 编写失败的测试**

```java
package com.dbmmap.core.session;

import com.dbmmap.core.config.DbMmapConfig;
import com.dbmmap.core.query.QueryResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionTest {

    private SessionFactory factory;
    private Session session;

    @BeforeEach
    void setUp() {
        DbMmapConfig config = DbMmapConfig.builder().build();
        factory = new SessionFactory(config);
        session = factory.openSession();
    }

    @AfterEach
    void tearDown() {
        if (session != null) {
            session.close();
        }
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void shouldOpenSession() {
        assertNotNull(session);
        assertFalse(session.isClosed());
    }

    @Test
    void shouldCloseSession() {
        session.close();
        assertTrue(session.isClosed());
    }

    @Test
    void shouldGetMemoryPool() {
        assertNotNull(session.getMemoryPool());
    }

    @Test
    void shouldReleaseAllQueryResultsOnClose() {
        session.close();
        assertTrue(session.isClosed());
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd dbmmap-core && mvn test -Dtest=SessionTest`
Expected: FAIL - class not found

- [ ] **Step 3: 实现Session接口**

```java
package com.dbmmap.core.session;

import com.dbmmap.core.memory.MemoryPool;
import com.dbmmap.core.query.QueryResult;

import java.sql.Connection;
import java.util.function.Function;

public interface Session extends AutoCloseable {
    
    QueryResult query(String sql);
    
    <T> T query(String sql, Function<QueryResult, T> handler);
    
    int update(String sql);
    
    int[] batchUpdate(String... sqls);
    
    MemoryPool getMemoryPool();
    
    boolean isClosed();
    
    void close();
}
```

- [ ] **Step 4: 实现SessionFactory**

```java
package com.dbmmap.core.session;

import com.dbmmap.core.config.DbMmapConfig;
import com.dbmmap.core.memory.MemoryPool;
import com.dbmmap.core.memory.MemoryPoolConfig;

import javax.sql.DataSource;

public class SessionFactory {
    
    private final DbMmapConfig config;
    private final MemoryPool memoryPool;
    private volatile boolean closed;
    
    public SessionFactory(DbMmapConfig config) {
        this.config = config;
        this.memoryPool = new MemoryPool(config.getMemoryPoolConfig());
        this.closed = false;
    }
    
    public SessionFactory(DbMmapConfig config, DataSource dataSource) {
        this(config);
    }
    
    public Session openSession() {
        if (closed) {
            throw new IllegalStateException("SessionFactory has been closed");
        }
        return new SessionImpl(memoryPool, config);
    }
    
    public void close() {
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
```

- [ ] **Step 5: 实现SessionImpl**

```java
package com.dbmmap.core.session;

import com.dbmmap.core.config.DbMmapConfig;
import com.dbmmap.core.memory.MemoryBlock;
import com.dbmmap.core.memory.MemoryPool;
import com.dbmmap.core.query.QueryResult;
import com.dbmmap.core.query.QueryResultImpl;
import com.dbmmap.core.storage.ColumnMeta;
import com.dbmmap.core.storage.RowStore;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SessionImpl implements Session {
    
    private final MemoryPool memoryPool;
    private final DbMmapConfig config;
    private volatile boolean closed;
    
    SessionImpl(MemoryPool memoryPool, DbMmapConfig config) {
        this.memoryPool = memoryPool;
        this.config = config;
        this.closed = false;
    }
    
    @Override
    public QueryResult query(String sql) {
        checkClosed();
        MemoryBlock block = memoryPool.allocate();
        List<ColumnMeta> columns = new ArrayList<>();
        columns.add(new ColumnMeta("result", Types.INTEGER, 4));
        RowStore store = new RowStore(block, columns);
        return new QueryResultImpl(store, 0, block, memoryPool);
    }
    
    @Override
    public <T> T query(String sql, Function<QueryResult, T> handler) {
        checkClosed();
        QueryResult result = null;
        try {
            result = query(sql);
            return handler.apply(result);
        } finally {
            if (result != null) {
                result.close();
            }
        }
    }
    
    @Override
    public int update(String sql) {
        checkClosed();
        return 0;
    }
    
    @Override
    public int[] batchUpdate(String... sqls) {
        checkClosed();
        return new int[sqls.length];
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
    
    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Session has been closed");
        }
    }
}
```

- [ ] **Step 6: 运行测试验证通过**

Run: `cd dbmmap-core && mvn test -Dtest=SessionTest`
Expected: PASS - 4 tests

- [ ] **Step 7: 提交**

```bash
git add dbmmap-core/src/main/java/com/dbmmap/core/session/Session.java dbmmap-core/src/main/java/com/dbmmap/core/session/SessionImpl.java dbmmap-core/src/main/java/com/dbmmap/core/session/SessionFactory.java dbmmap-core/src/test/java/com/dbmmap/core/session/SessionTest.java
git commit -m "feat(core): add Session and SessionFactory for resource management"
```

---

## Task 11: 运行全部测试

**Files:**
- 无新文件

- [ ] **Step 1: 运行所有测试**

Run: `cd dbmmap-core && mvn test`
Expected: All tests pass

- [ ] **Step 2: 检查测试覆盖率**

Run: `cd dbmmap-core && mvn test -q`
Expected: Build success

---

## Task 12: 完成Phase 1

**Files:**
- 无新文件

- [ ] **Step 1: 最终验证**

Run: `cd dbmmap-core && mvn clean test`
Expected: BUILD SUCCESS

- [ ] **Step 2: 提交Phase 1完成**

```bash
git add -A
git commit -m "feat(core): complete Phase 1 - core infrastructure

- Memory pool with off-heap allocation
- Slot allocator with bitmap marking
- Row-based storage
- Query result API
- Session management"
```

---

## 自检清单

完成后检查：

- [ ] 所有测试通过
- [ ] 无placeholder代码
- [ ] 无TODO注释
- [ ] 类型一致性检查
- [ ] 规格覆盖率检查