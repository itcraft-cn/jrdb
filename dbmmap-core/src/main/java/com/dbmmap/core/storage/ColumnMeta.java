package com.dbmmap.core.storage;

public class ColumnMeta {
    
    private final String name;
    private final int sqlType;
    private final int size;
    
    public ColumnMeta(String name, int sqlType, int size) {
        this.name = name;
        this.sqlType = sqlType;
        this.size = size;
    }
    
    public String getName() { return name; }
    public int getSqlType() { return sqlType; }
    public int getSize() { return size; }
}