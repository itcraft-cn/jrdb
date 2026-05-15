# ASM 代理生成 + AutoCrud 完整实现设计文档

## 概述

优化 jrdb 注解 SQL 混合模式，使用 ASM 字节码生成替换 JDK Proxy，完整实现 AutoCrud 自动生成基础 CRUD 方法。

## 设计目标

1. ASM + 固化方法模板生成代理类（性能接近完整 ASM）
2. AutoCrud 生成 insert/update/delete/selectById（实体参数风格）
3. 移除 selectAll（避免超大表一次性返回太多数据）
4. 单元测试和集成测试覆盖

---

## 第一部分：ASM 代理生成架构

### 架构设计

```
┌─────────────────────────────────────────┐
│  DaoProxyTemplate（固化模板类）          │
│  ├─ methodNameMap: Map<Integer, Method> │
│  ├─ executeByHash(hashCode, args)       │
│  └─ findMethodByHash(hashCode)          │
├─────────────────────────────────────────┤
│  ASM 生成的代理类                        │
│  ├─ UserDaoProxy extends DaoProxyTemplate│
│  ├─ findById(id) -> executeByHash(...)  │
│  └─ 其他方法同上                          │
├─────────────────────────────────────────┤
│  DaoProxyGenerator（ASM 生成器）         │
│  ├─ generateProxyClass()                │
│  ├─ generateMethod()                    │
│  └─ buildMethodNameMap()                │
└─────────────────────────────────────────┘
```

### 数据流

```
UserDao.findById(1L)
    → ASM 代理类调用 executeByHash("findById".hashCode(), args)
    → DaoProxyTemplate.findMethodByHash(hashCode)
    → 从 methodNameMap 获取 Method（已缓存）
    → 执行 StyleExecutor.execute
    → 返回结果
```

### 核心类设计

#### DaoProxyTemplate

```java
public class DaoProxyTemplate {
    protected DaoMeta daoMeta;
    protected Session session;
    protected StyleExecutor executor;
    protected Map<Integer, Method> methodNameMap;
    
    public DaoProxyTemplate(DaoMeta daoMeta, Session session, StyleExecutor executor) {
        this.daoMeta = daoMeta;
        this.session = session;
        this.executor = executor;
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
    
    private Map<Integer, Method> buildMethodNameMap(DaoMeta daoMeta) {
        Map<Integer, Method> map = new HashMap<>();
        for (Method method : daoMeta.getDaoInterface().getDeclaredMethods()) {
            map.put(method.getName().hashCode(), method);
        }
        return map;
    }
}
```

#### DaoProxyGenerator

