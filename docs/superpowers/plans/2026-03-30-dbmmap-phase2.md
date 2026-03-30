# dbmmap Phase 2 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现dbmmap映射能力 - 类型处理、命名策略、Mapper映射器、ASM动态代码生成

**Architecture:** TypeHandler接口+内置实现处理JDBC类型转换；NameStrategy接口+默认实现处理命名转换；Mapper接口定义映射规则；ASM动态生成POJO映射代码。

**Tech Stack:** Java 8+, ASM 9.x, Maven, JUnit 5

---

## 文件结构

```
dbmmap/
├── pom.xml                          # 添加ASM依赖
├── dbmmap-core/
│   └── src/
│       ├── main/java/com/dbmmap/core/
│       │   ├── type/
│       │   │   ├── TypeHandler.java
│       │   │   ├── TypeHandlerRegistry.java
│       │   │   ├── handlers/
│       │   │   │   ├── IntegerTypeHandler.java
│       │   │   │   ├── LongTypeHandler.java
│       │   │   │   ├── StringTypeHandler.java
│       │   │   │   ├── BooleanTypeHandler.java
│       │   │   │   ├── DateTypeHandler.java
│       │   │   │   ├── BigDecimalTypeHandler.java
│       │   │   │   └── EnumTypeHandler.java
│       │   │   └── TypeAliasRegistry.java
│       │   ├── mapping/
│       │   │   ├── NameStrategy.java
│       │   │   ├── DefaultNameStrategy.java
│       │   │   ├── MappingField.java
│       │   │   ├── MappingConfig.java
│       │   │   └── MapperRegistry.java
│       │   └── asm/
│       │       ├── MapperClassGenerator.java
│       │       └── MapperClassLoader.java
│       └── test/java/com/dbmmap/core/
│           ├── type/
│           ├── mapping/
│           └── asm/
└── dbmmap-mapping/                   # 独立映射模块(可选，Phase 2后期)
```

---

## Task 1: 添加ASM依赖

**Files:**
- Modify: `pom.xml`
- Modify: `dbmmap-core/pom.xml`

- [ ] **Step 1: 添加ASM到父POM dependencyManagement**

在`pom.xml`的`<dependencyManagement>`中添加：

```xml
<dependency>
    <groupId>org.ow2.asm</groupId>
    <artifactId>asm</artifactId>
    <version>9.6</version>
</dependency>
<dependency>
    <groupId>org.ow2.asm</groupId>
    <artifactId>asm-commons</artifactId>
    <version>9.6</version>
</dependency>
```

- [ ] **Step 2: 添加ASM到dbmmap-core依赖**

在`dbmmap-core/pom.xml`的`<dependencies>`中添加：

```xml
<dependency>
    <groupId>org.ow2.asm</groupId>
    <artifactId>asm</artifactId>
</dependency>
<dependency>
    <groupId>org.ow2.asm</groupId>
    <artifactId>asm-commons</artifactId>
</dependency>
```

- [ ] **Step 3: 创建type和mapping目录**

```bash
mkdir -p dbmmap-core/src/main/java/com/dbmmap/core/type/handlers
mkdir -p dbmmap-core/src/main/java/com/dbmmap/core/mapping
mkdir -p dbmmap-core/src/main/java/com/dbmmap/core/asm
mkdir -p dbmmap-core/src/test/java/com/dbmmap/core/type
mkdir -p dbmmap-core/src/test/java/com/dbmmap/core/mapping
mkdir -p dbmmap-core/src/test/java/com/dbmmap/core/asm
```

- [ ] **Step 4: 验证依赖**

```bash
cd dbmmap-core && mvn dependency:tree | grep asm
```

- [ ] **Step 5: 提交**

```bash
git add pom.xml dbmmap-core/pom.xml
git commit -m "feat: add ASM dependency for dynamic code generation"
```

---

## Task 2: TypeHandler接口和注册器

**Files:**
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/type/TypeHandler.java`
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/type/TypeHandlerRegistry.java`
- Create: `dbmmap-core/src/test/java/com/dbmmap/core/type/TypeHandlerRegistryTest.java`

- [ ] **Step 1: 编写TypeHandler接口**

```java
package com.dbmmap.core.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 类型处理器接口，处理Java类型与JDBC类型之间的转换
 */
public interface TypeHandler<T> {
    
    /**
     * 从ResultSet获取值
     */
    T getResult(ResultSet rs, String columnName) throws SQLException;
    
    /**
     * 从ResultSet获取值（按列索引）
     */
    T getResult(ResultSet rs, int columnIndex) throws SQLException;
    
    /**
     * 设置PreparedStatement参数
     */
    void setParameter(PreparedStatement ps, int i, T parameter) throws SQLException;
    
    /**
     * 获取处理的Java类型
     */
    Class<T> getJavaType();
}
```

- [ ] **Step 2: 编写TypeHandlerRegistry**

