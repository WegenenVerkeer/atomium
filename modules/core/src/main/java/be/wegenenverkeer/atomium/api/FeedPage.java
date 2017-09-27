package be.wegenenverkeer.atomium.api;

import be.wegenenverkeer.atomium.format.Adapters;
import be.wegenenverkeer.atomium.format.Entry;
import be.wegenenverkeer.atomium.format.Generator;
import be.wegenenverkeer.atomium.format.Link;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@XmlRootElement(namespace = "http://www.w3.org/2005/Atom", name = "feed")
@JsonPropertyOrder({"id", "base", "title", "generator", "updated", "links", "entries"})
@XmlType(propOrder = {"base", "id", "title", "generator", "updated", "links", "entries"})
@XmlAccessorType(XmlAccessType.NONE)
public final class FeedPage<T> {

    /**
     * no arg constructor, needed for JAXB and/or Jackson POJO support
     */
    public FeedPage() {
    }

    public FeedPage(String id, String base, String title, Generator generator) {
        this(id, base, title, generator, OffsetDateTime.now(), new ArrayList<>(), new ArrayList<>());
    }

    public FeedPage(String id, String base, String title, Generator generator, OffsetDateTime updated) {
        this(id, base, title, generator, updated, new ArrayList<>(), new ArrayList<>());
    }


    public FeedPage(String id, String base, String title, Generator generator,
                    OffsetDateTime updated, List<Link> links, List<Entry<T>> entries) {
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

    @XmlElement @XmlJavaTypeAdapter(Adapters.AtomDateTimeAdapter.class)
    private OffsetDateTime updated;

    @XmlElement(name = "link")
    @JsonProperty("links")
    private List<Link> links = new ArrayList<>();

    @XmlElement(name = "entry")
    @JsonProperty("entries")
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

    public OffsetDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(OffsetDateTime updated) {
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


    public Optional<Link> findLinkByName(String relName) {
        return links.stream()
                .filter(l -> l.getRel().equals(relName))
                .findFirst();
    }

   public Link selfLink() {
       return findLinkByName(Link.SELF).get();// safe, since invariant is checked in constructor
   }

   public Optional<Link> nextLink() {
       return findLinkByName(Link.NEXT);
   }

   public Optional<Link> firstLink() {
       return findLinkByName(Link.FIRST);
   }

    public Optional<Link> previousLink() {
        return findLinkByName(Link.PREVIOUS);
    }

    public Optional<Link> lastLink(){
        return findLinkByName(Link.LAST);
    }

    public Optional<Link> collectionLink() {
        return findLinkByName(Link.COLLECTION);
    }

    /**
     * A Feed page is complete (and won't change anymore), if there is a 'previous' link.
     *
     * @return true iff the feed page is completed, and won't change anymore.
     */
    public boolean complete() {
        return previousLink().isPresent();
    }

    public String calcETag() {
        try {
            MessageDigest message = MessageDigest.getInstance("MD5");
            String utf = "UTF-8";
            updateMessage(message, this.base);
            updateMessage(message, this.id);
            updateMessage(message, this.updated.toString());
            links.stream().forEach(link ->
                    updateMessage(message, link.toString())
            );
            entries.stream().forEach(entry -> {
                        updateMessage(message, entry.getId());
                        updateMessage(message, entry.getUpdated().toString());
                        return;
                    }
            );
            return new BigInteger(1, message.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            // then don't return ETag
            return "";
        }
    }

    private void updateMessage(MessageDigest digest, String el) {
        try {
            digest.update(el.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // don't update
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeedPage feedPage = (FeedPage) o;
        if (base == null && feedPage.base != null) return false;
        if (base != null && !base.equals(feedPage.base)) return false;
        if (entries != null ? !entries.equals(feedPage.entries) : feedPage.entries != null) return false;
        if (generator != null ? !generator.equals(feedPage.generator) : feedPage.generator != null) return false;
        if (!id.equals(feedPage.id)) return false;
        if (links != null ? !links.equals(feedPage.links) : feedPage.links != null) return false;
        if (title != null ? !title.equals(feedPage.title) : feedPage.title != null) return false;
        if (updated != null ? !updated.equals(feedPage.updated) : feedPage.updated != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (base != null ? base.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (generator != null ? generator.hashCode() : 0);
        result = 31 * result + (updated != null ? updated.hashCode() : 0);
        result = 31 * result + (links != null ? links.hashCode() : 0);
        result = 31 * result + (entries != null ? entries.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Feed{" +
                "id='" + id + '\'' +
                ", base='" + base + '\'' +
                ", title='" + title + '\'' +
                ", generator=" + generator +
                ", updated=" + updated +
                ", links=" + links +
                ", entries=" + entries +
                '}';
    }
}
