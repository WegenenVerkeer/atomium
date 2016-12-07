package be.wegenenverkeer.atomium.api;

import be.wegenenverkeer.atomium.format.Generator;
import org.reactivestreams.Publisher;

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
    public Publisher<FeedPage<T>> feedPage(FeedPageRef requestedPage) {

        if (requestedPage.isStrictlyMoreRecentThan(getHeadOfFeedRef())) {
            return new ErrorPublisher(new IndexOutOfBoundsException("Page beyond head of Feed requested"));
        }

        long requested = pageSize + 1;

        FeedPageBuilder<T> builder = new FeedPageBuilder<>(this, requestedPage.getPageNum());

        EntryProcessor<T> pub = new EntryProcessor<>(requested, builder);
        store.getEntries(requestedPage.getPageNum() * pageSize, requested).subscribe(pub);
        return pub;
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
