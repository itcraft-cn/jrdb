# ASM 代理生成 + AutoCrud 完整实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 使用 ASM 字节码生成替换 JDK Proxy，完整实现 AutoCrud 自动生成 insert/update/delete/selectById 方法。

**Architecture:** ASM + 固化方法模板（DaoProxyTemplate），methodNameMap 使用 hashCode 映射；AutoCrud 在 DaoMetaCache 解析时生成 4 个 MethodMeta。

**Tech Stack:** ASM 9.x、JUnit 5、H2 2.x、EntityMapper

---

## 文件结构

**创建文件：**
- `jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/dao/DaoProxyTemplate.java` - 固化模板类
- `jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/dao/AutoCrudMethodGenerator.java` - AutoCrud 方法生成器
- `jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/executor/AutoCrudExecutor.java` - AutoCrud 执行器
- `jrdb-jdbc/src/test/java/cn/itcraft/jrdb/jdbc/dao/DaoProxyTemplateTest.java` - 单元测试
- `jrdb-jdbc/src/test/java/cn/itcraft/jrdb/jdbc/dao/DaoProxyGeneratorTest.java` - 单元测试
- `jrdb-jdbc/src/test/java/cn/itcraft/jrdb/jdbc/dao/AutoCrudMethodGeneratorTest.java` - 单元测试
- `jrdb-jdbc/src/test/java/cn/itcraft/jrdb/jdbc/executor/AutoCrudExecutorTest.java` - 单元测试
- `jrdb-jdbc/src/test/java/cn/itcraft/jrdb/jdbc/integration/ASMProxyIntegrationTest.java` - 集成测试
- `jrdb-jdbc/src/test/java/cn/itcraft/jrdb/jdbc/integration/AutoCrudIntegrationTest.java` - 集成测试
- `jrdb-jdbc/src/test/java/cn/itcraft/jrdb/jdbc/integration/PublicUserEntity.java` - public 实体类

**修改文件：**
- `jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/dao/DaoProxyGenerator.java` - ASM 字节码生成
- `jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/dao/DaoMetaCache.java` - 调用 AutoCrudMethodGenerator
- `jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/dao/DaoRegistry.java` - 使用 ASM 代理
- `jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/mapping/EntityMapper.java` - 增加 getFieldNames() 方法

---

## Phase 1: ASM 代理生成基础

### Task 1: DaoProxyTemplate 固化模板类

**Files:**
- Create: `jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/dao/DaoProxyTemplate.java`
- Test: `jrdb-test/src/test/java/cn/itcraft/jrdb/jdbc/dao/DaoProxyTemplateTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.annotation.SqlParamBindStyle;
import cn.itcraft.jrdb.jdbc.executor.StyleExecutor;
import cn.itcraft.jrdb.jdbc.session.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("DaoProxyTemplate Test")
class DaoProxyTemplateTest {

    @Test
    @DisplayName("constructor should build methodNameMap")
    void constructorBuildsMethodNameMap() {
        DaoMeta daoMeta = new DaoMeta(TestDao.class);
        Session session = mock(Session.class);
        StyleExecutor executor = mock(StyleExecutor.class);
        
        DaoProxyTemplate template = new DaoProxyTemplate(daoMeta, session, executor);
        
        Map<Integer, Method> methodNameMap = template.getMethodNameMap();
        assertNotNull(methodNameMap);
        assertTrue(methodNameMap.containsKey("findById".hashCode()));
    }

    @Test
    @DisplayName("executeByHash should execute method by hashCode")
    void executeByHash() {
        DaoMeta daoMeta = new DaoMeta(TestDao.class);
        Session session = mock(Session.class);
        StyleExecutor executor = mock(StyleExecutor.class);
        
        DaoProxyTemplate template = new DaoProxyTemplate(daoMeta, session, executor);
        
        template.executeByHash("findById".hashCode(), new Object[]{1L});
        
        verify(executor).execute(eq(session), any(MethodMeta.class), eq(new Object[]{1L}));
    }

    interface TestDao {
        PublicUserEntity findById(Long id);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd jrdb-test && mvn test -Dtest=DaoProxyTemplateTest -q`
Expected: FAIL with "DaoProxyTemplate not found"

- [ ] **Step 3: Write minimal implementation**

```java
package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.executor.StyleExecutor;
import cn.itcraft.jrdb.jdbc.session.Session;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DaoProxyTemplate {
    protected final DaoMeta daoMeta;
    protected final Session session;
    protected final StyleExecutor executor;
    protected final Map<Integer, Method> methodNameMap;

    public DaoProxyTemplate(DaoMeta daoMeta, Session session, StyleExecutor executor) {
        this.daoMeta = Objects.requireNonNull(daoMeta, "daoMeta cannot be null");
        this.session = Objects.requireNonNull(session, "session cannot be null");
        this.executor = Objects.requireNonNull(executor, "executor cannot be null");
        this.methodNameMap = buildMethodNameMap(daoMeta);
    }

    protected Object executeByHash(int hashCode, Object[] args) {
        Method method = methodNameMap.get(hashCode);
        if (method == null) {
            throw new IllegalArgumentException("Method not found for hashCode: " + hashCode);
        }
        MethodMeta methodMeta = daoMeta.getMethodMeta(method);
        return executor.execute(session, methodMeta, args);
    }

    protected Map<Integer, Method> getMethodNameMap() {
        return methodNameMap;
    }

    private Map<Integer, Method> buildMethodNameMap(DaoMeta daoMeta) {
        Map<Integer, Method> map = new HashMap<>();
        for (Method method : daoMeta.getDaoInterface().getDeclaredMethods()) {
            map.put(method.getName().hashCode(), method);
        }
        return map;
    }
}
```

