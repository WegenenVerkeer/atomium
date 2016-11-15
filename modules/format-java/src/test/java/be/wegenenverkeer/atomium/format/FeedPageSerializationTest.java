package be.wegenenverkeer.atomium.format;

import be.wegenenverkeer.atomium.api.Entry;
import be.wegenenverkeer.atomium.api.FeedPage;
import be.wegenenverkeer.atomium.format.pub.Control;
import be.wegenenverkeer.atomium.format.pub.Draft;
import be.wegenenverkeer.atomium.format.pub.AtomPubEntry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.*;
import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class FeedPageSerializationTest {

    static OffsetDateTime dateTime = ZonedDateTime.now().with(ChronoField.MILLI_OF_SECOND, 0).toOffsetDateTime();
    private JAXBContext jaxbContext;
    private ObjectMapper objectMapper;
    private Customer customer = new Customer("cname", 666);

    @Before
    public void setup() throws JAXBException {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new OffsetDateTimeModule());
        objectMapper.setTimeZone(TimeZone.getDefault());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        jaxbContext = JAXBContext.newInstance(FeedPage.class, Link.class, Customer.class);
    }

    @Test
    public void testMarshallingFeedWithStrings() throws JAXBException, IOException {
        FeedBuilder<String> builder = FeedBuilder.stringFeed();
        builder.addEntry(new AtomEntry<>("id1", dateTime, new Content<>("foo", "text/plain"), new ArrayList<>()));
        //this will be escaped in xml but not in json
        builder.addEntry(new AtomEntry<>("id2", dateTime, new Content<>("<html><p>bla</p></html>", "text/html"), new ArrayList<>()));
        // \n will be escaped in json not in xml
        builder.addEntry(new AtomEntry<>("id3", dateTime, new Content<>("\n   ---foo---   \n   ---bar---   \n", "text/plain"), new ArrayList<>()));
        // json text will be quoted in json not in xml
        builder.addEntry(new AtomEntry<>("id4", dateTime, new Content<>("{'foo': 'bar'}", "application/json"), new ArrayList<>()));

        checkJson(builder.feedPage, new TypeReference<FeedPage<String>>() {
        });
        checkXml(builder.feedPage);
    }

    @Test
    public void testMarshallingFeedWithCustomers() throws JAXBException, IOException {

        FeedBuilder<Customer> builder = FeedBuilder.customerFeed();
        builder.addEntry(new AtomEntry<>("id", dateTime, new Content<>(customer, "application/xml"), new ArrayList<>()));

        checkXml(builder.feedPage);
        checkJson(builder.feedPage, new TypeReference<FeedPage<Customer>>() {
        });
    }

    @Test
    public void testMarshallingAtomPubFeedWithCustomers() throws JAXBException, IOException {

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
        checkXml(builder.feedPage);
        checkJson(builder.feedPage, new TypeReference<FeedPage<Customer>>() {
        });
    }

    @Test
    public void testMarshallingFeedWithJaxbElement() throws JAXBException, IOException {

        FeedBuilder<JAXBElement> builder = FeedBuilder.jabxFeed();
        JAXBElement<Integer> jaxbElement = new JAXBElement<>(new QName("http://www.w3.org/2001/XMLSchema-instance", "int"), Integer.class, 999);
        builder.addEntry(new AtomEntry<>("id1", dateTime, new Content<>(jaxbElement, "application/xml"), new ArrayList<>()));
        JAXBElement<Integer> jaxbElement2 = new JAXBElement<>(new QName("http://www.w3.org/2001/XMLSchema-instance", "int"), Integer.class, 1010);
        builder.addEntry(new AtomEntry<>("id2", dateTime, new Content<>(jaxbElement2, "application/xml"), new ArrayList<>()));

        checkJson(builder.feedPage, new TypeReference<FeedPage<Integer>>() {
        });
        checkXml(builder.feedPage);
    }

    private void checkXml(FeedPage feedPage) throws JAXBException {
        String xml = marshalToXml(feedPage);
        FeedPage feedPageFromXml = unmarshalFromXml(xml);
        Content.setJAXBElementUnmarshaller(new JAXBElementUnmarshaller<>(jaxbContext, Integer.class));
        assertEquals("different getUpdated on xml:", feedPage.getUpdated(), feedPageFromXml.getUpdated());
        feedPageFromXml.setUpdated(feedPage.getUpdated());
        assertEquals(feedPage, feedPageFromXml);
    }

    private void checkJson(FeedPage feedPage, TypeReference typeReference) throws IOException {
        String json = marshalToJson(feedPage);
        FeedPage feedPageFromJson = unmarshalFromJson(json, typeReference);
        assertEquals("different getUpdated on json:", feedPage.getUpdated(), feedPageFromJson.getUpdated());
        feedPageFromJson.setUpdated(feedPage.getUpdated());
        assertEquals(feedPage, feedPageFromJson);
    }

    private String marshalToJson(FeedPage feedPage) throws IOException {
        StringWriter writer = new StringWriter();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, feedPage);
        return writer.toString();
    }

    private String marshalToXml(FeedPage feedPage) throws JAXBException {
        StringWriter writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(feedPage, writer);
        return writer.toString();
    }

    private FeedPage unmarshalFromXml(String xml) throws JAXBException {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return (FeedPage) unmarshaller.unmarshal(new StringReader(xml));
    }

    private FeedPage unmarshalFromJson(String json, TypeReference typeReference) throws IOException {
        return objectMapper.readValue(new StringReader(json), typeReference);
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
