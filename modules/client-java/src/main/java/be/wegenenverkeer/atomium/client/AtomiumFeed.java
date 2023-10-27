package be.wegenenverkeer.atomium.client;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AtomiumFeed<E> {
    private final static Logger logger = LoggerFactory.getLogger(AtomiumFeed.class);

    private static final String FIRST_PAGE = "";
    private final PageFetcher<E> pageFetcher;

    public AtomiumFeed(PageFetcher<E> pageFetcher) {
        this.pageFetcher = pageFetcher;
    }

    private int retryCount = 0;

    public Flowable<FeedEntry<E>> fetchEntries(FeedPositionStrategy feedPositionStrategy) {
        AtomicReference<CachedFeedPage<E>> previousPage = new AtomicReference<>(null);

        return infiniteZeroes() // recursion breaks backpressure, so we're using a generator of infinite zeroes
                .concatMap(t -> fetchEntries(feedPositionStrategy, previousPage), 1);
    }

    private Publisher<? extends FeedEntry<E>> fetchEntries(FeedPositionStrategy feedPositionStrategy, AtomicReference<CachedFeedPage<E>> previousPageRef) {
        return getPreviousPage(previousPageRef)
                .flatMap(previousPage -> feedPositionStrategy.getNextFeedPosition(previousPage) // get next feed position
                        .flatMap(feedPosition -> fetchPage(feedPosition, previousPage) // fetch entries
                                .doOnSuccess(currentPage -> updatePreviousPageRef(previousPageRef, currentPage))
                                .map(currentPage -> ParsedFeedPage.parse(currentPage, feedPosition)))) // parse entries
                .toFlowable()
                .flatMap(parsedPage -> Flowable.fromIterable(parsedPage.getEntries()));
    }

    private void updatePreviousPageRef(AtomicReference<CachedFeedPage<E>> previousPageRef, CachedFeedPage<E> currentPage) {
        if ((currentPage instanceof EmptyCachedFeedPage)) {
            return; // keep position
        }

        previousPageRef.set(currentPage); // update ref so we're processing the correct page later on
    }

    private Single<CachedFeedPage<E>> getPreviousPage(AtomicReference<CachedFeedPage<E>> previousPage) {
        if (previousPage.get() != null) {
            logger.debug("Next fetch: url '{}' and eTag '{}'", previousPage.get().getSelfHref(), previousPage.get().etag);
            return Single.just(previousPage.get());
        } else {
            logger.debug("First fetch, no eTag");
            return fetchHeadPage().map(feedPage -> new CachedFeedPage<>(feedPage.getLinks(), feedPage.getEntries(), Optional.empty()));
        }
    }

    // generate an infinite stream of zeroes (Flowable.range only goes to Integer.MAX_VALUE)
    private Flowable<Integer> infiniteZeroes() {
        return Flowable.generate(
                () -> 0,
                (s, emitter) -> {
                    emitter.onNext(0);
                }
        );
    }

    public Single<CachedFeedPage<E>> fetchHeadPage() {
        return fetchPage(AtomiumFeed.FIRST_PAGE, Optional.empty());
    }

    Single<CachedFeedPage<E>> fetchPage(FeedPosition feedPosition, CachedFeedPage<E> previousPage) {
        if (feedPosition.getPageUrl().equals(previousPage.getSelfHref())) {
            return fetchPage(feedPosition.pageUrl, previousPage.etag);
        } else {
            return fetchPage(feedPosition.pageUrl, Optional.empty());
        }
    }

    public Single<CachedFeedPage<E>> fetchPage(String pageUrl, Optional<String> eTag) {
        return pageFetcher.fetch(pageUrl, eTag)
                .retryWhen(throwableFlowable -> throwableFlowable
                        .doOnNext(throwable -> logger.error("Problem fetching page '{}' with eTag '{}'", pageUrl, eTag, throwable))
                        .flatMap(this::applyRetryStrategy)
                        .flatMap(delay -> Flowable.just(1).delay(delay, TimeUnit.MILLISECONDS))
                )
                .doAfterSuccess(page -> {
                    if (this.retryCount > 0) {
                        pageFetcher.getRecoveryStrategy().apply(this.retryCount);
                    }
                    this.retryCount = 0;
                });
    }

    private Flowable<Long> applyRetryStrategy(Throwable throwable) {
        try {
            return Flowable.just(pageFetcher.getRetryStrategy().apply(++this.retryCount, throwable));
        } catch (Throwable e) {
            return Flowable.error(e);
        }
    }
}
