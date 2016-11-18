package be.wegenenverkeer.atomium.server.spring;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AtomiumServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now();
    private static final long PAGE_SIZE = 42;

    @Mock
    private AtomiumServiceHelper helper;

    @InjectMocks
    private AtomiumService atomiumService;

    @Mock
    private Request request;

    @Mock
    private FeedProvider<TestFeedEntry, TestFeedEntryTo> mockFeedProvider;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void getFeed() throws Exception {
        int page = 0;

        when(mockFeedProvider.getEntriesForPage(0)).thenReturn(Collections.singletonList(new TestFeedEntry(42, NOW)));
        when(mockFeedProvider.getTimestampForEntry(any())).thenReturn(OffsetDateTime.MIN);
        when(mockFeedProvider.getUrnForEntry(any())).thenReturn("THE URN");
        when(mockFeedProvider.toTo(any())).thenReturn(new TestFeedEntryTo());
        when(mockFeedProvider.getPageSize()).thenReturn(PAGE_SIZE);

        atomiumService.getFeed(mockFeedProvider, page, PAGE_SIZE, request);

        verify(helper, times(1)).sync(mockFeedProvider);
        verify(helper, times(1)).getFeed(mockFeedProvider, page, request, false);
    }

    @Test
    public void getFeed_foutePagesize() throws Exception {
        int page = 0;

        when(mockFeedProvider.getEntriesForPage(0)).thenReturn(Collections.singletonList(new TestFeedEntry(42, NOW)));
        when(mockFeedProvider.getTimestampForEntry(any())).thenReturn(OffsetDateTime.MIN);
        when(mockFeedProvider.getUrnForEntry(any())).thenReturn("THE URN");
        when(mockFeedProvider.toTo(any())).thenReturn(new TestFeedEntryTo());
        when(mockFeedProvider.getPageSize()).thenReturn(PAGE_SIZE);

        exception.expect(RuntimeException.class);
        exception.expectMessage(String.format("Pagina grootte komt niet overeen met verwachte waarde '%d', de gebruikte link werd niet gegenereerd door Atom feed.", PAGE_SIZE));

        atomiumService.getFeed(mockFeedProvider, page, PAGE_SIZE + 1, request);
    }

    @Test
    public void getCurrentFeed() throws Exception {
        double factor = 3;

        when(mockFeedProvider.getEntriesForPage(0)).thenReturn(Collections.singletonList(new TestFeedEntry(42, NOW)));
        when(mockFeedProvider.getTimestampForEntry(any())).thenReturn(OffsetDateTime.MIN);
        when(mockFeedProvider.getUrnForEntry(any())).thenReturn("THE URN");
        when(mockFeedProvider.toTo(any())).thenReturn(new TestFeedEntryTo());
        when(mockFeedProvider.totalNumberOfEntries()).thenReturn(Double.valueOf(PAGE_SIZE * factor).longValue());
        when(mockFeedProvider.getPageSize()).thenReturn(PAGE_SIZE);

        Response resp = atomiumService.getCurrentFeed(mockFeedProvider, request);

        verify(helper, times(1)).sync(mockFeedProvider);
        verify(helper, times(1)).getFeed(mockFeedProvider, atomiumService.detemineMostRecentPage(mockFeedProvider), request, true);
    }

    @Test
    public void detemineMostRecentPage() {
        double factor = 3.2;

        when(mockFeedProvider.totalNumberOfEntries()).thenReturn(Double.valueOf(PAGE_SIZE * factor).longValue());
        when(mockFeedProvider.getPageSize()).thenReturn(PAGE_SIZE);

        long actual = atomiumService.detemineMostRecentPage(mockFeedProvider);

        assertThat(actual).isEqualTo(Double.valueOf(factor).intValue());
    }

    @Test
    public void detemineMostRecentPage_countGelijkAanVeelvoudPageSize() {
        double factor = 3;

        when(mockFeedProvider.totalNumberOfEntries()).thenReturn(Double.valueOf(PAGE_SIZE * factor).longValue());
        when(mockFeedProvider.getPageSize()).thenReturn(PAGE_SIZE);

        long actual = atomiumService.detemineMostRecentPage(mockFeedProvider);

        assertThat(actual).isEqualTo(Double.valueOf(factor - 1).intValue());
    }

}