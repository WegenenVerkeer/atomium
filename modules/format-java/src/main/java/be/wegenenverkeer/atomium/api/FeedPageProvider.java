package be.wegenenverkeer.atomium.api;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
public interface FeedPageProvider<E> {


    FeedPage getFeedPage(FeedPageReference ref);

}
