package be.wegenenverkeer.atom.java;

import org.joda.time.LocalDateTime;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.NONE)
public final class Entry<T> {

    @XmlElement
    private String id;

    @XmlElement
    LocalDateTime updated;

    @XmlElement
    private Content<T> content;

    @XmlElement(name="link")
    private List<Link> links = new ArrayList<>();

    /**
     * no arg constructor, needed for JAXB and/or Jackson POJO support
     */
    private Entry() {

    }

    public Entry(String id, Content<T> content) {
        this(id, content, new ArrayList<Link>());
    }

    public Entry(String id, Content<T> content, List<Link> links) {
        this(id, new LocalDateTime(), content, links);
    }

    public Entry(String id, LocalDateTime updated, Content<T> content, List<Link> links) {
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

    public Content<T> getContent() {
        return content;
    }

    public void setContent(Content<T> content) {
        this.content = content;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Entry entry = (Entry) o;

        if (!content.equals(entry.content)) return false;
        if (!id.equals(entry.id)) return false;
        if (links != null ? !links.equals(entry.links) : entry.links != null) return false;
        if (updated != null ? !updated.equals(entry.updated) : entry.updated != null) return false;

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
