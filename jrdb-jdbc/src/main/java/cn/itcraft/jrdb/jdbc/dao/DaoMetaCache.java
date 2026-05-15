package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.annotation.AutoCrud;
import cn.itcraft.jrdb.jdbc.annotation.Delete;
import cn.itcraft.jrdb.jdbc.annotation.HibernateStyle;
import cn.itcraft.jrdb.jdbc.annotation.Insert;
import cn.itcraft.jrdb.jdbc.annotation.JdbcStyle;
import cn.itcraft.jrdb.jdbc.annotation.MyBatisStyle;
import cn.itcraft.jrdb.jdbc.annotation.Query;
import cn.itcraft.jrdb.jdbc.annotation.Select;
import cn.itcraft.jrdb.jdbc.annotation.SqlParamBindStyle;
import cn.itcraft.jrdb.jdbc.annotation.Update;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DaoMetaCache {

    private final Map<Class<?>, DaoMeta> cache = new ConcurrentHashMap<>();
    private final MapperRegistry mapperRegistry;
    private final SqlParamBindStyle defaultStyle;

    public DaoMetaCache(MapperRegistry mapperRegistry, SqlParamBindStyle defaultStyle) {
        this.mapperRegistry = Objects.requireNonNull(mapperRegistry, "mapperRegistry cannot be null");
        this.defaultStyle = Objects.requireNonNull(defaultStyle, "defaultStyle cannot be null");
    }

    public DaoMeta getDaoMeta(Class<?> daoInterface) {
        return cache.computeIfAbsent(daoInterface, this::parseDaoMeta);
    }

    private DaoMeta parseDaoMeta(Class<?> daoInterface) {
        DaoMeta daoMeta = new DaoMeta(daoInterface);

        parseClassStyle(daoInterface, daoMeta);
        parseAutoCrud(daoInterface, daoMeta);
        parseMethodMetas(daoInterface, daoMeta);

        return daoMeta;
    }

    private void parseClassStyle(Class<?> daoInterface, DaoMeta daoMeta) {
        if (daoInterface.isAnnotationPresent(MyBatisStyle.class)) {
            daoMeta.setClassStyle(SqlParamBindStyle.MYBATIS);
        } else if (daoInterface.isAnnotationPresent(HibernateStyle.class)) {
            daoMeta.setClassStyle(SqlParamBindStyle.HIBERNATE);
        } else if (daoInterface.isAnnotationPresent(JdbcStyle.class)) {
            daoMeta.setClassStyle(SqlParamBindStyle.JDBC);
        } else {
            daoMeta.setClassStyle(defaultStyle);
        }
    }

    private void parseAutoCrud(Class<?> daoInterface, DaoMeta daoMeta) {
        AutoCrud autoCrud = daoInterface.getAnnotation(AutoCrud.class);
        if (autoCrud != null) {
            Class<?> entityClass = autoCrud.entity();
            String primaryKey = autoCrud.primaryKey();
            String tableName = inferTableName(entityClass);
            daoMeta.setAutoCrudMeta(new AutoCrudMeta(entityClass, primaryKey, tableName));
        }
    }

    private void parseMethodMetas(Class<?> daoInterface, DaoMeta daoMeta) {
        for (Method method : daoInterface.getDeclaredMethods()) {
            MethodMeta methodMeta = parseMethodMeta(method, daoMeta);
            daoMeta.addMethodMeta(method, methodMeta);
        }
    }

    private MethodMeta parseMethodMeta(Method method, DaoMeta daoMeta) {
        MethodMeta methodMeta = new MethodMeta(method.getName());

        parseMethodStyle(method, methodMeta, daoMeta);
        parseSqlAnnotation(method, methodMeta);
        parseParams(method, methodMeta);
        parseReturnType(method, methodMeta);

        return methodMeta;
    }

    private void parseMethodStyle(Method method, MethodMeta methodMeta, DaoMeta daoMeta) {
        if (method.isAnnotationPresent(MyBatisStyle.class)) {
            methodMeta.setMethodStyle(SqlParamBindStyle.MYBATIS);
        } else if (method.isAnnotationPresent(HibernateStyle.class)) {
            methodMeta.setMethodStyle(SqlParamBindStyle.HIBERNATE);
        } else if (method.isAnnotationPresent(JdbcStyle.class)) {
            methodMeta.setMethodStyle(SqlParamBindStyle.JDBC);
        } else {
            methodMeta.setMethodStyle(daoMeta.getClassStyle());
        }
    }

    private void parseSqlAnnotation(Method method, MethodMeta methodMeta) {
        Select select = method.getAnnotation(Select.class);
        if (select != null) {
            methodMeta.setSql(select.value());
            methodMeta.setOperationType(SqlOperationType.SELECT);
            return;
        }

        Update update = method.getAnnotation(Update.class);
        if (update != null) {
            methodMeta.setSql(update.value());
            methodMeta.setOperationType(SqlOperationType.UPDATE);
            return;
        }

        Insert insert = method.getAnnotation(Insert.class);
        if (insert != null) {
            methodMeta.setSql(insert.value());
            methodMeta.setOperationType(SqlOperationType.INSERT);
            return;
        }

        Delete delete = method.getAnnotation(Delete.class);
        if (delete != null) {
            methodMeta.setSql(delete.value());
            methodMeta.setOperationType(SqlOperationType.DELETE);
            return;
        }

        Query query = method.getAnnotation(Query.class);
        if (query != null) {
            methodMeta.setSql(query.value());
            methodMeta.setOperationType(SqlOperationType.SELECT);
        }
    }

    private void parseParams(Method method, MethodMeta methodMeta) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            String name = parameters[i].getName();
            Class<?> type = parameters[i].getType();
            methodMeta.addParamMeta(new ParamMeta(i, name, type));
        }
    }

    private void parseReturnType(Method method, MethodMeta methodMeta) {
        methodMeta.setReturnType(method.getReturnType());
    }

    private String inferTableName(Class<?> entityClass) {
        String simpleName = entityClass.getSimpleName();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < simpleName.length(); i++) {
            char c = simpleName.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('_');
            }
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }
}