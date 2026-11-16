# jrdb（已归档）

[English](README_en.md) | [变更记录](CHANGELOG.md) | [手册](MANUAL.md)

> 本仓库已归档，不再维护。仓库保留在 `main` 分支，未删除。

## 为什么归档

- 技术判断成立：ASM 运行期生成映射确实能消掉反射开销，这条路走得通；
- 但通用 ORM 的生态位已被 MyBatis / Hibernate 占满，性能亮点不足以覆盖迁移成本；
- 对多数业务 MyBatis 的性能已经足够；低延时场景少，且常被异步化、批量合并、Redis 缓存绕开；
- 技术抉择没有错，只是出生太晚了。

## 保留内容

- 源码与历史提交完整保留；
- 设计、分析与基准文档见 `docs/`。

## 历史

完整版本记录见 [CHANGELOG.md](CHANGELOG.md)。
