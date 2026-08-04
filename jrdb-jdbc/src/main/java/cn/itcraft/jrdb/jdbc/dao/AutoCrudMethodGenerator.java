package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.mapping.EntityMapper;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;

import java.lang.reflect.Method;
import java.util.Objects;

public class AutoCrudMethodGenerator {

    private final MapperRegistry mapperRegistry;

    public AutoCrudMethodGenerator(MapperRegistry mapperRegistry) {
        this.mapperRegistry = Objects.requireNonNull(mapperRegistry, "mapperRegistry cannot be null");
    }

    public void generateAutoCrudMethods(DaoMeta daoMeta, AutoCrudMeta autoCrudMeta) {
        Class<?> entityClass = autoCrudMeta.getEntityClass();
        String tableName = autoCrudMeta.getTableName();
        String primaryKey = autoCrudMeta.getPrimaryKey();

        EntityMapper<?> mapper = mapperRegistry.getMapper(entityClass);
        String[] fieldNames = mapper.getFieldNames();

        generateInsertMethod(daoMeta, tableName, fieldNames, entityClass);
        generateUpdateMethod(daoMeta, tableName, fieldNames, primaryKey, entityClass);
        generateDeleteMethod(daoMeta, tableName, primaryKey);
        generateSelectByIdMethod(daoMeta, tableName, primaryKey, entityClass);
    }

    private void generateInsertMethod(DaoMeta daoMeta, String tableName, String[] fieldNames, Class<?> entityClass) {
        try {
            Method method = daoMeta.getDaoInterface().getDeclaredMethod("insert", entityClass);
            MethodMeta meta = new MethodMeta("insert");

            StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
            for (int i = 0; i < fieldNames.length; i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append(fieldNames[i]);
            }
            sql.append(") VALUES (");
            for (int i = 0; i < fieldNames.length; i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append("?");
            }
            sql.append(")");

            meta.setCachedSql(sql.toString());
            meta.setOperationType(SqlOperationType.INSERT);
            meta.setReturnType(int.class);

            for (int i = 0; i < fieldNames.length; i++) {
                meta.addParamMapping(i);
            }

            daoMeta.addMethodMeta(method, meta);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("insert method not found", e);
        }
    }

    private void generateUpdateMethod(DaoMeta daoMeta, String tableName, String[] fieldNames, String primaryKey,
                                      Class<?> entityClass) {
        try {
            Method method = daoMeta.getDaoInterface().getDeclaredMethod("update", entityClass);
            MethodMeta meta = new MethodMeta("update");

            StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
            boolean first = true;
            int pkIndex = -1;
            for (int i = 0; i < fieldNames.length; i++) {
                if (fieldNames[i].equalsIgnoreCase(primaryKey)) {
                    pkIndex = i;
                } else {
                    if (!first) {
                        sql.append(", ");
                    }
                    sql.append(fieldNames[i]).append("=?");
                    first = false;
                }
            }
            sql.append(" WHERE ").append(primaryKey).append("=?");

            meta.setCachedSql(sql.toString());
            meta.setOperationType(SqlOperationType.UPDATE);
            meta.setReturnType(int.class);

            for (int i = 0; i < fieldNames.length; i++) {
                if (i != pkIndex) {
                    meta.addParamMapping(i);
                }
            }
            meta.addParamMapping(pkIndex);

            daoMeta.addMethodMeta(method, meta);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("update method not found", e);
        }
    }

    private void generateDeleteMethod(DaoMeta daoMeta, String tableName, String primaryKey) {
        try {
            Method method = daoMeta.getDaoInterface().getDeclaredMethod("delete", Long.class);
            MethodMeta meta = new MethodMeta("delete");

            String sql = "DELETE FROM " + tableName + " WHERE " + primaryKey + "=?";
            meta.setCachedSql(sql);
            meta.setOperationType(SqlOperationType.DELETE);
            meta.setReturnType(int.class);
            meta.addParamMapping(0);

            daoMeta.addMethodMeta(method, meta);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("delete method not found", e);
        }
    }

    private void generateSelectByIdMethod(DaoMeta daoMeta, String tableName, String primaryKey, Class<?> entityClass) {
        try {
            Method method = daoMeta.getDaoInterface().getDeclaredMethod("selectById", Long.class);
            MethodMeta meta = new MethodMeta("selectById");

            String sql = "SELECT * FROM " + tableName + " WHERE " + primaryKey + "=?";
            meta.setCachedSql(sql);
            meta.setOperationType(SqlOperationType.SELECT);
            meta.setReturnType(entityClass);
            meta.addParamMapping(0);

            daoMeta.addMethodMeta(method, meta);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("selectById method not found", e);
        }
    }
}
