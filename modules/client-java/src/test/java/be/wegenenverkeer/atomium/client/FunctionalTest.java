package be.wegenenverkeer.atomium.client;

import be.wegenenverkeer.atomium.client.rxhttpclient.RxHttpAtomiumClient;
import be.wegenenverkeer.rxhttpclient.rxjava.RxJavaHttpClient;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static be.wegenenverkeer.atomium.client.FeedPositionStrategies.from;
import static be.wegenenverkeer.atomium.client.FeedPositionStrategies.fromNowOn;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FunctionalTest {

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
    public void testSubscribingToObservable() {
        String startEntryId = "urn:uuid:8641f2fd-e8dc-4756-acf2-3b708080ea3a";
        String nextEntryId = "urn:uuid:e9b01f20-e294-4900-9cd2-484b25e07dc3";

        List<FeedEntry<Event>> values = client.feed(client.getPageFetcherBuilder("/feeds/events/", Event.class).setAcceptXml().build())
                .fetchEntries(from("20/forward/10", startEntryId))
                .take(20)
                .test()
                .awaitCount(20)
                .assertNoErrors()
                .assertValueCount(20)
                .values();

        Assert.assertEquals(nextEntryId, values.get(0).getEntry().getId());
    }

    @Test
    public void testReceivingAnError() {
        stubFor(get(urlEqualTo("/fault/"))
                .willReturn(aResponse().withStatus(500)));

        client.feed(client.getPageFetcherBuilder("/fault", Event.class).setAcceptXml()
                .setRetryStrategy((n, t) -> {
                    throw new FeedFetchException("Error", t);
                })
                .build())
                .fetchEntries(fromNowOn())
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertValueCount(0)
                .assertError(FeedFetchException.class);
    }

    @Test
    public void testUnSubscribingFromObservable() throws InterruptedException {
        Flowable<FeedEntry<Event>> flowable = client.feed(client.getPageFetcherBuilder("/feeds/events/", Event.class).setAcceptXml().build())
                .fetchEntries(from("20/forward/10", "urn:uuid:8641f2fd-e8dc-4756-acf2-3b708080ea3a"));

        TestObserver<FeedEntry<Event>> subscriber = new TestObserver<>();
        subscriber.onSubscribe(flowable.subscribe());

        //let the observable emit events for 100 ms.
        Thread.sleep(100);

        subscriber.dispose();
        assertTrue(subscriber.isDisposed());

        //give the AtomiumClient to handle unsubscribe event
        Thread.sleep(100);
    }

    @Test
    public void testFeedEntryHasSelfLink() {
        List<FeedEntry<Event>> events = client.feed(client.getPageFetcherBuilder("/feeds/events/", Event.class).setAcceptXml().build())
                .fetchEntries(from("20/forward/10", "urn:uuid:8641f2fd-e8dc-4756-acf2-3b708080ea3a"))
                .take(2)
                .test()
                .awaitCount(2)
                .assertNoErrors()
                .values();

        events.forEach(entry -> assertEquals("20/forward/10", entry.getSelfHref()));
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
                            return client.feed(client.getPageFetcherBuilder("/feeds/events/", Event.class).setAcceptXml().build())
                                    .fetchEntries(from(state.lastSeenPage, state.lastSeenId));
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
        observable
                .take(10)
                .test()
                .awaitCount(10)
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

