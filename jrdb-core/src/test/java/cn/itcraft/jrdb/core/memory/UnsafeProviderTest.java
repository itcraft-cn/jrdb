package cn.itcraft.jrdb.core.memory;

import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.*;

class UnsafeProviderTest {

    @Test
    void shouldGetUnsafeInstance() {
        Unsafe unsafe = UnsafeProvider.getUnsafe();
        assertNotNull(unsafe);
    }

    @Test
    void shouldReturnSameInstance() {
        Unsafe u1 = UnsafeProvider.getUnsafe();
        Unsafe u2 = UnsafeProvider.getUnsafe();
        assertSame(u1, u2);
    }
}