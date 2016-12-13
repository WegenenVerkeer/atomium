package be.wegenenverkeer.atomium.store;

import java.sql.SQLException;

/**
 * Created by Karel Maesen, Geovise BVBA on 10/12/16.
 */
public interface JdbcOp<R> extends AutoCloseable {

    public R execute() throws SQLException;

    public void close();

}
