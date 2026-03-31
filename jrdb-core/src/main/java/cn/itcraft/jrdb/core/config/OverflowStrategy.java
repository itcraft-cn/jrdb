package cn.itcraft.jrdb.core.config;

/**
 * 溢出策略枚举，定义当数据超出预期容量时的处理方式。
 *
 * @author jrdb
 */
public enum OverflowStrategy {

    /**
     * 错误策略，当发生溢出时抛出异常。
     */
    ERROR,

    /**
     * 截断策略，当发生溢出时截断多余数据。
     */
    TRUNCATE
}