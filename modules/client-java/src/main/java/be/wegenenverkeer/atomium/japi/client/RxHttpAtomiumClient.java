package be.wegenenverkeer.atomium.japi.client;

import java.time.Duration;

import be.wegenenverkeer.rxhttpclient.RxHttpClient;
import be.wegenenverkeer.rxhttpclient.rxjava.RxJavaHttpClient;
import com.fasterxml.jackson.databind.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * A client for Atomium AtomPub feeds.
 *
 * <p>It is best-practice to create a single AtomiumClient for all feeds on a specific host.</p>
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 16/03/15.
 */
public class RxHttpAtomiumClient implements AtomiumClient {

    private final static Logger logger = LoggerFactory.getLogger(RxHttpAtomiumClient.class);
    private final List<PageFetcher<?>> pageFetchers = new ArrayList<>();
    private final RxHttpClient rxHttpClient;
    private final Duration pollingInterval;
    private final Callable<Map<String, String>> extraHeaders;
    private final List<Module> extraModules;
    private final RetryStrategy retryStrategy;

    /**
     * Creates an AtomiumClient from the specified {@code PageFetcher} instance
     */
    RxHttpAtomiumClient(RxHttpClient rxHttpClient, Duration pollingInterval, Callable<Map<String, String>> extraHeaders, List<Module> extraModules,
            RetryStrategy retryStrategy) {
        this.rxHttpClient = rxHttpClient;
        this.pollingInterval = pollingInterval;
        this.extraHeaders = extraHeaders;
        this.extraModules = extraModules;
        this.retryStrategy = retryStrategy;
    }

    @Override
    public <E> AtomiumFeed<E> feed(String feedUrl, Class<E> entryTypeMarker) {
        PageFetcher<E> pageFetcher = buildPageFetcher(feedUrl, entryTypeMarker);
        pageFetchers.add(pageFetcher);
        return new AtomiumFeedImpl<>(pageFetcher);
    }

    <E> PageFetcher<E> buildPageFetcher(String feedUrl, Class<E> entryTypeMarker) {
        RxHttpPageFetcherConfiguration<E> configuration = new RxHttpPageFetcherConfiguration<>();
        configuration.setRxHttpClient(rxHttpClient);
        configuration.setExtraHeaders(extraHeaders);
        configuration.setExtraModules(extraModules);
        configuration.setEntryTypeMarker(entryTypeMarker);
        configuration.setFeedUrl(feedUrl);
        configuration.setPollingInterval(pollingInterval);
        return new RxHttpPageFetcher<>(configuration);
    }

    public void close() {
        this.pageFetchers.forEach(PageFetcher::close);
    }

    public static class Builder {
        private final String JSON_MIME_TYPE = "application/json"; //TODO -- change to "official" AtomPub mimetypes
        private final String XML_MIME_TYPE = "application/xml";
        private String accept;
        private String baseUrl;
        private boolean followRedirect;
        private Duration pollingInterval = Duration.ofSeconds(1);
        private Callable<Map<String, String>> extraHeaders = Collections::emptyMap;
        private RetryStrategy retryStrategy = (count, exception) -> {
            throw new FeedFetchException("Error occured, exiting...", exception);
        };

        private List<Module> extraModules = new ArrayList<>();

        public Builder() {
        }

        public AtomiumClient build() {
            RxHttpClient rxHttpClient = new RxJavaHttpClient.Builder()
                    .setAccept(accept)
                    .setBaseUrl(baseUrl)
                    .setFollowRedirect(followRedirect)
                    .build();

            return new RxHttpAtomiumClient(rxHttpClient, pollingInterval, extraHeaders, extraModules, retryStrategy);
        }

        public Builder setAccept(String accept) {
            this.accept = accept;
            return this;
        }

        public Builder setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder setAcceptJson() {
            this.accept = JSON_MIME_TYPE;
            return this;
        }

        public Builder setAcceptXml() {
            this.accept = XML_MIME_TYPE;
            return this;
        }

        public Builder setFollowRedirect(boolean followRedirect) {
            this.followRedirect = followRedirect;
            return this;
        }

        public Builder setExtraHeaders(Callable<Map<String, String>> extraHeaders) {
            this.extraHeaders = extraHeaders;
            return this;
        }

        public Builder setExtraModules(List<Module> extraModules) {
            this.extraModules = extraModules;
            return this;
        }

        public Builder setPollingInterval(Duration pollingInterval) {
            this.pollingInterval = pollingInterval;
            return this;
        }
    }
}
