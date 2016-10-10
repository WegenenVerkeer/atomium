package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.atomium.format.Entry;
import be.wegenenverkeer.atomium.format.Feed;
import be.wegenenverkeer.atomium.format.Link;

import java.util.Collections;
import java.util.List;
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
        return getEntries().isEmpty();
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

    /**
     * Returns the (mandatory) 'self' link of the feed page
     * @return the rel='self' self link
     * @throws IllegalStateException if the 'self' link is not present
     */
    public String getSelfHref() {
        return getMandatoryLink("self");
    }

    /**
     * Returns the (mandatory) 'last' link of the feed page
     * @return the rel='last' link
     * @throws IllegalStateException if the 'last' link is not present
     */
    public String getLastHref() {
        return getMandatoryLink("last");
    }

    private String getMandatoryLink(String rel) {
        return getLinkHref(rel).orElseThrow(
                //it is a requirement for Atomium that there is always a self link
                () -> new IllegalStateException("No self link found on feed")
        );
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
     * @return list of entries in the feedpage in order oldest to newest.
     */
    public List<Entry<T>> getEntries() {
        return this.entries;
    }



}
