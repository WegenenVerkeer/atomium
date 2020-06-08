package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.atomium.format.Entry;
import be.wegenenverkeer.rxhttpclient.RxHttpClient;
import be.wegenenverkeer.rxhttpclient.rxjava.RxJavaHttpClient;
import com.fasterxml.jackson.databind.Module;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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
    private final RetryStrategy retryStrategy;
    private final Map<String, String> extraHeaders;
    /**
     * Creates an AtomiumClient from the specified {@code RxHttpClient} instance
     *
     * @param rxHttpClient the HTTP client
     * @param retryStrategy
     * @param extraHeaders
     */
    public AtomiumClient(RxHttpClient rxHttpClient, RetryStrategy retryStrategy, Map<String, String> extraHeaders) {
        this.rxHttpClient = rxHttpClient;
        this.retryStrategy = retryStrategy;
        this.extraHeaders = extraHeaders;
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
    public <E> AtomiumFeed<E> feed(String feedPath, Class<E> entryTypeMarker, Module... modules) {
        PageFetcher fetcher = new PageFetcher(feedPath, entryTypeMarker, rxHttpClient, extraHeaders, modules);
        return new AtomiumFeed<>(fetcher);
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
    public static class AtomiumFeed<E> {
        private final PageFetcher<E> fetcher;

        AtomiumFeed(PageFetcher<E> fetcher) {
            this.fetcher = fetcher;
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
         * <p>When subscribed to, the observable will:</p>
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
        public Flowable<FeedEntry<E>> observeFrom(final String entryId, final String pageUrl, final int intervalInMs) {
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
        public Flowable<FeedEntry<E>> observeSince(final String entryId, final String pageUrl, final int intervalInMs) {
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
        public Flowable<FeedEntry<E>> observeFromNowOn(final int intervalInMs) {
            final ClientState state = new ClientState();
            return feedWrapperObservable(state, intervalInMs);
        }

//        /**
//         * Creates a "cold" {@link Observable} that, when subscribed to, emits all entries on the feed
//         * starting from the begnning.
//         * <p>Starting from the beginning means going to the 'last' page of the feed, and the bottom entry on that page, and working back
//         * to the present.</p>
//         * @param intervalInMs the polling interval in ms.
//         * @return a "cold" {@link Observable}
//         */
//        public Flowable<FeedEntry<E>> observeFromBeginning(final int intervalInMs) {
//            return feedWrapperObservable(intervalInMs, new ClientState());
//        }

//        private Flowable<FeedEntry<E>> observeFromBeginning(final int intervalInMs, ClientState initialState) {
//            return observableToLastPageLink()
//                    .map(link -> {
//                        final ClientState state = new ClientState();
//                        state.lastSeenSelfHref = Optional.of(link);
//                        return state;
//                    })
//                    .flatMap(state -> fetchEntries(state, intervalInMs))
//                    .onErrorResumeNext(t -> {
//                        initialState.failedCount += 1;
//                        try {
//                            Long delay = retryStrategy.apply(initialState.failedCount, t);
//                            if (delay != null) Thread.sleep(delay);
//                            return observeFromBeginning(intervalInMs, initialState);
//                        } catch (InterruptedException e) {
//                            Thread.currentThread().interrupt();
//                            return Observable.error(e);
//                        } catch (Exception e) {
//                            return Observable.error(e);
//                        }
//                    });
//        }

        /**
         * This is the core of the feed client
         * <p>
         * It creates a Scheduler.Worker that with the specified interval polls the feed, and retrieves all entries not
         * yet "seen". The ClientState object is used to keep track of the latest seen feed-pages, Etags and entry-id's.
         */
        private Flowable<FeedEntry<E>> feedWrapperObservable(final ClientState state, final int intervalInMs) {
            String pageUrl = state.lastSeenSelfHref.orElse("");
            return getPagePruned(state, pageUrl).concatMap(page -> {
                updateClientState(page, state);
                Optional<String> previousPageUrl = page.getPreviousHref();
                if (previousPageUrl.isPresent()) {
                    return getPagePruned(state, previousPageUrl.get());
                } else {
                    return Single.timer(intervalInMs, TimeUnit.MILLISECONDS).concatMap(t -> getPagePruned(state, pageUrl));
                }
            }).flatMapPublisher(page -> Flowable.fromIterable(page.getEntries()).map(e -> new FeedEntry<E>(e, page)));
        }

        private void updateClientState(EtaggedFeedPage<E> page, ClientState state) {
            if (!page.isEmpty()) {
                logger.debug("Emitting: " + page.getEntries());
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

        private Single<EtaggedFeedPage<E>> getPagePruned(ClientState state, String url) {
            Optional<String> etag = Optional.empty();
            etag = state.lastSeenEtag;
            logger.debug("Retrieving page: " + url);
            Pruner<E> pruner = new Pruner<>(state);
            return fetcher.getPage(url, etag).map(pruner::prune);
        }




    }


    private static class Pruner<T>{
        final AtomiumFeed.ClientState state;
        Pruner(AtomiumFeed.ClientState state){
            this.state = state;
        }
        EtaggedFeedPage<T> prune(EtaggedFeedPage<T> page) {
            if (page.isEmpty() || !state.lastSeenEntryId.isPresent()) return page;
            if (!state.lastSeenSelfHref.isPresent() || !page.getSelfHref().equals(state.lastSeenSelfHref.get())) return page;
            List<Entry<T>> pruned = new ArrayList<>();
            boolean skip = true;
            for (Entry<T> entry : page.getEntries()) {
                if (!skip) {
                    pruned.add(entry);
                } else if (entry.getId().equals(state.lastSeenEntryId.get())) {
                    skip = false;
                    logger.debug("Skipping entry: " + entry.getId());
                }
            }
            return new EtaggedFeedPage<>(page.getLinks(), pruned, page.etag);
        }
    }

    /**
     * A Builder for an AtomiumClient.
     */
    public static class Builder {

        private final String JSON_MIME_TYPE = "application/json"; //TODO -- change to "official" AtomPub mimetypes
        private final String XML_MIME_TYPE = "application/xml";
        final private RxJavaHttpClient.Builder rxHttpClientBuilder = new RxJavaHttpClient.Builder();
        private RetryStrategy retryStrategy;
        private Map<String, String> extraHeaders;

        public Builder() {
            //default builder state
        }

        /**
         * Set the maximum time in millisecond a Client will keep connection
         * idle in pool.
         *
         * @param pooledConnectionIdleTimeout the maximum time in millisecond the client wil keep connection idle in pool
         * @return this Builder
         */
        public Builder setPooledConnectionIdleTimeout(int pooledConnectionIdleTimeout) {
            rxHttpClientBuilder.setPooledConnectionIdleTimeout(pooledConnectionIdleTimeout);
            return this;
        }

        public AtomiumClient build() {
            return new AtomiumClient(rxHttpClientBuilder.build(), retryStrategy, extraHeaders);
        }

        /**
         * Sets the Accept-header to user supplied string.
         *
         * @return this Builder
         */
        public Builder setAccept(String accept) {
            rxHttpClientBuilder.setAccept(accept);
            return this;
        }

        /**
         * Sets the Accept-header to JSON.
         *
         * @return this Builder
         */
        public Builder setAcceptJson() {
            rxHttpClientBuilder.setAccept(JSON_MIME_TYPE);
            return this;
        }

        /**
         * Sets the Accept-header to XML
         *
         * @return this Builder
         */
        public Builder setAcceptXml() {
            rxHttpClientBuilder.setAccept(XML_MIME_TYPE);
            return this;
        }


        /**
         * Sets the base URL for this instance.
         *
         * @param url absolute URL where feeds are published
         * @return this Builder
         */
        public Builder setBaseUrl(String url) {
            rxHttpClientBuilder.setBaseUrl(url);
            return this;
        }

        /**
         * Set the maximum number of connections a Client can handle.
         *
         * @param maxConnections the maximum number of connections a Client
         *                       can handle.
         * @return a Builder
         */
        public Builder setMaxConnections(int maxConnections) {
            rxHttpClientBuilder.setMaxConnections(maxConnections);
            return this;
        }

        /**
         * Set true if connection can be pooled by a ChannelPool. Default is true.
         *
         * @param allowPoolingConnections true if connection can be pooled by a ChannelPool
         * @return a Builder
         */
        public Builder setAllowPoolingConnections(boolean allowPoolingConnections) {
            rxHttpClientBuilder.setAllowPoolingConnections(allowPoolingConnections);
            return this;
        }

        /**
         * Set the maximum time in millisecond a Client can wait when
         * connecting to a remote host
         *
         * @param connectTimeOut the maximum time in millisecond a Client can
         *                       wait when connecting to a remote host
         * @return a Builder
         */
        public Builder setConnectTimeout(int connectTimeOut) {
            rxHttpClientBuilder.setConnectTimeout(connectTimeOut);
            return this;
        }

//        /**
//         * Set the {@link java.util.concurrent.ExecutorService} a Client use
//         * for handling
//         * asynchronous response.
//         *
//         * @param applicationThreadPool the {@link java.util.concurrent.ExecutorService} an {@link com.ning.http
//         *                              .client.AsyncHttpClient} use for handling
//         *                              asynchronous response.
//         * @return a Builder
//         */
//        public Builder setExecutorService(ExecutorService applicationThreadPool) {
//            rxHttpClientBuilder.setExecutorService(applicationThreadPool);
//            return this;
//        }

        public Builder setFollowRedirect(boolean followRedirects) {
            rxHttpClientBuilder.setFollowRedirect(followRedirects);
            return this;
        }

        public Builder withRetry(RetryStrategy strategy) {
            retryStrategy = strategy;
            return this;
        }

        public Builder withExtraHeaders(Map<String, String> headers) {
            extraHeaders = headers;
            return this;
        }

    }

}
