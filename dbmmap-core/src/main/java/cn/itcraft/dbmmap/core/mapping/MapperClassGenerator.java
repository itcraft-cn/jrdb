package cn.itcraft.dbmmap.core.mapping;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

public class MapperClassGenerator {

    private static final String ENTITY_MAPPER_INTERFACE = "cn/itcraft/dbmmap/core/mapping/EntityMapper";
    private static final int ASM_VERSION = Opcodes.ASM9;

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

    public String getGeneratedClassName(MappingConfig config) {
        String entityName = config.getEntityClass().getSimpleName();
        String packageName = config.getEntityClass().getPackage().getName();
        return packageName + "." + entityName + "Mapper";
    }

    private String getGeneratedClassNameInternal(MappingConfig config) {
        return getGeneratedClassName(config).replace('.', '/');
    }

    private void generateConstructor(ClassWriter cw, String className) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

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

    private void generateObjectTypeMapping(MethodVisitor mv, MappingField field,
                                           String entityClassName, String setterName) {
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/sql/ResultSet",
                           "getObject", "(Ljava/lang/String;)Ljava/lang/Object;", true);
        mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(field.getJavaType()));
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                           setterName, "(" + Type.getDescriptor(field.getJavaType()) + ")V", false);
    }

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

    private String getSetterName(String fieldName) {
        return "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    private String getGetterName(String fieldName) {
        return "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

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
