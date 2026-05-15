package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.annotation.AutoCrud;
import cn.itcraft.jrdb.jdbc.annotation.MyBatisStyle;
import cn.itcraft.jrdb.jdbc.annotation.Select;
import cn.itcraft.jrdb.jdbc.annotation.SqlParamBindStyle;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.type.TypeHandlerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DaoMetaCache Test")
class DaoMetaCacheTest {

    @Test
    @DisplayName("constructor with null mapperRegistry should throw NullPointerException")
    void constructorNullMapperRegistry() {
        assertThrows(NullPointerException.class, () -> new DaoMetaCache(null, SqlParamBindStyle.JDBC));
    }

    @Test
    @DisplayName("constructor with null defaultStyle should throw NullPointerException")
    void constructorNullDefaultStyle() {
        MapperRegistry mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        assertThrows(NullPointerException.class, () -> new DaoMetaCache(mapperRegistry, null));
    }

    @Test
    @DisplayName("parseDaoMeta should parse class style annotation")
    void parseClassStyle() {
        MapperRegistry mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        DaoMetaCache cache = new DaoMetaCache(mapperRegistry, SqlParamBindStyle.JDBC);

        DaoMeta meta = cache.getDaoMeta(MyBatisStyleDao.class);

        assertEquals(SqlParamBindStyle.MYBATIS, meta.getClassStyle());
    }

    @Test
    @DisplayName("parseDaoMeta should use default style when no class annotation")
    void parseDefaultStyle() {
        MapperRegistry mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        DaoMetaCache cache = new DaoMetaCache(mapperRegistry, SqlParamBindStyle.JDBC);

        DaoMeta meta = cache.getDaoMeta(NoStyleDao.class);

        assertEquals(SqlParamBindStyle.JDBC, meta.getClassStyle());
    }

    @Test
    @DisplayName("parseDaoMeta should parse AutoCrud annotation")
    void parseAutoCrud() {
        MapperRegistry mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        DaoMetaCache cache = new DaoMetaCache(mapperRegistry, SqlParamBindStyle.JDBC);

        DaoMeta meta = cache.getDaoMeta(AutoCrudDao.class);

        assertTrue(meta.hasAutoCrud());
        assertEquals(TestEntity.class, meta.getAutoCrudMeta().getEntityClass());
        assertEquals("id", meta.getAutoCrudMeta().getPrimaryKey());
        assertEquals("test_entity", meta.getAutoCrudMeta().getTableName());
    }

    @Test
    @DisplayName("parseDaoMeta should parse Select annotation")
    void parseSelectAnnotation() throws NoSuchMethodException {
        MapperRegistry mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        DaoMetaCache cache = new DaoMetaCache(mapperRegistry, SqlParamBindStyle.JDBC);

        DaoMeta meta = cache.getDaoMeta(MyBatisStyleDao.class);
        Method method = MyBatisStyleDao.class.getMethod("findById", Long.class);
        MethodMeta methodMeta = meta.getMethodMeta(method);

        assertNotNull(methodMeta);
        assertEquals("SELECT * FROM test_entity WHERE id = #{id}", methodMeta.getSql());
        assertEquals(SqlOperationType.SELECT, methodMeta.getOperationType());
    }

    @Test
    @DisplayName("parseDaoMeta should parse method params")
    void parseMethodParams() throws NoSuchMethodException {
        MapperRegistry mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        DaoMetaCache cache = new DaoMetaCache(mapperRegistry, SqlParamBindStyle.JDBC);

        DaoMeta meta = cache.getDaoMeta(MyBatisStyleDao.class);
        Method method = MyBatisStyleDao.class.getMethod("findById", Long.class);
        MethodMeta methodMeta = meta.getMethodMeta(method);

        assertNotNull(methodMeta);
        assertEquals(1, methodMeta.getParamMetas().size());
        assertEquals(0, methodMeta.getParamMetas().get(0).getIndex());
        assertEquals(Long.class, methodMeta.getParamMetas().get(0).getType());
    }

    @Test
    @DisplayName("clear should clear cache")
    void clearCache() {
        MapperRegistry mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        DaoMetaCache cache = new DaoMetaCache(mapperRegistry, SqlParamBindStyle.JDBC);

        cache.getDaoMeta(MyBatisStyleDao.class);
        assertEquals(1, cache.size());

        cache.clear();
        assertEquals(0, cache.size());
    }

    @MyBatisStyle
    interface MyBatisStyleDao {
        @Select("SELECT * FROM test_entity WHERE id = #{id}")
        TestEntity findById(Long id);
    }

    interface NoStyleDao {
        TestEntity findById(Long id);
    }

    @AutoCrud(entity = TestEntity.class, primaryKey = "id")
    interface AutoCrudDao {
        int insert(TestEntity entity);
        int update(TestEntity entity);
        int delete(Long id);
        TestEntity selectById(Long id);
    }

    public static class TestEntity {
        private Long id;
        private String name;
    }
}