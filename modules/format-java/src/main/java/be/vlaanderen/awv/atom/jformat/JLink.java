package be.vlaanderen.awv.atom.jformat;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.NONE)
public final class JLink {

    @XmlAttribute
    private String rel;

    @XmlAttribute
    private String href;

    private JLink() {
    }

    public JLink(String rel, String href) {
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

        JLink jLink = (JLink) o;

        if (!href.equals(jLink.href)) return false;
        if (!rel.equals(jLink.rel)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = rel.hashCode();
        result = 31 * result + href.hashCode();
        return result;
    }
}