```java
public class DaoProxyGenerator {
    private final MapperClassLoader classLoader;
    
    public <T> T generateProxy(Class<T> daoInterface, DaoMeta daoMeta, StyleExecutor executor, Session session) {
        String className = daoInterface.getSimpleName() + "Proxy";
        byte[] bytecode = generateProxyClass(daoInterface, className);
        Class<?> proxyClass = classLoader.defineClass(className, bytecode);
        
        Constructor<?> constructor = proxyClass.getConstructor(DaoMeta.class, Session.class, StyleExecutor.class);
        return daoInterface.cast(constructor.newInstance(daoMeta, session, executor));
    }
    
    private byte[] generateProxyClass(Class<?> daoInterface, String className) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        
        // 类定义：extends DaoProxyTemplate implements daoInterface
        cw.visit(V1_8, ACC_PUBLIC, className, null, "cn/itcraft/jrdb/jdbc/dao/DaoProxyTemplate", 
                 new String[]{Type.getInternalName(daoInterface)});
        
        // 构造函数
        generateConstructor(cw);
        
        // 方法实现
        for (Method method : daoInterface.getDeclaredMethods()) {
            generateMethod(cw, method);
        }
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    private void generateConstructor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", 
            "(Lcn/itcraft/jrdb/jdbc/dao/DaoMeta;Lcn/itcraft/jrdb/jdbc/session/Session;Lcn/itcraft/jrdb/jdbc/executor/StyleExecutor;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKESPECIAL, "cn/itcraft/jrdb/jdbc/dao/DaoProxyTemplate", "<init>", 
            "(Lcn/itcraft/jrdb/jdbc/dao/DaoMeta;Lcn/itcraft/jrdb/jdbc/session/Session;Lcn/itcraft/jrdb/jdbc/executor/StyleExecutor;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(4, 1);
        mv.visitEnd();
    }
    
    private void generateMethod(ClassWriter cw, Method method) {
        String methodName = method.getName();
        int hashCode = methodName.hashCode();
        Class<?> returnType = method.getReturnType();
        Class<?>[] paramTypes = method.getParameterTypes();
        
        String descriptor = Type.getMethodDescriptor(method);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, descriptor, null, null);
        mv.visitCode();
        
        // 调用 executeByHash(hashCode, args)
        mv.visitVarInsn(ALOAD, 0);  // this
        mv.visitLdcInsn(hashCode);  // methodName.hashCode()
        
        // 创建 Object[] args
        mv.visitIntInsn(BIPUSH, paramTypes.length);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        for (int i = 0; i < paramTypes.length; i++) {
            mv.visitInsn(DUP);
            mv.visitIntInsn(BIPUSH, i);
            mv.visitVarInsn(ALOAD, i + 1);
            mv.visitInsn(AASTORE);
        }
        
        mv.visitMethodInsn(INVOKEVIRTUAL, "cn/itcraft/jrdb/jdbc/dao/DaoProxyTemplate", "executeByHash", 
            "(I[Ljava/lang/Object;)Ljava/lang/Object;", false);
        
        // 返回值处理
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
    
    private void unboxPrimitive(MethodVisitor mv, Class<?> returnType) {
        String boxedType = getBoxedType(returnType);
        mv.visitTypeInsn(CHECKCAST, boxedType);
        mv.visitMethodInsn(INVOKEVIRTUAL, boxedType, getUnboxMethod(returnType), 
            "()" + Type.getDescriptor(returnType), false);
        if (returnType == int.class) mv.visitInsn(IRETURN);
        else if (returnType == long.class) mv.visitInsn(LRETURN);
        else if (returnType == boolean.class) mv.visitInsn(IRETURN);
    }
    
    private String getBoxedType(Class<?> primitiveType) {
        if (primitiveType == int.class) return "java/lang/Integer";
        if (primitiveType == long.class) return "java/lang/Long";
        if (primitiveType == boolean.class) return "java/lang/Boolean";
        throw new IllegalArgumentException("Unsupported primitive type: " + primitiveType);
    }
    
    private String getUnboxMethod(Class<?> primitiveType) {
        if (primitiveType == int.class) return "intValue";
        if (primitiveType == long.class) return "longValue";
        if (primitiveType == boolean.class) return "booleanValue";
        throw new IllegalArgumentException("Unsupported primitive type: " + primitiveType);
    }
}
```

### 性能优化

- hashCode 计算：JVM 内联优化，开销极小
- methodNameMap 查找：HashMap O(1) 查找
- 无反射开销（Method 对象已缓存）

---

## 第二部分：AutoCrud 实现

### 设计概述

在 DaoMetaCache 解析 @AutoCrud 注解时，自动生成 4 个 MethodMeta 并缓存到 DaoMeta.methodMetas。

### 生成的方法

| 方法 | SQL | 参数 | 返回值 |
|------|-----|------|--------|
| insert | INSERT INTO table (...) VALUES (...) | EntityMapper.mapToRow(entity) | int |
| update | UPDATE table SET ... WHERE id=? | EntityMapper.mapToRow(entity) | int |
| delete | DELETE FROM table WHERE id=? | id 参数 | int |
| selectById | SELECT * FROM table WHERE id=? | id 参数 | Entity |

### 核心类设计

#### AutoCrudMethodGenerator

