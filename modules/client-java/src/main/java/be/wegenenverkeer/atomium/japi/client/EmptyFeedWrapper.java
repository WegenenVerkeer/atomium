package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.atomium.format.Link;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by Karel Maesen, Geovise BVBA on 24/03/15.
 */
public class EmptyFeedWrapper<T> extends FeedWrapper<T> {

    public static List<Link> emptyLinks = new ArrayList<>(0);

    public EmptyFeedWrapper(Optional<String> etag) {
        super(emptyLinks, new ArrayList<>(0), etag);
    }

    public boolean isEmpty() {
        return true;
    }

}
