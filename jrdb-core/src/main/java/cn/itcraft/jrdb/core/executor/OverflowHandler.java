package cn.itcraft.jrdb.core.executor;

public interface OverflowHandler {

    boolean shouldContinue(int currentRow, int maxRows);

    void onOverflow(int maxRows);
}