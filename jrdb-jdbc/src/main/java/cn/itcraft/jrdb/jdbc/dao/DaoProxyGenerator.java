package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.executor.StyleExecutor;
import cn.itcraft.jrdb.jdbc.mapping.MapperClassLoader;
import cn.itcraft.jrdb.jdbc.session.Session;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

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
                                          "(Lcn/itcraft/jrdb/jdbc/dao/DaoMeta;Lcn/itcraft/jrdb/jdbc/session/Session;Lcn/itcraft/jrdb/jdbc/executor/StyleExecutor;)V",
                                          null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKESPECIAL, templateInternal, "<init>",
                           "(Lcn/itcraft/jrdb/jdbc/dao/DaoMeta;Lcn/itcraft/jrdb/jdbc/session/Session;Lcn/itcraft/jrdb/jdbc/executor/StyleExecutor;)V",
                           false);
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

        mv.visitMethodInsn(INVOKEVIRTUAL, templateInternal, "executeByHash", "(I[Ljava/lang/Object;)Ljava/lang/Object;",
                           false);

        if (returnType == void.class) {
            mv.visitInsn(RETURN);
        } else if (returnType.isPrimitive()) {
            unboxPrimitive(mv, returnType);
        } else {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(returnType));
            mv.visitInsn(ARETURN);
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

        if (returnType == int.class) {
            mv.visitInsn(IRETURN);
        } else if (returnType == long.class) {
            mv.visitInsn(LRETURN);
        } else if (returnType == boolean.class) {
            mv.visitInsn(IRETURN);
        } else if (returnType == double.class) {
            mv.visitInsn(DRETURN);
        } else if (returnType == float.class) {
            mv.visitInsn(FRETURN);
        }
    }

    private String getBoxedType(Class<?> primitiveType) {
        if (primitiveType == int.class) {
            return "java/lang/Integer";
        }
        if (primitiveType == long.class) {
            return "java/lang/Long";
        }
        if (primitiveType == boolean.class) {
            return "java/lang/Boolean";
        }
        if (primitiveType == double.class) {
            return "java/lang/Double";
        }
        if (primitiveType == float.class) {
            return "java/lang/Float";
        }
        throw new IllegalArgumentException("Unsupported primitive type: " + primitiveType);
    }

    private String getUnboxMethod(Class<?> primitiveType) {
        if (primitiveType == int.class) {
            return "intValue";
        }
        if (primitiveType == long.class) {
            return "longValue";
        }
        if (primitiveType == boolean.class) {
            return "booleanValue";
        }
        if (primitiveType == double.class) {
            return "doubleValue";
        }
        if (primitiveType == float.class) {
            return "floatValue";
        }
        throw new IllegalArgumentException("Unsupported primitive type: " + primitiveType);
    }
}