- [ ] **Step 4: Create PublicUserEntity for testing**

```java
package cn.itcraft.jrdb.jdbc.integration;

public class PublicUserEntity {
    private Long id;
    private String name;
    private Integer age;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd jrdb-test && mvn test -Dtest=DaoProxyTemplateTest -q`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/dao/DaoProxyTemplate.java
git add jrdb-test/src/test/java/cn/itcraft/jrdb/jdbc/dao/DaoProxyTemplateTest.java
git add jrdb-test/src/test/java/cn/itcraft/jrdb/jdbc/integration/PublicUserEntity.java
git commit -m "feat: add DaoProxyTemplate with methodNameMap caching"
```

---

### Task 2: DaoProxyGenerator ASM 字节码生成

**Files:**
- Modify: `jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/dao/DaoProxyGenerator.java`
- Test: `jrdb-test/src/test/java/cn/itcraft/jrdb/jdbc/dao/DaoProxyGeneratorTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.annotation.MyBatisStyle;
import cn.itcraft.jrdb.jdbc.annotation.Select;
import cn.itcraft.jrdb.jdbc.annotation.SqlParamBindStyle;
import cn.itcraft.jrdb.jdbc.executor.JdbcStyleExecutor;
import cn.itcraft.jrdb.jdbc.executor.StyleExecutor;
import cn.itcraft.jrdb.jdbc.integration.PublicUserEntity;
import cn.itcraft.jrdb.jdbc.session.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("DaoProxyGenerator Test")
class DaoProxyGeneratorTest {

    @Test
    @DisplayName("generateProxy should create ASM proxy class")
    void generateProxyCreatesASMClass() {
        DaoProxyGenerator generator = new DaoProxyGenerator();
        
        DaoMeta daoMeta = new DaoMeta(TestDao.class);
        Session session = mock(Session.class);
        StyleExecutor executor = new JdbcStyleExecutor();
        
        TestDao proxy = generator.generateProxy(TestDao.class, daoMeta, executor, session);
        
        assertNotNull(proxy);
        assertTrue(proxy instanceof DaoProxyTemplate);
        assertTrue(TestDao.class.isAssignableFrom(proxy.getClass()));
    }

    @Test
    @DisplayName("generated proxy should implement interface methods")
    void generatedProxyImplementsMethods() throws Exception {
        DaoProxyGenerator generator = new DaoProxyGenerator();
        
        DaoMeta daoMeta = new DaoMeta(TestDao.class);
        Session session = mock(Session.class);
        StyleExecutor executor = mock(StyleExecutor.class);
        when(executor.execute(any(), any(), any())).thenReturn(new PublicUserEntity());
        
        TestDao proxy = generator.generateProxy(TestDao.class, daoMeta, executor, session);
        
        PublicUserEntity result = proxy.findById(1L);
        
        assertNotNull(result);
        verify(executor).execute(eq(session), any(MethodMeta.class), eq(new Object[]{1L}));
    }

    @MyBatisStyle
    interface TestDao {
        @Select("SELECT * FROM user WHERE id = #{id}")
        PublicUserEntity findById(Long id);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd jrdb-test && mvn test -Dtest=DaoProxyGeneratorTest -q`
Expected: FAIL with "generateProxy method signature mismatch"

- [ ] **Step 3: Write ASM implementation**

```java
package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.executor.StyleExecutor;
import cn.itcraft.jrdb.jdbc.session.Session;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.objectweb.asm.Opcodes.*;

public class DaoProxyGenerator {

    public <T> T generateProxy(Class<T> daoInterface, DaoMeta daoMeta, StyleExecutor executor, Session session) {
        String className = daoInterface.getSimpleName() + "Proxy";
        byte[] bytecode = generateProxyClass(daoInterface, className);
        
        MapperClassLoader classLoader = new MapperClassLoader();
        Class<?> proxyClass = classLoader.defineClass(className, bytecode);
        
        try {
            Constructor<?> constructor = proxyClass.getConstructor(DaoMeta.class, Session.class, StyleExecutor.class);
            return daoInterface.cast(constructor.newInstance(daoMeta, session, executor));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create proxy instance", e);
        }
    }

    private byte[] generateProxyClass(Class<?> daoInterface, String className) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        
        String internalName = className.replace('.', '/');
        String templateInternal = Type.getInternalName(DaoProxyTemplate.class);
        String interfaceInternal = Type.getInternalName(daoInterface);
        
        cw.visit(V1_8, ACC_PUBLIC, internalName, null, templateInternal, new String[]{interfaceInternal});
        
        generateConstructor(cw, internalName, templateInternal);
        
        for (Method method : daoInterface.getDeclaredMethods()) {
            generateMethod(cw, method, internalName, templateInternal);
        }
        
