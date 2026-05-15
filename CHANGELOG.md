# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-05-15

### Added
- 注解SQL混合模式支持 - 三种Style注解（JdbcStyle/MyBatisStyle/HibernateStyle）
- ASM字节码生成代理（DaoProxyTemplate）替代JDK Proxy
- AutoCrud注解 - 自动生成insert/update/delete/selectById方法
- AutoCrud支持tableName属性指定表名
- AbstractStyleExecutor基类抽取公共代码
- HibernateStyleIntegrationTest集成测试（5个测试）
- JdbcStyleIntegrationTest集成测试（5个测试）
- StyleComparisonBenchmark性能测试
- AnnotationStyleBenchmark性能测试
- Session事务API - beginTransaction/commit/rollback/isInTransaction/setTransactionIsolation
- jrdb-jdbc-spring模块 - JrdbTransactionTemplate + JrdbSpringTransactionTemplate
- JrdbAutoConfiguration - Spring Boot自动配置
- JrdbProperties - 配置属性
- SessionTransactionTest - 12个测试场景

### Changed
- DaoMetaCache参数映射改为按参数名而非顺序
- HibernateStyle的:param转换逻辑修复
- insert返回值统一为int（受影响行数）
- pom.xml添加-parameters编译选项保留参数名
- AnnotationStyleBenchmark Session生命周期改为Level.Iteration（修正性能数据）
- SessionImpl.close方法自动rollback未提交事务
- 删除旧transaction包（Transaction/TransactionManager/TransactionCallback）

### Fixed
- AutoCrud SQL缓存修复
- AutoCrud字段顺序修复
- AutoCrud返回类型修复
- Style参数名映射按参数名而非顺序

### Performance
- 三种Style性能测试：selectById ~2.2M ops/s，性能相同
- AutoCrud vs ASM：selectById差距~0%，update差距~8%，delete差距~4%
- SQL转换和参数映射在parse时一次性缓存，运行时无额外开销

### Documentation
- README添加注解SQL模式性能对比数据
- README添加Style性能对比表格
- README_cn同步更新注解SQL模式文档
- docs/comparison-vs-frameworks.md（框架对比分析）
- docs/transaction-api-design.md（事务API改进设计）

## [1.1.0] - 2026-04-18

### Added
- 添加 RowMeta 和 ColumnMeta toString 方法便于调试
- 添加 SQL 直接执行批量操作 API (DirectSql)，批量操作性能提升 50%

### Fixed
- 修复 QueryResultImpl 索引语义不一致，统一为从1开始

### Changed
- 移除数据库特性相关的批量操作 API，推荐使用 DirectSql API

### Documentation
- 更新 AGENTS.md，移除不存在模块并修正 benchmark 命令
- 更新 README 和 AGENTS.md，添加 DirectSql 功能说明
- 添加 TODO 清单
- 添加代码走查报告
- 更新 CLAUDE.md 为实际项目文档
- 添加 MyBatis SESSION 级别缓存风险分析文档
- 更新 License 为 Apache 2.0，添加构建工具说明

### Tests
- EntityMappingBenchmark 开启最高缓存对比测试

### Build
- change to new version: 1.1.0
- change to new version: 1.1.0-dev

### Chore
- add claudecode id
- group name changed

## [1.0.0] - 2026-04-01

### Added
- 添加 jrdb-r2dbc Spring Boot 支持
- R2DBC 支持 CompletableFuture + 线程池模型
- 完善 Session API

### Performance
- 添加连接池和Session复用优化
- 优化 update/batchUpdate 性能

### Fixed
- 修复 JrdbTemplate Javadoc 错误
- 修复 Javadoc 生成错误
- 英文版 slogan 改为英文
- 移除连接池嵌套，直接使用传入的 ConnectionFactory

### Changed
- 清理各模块无用的 test 依赖
- 将 jrdb-jdbc 测试代码移动到 jrdb-test 模块
- 重命名 jrdb-benchmark 为 jrdb-test
- 简化 R2DBC 实现
- 重构模块结构，支持 R2DBC
- 优化 insert/batchInsert 返回值

### Documentation
- 添加中英文 README 互相跳转链接
- 更新 slogan 为极致性能项目的简单CRUD利器
- 重命名 README 为 README_cn.md，新增英文版 README.md
- 更新 README 和 AGENTS，添加 R2DBC 文档
- 更新 README，完善 Session API 说明
- 完善 README 示例代码
- 更新 README 性能基准数据
- 添加 Spring Boot 集成示例到 README
- 完善 README 性能基准数据

### Tests
- InsertBenchmark 使用 L1 Cache 公平对比

### Build
- change to new version: 1.0.0
- remove empty module

### Chore
- managed by updgitver
- clean the pom
- ignore
- LICENSE

## Pre-release

### Added
- 核心功能：基于堆外内存 + ASM 动态映射的高性能数据库操作框架
- jrdb-jdbc 模块：同步 JDBC 实现
  - ASM API - queryForObject/queryForList，直接返回实体
  - Streaming API - queryWithHandler，零对象创建，ETL 场景
  - Raw API - query 返回 QueryResult，精细控制
  - DirectSql API - execute/executeBatch，批量操作性能优化
- jrdb-r2dbc 模块：响应式 R2DBC 实现
  - queryOne - 查询单个值，返回 CompletableFuture<T>
  - queryList - 查询列表，返回 CompletableFuture<List<T>>
  - update/insert/delete - 更新操作，返回 CompletableFuture<Integer>
  - batch - 批量操作，返回 CompletableFuture<int[]>
- jrdb-test 模块：单元测试和 JMH 基准测试
- jrdb-spring-boot-starter 模块：Spring Boot 集成

### Performance
- 单条查询：6-8x MyBatis
- 批量查询：7-10x MyBatis
- 插入操作：+14% MyBatis
- DirectSql 批量操作：+50% PreparedStatement