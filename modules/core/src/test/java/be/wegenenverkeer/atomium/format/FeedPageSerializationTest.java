package be.wegenenverkeer.atomium.format;

import be.wegenenverkeer.atomium.api.Entry;
import be.wegenenverkeer.atomium.api.FeedPage;
import be.wegenenverkeer.atomium.api.FeedPageCodec;
import be.wegenenverkeer.atomium.format.pub.AtomPubEntry;
import be.wegenenverkeer.atomium.format.pub.Control;
import be.wegenenverkeer.atomium.format.pub.Draft;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class FeedPageSerializationTest {

    static OffsetDateTime dateTime = ZonedDateTime.now().with(ChronoField.MILLI_OF_SECOND, 0).toOffsetDateTime();

    private Customer customer = new Customer("cname", 666);
    private FeedPageCodec<Customer, String> jaxbCustomerCodec = new JaxbCodec<>(Customer.class);
    private FeedPageCodec<Customer, String> jsonCustomerCodec = new JacksonJSONCodec<>(Customer.class);
    private FeedPageCodec<String, String> jaxbStringCodec = new JaxbCodec<>(String.class);
    private FeedPageCodec<String, String> jsonStringCodec = new JacksonJSONCodec<>(String.class);


    @Test
    public void testMarshallingFeedWithStrings() throws Exception {
        FeedBuilder<String> builder = FeedBuilder.stringFeed();
        builder.addEntry(new AtomEntry<>("id1", dateTime, new Content<>("foo", "text/plain"), new ArrayList<>()));
        //this will be escaped in xml but not in json
        builder.addEntry(new AtomEntry<>("id2", dateTime, new Content<>("<html><p>bla</p></html>", "text/html"), new ArrayList<>()));
        // \n will be escaped in json not in xml
        builder.addEntry(new AtomEntry<>("id3", dateTime, new Content<>("\n   ---foo---   \n   ---bar---   \n", "text/plain"), new ArrayList<>()));
        // json text will be quoted in json not in xml
        builder.addEntry(new AtomEntry<>("id4", dateTime, new Content<>("{'foo': 'bar'}", "application/json"), new ArrayList<>()));

        check(builder.feedPage, jsonStringCodec);
        check(builder.feedPage, jaxbStringCodec);
    }

    @Test
    public void testMarshallingFeedWithCustomers() throws Exception {

        FeedBuilder<Customer> builder = FeedBuilder.customerFeed();
        builder.addEntry(new AtomEntry<>("id", dateTime, new Content<>(customer, "application/xml"), new ArrayList<>()));

        check(builder.feedPage, jsonCustomerCodec);
        check(builder.feedPage, jaxbCustomerCodec);
    }

    @Test
    public void testMarshallingAtomPubFeedWithCustomers() throws Exception {

        FeedBuilder<Customer> builder = FeedBuilder.customerFeed();
        builder.addEntry(
                new AtomPubEntry<>(
                        "id",
                        dateTime,
                        new Content<>(customer, "application/xml"),
                        new ArrayList<>(),
                        dateTime,
                        new Control(Draft.YES)
                )
        );
        builder.addEntry(
                new AtomPubEntry<>(
                        "id",
                        dateTime,
                        new Content<>(customer, "application/xml"),
                        new ArrayList<>(),
                        dateTime,
                        new Control(Draft.NO)
                )
        );
        check(builder.feedPage, jsonCustomerCodec);
        check(builder.feedPage, jaxbCustomerCodec);
    }



    private <E> void check(FeedPage feedPage, FeedPageCodec<E, String> codec) throws Exception {
        String json = marshal(feedPage, codec);
        FeedPage<E> feedPageFromJson = unmarshal(json, codec);
        assertEquals("different getUpdated on json:", feedPage.getUpdated(), feedPageFromJson.getUpdated());
        feedPageFromJson.setUpdated(feedPage.getUpdated());
        assertEquals(feedPage, feedPageFromJson);
    }

    private <E> String marshal(FeedPage feedPage, FeedPageCodec<E, String> codec) throws Exception {
        return codec.encode(feedPage);
    }

    private <E> FeedPage<E> unmarshal(String xml, FeedPageCodec<E, String> codec) throws Exception {
        return codec.decode(xml);
    }


    @XmlRootElement
    @XmlAccessorType(XmlAccessType.NONE)
    public static class Customer {
        @XmlElement
        public String name;
        @XmlAttribute
        public int id;

        public Customer() {
        }

        public Customer(String cname, int i) {
            this.name = cname;
            this.id = i;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Customer customer = (Customer) o;

            if (id != customer.id) return false;
            if (name != null ? !name.equals(customer.name) : customer.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + id;
            return result;
        }

        @Override
        public String toString() {
            return "Customer{" +
                    "name='" + name + '\'' +
                    ", id=" + id +
                    '}';
        }
    }

    private static class FeedBuilder<T> {
        private final FeedPage<T> feedPage;

        FeedBuilder(FeedPage<T> feedPage) {
            this.feedPage = feedPage;
        }

        public static FeedBuilder<String> stringFeed() {
            FeedPage<String> feedPage = new FeedPage<>(
                    "urn:id:" + UUID.randomUUID().toString(),
                    "http://www.example.org",
                    "strings of life",
                    null,
                    dateTime
            );
            feedPage.getLinks().add(new Link("self", "foo"));
            return new FeedBuilder<>(feedPage);
        }

        public static FeedBuilder<Customer> customerFeed() {

            FeedPage<Customer> feedPage = new FeedPage<>(
                    "urn:id:" + UUID.randomUUID().toString(),
                    "http://www.example.org",
                    "customers",
                    new Generator("atomium", "http://github.com/WegenenVerkeer/atomium", "0.0.1"),
                    dateTime
            );

            feedPage.getLinks().add(new Link("self", "foo"));

            return new FeedBuilder<>(feedPage);
        }

        public static FeedBuilder<JAXBElement> jabxFeed() {

            FeedPage<JAXBElement> feedPage = new FeedPage<>();
            feedPage.setId("urn:id:" + UUID.randomUUID().toString());
            feedPage.setUpdated(dateTime);
            feedPage.getLinks().add(new Link("self", "foo"));

            return new FeedBuilder<>(feedPage);
        }

        public FeedBuilder<T> addEntry(Entry<T> entry) {
            feedPage.getEntries().add(entry);
            return this;
        }
    }
}