```java
package com.dbmmap.core.type;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类型处理器注册中心
 */
public class TypeHandlerRegistry {
    
    private final Map<Class<?>, TypeHandler<?>> javaTypeHandlerMap = new ConcurrentHashMap<>();
    private final Map<Integer, TypeHandler<?>> sqlTypeHandlerMap = new ConcurrentHashMap<>();
    
    public TypeHandlerRegistry() {
        registerBuiltInHandlers();
    }
    
    private void registerBuiltInHandlers() {
        // 基础类型将在后续Task中实现
    }
    
    public <T> void register(Class<T> javaType, TypeHandler<T> handler) {
        javaTypeHandlerMap.put(javaType, handler);
    }
    
    public void register(int sqlType, TypeHandler<?> handler) {
        sqlTypeHandlerMap.put(sqlType, handler);
    }
    
    @SuppressWarnings("unchecked")
    public <T> TypeHandler<T> getTypeHandler(Class<T> javaType) {
        return (TypeHandler<T>) javaTypeHandlerMap.get(javaType);
    }
    
    public TypeHandler<?> getTypeHandler(int sqlType) {
        return sqlTypeHandlerMap.get(sqlType);
    }
    
    public boolean hasTypeHandler(Class<?> javaType) {
        return javaTypeHandlerMap.containsKey(javaType);
    }
}
```

- [ ] **Step 3: 编写测试**

```java
package com.dbmmap.core.type;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TypeHandlerRegistryTest {

    private TypeHandlerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TypeHandlerRegistry();
    }

    @Test
    void shouldRegisterTypeHandler() {
        TypeHandler<String> handler = new StringTypeHandler();
        registry.register(String.class, handler);
        
        assertEquals(handler, registry.getTypeHandler(String.class));
    }

    @Test
    void shouldReturnNullForUnregisteredType() {
        assertNull(registry.getTypeHandler(UnregisteredClass.class));
    }

    @Test
    void shouldCheckIfTypeHandlerExists() {
        registry.register(String.class, new StringTypeHandler());
        
        assertTrue(registry.hasTypeHandler(String.class));
        assertFalse(registry.hasTypeHandler(UnregisteredClass.class));
    }

    static class UnregisteredClass {}
}
```

- [ ] **Step 4: 运行测试**

```bash
cd dbmmap-core && mvn test -Dtest=TypeHandlerRegistryTest
```

- [ ] **Step 5: 提交**

```bash
git add dbmmap-core/src/main/java/com/dbmmap/core/type/ dbmmap-core/src/test/java/com/dbmmap/core/type/
git commit -m "feat(type): add TypeHandler interface and registry"
```

---

## Task 3: 基础类型Handler实现

**Files:**
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/type/handlers/IntegerTypeHandler.java`
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/type/handlers/LongTypeHandler.java`
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/type/handlers/StringTypeHandler.java`
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/type/handlers/BooleanTypeHandler.java`
- Create: `dbmmap-core/src/test/java/com/dbmmap/core/type/handlers/TypeHandlersTest.java`

- [ ] **Step 1: 实现IntegerTypeHandler**

```java
package com.dbmmap.core.type.handlers;

import com.dbmmap.core.type.TypeHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class IntegerTypeHandler implements TypeHandler<Integer> {
    
    @Override
    public Integer getResult(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
    
    @Override
    public Integer getResult(ResultSet rs, int columnIndex) throws SQLException {
        int value = rs.getInt(columnIndex);
        return rs.wasNull() ? null : value;
    }
    
    @Override
    public void setParameter(PreparedStatement ps, int i, Integer parameter) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, java.sql.Types.INTEGER);
        } else {
            ps.setInt(i, parameter);
        }
    }
    
    @Override
    public Class<Integer> getJavaType() {
        return Integer.class;
    }
}
```

- [ ] **Step 2: 实现LongTypeHandler**

```java
package com.dbmmap.core.type.handlers;

import com.dbmmap.core.type.TypeHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LongTypeHandler implements TypeHandler<Long> {
    
    @Override
    public Long getResult(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }
    
    @Override
    public Long getResult(ResultSet rs, int columnIndex) throws SQLException {
        long value = rs.getLong(columnIndex);
        return rs.wasNull() ? null : value;
    }
    
    @Override
    public void setParameter(PreparedStatement ps, int i, Long parameter) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, java.sql.Types.BIGINT);
        } else {
            ps.setLong(i, parameter);
        }
    }
    
    @Override
    public Class<Long> getJavaType() {
        return Long.class;
    }
}
```

- [ ] **Step 3: 实现StringTypeHandler**

```java
package com.dbmmap.core.type.handlers;

import com.dbmmap.core.type.TypeHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StringTypeHandler implements TypeHandler<String> {
    
    @Override
    public String getResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }
    
    @Override
    public String getResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }
    
    @Override
    public void setParameter(PreparedStatement ps, int i, String parameter) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, java.sql.Types.VARCHAR);
        } else {
            ps.setString(i, parameter);
        }
    }
    
    @Override
    public Class<String> getJavaType() {
        return String.class;
    }
}
```