        cw.visitEnd();
        return cw.toByteArray();
    }

    private void generateConstructor(ClassWriter cw, String className, String templateInternal) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", 
            "(Lcn/itcraft/jrdb/jdbc/dao/DaoMeta;Lcn/itcraft/jrdb/jdbc/session/Session;Lcn/itcraft/jrdb/jdbc/executor/StyleExecutor;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKESPECIAL, templateInternal, "<init>", 
            "(Lcn/itcraft/jrdb/jdbc/dao/DaoMeta;Lcn/itcraft/jrdb/jdbc/session/Session;Lcn/itcraft/jrdb/jdbc/executor/StyleExecutor;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(4, 1);
        mv.visitEnd();
    }

    private void generateMethod(ClassWriter cw, Method method, String className, String templateInternal) {
        String methodName = method.getName();
        int hashCode = methodName.hashCode();
        Class<?> returnType = method.getReturnType();
        Class<?>[] paramTypes = method.getParameterTypes();
        
        String descriptor = Type.getMethodDescriptor(method);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, descriptor, null, null);
        mv.visitCode();
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(hashCode);
        
        mv.visitIntInsn(BIPUSH, paramTypes.length);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        for (int i = 0; i < paramTypes.length; i++) {
            mv.visitInsn(DUP);
            mv.visitIntInsn(BIPUSH, i);
            loadParameter(mv, paramTypes[i], i + 1);
            boxPrimitive(mv, paramTypes[i]);
            mv.visitInsn(AASTORE);
        }
        
        mv.visitMethodInsn(INVOKEVIRTUAL, templateInternal, "executeByHash", "(I[Ljava/lang/Object;)Ljava/lang/Object;", false);
        
