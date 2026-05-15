package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.annotation.SqlParamBindStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MethodMeta Test")
class MethodMetaTest {

    @Test
    @DisplayName("constructor with null methodName should throw NullPointerException")
    void constructorNullMethodName() {
        assertThrows(NullPointerException.class, () -> new MethodMeta(null));
    }

    @Test
    @DisplayName("constructor with valid methodName should succeed")
    void constructorValidMethodName() {
        MethodMeta meta = new MethodMeta("findById");
        assertEquals("findById", meta.getMethodName());
    }

    @Test
    @DisplayName("setMethodStyle should work")
    void setMethodStyle() {
        MethodMeta meta = new MethodMeta("findById");
        meta.setMethodStyle(SqlParamBindStyle.MYBATIS);
        assertEquals(SqlParamBindStyle.MYBATIS, meta.getMethodStyle());
    }

    @Test
    @DisplayName("setSql should work")
    void setSql() {
        MethodMeta meta = new MethodMeta("findById");
        meta.setSql("SELECT * FROM users WHERE id = ?");
        assertEquals("SELECT * FROM users WHERE id = ?", meta.getSql());
    }

    @Test
    @DisplayName("setOperationType should work")
    void setOperationType() {
        MethodMeta meta = new MethodMeta("findById");
        meta.setOperationType(SqlOperationType.SELECT);
        assertEquals(SqlOperationType.SELECT, meta.getOperationType());
    }

    @Test
    @DisplayName("addParamMeta should work")
    void addParamMeta() {
        MethodMeta meta = new MethodMeta("findById");
        ParamMeta paramMeta = new ParamMeta(0, "id", Long.class);
        
        meta.addParamMeta(paramMeta);
        
        assertEquals(1, meta.getParamMetas().size());
        assertEquals(paramMeta, meta.getParamMetas().get(0));
    }

    @Test
    @DisplayName("addParamMeta with null should throw NullPointerException")
    void addParamMetaNull() {
        MethodMeta meta = new MethodMeta("findById");
        
        assertThrows(NullPointerException.class, () -> meta.addParamMeta(null));
    }

    @Test
    @DisplayName("getParamMetas should be unmodifiable")
    void getParamMetasUnmodifiable() {
        MethodMeta meta = new MethodMeta("findById");
        ParamMeta paramMeta = new ParamMeta(0, "id", Long.class);
        meta.addParamMeta(paramMeta);
        
        assertThrows(UnsupportedOperationException.class, () -> meta.getParamMetas().add(paramMeta));
    }
}