package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.atomium.japi.format.Entry;
import be.wegenenverkeer.atomium.japi.format.Feed;
import be.wegenenverkeer.atomium.japi.format.Link;
import be.wegenenverkeer.rxhttp.ClientRequest;
import be.wegenenverkeer.rxhttp.ClientRequestBuilder;
import be.wegenenverkeer.rxhttp.RxHttpClient;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Karel Maesen, Geovise BVBA on 16/03/15.
 */
public class AtomiumClient {

    private final static Logger logger = LoggerFactory.getLogger(AtomiumClient.class);

    private final RxHttpClient rxHttpClient;


    private AtomiumClient(RxHttpClient rxHttpClient) {
        this.rxHttpClient = rxHttpClient;
    }

    public <E> FeedObservableBuilder<E> feed(String feedName, Class<E> entryTypeMarker) {
        return new FeedObservableBuilder<>(feedName, entryTypeMarker, rxHttpClient);
    }

    public void close() {
        this.rxHttpClient.close();
    }


    public static class FeedObservableBuilder<E> {
        private final RxHttpClient rxHttpClient;
        private final JAXBContext jaxbContext;
        private final ObjectMapper objectMapper;
        private final String feedName;
        private final JavaType javaType;

        FeedObservableBuilder(String feedName, Class<E> entryTypeMarker, RxHttpClient rxClient) {
            this.rxHttpClient = rxClient;
            this.feedName = feedName;
            objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JodaModule());
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            objectMapper.setTimeZone(TimeZone.getDefault());
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            this.javaType = objectMapper.getTypeFactory().constructParametricType(Feed.class, entryTypeMarker);

            try {
                jaxbContext = JAXBContext.newInstance(Feed.class, Link.class, entryTypeMarker);
            } catch (JAXBException e) {
                throw new IllegalStateException(e);
            }
        }

        static class ClientState {
            Optional<String> lastSeenEtag = Optional.empty();
            Optional<String> lastSeenSelfHref = Optional.empty();
            Optional<String> lastSeenEntryId = Optional.empty();
        }


        public Observable<Entry<E>> observeFrom(final String entryId, final String pageUrl, final int intervalInMs) {
            final ClientState state = new ClientState();
            state.lastSeenEntryId = Optional.of(entryId);
            state.lastSeenSelfHref = Optional.of(pageUrl);
            return feedWrapperObservable(state, intervalInMs);
        }


        /**
         * Observes the head of the feed
         *
         * @return
         */
        public Observable<Entry<E>> observe(final int intervalInMs) {
            final ClientState state = new ClientState();
            return feedWrapperObservable(state, intervalInMs);
        }

        /**
         * This is the core of the feed client
         */
        private Observable<Entry<E>> feedWrapperObservable(final ClientState state, final int intervalInMs) {
            Observable<FeedWrapper<E>> observableFeedPage = Observable.create((subscriber) -> {
                Scheduler.Worker worker = Schedulers.newThread().createWorker();
                worker.schedulePeriodically(() -> {
                    logger.debug("Scheduled work started");
                    String url = state.lastSeenSelfHref.orElse("");
                    try {
                        while (!subscriber.isUnsubscribed() && url != null) {
                            logger.debug("Start polling");
                            Optional<String> etag = Optional.empty();
                            etag = state.lastSeenEtag;
                            logger.debug("Retreiving page: " + url);
                            Observable<FeedWrapper<E>> feedObservable = createFeedWrapperObservable(url, etag);
                            FeedWrapper<E> feed = prune(feedObservable.toBlocking().last(), state);
                            if (!feed.isEmpty()) {

                                logger.debug("Emitting: " + feed.getEntries());
                                subscriber.onNext(feed);
                                state.lastSeenEtag = feed.getEtag();
                                state.lastSeenEntryId = Optional.of(feed.getLastEntryId());
                                logger.debug("Setting lastseenSelfHref to :" + feed.getSelfHref());
                                state.lastSeenSelfHref = feed.getSelfHref();
                            } else {
                                logger.debug("Received 304");
                            }
                            url = feed.getPreviousHref().orElse(null);
                        }

                        if (subscriber.isUnsubscribed()) {
                            logger.debug("Worker unsubscribe");
                            worker.unsubscribe();
                        }

                    } catch (Exception e) {
                        subscriber.onError(e);
                        worker.unsubscribe();
                        logger.debug("Worker unsubscribe after error");
                    }
                    logger.debug("Scheduled work finished.");
                }, 0, intervalInMs, TimeUnit.MILLISECONDS);
            });

            return observableFeedPage.flatMap(feed -> Observable.from(feed.getEntries()));
        }


        //** removes the entries in the feedWrapper
        private static <T> FeedWrapper<T> prune(FeedWrapper<T> feedWrapper, ClientState state) {

            if (feedWrapper.isEmpty() || !state.lastSeenEntryId.isPresent()) return feedWrapper;
            if (!feedWrapper.getSelfHref().equals(state.lastSeenSelfHref)) return feedWrapper;
            List<Entry<T>> pruned = new ArrayList<Entry<T>>();
            boolean skip = true;
            for (Entry<T> entry : feedWrapper.getEntries()) {
                if (!skip) {
                    pruned.add(entry);
                } else if (entry.getId().equals(state.lastSeenEntryId.get())) {
                    skip = false;
                }
            }
            return new FeedWrapper<>(feedWrapper.getLinks(), pruned, feedWrapper.etag);
        }



