package be.wegenenverkeer.atom.java;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.NONE)
public final class Link {

    @XmlAttribute
    private String rel;

    @XmlAttribute
    private String href;

    private Link() {
    }

    public Link(String rel, String href) {
        this.rel = rel;
        this.href = href;
    }

    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Link link = (Link) o;

        if (!href.equals(link.href)) return false;
        if (!rel.equals(link.rel)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = rel.hashCode();
        result = 31 * result + href.hashCode();
        return result;
    }
}
