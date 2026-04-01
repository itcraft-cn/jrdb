package cn.itcraft.jrdb.r2dbc.mapping;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

public class R2dbcMapperClassGenerator {

    private static final String ENTITY_MAPPER_INTERFACE = "cn/itcraft/jrdb/r2dbc/mapping/R2dbcEntityMapper";
    private static final int ASM_VERSION = Opcodes.ASM9;

    public byte[] generate(R2dbcMappingConfig config) {
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
        generateMapFromRow(cw, className, config, entityClassName);
        generateMapToRow(cw, className, config, entityClassName);

        cw.visitEnd();
        return cw.toByteArray();
    }

    public String getGeneratedClassName(R2dbcMappingConfig config) {
        String entityName = config.getEntityClass().getSimpleName();
        String packageName = config.getEntityClass().getPackage().getName();
        return packageName + "." + entityName + "R2dbcMapper";
    }

    private String getGeneratedClassNameInternal(R2dbcMappingConfig config) {
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

    private void generateMapFromRow(ClassWriter cw, String className,
                                    R2dbcMappingConfig config, String entityClassName) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "mapFromRow",
                                          "(Lio/r2dbc/spi/Row;)Ljava/lang/Object;",
                                          "(Lio/r2dbc/spi/Row;)TT;",
                                          null);

        mv.visitCode();

        mv.visitTypeInsn(Opcodes.NEW, entityClassName);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, entityClassName, "<init>", "()V", false);
        mv.visitVarInsn(Opcodes.ASTORE, 2);

        List<R2dbcMappingField> fields = config.getFields();
        for (R2dbcMappingField field : fields) {
            generateFieldMapping(mv, field, entityClassName);
        }

        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(3, 4);
        mv.visitEnd();
    }

    private void generateFieldMapping(MethodVisitor mv, R2dbcMappingField field, String entityClassName) {
        Class<?> fieldType = field.getJavaType();
        String setterName = getSetterName(field.getFieldName());

        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn(field.getColumnName());
        
        mv.visitLdcInsn(org.objectweb.asm.Type.getType(org.objectweb.asm.Type.getDescriptor(fieldType)));

        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "io/r2dbc/spi/Row",
                           "get", "(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;", true);

        String typeDesc = Type.getDescriptor(fieldType);
        if (fieldType.isPrimitive()) {
            mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(getWrapperClass(fieldType)));
            String unboxMethod = getUnboxMethod(fieldType);
            String wrapperInternal = Type.getInternalName(getWrapperClass(fieldType));
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, wrapperInternal, unboxMethod, "()" + typeDesc, false);
        } else {
            mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(fieldType));
        }

        String setterDesc = "(" + typeDesc + ")V";
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entityClassName, setterName, setterDesc, false);
    }

    private String getUnboxMethod(Class<?> primitiveType) {
        if (primitiveType == int.class) return "intValue";
        if (primitiveType == long.class) return "longValue";
        if (primitiveType == double.class) return "doubleValue";
        if (primitiveType == float.class) return "floatValue";
        if (primitiveType == boolean.class) return "booleanValue";
        if (primitiveType == short.class) return "shortValue";
        if (primitiveType == byte.class) return "byteValue";
        return "intValue";
    }

    private void generateMapToRow(ClassWriter cw, String className,
                                  R2dbcMappingConfig config, String entityClassName) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "mapToRow",
                                          "(Ljava/lang/Object;)[Ljava/lang/Object;",
                                          "(TT;)[Ljava/lang/Object;",
                                          null);

        mv.visitCode();

        List<R2dbcMappingField> fields = config.getFields();
        int fieldCount = fields.size();

        mv.visitIntInsn(Opcodes.BIPUSH, fieldCount);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        for (int i = 0; i < fieldCount; i++) {
            R2dbcMappingField field = fields.get(i);

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