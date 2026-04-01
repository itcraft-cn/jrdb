package cn.itcraft.jrdb.r2dbc.mapping;

public class R2dbcMapperClassLoader extends ClassLoader {

    public R2dbcMapperClassLoader() {
        super(R2dbcMapperClassLoader.class.getClassLoader());
    }

    public Class<?> defineClass(String name, byte[] bytecode) {
        return defineClass(name, bytecode, 0, bytecode.length);
    }
}