- [ ] **Step 4: 实现BooleanTypeHandler**

```java
package com.dbmmap.core.type.handlers;

import com.dbmmap.core.type.TypeHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BooleanTypeHandler implements TypeHandler<Boolean> {
    
    @Override
    public Boolean getResult(ResultSet rs, String columnName) throws SQLException {
        boolean value = rs.getBoolean(columnName);
        return rs.wasNull() ? null : value;
    }
    
    @Override
    public Boolean getResult(ResultSet rs, int columnIndex) throws SQLException {
        boolean value = rs.getBoolean(columnIndex);
        return rs.wasNull() ? null : value;
    }
    
    @Override
    public void setParameter(PreparedStatement ps, int i, Boolean parameter) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, java.sql.Types.BOOLEAN);
        } else {
            ps.setBoolean(i, parameter);
        }
    }
    
    @Override
    public Class<Boolean> getJavaType() {
        return Boolean.class;
    }
}
```

- [ ] **Step 5: 编写综合测试**

```java
package com.dbmmap.core.type.handlers;

import com.dbmmap.core.type.TypeHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TypeHandlersTest {

    @Test
    void integerHandlerShouldHandleNull() throws Exception {
        TypeHandler<Integer> handler = new IntegerTypeHandler();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("col")).thenReturn(0);
        when(rs.wasNull()).thenReturn(true);
        
        assertNull(handler.getResult(rs, "col"));
    }

    @Test
    void integerHandlerShouldHandleValue() throws Exception {
        TypeHandler<Integer> handler = new IntegerTypeHandler();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("col")).thenReturn(42);
        when(rs.wasNull()).thenReturn(false);
        
        assertEquals(42, handler.getResult(rs, "col"));
    }

    @Test
    void stringHandlerShouldHandleValue() throws Exception {
        TypeHandler<String> handler = new StringTypeHandler();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("hello");
        
        assertEquals("hello", handler.getResult(rs, "col"));
    }

    @Test
    void longHandlerShouldHandleValue() throws Exception {
        TypeHandler<Long> handler = new LongTypeHandler();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong(1)).thenReturn(12345L);
        when(rs.wasNull()).thenReturn(false);
        
        assertEquals(12345L, handler.getResult(rs, 1));
    }

    @Test
    void booleanHandlerShouldHandleValue() throws Exception {
        TypeHandler<Boolean> handler = new BooleanTypeHandler();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBoolean("col")).thenReturn(true);
        when(rs.wasNull()).thenReturn(false);
        
        assertTrue(handler.getResult(rs, "col"));
    }

    @Test
    void shouldSetNullParameter() throws Exception {
        TypeHandler<Integer> handler = new IntegerTypeHandler();
        PreparedStatement ps = mock(PreparedStatement.class);
        
        handler.setParameter(ps, 1, null);
        
        verify(ps).setNull(1, java.sql.Types.INTEGER);
    }
}
```

- [ ] **Step 6: 更新TypeHandlerRegistry注册内置Handler**

在`TypeHandlerRegistry`的`registerBuiltInHandlers()`方法中添加：

```java
private void registerBuiltInHandlers() {
    register(Integer.class, new IntegerTypeHandler());
    register(int.class, new IntegerTypeHandler());
    register(Long.class, new LongTypeHandler());
    register(long.class, new LongTypeHandler());
    register(String.class, new StringTypeHandler());
    register(Boolean.class, new BooleanTypeHandler());
    register(boolean.class, new BooleanTypeHandler());
}
```

- [ ] **Step 7: 运行测试**

```bash
cd dbmmap-core && mvn test -Dtest=TypeHandlersTest,TypeHandlerRegistryTest
```

- [ ] **Step 8: 提交**

```bash
git add dbmmap-core/src/main/java/com/dbmmap/core/type/ dbmmap-core/src/test/java/com/dbmmap/core/type/
git commit -m "feat(type): add built-in type handlers (Integer, Long, String, Boolean)"
```

---

## Task 4: 扩展类型Handler (Date, BigDecimal, Enum)

**Files:**
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/type/handlers/DateTypeHandler.java`
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/type/handlers/BigDecimalTypeHandler.java`
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/type/handlers/EnumTypeHandler.java`
- Create: `dbmmap-core/src/test/java/com/dbmmap/core/type/handlers/ExtendedTypeHandlersTest.java`

- [ ] **Step 1: 实现DateTypeHandler**

```java
package com.dbmmap.core.type.handlers;

import com.dbmmap.core.type.TypeHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

public class DateTypeHandler implements TypeHandler<Date> {
    
    @Override
    public Date getResult(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp != null ? new Date(timestamp.getTime()) : null;
    }
    
