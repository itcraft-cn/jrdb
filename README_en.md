# jrdb (archived)

[中文](README.md) | [Changelog](CHANGELOG_en.md) | [Manual](MANUAL_en.md)

> This repository is archived and no longer maintained. It remains on the `main` branch and has not
> been deleted.

## Why archived

- The technical bet held: runtime ASM mapping does remove reflection overhead.
- But the general-purpose ORM niche is fully occupied by MyBatis / Hibernate; the performance edge
  does not justify migration cost.
- For most workloads MyBatis is fast enough; low-latency cases are rare and are often solved
  indirectly with async, batching, and Redis caching.
- The technical choices were not wrong. It was simply born too late.

## Kept

- Source code and git history are preserved;
- Design, analysis and benchmark documents live under `docs/`.

## History

See [CHANGELOG_en.md](CHANGELOG_en.md).
