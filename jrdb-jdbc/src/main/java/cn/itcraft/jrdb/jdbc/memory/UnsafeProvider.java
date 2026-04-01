package cn.itcraft.jrdb.jdbc.memory;

import sun.misc.Unsafe;
import java.lang.reflect.Field;

/**
 * Unsafe 实例提供者。
 * 
 * <p>通过反射获取 {@link sun.misc.Unsafe} 实例，为堆外内存操作提供基础能力。
 * 该类使用静态初始化块在类加载时完成 Unsafe 实例的获取，
 * 确保在后续使用时能够快速访问。</p>
 * 
 * <p>Unsafe 是 JDK 内部 API，用于执行低级别、不安全的操作，
 * 如直接内存访问、CAS 操作等。jrdb 框架使用 Unsafe 实现高性能的堆外内存管理。</p>
 * 
 * @author jrdb
 * @since 1.0.0
 */
public final class UnsafeProvider {
    
    /**
     * Unsafe 实例，在静态初始化块中通过反射获取。
     */
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
    
    /**
     * 私有构造方法，防止实例化。
     */
    private UnsafeProvider() {}
    
    /**
     * 获取 Unsafe 实例。
     * 
     * <p>Unsafe 实例在类加载时通过反射获取并缓存，
     * 该方法直接返回缓存的实例，调用效率高。</p>
     * 
     * @return Unsafe 实例，永不返回 null
     */
    public static Unsafe getUnsafe() {
        return UNSAFE;
    }
}