package be.wegenenverkeer.atomium.japi.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static be.wegenenverkeer.atomium.japi.client.FeedPositionStrategies.from;
import static be.wegenenverkeer.atomium.japi.client.FeedPositionStrategies.fromNowOn;
import static be.wegenenverkeer.atomium.japi.client.FeedPositionStrategies.fromStart;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;

public class ObserveFromTest {

    private final static ClasspathFileSource WIREMOCK_MAPPINGS = new ClasspathFileSource("from-beginning-scenario");

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
    public void testSubscribingFromBeginning() {
        List<FeedEntry<Event>> entries = client.feed("/feeds/events", Event.class)
                .fetchEntries(fromStart())
                .take(15) // process 2 pages
                .test()
                .awaitDone(3, TimeUnit.SECONDS)
                .assertNoErrors()
                .assertValueCount(15)
                .values();

        //we should have received entries with the self link 0/forward/0
        FeedEntry<Event> firstEntry = entries.get(0);
        assertEquals("0/forward/10", firstEntry.getSelfHref());
        assertEquals("urn:uuid:83aee39f-923d-451e-8ec4-d6333ba8999d", firstEntry.getEntry().getId());
        FeedEntry<Event> tenthEntry = entries.get(10);
        assertEquals("10/forward/10", tenthEntry.getSelfHref());
        assertEquals("urn:uuid:d66cbaba-9a14-43f0-bef9-5a27dff6434d", tenthEntry.getEntry().getId());
    }

    @Test
    public void testSubscribingFrom_latestEntry() {
        List<FeedEntry<Event>> entries = client.feed("/feeds/events", Event.class)
                .fetchEntries(from("/", "urn:uuid:669c1d7b-e206-451b-97de-29767465c43c"))
                .take(10)
                .test()
                .awaitDone(3, TimeUnit.SECONDS)
                .assertNoErrors()
                .values();

        Assert.assertEquals(0, entries.size());
    }

    @Test
    public void testSubscribingFrom_midEntry_prune() {
        List<FeedEntry<Event>> entries = client.feed("/feeds/events", Event.class)
                .fetchEntries(from("/", "urn:uuid:af399659-424f-4c07-b07b-a5338c69aaf3"))
                .take(10)
                .test()
                .awaitDone(3, TimeUnit.SECONDS)
                .assertNoErrors()
                .values();

        Assert.assertEquals(2, entries.size());

        FeedEntry<Event> firstEntry = entries.get(0);
        assertEquals("30/forward/10", firstEntry.getSelfHref());
        assertEquals("urn:uuid:6bc05372-3f05-43a7-be7b-56f352cfbb0a", firstEntry.getEntry().getId());

        FeedEntry<Event> lastEntry = entries.get(1);
        assertEquals("30/forward/10", lastEntry.getSelfHref());
        assertEquals("urn:uuid:669c1d7b-e206-451b-97de-29767465c43c", lastEntry.getEntry().getId());
    }

    @Test
    public void testSubscribingFromNowOn() {
        List<FeedEntry<Event>> entries = client.feed("/feeds/events", Event.class)
                .fetchEntries(fromNowOn())
                .take(10)
                .test()
                .awaitDone(3, TimeUnit.SECONDS)
                .assertNoErrors()
                .values();

        Assert.assertEquals(0, entries.size());
    }
}
