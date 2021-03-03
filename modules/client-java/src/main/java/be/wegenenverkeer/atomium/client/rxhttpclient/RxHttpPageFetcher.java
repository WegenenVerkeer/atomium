package be.wegenenverkeer.atomium.client.rxhttpclient;

import be.wegenenverkeer.atomium.api.FeedPageCodec;
import be.wegenenverkeer.atomium.client.CachedFeedPage;
import be.wegenenverkeer.atomium.client.EmptyCachedFeedPage;
import be.wegenenverkeer.atomium.client.PageFetcher;
import be.wegenenverkeer.atomium.client.RetryStrategy;
import be.wegenenverkeer.atomium.client.UrlHelper;
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
    public Single<CachedFeedPage<E>> fetch(String pageUrl, Optional<String> eTag) {
        return buildRequest(pageUrl, eTag)
                .doOnSuccess(request -> {
                    if (logger.isDebugEnabled() && request != null) {
                        logger.debug("Fetching page {} with headers {}", request.getUrl(), request.getHeaders());
                    }
                })
                .flatMap(request -> Single.fromPublisher(rxHttpClient.executeToCompletion(request, resp -> parseResponse(resp, pageUrl, eTag))))
                .doOnSuccess(feedPage -> {
                    if (logger.isDebugEnabled() && feedPage != null) {
                        logger.debug("Fetched page with links {}, {} entries and with eTag {}", feedPage.getLinks(), feedPage.getEntries().size(), feedPage.getEtag());
                    }
                })
                .doOnError(throwable -> logger.error("Error fetching page", throwable));
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

    Single<ClientRequest> buildRequest(String pageUrl, Optional<String> eTag) {
        ClientRequestBuilder builder = rxHttpClient.requestBuilder().setMethod("GET");
        builder.addHeader("Accept", codec.getMimeType());

        String relative = new UrlHelper(rxHttpClient.getBaseUrl()).toRelative(feedUrl, pageUrl);
        builder.setUrlRelativetoBase(relative);

        eTag.ifPresent(s -> builder.addHeader("If-None-Match", s));

        return this.requestCustomizer.apply(builder).map(ClientRequestBuilder::build);
    }

    CachedFeedPage<E> parseResponse(ServerResponse response, String pageUrl, Optional<String> eTag) {
        if (response.getStatusCode() == HTTP_NOT_MODIFIED) {
            if (logger.isDebugEnabled()) {
                logger.debug("Not modified, returning empty feed page");
            }
            return new EmptyCachedFeedPage<>(pageUrl, eTag);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Modified, returning feed page");
        }
        Optional<String> newETag = response.getHeader("ETag");
        return new CachedFeedPage<>(codec.decode(response.getResponseBody()), newETag);
    }

    public static class Builder<E> {
        private final RxHttpClient rxHttpClient;
        private final String feedUrl;
        private final Class<E> entryTypeMarker;
        private ClientRequestCustomizer requestCustomizer = Single::just;
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

            return new RxHttpPageFetcher<>(
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
