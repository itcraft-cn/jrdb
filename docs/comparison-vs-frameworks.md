# jrdb 使用方式点评 vs iBATIS/MyBatis/Hibernate/JPA

## 1. API风格对比

| 框架 | API层次 | 代码示例 | 灵活性 |
|------|---------|----------|--------|
| **iBATIS/MyBatis** | Mapper接口层 | `userMapper.selectById(1L)` | 中等 - 必须定义Mapper |
| **Hibernate/JPA** | Entity管理层 | `entityManager.find(User.class, 1L)` | 低 - 受Entity状态管理约束 |
| **jrdb ASM直接** | Session底层 | `session.queryForObject(sql, User.class, 1L)` | 最高 - 直接控制SQL |
| **jrdb Annotation** | DAO接口层 | `userDao.findById(1L)` | 中等 - 与MyBatis类似 |
| **jrdb AutoCrud** | 自动生成层 | `autoCrudDao.selectById(1L)` | 低 - 4个固定方法 |

**点评：jrdb提供5种API层次，从底层直接控制到高层自动生成，覆盖最广。**

## 2. SQL编写方式对比

| 框架 | SQL定义方式 | 示例 | SQL可见性 |
|------|------------|------|----------|
| **iBATIS** | XML文件 | `<select id="findById">SELECT...</select>` | 100%可见，集中管理 |
| **MyBatis** | XML + 注解 | `@Select("SELECT...")` | 注解可见，XML集中 |
| **Hibernate** | HQL/Criteria | `from User where id = ?` | SQL被抽象，不可见 |
| **JPA** | JPQL/Criteria | 同Hibernate | SQL被抽象，不可见 |
| **jrdb ASM** | 代码字符串 | `"SELECT * FROM users WHERE id = ?"` | 100%可见，分散在代码 |
| **jrdb Annotation** | 注解 | `@Select("SELECT...")` | 同MyBatis注解 |
| **jrdb AutoCrud** | 自动生成 | 无需编写 | SQL不可见（自动生成） |

**点评：**
- **iBATIS/XML派**：SQL集中管理，适合大型项目、SQL复杂场景
- **MyBatis/注解派**：SQL与代码同位置，适合中小项目
- **Hibernate/JPA派**：SQL完全抽象，适合不关心SQL的团队
- **jrdb ASM派**：SQL直接可见，适合追求极致性能和控制
- **jrdb AutoCrud派**：零SQL，适合简单CRUD

**jrdb独特优势**：同时支持**可见SQL**（ASM/Annotation）和**隐藏SQL**（AutoCrud），满足不同需求。

## 3. 实体映射方式对比

| 框架 | 映射技术 | 性能 | 约束 |
|------|---------|------|------|
| **iBATIS/MyBatis** | 反射 + BeanUtils | 慢 | 无约束，POJO即可 |
| **Hibernate** | 反射 + 字段访问 | 中 | Entity必须带@Id等注解 |
| **JPA** | 同Hibernate | 中 | 同Hibernate |
| **jrdb ASM** | ASM字节码生成 | 最快 | POJO即可，无注解约束 |

**点评：jrdb ASM映射性能最快（~2M ops/s），且无注解约束。**

## 4. 参数绑定方式对比

| 框架 | 参数风格 | 示例 | 可读性 |
|------|---------|------|--------|
| **iBATIS/MyBatis** | `#param` / `$param` | `WHERE id = #{id}` | 高 - 命名参数 |
| **Hibernate/HQL** | `?` 位置参数 | `WHERE id = ?` | 低 - 位置参数 |
| **JPA/JPQL** | `:param` 命名参数 | `WHERE id = :id` | 高 - 命名参数 |
| **jrdb JdbcStyle** | `?` 位置参数 | `WHERE id = ?` | 低 - 位置参数 |
| **jrdb MyBatisStyle** | `#{param}` 命名参数 | `WHERE id = #{id}` | 高 - 命名参数 |
| **jrdb HibernateStyle** | `:param` 命名参数 | `WHERE id = :id` | 高 - 命名参数 |

**点评：jrdb支持三种参数风格，兼容MyBatis和Hibernate习惯。**

## 5. 性能对比（单条查询）

| 框架 | 性能（ops/s） | 原因 |
|------|--------------|------|
| **MyBatis（无缓存）** | ~260K | 反射开销 + ResultSet遍历 |
| **Hibernate（无缓存）** | ~185K | 反射开销 + Session状态管理 |
| **JPA** | ~185K | 同Hibernate |
| **jrdb ASM** | **~2.2M** | ASM字节码 + 堆外内存，零反射 |
| **jrdb Annotation** | **~2.0M** | ASM映射 + 缓存SQL转换 |
| **jrdb AutoCrud** | **~2.05M** | ASM映射 + 缓存SQL生成 |

**点评：jrdb性能领先6-10倍，核心优势是ASM字节码映射。**

## 6. 适用场景对比

