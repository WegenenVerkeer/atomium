package be.vlaanderen.awv.atom.jformat;

import org.joda.time.LocalDateTime;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.NONE)
public final class JEntry<T> {

    @XmlElement
    private String id;

    @XmlElement
    LocalDateTime updated;

    @XmlElement
    private JContent<T> content;

    @XmlElement(name="link")
    private List<JLink> links = new ArrayList<>();

    /**
     * no arg constructor, needed for JAXB and/or Jackson POJO support
     */
    private JEntry() {

    }

    public JEntry(String id, JContent<T> content) {
        this(id, content, new ArrayList<JLink>());
    }

    public JEntry(String id, JContent<T> content, List<JLink> links) {
        this(id, new LocalDateTime(), content, links);
    }

    public JEntry(String id, LocalDateTime updated, JContent<T> content, List<JLink> links) {
        this.id = id;
        this.updated = updated;
        this.content = content;
        this.links = links;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public JContent<T> getContent() {
        return content;
    }

    public void setContent(JContent<T> content) {
        this.content = content;
    }

    public List<JLink> getLinks() {
        return links;
    }

    public void setLinks(List<JLink> links) {
        this.links = links;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JEntry jEntry = (JEntry) o;

        if (!content.equals(jEntry.content)) return false;
        if (!id.equals(jEntry.id)) return false;
        if (links != null ? !links.equals(jEntry.links) : jEntry.links != null) return false;
        if (updated != null ? !updated.equals(jEntry.updated) : jEntry.updated != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (updated != null ? updated.hashCode() : 0);
        result = 31 * result + content.hashCode();
        result = 31 * result + (links != null ? links.hashCode() : 0);
        return result;
    }
}
