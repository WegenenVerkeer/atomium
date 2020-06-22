package be.wegenenverkeer.atomium.japi.client;

import com.github.tomakehurst.wiremock.client.WireMock;
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

import java.util.concurrent.TimeUnit;

import static be.wegenenverkeer.atomium.japi.client.FeedPositionStrategies.fromStart;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;

public class RetryStrategyTest {
    final private static Logger logger = LoggerFactory.getLogger(RetryStrategyTest.class);

    private final static ClasspathFileSource WIREMOCK_MAPPINGS = new ClasspathFileSource("retry-scenario");

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(
            wireMockConfig()
                    .fileSource(WIREMOCK_MAPPINGS)
                    .notifier(new Slf4jNotifier(true))
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
                .fetchEntries(fromStart())
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertError(FeedFetchException.class);
    }

    @Test
    public void testRetryStrategyOneRetries() {
        client.feed("/feeds/events", Event.class)
                .withRetry((n, t) -> {
                    if (n < 2) {
                        logger.info("Retry request count " + n, t);
                        return 2 * n * 200L;
                    } else {
                        logger.info(format("Stop retrying after %d times.", n), t);
                        throw new IllegalStateException(t);
                    }
                })
                .fetchEntries(fromStart())
                .test()
                .awaitDone(60, TimeUnit.SECONDS)
                .assertError(IllegalStateException.class);
    }

    @Test
    public void testRetryStrategyThreeRetries() {
        client
                .feed("/feeds/events", Event.class)
                .withRetry((n, t) -> {
                    if (n < 3) {
                        logger.info(format("Retry request count %d", n), t);
                        return 2 * n * 200L;
                    } else {
                        logger.info(format("Stop retrying after %d times.", n), t);
                        throw new IllegalStateException(t);
                    }
                })
                .fetchEntries(fromStart())
                .take(25)
                .test()
                .awaitDone(60, TimeUnit.SECONDS)
                .assertNoErrors();
    }
}
