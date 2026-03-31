# jrdb 核心数据流图

## 概述

本文档描述 jrdb 项目中核心数据的流转过程，包括初始化、查询、更新、内存管理和实体映射等关键流程。

## 1. 初始化流程

```mermaid
flowchart TD
    A[创建 JrdbConfig] --> B[创建 MemoryPoolConfig]
    B --> C[创建 MemoryPool]
    C --> D[Unsafe 分配堆外内存]
    C --> E[创建 SlotAllocator]
    D --> F[创建 SessionFactory]
    F --> G[配置 DataSource]
    G --> H[系统就绪]
```

**关键步骤说明：**
- `MemoryPoolConfig` 定义预分配内存大小（默认256MB）和查询缓存大小（默认16MB）
- `MemoryPool` 使用 `Unsafe.allocateMemory()` 分配堆外内存
- `SlotAllocator` 使用位图管理内存槽位的分配与释放

## 2. 查询流程

```mermaid
flowchart TD
    A[Session.query] --> B{L2缓存开启?}
    B -->|yes| C[检查 QueryResultCache]
    C --> D{缓存命中?}
    D -->|yes| E[返回 CachedQueryResultAdapter]
    D -->|no| F[执行数据库查询]
    B -->|no| F
    
    F --> G[获取 Connection]
    G --> H[获取/创建 PreparedStatement]
    H --> I[设置参数]
    I --> J[executeQuery]
    J --> K[解析 ResultSetMetaData]
    K --> L[创建 ColumnMeta 列表]
    
    L --> M[MemoryPool.allocate]
    M --> N[创建 MemoryBlock]
    N --> O[创建 RowStore]
    
    O --> P{ResultSet.next?}
    P -->|有数据| Q[读取一行数据]
    Q --> R[写入 RowStore]
    R --> P
    P -->|完成| S[创建 QueryResultImpl]
    
    S --> T{L2缓存开启?}
    T -->|yes| U[复制到 CachedQueryResult]
    U --> V[存入缓存]
    V --> W[释放 MemoryBlock]
    W --> X[返回 CachedQueryResultAdapter]
    T -->|no| Y[返回 QueryResultImpl]
```

**数据写入细节：**
```mermaid
flowchart LR
    A[ResultSet.getInt] --> B[RowStore.setIntFast]
    B --> C[MemoryBlock.writeIntFast]
    C --> D[Unsafe.putInt]
    D --> E[堆外内存]
    
    F[ResultSet.getString] --> G[RowStore.setStringFast]
    G --> H[计算长度]
    H --> I[写入长度 4bytes]
    I --> J[写入字节内容]
    J --> K[堆外内存]
```

## 3. 数据读取流程

```mermaid
flowchart TD
    A[QueryResult.next] --> B[currentRow++]
    B --> C[QueryResult.getLong columnName]
    C --> D[查找列索引 columnNameIndex]
    D --> E[RowStore.getLong currentRow columnIndex]
    E --> F[计算偏移量]
    F --> G[offset = rowOffset + colOffset]
    G --> H[MemoryBlock.readLong offset]
    H --> I[Unsafe.getLong address+offset]
    I --> J[返回值]
```

**内存布局示意：**
```
MemoryBlock 内存布局:
┌─────────────────────────────────────────────────────┐
│ Row0: [Col0][Col1][Col2]...[ColN]                   │
│ Row1: [Col0][Col1][Col2]...[ColN]                   │
│ Row2: [Col0][Col1][Col2]...[ColN]                   │
│ ...                                                 │
│ RowM: [Col0][Col1][Col2]...[ColN]                   │
└─────────────────────────────────────────────────────┘

offset = currentRow * rowSize + columnOffsets[columnIndex]
```

## 4. 更新流程

```mermaid
flowchart TD
    A[Session.update sql params] --> B[获取 Connection]
    B --> C[创建 PreparedStatement]
    C --> D[设置参数]
    D --> E[executeUpdate]
    E --> F[返回影响行数]
    
    F --> G{L2缓存开启?}
    G -->|yes| H[invalidateResultCache]
    G -->|no| I[完成]
    H --> I
```

