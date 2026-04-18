# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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