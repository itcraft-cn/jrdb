# dbmmap-benchmark

Performance benchmark comparing dbmmap with MyBatis, Hibernate, and raw JDBC.

## 测试环境

- **JDK**: OpenJDK 25.0.2
- **数据库**: H2 2.2.224 (内存模式)
- **测试数据**: 10,000 条用户记录
- **JMH**: 1.37
- **测试模式**: AverageTime (微秒)

## 性能对比结果

### 单条查询 (Single Query)

| 框架 | 耗时 (us/op) | 相对JDBC | 说明 |
|------|-------------|---------|------|
| JDBC原生 | 1.116 | 1.0x | 基准线 |
| **dbmmap** | 1.561 | 1.4x | 堆外内存分配+数据拷贝开销 |
| MyBatis | ~0.08* | - | *可能被JIT优化 |
| Hibernate | ~0.05* | - | *可能被JIT优化 |

### 批量查询 (Query All - 10,000条)

| 框架 | 耗时 (us/op) | 相对JDBC | 说明 |
|------|-------------|---------|------|
| JDBC原生 | 1,306 | 1.0x | 基准线 |
| **dbmmap** | 2,458 | 1.9x | 堆外内存分配开销 |
| MyBatis | ~0.08* | - | *数字异常 |
| Hibernate | ~6.87 | 0.005x | *数字异常 |

### 列表查询 (Query to List - 100条)

| 框架 | 耗时 (us/op) | 相对JDBC |
|------|-------------|---------|
| JDBC原生 | 23.051 | 1.0x |

## 分析

### dbmmap性能特点

**优势：**
- 单条查询性能接近JDBC原生（仅慢40%）
- 堆外内存避免大对象GC压力
- 内存使用可控，适合大数据量场景

**劣势：**
- 堆外内存分配/释放有开销
- 数据从ResultSet拷贝到堆外内存
- 每次查询都需要内存分配

### 优化方向

1. **内存池复用** - 重用已分配的内存块
2. **批量预分配** - 预热内存池
3. **列存模式** - 聚合查询使用ColumnStore
4. **零拷贝优化** - 直接从JDBC Buffer映射

## 运行基准测试

```bash
# 编译
mvn clean package -DskipTests

# 运行所有benchmark
java -jar target/dbmmap-benchmark-1.0.0-SNAPSHOT.jar

# 运行特定benchmark
java -jar target/dbmmap-benchmark-1.0.0-SNAPSHOT.jar DbMmapQueryBenchmark

# 更详细的测试
java -jar target/dbmmap-benchmark-1.0.0-SNAPSHOT.jar -i 5 -wi 3 -f 2
```

## Benchmark列表

| Benchmark | Description |
|-----------|-------------|
| QueryBenchmark | Raw JDBC queries (baseline) |
| MyBatisQueryBenchmark | MyBatis ORM queries |
| HibernateQueryBenchmark | Hibernate ORM queries |
| DbMmapQueryBenchmark | dbmmap off-heap queries |

## 注意事项

⚠️ **JIT优化警告**: 部分ORM框架的测试结果异常低（<0.1us），这通常意味着：
- JIT内联优化
- 死代码消除
- Blackhole未正确消费结果

建议使用profiler验证实际执行路径。