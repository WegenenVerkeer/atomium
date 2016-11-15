package be.wegenenverkeer.atomium.format;

import be.wegenenverkeer.atomium.api.Entry;
import be.wegenenverkeer.atomium.format.pub.Control;
import be.wegenenverkeer.atomium.format.pub.AtomPubEntry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class Adapters {


    public static java.time.format.DateTimeFormatter formatter =
            new java.time.format.DateTimeFormatterBuilder()
                    .append(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                    .appendPattern("XXXXX")
                    .toFormatter();

    public static java.time.format.DateTimeFormatter datetimeParser =
            new java.time.format.DateTimeFormatterBuilder()
                    .append(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]"))
                    .optionalStart()
                    .appendPattern("XXXX")
                    .optionalEnd()
                    .optionalStart()
                    .appendPattern("XXX")
                    .optionalEnd()
                    .toFormatter();


    public static class AtomDateTimeAdapter extends XmlAdapter<String, OffsetDateTime> {

        @Override
        public OffsetDateTime unmarshal(String v) throws Exception {
            return OffsetDateTime.parse(v, datetimeParser);
        }

        @Override
        public String marshal(OffsetDateTime v) throws Exception {
            return formatter.format(v);
        }
    }

    public static class AtomEntryAdapter<T> extends XmlAdapter<AtomEntryAdapter.AdaptedEntry<T>, Entry<T>> {


        public static <E> Entry<E> toEntry(AtomEntryAdapter.AdaptedEntry<E> adaptedEntry) {
            if (adaptedEntry.control != null) {

                return new AtomPubEntry<>(
                        adaptedEntry.id,
                        adaptedEntry.updated,
                        adaptedEntry.content,
                        adaptedEntry.links,
                        adaptedEntry.edited,
                        adaptedEntry.control
                );

            } else {

                return new AtomEntry<>(
                        adaptedEntry.id,
                        adaptedEntry.updated,
                        adaptedEntry.content,
                        adaptedEntry.links
                );
            }
        }

        public static <E> AtomEntryAdapter.AdaptedEntry<E> toAdaptedEntry(Entry<E> entry) {

            AtomEntryAdapter.AdaptedEntry<E> adaptedEntry = new AtomEntryAdapter.AdaptedEntry<>();
            adaptedEntry.id = entry.getId();
            adaptedEntry.content = entry.getContent();
            adaptedEntry.links = entry.getLinks();
            adaptedEntry.updated = entry.getUpdated();

            if (entry instanceof AtomPubEntry) {
                AtomPubEntry atomPubEntry = (AtomPubEntry) entry;
                adaptedEntry.control = atomPubEntry.getControl();
                adaptedEntry.edited = atomPubEntry.getEdited();
            }

            return adaptedEntry;
        }

        @Override
        public Entry<T> unmarshal(AdaptedEntry<T> adaptedEntry) throws Exception {
            return toEntry(adaptedEntry);
        }

        @Override
        public AdaptedEntry<T> marshal(Entry<T> entry) throws Exception {
            return toAdaptedEntry(entry);
        }

        @XmlAccessorType(XmlAccessType.NONE)
        public static class AdaptedEntry<E> {

            @XmlElement
            public String id;

            @XmlElement
            @XmlJavaTypeAdapter(AtomDateTimeAdapter.class)
            public OffsetDateTime updated;

            @XmlElement
            public Content<E> content;

            @XmlElement(name = "link")
            public List<Link> links = new ArrayList<>();


            @XmlElement(namespace = "http://www.w3.org/2007/app")
            @XmlJavaTypeAdapter(AtomDateTimeAdapter.class)
            public OffsetDateTime edited;

            @XmlElement(namespace = "http://www.w3.org/2007/app")
            public Control control;

        }
    }
}
