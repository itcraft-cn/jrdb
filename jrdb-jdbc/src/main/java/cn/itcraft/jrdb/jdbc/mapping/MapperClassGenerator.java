package cn.itcraft.jrdb.jdbc.mapping;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

/**
 * ASM动态字节码生成器。
 *
 * <p>该类负责在运行时动态生成实现了 {@link EntityMapper} 接口的字节码。
 * 通过ASM库直接生成字节码，避免了反射调用带来的性能开销，
 * 实现了从ResultSet到实体对象的高效映射。</p>
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>动态生成EntityMapper实现类</li>
 *   <li>生成ResultSet到实体对象的映射方法</li>
 *   <li>生成实体对象到数据库行的映射方法</li>
 * </ul>
 *
 * <p>生成的字节码特点：</p>
 * <ul>
 *   <li>直接调用getter/setter方法，无反射开销</li>
 *   <li>针对原始类型和包装类型做了优化处理</li>
 *   <li>支持所有基本数据类型及其包装类</li>
 *   <li>支持String及其他对象类型</li>
 * </ul>
 *
 * @see EntityMapper
 * @see MappingConfig
 * @see MappingField
 */
public class MapperClassGenerator {

    /**
     * EntityMapper接口的内部名称（JVM格式）。
     *
     * <p>生成的类将实现此接口，提供mapFromResultSet和mapToRow方法的实现。</p>
     */
    private static final String ENTITY_MAPPER_INTERFACE = "cn/itcraft/jrdb/jdbc/mapping/EntityMapper";

    /**
     * ASM库版本号。
     *
     * <p>使用ASM9版本，对应JDK 21及以下版本的字节码操作支持。</p>
     */
    private static final int ASM_VERSION = Opcodes.ASM9;

    /**
     * 根据映射配置生成EntityMapper实现类的字节码。
     *
     * <p>该方法使用ASM ClassWriter动态生成一个实现了EntityMapper接口的类。
     * 生成的类包含：</p>
     * <ul>
     *   <li>默认构造函数</li>
     *   <li>mapFromResultSet方法：将ResultSet转换为实体对象</li>
     *   <li>mapToRow方法：将实体对象转换为数据库行数据</li>
     * </ul>
     *
     * @param config 映射配置，包含实体类信息和字段映射关系
     * @return 生成的类字节码
     */
    public byte[] generate(MappingConfig config) {
        String className = getGeneratedClassNameInternal(config);
        String entityClassName = Type.getInternalName(config.getEntityClass());

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        cw.visit(Opcodes.V1_8,
                 Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                 className,
                 null,
                 "java/lang/Object",
                 new String[]{ENTITY_MAPPER_INTERFACE});

        generateConstructor(cw, className);
        generateMapFromResultSet(cw, className, config, entityClassName);
        generateMapToRow(cw, className, config, entityClassName);

        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * 获取生成的Mapper类的全限定名。
     *
     * <p>命名规则：实体类包名 + 实体类简单名 + "Mapper"</p>
     *
     * @param config 映射配置
     * @return 生成的类的全限定名，如 "com.example.UserMapper"
     */
    public String getGeneratedClassName(MappingConfig config) {
        String entityName = config.getEntityClass().getSimpleName();
        String packageName = config.getEntityClass().getPackage().getName();
        return packageName + "." + entityName + "Mapper";
    }

    /**
     * 获取生成的Mapper类的内部名称（JVM格式）。
     *
     * <p>将全限定名中的点号替换为斜号，如 "com/example/UserMapper"。</p>
     *
     * @param config 映射配置
     * @return JVM内部格式的类名
     */
    private String getGeneratedClassNameInternal(MappingConfig config) {
        return getGeneratedClassName(config).replace('.', '/');
    }

    /**
     * 生成默认构造函数。
     *
     * <p>生成的字节码等同于：</p>
     * <pre>
     * public ClassName() {
     *     super();
     * }
     * </pre>
     *
     * @param cw ClassWriter实例
     * @param className 生成的类的内部名称
     */
    private void generateConstructor(ClassWriter cw, String className) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     * 生成mapFromResultSet方法。
     *
     * <p>该方法将ResultSet转换为实体对象。生成的字节码逻辑：</p>
     * <ol>
     *   <li>创建实体类实例</li>
     *   <li>遍历所有映射字段，从ResultSet获取值并调用setter方法设置</li>
     *   <li>返回填充完成的实体对象</li>
     * </ol>
     *
     * @param cw ClassWriter实例
     * @param className 生成的类的内部名称
     * @param config 映射配置
     * @param entityClassName 实体类的内部名称
     */
    private void generateMapFromResultSet(ClassWriter cw, String className,
                                          MappingConfig config, String entityClassName) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "mapFromResultSet",
                                          "(Ljava/sql/ResultSet;)Ljava/lang/Object;",
                                          "(Ljava/sql/ResultSet;)T;",
                                          new String[]{"java/sql/SQLException"});

