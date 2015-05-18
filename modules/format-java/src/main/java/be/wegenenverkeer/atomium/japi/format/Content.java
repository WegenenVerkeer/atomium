package be.wegenenverkeer.atomium.japi.format;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@XmlAccessorType(XmlAccessType.NONE)
public final class Content<T> {

    private static final ThreadLocal<JAXBElementUnmarshaller> jaxbElementUnmarshallerThreadLocal = new ThreadLocal<>();
    @XmlAttribute(name="type")
    private String type;
    @XmlAnyElement(lax = true) @XmlMixed @JsonIgnore
    private List objects = new ArrayList();
    @XmlTransient
    private T value;

    public Content() {}

    @SuppressWarnings("unchecked")
    public Content(T value, String type) {
        this.type = type;
        objects.add(value);
        if (value.getClass().isAnnotationPresent(XmlRootElement.class) || value instanceof String) {
            this.value = value;
        } else if (value instanceof JAXBElement) {
            this.value = ((JAXBElement<T>) value).getValue();
        } else {
            this.value = value;
        }
    }

    public static void setJAXBElementUnmarshaller(JAXBElementUnmarshaller jaxbElementUnmarshaller) {
        Content.jaxbElementUnmarshallerThreadLocal.set(jaxbElementUnmarshaller);
    }

    public static void resetJAXBElementUnmarshaller() {
        Content.jaxbElementUnmarshallerThreadLocal.remove();
    }

    public String getType() {
        return this.type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Content content = (Content) o;
        return Objects.equals(type, content.type) && Objects.equals(getValue(), content.getValue());

    }

    @Override
    public int hashCode() {
        return Objects.hash(type, getValue());
    }

    public T getValue() {
        if (value == null) {
            value = getValueFromObjects();
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private T getValueFromObjects() {
        StringBuilder buffer = new StringBuilder();
        for (Object o : objects) {
            buffer.append(o.toString());
            if (o.getClass().isAnnotationPresent(XmlRootElement.class)) {
                //xmlRootElements are already unmarshaled correctly
                return (T) o;
            } else if (o instanceof JAXBElement) {
                //get the value out of the JAXBElement wrapper
                return (T) ((JAXBElement) o).getValue();
            } else if (o instanceof Element) {
                if (jaxbElementUnmarshallerThreadLocal.get() != null) {
                    try {
                        return (T) jaxbElementUnmarshallerThreadLocal.get().unmarshalValue(((Element) o));
                    } catch (JAXBException e) {
                        throw new RuntimeException(e);
                    }
                }
                return (T) o;
            }
        }
        return (T) buffer.toString();
    }


    public String toString() {
        return String.format("Content(type=%s, value=%s)", type, getValue());
    }

}
