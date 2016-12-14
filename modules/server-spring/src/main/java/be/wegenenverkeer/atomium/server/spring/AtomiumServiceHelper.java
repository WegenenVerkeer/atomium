package be.wegenenverkeer.atomium.server.spring;

import be.wegenenverkeer.atomium.api.AtomiumEncodeException;
import be.wegenenverkeer.atomium.format.Entry;
import be.wegenenverkeer.atomium.api.FeedPage;
import be.wegenenverkeer.atomium.api.FeedPageCodec;
import be.wegenenverkeer.atomium.format.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Zodat @Transactional correct wordt geproxied.
 */
@Component
public class AtomiumServiceHelper {

    @Value("${atomium.cacheDuration:2592000}") // default: cache for 30 days
    private int cacheDuration;

    @Autowired
    private FeedPageCodec<?, String> encoder;

    /**
     * Update atom volgnummers (in aparte transactie).
     *
     * @param springFeedProvider feedProvider
     * @param <E> Entry waarop de feed wordt gebaseerd
     * @param <T> TO zoals deze in de feed moet verschijnen
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <E, T> void sync(SpringFeedProvider<E, T> springFeedProvider) {
        springFeedProvider.sync();
    }

    /**
     * Eigenlijke bepalen van de atom feed voor wijzigingen in de entries.
     *
     * @param springFeedProvider entry provider
     * @param page pagina
     * @param request request
     * @param isCurrent wordt de feed opgevraagd langs "/"?
     * @param <E> Entry waarop de feed wordt gebaseerd
     * @param <T> TO zoals deze in de feed moet verschijnen
     * @return atom feed data
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public <E, T> Response getFeed(SpringFeedProvider<E, T> springFeedProvider, long page, Request request, boolean isCurrent) {
        // entries voor pagina bepalen (ééntje meer om te weten of er nog een volgende pagina is)
        List<E> entriesForPage = springFeedProvider.getEntriesForPage(page);

        if (entriesForPage.isEmpty()) {
            return Response.status(404).entity("Pagina " + page + " niet gevonden.").build();
        }

        boolean pageComplete = entriesForPage.size() > springFeedProvider.getPageSize();
        OffsetDateTime updated = springFeedProvider.getTimestampForEntry(entriesForPage.get(0));

        // updated time known, check/calculate eTag
        CacheControl cc = new CacheControl();
        cc.setMaxAge(cacheDuration);
        Response.ResponseBuilder rb;
        EntityTag etag = new EntityTag(Integer.toString(updated.hashCode()));
        rb = request.evaluatePreconditions(etag); // Verify if it matched with etag available in http request
        if (null != rb) { // rb is not null when eTag matched
            return rb.cacheControl(cc).tag(etag).build();
        }

        // feed bouwen
        FeedPage<T> feedPage = new FeedPage<>(
                springFeedProvider.getFeedName(),
                springFeedProvider.getFeedUrl(),
                springFeedProvider.getFeedName(),
                new Generator(springFeedProvider.getProviderName(), springFeedProvider.getFeedUrl(), springFeedProvider.getProviderVersion()),
                updated
        );

        String pageUrl = "/" + page + '/' + springFeedProvider.getPageSize();

        List<Entry<T>> entries = entriesForPage.stream()
                .map(entry -> toAtomEntry(entry, springFeedProvider))
                .collect(toList());

        feedPage.setEntries(entries);

        List<Link> links = new ArrayList<>();
        links.add(new Link("last", "/0/" + springFeedProvider.getPageSize()));
        if (page >= 1) {
            links.add(new Link("next", "/" + (page - 1) + '/' + springFeedProvider.getPageSize()));
        }
        if (pageComplete) {
            links.add(new Link("previous", "/" + (page + 1) + '/' + springFeedProvider.getPageSize()));
            feedPage.getEntries().remove(0);
        }

        links.add(new Link("self", pageUrl));
        feedPage.setLinks(links);

        // response opbouwen
        try {
            rb = Response.ok(((FeedPageCodec<T,String>)encoder).encode(feedPage));
            if (!isCurrent && pageComplete) {
                rb.cacheControl(cc); // cache result enkel als pagina volledig en niet via de "recent" URL opgeroepen. Zie figuur 7-2 in Rest In Practice.
            }
            rb.tag(etag);
            return rb.build();
        } catch (AtomiumEncodeException jpe) {
            throw new AtomiumServerException("Kan stream niet converteren naar JSON.", jpe);
        }
    }

    /**
     * Entity to atom entry.
     *
     * @param entity entity
     * @param springFeedProvider feedProvider
     * @param <E> entity type
     * @param <T> to type
     * @return entity omgezet naar een TO
     */
    <E, T> AtomEntry<T> toAtomEntry(E entity, SpringFeedProvider<E, T> springFeedProvider) {
        return new AtomEntry<>(
                springFeedProvider.getUrnForEntry(entity),
                springFeedProvider.getTimestampForEntry(entity),
                new Content<>(springFeedProvider.toTo(entity), ""));
    }

}
