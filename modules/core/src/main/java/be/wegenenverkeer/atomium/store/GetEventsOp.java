package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Event;

import java.util.List;

/**
 * Created by Karel Maesen, Geovise BVBA on 10/12/16.
 */
public interface GetEventsOp<T> extends  JdbcOp<List<Event<T>>> {


    void setRange(long startNum, long size);

}
