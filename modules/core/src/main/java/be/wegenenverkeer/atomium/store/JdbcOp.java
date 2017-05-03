package be.wegenenverkeer.atomium.store;

import java.sql.SQLException;

/**
 * Created by Karel Maesen, Geovise BVBA on 10/12/16.
 */
public interface JdbcOp<R> extends AutoCloseable {

    public R execute() throws SQLException;

    default void close(){
        //when there is no resource field, do nothing
    }

    default void close(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable t) {
                //do nothing
            }
        }
    }


}
