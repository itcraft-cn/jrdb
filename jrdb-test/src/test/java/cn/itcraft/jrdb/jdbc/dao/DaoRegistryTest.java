package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.annotation.SqlParamBindStyle;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.session.Session;
import cn.itcraft.jrdb.jdbc.type.TypeHandlerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("DaoRegistry Test")
class DaoRegistryTest {

    @Test
    @DisplayName("constructor should work")
    void constructor() {
        MapperRegistry mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        DaoRegistry registry = new DaoRegistry(mapperRegistry, SqlParamBindStyle.JDBC);
        assertNotNull(registry);
        assertNotNull(registry.getDaoMetaCache());
    }

    @Test
    @DisplayName("getDao with null daoInterface should throw NullPointerException")
    void getDaoNullDaoInterface() {
        MapperRegistry mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        DaoRegistry registry = new DaoRegistry(mapperRegistry, SqlParamBindStyle.JDBC);
        Session session = mock(Session.class);

        assertThrows(NullPointerException.class, () -> registry.getDao(null, session));
    }

    @Test
    @DisplayName("getDao with null session should throw NullPointerException")
    void getDaoNullSession() {
        MapperRegistry mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        DaoRegistry registry = new DaoRegistry(mapperRegistry, SqlParamBindStyle.JDBC);

        assertThrows(NullPointerException.class, () -> registry.getDao(TestDao.class, null));
    }

    @Test
    @DisplayName("getDao should return proxy instance")
    void getDaoProxy() {
        MapperRegistry mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        DaoRegistry registry = new DaoRegistry(mapperRegistry, SqlParamBindStyle.JDBC);
        Session session = mock(Session.class);

        TestDao dao = registry.getDao(TestDao.class, session);
        assertNotNull(dao);
        assertTrue(TestDao.class.isAssignableFrom(dao.getClass()));
    }

    @Test
    @DisplayName("clear should clear caches")
    void clear() {
        MapperRegistry mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        DaoRegistry registry = new DaoRegistry(mapperRegistry, SqlParamBindStyle.JDBC);

        registry.clear();
        assertEquals(0, registry.getDaoMetaCache().size());
    }

    public interface TestDao {
        TestEntity findById(Long id);
    }

    public static class TestEntity {
        private Long id;
        private String name;
    }
}