package cn.itcraft.jrdb.core.executor;

public class ErrorOverflowHandler implements OverflowHandler {

    @Override
    public boolean shouldContinue(int currentRow, int maxRows) {
        if (currentRow >= maxRows) {
            throw new IllegalStateException(
                    "Query result exceeds max rows: " + maxRows +
                    ". Consider using OverflowStrategy.TRUNCATE or increase queryCacheSize.");
        }
        return true;
    }

    @Override
    public void onOverflow(int maxRows) {
        throw new IllegalStateException(
                "Query result exceeds max rows: " + maxRows +
                ". Consider using OverflowStrategy.TRUNCATE or increase queryCacheSize.");
    }
}