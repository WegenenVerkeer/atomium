package be.wegenenverkeer.atomium.api;

/**
 * Created by Karel Maesen, Geovise BVBA on 15/11/16.
 */
public interface FeedPageCodec<T,O> extends Codec<FeedPage<T>, O> {

    String getMimeType();

    O encode(FeedPage<T> page);

    FeedPage<T> decode(O encoded);

}
