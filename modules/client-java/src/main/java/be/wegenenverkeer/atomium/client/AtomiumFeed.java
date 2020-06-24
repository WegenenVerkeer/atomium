package be.wegenenverkeer.atomium.client;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AtomiumFeed<E> {
    private static String FIRST_PAGE = "/";
    private final static Logger logger = LoggerFactory.getLogger(AtomiumFeed.class);
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
                        .flatMap(feedPosition -> fetchPage(feedPosition.pageUrl, previousPage.etag) // fetch entries
                                .doOnSuccess(previousPageRef::set) // update ref so we're processing the correct page later on
                                .map(cachedFeedPage -> ParsedFeedPage.parse(cachedFeedPage, feedPosition)))) // parse entries
                .toFlowable()
                .flatMap(parsedPage -> Flowable.fromIterable(parsedPage.getEntries()));
    }

    private Single<CachedFeedPage<E>> getPreviousPage(AtomicReference<CachedFeedPage<E>> previousPage) {
        if (previousPage.get() != null) {
            return Single.just(previousPage.get());
        } else {
            return fetchHeadPage();
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

    private Single<CachedFeedPage<E>> fetchHeadPage() {
        return fetchPage(AtomiumFeed.FIRST_PAGE, Optional.empty());
    }

    private Single<CachedFeedPage<E>> fetchPage(String pageUrl, Optional<String> eTag) {
        return pageFetcher.fetch(pageUrl, eTag)
                .retryWhen(throwableFlowable -> throwableFlowable
                        .flatMap(this::applyRetryStrategy)
                        .flatMap(delay -> Flowable.just("ignored").delay(delay.longValue(), TimeUnit.MILLISECONDS))
                )
                .doAfterSuccess(page -> this.retryCount = 0);
    }

    private Flowable<Long> applyRetryStrategy(Throwable throwable) {
        try {
            return Flowable.just(pageFetcher.getRetryStrategy().apply(++this.retryCount, throwable));
        } catch(Throwable e) {
            return Flowable.error(e);
        }
    }
}
