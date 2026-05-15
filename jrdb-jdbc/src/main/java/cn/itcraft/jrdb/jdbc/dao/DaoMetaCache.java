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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DaoMetaCache {

    private final Map<Class<?>, DaoMeta> cache = new ConcurrentHashMap<>();
    private final MapperRegistry mapperRegistry;
    private final SqlParamBindStyle defaultStyle;
    private static final Pattern MYBATIS_PARAM_PATTERN = Pattern.compile("#\\{(\\w+)}");
    private static final Pattern HIBERNATE_PARAM_PATTERN = Pattern.compile(":(\\w+)");

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
            String tableName = autoCrud.tableName();
            if (tableName == null || tableName.isEmpty()) {
                tableName = inferTableName(entityClass);
            }
            AutoCrudMeta autoCrudMeta = new AutoCrudMeta(entityClass, primaryKey, tableName);
            daoMeta.setAutoCrudMeta(autoCrudMeta);
            
            AutoCrudMethodGenerator generator = new AutoCrudMethodGenerator(mapperRegistry);
            generator.generateAutoCrudMethods(daoMeta, autoCrudMeta);
        }
    }

    private void parseMethodMetas(Class<?> daoInterface, DaoMeta daoMeta) {
        for (Method method : daoInterface.getDeclaredMethods()) {
            if (daoMeta.getMethodMeta(method) != null) {
                continue;
            }
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
        cacheSqlConversion(methodMeta);

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

    private void cacheSqlConversion(MethodMeta methodMeta) {
        String sql = methodMeta.getSql();
        if (sql == null) {
            return;
        }

        SqlParamBindStyle style = methodMeta.getMethodStyle();
        
        switch (style) {
            case MYBATIS:
                cacheMyBatisConversion(methodMeta, sql);
                break;
            case HIBERNATE:
                cacheHibernateConversion(methodMeta, sql);
                break;
            case JDBC:
                methodMeta.setCachedSql(sql);
                cacheJdbcParamMapping(methodMeta);
                break;
        }
    }

    private void cacheMyBatisConversion(MethodMeta methodMeta, String sql) {
        String cachedSql = sql.replaceAll("#\\{(\\w+)}", "?");
        methodMeta.setCachedSql(cachedSql);
        
        methodMeta.clearParamMapping();
        Matcher matcher = MYBATIS_PARAM_PATTERN.matcher(sql);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            int paramIndex = findParamIndexByName(methodMeta.getParamMetas(), paramName);
            methodMeta.addParamMapping(paramIndex);
        }
    }

    private void cacheHibernateConversion(MethodMeta methodMeta, String sql) {
        String originalSql = sql;
        if (sql.toLowerCase().startsWith("from ")) {
            String entityPart = sql.substring(5).trim();
            int whereIndex = entityPart.toLowerCase().indexOf(" where ");
            if (whereIndex > 0) {
                String entityName = entityPart.substring(0, whereIndex).trim();
                String whereClause = entityPart.substring(whereIndex + 7).trim();
                String convertedSql = "SELECT * FROM " + inferTableNameFromSimpleName(entityName) + " WHERE " + whereClause;
                sql = convertedSql;
            } else {
                String entityName = entityPart;
                sql = "SELECT * FROM " + inferTableNameFromSimpleName(entityName);
            }
        }
        
        String cachedSql = sql.replaceAll(":(\\w+)", "?");
        methodMeta.setCachedSql(cachedSql);
        
        methodMeta.clearParamMapping();
        Matcher matcher = HIBERNATE_PARAM_PATTERN.matcher(originalSql);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            int paramIndex = findParamIndexByName(methodMeta.getParamMetas(), paramName);
            methodMeta.addParamMapping(paramIndex);
        }
    }

    private int findParamIndexByName(List<ParamMeta> paramMetas, String paramName) {
        for (ParamMeta paramMeta : paramMetas) {
            if (paramMeta.getName().equals(paramName)) {
                return paramMeta.getIndex();
            }
        }
        throw new IllegalArgumentException("Parameter not found: " + paramName);
    }

    private void cacheJdbcParamMapping(MethodMeta methodMeta) {
        methodMeta.clearParamMapping();
        String sql = methodMeta.getCachedSql();
        if (sql != null) {
            int paramCount = countParameters(sql);
            for (int i = 0; i < paramCount; i++) {
                methodMeta.addParamMapping(i);
            }
        }
    }

    private int countParameters(String sql) {
        int count = 0;
        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) == '?') {
                count++;
            }
        }
        return count;
    }

    private String inferTableName(Class<?> entityClass) {
        String simpleName = entityClass.getSimpleName();
        return inferTableNameFromSimpleName(simpleName);
    }

    private String inferTableNameFromSimpleName(String simpleName) {
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