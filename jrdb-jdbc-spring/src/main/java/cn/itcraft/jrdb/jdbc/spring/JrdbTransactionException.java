package cn.itcraft.jrdb.jdbc.spring;

/**
 * jrdb事务异常。
 *
 * <p>在事务执行过程中发生异常时抛出，包含原始异常信息。
 */
public class JrdbTransactionException extends RuntimeException {

    /**
     * 创建事务异常。
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public JrdbTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 创建事务异常（无原始异常）。
     *
     * @param message 异常消息
     */
    public JrdbTransactionException(String message) {
        super(message);
    }
}