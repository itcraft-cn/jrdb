package com.dbmmap.core.mapping;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.List;

public class MapperClassGenerator {

    private static final String ENTITY_MAPPER_INTERFACE = "com/dbmmap/core/mapping/EntityMapper";
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
        for (int i = 0; i < fields.size(); i++) {
            MappingField field = fields.get(i);
            Class<?> fieldType = field.getJavaType();
            String setterName = getSetterName(field.getFieldName());
            
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitLdcInsn(field.getColumnName());

            if (fieldType == int.class || fieldType == Integer.class) {
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/sql/ResultSet",
                        "getInt", "(Ljava/lang/String;)I", true);
                if (fieldType == Integer.class) {
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer",
                            "valueOf", "(I)Ljava/lang/Integer;", false);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                            setterName, "(Ljava/lang/Integer;)V", false);
                } else {
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                            setterName, "(I)V", false);
                }
            } else if (fieldType == long.class || fieldType == Long.class) {
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/sql/ResultSet",
                        "getLong", "(Ljava/lang/String;)J", true);
                if (fieldType == Long.class) {
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long",
                            "valueOf", "(J)Ljava/lang/Long;", false);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                            setterName, "(Ljava/lang/Long;)V", false);
                } else {
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                            setterName, "(J)V", false);
                }
            } else if (fieldType == String.class) {
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/sql/ResultSet",
                        "getString", "(Ljava/lang/String;)Ljava/lang/String;", true);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                        setterName, "(Ljava/lang/String;)V", false);
            } else if (fieldType == double.class || fieldType == Double.class) {
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/sql/ResultSet",
                        "getDouble", "(Ljava/lang/String;)D", true);
                if (fieldType == Double.class) {
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double",
                            "valueOf", "(D)Ljava/lang/Double;", false);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                            setterName, "(Ljava/lang/Double;)V", false);
                } else {
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                            setterName, "(D)V", false);
                }
            } else if (fieldType == float.class || fieldType == Float.class) {
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/sql/ResultSet",
                        "getFloat", "(Ljava/lang/String;)F", true);
                if (fieldType == Float.class) {
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float",
                            "valueOf", "(F)Ljava/lang/Float;", false);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                            setterName, "(Ljava/lang/Float;)V", false);
                } else {
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                            setterName, "(F)V", false);
                }
            } else if (fieldType == boolean.class || fieldType == Boolean.class) {
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/sql/ResultSet",
                        "getBoolean", "(Ljava/lang/String;)Z", true);
                if (fieldType == Boolean.class) {
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean",
                            "valueOf", "(Z)Ljava/lang/Boolean;", false);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                            setterName, "(Ljava/lang/Boolean;)V", false);
                } else {
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                            setterName, "(Z)V", false);
                }
            } else if (fieldType == short.class || fieldType == Short.class) {
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/sql/ResultSet",
                        "getShort", "(Ljava/lang/String;)S", true);
                if (fieldType == Short.class) {
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short",
                            "valueOf", "(S)Ljava/lang/Short;", false);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                            setterName, "(Ljava/lang/Short;)V", false);
                } else {
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                            setterName, "(S)V", false);
                }
            } else if (fieldType == byte.class || fieldType == Byte.class) {
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/sql/ResultSet",
                        "getByte", "(Ljava/lang/String;)B", true);
                if (fieldType == Byte.class) {
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte",
                            "valueOf", "(B)Ljava/lang/Byte;", false);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                            setterName, "(Ljava/lang/Byte;)V", false);
                } else {
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                            setterName, "(B)V", false);
                }
            } else {
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/sql/ResultSet",
                        "getObject", "(Ljava/lang/String;)Ljava/lang/Object;", true);
                mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(fieldType));
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName,
                        setterName, "(" + Type.getDescriptor(fieldType) + ")V", false);
            }
        }

        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(3, 4);
        mv.visitEnd();
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
        if (primitiveType == int.class) return Integer.class;
        if (primitiveType == long.class) return Long.class;
        if (primitiveType == boolean.class) return Boolean.class;
        if (primitiveType == byte.class) return Byte.class;
        if (primitiveType == short.class) return Short.class;
        if (primitiveType == char.class) return Character.class;
        if (primitiveType == float.class) return Float.class;
        if (primitiveType == double.class) return Double.class;
        return primitiveType;
    }
}