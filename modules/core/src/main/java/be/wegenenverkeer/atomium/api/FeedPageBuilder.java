package be.wegenenverkeer.atomium.api;

import be.wegenenverkeer.atomium.format.Entry;
import be.wegenenverkeer.atomium.format.Link;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Karel Maesen, Geovise BVBA on 05/12/16.
 */
public class FeedPageBuilder<T> {

    final private FeedMetadata meta;
    final private long page;
    private List<Event<T>> events;
    private OffsetDateTime updated;
    private List<Link> links;
    private boolean hasPrevious;

    public FeedPageBuilder(FeedMetadata meta, long pageNum) {
        this.page = pageNum;
        this.meta = meta;
    }

    /**
     * Pass the events for the page, orderd from most recent to least recent
     *
     * <p>For purposes of determining links, there are usually more events passsed. We take pageSize of the oldest</p>
     *
     * @param events a list of events
     * @return this
     */
    public FeedPageBuilder<T> setEvents(List<Event<T>> events) {
        this.events = events;
        Collections.reverse(this.events);
        checkForPreviousLink();
        selectOldestForPage();
        calcUpdated();
        calcLinks();
        return this;
    }

    private void selectOldestForPage() {
        long leastIndex = Math.max(0, events.size() - this.meta.getPageSize());
        events = events.subList((int)leastIndex, this.events.size());
    }

    private void calcUpdated() {
        updated = events.isEmpty() ? OffsetDateTime.now() : events.get(0).getUpdated();
    }

    private void checkForPreviousLink() {
        hasPrevious = (events.size() > this.meta.getPageSize());
    }


    private void calcLinks() {
        links = new ArrayList<>();
        String suffix = "/" + this.meta.getPageSize();
        links.add(new Link(Link.SELF, "/" + this.page + suffix));
        links.add(new Link(Link.LAST, "/0" + suffix));
        if (page > 0) {
            links.add(new Link(Link.NEXT, "/" + (page - 1) + suffix));
        }
        if (hasPrevious) {
            links.add(new Link(Link.PREVIOUS, "/" + (page + 1) + suffix));
        }

    }

    public FeedPage<T> build() {
        List<Entry<T>> pageEntries = events.stream().map(Event::toAtomEntry).collect(Collectors.toList());
        return new FeedPage<>(
                this.meta.getFeedName(),
                this.meta.getFeedUrl(),
                this.meta.getFeedName(),
                this.meta.getFeedGenerator(),
                updated,
                links,
                pageEntries
        );
    }


}
