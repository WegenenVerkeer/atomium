package be.wegenenverkeer.atomium.client;

import be.wegenenverkeer.atomium.api.FeedPage;
import be.wegenenverkeer.atomium.format.Link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Created by Karel Maesen, Geovise BVBA on 24/03/15.
 */
public class EmptyCachedFeedPage<T> extends CachedFeedPage<T> {

    public EmptyCachedFeedPage(String pageUrl, Optional<String> etag) {
        super(createLinks(pageUrl), Collections.emptyList(), etag);
    }

    public boolean isEmpty() {
        return true;
    }

    private static List<Link> createLinks(String pageUrl) {
        return Collections.singletonList(new Link(Link.SELF, pageUrl));
    }
}
