package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.FeedMetadata;
import be.wegenenverkeer.atomium.api.FeedPage;
import be.wegenenverkeer.atomium.api.FeedPageProviderAdapters;
import be.wegenenverkeer.atomium.format.JacksonCodec;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TestEmptyFeedEntryStore {

    private StoreFixture<String> fixture;

    @Before
    public void setUp(){
        this.fixture = new StoreFixture<>();
    }

    @Test
    public void testEmptyFeedStore(){
        var feedPageProvider = FeedPageProviderAdapters.adapt(fixture.store, new FeedMetadata(10, "test-feed-url", "test-feed-name"));
        var headPage = feedPageProvider.getFeedPage(feedPageProvider.getHeadOfFeedRef());

        assertEquals(0, headPage.getEntries().size());

        JacksonCodec<FeedPage> jacksonCodec = new JacksonCodec<>(FeedPage.class);
        var headPageJson=  jacksonCodec.encode(headPage);

        assertThat(headPageJson, StringContains.containsString("entries"));
    }
}


