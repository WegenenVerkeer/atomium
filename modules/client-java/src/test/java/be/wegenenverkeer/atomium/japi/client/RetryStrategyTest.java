package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.rxhttpclient.HttpServerError;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class RetryStrategyTest {

    private final static ClasspathFileSource WIREMOCK_MAPPINGS = new ClasspathFileSource("retry-scenario");

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(
            wireMockConfig().fileSource(WIREMOCK_MAPPINGS)
    );

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    private AtomiumClient client;

    @Before
    public void before() {
        client = new RxHttpAtomiumClient.Builder()
                .setBaseUrl("http://localhost:8080/")
                .setAcceptJson()
                .build();

        //reset WireMock so it will serve the events feed
        WireMock.resetToDefault();
    }

    @After
    public void after() {
        client.close();
    }

    @Test
    public void testNoRetryStrategy() {
        client.feed("/feeds/events", Event.class)
                .fromBeginning()
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertError(FeedFetchException.class);
    }

    @Test
    public void testRetryStrategyOneRetries() {
        client.feed("/feeds/events", Event.class)
                .withRetry((n, t) -> {
                    if (n < 2) {
                        return 2 * n * 1000L;
                    } else {
                        throw new RuntimeException(t);
                    }
                })
                .fromBeginning()
                .test()
                .awaitDone(60, TimeUnit.SECONDS)
                .assertError(RuntimeException.class);
    }

    @Test
    public void testRetryStrategyThreeRetries() {
        client
                .feed("/feeds/events", Event.class)
                .withRetry((n, t) -> {
                    if (n < 3) {
                        return 2 * n * 1000L;
                    } else {
                        throw new RuntimeException(t);
                    }
                })
                .fromBeginning()
                .take(25)
                .test()
                .awaitDone(60, TimeUnit.SECONDS)
                .assertNoErrors();
    }
}
