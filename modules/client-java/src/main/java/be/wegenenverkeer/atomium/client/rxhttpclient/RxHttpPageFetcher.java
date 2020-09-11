package be.wegenenverkeer.atomium.client.rxhttpclient;

import be.wegenenverkeer.atomium.api.FeedPageCodec;
import be.wegenenverkeer.atomium.client.CachedFeedPage;
import be.wegenenverkeer.atomium.client.EmptyCachedFeedPage;
import be.wegenenverkeer.atomium.client.PageFetcher;
import be.wegenenverkeer.atomium.client.RetryStrategy;
import be.wegenenverkeer.atomium.client.UrlHelper;
import be.wegenenverkeer.atomium.format.JacksonFeedPageCodec;
import be.wegenenverkeer.atomium.format.JaxbCodec;
import be.wegenenverkeer.atomium.client.FeedFetchException;
import be.wegenenverkeer.rxhttpclient.ClientRequest;
import be.wegenenverkeer.rxhttpclient.ClientRequestBuilder;
import be.wegenenverkeer.rxhttpclient.RxHttpClient;
import be.wegenenverkeer.rxhttpclient.ServerResponse;
import com.fasterxml.jackson.databind.Module;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;

/**
 * Created by Karel Maesen, Geovise BVBA on 06/06/2020.
 */
class RxHttpPageFetcher<E> implements PageFetcher<E> {
    private final static Logger logger = LoggerFactory.getLogger(RxHttpPageFetcher.class);
    private final String feedUrl;
    private final Class<E> entryTypeMarker;
    private final FeedPageCodec<E, String> codec;
    private final RxHttpClient rxHttpClient;
    private final ClientRequestCustomizer requestCustomizer;
    private final RetryStrategy retryStrategy;

    RxHttpPageFetcher(String feedUrl,
            Class<E> entryTypeMarker,
            FeedPageCodec<E, String> codec,
            RxHttpClient rxHttpClient,
            ClientRequestCustomizer requestCustomizer,
            RetryStrategy retryStrategy) {
        this.feedUrl = feedUrl;
        this.entryTypeMarker = entryTypeMarker;
        this.codec = codec;
        this.rxHttpClient = rxHttpClient;
        this.requestCustomizer = requestCustomizer;
        this.retryStrategy = retryStrategy;
    }

    @Override
    public Single<CachedFeedPage<E>> fetch(String pageUrl, Optional<String> etag) {
        ClientRequest request = buildRequest(pageUrl, etag);
        return Single.fromPublisher(rxHttpClient.executeToCompletion(request, resp -> parseResponse(resp, pageUrl, etag)));
    }

    @Override
    public void close() {
        rxHttpClient.close();
    }

    @Override
    public Class<E> getEntryTypeMarker() {
        return this.entryTypeMarker;
    }

    @Override
    public RetryStrategy getRetryStrategy() {
        return this.retryStrategy;
    }

    ClientRequest buildRequest(String pageUrl, Optional<String> etag) {
        ClientRequestBuilder builder = rxHttpClient.requestBuilder().setMethod("GET");
        builder.addHeader("Accept", codec.getMimeType());

        String relative = new UrlHelper(rxHttpClient.getBaseUrl()).toRelative(feedUrl, pageUrl);
        builder.setUrlRelativetoBase(relative);

        etag.ifPresent(s -> builder.addHeader("If-None-Match", s));

        this.requestCustomizer.apply(builder);

        return builder.build();
    }


    CachedFeedPage<E> parseResponse(ServerResponse response, String pageUrl, Optional<String> etag) {
        if (response.getStatusCode() == HTTP_NOT_MODIFIED) {
            return new EmptyCachedFeedPage<>(pageUrl, etag);
        }

        Optional<String> newETag = response.getHeader("ETag");
        return new CachedFeedPage<E>(codec.decode(response.getResponseBody()), newETag);
    }

    public static class Builder<E> {
        private final RxHttpClient rxHttpClient;
        private final String feedUrl;
        private final Class<E> entryTypeMarker;
        private ClientRequestCustomizer requestCustomizer = builder -> {
        };
        private RetryStrategy retryStrategy = (count, exception) -> {
            logger.info("Retry feed count {}", count);
            return count.longValue();
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
                    requestCustomizer,
                    retryStrategy
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

        public RxHttpPageFetcher.Builder<E> setClientRequestCustomizer(ClientRequestCustomizer requestCustomizer) {
            this.requestCustomizer = requestCustomizer;
            return this;
        }

        public RxHttpPageFetcher.Builder<E> setRetryStrategy(RetryStrategy retryStrategy) {
            this.retryStrategy = retryStrategy;
            return this;
        }
    }
}
