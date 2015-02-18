package be.wegenenverkeer.atomium.japi.format;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.*;
import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class FeedSerializationTest {

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.NONE)
    public static class Customer {
        @XmlElement
        private String name;
        @XmlAttribute
        private int id;

        public Customer() {
        }

        public Customer(String cname, int i) {
            this.name = cname;
            this.id = i;
        }
    }

    private JAXBContext jaxbContext;
    private ObjectMapper objectMapper;
    private Feed<String> stringsFeed;
    private Feed<Customer> customersFeed;
    private Feed<JAXBElement> jaxbElementFeed;

    private Customer customer = new Customer("cname", 666);

    @Before
    public void setup() throws JAXBException {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        jaxbContext = JAXBContext.newInstance(Feed.class, Link.class, Customer.class);

        DateTime dateTime = new DateTime().withMillisOfSecond(0);

        stringsFeed = new Feed<>("urn:id:"+ UUID.randomUUID().toString(),
                "http://www.example.org",
                "strings of life",
                null,
                dateTime);
        stringsFeed.getLinks().add(new Link("self", "foo"));
        stringsFeed.getEntries().add(new Entry<>("id1", new Content<>("foo", "text/plain")));
        stringsFeed.getEntries().add(new Entry<>("id2", new Content<>("<html><p>bla</p></html>", "text/html"))); //this will be escaped in xml but not in json
        stringsFeed.getEntries().add(new Entry<>("id3", new Content<>("\n   ---foo---   \n   ---bar---   \n", "text/plain"))); // \n will be escaped in json not in xml
        stringsFeed.getEntries().add(new Entry<>("id4", new Content<>("{'foo': 'bar'}", "application/json"))); // json text will be quoted in json not in xml

        customersFeed = new Feed<>("urn:id:" + UUID.randomUUID().toString(),
                "http://www.example.org",
                "customers",
                new Generator("atomium", "http://github.com/WegenenVerkeer/atomium", "0.0.1"),
                dateTime);
        customersFeed.getLinks().add(new Link("self", "foo"));
        customersFeed.getEntries().add(new Entry<>("id", new Content<>(customer, "application/xml")));

        jaxbElementFeed = new Feed<>();
        jaxbElementFeed.setId("urn:id:" + UUID.randomUUID().toString());
        jaxbElementFeed.setUpdated(dateTime);
        JAXBElement<Integer> jaxbElement = new JAXBElement<>(new QName("http://www.w3.org/2001/XMLSchema-instance", "int"), Integer.class, 999);
        jaxbElementFeed.getEntries().add(new Entry<>("id1", new Content<>(jaxbElement, "application/xml")));
        JAXBElement<Integer> jaxbElement2 = new JAXBElement<>(new QName("http://www.w3.org/2001/XMLSchema-instance", "int"), Integer.class, 1010);
        jaxbElementFeed.getEntries().add(new Entry<>("id2", new Content<>(jaxbElement2, "application/xml")));

    }

    @Test
    public void testMarshallingJFeedWithStrings() throws JAXBException, IOException {
        checkXml(stringsFeed);
        checkJson(stringsFeed, new TypeReference<Feed<String>>() { });
    }

    @Test
    public void testMarshallingJFeedWithCustomers() throws JAXBException, IOException {
        checkXml(customersFeed);
        checkJson(customersFeed, new TypeReference<Feed<Customer>>() { });
    }

    @Test
    public void testMarshallingJFeedWithJaxbElement() throws JAXBException, IOException {
        checkXml(jaxbElementFeed);
        checkJson(jaxbElementFeed, new TypeReference<Feed<Integer>>() { });
    }

    private void checkXml(Feed feed) throws JAXBException {
        String xml = marshalToXml(feed);
        Feed feedFromXml = unmarshalFromXml(xml);
        Content.setJAXBElementUnmarshaller(new JAXBElementUnmarshaller<>(jaxbContext, Integer.class));
        assertEquals(feed.getUpdated(), feedFromXml.getUpdated());
        feedFromXml.setUpdated(feed.getUpdated());
        assertEquals(feed, feedFromXml);
    }

    private void checkJson(Feed feed, TypeReference typeReference) throws IOException {
        String json = marshalToJson(feed);
        Feed feedFromJson = unmarshalFromJson(json, typeReference);
        assertEquals(feed.getUpdated(), feedFromJson.getUpdated());
        feedFromJson.setUpdated(feed.getUpdated());
        assertEquals(feed, feedFromJson);
    }

    private String marshalToJson(Feed feed) throws IOException {
        StringWriter writer = new StringWriter();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, feed);
        return writer.toString();
    }

    private String marshalToXml(Feed feed) throws JAXBException {
        StringWriter writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(feed, writer);
        return writer.toString();
    }

    private Feed unmarshalFromXml(String xml) throws JAXBException {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return (Feed) unmarshaller.unmarshal(new StringReader(xml));
    }

    private Feed unmarshalFromJson(String json, TypeReference typeReference) throws IOException {
        return objectMapper.readValue(new StringReader(json), typeReference);
    }
}
