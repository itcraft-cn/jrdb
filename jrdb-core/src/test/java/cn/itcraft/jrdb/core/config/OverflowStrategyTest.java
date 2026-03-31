package cn.itcraft.jrdb.core.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OverflowStrategyTest {

    @Test
    void shouldHaveErrorAndTruncateValues() {
        OverflowStrategy[] values = OverflowStrategy.values();
        assertEquals(2, values.length);
        assertEquals(OverflowStrategy.ERROR, OverflowStrategy.valueOf("ERROR"));
        assertEquals(OverflowStrategy.TRUNCATE, OverflowStrategy.valueOf("TRUNCATE"));
    }
}