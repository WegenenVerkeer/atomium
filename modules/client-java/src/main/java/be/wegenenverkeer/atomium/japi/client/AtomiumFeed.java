package be.wegenenverkeer.atomium.japi.client;

import io.reactivex.rxjava3.core.Flowable;

public interface AtomiumFeed<E> {
    String FIRST_PAGE = "/";

    /**
     * Creates a "cold" {@link Flowable} that, when subscribed to, emits all entries in the feed
     * starting from the oldest entry immediately after the specified entry.
     * <p>When subscribed to, the observable will:</p>
     * <ul>
     * <li>retrieve the specified feed page</li>
     * <li>emit all entries more recent that the specified feed entry</li>
     * <li>follow iteratively the 'previous'-links and emits all entries on the linked-to pages until it
     * arrives at the head of the feed (identified by not having a 'previous'-link)</li>
     * <li>poll the feed at the specified interval (using conditional GETs) and emit all entries not yet seen</li>
     * </ul>
     * <p>The worker will exit only on an error condition, or on unsubscribe.</p>
     * <p><em>Important:</em> a new and independent worker is created for each subscriber.</p>
     *
     * @param entryId the entry-id of an entry on the specified page
     * @param pageUrl the url (absolute, or relative to the feed's base url) of the feed-page, containing the entry
     *                identified with the entryId argument
     * @return an Observable emitting all entries since the specified entry
     */
    default Flowable<FeedEntry<E>> from(final String entryId, final String pageUrl) {
        return from(FeedPositions.of(pageUrl, entryId));
    }

    /**
     * Creates a "cold" {@link Flowable} that, when subscribed to, emits all entries in the feed
     * starting from the oldest entry immediately after the specified entry.
     * <p>When subscribed to, the observable will:</p>
     * <ul>
     * <li>retrieve the specified feed page</li>
     * <li>emit all entries more recent that the specified feed entry</li>
     * <li>follow iteratively the 'previous'-links and emits all entries on the linked-to pages until it
     * arrives at the head of the feed (identified by not having a 'previous'-link)</li>
     * <li>poll the feed at the specified interval (using conditional GETs) and emit all entries not yet seen</li>
     * </ul>
     * <p>The worker will exit only on an error condition, or on unsubscribe.</p>
     * <p><em>Important:</em> a new and independent worker is created for each subscriber.</p>
     *
     * @param feedPosition feedPosition
     * @return an Observable emitting all entries since the specified entry
     */
    Flowable<FeedEntry<E>> from(FeedPosition feedPosition);

    /**
     * Creates a "cold" {@link Flowable} that, when subscribed to, emits all entries on the feed
     * starting from those then on the head of the feed.
     * <p>The behavior is analogous to the method {@code observeFrom()} but starting from the head page</p>
     *
     * @return a "cold" {@link Flowable}
     */
    Flowable<FeedEntry<E>> fromNowOn();

    /**
     * Creates a "cold" {@link Flowable} that, when subscribed to, emits all entries on the feed starting from the begnning.
     * <p>Starting from the beginning means going to the 'last' page of the feed, and the bottom entry on that page, and working back
     * to the present.</p>
     *
     * @return a "cold" {@link Flowable}
     */
    Flowable<FeedEntry<E>> fromBeginning();

    AtomiumFeed<E> withRetry(RetryStrategy retryStrategy);
}
