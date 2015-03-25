package be.wegenenverkeer.atomium.japi.client;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Karel Maesen, Geovise BVBA on 17/03/15.
 */
public class UrlHelper {

    final private URI baseURI;

    public UrlHelper(String baseURI) {
        try {
            this.baseURI = new URI(baseURI).normalize();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Relativizes the [feed]/[page] URL path to the baseURI
     *
     * @param feedbase the name of the feed ([baseURI]/[feedbase] should refer to head of the feed)
     * @param path the path to a page in the feed
     * @return the path to the specified feed page, relative to this instance's baseURI
     */
    public String toRelative(String feedbase, String path) {
        try {
            URI pURI = new URI(path);
            if (pURI.isAbsolute()) {
                return baseURI.relativize(pURI).toString();
            } else if(path.isEmpty()) {
                return new URI(feedbase).normalize().toString();
            } else {
                return new URI(feedbase + "/" + path).normalize().toString();
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

    }


}
