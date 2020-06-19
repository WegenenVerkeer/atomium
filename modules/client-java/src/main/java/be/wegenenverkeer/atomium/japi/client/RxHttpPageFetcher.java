package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.atomium.api.FeedPageCodec;
import be.wegenenverkeer.atomium.format.JacksonFeedPageCodec;
import be.wegenenverkeer.atomium.format.JaxbCodec;
import be.wegenenverkeer.rxhttpclient.ClientRequest;
import be.wegenenverkeer.rxhttpclient.ClientRequestBuilder;
import be.wegenenverkeer.rxhttpclient.ServerResponse;
import com.fasterxml.jackson.databind.Module;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by Karel Maesen, Geovise BVBA on 06/06/2020.
 */
class RxHttpPageFetcher<E> implements PageFetcher<E> {
    private final static Logger logger = LoggerFactory.getLogger(RxHttpPageFetcher.class);
    private final FeedPageCodec<E, String> jsonCodec;
    private final FeedPageCodec<E, String> xmlCodec;
    private final RxHttpPageFetcherConfiguration<E> config;

    RxHttpPageFetcher(RxHttpPageFetcherConfiguration<E> config) {
        this.config = config;

        this.jsonCodec = new JacksonFeedPageCodec<>(config.getEntryTypeMarker());
        ((JacksonFeedPageCodec<E>) this.jsonCodec).registerModules(config.getModules().toArray(new Module[0]));

        this.xmlCodec = new JaxbCodec<>(config.getEntryTypeMarker());
    }

    @Override
    public Single<CachedFeedPage<E>> fetch(String pageUrl, Optional<String> etag) {
        ClientRequest request = buildRequest(pageUrl, etag);
        return Single.fromPublisher(config.getRxHttpClient().executeToCompletion(request, resp -> parseResponse(resp, etag)));
    }

    @Override
    public void close() {
        this.config.getRxHttpClient().close();
    }

    ClientRequest buildRequest(String pageUrl, Optional<String> etag) {
        ClientRequestBuilder builder = this.config.getRxHttpClient().requestBuilder().setMethod("GET");

        String relative = new UrlHelper(this.config.getRxHttpClient().getBaseUrl()).toRelative(config.getFeedUrl(), pageUrl);
        builder.setUrlRelativetoBase(relative);

        etag.ifPresent(s -> builder.addHeader("If-None-Match", s));

        try {
            config.getExtraHeaders().call().forEach(builder::addHeader);
        } catch (Exception e) {
            throw new FeedFetchException("Error processing user defined headers.", e);
        }

        return builder.build();
    }


    CachedFeedPage<E> parseResponse(ServerResponse resp, Optional<String> etag) {
        if (resp.getStatusCode() == 304) {
            return new EmptyCachedFeedPage<>(etag);
        }

        Optional<String> newETag = resp.getHeader("ETag");

        if (isJson(resp.getContentType())) {
            return new CachedFeedPage<>(jsonCodec.decode(resp.getResponseBody()), newETag);
        } else {
            return new CachedFeedPage<>(xmlCodec.decode(resp.getResponseBody()), newETag);
        }
    }

    boolean isJson(Optional<String> contentType) {
        if (contentType.isPresent()) {
            return contentType.map(ct -> ct.contains("json") || ct.contains("JSON")).get();
        }

        //assume JSON
        return true;
    }
//
//    public static class Builder<E> {
//        final private RxHttpPageFetcher.Builder<E> builder = new RxHttpPageFetcher.Builder<>();
//
//        private String baseUrl;
//        private Class<E> entryTypeMarker;
//        private RxHttpClient rxHttpClient;
//        private List<Module> modules = new ArrayList<>();
//
//        public Builder() {
//        }
//
//        public PageFetcher<E> build() {
//            return new RxHttpPageFetcher<E>(baseUrl, entryTypeMarker, rxHttpClient, modules.toArray(new Module[0]));
//        }
//
//        public RxHttpPageFetcher.Builder<E> setBaseUrl(String baseUrl) {
//            this.builder.baseUrl = baseUrl;
//            return this;
//        }
//
//        public RxHttpPageFetcher.Builder<E> setEntryTypeMarker(Class<E> entryTypeMarker) {
//            this.builder.entryTypeMarker = entryTypeMarker;
//            return this;
//        }
//
//        public RxHttpPageFetcher.Builder<E> setRxHttpClient(RxHttpClient rxHttpClient) {
//            this.builder.rxHttpClient = rxHttpClient;
//            return this;
//        }
//
//        public RxHttpPageFetcher.Builder<E> addModule(Module module) {
//            this.builder.modules.add(module);
//            return this;
//        }
//    }
}