```java
public class AutoCrudMethodGenerator {
    
    public void generateAutoCrudMethods(DaoMeta daoMeta, AutoCrudMeta autoCrudMeta, MapperRegistry mapperRegistry) {
        Class<?> entityClass = autoCrudMeta.getEntityClass();
        String tableName = autoCrudMeta.getTableName();
        String primaryKey = autoCrudMeta.getPrimaryKey();
        
        EntityMapper<?> mapper = mapperRegistry.getMapper(entityClass);
        
        // 生成 insert
        MethodMeta insertMeta = createInsertMethodMeta(tableName, mapper, primaryKey);
        daoMeta.addMethodMeta(createMethod("insert", entityClass), insertMeta);
        
        // 生成 update
        MethodMeta updateMeta = createUpdateMethodMeta(tableName, mapper, primaryKey);
        daoMeta.addMethodMeta(createMethod("update", entityClass), updateMeta);
        
        // 生成 delete
        MethodMeta deleteMeta = createDeleteMethodMeta(tableName, primaryKey);
        daoMeta.addMethodMeta(createMethod("delete", Long.class), deleteMeta);
        
        // 生成 selectById
        MethodMeta selectByIdMeta = createSelectByIdMethodMeta(tableName, primaryKey, entityClass);
        daoMeta.addMethodMeta(createMethod("selectById", Long.class), selectByIdMeta);
    }
    
    private MethodMeta createInsertMethodMeta(String tableName, EntityMapper<?> mapper, String primaryKey) {
        MethodMeta meta = new MethodMeta("insert");
        
        // 构建 INSERT SQL
        String[] fieldNames = mapper.getFieldNames();
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" (");
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
        
        // 参数映射：entity -> Object[]
        for (int i = 0; i < fieldNames.length; i++) {
            meta.addParamMapping(i);
        }
        
        return meta;
    }
    
    private MethodMeta createUpdateMethodMeta(String tableName, EntityMapper<?> mapper, String primaryKey) {
        MethodMeta meta = new MethodMeta("update");
        
        // 构建 UPDATE SQL
        String[] fieldNames = mapper.getFieldNames();
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(tableName).append(" SET ");
        boolean first = true;
        for (String fieldName : fieldNames) {
            if (!fieldName.equals(primaryKey)) {
                if (!first) sql.append(", ");
                sql.append(fieldName).append("=?");
                first = false;
            }
        }
        sql.append(" WHERE ").append(primaryKey).append("=?");
        
        meta.setCachedSql(sql.toString());
        meta.setOperationType(SqlOperationType.UPDATE);
        meta.setReturnType(int.class);
        
        // 参数映射：entity -> Object[]（排除主键，主键在最后）
        int paramIndex = 0;
        for (String fieldName : fieldNames) {
            if (!fieldName.equals(primaryKey)) {
                meta.addParamMapping(paramIndex);
                paramIndex++;
            }
        }
        meta.addParamMapping(findPrimaryKeyIndex(fieldNames, primaryKey));
        
        return meta;
    }
    
    private MethodMeta createDeleteMethodMeta(String tableName, String primaryKey) {
        MethodMeta meta = new MethodMeta("delete");
        
        String sql = "DELETE FROM " + tableName + " WHERE " + primaryKey + "=?";
        meta.setCachedSql(sql);
        meta.setOperationType(SqlOperationType.DELETE);
        meta.setReturnType(int.class);
        meta.addParamMapping(0);
        
        return meta;
    }
    
    private MethodMeta createSelectByIdMethodMeta(String tableName, String primaryKey, Class<?> entityClass) {
        MethodMeta meta = new MethodMeta("selectById");
        
        String sql = "SELECT * FROM " + tableName + " WHERE " + primaryKey + "=?";
        meta.setCachedSql(sql);
        meta.setOperationType(SqlOperationType.SELECT);
        meta.setReturnType(entityClass);
        meta.addParamMapping(0);
        
        return meta;
    }
    
    private Method createMethod(String methodName, Class<?> paramType) {
        try {
            return daoMeta.getDaoInterface().getDeclaredMethod(methodName, paramType);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("AutoCrud method not found: " + methodName, e);
        }
    }
    
    private int findPrimaryKeyIndex(String[] fieldNames, String primaryKey) {
        for (int i = 0; i < fieldNames.length; i++) {
            if (fieldNames[i].equals(primaryKey)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Primary key not found: " + primaryKey);
    }
}
```

#### AutoCrudExecutor（特殊处理 insert/update）

