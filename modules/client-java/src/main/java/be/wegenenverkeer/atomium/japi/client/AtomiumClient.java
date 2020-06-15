package be.wegenenverkeer.atomium.japi.client;

import java.time.Duration;

public interface AtomiumClient {

    /**
     * Creates a {@code AtomiumFeed} for the specified feed and entry type
     *
     * <p>The feedPath argument appended to the baseUrl of this {@code AtomiumClient} should equal the
     * xml:base-attribute of the feedpage</p>
     *
     * <p>The entryTypeMarker-class should have the required public accessors and JAXB-annotations to enable
     * proper unmarshalling. For Json-unmarshalling, the  Jackson library is used.</p>
     *
     * @param feedUrl         Feed URL
     * @param entryTypeMarker entry type
     * @param <E>             the class parameter of the Entry content value
     * @return a {@code FeedObservableBuilder}
     */
    <E> AtomiumFeed<E> feed(String feedUrl, Class<E> entryTypeMarker);

    /**
     * Cleanup client and its fetchers
     */
    void close();
}
