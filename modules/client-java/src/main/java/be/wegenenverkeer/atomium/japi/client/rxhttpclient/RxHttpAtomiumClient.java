package be.wegenenverkeer.atomium.japi.client.rxhttpclient;

import be.wegenenverkeer.atomium.japi.client.AtomiumClient;
import be.wegenenverkeer.atomium.japi.client.AtomiumFeed;
import be.wegenenverkeer.atomium.japi.client.PageFetcher;
import be.wegenenverkeer.rxhttpclient.RxHttpClient;
import be.wegenenverkeer.rxhttpclient.rxjava.RxJavaHttpClient;

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
    RxHttpAtomiumClient(RxHttpClient rxHttpClient) {
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

    public static class Builder {
        private String baseUrl;
        private boolean followRedirect;

        public Builder() {
        }

        public RxHttpAtomiumClient build() {
            RxHttpClient rxHttpClient = new RxJavaHttpClient.Builder()
                    .setBaseUrl(baseUrl)
                    .setFollowRedirect(followRedirect)
                    .build();

            return new RxHttpAtomiumClient(rxHttpClient);
        }

        public Builder setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder setFollowRedirect(boolean followRedirect) {
            this.followRedirect = followRedirect;
            return this;
        }
    }
}
