package be.wegenenverkeer.atomium.format;

import org.w3c.dom.Node;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;

public class JAXBElementUnmarshaller<T> {

    private final JAXBContext jaxbContext;
    private final Class<T> valueClazz;

    public JAXBElementUnmarshaller(JAXBContext jaxbContext, Class<T> valueClazz) {
        this.jaxbContext = jaxbContext;
        this.valueClazz = valueClazz;
    }

    public JAXBElement<T> unmarshal(Node node) throws JAXBException {
        return jaxbContext.createUnmarshaller().unmarshal(node, valueClazz);
    }

    public T unmarshalValue(Node node) throws JAXBException {
        return unmarshal(node).getValue();
    }

}
