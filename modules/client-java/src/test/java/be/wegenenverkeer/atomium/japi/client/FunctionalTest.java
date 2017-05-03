package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.rxhttp.HttpServerError;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.*;
import rx.Observable;
import rx.Subscription;
import rx.observers.TestSubscriber;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

import static org.junit.Assert.assertEquals;

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
    private Observable<Long> observable;

    @Before
    public void before(){
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
    public void testSubscribingToObservable(){
        Observable<FeedEntry<Event>> observable = client.feed("/feeds/events", Event.class).observeFrom("urn:uuid:8641f2fd-e8dc-4756-acf2-3b708080ea3a", "20/forward/10", 1000);


        TestSubscriber<FeedEntry<Event>> subscriber = new TestSubscriber<>();

        observable.take(20).subscribe(subscriber);

        subscriber.awaitTerminalEvent(30, TimeUnit.SECONDS);

        subscriber.assertNoErrors();

        //we should have received exactly 1000 events.
        assertEquals(20, subscriber.getOnNextEvents().size());

        //we should have received exactly 1 onComplete
        assertEquals(1, subscriber.getOnCompletedEvents().size());

    }

    @Test
    public void testReceivingAnError(){
        stubFor(get(urlEqualTo("/fault"))
                .willReturn(aResponse().withStatus(500)));

        Observable<FeedEntry<Event>> observable = client.feed("/fault", Event.class).observeFromNowOn(100);

        TestSubscriber<FeedEntry<Event>> subscriber = new TestSubscriber<>();

        observable.subscribe(subscriber);

        subscriber.awaitTerminalEvent(30, TimeUnit.SECONDS);

        assertEquals(0, subscriber.getOnNextEvents().size());
        assertEquals(1, subscriber.getOnErrorEvents().size());

        assertEquals(HttpServerError.class, subscriber.getOnErrorEvents().get(0).getClass());

    }

    @Test
    public void testUnSubscribingFromObservable() throws InterruptedException {

        Observable<FeedEntry<Event>> observable = client.feed("/feeds/events", Event.class).observeFrom("urn:uuid:8641f2fd-e8dc-4756-acf2-3b708080ea3a", "20/forward/10", 1000);


        TestSubscriber<FeedEntry<Event>> subscriber = new TestSubscriber<>();

        Subscription subscription = observable.subscribe(subscriber);

        //let the observable emit events for 100 ms.
        Thread.sleep(100);

        subscription.unsubscribe();

        subscriber.assertUnsubscribed();

        //give the AtomiumClient to handle unsubscribe event
        Thread.sleep(100);

    }

    @Test
    public void testFeedEntryHasSelfLink() throws InterruptedException {
        Observable<FeedEntry<Event>> observable = client.feed("/feeds/events", Event.class)
                .observeFrom("urn:uuid:8641f2fd-e8dc-4756-acf2-3b708080ea3a", "20/forward/10", 1000);


        TestSubscriber<FeedEntry<Event>> subscriber = new TestSubscriber<>();

        observable.take(2).subscribe(subscriber);

        subscriber.awaitTerminalEvent(30, TimeUnit.SECONDS);

        subscriber.assertNoErrors();

        List<FeedEntry<Event>> events = subscriber.getOnNextEvents();


        for (FeedEntry<Event> entry : events) {
            assertEquals("20/forward/10", entry.getSelfHref());
        }
    }


    //member variable
    private boolean failing = false;

    /**
     * This examples demonstrates who to use retryWhen for retrying the processing of events in case of process failure.
     *
     * Experimentation with very large failures revealed that the retryWhen operator does not grow the stack (in contrast to the
     * onErrorResumeNext operator)
     */
    @Test
    public void testRetryWhen() {

        //mutable state that simulates the database
        final PersistentState initState = new PersistentState("urn:uuid:8641f2fd-e8dc-4756-acf2-3b708080ea3a", "20/forward/10");

        Observable<FeedEntry<Event>> observable =
                Observable.just( initState ) // start with reading the state
                .flatMap(state -> {
                    System.out.println("Initing from " + state.toString());
                    return client.feed("/feeds/events", Event.class)
                            .observeFrom(state.lastSeenId, state.lastSeenPage, 1000);
                })
                .doOnNext( entry -> {
                    //process the event
                    //but every other attempt fails - simulate very unreliable commmunication or processing
                    if (failing) {
                        failing = false;
                        System.out.println("Failing:   " + entry.getEntry().getId());
                        throw new RuntimeException();
                    } else {
                        failing = true; // fail next time
                    }
                    System.out.println( "Processing " + entry.getEntry().getId());
                    // and remember the last seen position
                    initState.lastSeenId = entry.getEntry().getId();
                    initState.lastSeenPage = entry.getSelfHref();
                })
                .retryWhen(errors ->  // retry on error should ensure that the whole observable is restarted, which is includes restarting from the PersistentState
                        errors.zipWith(Observable.range(1, 3000), (n, i) -> i)
                        .flatMap(i -> {
                            System.out.println("Retrying on attempt " + i);
                            return Observable.timer(2, TimeUnit.MILLISECONDS);
                }));


        TestSubscriber<FeedEntry<Event>> subscriber = new TestSubscriber<>();

        //We only take 10 examples to make the point
        observable.take(10).subscribe(subscriber);

        subscriber.awaitTerminalEvent(10, TimeUnit.SECONDS);
        subscriber.assertNoErrors();
        assertEquals(10, subscriber.getOnNextEvents().size());
    }

    static class PersistentState {
        String lastSeenId;
        String lastSeenPage;

        PersistentState(String id, String page){
            lastSeenId = id;
            lastSeenPage = page;
        }

        public String toString(){
            return "id: " + lastSeenId + " on page: " + lastSeenPage;
        }
    }
}

