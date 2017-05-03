package be.wegenenverkeer.atomium.api;

import java.util.List;

/**
 * Writes {@code Event}s to a store
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 14/12/16.
 */
public interface EventWriter<T> {

    boolean push(List<Event<T>> events);

    boolean push(Event<T> event);

}
