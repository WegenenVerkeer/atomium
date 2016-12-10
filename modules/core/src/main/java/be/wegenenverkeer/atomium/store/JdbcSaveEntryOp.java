package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Entry;

import java.sql.SQLException;

/**
 * Created by Karel Maesen, Geovise BVBA on 10/12/16.
 */
public interface JdbcSaveEntryOp<T> extends JdbcOp<Boolean> {

    public void set(Entry<T> entry) throws SQLException;

}
