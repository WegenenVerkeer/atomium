package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Entry;

import java.util.List;

/**
 * Created by Karel Maesen, Geovise BVBA on 10/12/16.
 */
public interface JdbcGetEntriesOp<T> extends  JdbcOp<List<Entry<T>>> {


    void setRange(long startNum, long size);

}
