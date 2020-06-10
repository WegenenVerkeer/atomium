package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.rxhttpclient.HttpClientError;
import be.wegenenverkeer.rxhttpclient.HttpServerError;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.*;
import org.reactivestreams.Subscription;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Karel Maesen, Geovise BVBA on 16/03/15.
 */
public class FunctionalTest {

    private final static SingleRootFileSource WIREMOCK_MAPPINGS = new SingleRootFileSource
            ("modules/client-java/src/test/resources/basis-scenario");

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
                .setAcceptXml()
                .build();

        //reset WireMock so it will serve the events feed
        WireMock.resetToDefault();
    }

    @After
    public void after() {
        client.close();
    }


    @Test
    public void testSubscribingToObservable() {
        client.feed("/feeds/events", Event.class)
                .observeFrom("urn:uuid:8641f2fd-e8dc-4756-acf2-3b708080ea3a", "20/forward/10", 1000)
                .take(20)
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertNoErrors()
                .assertValueCount(20)
                .assertComplete();
    }

    @Test
    public void testReceivingAnError() {
        stubFor(get(urlEqualTo("/fault"))
                .willReturn(aResponse().withStatus(500)));

        client.feed("/fault", Event.class)
                .observeFromNowOn(100)
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertValueCount(0)
                .assertError(HttpServerError.class);
    }

    @Test
    public void testUnSubscribingFromObservable() throws InterruptedException {
        boolean isUnsubscribed = client.feed("/feeds/events", Event.class)
                .observeFrom("urn:uuid:8641f2fd-e8dc-4756-acf2-3b708080ea3a", "20/forward/10", 1000)
                .test()
                .await(100, TimeUnit.MILLISECONDS);

        assertTrue(isUnsubscribed);
    }

    @Test
    public void testFeedEntryHasSelfLink() throws InterruptedException {
        List<FeedEntry<Event>> events = client.feed("/feeds/events", Event.class)
                .observeFrom("urn:uuid:8641f2fd-e8dc-4756-acf2-3b708080ea3a", "20/forward/10", 1000)
                .take(2)
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertNoErrors()
                .values();

        for (FeedEntry<Event> entry : events) {
            assertEquals("20/forward/10", entry.getSelfHref());
        }
    }


    //member variable
    private boolean failing = false;

    /**
     * This examples demonstrates who to use retryWhen for retrying the processing of events in case of process failure.
     * <p>
     * Experimentation with very large failures revealed that the retryWhen operator does not grow the stack (in contrast to the
     * onErrorResumeNext operator)
     */
    @Test
    public void testRetryWhen() {

        //mutable state that simulates the database
        final PersistentState initState = new PersistentState("urn:uuid:8641f2fd-e8dc-4756-acf2-3b708080ea3a", "20/forward/10");

        Flowable<FeedEntry<Event>> observable =
                Flowable.just(initState) // start with reading the state
                        .flatMap(state -> {
                            System.out.println("Initing from " + state.toString());
                            return client.feed("/feeds/events", Event.class)
                                    .observeFrom(state.lastSeenId, state.lastSeenPage, 1000);
                        })
                        .doOnNext(entry -> {
                            //process the event
                            //but every other attempt fails - simulate very unreliable commmunication or processing
                            if (failing) {
                                failing = false;
                                System.out.println("Failing:   " + entry.getEntry().getId());
                                throw new RuntimeException();
                            } else {
                                failing = true; // fail next time
                            }
                            System.out.println("Processing " + entry.getEntry().getId());
                            // and remember the last seen position
                            initState.lastSeenId = entry.getEntry().getId();
                            initState.lastSeenPage = entry.getSelfHref();
                        })
                        .retryWhen(errors ->  // retry on error should ensure that the whole observable is restarted, which is includes restarting from the PersistentState
                                errors.zipWith(Flowable.range(1, 3000), (n, i) -> i)
                                        .flatMap(i -> {
                                            System.out.println("Retrying on attempt " + i);
                                            return Flowable.timer(2, TimeUnit.MILLISECONDS);
                                        }));

        //We only take 10 examples to make the point
        observable.take(10)
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertNoErrors()
                .assertValueCount(10);
    }

    static class PersistentState {
        String lastSeenId;
        String lastSeenPage;

        PersistentState(String id, String page) {
            lastSeenId = id;
            lastSeenPage = page;
        }

        public String toString() {
            return "id: " + lastSeenId + " on page: " + lastSeenPage;
        }
    }
}

