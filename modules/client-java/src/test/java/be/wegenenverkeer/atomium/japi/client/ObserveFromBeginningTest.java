package be.wegenenverkeer.atomium.japi.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;

public class ObserveFromBeginningTest {

    private final static ClasspathFileSource WIREMOCK_MAPPINGS = new ClasspathFileSource("from-beginning-scenario");

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
    public void testSubscribingToObservableFromBeginning(){
        List<FeedEntry<Event>> entries = client.feed("/feeds/events", Event.class)
                .fromBeginning()
                .take(3)
                .test()
                .awaitCount(3)
                .assertNoErrors()
                .assertValueCount(3)
                .values();

        //we should have received entries with the self link 0/forward/0
        FeedEntry<Event> firstEntry = entries.get(0);
        assertEquals("0/forward/10", firstEntry.getSelfHref());
        assertEquals("urn:uuid:83aee39f-923d-451e-8ec4-d6333ba8999d", firstEntry.getEntry().getId());
    }
}
