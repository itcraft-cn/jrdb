package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.annotation.SqlParamBindStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MethodMeta {
    private final String methodName;
    private SqlParamBindStyle methodStyle;
    private String sql;
    private SqlOperationType operationType;
    private final List<ParamMeta> paramMetas;

    public MethodMeta(String methodName) {
        this.methodName = Objects.requireNonNull(methodName, "methodName cannot be null");
        this.paramMetas = new ArrayList<>();
    }

    public String getMethodName() {
        return methodName;
    }

    public SqlParamBindStyle getMethodStyle() {
        return methodStyle;
    }

    public void setMethodStyle(SqlParamBindStyle methodStyle) {
        this.methodStyle = methodStyle;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public SqlOperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(SqlOperationType operationType) {
        this.operationType = operationType;
    }

    public List<ParamMeta> getParamMetas() {
        return Collections.unmodifiableList(paramMetas);
    }

    public void addParamMeta(ParamMeta paramMeta) {
        Objects.requireNonNull(paramMeta, "paramMeta cannot be null");
        paramMetas.add(paramMeta);
    }
}