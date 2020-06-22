package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.atomium.japi.client.rxhttpclient.RxHttpAtomiumClient;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;

import static be.wegenenverkeer.atomium.japi.client.FeedPositionStrategies.from;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BackpressureTest {
    private final static ClasspathFileSource WIREMOCK_MAPPINGS = new ClasspathFileSource("basis-scenario");

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
        client = new RxHttpAtomiumClient.Builder()
                .setBaseUrl("http://localhost:8080/")
                .build();

        //reset WireMock so it will serve the events feed
        WireMock.resetToDefault();
    }

    @After
    public void after() {
        client.close();
    }

    @Test
    public void testProcessing_sameThread() {
        client.feed(client.getPageFetcherBuilder("/feeds/events", Event.class).setAcceptXml().build())
                .fetchEntries(from("20/forward/10", "urn:uuid:8641f2fd-e8dc-4756-acf2-3b708080ea3a"))
                .concatMap(event -> Flowable.just(event)
                        .doOnNext(myEvent -> Thread.sleep(Duration.ofSeconds(1).toMillis())))
                .test()
                .awaitCount(3)
                .assertNoErrors()
                .assertValueCount(3);

        // only 1 page is queried
        WireMock.verify(exactly(1), WireMock.getRequestedFor(WireMock.urlPathEqualTo("/feeds/events/20/forward/10")).withHeader("Accept", equalTo("application/xml")));
        WireMock.verify(exactly(0), WireMock.getRequestedFor(WireMock.urlPathEqualTo("/feeds/events/30/forward/10")).withHeader("Accept", equalTo("application/xml")));
        WireMock.verify(exactly(0), WireMock.getRequestedFor(WireMock.urlPathEqualTo("/feeds/events/40/forward/10")).withHeader("Accept", equalTo("application/xml")));
        WireMock.verify(exactly(0), WireMock.getRequestedFor(WireMock.urlPathEqualTo("/feeds/events/50/forward/10")).withHeader("Accept", equalTo("application/xml")));
    }
}

