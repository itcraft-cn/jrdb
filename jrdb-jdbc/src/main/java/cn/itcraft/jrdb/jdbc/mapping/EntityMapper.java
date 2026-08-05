package cn.itcraft.jrdb.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 实体映射器接口，用于Java实体对象与数据库行之间的双向映射。
 *
 * <p>实现类提供Java实体对象与数据库结果集之间的映射逻辑，
 * 通常通过ASM字节码动态生成实现类以获得最佳性能。
 *
 * @param <T> 实体类型
 * @author Helly Guo
 * @since 1.0.0
 */
public interface EntityMapper<T> {

    /**
     * 将ResultSet当前行映射为实体对象。
     *
     * @param rs ResultSet，位于待映射的行
     * @return 映射后的实体对象
     * @throws SQLException 数据库访问异常
     */
    T mapFromResultSet(ResultSet rs) throws SQLException;

    /**
     * 将实体对象映射为数据库列值数组。
     *
     * @param entity 待映射的实体
     * @return 列值数组，按字段定义顺序
     */
    Object[] mapToRow(T entity);

    /**
     * 获取映射的字段名称数组。
     *
     * @return 字段名称数组
     */
    String[] getFieldNames();
}
