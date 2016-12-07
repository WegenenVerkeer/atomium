package be.wegenenverkeer.atomium.api;

import be.wegenenverkeer.atomium.format.Generator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
public class DefaultFeedPageProvider<T> implements FeedPageProvider<T>{

    private final static Generator GENERATOR = new Generator("Default Feed provider", "https://github.com/WegenenVerkeer/atomium", "1.0");
    final private FeedEntryStore<T> store;
    final private long pageSize;
    final private String feedUrl;
    final private String name;


    public DefaultFeedPageProvider(FeedEntryStore<T> store, String name, long pageSize, String feedUrl){
        this.store = store;
        this.pageSize = pageSize;
        this.feedUrl = feedUrl;
        this.name = name;
    }

    @Override
    public Future<FeedPage<T>> getFeedPageAsync(FeedPageRef requestedPage) {

        CompletableFuture<FeedPage<T>> futurePage = new CompletableFuture<>();
        if (requestedPage.isStrictlyMoreRecentThan(getHeadOfFeedRef())) {
            futurePage.completeExceptionally(new IndexOutOfBoundsException("Requested page currently beyond head of feed"));
        }

        long requested = pageSize + 1;

        FeedPageBuilder<T> builder = new FeedPageBuilder<>(this, requestedPage.getPageNum());

        EntryToPage<T> pub = new EntryToPage<>(requested, builder, futurePage);
        store.getEntries(requestedPage.getPageNum() * pageSize, requested).subscribe(pub);
        return futurePage;
    }

    @Override
    public FeedPageRef getHeadOfFeedRef() {
        long n = store.totalNumberOfEntries() / getPageSize();
        return FeedPageRef.page(n);
    }

    @Override
    public long getPageSize() {
        return this.pageSize;
    }

    @Override
    public String getFeedUrl() {
        return this.feedUrl;
    }

    @Override
    public String getFeedName() {
        return this.name;
    }

    @Override
    public Generator getFeedGenerator() {
        return GENERATOR;
    }


}
