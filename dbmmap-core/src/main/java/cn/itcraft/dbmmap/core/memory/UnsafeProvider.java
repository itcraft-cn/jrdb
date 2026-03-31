package cn.itcraft.dbmmap.core.memory;

import sun.misc.Unsafe;
import java.lang.reflect.Field;

public final class UnsafeProvider {
    
    private static final Unsafe UNSAFE;
    
    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Unsafe instance", e);
        }
    }
    
    private UnsafeProvider() {}
    
    public static Unsafe getUnsafe() {
        return UNSAFE;
    }
}