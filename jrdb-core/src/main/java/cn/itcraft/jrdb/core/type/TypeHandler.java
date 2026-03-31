package cn.itcraft.jrdb.core.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 类型处理器接口，用于Java类型与JDBC类型之间的转换。
 *
 * <p>类型处理器是jrdb框架的核心组件之一，负责处理Java对象与数据库字段之间的双向转换。
 * 每种Java类型都有对应的类型处理器实现，用于：
 * <ul>
 *     <li>从{@link ResultSet}中读取数据并转换为Java对象</li>
 *     <li>将Java对象设置到{@link PreparedStatement}的参数中</li>
 * </ul>
 *
 * <p>框架内置了常见Java类型的处理器实现，如：
 * <ul>
 *     <li>基本类型：int、long、double、boolean等</li>
 *     <li>包装类型：Integer、Long、Double、Boolean等</li>
 *     <li>字符串类型：String</li>
 *     <li>日期时间类型：Date、LocalDateTime、LocalDate、LocalTime等</li>
 *     <li>字节数组：byte[]</li>
 * </ul>
 *
 * <p>用户也可以通过实现此接口来自定义类型处理器，以支持特殊的类型转换需求。
 *
 * <p><b>使用示例：</b>
 * <pre>{@code
 * public class MyEnumTypeHandler implements TypeHandler<MyEnum> {
 *     @Override
 *     public MyEnum getResult(ResultSet rs, String columnName) throws SQLException {
 *         String value = rs.getString(columnName);
 *         return value == null ? null : MyEnum.valueOf(value);
 *     }
 *
 *     @Override
 *     public MyEnum getResult(ResultSet rs, int columnIndex) throws SQLException {
 *         String value = rs.getString(columnIndex);
 *         return value == null ? null : MyEnum.valueOf(value);
 *     }
 *
 *     @Override
 *     public void setParameter(PreparedStatement ps, int i, MyEnum parameter) throws SQLException {
 *         ps.setString(i, parameter == null ? null : parameter.name());
 *     }
 *
 *     @Override
 *     public Class<MyEnum> getJavaType() {
 *         return MyEnum.class;
 *     }
 * }
 * }</pre>
 *
 * @param <T> 此处理器处理的Java类型
 * @author jrdb
 * @see ResultSet
 * @see PreparedStatement
 * @see cn.itcraft.jrdb.core.type.TypeHandlerRegistry
 */
public interface TypeHandler<T> {

    /**
     * 根据列名从{@link ResultSet}中获取结果值。
     *
     * <p>此方法用于从数据库查询结果集中读取指定列的值，并将其转换为对应的Java类型对象。
     * 如果列值为SQL NULL，则返回null。
     *
     * <p><b>注意：</b>列名区分大小写，应与SQL查询中的列名或别名完全匹配。
     *
     * @param rs 数据库结果集，不能为null
     * @param columnName 列名，区分大小写，不能为null或空字符串
     * @return 转换后的Java对象，如果列值为SQL NULL则返回null
     * @throws SQLException 如果发生数据库访问错误或列名不存在
     */
    T getResult(ResultSet rs, String columnName) throws SQLException;

    /**
     * 根据列索引从{@link ResultSet}中获取结果值。
     *
     * <p>此方法用于从数据库查询结果集中读取指定位置列的值，并将其转换为对应的Java类型对象。
     * 如果列值为SQL NULL，则返回null。
     *
     * <p><b>注意：</b>列索引从1开始，按照SQL查询中列的出现顺序编号。
     *
     * @param rs 数据库结果集，不能为null
     * @param columnIndex 列索引，从1开始
     * @return 转换后的Java对象，如果列值为SQL NULL则返回null
     * @throws SQLException 如果发生数据库访问错误或列索引超出范围
     */
    T getResult(ResultSet rs, int columnIndex) throws SQLException;

    /**
     * 设置{@link PreparedStatement}的参数值。
     *
     * <p>此方法用于将Java对象设置到预编译SQL语句的指定参数位置。
     * 框架在执行SQL语句前会调用此方法来填充参数值。
     *
     * <p>如果参数值为null，实现类应调用PreparedStatement的setNull方法或设置对应类型的NULL值。
     *
     * @param ps 预编译语句对象，不能为null
     * @param i 参数索引，从1开始
     * @param parameter 要设置的参数值，可以为null
     * @throws SQLException 如果发生数据库访问错误
     */
    void setParameter(PreparedStatement ps, int i, T parameter) throws SQLException;

    /**
     * 获取此类型处理器处理的Java类型。
     *
     * <p>此方法返回类型参数T的具体类型，用于框架内部的类型匹配和注册。
     * 当框架需要为某个Java类型查找对应的类型处理器时，会调用此方法进行匹配。
     *
     * @return 此处理器处理的Java类型的Class对象，永不为null
     */
    Class<T> getJavaType();
}