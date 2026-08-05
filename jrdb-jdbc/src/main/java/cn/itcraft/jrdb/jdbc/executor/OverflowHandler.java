package cn.itcraft.jrdb.jdbc.executor;

/**
 * 溢出处理器接口，定义查询结果超出限制时的处理策略。
 *
 * @author Helly Guo
 * @since 1.2.0
 */
public interface OverflowHandler {

    /**
     * 判断是否继续处理。
     *
     * @param currentRow 当前行号
     * @param maxRows 最大行数限制
     * @return 是否继续
     */
    boolean shouldContinue(int currentRow, int maxRows);

    /**
     * 溢出时的回调处理。
     *
     * @param maxRows 最大行数限制
     */
    void onOverflow(int maxRows);
}
