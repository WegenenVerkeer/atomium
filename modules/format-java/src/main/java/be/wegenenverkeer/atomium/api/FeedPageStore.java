package be.wegenenverkeer.atomium.api;

import java.util.List;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
public interface FeedPageStore<E> {

    void add(List<E> entries);

    void add(E... entries);

    List<E> getEntries(int startNum, int size);

}
