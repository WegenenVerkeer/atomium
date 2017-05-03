package be.wegenenverkeer.atomium.api;

import be.wegenenverkeer.atomium.store.StoreFixture;
import org.junit.Before;
import org.junit.Test;

import static be.wegenenverkeer.atomium.api.FeedPageProviderAdapters.adapt;
import static be.wegenenverkeer.atomium.api.FeedPageRef.page;
import static org.junit.Assert.*;

/**
 * Created by Karel Maesen, Geovise BVBA on 05/12/16.
 */
public class TestDaoBackedFeedPageProvider {

    StoreFixture<String> fixture;
    FeedMetadata meta = new FeedMetadata(10 , "http://localhost/feeds/test", "Feed name") ;
    FeedPageProvider<String> provider;

    @Before
    public void before() {
        this.fixture = new StoreFixture<>();
        this.fixture.loadEntries(20, "test string");
        this.provider = adapt(this.fixture.store, meta);
    }

    @Test
    public void testFeedPagePage0() {
        FeedPageRef pageRef = page(0);
        FeedPage<String> page = provider.getFeedPage(pageRef);
        assertEquals(10, page.getEntries().size());
        assertTrue(page.previousLink().isPresent());
        assertFalse(page.nextLink().isPresent());

    }

    @Test
    public void testFeedPagePage1() {
        FeedPageRef pageRef = page(1);
        FeedPage<String> page = provider.getFeedPage(pageRef);
        assertEquals(10, page.getEntries().size());
        assertFalse(page.previousLink().isPresent());
        assertTrue(page.nextLink().isPresent());
    }

    @Test
    public void testFeedPagePage2() {
        FeedPageRef pageRef = page(2);
        FeedPage<String> page = provider.getFeedPage(pageRef);
        assertEquals(0, page.getEntries().size());
        assertFalse(page.previousLink().isPresent());
        assertTrue(page.nextLink().isPresent());
    }

    @Test
    public void testHead() {
        fixture.loadEntries(2, "test"); // add 2, so head is now page 2
        assertEquals(2, provider.getHeadOfFeedRef().getPageNum());
    }


    @Test
    public void testHead2() {
        assertEquals(2, provider.getHeadOfFeedRef().getPageNum());
    }

    @Test
    public void testHeadOfEmptyFeedIsAcceptable(){
        this.fixture = new StoreFixture<>();
        FeedPageProvider<String> provider = adapt(this.fixture.store, meta);
        FeedPageRef head = provider.getHeadOfFeedRef();
        FeedPage<String> page = provider.getFeedPage(head);
        assertEquals(0, page.getEntries().size());
        assertFalse(page.previousLink().isPresent());
        assertFalse(page.nextLink().isPresent());
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testRequestBeyondHeadOfFeed(){
        FeedPageProvider<String> provider = adapt(this.fixture.store, meta);
        provider.getFeedPage(page(3));
    }


}
