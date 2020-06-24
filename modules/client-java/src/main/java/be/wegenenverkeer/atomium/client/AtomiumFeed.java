package be.wegenenverkeer.atomium.client;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
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
        // recursion breaks backpressure, so we're using a generator:
        AtomicReference<CachedFeedPage<E>> previousPage = new AtomicReference<>(null);
        return Flowable.generate( // generate an infinite stream of zeroes (Flowable.range only goes to Integer.MAX_VALUE)
                () -> 0,
                (s, emitter) -> {
                    emitter.onNext(0);
                }
            ).concatMap(t -> { // fetch the page to pass to the feed position strategy
                    Single<CachedFeedPage<E>> page;
                    if (previousPage.get() != null) {
                        page = Single.just(previousPage.get());
                    } else {
                        page = fetchHeadPage();
                    }
                    return page.flatMap(eCachedFeedPage -> feedPositionStrategy.getNextFeedPosition(eCachedFeedPage) // get next feed position
                                    .flatMap(feedPosition -> fetchPage(feedPosition.pageUrl, eCachedFeedPage.etag) // fetch entries, and parse them
                                            .doOnSuccess(previousPage::set)
                                            .map(cachedFeedPage -> ParsedFeedPage.parse(cachedFeedPage, feedPosition))))
                            .toFlowable()
                            .flatMap(parsedPage -> Flowable.fromIterable(parsedPage.getEntries()));
                }, 1);
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
