package be.wegenenverkeer.atomium.api;

/**
 * Created by Karel Maesen, Geovise BVBA on 15/11/16.
 */
public interface FeedPageCodec<T,O> {


    public O encode(FeedPage<T> page);

    public FeedPage<T> decode(O encoded);

}
