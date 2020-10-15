package be.wegenenverkeer.atomium.client;

import be.wegenenverkeer.atomium.client.rxhttpclient.RxHttpAtomiumClient;
import be.wegenenverkeer.rxhttpclient.HTTPStatusCode;
import be.wegenenverkeer.rxhttpclient.rxjava.RxJavaHttpClient;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static be.wegenenverkeer.atomium.client.FeedPositionStrategies.fromStart;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThan;
import static com.github.tomakehurst.wiremock.client.WireMock.resetToDefault;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class OnlyOnePageOnFeedTest {
    private final static Logger logger = LoggerFactory.getLogger(RetryStrategyTest.class);
//    private final static ClasspathFileSource WIREMOCK_MAPPINGS = new ClasspathFileSource("mini-scenario");

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(
            wireMockConfig().notifier(new Slf4jNotifier(true))
    );

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    private RxHttpAtomiumClient client;

    @Before
    public void before() throws Exception {
        client = new RxHttpAtomiumClient(new RxJavaHttpClient.Builder()
                .setBaseUrl("http://localhost:8080/")
                .build());

        //reset WireMock so it will serve the events feed
        resetToDefault();
        var body = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("mini-scenario/body-0-forward-10.json"));
        var bodyWithExtraEntry = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("mini-scenario/body-0-forward-10-next.json"));

        stubFor(get(urlPathEqualTo("/feeds/events/"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("ETag", "a7efe08ab6814170156ae94cce8c4fbd")
                        .withStatus(HTTPStatusCode.OK)
                        .withBody(body)
                ));

        stubFor(get(urlPathEqualTo("/feeds/events/0/forward/10"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("ETag", "a7efe08ab6814170156ae94cce8c4fbd")
                        .withStatus(HTTPStatusCode.OK)
                        .withBody(body)
                ))
                .setNewScenarioState("NO_NEW_ENTRIES");

        stubFor(get(urlPathEqualTo("/feeds/events/0/forward/10"))
                .inScenario("NO_NEW_ENTRIES")
                .withHeader("If-None-Match", matching(".*"))
                .willReturn(WireMock
                        .aResponse()
                        .withStatus(HTTPStatusCode.NotModified)
                ))
                .setNewScenarioState("ADD_NEW_ENTRY");

        stubFor(get(urlPathEqualTo("/feeds/events/0/forward/10"))
                .inScenario("ADD_NEW_ENTRY")
                .withHeader("If-None-Match", matching(".*"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("ETag", "fuu")
                        .withStatus(HTTPStatusCode.OK)
                        .withBody(bodyWithExtraEntry)
                ))
                .setNewScenarioState("NO_NEW_ENTRIES");
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
                .fetchEntries(fromStart().withPollingDelay(Duration.ofMillis(100)))
                .doOnNext(eventFeedEntry -> {
                    logger.debug("event feed entry found {}", eventFeedEntry);
                })
                .test()
                .awaitDone(2, TimeUnit.SECONDS)
                .assertValueCount(11);

        verify(exactly(1), getRequestedFor(urlPathEqualTo("/feeds/events/")).withoutHeader("If-None-Match")); // determine first feed position
        verify(exactly(1), getRequestedFor(urlPathEqualTo("/feeds/events/0/forward/10")).withoutHeader("If-None-Match")); // first fetch returning entries
        verify(exactly(1), getRequestedFor(urlPathEqualTo("/feeds/events/0/forward/10")).withHeader("If-None-Match", equalTo("a7efe08ab6814170156ae94cce8c4fbd"))); // next tries
        verify(moreThan(1), getRequestedFor(urlPathEqualTo("/feeds/events/0/forward/10")).withHeader("If-None-Match", equalTo("fuu"))); // next tries
    }
}