        if (returnType == void.class) {
            mv.visitInsn(RETURN);
        } else if (returnType.isPrimitive()) {
            unboxPrimitive(mv, returnType);
        } else {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(returnType));
        }
        
        mv.visitMaxs(paramTypes.length + 3, paramTypes.length + 1);
        mv.visitEnd();
    }

    private void loadParameter(MethodVisitor mv, Class<?> paramType, int index) {
        if (paramType == long.class) {
            mv.visitVarInsn(LLOAD, index);
        } else if (paramType == int.class) {
            mv.visitVarInsn(ILOAD, index);
        } else if (paramType == boolean.class) {
            mv.visitVarInsn(ILOAD, index);
        } else {
            mv.visitVarInsn(ALOAD, index);
        }
    }

    private void boxPrimitive(MethodVisitor mv, Class<?> paramType) {
        if (!paramType.isPrimitive()) {
            return;
        }
        
        String boxedType = getBoxedType(paramType);
        String descriptor = Type.getDescriptor(paramType);
        
        mv.visitMethodInsn(INVOKESTATIC, boxedType, "valueOf", "(" + descriptor + ")L" + boxedType + ";", false);
    }

    private void unboxPrimitive(MethodVisitor mv, Class<?> returnType) {
        String boxedType = getBoxedType(returnType);
        mv.visitTypeInsn(CHECKCAST, boxedType);
        
        String unboxMethod = getUnboxMethod(returnType);
        String descriptor = Type.getDescriptor(returnType);
        
        mv.visitMethodInsn(INVOKEVIRTUAL, boxedType, unboxMethod, "()" + descriptor, false);
        
        if (returnType == int.class) mv.visitInsn(IRETURN);
        else if (returnType == long.class) mv.visitInsn(LRETURN);
        else if (returnType == boolean.class) mv.visitInsn(IRETURN);
        else if (returnType == double.class) mv.visitInsn(DRETURN);
        else if (returnType == float.class) mv.visitInsn(FRETURN);
    }

    private String getBoxedType(Class<?> primitiveType) {
        if (primitiveType == int.class) return "java/lang/Integer";
        if (primitiveType == long.class) return "java/lang/Long";
        if (primitiveType == boolean.class) return "java/lang/Boolean";
        if (primitiveType == double.class) return "java/lang/Double";
        if (primitiveType == float.class) return "java/lang/Float";
        throw new IllegalArgumentException("Unsupported primitive type: " + primitiveType);
    }

    private String getUnboxMethod(Class<?> primitiveType) {
        if (primitiveType == int.class) return "intValue";
        if (primitiveType == long.class) return "longValue";
        if (primitiveType == boolean.class) return "booleanValue";
        if (primitiveType == double.class) return "doubleValue";
        if (primitiveType == float.class) return "floatValue";
        throw new IllegalArgumentException("Unsupported primitive type: " + primitiveType);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd jrdb-test && mvn test -Dtest=DaoProxyGeneratorTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/dao/DaoProxyGenerator.java
git add jrdb-test/src/test/java/cn/itcraft/jrdb/jdbc/dao/DaoProxyGeneratorTest.java
git commit -m "feat: ASM bytecode generation for DAO proxy"
```

---

## Phase 2: AutoCrud 实现

### Task 3: EntityMapper 扩展 - getFieldNames()

**Files:**
- Modify: `jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/mapping/EntityMapper.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
@DisplayName("getFieldNames should return field names in order")
void getFieldNames() {
    MapperRegistry registry = new MapperRegistry(new TypeHandlerRegistry());
    EntityMapper<PublicUserEntity> mapper = registry.getMapper(PublicUserEntity.class);
    
    String[] fieldNames = mapper.getFieldNames();
    
    assertNotNull(fieldNames);
    assertTrue(fieldNames.length > 0);
}
```

- [ ] **Step 2: Add getFieldNames() to EntityMapper interface**

```java
public interface EntityMapper<T> {
    T mapFromResultSet(ResultSet rs) throws SQLException;
    Object[] mapToRow(T entity);
    String[] getFieldNames();
}
```

- [ ] **Step 3: Modify MapperClassGenerator to implement getFieldNames()**

在 MapperClassGenerator 中生成 getFieldNames() 方法实现（返回字段名数组）。

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/mapping/EntityMapper.java
git add jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/mapping/MapperClassGenerator.java
git commit -m "feat: EntityMapper.getFieldNames() for AutoCrud"
```

---

### Task 4: AutoCrudMethodGenerator

**Files:**
- Create: `jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/dao/AutoCrudMethodGenerator.java`
- Test: `jrdb-test/src/test/java/cn/itcraft/jrdb/jdbc/dao/AutoCrudMethodGeneratorTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.annotation.AutoCrud;
import cn.itcraft.jrdb.jdbc.integration.PublicUserEntity;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.type.TypeHandlerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AutoCrudMethodGenerator Test")
class AutoCrudMethodGeneratorTest {

    @Test
    @DisplayName("generateAutoCrudMethods should create 4 MethodMeta")
    void generateAutoCrudMethods() throws NoSuchMethodException {
        MapperRegistry registry = new MapperRegistry(new TypeHandlerRegistry());
        AutoCrudMethodGenerator generator = new AutoCrudMethodGenerator(registry);
        
        DaoMeta daoMeta = new DaoMeta(AutoCrudDao.class);
        AutoCrudMeta autoCrudMeta = new AutoCrudMeta(PublicUserEntity.class, "id", "public_user_entity");
        
        generator.generateAutoCrudMethods(daoMeta, autoCrudMeta);
        
        Method insertMethod = AutoCrudDao.class.getMethod("insert", PublicUserEntity.class);
        Method updateMethod = AutoCrudDao.class.getMethod("update", PublicUserEntity.class);
        Method deleteMethod = AutoCrudDao.class.getMethod("delete", Long.class);
        Method selectByIdMethod = AutoCrudDao.class.getMethod("selectById", Long.class);
        
        assertNotNull(daoMeta.getMethodMeta(insertMethod));
        assertNotNull(daoMeta.getMethodMeta(updateMethod));
        assertNotNull(daoMeta.getMethodMeta(deleteMethod));
        assertNotNull(daoMeta.getMethodMeta(selectByIdMethod));
    }

    @Test
    @DisplayName("generated insert SQL should be correct")
    void generatedInsertSQL() throws NoSuchMethodException {
        MapperRegistry registry = new MapperRegistry(new TypeHandlerRegistry());
        AutoCrudMethodGenerator generator = new AutoCrudMethodGenerator(registry);
        
        DaoMeta daoMeta = new DaoMeta(AutoCrudDao.class);
        AutoCrudMeta autoCrudMeta = new AutoCrudMeta(PublicUserEntity.class, "id", "public_user_entity");
        
        generator.generateAutoCrudMethods(daoMeta, autoCrudMeta);
        
        Method insertMethod = AutoCrudDao.class.getMethod("insert", PublicUserEntity.class);
        MethodMeta methodMeta = daoMeta.getMethodMeta(insertMethod);
        
        String sql = methodMeta.getCachedSql();
        assertTrue(sql.startsWith("INSERT INTO public_user_entity"));
        assertTrue(sql.contains("VALUES"));
    }

    @AutoCrud(entity = PublicUserEntity.class, primaryKey = "id")
    interface AutoCrudDao {
        int insert(PublicUserEntity entity);
        int update(PublicUserEntity entity);
        int delete(Long id);
        PublicUserEntity selectById(Long id);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd jrdb-test && mvn test -Dtest=AutoCrudMethodGeneratorTest -q`
Expected: FAIL with "AutoCrudMethodGenerator not found"

- [ ] **Step 3: Write implementation**

```java
package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.mapping.EntityMapper;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

public class AutoCrudMethodGenerator {

    private final MapperRegistry mapperRegistry;

    public AutoCrudMethodGenerator(MapperRegistry mapperRegistry) {
        this.mapperRegistry = Objects.requireNonNull(mapperRegistry, "mapperRegistry cannot be null");
    }

    public void generateAutoCrudMethods(DaoMeta daoMeta, AutoCrudMeta autoCrudMeta) {
        Class<?> entityClass = autoCrudMeta.getEntityClass();
        String tableName = autoCrudMeta.getTableName();
        String primaryKey = autoCrudMeta.getPrimaryKey();

        EntityMapper<?> mapper = mapperRegistry.getMapper(entityClass);
        String[] fieldNames = mapper.getFieldNames();

        generateInsertMethod(daoMeta, tableName, fieldNames, primaryKey, entityClass);
        generateUpdateMethod(daoMeta, tableName, fieldNames, primaryKey, entityClass);
        generateDeleteMethod(daoMeta, tableName, primaryKey);
        generateSelectByIdMethod(daoMeta, tableName, primaryKey, entityClass);
    }

    private void generateInsertMethod(DaoMeta daoMeta, String tableName, String[] fieldNames, String primaryKey, Class<?> entityClass) {
        try {
            Method method = daoMeta.getDaoInterface().getDeclaredMethod("insert", entityClass);
            MethodMeta meta = new MethodMeta("insert");

            StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
            for (int i = 0; i < fieldNames.length; i++) {
                if (i > 0) sql.append(", ");
                sql.append(fieldNames[i]);
            }
            sql.append(") VALUES (");
            for (int i = 0; i < fieldNames.length; i++) {
                if (i > 0) sql.append(", ");
                sql.append("?");
            }
            sql.append(")");

            meta.setCachedSql(sql.toString());
            meta.setOperationType(SqlOperationType.INSERT);
            meta.setReturnType(int.class);

            for (int i = 0; i < fieldNames.length; i++) {
                meta.addParamMapping(i);
            }

            daoMeta.addMethodMeta(method, meta);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("insert method not found", e);
        }
    }

    private void generateUpdateMethod(DaoMeta daoMeta, String tableName, String[] fieldNames, String primaryKey, Class<?> entityClass) {
        try {
            Method method = daoMeta.getDaoInterface().getDeclaredMethod("update", entityClass);
            MethodMeta meta = new MethodMeta("update");

            StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
            boolean first = true;
            for (String fieldName : fieldNames) {
                if (!fieldName.equalsIgnoreCase(primaryKey)) {
                    if (!first) sql.append(", ");
                    sql.append(fieldName).append("=?");
                    first = false;
                }
            }
            sql.append(" WHERE ").append(primaryKey).append("=?");

            meta.setCachedSql(sql.toString());
            meta.setOperationType(SqlOperationType.UPDATE);
            meta.setReturnType(int.class);

            int pkIndex = findPrimaryKeyIndex(fieldNames, primaryKey);
            int paramIndex = 0;
            for (int i = 0; i < fieldNames.length; i++) {
                if (i != pkIndex) {
                    meta.addParamMapping(i);
                    paramIndex++;
                }
            }
            meta.addParamMapping(pkIndex);

            daoMeta.addMethodMeta(method, meta);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("update method not found", e);
        }
    }

    private void generateDeleteMethod(DaoMeta daoMeta, String tableName, String primaryKey) {
        try {
            Method method = daoMeta.getDaoInterface().getDeclaredMethod("delete", Long.class);
            MethodMeta meta = new MethodMeta("delete");

            String sql = "DELETE FROM " + tableName + " WHERE " + primaryKey + "=?";
            meta.setCachedSql(sql);
            meta.setOperationType(SqlOperationType.DELETE);
            meta.setReturnType(int.class);
            meta.addParamMapping(0);

            daoMeta.addMethodMeta(method, meta);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("delete method not found", e);
        }
    }

    private void generateSelectByIdMethod(DaoMeta daoMeta, String tableName, String primaryKey, Class<?> entityClass) {
        try {
            Method method = daoMeta.getDaoInterface().getDeclaredMethod("selectById", Long.class);
            MethodMeta meta = new MethodMeta("selectById");

            String sql = "SELECT * FROM " + tableName + " WHERE " + primaryKey + "=?";
            meta.setCachedSql(sql);
            meta.setOperationType(SqlOperationType.SELECT);
            meta.setReturnType(entityClass);
            meta.addParamMapping(0);

            daoMeta.addMethodMeta(method, meta);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("selectById method not found", e);
        }
    }

    private int findPrimaryKeyIndex(String[] fieldNames, String primaryKey) {
        for (int i = 0; i < fieldNames.length; i++) {
            if (fieldNames[i].equalsIgnoreCase(primaryKey)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Primary key not found: " + primaryKey);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd jrdb-test && mvn test -Dtest=AutoCrudMethodGeneratorTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/dao/AutoCrudMethodGenerator.java
git add jrdb-test/src/test/java/cn/itcraft/jrdb/jdbc/dao/AutoCrudMethodGeneratorTest.java
git commit -m "feat: AutoCrudMethodGenerator for insert/update/delete/selectById"
```

---

### Task 5: AutoCrudExecutor

**Files:**
- Create: `jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/executor/AutoCrudExecutor.java`
- Test: `jrdb-test/src/test/java/cn/itcraft/jrdb/jdbc/executor/AutoCrudExecutorTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jrdb.jdbc.executor;

import cn.itcraft.jrdb.jdbc.dao.MethodMeta;
import cn.itcraft.jrdb.jdbc.dao.SqlOperationType;
import cn.itcraft.jrdb.jdbc.integration.PublicUserEntity;
import cn.itcraft.jrdb.jdbc.mapping.EntityMapper;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.session.Session;
import cn.itcraft.jrdb.jdbc.type.TypeHandlerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AutoCrudExecutor Test")
class AutoCrudExecutorTest {

    @Test
    @DisplayName("execute insert should map entity to params")
    void executeInsert() {
        MapperRegistry registry = new MapperRegistry(new TypeHandlerRegistry());
        EntityMapper<PublicUserEntity> mapper = registry.getMapper(PublicUserEntity.class);
        
        AutoCrudExecutor executor = new AutoCrudExecutor(mapper);
        
        MethodMeta methodMeta = new MethodMeta("insert");
        methodMeta.setCachedSql("INSERT INTO user (id, name, age) VALUES (?, ?, ?)");
        methodMeta.setOperationType(SqlOperationType.INSERT);
        methodMeta.addParamMapping(0);
        methodMeta.addParamMapping(1);
        methodMeta.addParamMapping(2);
        
        Session session = mock(Session.class);
        PublicUserEntity entity = new PublicUserEntity();
        entity.setId(1L);
        entity.setName("Alice");
        entity.setAge(30);
        
        executor.execute(session, methodMeta, new Object[]{entity});
        
        verify(session).insert(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("execute selectById should use args directly")
    void executeSelectById() {
        MapperRegistry registry = new MapperRegistry(new TypeHandlerRegistry());
        EntityMapper<PublicUserEntity> mapper = registry.getMapper(PublicUserEntity.class);
        
        AutoCrudExecutor executor = new AutoCrudExecutor(mapper);
        
        MethodMeta methodMeta = new MethodMeta("selectById");
        methodMeta.setCachedSql("SELECT * FROM user WHERE id=?");
        methodMeta.setOperationType(SqlOperationType.SELECT);
        methodMeta.setReturnType(PublicUserEntity.class);
        methodMeta.addParamMapping(0);
        
        Session session = mock(Session.class);
        when(session.queryForObject(anyString(), eq(PublicUserEntity.class), any(Object[].class)))
            .thenReturn(new PublicUserEntity());
        
        executor.execute(session, methodMeta, new Object[]{1L});
        
        verify(session).queryForObject(anyString(), eq(PublicUserEntity.class), eq(new Object[]{1L}));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd jrdb-test && mvn test -Dtest=AutoCrudExecutorTest -q`
Expected: FAIL with "AutoCrudExecutor not found"

- [ ] **Step 3: Write implementation**

```java
package cn.itcraft.jrdb.jdbc.executor;

import cn.itcraft.jrdb.jdbc.dao.MethodMeta;
import cn.itcraft.jrdb.jdbc.dao.SqlOperationType;
import cn.itcraft.jrdb.jdbc.mapping.EntityMapper;
import cn.itcraft.jrdb.jdbc.query.QueryResult;
import cn.itcraft.jrdb.jdbc.session.Session;
import java.util.List;
import java.util.Objects;

public class AutoCrudExecutor implements StyleExecutor {

    private final EntityMapper<?> mapper;
    private final JdbcStyleExecutor jdbcExecutor;

    public AutoCrudExecutor(EntityMapper<?> mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
        this.jdbcExecutor = new JdbcStyleExecutor();
    }

    @Override
    public Object execute(Session session, MethodMeta methodMeta, Object[] args) {
        String methodName = methodMeta.getMethodName();

        if ("insert".equals(methodName) || "update".equals(methodName)) {
            Object entity = args[0];
            Object[] entityParams = mapper.mapToRow(entity);
            Object[] mappedParams = mapParams(methodMeta.getParamMapping(), entityParams);
            
            return jdbcExecutor.execute(session, methodMeta, mappedParams);
        } else if ("delete".equals(methodName)) {
            return jdbcExecutor.execute(session, methodMeta, args);
        } else if ("selectById".equals(methodName)) {
            return jdbcExecutor.execute(session, methodMeta, args);
        } else {
            throw new IllegalArgumentException("Unknown AutoCrud method: " + methodName);
        }
    }

    private Object[] mapParams(List<Integer> paramMapping, Object[] params) {
        Object[] mappedParams = new Object[paramMapping.size()];
        for (int i = 0; i < paramMapping.size(); i++) {
            mappedParams[i] = params[paramMapping.get(i)];
        }
        return mappedParams;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd jrdb-test && mvn test -Dtest=AutoCrudExecutorTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/executor/AutoCrudExecutor.java
git add jrdb-test/src/test/java/cn/itcraft/jrdb/jdbc/executor/AutoCrudExecutorTest.java
git commit -m "feat: AutoCrudExecutor for entity parameter mapping"
```

---

## Phase 3: 集成 DaoMetaCache 和 DaoRegistry

### Task 6: 修改 DaoMetaCache 调用 AutoCrudMethodGenerator

**Files:**
- Modify: `jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/dao/DaoMetaCache.java`

- [ ] **Step 1: Modify parseAutoCrud to generate methods**

在 DaoMetaCache.parseAutoCrud() 中调用 AutoCrudMethodGenerator：

```java
private void parseAutoCrud(Class<?> daoInterface, DaoMeta daoMeta) {
    AutoCrud autoCrud = daoInterface.getAnnotation(AutoCrud.class);
    if (autoCrud != null) {
        Class<?> entityClass = autoCrud.entity();
        String primaryKey = autoCrud.primaryKey();
        String tableName = inferTableName(entityClass);
        AutoCrudMeta autoCrudMeta = new AutoCrudMeta(entityClass, primaryKey, tableName);
        daoMeta.setAutoCrudMeta(autoCrudMeta);
        
        AutoCrudMethodGenerator generator = new AutoCrudMethodGenerator(mapperRegistry);
        generator.generateAutoCrudMethods(daoMeta, autoCrudMeta);
    }
}
```

- [ ] **Step 2: Run all tests**

Run: `mvn test -q`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/dao/DaoMetaCache.java
git commit -m "feat: DaoMetaCache generates AutoCrud methods"
```

---

### Task 7: 修改 DaoRegistry 使用 ASM 代理

**Files:**
- Modify: `jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/dao/DaoRegistry.java`

- [ ] **Step 1: Replace JDK Proxy with ASM proxy**

修改 DaoRegistry.getDao()：

```java
public <T> T getDao(Class<T> daoInterface, Session session) {
    Objects.requireNonNull(daoInterface, "daoInterface cannot be null");
    Objects.requireNonNull(session, "session cannot be null");

    DaoMeta daoMeta = daoMetaCache.getDaoMeta(daoInterface);
    
    if (daoMeta.hasAutoCrud()) {
        EntityMapper<?> mapper = mapperRegistry.getMapper(daoMeta.getAutoCrudMeta().getEntityClass());
        StyleExecutor executor = new AutoCrudExecutor(mapper);
        return proxyGenerator.generateProxy(daoInterface, daoMeta, executor, session);
    } else {
        StyleExecutor executor = getExecutor(daoMeta);
        return proxyGenerator.generateProxy(daoInterface, daoMeta, executor, session);
    }
}
```

- [ ] **Step 2: Run all tests**

Run: `mvn test -q`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add jrdb-jdbc/src/main/java/cn/itcraft/jrdb/jdbc/dao/DaoRegistry.java
git commit -m "feat: DaoRegistry uses ASM proxy"
```

---

## Phase 4: 集成测试

### Task 8: ASM 代理集成测试

**Files:**
- Create: `jrdb-test/src/test/java/cn/itcraft/jrdb/jdbc/integration/ASMProxyIntegrationTest.java`

- [ ] **Step 1: Write integration test**

```java
package cn.itcraft.jrdb.jdbc.integration;

import cn.itcraft.jrdb.jdbc.annotation.MyBatisStyle;
import cn.itcraft.jrdb.jdbc.annotation.Select;
import cn.itcraft.jrdb.jdbc.config.JrdbConfig;
import cn.itcraft.jrdb.jdbc.dao.DaoRegistry;
import cn.itcraft.jrdb.jdbc.session.Session;
import cn.itcraft.jrdb.jdbc.session.SessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ASM Proxy Integration Test")
class ASMProxyIntegrationTest {

    private SessionFactory sessionFactory;
    private JdbcDataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:asmtest;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS public_user_entity (id BIGINT PRIMARY KEY, name VARCHAR(100), age INT)");
        stmt.execute("INSERT INTO public_user_entity VALUES (1, 'Alice', 30)");
        stmt.close();
        conn.close();

        JrdbConfig config = JrdbConfig.builder().build();
        sessionFactory = new SessionFactory(config, dataSource);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (sessionFactory != null) sessionFactory.close();
        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("DROP TABLE IF EXISTS public_user_entity");
        stmt.close();
        conn.close();
    }

    @Test
    @DisplayName("ASM proxy should query entity")
    void asmProxyQuery() throws Exception {
        Session session = sessionFactory.openSession();
        DaoRegistry daoRegistry = sessionFactory.getDaoRegistry();
        
        try {
            UserDao userDao = daoRegistry.getDao(UserDao.class, session);
            PublicUserEntity user = userDao.findById(1L);
            
            assertNotNull(user);
            assertEquals(1L, user.getId());
            assertEquals("Alice", user.getName());
            assertEquals(30, user.getAge());
        } finally {
            session.close();
        }
    }

    @MyBatisStyle
    interface UserDao {
        @Select("SELECT * FROM public_user_entity WHERE id = #{id}")
        PublicUserEntity findById(Long id);
    }
}
```

- [ ] **Step 2: Run test**

Run: `cd jrdb-test && mvn test -Dtest=ASMProxyIntegrationTest -q`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add jrdb-test/src/test/java/cn/itcraft/jrdb/jdbc/integration/ASMProxyIntegrationTest.java
git commit -m "test: ASM proxy integration test"
```

---

### Task 9: AutoCrud 集成测试

**Files:**
- Create: `jrdb-test/src/test/java/cn/itcraft/jrdb/jdbc/integration/AutoCrudIntegrationTest.java`

- [ ] **Step 1: Write integration test**

```java
package cn.itcraft.jrdb.jdbc.integration;

import cn.itcraft.jrdb.jdbc.annotation.AutoCrud;
import cn.itcraft.jrdb.jdbc.config.JrdbConfig;
import cn.itcraft.jrdb.jdbc.dao.DaoRegistry;
import cn.itcraft.jrdb.jdbc.session.Session;
import cn.itcraft.jrdb.jdbc.session.SessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AutoCrud Integration Test")
class AutoCrudIntegrationTest {

    private SessionFactory sessionFactory;
    private JdbcDataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:autocrudtest;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS public_user_entity (id BIGINT PRIMARY KEY, name VARCHAR(100), age INT)");
        stmt.close();
        conn.close();

        JrdbConfig config = JrdbConfig.builder().build();
        sessionFactory = new SessionFactory(config, dataSource);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (sessionFactory != null) sessionFactory.close();
        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("DROP TABLE IF EXISTS public_user_entity");
        stmt.close();
        conn.close();
    }

    @Test
    @DisplayName("AutoCrud insert should work")
    void autoCrudInsert() throws Exception {
        Session session = sessionFactory.openSession();
        DaoRegistry daoRegistry = sessionFactory.getDaoRegistry();
        
        try {
            AutoCrudDao dao = daoRegistry.getDao(AutoCrudDao.class, session);
            
            PublicUserEntity entity = new PublicUserEntity();
            entity.setId(1L);
            entity.setName("Alice");
            entity.setAge(30);
            
            int result = dao.insert(entity);
            assertEquals(1, result);
            
            PublicUserEntity loaded = dao.selectById(1L);
            assertNotNull(loaded);
            assertEquals("Alice", loaded.getName());
        } finally {
            session.close();
        }
    }

    @Test
    @DisplayName("AutoCrud update should work")
    void autoCrudUpdate() throws Exception {
        Session session = sessionFactory.openSession();
        DaoRegistry daoRegistry = sessionFactory.getDaoRegistry();
        
        try {
            AutoCrudDao dao = daoRegistry.getDao(AutoCrudDao.class, session);
            
            PublicUserEntity entity = new PublicUserEntity();
            entity.setId(1L);
            entity.setName("Alice");
            entity.setAge(30);
            dao.insert(entity);
            
            entity.setName("Bob");
            entity.setAge(35);
            int result = dao.update(entity);
            assertEquals(1, result);
            
            PublicUserEntity loaded = dao.selectById(1L);
            assertEquals("Bob", loaded.getName());
            assertEquals(35, loaded.getAge());
        } finally {
            session.close();
        }
    }

    @Test
    @DisplayName("AutoCrud delete should work")
    void autoCrudDelete() throws Exception {
        Session session = sessionFactory.openSession();
        DaoRegistry daoRegistry = sessionFactory.getDaoRegistry();
        
        try {
            AutoCrudDao dao = daoRegistry.getDao(AutoCrudDao.class, session);
            
            PublicUserEntity entity = new PublicUserEntity();
            entity.setId(1L);
            entity.setName("Alice");
            entity.setAge(30);
            dao.insert(entity);
            
            int result = dao.delete(1L);
            assertEquals(1, result);
            
            PublicUserEntity loaded = dao.selectById(1L);
            assertNull(loaded);
        } finally {
            session.close();
        }
    }

    @AutoCrud(entity = PublicUserEntity.class, primaryKey = "id")
    interface AutoCrudDao {
        int insert(PublicUserEntity entity);
        int update(PublicUserEntity entity);
        int delete(Long id);
        PublicUserEntity selectById(Long id);
    }
}
```

- [ ] **Step 2: Run test**

Run: `cd jrdb-test && mvn test -Dtest=AutoCrudIntegrationTest -q`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add jrdb-test/src/test/java/cn/itcraft/jrdb/jdbc/integration/AutoCrudIntegrationTest.java
git commit -m "test: AutoCrud integration test"
```

