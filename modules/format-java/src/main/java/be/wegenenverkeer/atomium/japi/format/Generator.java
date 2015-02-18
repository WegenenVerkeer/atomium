package be.wegenenverkeer.atomium.japi.format;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.NONE)
public final class Generator {

    @XmlValue
    private String text;

    @XmlAttribute
    private String uri;

    @XmlAttribute
    private String version;

    private Generator() {
    }

    public Generator(String text, String uri, String version) {
        this.text = text;
        this.uri = uri;
        this.version = version;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Generator that = (Generator) o;

        if (!text.equals(that.text)) return false;
        if (uri != null ? !uri.equals(that.uri) : that.uri != null) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = text.hashCode();
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }
}
