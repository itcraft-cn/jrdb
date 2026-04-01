package cn.itcraft.jrdb.jdbc.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TruncateOverflowHandler implements OverflowHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TruncateOverflowHandler.class);

    @Override
    public boolean shouldContinue(int currentRow, int maxRows) {
        return currentRow < maxRows;
    }

    @Override
    public void onOverflow(int maxRows) {
        LOGGER.warn("Query result truncated due to memory limit. maxRows={}", maxRows);
    }
}