package be.wegenenverkeer.atomium.server.spring;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Feed provider.
 *
 * @param <E> Entry waarop de feed wordt gebaseerd
 * @param <T> TO zoals deze in de feed moet verschijnen
 */
public interface FeedProvider<E, T> {

    /**
     * Default page size.
     */
    long DEFAULT_PAGE_SIZE = 50;

    /**
     * URN prefix
     */
    String URN_ID = "urn:id:";

    /**
     * Haal entries op voor de gegeven page.
     * <em>BELANGRIJK</em>:
     * <ul>
     * <li>Voorzie 1 extra entry. De extra entry is nodig om te bepalen of er een volgende pagina is. Op die manier hebben we maar 1 call naar de datastore nodig.</li>
     * <li>Zet de nieuwste eerst! Atomium verwerkt de feed zoals een blog - de meest recente entry moet bovenaan staan.</li>
     * </ul>
     *
     * @param pageNumber page nummer. Zero based.
     * @return lijst van entries + <em>BELANGRIJK</em> 1 extra entry.
     */
    List<E> getEntriesForPage(long pageNumber);

    /**
     * Hoeveel entries in totaal?
     *
     * @return totaal aantal entries
     */
    long totalNumberOfEntries();

    /**
     * Zorg dat alle bestaande entries beschikbaar worden voor de feed. Kan gebruikt worden om de volgorde van de entries te garanderen.
     */
    void sync();

    /**
     * URN aanduiding voor elke entry in de feed.
     *
     * @param entry entry
     * @return URN. <em>Opgelet</em>, moet beginnen met <code>urn:</code>
     */
    String getUrnForEntry(E entry);

    /**
     * Bereken de timestamp van de gegeven entry.
     *
     * @param entry entry
     * @return timestamp
     */
    OffsetDateTime getTimestampForEntry(E entry);

    /**
     * Zet entry om naar TO.
     *
     * @param entry entry
     * @return TO
     */
    T toTo(E entry);

    /**
     * De gewenste feed URL.
     *
     * @return feed URL
     */
    String getFeedUrl();

    /**
     * Naam van de feed.
     *
     * @return naam van de feed
     */
    String getFeedName();

    /**
     * Geef de page size terug die gewenst is voor dit type feed.
     *
     * @return page size
     */
    default long getPageSize() {
        return DEFAULT_PAGE_SIZE;
    }

    /**
     * Geef de naam van de aanbieder.
     *
     * @return de naam van de aanbieder
     */
    default String getProviderName() {
        return "DistrictCenter";
    }

    /**
     * Geef de versie van de aanbieder.
     *
     * @return de versie van de aanbieder
     */
    default String getProviderVersion() {
        return "1.0";
    }
}
