# dbmmap-benchmark

Performance benchmark comparing dbmmap with MyBatis, Hibernate, and raw JDBC.

## Running Benchmarks

### Run all benchmarks
```bash
mvn clean package
java -jar target/dbmmap-benchmark-1.0.0-SNAPSHOT.jar
```

### Run specific benchmark
```bash
java -jar target/dbmmap-benchmark-1.0.0-SNAPSHOT.jar QueryBenchmark
java -jar target/dbmmap-benchmark-1.0.0-SNAPSHOT.jar MyBatisQueryBenchmark
java -jar target/dbmmap-benchmark-1.0.0-SNAPSHOT.jar HibernateQueryBenchmark
java -jar target/dbmmap-benchmark-1.0.0-SNAPSHOT.jar DbMmapQueryBenchmark
```

### Run with specific parameters
```bash
java -jar target/dbmmap-benchmark-1.0.0-SNAPSHOT.jar -wi 5 -i 10 -t 4 QueryBenchmark
```

## Benchmark Categories

| Benchmark | Description |
|-----------|-------------|
| QueryBenchmark | Raw JDBC queries (baseline) |
| MyBatisQueryBenchmark | MyBatis ORM queries |
| HibernateQueryBenchmark | Hibernate ORM queries |
| DbMmapQueryBenchmark | dbmmap off-heap queries |

## Test Data

- 10,000 user records inserted at benchmark initialization
- Each record: id, name, age, email, status

## Expected Results

Lower average time (microseconds) indicates better performance.

| Operation | JDBC | MyBatis | Hibernate | dbmmap |
|-----------|------|---------|-----------|--------|
| Single Query | ~10μs | ~15μs | ~50μs | TBD |
| Query All (10K rows) | ~5ms | ~8ms | ~20ms | TBD |
| Query To List (100 rows) | ~100μs | ~200μs | ~500μs | TBD |

## Interpretation Guide

1. **JDBC** serves as baseline - fastest possible with no overhead
2. **MyBatis** adds minimal mapping overhead
3. **Hibernate** has higher overhead due to full ORM features
4. **dbmmap** should be competitive with MyBatis while providing off-heap storage benefits