        mv.visitCode();

        mv.visitTypeInsn(Opcodes.NEW, entityClassName);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, entityClassName, "<init>", "()V", false);
        mv.visitVarInsn(Opcodes.ASTORE, 2);

        List<MappingField> fields = config.getFields();
        for (MappingField field : fields) {
            generateFieldMapping(mv, field, entityClassName);
        }

        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(3, 4);
        mv.visitEnd();
    }

    /**
     * 生成单个字段的映射字节码。
     *
     * <p>根据字段类型生成相应的字节码：</p>
     * <ul>
     *   <li>基本类型及包装类型：调用ResultSet的getXxx方法获取值</li>
     *   <li>String类型：调用ResultSet的getString方法</li>
     *   <li>其他对象类型：调用ResultSet的getObject方法并强制类型转换</li>
     * </ul>
     *
     * @param mv MethodVisitor实例
     * @param field 映射字段配置
     * @param entityClassName 实体类的内部名称
     */
    private void generateFieldMapping(MethodVisitor mv, MappingField field, String entityClassName) {
        Class<?> fieldType = field.getJavaType();
        String setterName = getSetterName(field.getFieldName());

        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn(field.getColumnName());

        if (fieldType == int.class || fieldType == Integer.class) {
            generatePrimitiveTypeMapping(mv, entityClassName, setterName, "getInt", "I",
                                         "java/lang/Integer", "Ljava/lang/Integer;", fieldType == Integer.class);
        } else if (fieldType == long.class || fieldType == Long.class) {
            generatePrimitiveTypeMapping(mv, entityClassName, setterName, "getLong", "J",
                                         "java/lang/Long", "Ljava/lang/Long;", fieldType == Long.class);
        } else if (fieldType == double.class || fieldType == Double.class) {
            generatePrimitiveTypeMapping(mv, entityClassName, setterName, "getDouble", "D",
                                         "java/lang/Double", "Ljava/lang/Double;", fieldType == Double.class);
        } else if (fieldType == float.class || fieldType == Float.class) {
            generatePrimitiveTypeMapping(mv, entityClassName, setterName, "getFloat", "F",
                                         "java/lang/Float", "Ljava/lang/Float;", fieldType == Float.class);
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            generatePrimitiveTypeMapping(mv, entityClassName, setterName, "getBoolean", "Z",
                                         "java/lang/Boolean", "Ljava/lang/Boolean;", fieldType == Boolean.class);
        } else if (fieldType == short.class || fieldType == Short.class) {
            generatePrimitiveTypeMapping(mv, entityClassName, setterName, "getShort", "S",
                                         "java/lang/Short", "Ljava/lang/Short;", fieldType == Short.class);
        } else if (fieldType == byte.class || fieldType == Byte.class) {
            generatePrimitiveTypeMapping(mv, entityClassName, setterName, "getByte", "B",
                                         "java/lang/Byte", "Ljava/lang/Byte;", fieldType == Byte.class);
        } else if (fieldType == String.class) {
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/sql/ResultSet",
                               "getString", "(Ljava/lang/String;)Ljava/lang/String;", true);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                               setterName, "(Ljava/lang/String;)V", false);
        } else {
            generateObjectTypeMapping(mv, field, entityClassName, setterName);
        }
    }

    /**
     * 生成基本类型及其包装类型的映射字节码。
     *
     * <p>生成逻辑：</p>
     * <ol>
     *   <li>调用ResultSet的getXxx方法获取基本类型值</li>
     *   <li>如果是包装类型，调用包装类的valueOf方法进行装箱</li>
     *   <li>调用实体类的setter方法设置值</li>
     * </ol>
     *
     * @param mv MethodVisitor实例
     * @param entityClassName 实体类的内部名称
     * @param setterName setter方法名
     * @param resultSetMethod ResultSet的获取方法名，如"getInt"、"getLong"
     * @param primitiveDesc 基本类型的描述符，如"I"表示int，"J"表示long
     * @param wrapperClass 包装类的内部名称，如"java/lang/Integer"
     * @param wrapperDesc 包装类的描述符，如"Ljava/lang/Integer;"
     * @param isWrapper 是否为包装类型
     */
    private void generatePrimitiveTypeMapping(MethodVisitor mv, String entityClassName, String setterName,
                                              String resultSetMethod, String primitiveDesc,
                                              String wrapperClass, String wrapperDesc, boolean isWrapper) {
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/sql/ResultSet",
                           resultSetMethod, "(Ljava/lang/String;)" + primitiveDesc, true);
        if (isWrapper) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, wrapperClass, "valueOf",
                               "(" + primitiveDesc + ")" + wrapperDesc, false);
        }
        String setterDesc = "(" + (isWrapper ? wrapperDesc : primitiveDesc) + ")V";
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName, setterName, setterDesc, false);
    }

    /**
     * 生成对象类型的映射字节码。
     *
     * <p>生成逻辑：</p>
     * <ol>
     *   <li>调用ResultSet的getObject方法获取Object类型值</li>
     *   <li>使用CHECKCAST指令进行类型转换</li>
     *   <li>调用实体类的setter方法设置值</li>
     * </ol>
     *
     * @param mv MethodVisitor实例
     * @param field 映射字段配置
     * @param entityClassName 实体类的内部名称
     * @param setterName setter方法名
     */
    private void generateObjectTypeMapping(MethodVisitor mv, MappingField field,
                                           String entityClassName, String setterName) {
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/sql/ResultSet",
                           "getObject", "(Ljava/lang/String;)Ljava/lang/Object;", true);
        mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(field.getJavaType()));
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                           setterName, "(" + Type.getDescriptor(field.getJavaType()) + ")V", false);
    }

    /**
     * 生成mapToRow方法。
     *
     * <p>该方法将实体对象转换为Object数组（数据库行数据）。生成的字节码逻辑：</p>
     * <ol>
     *   <li>创建Object数组，长度等于字段数量</li>
     *   <li>遍历所有字段，调用getter方法获取值</li>
     *   <li>如果是基本类型，调用包装类的valueOf方法装箱</li>
     *   <li>将值存入数组对应位置</li>
     *   <li>返回填充完成的Object数组</li>
     * </ol>
     *
     * @param cw ClassWriter实例
     * @param className 生成的类的内部名称
     * @param config 映射配置
     * @param entityClassName 实体类的内部名称
     */
    private void generateMapToRow(ClassWriter cw, String className,
                                  MappingConfig config, String entityClassName) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "mapToRow",
                                          "(Ljava/lang/Object;)[Ljava/lang/Object;",
                                          "(TT;)[Ljava/lang/Object;",
                                          null);

        mv.visitCode();

        List<MappingField> fields = config.getFields();
        int fieldCount = fields.size();

        mv.visitIntInsn(Opcodes.BIPUSH, fieldCount);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        for (int i = 0; i < fieldCount; i++) {
            MappingField field = fields.get(i);

            mv.visitInsn(Opcodes.DUP);
            mv.visitIntInsn(Opcodes.BIPUSH, i);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitTypeInsn(Opcodes.CHECKCAST, entityClassName);

            String getterName = getGetterName(field.getFieldName());
            String getterDesc = "()" + Type.getDescriptor(field.getJavaType());
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                               getterName, getterDesc, false);

            if (field.getJavaType().isPrimitive()) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                   Type.getInternalName(getWrapperClass(field.getJavaType())),
                                   "valueOf", "(" + Type.getDescriptor(field.getJavaType()) + ")"
                                           + Type.getDescriptor(getWrapperClass(field.getJavaType())), false);
            }

            mv.visitInsn(Opcodes.AASTORE);
        }

        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(4, 3);
        mv.visitEnd();
    }

    /**
     * 根据字段名生成setter方法名。
     *
     * <p>将字段名首字母大写并添加"set"前缀。</p>
     *
     * @param fieldName 字段名
     * @return setter方法名，如字段名为"userName"则返回"setUserName"
     */
    private String getSetterName(String fieldName) {
        return "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    /**
     * 根据字段名生成getter方法名。
     *
     * <p>将字段名首字母大写并添加"get"前缀。</p>
     *
     * @param fieldName 字段名
     * @return getter方法名，如字段名为"userName"则返回"getUserName"
     */
    private String getGetterName(String fieldName) {
        return "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    /**
     * 获取基本类型对应的包装类。
     *
     * <p>用于在mapToRow方法中将基本类型值转换为包装类型并存入Object数组。</p>
     *
     * @param primitiveType 基本类型Class对象
     * @return 对应的包装类Class对象
     */
    private Class<?> getWrapperClass(Class<?> primitiveType) {
        if (primitiveType == int.class) {
            return Integer.class;
        }
        if (primitiveType == long.class) {
            return Long.class;
        }
        if (primitiveType == boolean.class) {
            return Boolean.class;
        }
        if (primitiveType == byte.class) {
            return Byte.class;
        }
        if (primitiveType == short.class) {
            return Short.class;
        }
        if (primitiveType == char.class) {
            return Character.class;
        }
        if (primitiveType == float.class) {
            return Float.class;
        }
        if (primitiveType == double.class) {
            return Double.class;
        }
        return primitiveType;
    }
}