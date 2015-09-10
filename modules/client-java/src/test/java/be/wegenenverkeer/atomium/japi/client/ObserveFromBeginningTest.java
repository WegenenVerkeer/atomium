package be.wegenenverkeer.atomium.japi.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.*;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;

/**
 * Created by Karel Maesen, Geovise BVBA on 26/08/15.
 */
public class ObserveFromBeginningTest {


    private final static SingleRootFileSource WIREMOCK_MAPPINGS = new SingleRootFileSource
            ("modules/client-java/src/test/resources/from-beginning-scenario");

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(
            wireMockConfig().fileSource(WIREMOCK_MAPPINGS)
    );

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    AtomiumClient client;

    @Before
    public void before(){
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
    public void testSubscribingToObservableFromBeginning(){
        Observable<FeedEntry<Event>> observable = client.feed("/feeds/events", Event.class).observeFromBeginning(1000);

        TestSubscriber<FeedEntry<Event>> subscriber = new TestSubscriber<>();

        observable.take(3).subscribe(subscriber);

        subscriber.awaitTerminalEvent(60, TimeUnit.SECONDS);

        subscriber.assertNoErrors();

        //we should have received entries with the self link 0/forward/0
        List<FeedEntry<Event>> onNextEvents = subscriber.getOnNextEvents();
        FeedEntry<Event> firstEntry = onNextEvents.get(0);
        assertEquals("0/forward/10", firstEntry.getSelfHref());
        assertEquals("urn:uuid:83aee39f-923d-451e-8ec4-d6333ba8999d", firstEntry.getEntry().getId());

    }


}
