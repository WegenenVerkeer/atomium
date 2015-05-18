package be.wegenenverkeer.atomium.japi.format;

import be.wegenenverkeer.atomium.japi.format.pub.AtomPubEntry;
import be.wegenenverkeer.atomium.japi.format.pub.Control;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;

public class Adapters {

    public static DateTimeFormatter outputFormatterWithSecondsAndOptionalTZ = new DateTimeFormatterBuilder()
            .append(ISODateTimeFormat.dateHourMinuteSecond())
            .appendTimeZoneOffset("Z", true, 2, 4)
            .toFormatter();

    public static class AtomDateTimeAdapter extends XmlAdapter<String, DateTime> {

        @Override
        public DateTime unmarshal(String v) throws Exception {
            return outputFormatterWithSecondsAndOptionalTZ.parseDateTime(v).toDateTime();
        }

        @Override
        public String marshal(DateTime v) throws Exception {
            return outputFormatterWithSecondsAndOptionalTZ.print(v);
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
            @XmlJavaTypeAdapter(Adapters.AtomDateTimeAdapter.class)
            public DateTime updated;

            @XmlElement
            public Content<E> content;

            @XmlElement(name = "link")
            public List<Link> links = new ArrayList<>();


            @XmlElement(namespace = "http://www.w3.org/2007/app")
            @XmlJavaTypeAdapter(Adapters.AtomDateTimeAdapter.class)
            public DateTime edited;

            @XmlElement(namespace = "http://www.w3.org/2007/app")
            public Control control;

        }
    }
}
