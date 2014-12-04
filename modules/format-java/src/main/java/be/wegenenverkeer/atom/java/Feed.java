package be.wegenenverkeer.atom.java;

import be.wegenenverkeer.atom.java.Adapters.AtomDateTimeAdapter;
import org.joda.time.LocalDateTime;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(namespace = "http://www.w3.org/2005/Atom", name = "feed")
@XmlType(propOrder = {"base", "id", "title", "generator", "updated", "links", "entries"})
@XmlAccessorType(XmlAccessType.NONE)
public final class Feed<T> {

    /**
     * no arg constructor, needed for JAXB and/or Jackson POJO support
     */
    public Feed() {
    }

    public Feed(String id, String base, String title, Generator generator, LocalDateTime updated) {
        this(id, base, title, generator, updated, new ArrayList<Link>(), new ArrayList<Entry<T>>());
    }


    public Feed(String id, String base, String title, Generator generator,
                LocalDateTime updated, List<Link> links, List<Entry<T>> entries) {
        this.id = id;
        this.base = base;
        this.title = title;
        this.generator = generator;
        this.updated = updated;
        this.links = links;
        this.entries = entries;
    }

    @XmlElement(required = true)
    private String id;

    @XmlAttribute(name="base", namespace = "http://www.w3.org/XML/1998/namespace")
    private String base;

    @XmlElement
    private String title;

    @XmlElement
    private Generator generator;

    @XmlElement @XmlJavaTypeAdapter(AtomDateTimeAdapter.class)
    private LocalDateTime updated;

    @XmlElement(name = "link")
    private List<Link> links = new ArrayList<>();

    @XmlElement(name = "entry")
    private List<Entry<T>> entries = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Generator getGenerator() {
        return generator;
    }

    public void setGenerator(Generator generator) {
        this.generator = generator;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public List<Entry<T>> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry<T>> entries) {
        this.entries = entries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Feed feed = (Feed) o;

        if (!base.equals(feed.base)) return false;
        if (entries != null ? !entries.equals(feed.entries) : feed.entries != null) return false;
        if (generator != null ? !generator.equals(feed.generator) : feed.generator != null) return false;
        if (!id.equals(feed.id)) return false;
        if (links != null ? !links.equals(feed.links) : feed.links != null) return false;
        if (title != null ? !title.equals(feed.title) : feed.title != null) return false;
        if (updated != null ? !updated.equals(feed.updated) : feed.updated != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + base.hashCode();
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (generator != null ? generator.hashCode() : 0);
        result = 31 * result + (updated != null ? updated.hashCode() : 0);
        result = 31 * result + (links != null ? links.hashCode() : 0);
        result = 31 * result + (entries != null ? entries.hashCode() : 0);
        return result;
    }
}
