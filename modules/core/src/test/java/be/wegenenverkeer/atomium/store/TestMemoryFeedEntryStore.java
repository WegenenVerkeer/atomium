package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Event;
import be.wegenenverkeer.atomium.api.FeedMetadata;
import be.wegenenverkeer.atomium.api.FeedPage;
import be.wegenenverkeer.atomium.api.FeedPageProviderAdapters;
import be.wegenenverkeer.atomium.format.JacksonCodec;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Created by Karel Maesen, Geovise BVBA on 05/12/16.
 */
public class TestMemoryFeedEntryStore {


    private StoreFixture<String> fixture;


    @Before
    public void setUp() {
        this.fixture = new StoreFixture<>();
    }

    @Test
    public void testMemoryFeedStore() {
        fixture.loadEntries(1000, "Test value");

        List<Event<String>> received = fixture.store.getEvents(0, 11);

        assertEquals(11, received.size());

        Stream<Integer> intStream = (IntStream.range(0, 11)).boxed();
        List<Integer> expected = intStream.collect(Collectors.toList());
        Stream<Integer> receivedIdsStream = received.stream().map(e -> Integer.parseInt(e.getId()));
        List<Integer> receivedIds = receivedIdsStream.collect(Collectors.toList());
        assertEquals(expected, receivedIds);

        var feedPageProvider = FeedPageProviderAdapters.adapt(fixture.store, new FeedMetadata(15, "test-feed-url", "test-feed-name"));
        var headPage = feedPageProvider.getFeedPage(feedPageProvider.getHeadOfFeedRef());

        assertEquals(10, headPage.getEntries().size());

        JacksonCodec<FeedPage> jacksonCodec = new JacksonCodec<>(FeedPage.class);
        var headPageJson = jacksonCodec.encode(headPage);

        assertThat(headPageJson, StringContains.containsString("entries"));
    }
}
