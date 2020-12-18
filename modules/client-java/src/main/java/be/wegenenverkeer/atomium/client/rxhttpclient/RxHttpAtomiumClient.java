package be.wegenenverkeer.atomium.client.rxhttpclient;

import be.wegenenverkeer.atomium.client.PageFetcher;
import be.wegenenverkeer.atomium.client.AtomiumClient;
import be.wegenenverkeer.atomium.client.AtomiumFeed;
import be.wegenenverkeer.rxhttpclient.RxHttpClient;

import java.util.ArrayList;
import java.util.List;

/**
 * A client for Atomium AtomPub feeds.
 *
 * <p>It is best-practice to create a single AtomiumClient for all feeds on a specific host.</p>
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 16/03/15.
 */
public class RxHttpAtomiumClient implements AtomiumClient {
    private final List<PageFetcher<?>> pageFetchers = new ArrayList<>();
    private final RxHttpClient rxHttpClient;

    /**
     * Creates an AtomiumClient from the specified {@code PageFetcher} instance
     */
    public RxHttpAtomiumClient(RxHttpClient rxHttpClient) {
        this.rxHttpClient = rxHttpClient;
    }

    @Override
    public <E> AtomiumFeed<E> feed(PageFetcher<E> pageFetcher) {
        pageFetchers.add(pageFetcher);
        return new AtomiumFeed<>(pageFetcher);
    }

    public <E> RxHttpPageFetcher.Builder<E> getPageFetcherBuilder(String feedUrl, Class<E> entityTypeMarker) {
        return new RxHttpPageFetcher.Builder<>(rxHttpClient, feedUrl, entityTypeMarker);
    }

    public void close() {
        this.pageFetchers.forEach(PageFetcher::close);
    }
}
