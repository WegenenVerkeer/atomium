package be.wegenenverkeer.atomium.api;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 *
 * @param <T> entry value type*
 */
public interface EventDao<T> extends EventWriter<T>, EventReader<T> {

}
