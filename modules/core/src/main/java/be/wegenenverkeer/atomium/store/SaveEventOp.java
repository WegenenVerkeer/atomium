package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Event;

import java.sql.SQLException;

/**
 * Created by Karel Maesen, Geovise BVBA on 10/12/16.
 */
public interface SaveEventOp<T> extends JdbcOp<Boolean> {

    public void set(Event<T> event) throws SQLException;

}
