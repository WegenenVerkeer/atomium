package be.wegenenverkeer.atomium.server.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

/**
 * Atom feeds helper voor vaststellingen.
 */
@Component
public class AtomiumService {

    @Autowired
    private AtomiumServiceHelper helper;

    /**
     * Haal de feed op. Voor een feed met cache headers, gebruik getCachedFeed.
     * <p>
     * Voordat de feed wordt opgehaald wordt "sync" opgeroepen in een aparte transactie.
     *
     * @param springFeedProvider entry provider
     * @param page pagina
     * @param count max number of items in page
     * @param request request
     * @param <E> Entry waarop de feed wordt gebaseerd
     * @param <T> TO zoals deze in de feed moet verschijnen
     * @return atom feed data
     */
    public <E, T> Response getFeed(SpringFeedProvider<E, T> springFeedProvider, long page, long count, Request request) {
        // safeguard, als je andere page sizes gebruikt dan ingesteld in de feedProvider kan je caching issues krijgen
        if (springFeedProvider.getPageSize() != count) {
            throw new AtomiumServerException(String.format("Pagina grootte komt niet overeen met verwachte waarde '%d', "
                    + "de gebruikte link werd niet gegenereerd door Atom feed.", springFeedProvider.getPageSize()));
        }

        helper.sync(springFeedProvider);

        return helper.getFeed(springFeedProvider, page, request, false);
    }

    /**
     * Haal de feed met cache headers op.
     * <p>
     * Voordat de feed wordt opgehaald wordt "sync" opgeroepen in een aparte transactie.
     * <p>
     * De meest recente page wordt opgehaald.
     *
     * @param springFeedProvider entry provider
     * @param request request
     * @param <E> Entry waarop de feed wordt gebaseerd
     * @param <T> TO zoals deze in de feed moet verschijnen
     * @return atom feed data
     */
    public <E, T> Response getCurrentFeed(SpringFeedProvider<E, T> springFeedProvider, Request request) {
        helper.sync(springFeedProvider);

        return helper.getFeed(springFeedProvider, detemineMostRecentPage(springFeedProvider), request, true);
    }

    /**
     * Bepaal de meest recente page
     *
     * @param springFeedProvider entry provider
     * @param <E> Entry waarop de feed wordt gebaseerd
     * @param <T> TO zoals deze in de feed moet verschijnen
     * @return meest recente page
     */
    <E, T> long detemineMostRecentPage(SpringFeedProvider<E, T> springFeedProvider) {
        long count = springFeedProvider.totalNumberOfEntries();
        long pageSize = springFeedProvider.getPageSize();

        long page = count / pageSize;

        if (0 == count % pageSize && page > 0) {
            page--;
        }

        return page;
    }
}
