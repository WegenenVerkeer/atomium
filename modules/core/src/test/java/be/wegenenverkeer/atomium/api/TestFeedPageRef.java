package be.wegenenverkeer.atomium.api;

import org.junit.Test;

import static be.wegenenverkeer.atomium.api.FeedPageRef.page;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Karel Maesen, Geovise BVBA on 07/12/16.
 */
public class TestFeedPageRef {

    @Test
    public void testPageComparisons(){
        assertEquals(0, page(1).compareTo(page(1)));
        assertEquals(-1,page(1).compareTo(page(2)));
        assertEquals(1, page(5).compareTo(page(2)));
    }

    @Test
    public void testEqualPages(){
        assertEquals(FeedPageRef.page(3), FeedPageRef.page(3));
    }

    @Test
    public void testEarlierPage(){
        assertTrue(FeedPageRef.page(1).isStrictlyOlderThan(page(2)));
    }

    @Test
    public void testMoreRecentPage(){
        assertTrue(FeedPageRef.page(2).isStrictlyMoreRecentThan(page(1)));
    }
}

