package be.wegenenverkeer.atomium.japi.client;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
        return fetchEntries(feedPosition, Optional.empty());
    }

    @Override
    public Flowable<FeedEntry<E>> fromNowOn() {
        return fetchHeadPage()
                .toFlowable()
                .flatMap(aPage -> fetchEntries(FeedPositions.ofMostRecentEntry(aPage), Optional.empty()));
    }

    @Override
    public Flowable<FeedEntry<E>> fromBeginning() {
        return fetchHeadPage()
                .toFlowable()
                .flatMap(aPage -> fetchEntries(FeedPositions.ofOldest(aPage), Optional.empty()));
    }

    @Override
    public AtomiumFeed<E> withRetry(RetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
        return this;
    }

    private int retryCount = 0;

    private Flowable<FeedEntry<E>> fetchEntries(FeedPosition feedPosition, Optional<String> eTag) {
        return fetchPage(feedPosition.getPageUrl(), eTag)
                .map(page -> ParsedFeedPage.parse(page, feedPosition))
                .toFlowable()
                .flatMap(parsedPage -> Flowable.fromIterable(parsedPage.getEntries()).concatWith(Flowable.just("")
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
