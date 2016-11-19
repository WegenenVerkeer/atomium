package be.wegenenverkeer.atomium.server.spring;

import be.wegenenverkeer.atomium.format.AtomEntry;
import be.wegenenverkeer.atomium.api.FeedPage;
import be.wegenenverkeer.atomium.format.Link;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class AtomiumServiceHelperTest {

    private static final int TEST_PAGE_SIZE = 2;
    private static final OffsetDateTime NOW = OffsetDateTime.now();
    public static final String TEST_FEED_NAME = "Test feed";
    public static final String TEST_FEED_URL = "/test/feed/url";

    @Spy
    private RestJsonMapper mapper;

    @InjectMocks
    private AtomiumServiceHelper helper;

    @Mock
    Request request;

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private List<TestFeedEntry> onvolledigeLijst = maakTestFeedEntries(TEST_PAGE_SIZE);
    private List<TestFeedEntry> volledigeLijst = maakTestFeedEntries(TEST_PAGE_SIZE + 1);

    @Test
    public void sync() throws Exception {
        TestSpringFeedProvider testFeedProvider = mock(TestSpringFeedProvider.class);

        helper.sync(testFeedProvider);

        verify(testFeedProvider, times(1)).sync();
    }

    @Test
    public void geenEntriesVoorPageGevonden() throws Exception {
        TestSpringFeedProvider testFeedProvider = new TestSpringFeedProvider();
        testFeedProvider.setEntriesForPage(emptyList());

        //TODO why are execptions no longer thrown?
//        exception.expect(NotFoundException.class);
//        exception.expectMessage("Pagina 0 niet gevonden.");

        Response response = helper.getFeed(testFeedProvider, 0, request, true);
        assertThat(response.getStatus()).isEqualTo(SC_NOT_FOUND);
    }

    @Test
    public void etag_isDeHashVanDeTimestampVanDeEersteEntry() throws Exception {
        TestSpringFeedProvider testFeedProvider = new TestSpringFeedProvider();
        testFeedProvider.setEntriesForPage(volledigeLijst);

        Response response = helper.getFeed(testFeedProvider, 0, request, true);

        assertThat(response.getMetadata().getFirst("ETag")).isNotNull().isInstanceOf(EntityTag.class);

        String hashcodeVanVoorlaatsteItem = Integer.toString(volledigeLijst.get(0).getTimestamp().hashCode());

        assertThat(response.getMetadata().getFirst("ETag")).isEqualTo(EntityTag.valueOf(hashcodeVanVoorlaatsteItem));
    }

    @Test
    public void etag_geenMatch() throws Exception {
        TestSpringFeedProvider testFeedProvider = new TestSpringFeedProvider();
        testFeedProvider.setEntriesForPage(volledigeLijst);
        String hashcodeVanVoorlaatsteItem = Integer.toString(volledigeLijst.get(TEST_PAGE_SIZE - 1).getTimestamp().hashCode());
        EntityTag etag = new EntityTag(hashcodeVanVoorlaatsteItem);

        // wanneer de etag niet matcht wordt de feed opnieuw opgebouwd
        when(request.evaluatePreconditions(etag)).thenReturn(null);

        Response actualResponse = helper.getFeed(testFeedProvider, 0, request, true);

        assertThat(actualResponse.getStatus()).isEqualTo(SC_OK); // echte response
        verify(mapper, times(1)).encode(any()); // nieuwe feed schrijven
    }

    @Test
    public void etag_match() throws Exception {
        TestSpringFeedProvider testFeedProvider = new TestSpringFeedProvider();
        testFeedProvider.setEntriesForPage(volledigeLijst);
        String hashcodeVanVoorlaatsteItem = Integer.toString(volledigeLijst.get(0).getTimestamp().hashCode());
        EntityTag etag = new EntityTag(hashcodeVanVoorlaatsteItem);

        Response expectedResponse = mock(Response.class);

        // wanneer de etag matcht wordt deze response builder gebruikt
        Response.ResponseBuilder responseBuilder = mock(Response.ResponseBuilder.class);
        when(responseBuilder.cacheControl(any())).thenReturn(responseBuilder);
        when(responseBuilder.tag(etag)).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(expectedResponse);
        when(request.evaluatePreconditions(etag)).thenReturn(responseBuilder);

        Response actualResponse = helper.getFeed(testFeedProvider, 0, request, true);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(responseBuilder, times(1)).build();
        verify(mapper, never()).encode(any()); // geen nieuwe feed schrijven
    }

    @Test
    public void indienCurrentPage_nietGecached() throws Exception {
        TestSpringFeedProvider testFeedProvider = new TestSpringFeedProvider();
        testFeedProvider.setEntriesForPage(onvolledigeLijst);

        Response response = helper.getFeed(testFeedProvider, 0, request, true);

        assertThat(response.getMetadata().getFirst("Cache-Control")).isNull();
    }

    @Test
    public void indienRecentPageNietVolledig_nietGecached() throws Exception {
        TestSpringFeedProvider testFeedProvider = new TestSpringFeedProvider();
        testFeedProvider.setEntriesForPage(onvolledigeLijst);

        Response response = helper.getFeed(testFeedProvider, 0, request, false);

        assertThat(response.getMetadata().getFirst("Cache-Control")).isNull();
    }

    @Test
    public void indienRecentPageWelVolledig_welGecached() throws Exception {
        TestSpringFeedProvider testFeedProvider = new TestSpringFeedProvider();
        testFeedProvider.setEntriesForPage(volledigeLijst);

        Response response = helper.getFeed(testFeedProvider, 0, request, false);

        assertThat(response.getMetadata().getFirst("Cache-Control")).isNotNull();
        assertThat(response.getMetadata().getFirst("Cache-Control").toString())
                .isEqualTo("no-transform, max-age=0"); // max-age=0, want @Value wordt niet geïnitialiseerd in unit tests
    }

    @Test
    public void feedWordtOpgebouwd_metadata() throws Exception {
        FeedPage<TestFeedEntryTo> feedPage = getFeed(volledigeLijst);

        assertThat(feedPage.getId()).isEqualTo(TEST_FEED_NAME);
        assertThat(feedPage.getBase()).isEqualTo(TEST_FEED_URL);
        assertThat(feedPage.getTitle()).isEqualTo(TEST_FEED_NAME);
        assertThat(feedPage.getGenerator().getText()).isEqualTo("DistrictCenter");
        assertThat(feedPage.getGenerator().getUri()).isEqualTo(TEST_FEED_URL);
        assertThat(feedPage.getGenerator().getVersion()).isEqualTo("1.0");
    }

    @Test
    public void feedWordtOpgebouwd_onvolledigePage() throws Exception {
        List<TestFeedEntry> teTestenLijst = new ArrayList<>(onvolledigeLijst);
        int teTestenLijstSize = teTestenLijst.size();

        FeedPage<TestFeedEntryTo> feedPage = getFeed(teTestenLijst);

        String[] expectedIds = teTestenLijst.stream()
                .map(entry -> "urn:id:" + entry.getId())
                .toArray(String[]::new);

        assertThat(feedPage.getEntries()).hasSize(teTestenLijstSize).extracting("id").containsExactly(expectedIds);
    }

    @Test
    public void feedWordtOpgebouwd_volledigePage() throws Exception {
        // de code gaat er van uit dat je een page + 1 geeft, dat is een efficiente manier om te weten of de page volledig is
        // als je immers page size + 1 items krijgt weet je dat er nog elementen volgen
        List<TestFeedEntry> teTestenLijst = new ArrayList<>(volledigeLijst);
        int teTestenLijstSize = teTestenLijst.size();

        FeedPage<TestFeedEntryTo> feedPage = getFeed(teTestenLijst);

        String[] expectedIds = teTestenLijst.subList(1, teTestenLijst.size())
                .stream()
                .map(entry -> "urn:id:" + entry.getId())
                .toArray(String[]::new);

        assertThat(feedPage.getEntries()).hasSize(teTestenLijstSize - 1).extracting("id").containsExactly(expectedIds);
    }

    @Test
    public void feedWordtOpgebouwd_volledigePageIsVolledigeLijstMinDeLaatste() throws Exception {
        // de code gaat er van uit dat je een page + 1 geeft, dat is een efficiente manier om te weten of de page volledig is
        // als je immers page size + 1 items krijgt weet je dat er nog elementen volgen
        FeedPage<TestFeedEntryTo> feedPage = getFeed(volledigeLijst);

        List<TestFeedEntry> lijstClone = new ArrayList<>(volledigeLijst);
        String[] expectedIds = lijstClone.subList(1, lijstClone.size()).stream()
                .map(entry -> "urn:id:" + entry.getId())
                .toArray(String[]::new);

        assertThat(feedPage.getEntries()).hasSize(volledigeLijst.size() - 1).extracting("id").containsExactly(expectedIds);
    }

    @Test
    public void feedWordtOpgebouwd_linksVoorEenVolledigePage0() throws Exception {
        FeedPage<TestFeedEntryTo> feedPage = getFeed(volledigeLijst);

        assertThat(feedPage.getLinks()).isNotEmpty();
        assertThat(feedPage.getLinks()).extracting("rel").containsExactly("last", "previous", "self");

        assertThat(getLink(feedPage.getLinks(), "self").getHref()).isEqualTo("/0/2");
        assertThat(getLink(feedPage.getLinks(), "last").getHref()).isEqualTo("/0/2");
        assertThat(getLink(feedPage.getLinks(), "previous").getHref()).isEqualTo("/1/2");  // previous is de volgende pagina :-/
    }


    @Test
    public void feedWordtOpgebouwd_linksVoorEenVolledigePage1() throws Exception {
        FeedPage<TestFeedEntryTo> feedPage = getFeed(volledigeLijst, 1);

        assertThat(feedPage.getLinks()).isNotEmpty();
        assertThat(feedPage.getLinks()).extracting("rel").containsExactly("last", "next", "previous", "self");

        assertThat(getLink(feedPage.getLinks(), "self").getHref()).isEqualTo("/1/2");
        assertThat(getLink(feedPage.getLinks(), "last").getHref()).isEqualTo("/0/2");
        assertThat(getLink(feedPage.getLinks(), "next").getHref()).isEqualTo("/0/2"); // next is de vorige pagina :-/
        assertThat(getLink(feedPage.getLinks(), "previous").getHref()).isEqualTo("/2/2");  // previous is de volgende pagina :-/
    }

    @Test
    public void feedWordtOpgebouwd_linksVoorEenOnvolledigePage() throws Exception {
        FeedPage<TestFeedEntryTo> feedPage = getFeed(onvolledigeLijst);

        assertThat(feedPage.getLinks()).isNotEmpty();
        assertThat(feedPage.getLinks()).extracting("rel").containsExactly("last", "self");

        assertThat(getLink(feedPage.getLinks(), "self").getHref()).isEqualTo("/0/2");
        assertThat(getLink(feedPage.getLinks(), "last").getHref()).isEqualTo("/0/2");
    }

    @Test
    public void toAtomEntry() throws Exception {
        int id = 42;
        TestSpringFeedProvider testFeedProvider = new TestSpringFeedProvider();
        testFeedProvider.setEntriesForPage(onvolledigeLijst);
        AtomEntry<TestFeedEntryTo> atomEntry = helper.toAtomEntry(new TestFeedEntry(id, NOW), testFeedProvider);

        assertThat(atomEntry.getId()).isEqualTo("urn:id:" + id);
    }

    private Link getLink(List<Link> links, String name) {
        Optional<Link> link = links.stream().filter(current -> current.getRel().equals(name)).findFirst();
        assert (link.isPresent());
        return link.get();
    }

    private FeedPage<TestFeedEntryTo> getFeed(List<TestFeedEntry> lijst) throws Exception {
        return getFeed(lijst, 0);
    }

    private FeedPage<TestFeedEntryTo> getFeed(List<TestFeedEntry> lijst, int page) throws Exception {
        TestSpringFeedProvider testFeedProvider = new TestSpringFeedProvider();
        testFeedProvider.setEntriesForPage(lijst);

        Response response = helper.getFeed(testFeedProvider, page, request, false);
        return mapper.decode(response.getEntity().toString());
    }

    private List<TestFeedEntry> maakTestFeedEntries(int testPageSize) {
        return IntStream.rangeClosed(1, testPageSize)
                .mapToObj(integer -> new TestFeedEntry(testPageSize + 42 - integer, NOW.minusDays(integer)))
                .collect(Collectors.toList());
    }

    public class TestSpringFeedProvider implements SpringFeedProvider<TestFeedEntry, TestFeedEntryTo> {
        private List<TestFeedEntry> entries;

        @Override
        public List<TestFeedEntry> getEntriesForPage(long pageNumber) {
            return entries;
        }

        @Override
        public long totalNumberOfEntries() {
            return 42;
        }

        public void setEntriesForPage(List<TestFeedEntry> entries) {
            this.entries = entries;
        }

        @Override
        public void sync() {
            // no-op
        }

        @Override
        public String getUrnForEntry(TestFeedEntry entry) {
            return URN_ID + entry.getId();
        }

        @Override
        public OffsetDateTime getTimestampForEntry(TestFeedEntry entry) {
            return entry.getTimestamp();
        }

        @Override
        public TestFeedEntryTo toTo(TestFeedEntry entry) {
            return new TestFeedEntryTo(entry.getId());
        }

        @Override
        public String getFeedUrl() {
            return TEST_FEED_URL;
        }

        @Override
        public String getFeedName() {
            return TEST_FEED_NAME;
        }

        @Override
        public long getPageSize() {
            return TEST_PAGE_SIZE;
        }
    }
}