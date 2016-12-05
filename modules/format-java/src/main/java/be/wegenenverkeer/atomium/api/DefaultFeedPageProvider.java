package be.wegenenverkeer.atomium.api;

import be.wegenenverkeer.atomium.format.Generator;
import org.reactivestreams.Publisher;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
public class DefaultFeedPageProvider<T> implements FeedPageProvider<T>{

    private final static Generator GENERATOR = new Generator("Default Feed provider", "https://github.com/WegenenVerkeer/atomium", "1.0");
    final private FeedEntryStore<T> store;
    final private int pageSize;
    final private String feedUrl;
    final private String name;


    public DefaultFeedPageProvider(FeedEntryStore<T> store, String name, int pageSize, String feedUrl){
        this.store = store;
        this.pageSize = pageSize;
        this.feedUrl = feedUrl;
        this.name = name;
    }

    @Override
    public Publisher<FeedPage<T>> feedPage(FeedPageReference ref) {
        int requested = pageSize + 1;

        FeedPageBuilder<T> builder = new FeedPageBuilder<>(this, ref.getPageNum());

        EntryProcessor<T> pub = new EntryProcessor<>(requested, builder);
        store.getEntries(ref.getPageNum() * pageSize, requested).subscribe(pub);
        return pub;
    }

    @Override
    public int getPageSize() {
        return this.pageSize;
    }

    @Override
    public String getFeedUrl() {
        return this.getFeedUrl();
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