    @Override
    public Date getResult(ResultSet rs, int columnIndex) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnIndex);
        return timestamp != null ? new Date(timestamp.getTime()) : null;
    }
    
    @Override
    public void setParameter(PreparedStatement ps, int i, Date parameter) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, java.sql.Types.TIMESTAMP);
        } else {
            ps.setTimestamp(i, new Timestamp(parameter.getTime()));
        }
    }
    
    @Override
    public Class<Date> getJavaType() {
        return Date.class;
    }
}
```

- [ ] **Step 2: 实现BigDecimalTypeHandler**

```java
package com.dbmmap.core.type.handlers;

import com.dbmmap.core.type.TypeHandler;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BigDecimalTypeHandler implements TypeHandler<BigDecimal> {
    
    @Override
    public BigDecimal getResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getBigDecimal(columnName);
    }
    
    @Override
    public BigDecimal getResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getBigDecimal(columnIndex);
    }
    
    @Override
    public void setParameter(PreparedStatement ps, int i, BigDecimal parameter) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, java.sql.Types.DECIMAL);
        } else {
            ps.setBigDecimal(i, parameter);
        }
    }
    
    @Override
    public Class<BigDecimal> getJavaType() {
        return BigDecimal.class;
    }
}
```

- [ ] **Step 3: 实现EnumTypeHandler**

```java
package com.dbmmap.core.type.handlers;

import com.dbmmap.core.type.TypeHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EnumTypeHandler<E extends Enum<E>> implements TypeHandler<E> {
    
    private final Class<E> enumType;
    
    public EnumTypeHandler(Class<E> enumType) {
        this.enumType = enumType;
    }
    
    @Override
    public E getResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value != null ? Enum.valueOf(enumType, value) : null;
    }
    
    @Override
    public E getResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value != null ? Enum.valueOf(enumType, value) : null;
    }
    
    @Override
    public void setParameter(PreparedStatement ps, int i, E parameter) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, java.sql.Types.VARCHAR);
        } else {
            ps.setString(i, parameter.name());
        }
    }
    
    @Override
    public Class<E> getJavaType() {
        return enumType;
    }
}
```

- [ ] **Step 4: 编写测试**

```java
package com.dbmmap.core.type.handlers;

