package cn.itcraft.jrdb.jdbc.integration;

import cn.itcraft.jrdb.jdbc.annotation.MyBatisStyle;
import cn.itcraft.jrdb.jdbc.annotation.Select;
import cn.itcraft.jrdb.jdbc.annotation.SqlParamBindStyle;
import cn.itcraft.jrdb.jdbc.config.JrdbConfig;
import cn.itcraft.jrdb.jdbc.dao.DaoMeta;
import cn.itcraft.jrdb.jdbc.dao.DaoMetaCache;
import cn.itcraft.jrdb.jdbc.dao.MethodMeta;
import cn.itcraft.jrdb.jdbc.dao.ParamMeta;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.session.SessionFactory;
import cn.itcraft.jrdb.jdbc.type.TypeHandlerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Param Name Debug Test")
class ParamNameDebugTest {

    @Test
    @DisplayName("debug param names")
    void debugParamNames() throws NoSuchMethodException {
        MapperRegistry mapperRegistry = new MapperRegistry(new TypeHandlerRegistry());
        DaoMetaCache cache = new DaoMetaCache(mapperRegistry, SqlParamBindStyle.JDBC);

        DaoMeta meta = cache.getDaoMeta(UserDao.class);
        Method method = UserDao.class.getMethod("findById", Long.class);
        MethodMeta methodMeta = meta.getMethodMeta(method);

        List<ParamMeta> paramMetas = methodMeta.getParamMetas();
        for (ParamMeta paramMeta : paramMetas) {
            System.out.println("Param index: " + paramMeta.getIndex() + ", name: " + paramMeta.getName() + ", type: " + paramMeta.getType());
        }
    }

    @MyBatisStyle
    interface UserDao {
        @Select("SELECT * FROM users WHERE id = #{id}")
        User findById(Long id);
    }

    static class User {
        private Long id;
        private String name;
    }
}