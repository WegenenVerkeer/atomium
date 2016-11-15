package be.wegenenverkeer.atomium.format;

import be.wegenenverkeer.atomium.api.AtomiumDecodeException;
import be.wegenenverkeer.atomium.api.FeedPage;
import be.wegenenverkeer.atomium.api.FeedPageCodec;
import org.xml.sax.InputSource;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.StringReader;

/**
 * Created by Karel Maesen, Geovise BVBA on 15/11/16.
 */
public class JAXBCodec<T> implements FeedPageCodec<T,String> {

    private final JAXBContext jaxbContext;

    public JAXBCodec(Class<T> entryTypeMarker) {
        try {
            jaxbContext = JAXBContext.newInstance(FeedPage.class, Link.class, entryTypeMarker);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String encode(FeedPage<T> page) {
        throw new NotImplementedException();
    }

    @Override
    public FeedPage<T> decode(String encoded) {
        try {
            return (FeedPage<T>)jaxbContext.createUnmarshaller().unmarshal(new InputSource(new StringReader(encoded)));
        } catch (JAXBException e) {
            throw new AtomiumDecodeException(e.getMessage(), e);
        }
    }
}
