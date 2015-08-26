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
 * Created by Karel Maesen, Geovise BVBA on 20/08/15.
 */
public class FailureTest {

    private final static SingleRootFileSource WIREMOCK_MAPPINGS =
            new SingleRootFileSource("modules/client-java/src/test/resources/no-self-link-scenario");

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
    public void testReceivingAnError() {
        Observable<FeedEntry<Event>> observable = client.feed("/noselflinkfeed", Event.class).observeFromNowOn(100)
                .take(10);

        TestSubscriber<FeedEntry<Event>> subscriber = new TestSubscriber<>();

        observable.subscribe(subscriber);

        subscriber.awaitTerminalEvent(5, TimeUnit.SECONDS);

        assertEquals(1, subscriber.getOnErrorEvents().size());
        Throwable receivedError = subscriber.getOnErrorEvents().get(0);
        assertEquals(IllegalStateException.class, receivedError.getClass());


    }

}