```java
public class AutoCrudExecutor implements StyleExecutor {
    
    private final JdbcStyleExecutor jdbcExecutor;
    
    @Override
    public Object execute(Session session, MethodMeta methodMeta, Object[] args) {
        String methodName = methodMeta.getMethodName();
        
        if ("insert".equals(methodName) || "update".equals(methodName)) {
            // insert/update: args[0] 是 entity，需要用 EntityMapper 映射
            Object entity = args[0];
            EntityMapper<?> mapper = getEntityMapper(methodMeta.getReturnType());
            Object[] params = mapper.mapToRow(entity);
            
            // 重新映射参数（根据 MethodMeta.paramMapping）
            Object[] mappedParams = mapParams(methodMeta.getParamMapping(), params);
            
            return jdbcExecutor.execute(session, methodMeta, mappedParams);
        } else {
            // delete/selectById: 直接使用 args
            return jdbcExecutor.execute(session, methodMeta, args);
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

---

## 第三部分：测试策略

### 单元测试

| 测试类 | 测试内容 |
|--------|----------|
| DaoProxyTemplateTest | methodNameMap 构建、executeByHash |
| DaoProxyGeneratorTest | ASM 字节码生成、方法实现 |
| AutoCrudMethodGeneratorTest | SQL 生成、MethodMeta 生成 |
| AutoCrudExecutorTest | entity 参数映射 |

### 集成测试

| 测试类 | 测试内容 |
|--------|----------|
| ASMProxyIntegrationTest | ASM 代理类完整调用链 |
| AutoCrudIntegrationTest | insert/update/delete/selectById 真实数据库操作 |
| MixedModeIntegrationTest | AutoCrud + 自定义方法混合使用 |

---

## 第四部分：实现计划

### Phase 1：ASM 代理生成

| 步骤 | 输出物 | 验证方式 |
|------|--------|----------|
| 1.1 DaoProxyTemplate | DaoProxyTemplate.java | 单元测试 |
| 1.2 DaoProxyGenerator | DaoProxyGenerator.java | 单元测试 |
| 1.3 ASM 字节码生成 | generateProxyClass() | 单元测试 |
| 1.4 methodNameMap 缓存 | buildMethodNameMap() | 单元测试 |

### Phase 2：AutoCrud 实现

| 步骤 | 输出物 | 验证方式 |
|------|--------|----------|
| 2.1 AutoCrudMethodGenerator | AutoCrudMethodGenerator.java | 单元测试 |
| 2.2 SQL 生成逻辑 | createInsertMethodMeta() 等 | 单元测试 |
| 2.3 AutoCrudExecutor | AutoCrudExecutor.java | 单元测试 |
| 2.4 entity 参数映射 | mapToRow() | 单元测试 |

### Phase 3：集成测试

| 步骤 | 输出物 | 验证方式 |
|------|--------|----------|
| 3.1 ASM 代理集成测试 | ASMProxyIntegrationTest.java | 测试通过 |
| 3.2 AutoCrud 集成测试 | AutoCrudIntegrationTest.java | 测试通过 |
| 3.3 混合模式测试 | MixedModeIntegrationTest.java | 测试通过 |

### Phase 4：替换 JDK Proxy

| 步骤 | 输出物 | 验证方式 |
|------|--------|----------|
| 4.1 修改 DaoRegistry | 使用 ASM 代理 | 所有测试通过 |
| 4.2 删除旧实现 | 删除 JDK Proxy 代码 | 编译通过 |
| 4.3 运行全量测试 | mvn test | 333 tests passed |

---

## 风险识别

| 风险 | 缓解策略 |
|------|----------|
| ASM 字节码生成错误 | 使用 COMPUTE_FRAMES 自动计算栈帧 |
| AutoCrud 方法签名冲突 | 在 DAO 接口声明时检查方法是否存在 |
| EntityMapper 字段顺序不一致 | 使用 MappingConfig 确保字段顺序 |
| 参数映射错误 | 单元测试覆盖所有场景 |

---

## 后续优化

1. 方法索引缓存（避免 hashCode 计算）
2. 批量操作 AutoCrud（insertBatch/updateBatch）
3. 条件查询 AutoCrud（selectByCondition）