        @SuppressWarnings("unchecked")
        private Observable<FeedWrapper<E>> createFeedWrapperObservable(String pageUrl, Optional<String> etag) {
            ClientRequest request = buildConditionalGet(pageUrl, etag);

            Observable<FeedWrapper<E>> feedObservable = rxHttpClient.executeToCompletion(request, resp -> {
                if (resp.getStatusCode() == 304) {
                    return new EmptyFeedWrapper<>(etag);
                }
                Optional<String> newETag = resp.getHeader("ETag");
                if (isJson(resp.getContentType())) {
                    return new FeedWrapper<>((Feed<E>) unmarshalJson(resp.getResponseBody()), newETag);
                } else {
                    return new FeedWrapper<>((Feed<E>) unmarshalXml(resp.getResponseBody()), newETag);
                }
            });

            return feedObservable.flatMap(f -> f != null ? Observable.just(f) : Observable.empty());
        }

        private ClientRequest buildConditionalGet(String url, Optional<String> etag) {
            ClientRequestBuilder builder = rxHttpClient.requestBuilder().setMethod("GET");
            String relative = new UrlHelper(rxHttpClient.getBaseUrl()).toRelative(feedName, url);
            builder.setUrlRelativetoBase(relative);

            if (etag.isPresent()) {
                builder.addHeader("If-None-Match", etag.get());
            }

            return builder.build();
        }

        private boolean isJson(Optional<String> contentType) {
            if (!contentType.isPresent()) {
                //assume JSON
                return true;
            }
            String ct = contentType.get();
            return ct.contains("json") || ct.contains("JSON");
        }


        private Object unmarshalXml(String str) {
            try {
                return jaxbContext.createUnmarshaller().unmarshal(new InputSource(new StringReader(str)));
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }

        private Object unmarshalJson(String str) {
            try {
                return objectMapper.readValue(str, javaType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    /**
     * A Builder for an AtomiumClient.
     *
     */
    public static class Builder {

        private final String JSON_MIME_TYPE = "application/json"; //TODO -- change to "official" AtomPub mimetypes
        private final String XML_MIME_TYPE = "application/xml";

        final private RxHttpClient.Builder rxHttpClientBuilder = new RxHttpClient.Builder();

        public Builder() {
            //default builder state
        }

        /**
         * Set the maximum time in millisecond an {@link com.ning.http.client.AsyncHttpClient} will keep connection
         * idle in pool.
         *
         * @param pooledConnectionIdleTimeout @return a {@link be.wegenenverkeer.rxhttp.RxHttpClient.Builder}
         */
        public Builder setPooledConnectionIdleTimeout(int pooledConnectionIdleTimeout) {
            rxHttpClientBuilder.setPooledConnectionIdleTimeout(pooledConnectionIdleTimeout);
            return this;
        }

        public AtomiumClient build() {
            return new AtomiumClient(rxHttpClientBuilder.build());
        }

        public Builder setAcceptJson() {
            rxHttpClientBuilder.setAccept(JSON_MIME_TYPE);
            return this;
        }

        public Builder setAcceptXml() {
            rxHttpClientBuilder.setAccept(XML_MIME_TYPE);
            return this;
        }


        public Builder setBaseUrl(String url) {
            rxHttpClientBuilder.setBaseUrl(url);
            return this;
        }

        /**
         * Set the maximum number of connections an {@link com.ning.http.client.AsyncHttpClient} can handle.
         *
         * @param maxConnections the maximum number of connections an {@link com.ning.http.client.AsyncHttpClient}
         *                       can handle.
         * @return a {@link be.wegenenverkeer.rxhttp.RxHttpClient.Builder}
         */
        public Builder setMaxConnections(int maxConnections) {
            rxHttpClientBuilder.setMaxConnections(maxConnections);
            return this;
        }

        /**
         * Set true if connection can be pooled by a ChannelPool. Default is true.
         *
         * @param allowPoolingConnections true if connection can be pooled by a ChannelPool
         * @return a {@link be.wegenenverkeer.rxhttp.RxHttpClient.Builder}
         */
        public Builder setAllowPoolingConnections(boolean allowPoolingConnections) {
            rxHttpClientBuilder.setAllowPoolingConnections(allowPoolingConnections);
            return this;
        }

        /**
         * Set the maximum time in millisecond an {@link com.ning.http.client.AsyncHttpClient} can wait when
         * connecting to a remote host
         *
         * @param connectTimeOut the maximum time in millisecond an {@link com.ning.http.client.AsyncHttpClient} can
         *                       wait when connecting to a remote host
         * @return a {@link be.wegenenverkeer.rxhttp.RxHttpClient.Builder}
         */
        public Builder setConnectTimeout(int connectTimeOut) {
            rxHttpClientBuilder.setConnectTimeout(connectTimeOut);
            return this;
        }

        /**
         * Set the {@link java.util.concurrent.ExecutorService} an {@link com.ning.http.client.AsyncHttpClient} use
         * for handling
         * asynchronous response.
         *
         * @param applicationThreadPool the {@link java.util.concurrent.ExecutorService} an {@link com.ning.http
         *                              .client.AsyncHttpClient} use for handling
         *                              asynchronous response.
         * @return a {@link be.wegenenverkeer.rxhttp.RxHttpClient.Builder}
         */
        public Builder setExecutorService(ExecutorService applicationThreadPool) {
            rxHttpClientBuilder.setExecutorService(applicationThreadPool);
            return this;
        }
    }

}
