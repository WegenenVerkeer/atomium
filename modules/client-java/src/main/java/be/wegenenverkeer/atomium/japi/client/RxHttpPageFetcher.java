package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.atomium.api.FeedPageCodec;
import be.wegenenverkeer.atomium.format.JacksonFeedPageCodec;
import be.wegenenverkeer.atomium.format.JaxbCodec;
import be.wegenenverkeer.rxhttpclient.ClientRequest;
import be.wegenenverkeer.rxhttpclient.ClientRequestBuilder;
import be.wegenenverkeer.rxhttpclient.RxHttpClient;
import be.wegenenverkeer.rxhttpclient.ServerResponse;
import com.fasterxml.jackson.databind.Module;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Created by Karel Maesen, Geovise BVBA on 06/06/2020.
 */
class RxHttpPageFetcher<E> implements PageFetcher<E> {
    private final static Logger logger = LoggerFactory.getLogger(RxHttpPageFetcher.class);
    private final String feedUrl;
    private final Class<E> entryTypeMarker;
    private final FeedPageCodec<E, String> codec;
    private final RxHttpClient rxHttpClient;
    private final RxHttpRequestStrategy requestStrategy;

    RxHttpPageFetcher(String feedUrl,
            Class<E> entryTypeMarker,
            FeedPageCodec<E, String> codec,
            RxHttpClient rxHttpClient,
            RxHttpRequestStrategy requestStrategy) {
        this.feedUrl = feedUrl;
        this.entryTypeMarker = entryTypeMarker;
        this.codec = codec;
        this.rxHttpClient = rxHttpClient;
        this.requestStrategy = requestStrategy;
    }

    @Override
    public Single<CachedFeedPage<E>> fetch(String pageUrl, Optional<String> etag) {
        ClientRequest request = buildRequest(pageUrl, etag);
        return Single.fromPublisher(rxHttpClient.executeToCompletion(request, resp -> parseResponse(resp, etag)));
    }

    @Override
    public void close() {
        rxHttpClient.close();
    }

    @Override
    public Class<E> getEntryTypeMarker() {
        return null;
    }

    ClientRequest buildRequest(String pageUrl, Optional<String> etag) {
        ClientRequestBuilder builder = rxHttpClient.requestBuilder().setMethod("GET");
        builder.addHeader("Accept", codec.getMimeType());

        String relative = new UrlHelper(rxHttpClient.getBaseUrl()).toRelative(feedUrl, pageUrl);
        builder.setUrlRelativetoBase(relative);

        etag.ifPresent(s -> builder.addHeader("If-None-Match", s));

        this.requestStrategy.apply(builder);

        return builder.build();
    }


    CachedFeedPage<E> parseResponse(ServerResponse response, Optional<String> etag) {
        if (response.getStatusCode() == 304) {
            return new EmptyCachedFeedPage<>(etag);
        }

        Optional<String> newETag = response.getHeader("ETag");
        return new CachedFeedPage<E>(codec.decode(response.getResponseBody()), newETag);
    }

    boolean isJson(Optional<String> contentType) {
        if (contentType.isPresent()) {
            return contentType.map(ct -> ct.contains("json") || ct.contains("JSON")).get();
        }

        //assume JSON
        return true;
    }

    public static class Builder<E> {
        private final RxHttpClient rxHttpClient;
        private final String feedUrl;
        private final Class<E> entryTypeMarker;
        private RxHttpRequestStrategy requestStrategy = builder -> {
        };

        private FeedPageCodec<E, String> codec;

        Builder(RxHttpClient rxHttpClient, String feedUrl, Class<E> entryTypeMarker) {
            this.rxHttpClient = rxHttpClient;
            this.feedUrl = feedUrl;
            this.entryTypeMarker = entryTypeMarker;
        }

        public PageFetcher<E> build() {
            if (this.codec == null) {
                setAcceptJson();
            }

            return new RxHttpPageFetcher<E>(
                    feedUrl,
                    entryTypeMarker,
                    codec,
                    rxHttpClient,
                    requestStrategy
            );
        }

        public RxHttpPageFetcher.Builder<E> setAcceptJson() {
            this.codec = new JacksonFeedPageCodec<>(this.entryTypeMarker);

            return this;
        }

        public RxHttpPageFetcher.Builder<E> setAcceptXml() {
            this.codec = new JaxbCodec<>(this.entryTypeMarker);

            return this;
        }

        public RxHttpPageFetcher.Builder<E> registerModules(Module... modules) {
            if (this.codec != null && this.codec instanceof JacksonFeedPageCodec) {
                ((JacksonFeedPageCodec<E>) this.codec).registerModules(modules);
            }

            return this;
        }

        public RxHttpPageFetcher.Builder<E> setRequestStrategy(RxHttpRequestStrategy requestStrategy) {
            this.requestStrategy = requestStrategy;
            return this;
        }
    }
}
