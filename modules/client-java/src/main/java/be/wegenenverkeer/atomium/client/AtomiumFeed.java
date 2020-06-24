package be.wegenenverkeer.atomium.client;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public Flowable<FeedEntry<E>> fetchEntries2(FeedPositionStrategy feedPositionStrategy) {
        AtomicReference<CachedFeedPage<E>> previousPage = new AtomicReference<>(null);
        Flowable<Object> infiniteFlowable = Flowable.generate(
                () -> 0,
                (s, emitter) -> {
                    emitter.onNext(0);
                }
        );
        return infiniteFlowable
                .concatMap(t -> {
                    Single<CachedFeedPage<E>> page;
                    if (previousPage.get() != null) {
                        page = Single.just(previousPage.get());
                    } else {
                        page = fetchHeadPage();
                    }
                    return page.flatMap(feedPositionStrategy::getNextFeedPosition)
                            .flatMap(feedPosition -> fetchPage(feedPosition.pageUrl, Optional.empty())
                                    .doOnSuccess(previousPage::set)
                                    .map(cachedFeedPage -> ParsedFeedPage.parse(cachedFeedPage, feedPosition)))
                            .toFlowable()
                            .flatMap(parsedPage -> Flowable.fromIterable(parsedPage.getEntries()));
                }, 1);
    }

    public Flowable<FeedEntry<E>> fetchEntries(FeedPositionStrategy feedPositionStrategy) {
        return fetchEntries2(feedPositionStrategy);
//        return fetchHeadPage()
//                .toFlowable()
//                .flatMap(headPage -> this.fetchEntries(headPage, feedPositionStrategy, Optional.empty()));
    }

    public Flowable<FeedEntry<E>> fetchEntries(CachedFeedPage<E> currentPage, FeedPositionStrategy feedPositionStrategy, Optional<String> eTag) {
        return feedPositionStrategy.getNextFeedPosition(currentPage)
                .flatMap(feedPosition -> fetchPage(feedPosition.getPageUrl(), eTag)
                        .map(page -> ParsedFeedPage.parse(page, feedPosition))
                )
                .toFlowable()
                .flatMap(parsedPage -> {
                    Flowable<FeedEntry<E>> next = fetchEntries(parsedPage.getPage(), feedPositionStrategy, eTag);
                    return Flowable.fromIterable(parsedPage.getEntries()).concatWith(next);
                }, false, 1, 1);
    }

    private Single<CachedFeedPage<E>> fetchNextPage(CachedFeedPage<E> currentPage, FeedPositionStrategy feedPositionStrategy, Optional<String> eTag) {
        return feedPositionStrategy.getNextFeedPosition(currentPage)
                .flatMap(feedPosition -> fetchPage(feedPosition.getPageUrl(), eTag));
    }

    private Single<CachedFeedPage<E>> fetchHeadPage() {
        return fetchPage(AtomiumFeed.FIRST_PAGE, Optional.empty());
    }

    private Single<CachedFeedPage<E>> fetchPage(String pageUrl, Optional<String> eTag) {
        return pageFetcher.fetch(pageUrl, eTag)
                .retryWhen(throwableFlowable -> throwableFlowable
                        .map(throwable -> pageFetcher.getRetryStrategy().apply(++this.retryCount, throwable)) // TODO handle error by catching exceptions and returning Single.error
                        .flatMap(delay -> Flowable.just("ignored").delay(delay.longValue(), TimeUnit.MILLISECONDS))
                )
                .doAfterSuccess(page -> this.retryCount = 0);
    }
}
