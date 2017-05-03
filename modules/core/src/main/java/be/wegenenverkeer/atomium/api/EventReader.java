package be.wegenenverkeer.atomium.api;

import java.util.List;

/**
 * Created by Karel Maesen, Geovise BVBA on 14/12/16.
 */
public interface EventReader<T> {

    List<Event<T>> getEvents(long startNum, long size);

    Long totalNumberOfEvents();

}
