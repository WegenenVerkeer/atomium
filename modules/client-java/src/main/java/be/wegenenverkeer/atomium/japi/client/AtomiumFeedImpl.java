package be.wegenenverkeer.atomium.japi.client;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.reactivex.rxjava3.core.Flowable.fromIterable;

class AtomiumFeedImpl<E> implements AtomiumFeed<E> {
    private final static Logger logger = LoggerFactory.getLogger(AtomiumFeedImpl.class);
    private final PageFetcher<E> pageFetcher;

    private RetryStrategy retryStrategy = (count, exception) -> {
        throw new FeedFetchException("Problem fetching page", exception);
    };

    AtomiumFeedImpl(PageFetcher<E> pageFetcher) {
        this.pageFetcher = pageFetcher;
    }

    @Override
    public Flowable<FeedEntry<E>> from(final String entryId, final String pageUrl) {
        return new AtomiumFeedFetcher(entryId).fetchEntries(pageUrl, Optional.empty());
    }

    @Override
    public Flowable<FeedEntry<E>> fromNowOn() {
        return new AtomiumFeedFetcher().fetchEntries(AtomiumFeed.FIRST_PAGE, Optional.empty());
    }

    @Override
    public Flowable<FeedEntry<E>> fromBeginning() {
        AtomiumFeedFetcher feedFetcher = new AtomiumFeedFetcher();
        return feedFetcher.fetchFirstPage()
                .toFlowable()
                .flatMap(lastPage -> feedFetcher.fetchEntries(lastPage.getLastHref(), Optional.empty()));
    }

    @Override
    public AtomiumFeed<E> withRetry(RetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
        return this;
    }

    private class AtomiumFeedFetcher {
        private final Optional<String> startEntryId;
        private boolean pruneEntries = false;
        private int retryCount = 0;

        public AtomiumFeedFetcher() {
            this(null);
        }

        public AtomiumFeedFetcher(String startEntryId) {
            this.startEntryId = Optional.ofNullable(startEntryId);
            this.startEntryId.ifPresent(s -> pruneEntries = true);
        }

        private Flowable<FeedEntry<E>> fetchEntries(String pageUrl, Optional<String> eTag) {
            return fetchPage(pageUrl, eTag)
                    .toFlowable()
                    .flatMap(page -> this.parseEntries(page).concatWith(Flowable.just("")
                            .delay(pageFetcher.getPollingInterval().toMillis(), TimeUnit.MILLISECONDS)
                            .doOnNext(delay -> logger.debug("Waited {}ms to fetch more entries.", pageFetcher.getPollingInterval().toMillis()))
                            .flatMap(delay -> fetchEntries(previousOrSelfHref(page), page.getEtag())))
                    )
                    .map(this::pruneEntries)
                    .filter(Optional::isPresent)
                    .map(Optional::get);
        }

        private Single<CachedFeedPage<E>> fetchFirstPage() {
            return fetchPage(AtomiumFeed.FIRST_PAGE, Optional.empty());
        }

        private Single<CachedFeedPage<E>> fetchPage(String pageUrl, Optional<String> eTag) {
            return pageFetcher.fetch(pageUrl, eTag)
                    .retryWhen(throwableFlowable -> throwableFlowable
                            .map(throwable -> retryStrategy.apply(++this.retryCount, throwable)) // TODO handle error by catching exceptions and returning Single.error
                            .flatMap(delay -> Flowable.just("ignored").delay(delay.longValue(), TimeUnit.MILLISECONDS))
                    );
        }

        private Flowable<FeedEntry<E>> parseEntries(CachedFeedPage<E> page) {
            return fromIterable(page.getEntries())
                    .map(entry -> new FeedEntry<>(entry, page));
        }

        private String previousOrSelfHref(CachedFeedPage<E> page) {
            return page.getPreviousHref().orElseGet(page::getSelfHref);
        }

        private Optional<FeedEntry<E>> pruneEntries(FeedEntry<E> feedEntry) {
            if (startEntryId.isPresent() && pruneEntries) {
                if (feedEntry.getEntry().getId().equals(startEntryId.get())) {
                    pruneEntries = false;
                    return Optional.of(feedEntry);
                } else {
                    return Optional.empty();
                }
            } else {
                return Optional.of(feedEntry);
            }
        }
    }
}
