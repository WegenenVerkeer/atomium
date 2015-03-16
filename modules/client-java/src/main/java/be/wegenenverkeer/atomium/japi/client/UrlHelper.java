package be.wegenenverkeer.atomium.japi.client;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Karel Maesen, Geovise BVBA on 17/03/15.
 */
public class UrlHelper {

    final private URI baseURI;

    public UrlHelper(String base) {
        try {
            baseURI = new URI(base);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String toRelative(String path) {
        try {
            URI pURI = new URI(path);
            if (pURI.isAbsolute()) {
                return baseURI.relativize(pURI).toString();
            } else {
                return path;
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

    }


}
