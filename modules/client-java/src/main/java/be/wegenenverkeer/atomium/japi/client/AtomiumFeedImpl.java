package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.atomium.format.Entry;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    public Flowable<FeedEntry<E>> from(FeedPosition feedPosition) {
        return new AtomiumFeedFetcher().fetchEntries(feedPosition, Optional.empty());
    }

    @Override
    public Flowable<FeedEntry<E>> fromNowOn() {
        AtomiumFeedFetcher feedFetcher = new AtomiumFeedFetcher();

        return feedFetcher.fetchHeadPage()
                .toFlowable()
                .flatMap(aPage -> feedFetcher.fetchEntries(FeedPositions.ofMostRecentEntry(aPage), Optional.empty()));
    }

    @Override
    public Flowable<FeedEntry<E>> fromBeginning() {
        AtomiumFeedFetcher feedFetcher = new AtomiumFeedFetcher();
        return feedFetcher.fetchHeadPage()
                .toFlowable()
                .flatMap(aPage -> feedFetcher.fetchEntries(FeedPositions.ofBeginning(aPage), Optional.empty()));
    }

    @Override
    public AtomiumFeed<E> withRetry(RetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
        return this;
    }

    private class AtomiumFeedFetcher {
        private int retryCount = 0;

        private Flowable<FeedEntry<E>> fetchEntries(FeedPosition feedPosition, Optional<String> eTag) {
            return fetchPage(feedPosition.getPageUrl(), eTag)
                    .map(page -> new ParsedFeedPage(page, feedPosition))
                    .toFlowable()
                    .flatMap(parsedPage -> Flowable.fromIterable(parsedPage.entries).concatWith(Flowable.just("")
                            .delay(pageFetcher.getPollingInterval().toMillis(), TimeUnit.MILLISECONDS)
                            .doOnNext(delay -> logger.debug("Waited {}ms to fetch more entries.", pageFetcher.getPollingInterval().toMillis()))
                            .flatMap(delay -> fetchEntries(parsedPage.getNextFeedPosition(), parsedPage.getEtag())))
                    );
        }

        private Single<CachedFeedPage<E>> fetchHeadPage() {
            return fetchPage(AtomiumFeed.FIRST_PAGE, Optional.empty());
        }

        private Single<CachedFeedPage<E>> fetchPage(String pageUrl, Optional<String> eTag) {
            return pageFetcher.fetch(pageUrl, eTag)
                    .retryWhen(throwableFlowable -> throwableFlowable
                            .map(throwable -> retryStrategy.apply(++this.retryCount, throwable)) // TODO handle error by catching exceptions and returning Single.error
                            .flatMap(delay -> Flowable.just("ignored").delay(delay.longValue(), TimeUnit.MILLISECONDS))
                    );
        }
    }

    private class ParsedFeedPage {
        private final CachedFeedPage<E> page;
        private final FeedPosition feedPosition;
        private List<FeedEntry<E>> entries;
        private FeedPosition nextFeedPosition;

        private ParsedFeedPage(CachedFeedPage<E> page, FeedPosition lastKnownPosition) {
            this.page = page;
            this.feedPosition = lastKnownPosition;

            this.parse();
        }

        public List<FeedEntry<E>> getEntries() {
            return entries;
        }

        public FeedPosition getFeedPosition() {
            return feedPosition;
        }

        public Optional<String> getEtag() {
            return page.getEtag();
        }

        public FeedPosition getNextFeedPosition() {
            return nextFeedPosition;
        }

        private ParsedFeedPage parse() {
            List<Entry<E>> entries = new ArrayList<>(page.getEntries());

            if (feedPosition.getEntryId().isPresent()) {
                if (pageHasEntry(page, feedPosition.getEntryId().get())) {
                    logger.debug("Page {} has an entry with ID {}, so we're only emitting items since that ID", feedPosition.getPageUrl(), feedPosition.getEntryId().get());
                    entries = omitEntriesBeforeEntryId(entries, feedPosition.getEntryId().get());
                } else {
                    logger.debug("Page {} does not have an entry with ID {}, so we're emitting every item", feedPosition.getPageUrl(), feedPosition.getEntryId().get());
                }
            }

            this.entries = entries.stream().map(entry -> new FeedEntry<>(entry, page)).collect(Collectors.toList());
            this.nextFeedPosition = FeedPositions.of(page.getPreviousHref().orElseGet(page::getSelfHref), page.getLastEntryId());

            return this;
        }

        private boolean pageHasEntry(CachedFeedPage<E> page, String entryId) {
            return page.getEntries().stream().anyMatch(entry -> entry.getId().equals(entryId));
        }

        private List<Entry<E>> omitEntriesBeforeEntryId(List<Entry<E>> entries, String entryId) {
            Collections.reverse(entries);
            List<Entry<E>> cleanedEntries = entries.stream().takeWhile(entry -> !entry.getId().equals(entryId)).collect(Collectors.toList());
            Collections.reverse(cleanedEntries);
            return cleanedEntries;
        }
    }
}
