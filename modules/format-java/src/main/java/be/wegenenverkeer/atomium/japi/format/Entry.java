package be.wegenenverkeer.atomium.japi.format;

import be.wegenenverkeer.atomium.japi.format.pub.AtomPubEntry;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.joda.time.DateTime;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AtomPubEntry.class, name = "atom-pub"),
        @JsonSubTypes.Type(value = AtomEntry.class, name = "atom")})
@XmlJavaTypeAdapter(Adapters.AtomEntryAdapter.class)
public abstract class Entry<T> {

    public abstract String getId();

    public abstract DateTime getUpdated();

    public abstract Content<T> getContent();

    public abstract List<Link> getLinks();

}