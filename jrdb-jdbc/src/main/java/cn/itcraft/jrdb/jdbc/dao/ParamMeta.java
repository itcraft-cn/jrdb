package cn.itcraft.jrdb.jdbc.dao;

public class ParamMeta {
    private final int index;
    private final String name;
    private final Class<?> type;

    public ParamMeta(int index, String name, Class<?> type) {
        this.index = index;
        this.name = name;
        this.type = type;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }
}