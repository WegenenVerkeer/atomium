package be.wegenenverkeer.atomium.client;

import be.wegenenverkeer.atomium.client.rxhttpclient.RxHttpAtomiumClient;
import be.wegenenverkeer.rxhttpclient.rxjava.RxJavaHttpClient;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static be.wegenenverkeer.atomium.client.FeedPositionStrategies.from;
import static be.wegenenverkeer.atomium.client.FeedPositionStrategies.fromNowOn;
import static be.wegenenverkeer.atomium.client.FeedPositionStrategies.fromStart;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThan;
import static com.github.tomakehurst.wiremock.client.WireMock.resetToDefault;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class ETagTest {
    private final static Logger logger = LoggerFactory.getLogger(ETagTest.class);
    private final static ClasspathFileSource WIREMOCK_MAPPINGS = new ClasspathFileSource("from-beginning-scenario");

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(
            wireMockConfig()
                    .fileSource(WIREMOCK_MAPPINGS)
                    .notifier(new Slf4jNotifier(true))
    );

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    private RxHttpAtomiumClient client;

    @Before
    public void before() {
        client = new RxHttpAtomiumClient(new RxJavaHttpClient.Builder()
                .setBaseUrl("http://localhost:8080/")
                .build());

        //reset WireMock so it will serve the events feed
        resetToDefault();
    }

    @After
    public void after() {
        client.close();
    }

    @Test
    public void testClearETagOnHeadPage() {
        client.feed(client.getPageFetcherBuilder("/feeds/events/", Event.class)
                .setRetryStrategy((n, t) -> {
                    throw new FeedFetchException("Error", t);
                })
                .build())
                .fetchEntries(fromNowOn().withPollingDelay(Duration.ofMillis(100)))
                .take(10)
                .doOnNext(eventFeedEntry -> {
                    logger.debug("event feed entry found {}", eventFeedEntry.getEntry().getContent().getValue());
                })
                .test()
                .awaitDone(1, TimeUnit.SECONDS)
                .assertNoValues();

        verify(exactly(1), getRequestedFor(urlPathEqualTo("/feeds/events/")).withoutHeader("If-None-Match")); // determine first feed position
        verify(exactly(1), getRequestedFor(urlPathEqualTo("/feeds/events/30/forward/10")).withoutHeader("If-None-Match")); // first fetch returning entries
        verify(moreThan(1), getRequestedFor(urlPathEqualTo("/feeds/events/30/forward/10")).withHeader("If-None-Match", equalTo("a7efe08ab6814170156ae94cce8c4fbd"))); // next tries
    }

    @Test
    public void testClearETagOnHeadPage_from() {
        client.feed(client.getPageFetcherBuilder("/feeds/events/", Event.class)
                .setRetryStrategy((n, t) -> {
                    throw new FeedFetchException("Error", t);
                })
                .build())
                .fetchEntries(from("/30/forward/10", "urn:uuid:af399659-424f-4c07-b07b-a5338c69aaf3").withPollingDelay(Duration.ofMillis(100)))
                .take(10)
                .doOnNext(eventFeedEntry -> {
                    logger.debug("event feed entry found {}", eventFeedEntry.getEntry().getContent().getValue());
                })
                .test()
                .awaitDone(1, TimeUnit.SECONDS)
                .assertValueCount(2);

        verify(exactly(1), getRequestedFor(urlPathEqualTo("/feeds/events/")).withoutHeader("If-None-Match")); // determine first feed position
        verify(exactly(1), getRequestedFor(urlPathEqualTo("/feeds/events/30/forward/10")).withoutHeader("If-None-Match")); // first fetch returning entries
        verify(moreThan(1), getRequestedFor(urlPathEqualTo("/feeds/events/30/forward/10")).withHeader("If-None-Match", equalTo("a7efe08ab6814170156ae94cce8c4fbd"))); // next tries
    }

    @Test
    public void testNoETagsWhenChangingURLs() {
        client.feed(client.getPageFetcherBuilder("/feeds/events/", Event.class)
                .setRetryStrategy((n, t) -> {
                    throw new FeedFetchException("Error", t);
                })
                .build())
                .fetchEntries(fromStart().withPollingDelay(Duration.ofMillis(100)))
                .take(20)
                .test()
                .awaitDone(1, TimeUnit.SECONDS)
                .assertValueCount(20);

        verify(exactly(1), getRequestedFor(urlPathEqualTo("/feeds/events/")).withoutHeader("If-None-Match"));
        verify(exactly(1), getRequestedFor(urlPathEqualTo("/feeds/events/0/forward/10")).withoutHeader("If-None-Match"));
        verify(exactly(1), getRequestedFor(urlPathEqualTo("/feeds/events/10/forward/10")).withoutHeader("If-None-Match"));
    }
}
