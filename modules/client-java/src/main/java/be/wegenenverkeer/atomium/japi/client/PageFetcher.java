package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.atomium.api.FeedPageCodec;
import be.wegenenverkeer.atomium.format.JacksonFeedPageCodec;
import be.wegenenverkeer.atomium.format.JaxbCodec;
import be.wegenenverkeer.rxhttpclient.ClientRequest;
import be.wegenenverkeer.rxhttpclient.ClientRequestBuilder;
import be.wegenenverkeer.rxhttpclient.RxHttpClient;
import be.wegenenverkeer.rxhttpclient.ServerResponse;
import com.fasterxml.jackson.databind.Module;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

import java.util.Map;
import java.util.Optional;

/**
 * Created by Karel Maesen, Geovise BVBA on 06/06/2020.
 */
class PageFetcher<E> {

    private final RxHttpClient rxHttpClient;
    private final FeedPageCodec<E, String> jsonCodec;
    private final FeedPageCodec<E, String> xmlCodec;
    private final String feedName;
    private final Map<String, String> headers;

    PageFetcher(String feedPath, Class<E> entryTypeMarker, RxHttpClient client, Map<String, String> headers, Module... modules) {
        this.rxHttpClient = client;
        this.feedName = feedPath;
        this.jsonCodec = new JacksonFeedPageCodec<E>(entryTypeMarker);
        ((JacksonFeedPageCodec<E>) this.jsonCodec).registerModules(modules);
        this.xmlCodec = new JaxbCodec<E>(entryTypeMarker);
        this.headers = headers;
    }

    Single<EtaggedFeedPage<E>> getPage(String url, Optional<String> etag) {
        ClientRequest request = buildConditionalGet(url, etag);
        return Single.fromPublisher(
                rxHttpClient.executeToCompletion(request, resp -> toFeedWrapper(resp, etag))
        );
    }

    private ClientRequest buildConditionalGet(String url, Optional<String> etag) {
        ClientRequestBuilder builder = rxHttpClient.requestBuilder().setMethod("GET");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.addHeader(header.getKey(), header.getValue());
        }
        String relative = new UrlHelper(rxHttpClient.getBaseUrl()).toRelative(feedName, url);
        builder.setUrlRelativetoBase(relative);

        if (etag.isPresent()) {
            builder.addHeader("If-None-Match", etag.get());
        }

        return builder.build();
    }


    private EtaggedFeedPage<E> toFeedWrapper(ServerResponse resp, Optional<String> etag) {
        if (resp.getStatusCode() == 304) {
            return new EmptyEtaggedFeedPage<>(etag);
        }
        Optional<String> newETag = resp.getHeader("ETag");
        if (isJson(resp.getContentType())) {
            return new EtaggedFeedPage<>(jsonCodec.decode(resp.getResponseBody()), newETag);
        } else {
            return new EtaggedFeedPage<>( xmlCodec.decode(resp.getResponseBody()), newETag);
        }
    }

    private boolean isJson(Optional<String> contentType) {
        if (!contentType.isPresent()) {
            //assume JSON
            return true;
        }
        String ct = contentType.get();
        return ct.contains("json") || ct.contains("JSON");
    }

}