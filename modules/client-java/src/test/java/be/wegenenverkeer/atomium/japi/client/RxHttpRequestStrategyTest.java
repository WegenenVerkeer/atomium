package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.atomium.japi.client.rxhttpclient.RxHttpAtomiumClient;
import be.wegenenverkeer.rxhttpclient.rxjava.RxJavaHttpClient;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static be.wegenenverkeer.atomium.japi.client.FeedPositionStrategies.fromStart;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class RxHttpRequestStrategyTest {

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
        WireMock.resetToDefault();
    }

    @After
    public void after() {
        client.close();
    }

    @Test
    public void testAddHeaders() {
        client.feed(client.getPageFetcherBuilder("/feeds/events", Event.class)
                .setRequestStrategy(builder -> builder.addHeader("X-FOO", "bar"))
                .build())
                .fetchEntries(fromStart().withPollingDelay(Duration.ofMillis(100)))
                .take(15) // process 2 pages
                .test()
                .awaitDone(1, TimeUnit.SECONDS)
                .assertNoErrors()
                .assertValueCount(15)
                .values();

        WireMock.verify(exactly(1), WireMock.getRequestedFor(WireMock.urlPathEqualTo("/feeds/events/")).withHeader("X-FOO", equalTo("bar")));
    }
}
