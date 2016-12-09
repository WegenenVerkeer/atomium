package be.wegenenverkeer.atomium.api;

import be.wegenenverkeer.atomium.store.StoreFixture;
import org.junit.Before;
import org.junit.Test;

import static be.wegenenverkeer.atomium.api.FeedPageRef.page;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Karel Maesen, Geovise BVBA on 05/12/16.
 */
public class TestDefaultFeedPageAdapter {

    StoreFixture<String> fixture;
    FeedPageMetadata meta = new FeedPageMetadata(10 , "http://localhost/feeds/test", "Feed name") ;


    @Before
    public void before() {
        this.fixture = new StoreFixture();
        this.fixture.loadEntries(20, "test string");
    }

    @Test
    public void testFeedPagePage0() {
        FeedPageAdapter<String> provider = new DefaultFeedPageAdapter<>(this.fixture.store, meta);
        FeedPageRef pageRef = page(0);
        FeedPage<String> page = provider.getFeedPage(pageRef);
        assertEquals(10, page.getEntries().size());
        assertTrue(page.previousLink().isPresent());
        assertFalse(page.nextLink().isPresent());

    }

    @Test
    public void testFeedPagePage1() {
        FeedPageAdapter<String> provider = new DefaultFeedPageAdapter<>(this.fixture.store, meta);
        FeedPageRef pageRef = page(1);
        FeedPage<String> page = provider.getFeedPage(pageRef);
        assertEquals(10, page.getEntries().size());
        assertFalse(page.previousLink().isPresent());
        assertTrue(page.nextLink().isPresent());
    }

    @Test
    public void testFeedPagePage2() {
        FeedPageAdapter<String> provider = new DefaultFeedPageAdapter<>(this.fixture.store, meta);
        FeedPageRef pageRef = page(2);
        FeedPage<String> page = provider.getFeedPage(pageRef);
        assertEquals(0, page.getEntries().size());
        assertFalse(page.previousLink().isPresent());
        assertTrue(page.nextLink().isPresent());
    }

    @Test
    public void testHead() {
        fixture.loadEntries(2, "test"); // add 2, so head is now page 2
        FeedPageAdapter<String> provider = new DefaultFeedPageAdapter<>(this.fixture.store, meta);
        assertEquals(2, provider.getHeadOfFeedRef().getPageNum());
    }


    @Test
    public void testHead2() {
        FeedPageAdapter<String> provider = new DefaultFeedPageAdapter<>(this.fixture.store, meta);
        assertEquals(2, provider.getHeadOfFeedRef().getPageNum());
    }

    @Test
    public void testHeadOfEmptyFeedIsAcceptable(){
        this.fixture = new StoreFixture();
        FeedPageAdapter<String> provider = new DefaultFeedPageAdapter<>(this.fixture.store, meta);
        FeedPageRef head = provider.getHeadOfFeedRef();
        FeedPage<String> page = provider.getFeedPage(head);
        assertEquals(0, page.getEntries().size());
        assertFalse(page.previousLink().isPresent());
        assertFalse(page.nextLink().isPresent());
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testRequestBeyondHeadOfFeed(){
        FeedPageAdapter<String> provider = new DefaultFeedPageAdapter<>(this.fixture.store, meta);
        provider.getFeedPage(page(3));
    }



}
