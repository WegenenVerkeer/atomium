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
 * A client for Atomium AtomPub feeds.
 *
 * <p>It is best-practice to create a single AtomiumClient for all feeds on a specific host.</p>
 *
 * Created by Karel Maesen, Geovise BVBA on 16/03/15.
 */
public class AtomiumClient {

    private final static Logger logger = LoggerFactory.getLogger(AtomiumClient.class);

    private final RxHttpClient rxHttpClient;


    private AtomiumClient(RxHttpClient rxHttpClient) {
        this.rxHttpClient = rxHttpClient;
    }

    /**
     * Creates a {@code FeedObservableBuilder} for the specified feed and entry type
     *
     * <p>The feedPath argument appended to the baseUrl of this {@code AtomiumClient} should equal the
     * xml:base-attribute of the feedpage</p>
     *
     * <p>The entryTypeMarker-class should have the required public accessors and JAXB-annotations to enable
     * proper unmarshalling. For Json-unmarshalling, the  Jackson library is used.</p>
     *
     * @param feedPath the path to the feed
     * @param entryTypeMarker the Class of the Entry content value
     * @param <E> the class parameter of the Entry content value
     * @return
     */
    public <E> FeedObservableBuilder<E> feed(String feedPath, Class<E> entryTypeMarker) {
        return new FeedObservableBuilder<>(feedPath, entryTypeMarker, rxHttpClient);
    }

    /**
     * Closes this instance.
     *
     */
    public void close() {
        this.rxHttpClient.close();
    }


    /**
     * Builds an {@code Observable<Entry<E>>}
     * @param <E> the type of Entry content
     */
    public static class FeedObservableBuilder<E> {
        private final RxHttpClient rxHttpClient;
        private final JAXBContext jaxbContext;
        private final ObjectMapper objectMapper;
        private final String feedName;
        private final JavaType javaType;

        /**
         * Creates an instance - only accessible from AtomiumClient.
         * @param feedPath the path to the feed
         * @param entryTypeMarker the class of Entry content
         * @param rxClient the underlying Http-client.
         */
        FeedObservableBuilder(String feedPath, Class<E> entryTypeMarker, RxHttpClient rxClient) {
            this.rxHttpClient = rxClient;
            this.feedName = feedPath;
            this.objectMapper = configureObjectMapper();
            this.javaType = objectMapper.getTypeFactory().constructParametricType(Feed.class, entryTypeMarker);
            try {
                jaxbContext = JAXBContext.newInstance(Feed.class, Link.class, entryTypeMarker);
            } catch (JAXBException e) {
                throw new IllegalStateException(e);
            }
        }

        private ObjectMapper configureObjectMapper() {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JodaModule());
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            objectMapper.setTimeZone(TimeZone.getDefault()); //this is required since default TimeZone is GMT in Jackons!
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            return objectMapper;
        }

        /**
         * Helper class that contains the mutable state of the Observable
         */
        static class ClientState {
            Optional<String> lastSeenEtag = Optional.empty();
            Optional<String> lastSeenSelfHref = Optional.empty();
            Optional<String> lastSeenEntryId = Optional.empty();
        }


        /**
         * Creates a "cold" {@code Observable<Entry<E>>} that, when subscribed to, emits all entries in the feed
         * starting from the oldest entry immediately after the specified entry.
         *
         * <p>When subscribed to, the observable will create a single-threaded {@link Scheduler.Worker} that will:</p>
         * <ul>
         *     <li>retrieve the specified feed page</li>
         *     <li>emit all entries more recent that the specified feed entry</li>
         *     <li>follow iteratively the 'previous'-links and emits all entries on the linked-to pages until it
         *     arrives at the head of the feed (identified by not having a 'previous'-link)</li>
         *     <li>poll the feed at the specified interval (using conditional GETs) and emit all entries not yet seen</li>
         * </ul>
         *
         * <p>The worker will exit only on an error condition, or on unsubscribe.</p>
         *
         * <p><em>Important:</em> a new and independent worker is created for each subscriber.</p>
         *
         * @param entryId the entry-id of an entry on the specified page
         * @param pageUrl the url (absolute, or relative to the feed's base url) of the feed-page, containing the entry
         *                identified with the entryId argument
         * @param intervalInMs the polling interval in milliseconds.
         * @return an Observable<Entry<E>> emitting all entries since the specified entry
         */
        public Observable<Entry<E>> observeSince(final String entryId, final String pageUrl, final int intervalInMs) {
            final ClientState state = new ClientState();
            state.lastSeenEntryId = Optional.of(entryId);
            state.lastSeenSelfHref = Optional.of(pageUrl);
            return feedWrapperObservable(state, intervalInMs);
        }


        /**
         * Creates a "cold" {@code Observale<Entry<E>>} that, when subscribed to, emits all entries on the feed
         * starting from those then on the head of the feed.
         *
         * <p>The behavior is analogous to the method {@code observeSince()} but starting form </p>
         *
         * @return
         */
        public Observable<Entry<E>> observe(final int intervalInMs) {
            final ClientState state = new ClientState();
            return feedWrapperObservable(state, intervalInMs);
        }

        /**
         * This is the core of the feed client
         *
         * It creates a Scheduler.Worker that with the specified interval polls the feed, and retrieves all entries not
         * yet "seen". The ClientState object is used to keep track of the latest seen feed-pages, Etags and entry-id's
         *
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


        //** removes the entries in the feedWrapper that have already been seen.
        private static <T> FeedWrapper<T> prune(FeedWrapper<T> feedWrapper, ClientState state) {

            if (feedWrapper.isEmpty() || !state.lastSeenEntryId.isPresent()) return feedWrapper;
            if (!feedWrapper.getSelfHref().equals(state.lastSeenSelfHref)) return feedWrapper;
            List<Entry<T>> pruned = new ArrayList<>();
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

        /**
         * Sets the Accept-header to JSON.
         * @return
         */
        public Builder setAcceptJson() {
            rxHttpClientBuilder.setAccept(JSON_MIME_TYPE);
            return this;
        }

        /**
         * Sets the Accept-header to XML
         * @return
         */
        public Builder setAcceptXml() {
            rxHttpClientBuilder.setAccept(XML_MIME_TYPE);
            return this;
        }


        /**
         * Sets the base URL for this instance.
         *
         * @param url absolute URL where feeds are published
         * @return
         */
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
