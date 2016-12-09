package be.wegenenverkeer.atomium.api;

import be.wegenenverkeer.atomium.format.Generator;

/**
 * Created by Karel Maesen, Geovise BVBA on 09/12/16.
 */
public class FeedPageMetadata {


    private final static Generator GENERATOR = new Generator("Default Feed provider", "https://github.com/WegenenVerkeer/atomium", "1.0");
    final private long pageSize;
    final private String feedUrl;
    final private String name;


    public FeedPageMetadata(long pageSize, String feedUrl, String name) {
        this.pageSize = pageSize;
        this.feedUrl = feedUrl;
        this.name = name;
    }

    public long getPageSize() {
        return this.pageSize;
    }


    public String getFeedUrl() {
        return this.feedUrl;
    }


    public String getFeedName() {
        return this.name;
    }


    public Generator getFeedGenerator() {
        return GENERATOR;
    }

}