## 5. 内存管理流程

```mermaid
flowchart TD
    subgraph 分配流程
        A1[MemoryPool.allocate] --> A2[SlotAllocator.allocate]
        A2 --> A3[查找空闲slot]
        A3 --> A4[位图标记已使用]
        A4 --> A5[计算地址]
        A5 --> A6[创建 MemoryBlock]
    end
    
    subgraph 释放流程
        B1[MemoryPool.release] --> B2[SlotAllocator.release]
        B2 --> B3[位图标记未使用]
        B3 --> B4[更新 nextFreeSlot]
    end
    
    subgraph 销毁流程
        C1[MemoryPool.destroy] --> C2[Unsafe.freeMemory]
        C2 --> C3[释放堆外内存]
    end
```

**SlotAllocator 位图结构：**
```
slots: long[] 数组，每64个slot用一个long表示
位图位: 1 = 已使用, 0 = 未使用

示例（slotCount=128）:
slots[0]: |slot0|slot1|slot2|...|slot63|  (64 bits)
slots[1]: |slot64|slot65|...|slot127|     (64 bits)
```

## 6. 实体映射流程

```mermaid
flowchart TD
    A[MapperRegistry.getMapper] --> B{mapper已缓存?}
    B -->|no| C[autoConfigure]
    C --> D[反射扫描字段]
    D --> E[推断表名 驼峰转下划线]
    E --> F[创建 MappingConfig]
    
    F --> G[MapperClassGenerator.generate]
    G --> H[ASM字节码生成]
    H --> I[生成 mapFromResultSet]
    H --> J[生成 mapToRow]
    
    I --> K[MapperClassLoader.defineClass]
    J --> K
    K --> L[创建 mapper实例]
    L --> M[缓存 mapper]
    
    B -->|yes| N[返回缓存mapper]
    M --> N
    N --> O[EntityMapper.mapFromResultSet]
    O --> P[ASM代码执行]
    P --> Q[ResultSet.getXxx]
    Q --> R[entity.setXxx]
    R --> S[返回实体对象]
```

**ASM 字节码生成示例：**
```java
// 生成的 mapFromResultSet 方法伪代码
public User mapFromResultSet(ResultSet rs) {
    User user = new User();
    user.setId(rs.getLong("id"));
    user.setName(rs.getString("name"));
    user.setAge(rs.getInt("age"));
    return user;
}
```

## 7. 缓存策略

```mermaid
flowchart TD
    subgraph L1缓存
        A1[Statement缓存] --> A2[PreparedStatement复用]
        A3[Meta缓存] --> A4[QueryMeta复用]
    end
    
    subgraph L2缓存
        B1[QueryResultCache] --> B2[CachedQueryResult存储]
        B3[Guava Cache] --> B4[最大容量限制]
        B4 --> B5[过期时间控制]
    end
```

**缓存级别说明：**
- `NONE`: 无缓存，每次创建新的 PreparedStatement
- `L1`: Statement 和 Meta 缓存，减少对象创建开销
- `L2`: 结果缓存，相同查询直接返回缓存结果

## 8. 事务流程

```mermaid
flowchart TD
    A[TransactionManager.beginTransaction] --> B[创建 TransactionImpl]
    B --> C[懒加载 Connection]
    
    D[executeInTransaction] --> E[执行业务逻辑]
    E --> F{成功?}
    F -->|yes| G[commit]
    F -->|no| H[rollback]
    
    G --> I[connection.commit]
    H --> J[connection.rollback]
    I --> K[connection.close]
    J --> K
```

## 数据流总结

| 操作 | 数据流向 | 关键组件 |
|------|----------|----------|
| 查询 | DB → ResultSet → MemoryBlock → QueryResult | Session, RowStore, MemoryPool |
| 读取 | QueryResult → RowStore → MemoryBlock → Unsafe | 堆外内存直接读取 |
| 更新 | SQL → PreparedStatement → DB | Session, Connection |
| 映射 | ResultSet → ASM生成代码 → Entity | MapperRegistry, EntityMapper |
| 缓存 | QueryResult → CachedQueryResult → Cache | QueryResultCache, Guava Cache |