package cn.itcraft.dbmmap.core.query;

public interface QueryResult extends AutoCloseable {
    
    int getRowCount();
    
    int getColumnCount();
    
    String getColumnName(int columnIndex);
    
    int getInt(String columnName);
    
    String getString(String columnName);
    
    long getLong(String columnName);
    
    double getDouble(String columnName);
    
    int getInt(int columnIndex);
    
    String getString(int columnIndex);
    
    long getLong(int columnIndex);
    
    double getDouble(int columnIndex);
    
    boolean next();
    
    void reset();
    
    void release();
    
    void close();
}