package be.vlaanderen.awv.atom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.NONE)
public final class JContent<T> {

    private static final ThreadLocal<JAXBElementUnmarshaller> jaxbElementUnmarshallerThreadLocal = new ThreadLocal<>();

    public static void setJAXBElementUnmarshaller(JAXBElementUnmarshaller jaxbElementUnmarshaller) {
        JContent.jaxbElementUnmarshallerThreadLocal.set(jaxbElementUnmarshaller);
    }

    public static void resetJAXBElementUnmarshaller() {
        JContent.jaxbElementUnmarshallerThreadLocal.remove();
    }

    @XmlAttribute(name="type")
    private String type;

    public String getType() {
        return this.type;
    }

    @XmlAnyElement(lax = true) @XmlMixed @JsonIgnore
    private List objects = new ArrayList();

    @XmlTransient
    private T value;

    public JContent() {}

    public JContent(T value, String type) {
        this.type = type;
        objects.add(value);
        if (value.getClass().isAnnotationPresent(XmlRootElement.class) || value instanceof String) {
            this.value = value;
        } else if (value instanceof JAXBElement) {
            this.value = ((JAXBElement<T>) value).getValue();
        } else {
            throw new IllegalArgumentException("type not supported "+value.getClass());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JContent jContent = (JContent) o;

        if (type != null ? !type.equals(jContent.type) : jContent.type != null) return false;
        T value = getValue();
        Object otherValue = jContent.getValue();
        if (value != null ? !value.equals(otherValue) : otherValue != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        T value = getValue();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    public T getValue() {
        if (value == null) {
            value = getValueFromObjects();
        }
        return value;
    }

    private T getValueFromObjects() {
        StringBuffer buffer = new StringBuffer();
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
        return String.format("JContent(type=%s, value=%s)", type, getValue());
    }

}