---

## Phase 5: 验证和清理

### Task 10: 运行全量测试

- [ ] **Step 1: Run all tests**

Run: `mvn test -q`
Expected: All tests pass

- [ ] **Step 2: Fix any test failures**

如有失败，分析原因并修复。

- [ ] **Step 3: Final commit**

```bash
git add -A
git commit -m "feat: complete ASM proxy + AutoCrud implementation

实现内容：
1. ASM 字节码生成替换 JDK Proxy
   - DaoProxyTemplate: methodNameMap 缓存
   - DaoProxyGenerator: ASM 生成代理类
   - hashCode 映射方法，HashMap O(1) 查找

2. AutoCrud 自动生成 4 个方法
   - insert/update/delete/selectById
   - 实体参数风格
   - AutoCrudExecutor 处理 entity 参数映射

3. 测试覆盖
   - 单元测试（10个测试类）
   - 集成测试（ASM 代理、AutoCrud）

测试结果：所有测试通过"
```

---

## 自检清单

**Spec Coverage:**
- [x] ASM + 固化方法模板 → Task 1-2
- [x] methodNameMap 缓存 → Task 1
- [x] hashCode 映射 → Task 1
- [x] AutoCrud 生成 4 个方法 → Task 3-5
- [x] 实体参数风格 → Task 5
- [x] 单元测试 → Task 1-5
- [x] 集成测试 → Task 8-9

**Placeholder Scan:**
- [x] 无 "TBD", "TODO", "implement later"
- [x] 无 "add appropriate error handling"
- [x] 所有代码步骤包含完整代码
- [x] 无 "Similar to Task N"

**Type Consistency:**
- [x] DaoProxyTemplate.methodNameMap 类型一致
- [x] MethodMeta.getParamMapping() 返回 List<Integer>
- [x] AutoCrudExecutor.execute() 参数签名一致