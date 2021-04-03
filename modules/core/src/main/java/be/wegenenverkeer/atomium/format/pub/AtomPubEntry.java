package be.wegenenverkeer.atomium.format.pub;

import be.wegenenverkeer.atomium.format.Adapters;
import be.wegenenverkeer.atomium.format.Content;
import be.wegenenverkeer.atomium.format.Entry;
import be.wegenenverkeer.atomium.format.Link;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class AtomPubEntry<T> extends Entry<T> {

    @XmlElement
    @XmlJavaTypeAdapter(Adapters.AtomDateTimeAdapter.class)
    OffsetDateTime updated;
    @XmlElement
    private String id;
    @XmlElement
    private Content<T> content;

    @XmlElement(name = "link")
    @JsonProperty("links")
    private List<Link> links = new ArrayList<>();


    @XmlElement(namespace = "http://www.w3.org/2007/app")
    @XmlJavaTypeAdapter(Adapters.AtomDateTimeAdapter.class)
    private OffsetDateTime edited;

    @XmlElement(namespace = "http://www.w3.org/2007/app")
    private Control control;


    /**
     * no arg constructor, needed for JAXB and/or Jackson POJO support
     */
    @SuppressWarnings("unused")
    private AtomPubEntry() {

    }

    public AtomPubEntry(String id, OffsetDateTime updated, Content<T> content, List<Link> links, OffsetDateTime edited, Control control) {
        this.id = id;
        this.updated = updated;
        this.content = content;
        this.links = links;
        this.edited = edited;
        this.control = control;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public OffsetDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(OffsetDateTime updated) {
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

    public OffsetDateTime getEdited() {
        return edited;
    }

    public void setEdited(OffsetDateTime edited) {
        this.edited = edited;
    }

    public Control getControl() {
        return control;
    }

    public void setControl(Control control) {
        this.control = control;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AtomPubEntry<?> that = (AtomPubEntry<?>) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (updated != null ? !updated.equals(that.updated) : that.updated != null) return false;
        if (content != null ? !content.equals(that.content) : that.content != null) return false;
        if (links != null ? !links.equals(that.links) : that.links != null) return false;
        if (edited != null ? !edited.equals(that.edited) : that.edited != null) return false;
        return !(control != null ? !control.equals(that.control) : that.control != null);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (updated != null ? updated.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (links != null ? links.hashCode() : 0);
        result = 31 * result + (edited != null ? edited.hashCode() : 0);
        result = 31 * result + (control != null ? control.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AtomPubEntry{" +
                "id='" + id + '\'' +
                ", getUpdated=" + updated +
                ", content=" + content +
                ", links=" + links +
                ", edited=" + edited +
                ", control=" + control +
                "} ";
    }
}