import com.dbmmap.core.type.TypeHandler;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExtendedTypeHandlersTest {

    @Test
    void dateHandlerShouldHandleValue() throws Exception {
        TypeHandler<Date> handler = new DateTypeHandler();
        ResultSet rs = mock(ResultSet.class);
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        when(rs.getTimestamp("col")).thenReturn(ts);
        
        Date result = handler.getResult(rs, "col");
        assertEquals(ts.getTime(), result.getTime());
    }

    @Test
    void dateHandlerShouldHandleNull() throws Exception {
        TypeHandler<Date> handler = new DateTypeHandler();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp("col")).thenReturn(null);
        
        assertNull(handler.getResult(rs, "col"));
    }

    @Test
    void bigDecimalHandlerShouldHandleValue() throws Exception {
        TypeHandler<BigDecimal> handler = new BigDecimalTypeHandler();
        ResultSet rs = mock(ResultSet.class);
        BigDecimal expected = new BigDecimal("123.45");
        when(rs.getBigDecimal("col")).thenReturn(expected);
        
        assertEquals(expected, handler.getResult(rs, "col"));
    }

    @Test
    void enumHandlerShouldHandleValue() throws Exception {
        TypeHandler<TestEnum> handler = new EnumTypeHandler<>(TestEnum.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("VALUE1");
        
        assertEquals(TestEnum.VALUE1, handler.getResult(rs, "col"));
    }

    @Test
    void enumHandlerShouldHandleNull() throws Exception {
        TypeHandler<TestEnum> handler = new EnumTypeHandler<>(TestEnum.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn(null);
        
        assertNull(handler.getResult(rs, "col"));
    }

    enum TestEnum {
        VALUE1, VALUE2
    }
}
```

- [ ] **Step 5: 更新TypeHandlerRegistry**

```java
register(Date.class, new DateTypeHandler());
register(java.sql.Date.class, new DateTypeHandler());
register(BigDecimal.class, new BigDecimalTypeHandler());
```

- [ ] **Step 6: 运行测试并提交**

```bash
cd dbmmap-core && mvn test -Dtest=ExtendedTypeHandlersTest
git add . && git commit -m "feat(type): add extended type handlers (Date, BigDecimal, Enum)"
```

---

## Task 5: NameStrategy命名策略

**Files:**
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/mapping/NameStrategy.java`
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/mapping/DefaultNameStrategy.java`
- Create: `dbmmap-core/src/test/java/com/dbmmap/core/mapping/NameStrategyTest.java`

- [ ] **Step 1: 实现NameStrategy接口**

```java
package com.dbmmap.core.mapping;

/**
 * 命名策略接口，处理列名到字段名的转换
 */
public interface NameStrategy {
    
    /**
     * 将列名转换为字段名
     */
    String columnNameToFieldName(String columnName);
    
    /**
     * 将字段名转换为列名
     */
    String fieldNameToColumnName(String fieldName);
    
    /**
     * 获取策略名称
     */
    String getName();
}
```

- [ ] **Step 2: 实现DefaultNameStrategy**

```java
package com.dbmmap.core.mapping;

/**
 * 默认命名策略：下划线转驼峰
 */
public class DefaultNameStrategy implements NameStrategy {
    
    public static final String NAME = "default";
    
    @Override
    public String columnNameToFieldName(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            return columnName;
        }
        
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;
        
        for (int i = 0; i < columnName.length(); i++) {
            char c = columnName.charAt(i);
            if (c == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    result.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        
        return result.toString();
    }
    
    @Override
    public String fieldNameToColumnName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return fieldName;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    @Override
    public String getName() {
        return NAME;
    }
}
```

- [ ] **Step 3: 编写测试**

```java
package com.dbmmap.core.mapping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NameStrategyTest {

    private final NameStrategy strategy = new DefaultNameStrategy();

    @Test
    void shouldConvertColumnNameToFieldName() {
        assertEquals("userId", strategy.columnNameToFieldName("user_id"));
        assertEquals("userName", strategy.columnNameToFieldName("user_name"));
        assertEquals("id", strategy.columnNameToFieldName("id"));
        assertEquals("firstName", strategy.columnNameToFieldName("first_name"));
    }

    @Test
    void shouldConvertFieldNameToColumnName() {
        assertEquals("user_id", strategy.fieldNameToColumnName("userId"));
        assertEquals("user_name", strategy.fieldNameToColumnName("userName"));
        assertEquals("id", strategy.fieldNameToColumnName("id"));
        assertEquals("first_name", strategy.fieldNameToColumnName("firstName"));
    }

    @Test
    void shouldHandleNullAndEmpty() {
        assertNull(strategy.columnNameToFieldName(null));
        assertEquals("", strategy.columnNameToFieldName(""));
        assertNull(strategy.fieldNameToColumnName(null));
        assertEquals("", strategy.fieldNameToColumnName(""));
    }

    @Test
    void shouldHandleConsecutiveUnderscores() {
        assertEquals("userId", strategy.columnNameToFieldName("user__id"));
    }

    @Test
    void shouldGetName() {
        assertEquals("default", strategy.getName());
    }
}
```

- [ ] **Step 4: 运行测试并提交**

```bash
cd dbmmap-core && mvn test -Dtest=NameStrategyTest
git add . && git commit -m "feat(mapping): add NameStrategy with default underscore-to-camel conversion"
```

---

## Task 6: MappingField和MappingConfig

**Files:**
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/mapping/MappingField.java`
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/mapping/MappingConfig.java`
- Create: `dbmmap-core/src/test/java/com/dbmmap/core/mapping/MappingConfigTest.java`

- [ ] **Step 1: 实现MappingField**

```java
package com.dbmmap.core.mapping;

import com.dbmmap.core.type.TypeHandler;

/**
 * 映射字段配置
 */
public class MappingField {
    
    private final String fieldName;
    private final String columnName;
    private final Class<?> javaType;
    private TypeHandler<?> typeHandler;
    private boolean id;
    
    public MappingField(String fieldName, String columnName, Class<?> javaType) {
        this.fieldName = fieldName;
        this.columnName = columnName;
        this.javaType = javaType;
        this.id = false;
    }
    
    public String getFieldName() { return fieldName; }
    public String getColumnName() { return columnName; }
    public Class<?> getJavaType() { return javaType; }
    public TypeHandler<?> getTypeHandler() { return typeHandler; }
    public boolean isId() { return id; }
    
    public MappingField typeHandler(TypeHandler<?> typeHandler) {
        this.typeHandler = typeHandler;
        return this;
    }
    
    public MappingField id(boolean id) {
        this.id = id;
        return this;
    }
}
```

- [ ] **Step 2: 实现MappingConfig**

```java
package com.dbmmap.core.mapping;

import com.dbmmap.core.type.TypeHandlerRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 映射配置
 */
public class MappingConfig {
    
    private final Class<?> entityClass;
    private final String tableName;
    private final List<MappingField> fields = new ArrayList<>();
    private final Map<String, MappingField> fieldNameMap = new HashMap<>();
    private final Map<String, MappingField> columnNameMap = new HashMap<>();
    private NameStrategy nameStrategy = new DefaultNameStrategy();
    private TypeHandlerRegistry typeHandlerRegistry;
    
    public MappingConfig(Class<?> entityClass) {
        this.entityClass = entityClass;
        this.tableName = entityClass.getSimpleName().toLowerCase();
    }
    
    public MappingConfig(Class<?> entityClass, String tableName) {
        this.entityClass = entityClass;
        this.tableName = tableName;
    }
    
    public MappingConfig field(String fieldName, String columnName, Class<?> javaType) {
        MappingField field = new MappingField(fieldName, columnName, javaType);
        fields.add(field);
        fieldNameMap.put(fieldName, field);
        columnNameMap.put(columnName, field);
        return this;
    }
    
    public MappingConfig field(String fieldName, Class<?> javaType) {
        String columnName = nameStrategy.fieldNameToColumnName(fieldName);
        return field(fieldName, columnName, javaType);
    }
    
    public MappingConfig idField(String fieldName, Class<?> javaType) {
        String columnName = nameStrategy.fieldNameToColumnName(fieldName);
        MappingField field = new MappingField(fieldName, columnName, javaType);
        field.id(true);
        fields.add(field);
        fieldNameMap.put(fieldName, field);
        columnNameMap.put(columnName, field);
        return this;
    }
    
    public MappingConfig nameStrategy(NameStrategy nameStrategy) {
        this.nameStrategy = nameStrategy;
        return this;
    }
    
    public MappingConfig typeHandlerRegistry(TypeHandlerRegistry registry) {
        this.typeHandlerRegistry = registry;
        return this;
    }
    
    public Class<?> getEntityClass() { return entityClass; }
    public String getTableName() { return tableName; }
    public List<MappingField> getFields() { return fields; }
    public MappingField getFieldByFieldName(String fieldName) { return fieldNameMap.get(fieldName); }
    public MappingField getFieldByColumnName(String columnName) { return columnNameMap.get(columnName); }
    public NameStrategy getNameStrategy() { return nameStrategy; }
    public TypeHandlerRegistry getTypeHandlerRegistry() { return typeHandlerRegistry; }
}
```

- [ ] **Step 3: 编写测试**

```java
package com.dbmmap.core.mapping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MappingConfigTest {

    @Test
    void shouldCreateMappingConfig() {
        MappingConfig config = new MappingConfig(User.class)
            .field("id", "user_id", Long.class)
            .field("name", "user_name", String.class);
        
        assertEquals(User.class, config.getEntityClass());
        assertEquals("user", config.getTableName());
        assertEquals(2, config.getFields().size());
    }

    @Test
    void shouldAutoConvertFieldName() {
        MappingConfig config = new MappingConfig(User.class)
            .field("userId", Long.class)
            .field("userName", String.class);
        
        assertEquals("user_id", config.getFieldByFieldName("userId").getColumnName());
        assertEquals("user_name", config.getFieldByFieldName("userName").getColumnName());
    }

    @Test
    void shouldSetIdField() {
        MappingConfig config = new MappingConfig(User.class)
            .idField("id", Long.class);
        
        assertTrue(config.getFieldByFieldName("id").isId());
    }

    @Test
    void shouldGetFieldByColumnName() {
        MappingConfig config = new MappingConfig(User.class)
            .field("userId", "user_id", Long.class);
        
        MappingField field = config.getFieldByColumnName("user_id");
        assertNotNull(field);
        assertEquals("userId", field.getFieldName());
    }

    static class User {
        private Long id;
        private String name;
    }
}
```

- [ ] **Step 4: 运行测试并提交**

```bash
cd dbmmap-core && mvn test -Dtest=MappingConfigTest
git add . && git commit -m "feat(mapping): add MappingField and MappingConfig for field mapping"
```

---

## Task 7: ASM动态代码生成

**Files:**
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/asm/MapperClassGenerator.java`
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/asm/MapperClassLoader.java`
- Create: `dbmmap-core/src/test/java/com/dbmmap/core/asm/MapperClassGeneratorTest.java`

- [ ] **Step 1: 实现MapperClassLoader**

```java
package com.dbmmap.core.asm;

/**
 * 自定义类加载器，加载ASM生成的类
 */
public class MapperClassLoader extends ClassLoader {
    
    public MapperClassLoader() {
        super(MapperClassLoader.class.getClassLoader());
    }
    
    public Class<?> defineClass(String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }
}
```

- [ ] **Step 2: 实现MapperClassGenerator**

```java
package com.dbmmap.core.asm;

import com.dbmmap.core.mapping.MappingConfig;
import com.dbmmap.core.mapping.MappingField;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Field;

/**
 * 使用ASM动态生成Mapper实现类
 */
public class MapperClassGenerator implements Opcodes {
    
    private static final String MAPPER_SUFFIX = "$MapperImpl";
    
    /**
     * 生成Mapper实现类的字节码
     */
    public byte[] generate(MappingConfig config) {
        String className = config.getEntityClass().getName().replace('.', '/') + MAPPER_SUFFIX;
        String entityClassName = config.getEntityClass().getName().replace('.', '/');
        
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        
        // 定义类
        cw.visit(V1_8, ACC_PUBLIC, className, null, "java/lang/Object", 
            new String[]{"com/dbmmap/core/mapping/EntityMapper"});
        
        // 添加默认构造方法
        generateConstructor(cw);
        
        // 生成mapFromResultSet方法
        generateMapFromResultSet(cw, config, className, entityClassName);
        
        // 生成mapToRow方法
        generateMapToRow(cw, config, className, entityClassName);
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    private void generateConstructor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }
    
    private void generateMapFromResultSet(ClassWriter cw, MappingConfig config, 
            String className, String entityClassName) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "mapFromResultSet", 
            "(Ljava/sql/ResultSet;)Ljava/lang/Object;", null, 
            new String[]{"java/sql/SQLException"});
        
        mv.visitCode();
        
        // 创建实体对象
        mv.visitTypeInsn(NEW, entityClassName);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, entityClassName, "<init>", "()V", false);
        mv.visitVarInsn(ASTORE, 2);
        
        // 设置字段值
        for (MappingField field : config.getFields()) {
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn(field.getColumnName());
            
            String descriptor = getTypeDescriptor(field.getJavaType());
            String resultSetMethod = getResultSetMethod(field.getJavaType());
            
            mv.visitMethodInsn(INVOKEINTERFACE, "java/sql/ResultSet", resultSetMethod, 
                "(Ljava/lang/String;)" + descriptor, true);
            
            String fieldDesc = descriptor;
            if (field.getJavaType() == int.class) fieldDesc = "I";
            else if (field.getJavaType() == long.class) fieldDesc = "J";
            else if (field.getJavaType() == boolean.class) fieldDesc = "Z";
            
            mv.visitFieldInsn(PUTFIELD, entityClassName, field.getFieldName(), fieldDesc);
        }
        
        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }
    
    private void generateMapToRow(ClassWriter cw, MappingConfig config, 
            String className, String entityClassName) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "mapToRow", 
            "(Ljava/lang/Object;)[Ljava/lang/Object;", null, null);
        
        mv.visitCode();
        
        int fieldCount = config.getFields().size();
        mv.visitIntInsn(BIPUSH, fieldCount);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mv.visitVarInsn(ASTORE, 2);
        
        int index = 0;
        for (MappingField field : config.getFields()) {
            mv.visitVarInsn(ALOAD, 2);
            mv.visitIntInsn(BIPUSH, index);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, entityClassName);
            
            String fieldDesc = getTypeDescriptor(field.getJavaType());
            mv.visitFieldInsn(GETFIELD, entityClassName, field.getFieldName(), fieldDesc);
            
            if (field.getJavaType().isPrimitive()) {
                boxPrimitive(mv, field.getJavaType());
            }
            
            mv.visitInsn(AASTORE);
            index++;
        }
        
        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }
    
    private String getTypeDescriptor(Class<?> type) {
        if (type == int.class) return "I";
        if (type == long.class) return "J";
        if (type == boolean.class) return "Z";
        if (type == double.class) return "D";
        if (type == float.class) return "F";
        if (type == short.class) return "S";
        if (type == byte.class) return "B";
        if (type == char.class) return "C";
        return "L" + type.getName().replace('.', '/') + ";";
    }
    
    private String getResultSetMethod(Class<?> type) {
        if (type == int.class || type == Integer.class) return "getInt";
        if (type == long.class || type == Long.class) return "getLong";
        if (type == boolean.class || type == Boolean.class) return "getBoolean";
        if (type == double.class || type == Double.class) return "getDouble";
        if (type == float.class || type == Float.class) return "getFloat";
        if (type == short.class || type == Short.class) return "getShort";
        if (type == byte.class || type == Byte.class) return "getByte";
        if (type == String.class) return "getString";
        return "getObject";
    }
    
    private void boxPrimitive(MethodVisitor mv, Class<?> type) {
        if (type == int.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        } else if (type == long.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
        } else if (type == boolean.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
        } else if (type == double.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        } else if (type == float.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
        }
    }
    
    public String getMapperClassName(Class<?> entityClass) {
        return entityClass.getName() + MAPPER_SUFFIX;
    }
}
```

- [ ] **Step 3: 创建EntityMapper接口**

```java
package com.dbmmap.core.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 实体映射器接口
 */
