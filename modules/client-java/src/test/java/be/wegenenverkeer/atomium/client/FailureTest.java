package be.wegenenverkeer.atomium.client;

import be.wegenenverkeer.atomium.client.rxhttpclient.RxHttpAtomiumClient;
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

import static be.wegenenverkeer.atomium.client.FeedPositionStrategies.fromNowOn;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;

public class FailureTest {

    private final static ClasspathFileSource WIREMOCK_MAPPINGS = new ClasspathFileSource("no-self-link-scenario");

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
    public void testReceivingAnError() {
        client.feed(client.getPageFetcherBuilder("/noselflinkfeed", Event.class)
                .setRetryStrategy((n, t) -> {
                     throw new FeedFetchException("Error", t);
                })
                .build())
                .fetchEntries(fromNowOn().withPollingDelay(Duration.ofMillis(100)))
                .take(10)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertError(FeedFetchException.class);
    }

}
