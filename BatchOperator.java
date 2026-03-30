package demo.jdbc.batch;

import java.util.List;

/**
 * @author Helly Guo
 * <p>
 * Created on 3/3/22 6:43 PM
 */
public interface BatchOperator {
    <T> void batchInsert(String id, List<T> list);

    <T> int batchUpdate(String id, List<T> list);
}
