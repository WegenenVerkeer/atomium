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
     * @param feedProvider entry provider
     * @param page pagina
     * @param count max number of items in page
     * @param request request
     * @param <E> Entry waarop de feed wordt gebaseerd
     * @param <T> TO zoals deze in de feed moet verschijnen
     * @return atom feed data
     */
    public <E, T> Response getFeed(FeedProvider<E, T> feedProvider, long page, long count, Request request) {
        // safeguard, als je andere page sizes gebruikt dan ingesteld in de feedProvider kan je caching issues krijgen
        if (feedProvider.getPageSize() != count) {
            throw new AtomiumServerException(String.format("Pagina grootte komt niet overeen met verwachte waarde '%d', "
                    + "de gebruikte link werd niet gegenereerd door Atom feed.", feedProvider.getPageSize()));
        }

        helper.sync(feedProvider);

        return helper.getFeed(feedProvider, page, request, false);
    }

    /**
     * Haal de feed met cache headers op.
     * <p>
     * Voordat de feed wordt opgehaald wordt "sync" opgeroepen in een aparte transactie.
     * <p>
     * De meest recente page wordt opgehaald.
     *
     * @param feedProvider entry provider
     * @param request request
     * @param <E> Entry waarop de feed wordt gebaseerd
     * @param <T> TO zoals deze in de feed moet verschijnen
     * @return atom feed data
     */
    public <E, T> Response getCurrentFeed(FeedProvider<E, T> feedProvider, Request request) {
        helper.sync(feedProvider);

        return helper.getFeed(feedProvider, detemineMostRecentPage(feedProvider), request, true);
    }

    /**
     * Bepaal de meest recente page
     *
     * @param feedProvider entry provider
     * @param <E> Entry waarop de feed wordt gebaseerd
     * @param <T> TO zoals deze in de feed moet verschijnen
     * @return meest recente page
     */
    <E, T> long detemineMostRecentPage(FeedProvider<E, T> feedProvider) {
        long count = feedProvider.totalNumberOfEntries();
        long pageSize = feedProvider.getPageSize();

        long page = count / pageSize;

        if (0 == count % pageSize && page > 0) {
            page--;
        }

        return page;
    }
}
