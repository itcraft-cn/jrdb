package cn.itcraft.jrdb.jdbc.executor;

public interface OverflowHandler {

    boolean shouldContinue(int currentRow, int maxRows);

    void onOverflow(int maxRows);
}