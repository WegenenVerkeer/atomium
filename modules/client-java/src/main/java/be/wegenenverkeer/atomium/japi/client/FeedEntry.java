package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.atomium.format.Entry;

/**
 * An Entry in the feed, as returned by the {@code AtomiumClient}.
 *
 * <p>Provides access to both the Atom entry, and the page links of the page of the entry.</p>
 * Created by Karel Maesen, Geovise BVBA on 20/08/15.
 */
public class FeedEntry<E> {


    private final Entry<E> entry;
    private final String selfHref;

    //we pass in the complete feedwrapper so that it's easy to extend with other links if necessary
    protected FeedEntry(Entry<E> entry, CachedFeedPage<E> feed){
        this.entry = entry;
        this.selfHref = feed.getSelfHref();
    }

    /**
     * Returns the entry
     * @return the entry
     */
    public Entry<E> getEntry() {
        return entry;
    }

    /**
     * Returns the self-link of the page containing this entry
     * @return the self-link of the page containing this entry
     */
    public String getSelfHref() {
        return selfHref;
    }
}
