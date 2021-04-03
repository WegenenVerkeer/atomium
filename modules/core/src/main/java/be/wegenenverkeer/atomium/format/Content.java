package be.wegenenverkeer.atomium.format;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.Objects;

@XmlAccessorType(XmlAccessType.NONE)
public final class Content<T> {

    @XmlAttribute(name="type")
    private String type;

    @XmlElement
    @JsonProperty("value")
    private T value;

    public Content() {}

    @SuppressWarnings("unchecked")
    public Content(T value, String type) {
        this.type = type;
        this.value = value;
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
        return value;
    }

    public String toString() {
        return String.format("Content(type=%s, value=%s)", type, getValue());
    }

}
