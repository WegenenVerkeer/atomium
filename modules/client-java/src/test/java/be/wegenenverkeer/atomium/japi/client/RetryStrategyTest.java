package be.wegenenverkeer.atomium.japi.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.*;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;

/**
 * Created by Karel Maesen, Geovise BVBA on 28/10/15.
 */
public class RetryStrategyTest {

    private final static SingleRootFileSource WIREMOCK_MAPPINGS = new SingleRootFileSource
            ("modules/client-java/src/test/resources/retry-scenario");

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(
            wireMockConfig().fileSource(WIREMOCK_MAPPINGS)
    );

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    AtomiumClient client;

    @Before
    public void before() {
        client = new AtomiumClient.Builder()
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

        Observable<FeedEntry<Event>> observable = client.feed("/feeds/events", Event.class)
                .observeFromBeginning(1000);

        TestSubscriber<FeedEntry<Event>> subscriber = new TestSubscriber<>();

        observable.subscribe(subscriber);

        subscriber.awaitTerminalEvent(60, TimeUnit.SECONDS);

        assertEquals(1, subscriber.getOnErrorEvents().size());

    }


    @Test
    public void testRetryStrategyOneRetries() {

        Observable<FeedEntry<Event>> observable = client
                .feed("/feeds/events", Event.class)
                .withRetry((n, t) -> {
                    if (n < 2) {
                        return 1000L;
                    } else throw new RuntimeException(t);
                })
                .observeFromBeginning(1000);

        TestSubscriber<FeedEntry<Event>> subscriber = new TestSubscriber<>();

        observable.subscribe(subscriber);

        subscriber.awaitTerminalEvent(60, TimeUnit.SECONDS);

        assertEquals(1, subscriber.getOnErrorEvents().size());

    }

    @Test
    public void testretryStrategyThreeRetries() {

        Observable<FeedEntry<Event>> observable = client
                .feed("/feeds/events", Event.class)
                .withRetry((n, t) -> {
                    if (n < 3) {
                        return 1000L;
                    } else throw new RuntimeException(t);
                })
                .observeFromBeginning(1000).take(25);

        TestSubscriber<FeedEntry<Event>> subscriber = new TestSubscriber<>();

        observable.subscribe(subscriber);

        subscriber.awaitTerminalEvent(60, TimeUnit.SECONDS);

        subscriber.assertNoErrors();

    }

}
