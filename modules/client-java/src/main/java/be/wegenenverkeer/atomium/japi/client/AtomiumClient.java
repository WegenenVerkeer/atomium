package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.atomium.format.Entry;
import be.wegenenverkeer.atomium.api.FeedPage;
import be.wegenenverkeer.atomium.api.FeedPageCodec;
import be.wegenenverkeer.atomium.format.JacksonFeedPageCodec;
import be.wegenenverkeer.atomium.format.JaxbCodec;
import be.wegenenverkeer.rxhttp.ClientRequest;
import be.wegenenverkeer.rxhttp.ClientRequestBuilder;
import be.wegenenverkeer.rxhttp.RxHttpClient;
import com.fasterxml.jackson.databind.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.schedulers.Schedulers;
import rx.subscriptions.MultipleAssignmentSubscription;

import java.util.*;
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

    /**
     * Creates an AtomiumClient from the specified {@code RxHttpClient} instance
     *
     * @param rxHttpClient the HTTP client
     */
    public AtomiumClient(RxHttpClient rxHttpClient) {
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
     * @param feedPath        the path to the feed
     * @param entryTypeMarker the Class of the Entry content value
     * @param <E>             the class parameter of the Entry content value
     * @param modules         optional extra Jackson modules
     * @return a {@code FeedObservableBuilder}
     */
    public <E> FeedObservableBuilder<E> feed(String feedPath, Class<E> entryTypeMarker, Module... modules) {
        return new FeedObservableBuilder<>(feedPath, entryTypeMarker, rxHttpClient, modules);
    }

    /**
     * Closes this instance.
     */
    public void close() {
        this.rxHttpClient.close();
    }


    /**
     * Builds an {@code Observable<Entry<E>>}
     *
     * @param <E> the type of Entry content
     */
    public static class FeedObservableBuilder<E> {
        private final RxHttpClient rxHttpClient;
        private final FeedPageCodec<E, String> jsonCodec;
        private final FeedPageCodec<E, String> xmlCodec;
        private final String feedName;
        private Map<String, String> extraHeaders = Collections.emptyMap();
        private RetryStrategy retryStrategy= (n, t) -> {
            if (t instanceof RuntimeException) {
                throw (RuntimeException)t;
            } else {
                throw new RuntimeException(t);
            }
        };

        /**
         * Creates an instance - only accessible from AtomiumClient.
         *
         * @param feedPath        the path to the feed
         * @param entryTypeMarker the class of Entry content
         * @param rxClient        the underlying Http-client.
         */
        FeedObservableBuilder(String feedPath, Class<E> entryTypeMarker, RxHttpClient rxClient, Module... modules) {
            this.rxHttpClient = rxClient;
            this.feedName = feedPath;
            this.jsonCodec = new JacksonFeedPageCodec<E>(entryTypeMarker);
            ((JacksonFeedPageCodec<E>) this.jsonCodec).registerModules(modules);
            this.xmlCodec = new JaxbCodec<E>(entryTypeMarker);
        }

        public FeedObservableBuilder<E> withRetry(RetryStrategy strategy) {
            retryStrategy = strategy;
            return this;
        }

        public FeedObservableBuilder<E> withExtraHeaders(Map<String, String> headers) {
            this.extraHeaders = headers;
            return this;
        }

        /**
         * Helper class that contains the mutable state of the Observable
         */
        static class ClientState {
            Optional<String> lastSeenEtag = Optional.empty();
            Optional<String> lastSeenSelfHref = Optional.empty();
            Optional<String> lastSeenEntryId = Optional.empty();
            int failedCount = 0;
        }

        /**
         * Creates a "cold" {@link Observable} that, when subscribed to, emits all entries in the feed
         * starting from the oldest entry immediately after the specified entry.
         * <p>When subscribed to, the observable will create a single-threaded {@link Scheduler.Worker} that will:</p>
         * <ul>
         * <li>retrieve the specified feed page</li>
         * <li>emit all entries more recent that the specified feed entry</li>
         * <li>follow iteratively the 'previous'-links and emits all entries on the linked-to pages until it
         * arrives at the head of the feed (identified by not having a 'previous'-link)</li>
         * <li>poll the feed at the specified interval (using conditional GETs) and emit all entries not yet seen</li>
         * </ul>
         * <p>The worker will exit only on an error condition, or on unsubscribe.</p>
         * <p><em>Important:</em> a new and independent worker is created for each subscriber.</p>
         *
         * @param entryId      the entry-id of an entry on the specified page
         * @param pageUrl      the url (absolute, or relative to the feed's base url) of the feed-page, containing the entry
         *                     identified with the entryId argument
         * @param intervalInMs the polling interval in milliseconds.
         * @return an Observable emitting all entries since the specified entry
         */
        public Observable<FeedEntry<E>> observeFrom(final String entryId, final String pageUrl, final int intervalInMs) {
            final ClientState state = new ClientState();
            state.lastSeenEntryId = Optional.of(entryId);
            state.lastSeenSelfHref = Optional.of(pageUrl);
            return feedWrapperObservable(state, intervalInMs);
        }

        /**
         * @param entryId      the entry-id of an entry on the specified page
         * @param pageUrl      the url (absolute, or relative to the feed's base url) of the feed-page, containing the entry
         *                     identified with the entryId argument
         * @param intervalInMs the polling interval in milliseconds.
         * @return an Observable emitting all entries since the specified entry
         * @see #observeFrom(String, String, int)
         * @deprecated Replaced by {@link #observeFrom(String, String, int) observeFrom}. This method will be removed in the next version.
         */
        @Deprecated
        public Observable<FeedEntry<E>> observeSince(final String entryId, final String pageUrl, final int intervalInMs) {
            return observeFrom(entryId, pageUrl, intervalInMs);
        }

        /**
         * Creates a "cold" {@link Observable} that, when subscribed to, emits all entries on the feed
         * starting from those then on the head of the feed.
         * <p>The behavior is analogous to the method {@code observeFrom()} but starting from the head page</p>
         *
         * @param intervalInMs the polling interval in milliseconds.
         * @return a "cold" {@link Observable}
         */
        public Observable<FeedEntry<E>> observeFromNowOn(final int intervalInMs) {
            final ClientState state = new ClientState();
            return feedWrapperObservable(state, intervalInMs);
        }

        /**
         * Creates a "cold" {@link Observable} that, when subscribed to, emits all entries on the feed
         * starting from the begnning.
         * <p>Starting from the beginning means going to the 'last' page of the feed, and the bottom entry on that page, and working back
         * to the present.</p>
         * @param intervalInMs the polling interval in ms.
         * @return a "cold" {@link Observable}
         */
        public Observable<FeedEntry<E>> observeFromBeginning(final int intervalInMs) {
            return observeFromBeginning(intervalInMs, new ClientState());
        }

        private Observable<FeedEntry<E>> observeFromBeginning(final int intervalInMs, ClientState initialState) {
            return observableToLastPageLink()
                    .map(link -> {
                        final ClientState state = new ClientState();
                        state.lastSeenSelfHref = Optional.of(link);
                        return state;
                    })
                    .flatMap(state -> feedWrapperObservable(state, intervalInMs))
                    .onErrorResumeNext(t -> {
                        initialState.failedCount += 1;
                        try {
                            Long delay = retryStrategy.apply(initialState.failedCount, t);
                            if (delay != null) Thread.sleep(delay);
                            return observeFromBeginning(intervalInMs, initialState);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return Observable.error(e);
                        } catch (Exception e) {
                            return Observable.error(e);
                        }
                    });
        }

        /**
         * This is the core of the feed client
         * <p>
         * It creates a Scheduler.Worker that with the specified interval polls the feed, and retrieves all entries not
         * yet "seen". The ClientState object is used to keep track of the latest seen feed-pages, Etags and entry-id's.
         */
        private Observable<FeedEntry<E>> feedWrapperObservable(final ClientState state, final int intervalInMs) {
            Observable<FeedWrapper<E>> observableFeedPage = Observable.create((subscriber) -> {
                Scheduler.Worker worker = Schedulers.io().createWorker();
                schedulePeriodically(worker, () -> {
                    logger.debug("Scheduled work started");
                    String urlOfPageToRetrieve = state.lastSeenSelfHref.orElse("");
                    try {

                        while (!subscriber.isUnsubscribed() && urlOfPageToRetrieve != null) {
                            logger.debug("Start polling");
                            FeedWrapper<E> page = retrievePagePruned(state, urlOfPageToRetrieve);
                            notifySubscriberAndUpdateClientState(subscriber, page, state);
                            urlOfPageToRetrieve = page.getPreviousHref().orElse(null);
                        }

                        if (subscriber.isUnsubscribed()) {
                            logger.debug("Worker unsubscribe");
                            worker.unsubscribe();
                        }

                    } catch (Exception e) {
                        try {
                            state.failedCount = state.failedCount + 1;
                            logger.warn("Receiving error on retrieving: " + urlOfPageToRetrieve + ": " + e.getMessage() + " (" + e.getClass().getName() + ")");
                            logger.info("Setting failed count to:" + state.failedCount);
                            return retryStrategy.apply(state.failedCount, e);
                        } catch (Exception e2) {
                            subscriber.onError(e2);
                            worker.unsubscribe();
                            logger.debug("Worker unsubscribe after error");
                        }
                    }
                    logger.debug("Scheduled work finished.");
                    return Long.valueOf(intervalInMs);
                }, 0, TimeUnit.MILLISECONDS);
            });

            return observableFeedPage
                    .flatMap(feed ->
                                    Observable.from(feed.getEntries())
                                            .map(entry -> new FeedEntry<>(entry, feed))
                    );
        }

        private void notifySubscriberAndUpdateClientState(Subscriber<? super FeedWrapper<E>> subscriber, FeedWrapper<E> page, ClientState state) {
            if (!page.isEmpty()) {
                logger.debug("Emitting: " + page.getEntries());
                subscriber.onNext(page);
                state.lastSeenEtag = page.getEtag();
                state.lastSeenEntryId = Optional.of(page.getLastEntryId());
                state.lastSeenSelfHref = Optional.of(page.getSelfHref());
                //reset failed count and  delay between executions
                state.failedCount = 0;
                logger.debug("Setting lastseenSelfHref to :" + page.getSelfHref());
            } else {
                logger.info("Received Empty page (after pruning)");
            }
        }

        private FeedWrapper<E> retrievePagePruned(ClientState state, String url) {
            Optional<String> etag = Optional.empty();
            etag = state.lastSeenEtag;
            logger.debug("Retrieving page: " + url);
            Observable<FeedWrapper<E>> feedObservable = createFeedWrapperObservable(url, etag);
            return prune(feedObservable.toBlocking().last(), state);
        }

        /**
         * This is an adaptation of Scheduler.Worker#schedulePeriodically()
         * <p>
         * Schedules a cancelable action to be executed periodically. This implementation schedules
         * recursively and waits for actions to complete (instead of potentially executing long-running actions
         * concurrently). The delay time for the next execution is read out of the DelayHolder. This allows the action to modify
         * delay time
         * </p>
         */
        private Subscription schedulePeriodically(final Scheduler.Worker worker, final Func0<Long> action, long initialDelay, TimeUnit unit) {
            final long startInNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis()) + unit.toNanos(initialDelay);
            final MultipleAssignmentSubscription mas = new MultipleAssignmentSubscription();
            final Action0 recursiveAction = new Action0() {
                long count = 0;
                long nextTick = startInNanos;
                long delay = initialDelay;
                @Override
                public void call() {
                    if (!mas.isUnsubscribed()) {
                        delay  = action.call();
                        final long periodInNanos = unit.toNanos(delay);
                        nextTick = nextTick + periodInNanos;
                        long waitTime = nextTick - TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
                        logger.debug("Setting wait time to: " + TimeUnit.NANOSECONDS.toMillis(waitTime));
                        mas.set(worker.schedule(this, waitTime, TimeUnit.NANOSECONDS));
                    }
                }
            };
            mas.set(worker.schedule(recursiveAction, initialDelay, unit));
            return mas;
        }


        //** removes the entries in the feedWrapper that have already been seen.
        private static <T> FeedWrapper<T> prune(FeedWrapper<T> feedWrapper, ClientState state) {
            if (feedWrapper.isEmpty() || !state.lastSeenEntryId.isPresent()) return feedWrapper;
            if (!state.lastSeenSelfHref.isPresent() || !feedWrapper.getSelfHref().equals(state.lastSeenSelfHref.get())) return feedWrapper;
            List<Entry<T>> pruned = new ArrayList<>();
            boolean skip = true;
            for (Entry<T> entry : feedWrapper.getEntries()) {
                if (!skip) {
                    pruned.add(entry);
                } else if (entry.getId().equals(state.lastSeenEntryId.get())) {
                    skip = false;
                    logger.debug("Skipping entry: " + entry.getId());
                }
            }
            return new FeedWrapper<>(feedWrapper.getLinks(), pruned, feedWrapper.etag);
        }


        @SuppressWarnings("unchecked")
        private Observable<FeedWrapper<E>> createFeedWrapperObservable(String pageUrl, Optional<String> etag) {
            ClientRequest request = buildConditionalGet(pageUrl, etag);

            return rxHttpClient.executeToCompletion(request, resp -> {
                if (resp.getStatusCode() == 304) {
                    return new EmptyFeedWrapper<>(etag);
                }
                Optional<String> newETag = resp.getHeader("ETag");
                if (isJson(resp.getContentType())) {
                    return new FeedWrapper<>((FeedPage<E>) jsonCodec.decode(resp.getResponseBody()), newETag);
                } else {
                    return new FeedWrapper<>( xmlCodec.decode(resp.getResponseBody()), newETag);
                }
            });
        }

        private Observable<String> observableToLastPageLink() {
            ClientRequest request = buildConditionalGet("/", Optional.empty());
            return rxHttpClient.executeToCompletion(request, resp -> {
                        FeedWrapper<?> fw = null;
                        if (isJson(resp.getContentType())) {
                            fw = new FeedWrapper<>( jsonCodec.decode(resp.getResponseBody()), Optional.empty());
                        } else {
                            fw = new FeedWrapper<>( xmlCodec.decode(resp.getResponseBody()), Optional.empty());
                        }
                        return fw.getLastHref();
                    }
            );
        }

        private ClientRequest buildConditionalGet(String url, Optional<String> etag) {
            ClientRequestBuilder builder = rxHttpClient.requestBuilder().setMethod("GET");
            for (Map.Entry<String, String> header : extraHeaders.entrySet()) {
                builder.addHeader(header.getKey(), header.getValue());
            }
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


    }

    /**
     * A Builder for an AtomiumClient.
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
         * @param pooledConnectionIdleTimeout the maximum time in millisecond the client wil keep connection idle in pool
         * @return this {@link be.wegenenverkeer.rxhttp.RxHttpClient.Builder}
         */
        public Builder setPooledConnectionIdleTimeout(int pooledConnectionIdleTimeout) {
            rxHttpClientBuilder.setPooledConnectionIdleTimeout(pooledConnectionIdleTimeout);
            return this;
        }

        public AtomiumClient build() {
            return new AtomiumClient(rxHttpClientBuilder.build());
        }

        /**
         * Sets the Accept-header to user supplied string.
         *
         * @return this {@link be.wegenenverkeer.rxhttp.RxHttpClient.Builder}
         */
        public Builder setAccept(String accept) {
            rxHttpClientBuilder.setAccept(accept);
            return this;
        }

        /**
         * Sets the Accept-header to JSON.
         *
         * @return this {@link be.wegenenverkeer.rxhttp.RxHttpClient.Builder}
         */
        public Builder setAcceptJson() {
            rxHttpClientBuilder.setAccept(JSON_MIME_TYPE);
            return this;
        }

        /**
         * Sets the Accept-header to XML
         *
         * @return this {@link be.wegenenverkeer.rxhttp.RxHttpClient.Builder}
         */
        public Builder setAcceptXml() {
            rxHttpClientBuilder.setAccept(XML_MIME_TYPE);
            return this;
        }


        /**
         * Sets the base URL for this instance.
         *
         * @param url absolute URL where feeds are published
         * @return this {@link be.wegenenverkeer.rxhttp.RxHttpClient.Builder}
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

        /**
         * Set to true to enable HTTP redirect
         *
         * @param followRedirects if true redirects will be automatically followed
         * @return a {@link be.wegenenverkeer.rxhttp.RxHttpClient.Builder}
         */
        public Builder setFollowRedirect(boolean followRedirects) {
            rxHttpClientBuilder.setFollowRedirect(followRedirects);
            return this;
        }
    }

}