public interface EntityMapper<T> {
    
    /**
     * 从ResultSet映射到实体
     */
    T mapFromResultSet(ResultSet rs) throws SQLException;
    
    /**
     * 将实体映射到行数据
     */
    Object[] mapToRow(T entity);
}
```

- [ ] **Step 4: 编写测试**

```java
package com.dbmmap.core.asm;

import com.dbmmap.core.mapping.EntityMapper;
import com.dbmmap.core.mapping.MappingConfig;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MapperClassGeneratorTest {

    @Test
    void shouldGenerateMapperClass() throws Exception {
        MappingConfig config = new MappingConfig(TestEntity.class)
            .field("id", Long.class)
            .field("name", String.class);
        
        MapperClassGenerator generator = new MapperClassGenerator();
        byte[] bytes = generator.generate(config);
        
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void shouldLoadAndUseMapperClass() throws Exception {
        MappingConfig config = new MappingConfig(TestEntity.class)
            .field("id", "id", Long.class)
            .field("name", "name", String.class);
        
        MapperClassGenerator generator = new MapperClassGenerator();
        byte[] bytes = generator.generate(config);
        
        MapperClassLoader loader = new MapperClassLoader();
        String className = generator.getMapperClassName(TestEntity.class);
        Class<?> mapperClass = loader.defineClass(className, bytes);
        
        assertTrue(EntityMapper.class.isAssignableFrom(mapperClass));
    }

    static class TestEntity {
        public Long id;
        public String name;
    }
}
```

- [ ] **Step 5: 运行测试并提交**

```bash
cd dbmmap-core && mvn test -Dtest=MapperClassGeneratorTest
git add . && git commit -m "feat(asm): add ASM-based mapper class generator"
```

---

## Task 8: MapperRegistry整合

**Files:**
- Create: `dbmmap-core/src/main/java/com/dbmmap/core/mapping/MapperRegistry.java`
- Create: `dbmmap-core/src/test/java/com/dbmmap/core/mapping/MapperRegistryTest.java`

- [ ] **Step 1: 实现MapperRegistry**

```java
package com.dbmmap.core.mapping;

import com.dbmmap.core.asm.MapperClassLoader;
import com.dbmmap.core.asm.MapperClassGenerator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mapper注册中心，管理所有实体映射器
 */
public class MapperRegistry {
    
    private final Map<Class<?>, EntityMapper<?>> mapperMap = new ConcurrentHashMap<>();
    private final MapperClassGenerator generator = new MapperClassGenerator();
    private final MapperClassLoader classLoader = new MapperClassLoader();
    
    @SuppressWarnings("unchecked")
    public <T> EntityMapper<T> getMapper(Class<T> entityClass) {
        return (EntityMapper<T>) mapperMap.computeIfAbsent(entityClass, this::createMapper);
    }
    
    public <T> void register(MappingConfig config) {
        EntityMapper<T> mapper = createMapperFromConfig(config);
        mapperMap.put(config.getEntityClass(), mapper);
    }
    
    @SuppressWarnings("unchecked")
    private <T> EntityMapper<T> createMapper(Class<T> entityClass) {
        MappingConfig config = new MappingConfig(entityClass);
        autoConfigureFields(entityClass, config);
        return createMapperFromConfig(config);
    }
    
    private void autoConfigureFields(Class<?> entityClass, MappingConfig config) {
        java.lang.reflect.Field[] fields = entityClass.getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            config.field(field.getName(), field.getType());
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> EntityMapper<T> createMapperFromConfig(MappingConfig config) {
        try {
            byte[] bytes = generator.generate(config);
            String className = generator.getMapperClassName(config.getEntityClass());
            Class<?> mapperClass = classLoader.defineClass(className, bytes);
            return (EntityMapper<T>) mapperClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mapper for " + config.getEntityClass().getName(), e);
        }
    }
    
    public boolean hasMapper(Class<?> entityClass) {
        return mapperMap.containsKey(entityClass);
    }
}
```

- [ ] **Step 2: 编写测试**

```java
package com.dbmmap.core.mapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MapperRegistryTest {

    private MapperRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new MapperRegistry();
    }

    @Test
    void shouldGetMapper() {
        EntityMapper<User> mapper = registry.getMapper(User.class);
        
        assertNotNull(mapper);
        assertTrue(registry.hasMapper(User.class));
    }

    @Test
    void shouldReturnSameMapperInstance() {
        EntityMapper<User> mapper1 = registry.getMapper(User.class);
        EntityMapper<User> mapper2 = registry.getMapper(User.class);
        
        assertSame(mapper1, mapper2);
    }

    @Test
    void shouldRegisterWithConfig() {
        MappingConfig config = new MappingConfig(User.class)
            .field("id", "user_id", Long.class)
            .field("name", "user_name", String.class);
        
        registry.register(config);
        
        assertTrue(registry.hasMapper(User.class));
    }

    static class User {
        public Long id;
        public String name;
    }
}
```

- [ ] **Step 3: 运行全部测试**

```bash
cd dbmmap-core && mvn test
```

- [ ] **Step 4: 提交Phase 2完成**

```bash
git add .
git commit -m "feat(mapping): complete Phase 2 - mapping capabilities

- TypeHandler interface and built-in handlers (Integer, Long, String, Boolean, Date, BigDecimal, Enum)
- TypeHandlerRegistry for type handler management
- NameStrategy for column/field name conversion
- MappingField and MappingConfig for field mapping configuration
- ASM-based MapperClassGenerator for dynamic code generation
- MapperRegistry for entity mapper management"
```

---

## 自检清单

完成后检查：

- [ ] 所有测试通过
- [ ] 无placeholder代码
- [ ] 无TODO注释
- [ ] 类型一致性检查
- [ ] 规格覆盖率检查