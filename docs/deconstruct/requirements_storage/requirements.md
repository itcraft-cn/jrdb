# storage 模块需求文档

## 功能需求

### FR-STO-001: 行式存储

**需求描述：** 系统需要支持按行组织数据。

**验收标准：**
- 支持按行索引存储数据
- 支持按行索引读取数据
- 支持多种数据类型存储
- 计算行大小和最大行数

**优先级：** 高

### FR-STO-002: 列式存储

**需求描述：** 系统需要支持按列组织数据。

**验收标准：**
- 支持获取整列数据
- 支持设置整列数据
- 支持聚合操作（sum, avg, max, min）

**优先级：** 中

### FR-STO-003: 混合视图

**需求描述：** 系统需要支持行式和列式视图切换。

**验收标准：**
- 支持从同一内存块创建行视图
- 支持从同一内存块创建列视图
- 视图切换不涉及数据拷贝

**优先级：** 低

### FR-STO-004: 字符串存储

**需求描述：** 系统需要支持定长字符串存储。

**验收标准：**
- 支持存储 UTF-8 编码字符串
- 支持存储 null 值
- 字符串长度超出时自动截断

**优先级：** 高

## 非功能需求

### NFR-STO-001: 性能要求

- 数据读写应直接操作内存
- Fast 方法应跳过边界检查

### NFR-STO-002: 空间效率

- 行大小应为各列大小之和
- 无额外存储开销

## 使用场景

```java
// 场景1：行式存储
RowStore store = new RowStore(block, columns);
store.setLong(0, 1L, 0);  // 第0行，第0列
store.setString(0, "Alice", 1);  // 第0行，第1列

// 场景2：列式存储
ColumnStore colStore = new ColumnStore(block, columns, 1000);
int[] ids = colStore.getIntColumn(0);
long sum = colStore.sumIntColumn(0);

// 场景3：混合视图
HybridView view = new HybridView(block, columns, 1000);
RowStore rowView = view.toRowView();
ColumnStore colView = view.toColumnView();
```