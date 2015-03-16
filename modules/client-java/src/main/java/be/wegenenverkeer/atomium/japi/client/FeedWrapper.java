package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.atomium.japi.format.Entry;
import be.wegenenverkeer.atomium.japi.format.Feed;
import be.wegenenverkeer.atomium.japi.format.Link;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Created by Karel Maesen, Geovise BVBA on 24/03/15.
 */
class FeedWrapper<T> {

    final private List<Link> links;
    final private List<Entry<T>> entries;
    final Optional<String> etag;


    public FeedWrapper(Feed<T> feed, Optional<String> etag) {
        this(feed.getLinks(), feed.getEntries(), etag);
        Collections.reverse(entries);
    }

    public FeedWrapper(List<Link> links, List<Entry<T>> entries, Optional<String> etag) {
        this.links = links;
        this.entries = entries;
        this.etag = etag;
    }

    public Optional<String> getEtag() {
        return etag;
    }

    public boolean isEmpty(){
        return false;
    }

    public List<Link> getLinks() {
        return this.links;
    }

    public Optional<String> getPreviousHref() {
        return getLinkHref("previous");
    }

    public Optional<String> getNextHref() {
        return getLinkHref("next");
    }

    public Optional<String> getSelfHref() {
        return getLinkHref("self");
    }

    public Optional<String> getLinkHref(String attr){
        for (Link l : getLinks()) {
            if (l.getRel().equals(attr)) {
                return Optional.of(l.getHref());
            }
        }
        return Optional.empty();
    }

    public String getLastEntryId() {
        if (this.entries.size() >= 1)
            return this.entries.get(this.entries.size() - 1).getId();
        return "";
    }

    /**
     * Returns the entries in the feedpage in order oldest to newest.
     * @return
     */
    public List<Entry<T>> getEntries() {
        return this.entries;
    }



}
