package be.wegenenverkeer.atomium.format;

import be.wegenenverkeer.atomium.format.pub.AtomPubEntry;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.OffsetDateTime;
import java.util.List;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type", defaultImpl = AtomEntry.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AtomPubEntry.class, name = "atom-pub"),
        @JsonSubTypes.Type(value = AtomEntry.class, name = "atom")})
@XmlJavaTypeAdapter(Adapters.AtomEntryAdapter.class)
public abstract class Entry<T> {

    public abstract String getId();

    public abstract OffsetDateTime getUpdated();

    public abstract Content<T> getContent();

    public abstract List<Link> getLinks();

}