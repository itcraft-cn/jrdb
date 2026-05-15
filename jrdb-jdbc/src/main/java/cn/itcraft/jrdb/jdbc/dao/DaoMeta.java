package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.annotation.SqlParamBindStyle;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DaoMeta {
    private final Class<?> daoInterface;
    private SqlParamBindStyle classStyle;
    private AutoCrudMeta autoCrudMeta;
    private final Map<Method, MethodMeta> methodMetas;

    public DaoMeta(Class<?> daoInterface) {
        this.daoInterface = Objects.requireNonNull(daoInterface, "daoInterface cannot be null");
        this.methodMetas = new HashMap<>();
    }

    public Class<?> getDaoInterface() {
        return daoInterface;
    }

    public SqlParamBindStyle getClassStyle() {
        return classStyle;
    }

    public void setClassStyle(SqlParamBindStyle classStyle) {
        this.classStyle = classStyle;
    }

    public AutoCrudMeta getAutoCrudMeta() {
        return autoCrudMeta;
    }

    public void setAutoCrudMeta(AutoCrudMeta autoCrudMeta) {
        this.autoCrudMeta = autoCrudMeta;
    }

    public Map<Method, MethodMeta> getMethodMetas() {
        return Collections.unmodifiableMap(methodMetas);
    }

    public void addMethodMeta(Method method, MethodMeta meta) {
        Objects.requireNonNull(method, "method cannot be null");
        Objects.requireNonNull(meta, "meta cannot be null");
        methodMetas.put(method, meta);
    }

    public MethodMeta getMethodMeta(Method method) {
        return methodMetas.get(method);
    }

    public boolean hasAutoCrud() {
        return autoCrudMeta != null;
    }
}