package be.wegenenverkeer.atomium.client;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AtomiumFeed<E> {
    private static String FIRST_PAGE = "/";

    private final static Logger logger = LoggerFactory.getLogger(AtomiumFeed.class);
    private final PageFetcher<E> pageFetcher;

    public AtomiumFeed(PageFetcher<E> pageFetcher) {
        this.pageFetcher = pageFetcher;
    }

    private int retryCount = 0;

    public Flowable<FeedEntry<E>> fetchEntries(FeedPositionStrategy feedPositionStrategy) {
        return fetchHeadPage()
                .toFlowable()
                .flatMap(headPage -> this.fetchEntries(headPage, feedPositionStrategy, Optional.empty()));
    }

    public Flowable<FeedEntry<E>> fetchEntries(CachedFeedPage<E> currentPage, FeedPositionStrategy feedPositionStrategy, Optional<String> eTag) {
        return feedPositionStrategy.getNextFeedPosition(currentPage)
                .doOnEvent((feedPosition, throwable) -> logger.debug("Next feed position = {} (page {})", feedPosition.getEntryId(), feedPosition.getPageUrl()))
                .flatMap(feedPosition -> fetchPage(feedPosition.getPageUrl(), eTag)
                        .map(page -> ParsedFeedPage.parse(page, feedPosition))
                )
                .doOnEvent((page, throwable) -> logger.debug("Page fetched {}", page.getPage().getSelfHref()))
                .toFlowable()
                .flatMap(parsedPage -> Flowable
                        .fromIterable(parsedPage.getEntries())
                        .doOnNext(eFeedEntry -> logger.debug("Returning feed entry {} (page {})", eFeedEntry.getEntry().getId(), eFeedEntry.getSelfHref()))
                        .concatWith(fetchEntries(parsedPage.getPage(), feedPositionStrategy, eTag)
                ))
                .doOnNext(eFeedEntry -> logger.debug("Returning feed entry {} (page {})", eFeedEntry.getEntry().getId(), eFeedEntry.getSelfHref()))
                ;
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
