package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.annotation.SqlParamBindStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DaoMeta Test")
class DaoMetaTest {

    @Test
    @DisplayName("constructor with null daoInterface should throw NullPointerException")
    void constructorNullDaoInterface() {
        assertThrows(NullPointerException.class, () -> new DaoMeta(null));
    }

    @Test
    @DisplayName("constructor with valid daoInterface should succeed")
    void constructorValidDaoInterface() {
        DaoMeta meta = new DaoMeta(TestDao.class);
        assertEquals(TestDao.class, meta.getDaoInterface());
    }

    @Test
    @DisplayName("setClassStyle should work")
    void setClassStyle() {
        DaoMeta meta = new DaoMeta(TestDao.class);
        meta.setClassStyle(SqlParamBindStyle.MYBATIS);
        assertEquals(SqlParamBindStyle.MYBATIS, meta.getClassStyle());
    }

    @Test
    @DisplayName("addMethodMeta should work")
    void addMethodMeta() throws NoSuchMethodException {
        DaoMeta meta = new DaoMeta(TestDao.class);
        Method method = TestDao.class.getMethod("findById", Long.class);
        MethodMeta methodMeta = new MethodMeta("findById");
        
        meta.addMethodMeta(method, methodMeta);
        
        Map<Method, MethodMeta> methodMetas = meta.getMethodMetas();
        assertEquals(1, methodMetas.size());
        assertTrue(methodMetas.containsKey(method));
        assertEquals(methodMeta, methodMetas.get(method));
    }

    @Test
    @DisplayName("getMethodMeta should return null if not exists")
    void getMethodMetaNotExists() throws NoSuchMethodException {
        DaoMeta meta = new DaoMeta(TestDao.class);
        Method method = TestDao.class.getMethod("findById", Long.class);
        
        assertNull(meta.getMethodMeta(method));
    }

    @Test
    @DisplayName("setAutoCrudMeta should work")
    void setAutoCrudMeta() {
        DaoMeta meta = new DaoMeta(TestDao.class);
        AutoCrudMeta autoCrudMeta = new AutoCrudMeta(TestEntity.class, "id", "test_entity");
        
        meta.setAutoCrudMeta(autoCrudMeta);
        
        assertEquals(autoCrudMeta, meta.getAutoCrudMeta());
        assertTrue(meta.hasAutoCrud());
    }

    @Test
    @DisplayName("hasAutoCrud should return false when not set")
    void hasAutoCrudFalse() {
        DaoMeta meta = new DaoMeta(TestDao.class);
        assertFalse(meta.hasAutoCrud());
    }

    @Test
    @DisplayName("addMethodMeta with null method should throw NullPointerException")
    void addMethodMetaNullMethod() {
        DaoMeta meta = new DaoMeta(TestDao.class);
        MethodMeta methodMeta = new MethodMeta("test");
        
        assertThrows(NullPointerException.class, () -> meta.addMethodMeta(null, methodMeta));
    }

    @Test
    @DisplayName("addMethodMeta with null meta should throw NullPointerException")
    void addMethodMetaNullMeta() throws NoSuchMethodException {
        DaoMeta meta = new DaoMeta(TestDao.class);
        Method method = TestDao.class.getMethod("findById", Long.class);
        
        assertThrows(NullPointerException.class, () -> meta.addMethodMeta(method, null));
    }

    interface TestDao {
        TestEntity findById(Long id);
    }

    static class TestEntity {
        private Long id;
        private String name;
    }
}