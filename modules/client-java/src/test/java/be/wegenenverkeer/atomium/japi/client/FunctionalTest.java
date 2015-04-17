package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.atomium.japi.format.Entry;
import be.wegenenverkeer.rxhttp.HttpServerError;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.*;
import rx.Observable;
import rx.Subscription;
import rx.observers.TestSubscriber;

import javax.xml.bind.annotation.*;
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
        Observable<Entry<Event>> observable = client.feed("/feeds/events", Event.class).observeSince("urn:uuid:8641f2fd-e8dc-4756-acf2-3b708080ea3a", "20/forward/10", 1000);


        TestSubscriber<Entry<Event>> subscriber = new TestSubscriber<>();

        observable.take(1000).subscribe(subscriber);

        subscriber.awaitTerminalEvent(3, TimeUnit.SECONDS);

        subscriber.assertNoErrors();

        //we should have received exactly 1000 events.
        assertEquals(1000, subscriber.getOnNextEvents().size());

        //we should have received exactly 1 onComplete
        assertEquals(1, subscriber.getOnCompletedEvents().size());

    }

    @Test
    public void testReceivingAnError(){
        stubFor(get(urlEqualTo("/fault"))
                .willReturn(aResponse().withStatus(500)));

        Observable<Entry<Event>> observable = client.feed("/fault", Event.class).observe(100);

        TestSubscriber<Entry<Event>> subscriber = new TestSubscriber<>();

        observable.subscribe(subscriber);

        subscriber.awaitTerminalEvent(1, TimeUnit.SECONDS);

        assertEquals(0, subscriber.getOnNextEvents().size());
        assertEquals(1, subscriber.getOnErrorEvents().size());

        assertEquals(HttpServerError.class, subscriber.getOnErrorEvents().get(0).getClass());

    }

    @Test
    public void testUnSubscribingFromObservable() throws InterruptedException {

        Observable<Entry<Event>> observable = client.feed("/feeds/events", Event.class).observeSince("urn:uuid:8641f2fd-e8dc-4756-acf2-3b708080ea3a", "20/forward/10", 1000);


        TestSubscriber<Entry<Event>> subscriber = new TestSubscriber<>();

        Subscription subscription = observable.subscribe(subscriber);

        //let the observable emit events for 100 ms.
        Thread.sleep(100);

        subscription.unsubscribe();

        subscriber.assertUnsubscribed();

        //give the AtomiumClient to handle unsubscribe event
        Thread.sleep(100);

    }


}




// this is the Event model, annotations for XML deserialization
// and public accessors for JSON
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
class Event {

    @XmlElement
    public Double value;

    @XmlElement
    public String description;

    @XmlAttribute
    public Integer version;

    public Event(){
    }


    public String toString() {
        return "Event " + version + " " + "description " + " value: " + value;
    }

}