| 场景 | MyBatis | Hibernate/JPA | jrdb ASM | jrdb Annotation | jrdb AutoCrud |
|------|---------|---------------|----------|----------------|---------------|
| **复杂SQL（JOIN、子查询）** | ✅ 最强 | ⚠️ HQL受限 | ✅ 最强 | ✅ 强 | ❌ 不支持 |
| **动态SQL** | ✅ XML最强 | ⚠️ Criteria复杂 | ⚠️ 代码拼接 | ⚠️ 有限支持 | ❌ 不支持 |
| **简单CRUD** | ⚠️ 冗余 | ⚠️ 过重 | ✅ 直接 | ✅ 便捷 | ✅ 最便捷 |
| **极致性能** | ❌ 慢 | ❌ 慢 | ✅ 最快 | ✅ 快 | ✅ 快 |
| **ETL/流式处理** | ⚠️ 有 | ❌ 无 | ✅ RowHandler | ❌ 无 | ❌ 无 |
| **批量导入** | ⚠️ 中等 | ⚠️ 中等 | ✅ DirectSql +50% | ❌ 无 | ❌ 无 |
| **事务管理** | ✅ 强 | ✅ 最强 | ⚠️ 手动 | ⚠️ 手动 | ⚠️ 手动 |
| **懒加载** | ❌ 无 | ✅ 强 | ❌ 无 | ❌ 无 | ❌ 无 |
| **级联保存** | ❌ 无 | ✅ 强 | ❌ 无 | ❌ 无 | ❌ 无 |

**点评：**
- **MyBatis**：SQL控制最强，适合复杂查询、动态SQL
- **Hibernate/JPA**：ORM功能最全，适合复杂业务关系
- **jrdb ASM**：性能最强，适合读多写少、高吞吐场景
- **jrdb Annotation**：性能+便捷平衡，适合中小项目
- **jrdb AutoCrud**：最便捷，适合简单CRUD原型

## 7. 学习曲线对比

| 框架 | 学习难度 | 核心概念数量 |
|------|---------|-------------|
| **iBATIS** | 低 | 3个（Mapper/SQL/ResultMap） |
| **MyBatis** | 中 | 5个（Mapper/SQL/注解/动态SQL/缓存） |
| **Hibernate** | 高 | 10+（Entity/Session/Transaction/缓存/懒加载/级联...） |
| **JPA** | 高 | 同Hibernate + JPQL规范 |
| **jrdb ASM** | 低 | 3个（Session/SQL/Mapper） |
| **jrdb Annotation** | 中 | 5个（Session/DAO/注解/Style/AutoCrud） |
| **jrdb AutoCrud** | 低 | 2个（DAO/AutoCrud） |

**点评：jrdb ASM学习曲线最低，仅需Session+SQL。**

## 8. 综合推荐矩阵

| 项目特征 | 推荐框架 | 原因 |
|---------|---------|------|
| **大型项目 + 复杂SQL** | MyBatis XML | SQL集中管理，动态SQL强 |
| **大型项目 + 复杂业务关系** | Hibernate/JPA | ORM功能全，级联强 |
| **中小项目 + 追求性能** | jrdb Annotation | 性能+便捷平衡 |
| **极致性能场景** | jrdb ASM | 最快映射，最大控制 |
| **简单CRUD原型** | jrdb AutoCrud | 零SQL，快速开发 |
| **ETL/数据导出** | jrdb RowHandler | 流式处理，零对象创建 |
| **批量导入场景** | jrdb DirectSql | +50%性能 |

## 9. jrdb独特优势总结

| 特性 | jrdb | 其他框架 |
|------|------|---------|
| **5层API** | ASM/Annotation/AutoCrud/Raw/Streaming | 通常1-2层 |
| **三种参数风格** | ?/#/: 三选一 | 通常固定一种 |
| **ASM映射零反射** | 性能领先6-10x | 反射开销大 |
| **堆外内存** | 避免GC压力 | 对象创建多 |
| **DirectSql批量** | +50%性能 | PreparedStatement开销 |
| **RowHandler流式** | ETL零对象 | 需创建List |

## 10. jrdb局限性

| 局限 | 影响 | 替代方案 |
|------|------|---------|
| **无懒加载** | 不适合复杂关联查询 | 用JOIN + ASM直接映射 |
| **无级联保存** | 需手动保存关联Entity | 循环调用insert/update |
| **无动态SQL** | 不适合条件复杂查询 | 代码拼接SQL或用MyBatis |
| **事务管理弱** | 需手动管理事务 | 用Spring @Transactional |
| **无二级ORM功能** | 仅数据映射 | 复杂业务用Hibernate |

## 11. 结论：jrdb定位

**jrdb = 读优化型框架，适合读多写少、追求极致性能场景**

**核心定位：**
- **性能优先**：ASM映射领先6-10x
- **SQL可见**：100%控制SQL（ASM/Annotation）
- **轻量便捷**：AutoCrud零SQL快速开发
- **流式处理**：RowHandler适合ETL

**不适合场景：**
- 复杂业务关系（级联、懒加载）→ 用Hibernate/JPA
- 动态SQL复杂条件 → 用MyBatis XML
- 写入密集型 → 性能优势不明显

**最佳组合：**
- **jrdb（读取）+ MyBatis（复杂查询）** - 读性能优化 + SQL灵活性
- **jrdb（ETL）+ Hibernate（业务）** - 批量处理 + ORM功能
- **jrdb（全栈）** - 读多写少高性能项目

---

**一句话总结：jrdb是"高性能版MyBatis注解模式"，牺牲ORM高级功能换取极致读性能。**