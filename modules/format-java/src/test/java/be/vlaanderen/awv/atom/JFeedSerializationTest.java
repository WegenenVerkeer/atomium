package be.vlaanderen.awv.atom;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import lombok.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.*;
import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class JFeedSerializationTest {

    @XmlRootElement
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlAccessorType(XmlAccessType.NONE)
    public static class Customer {
        @XmlElement
        private String name;
        @XmlAttribute
        private int id;

    }

    private JAXBContext jaxbContext;
    private ObjectMapper objectMapper;
    private JFeed stringsFeed;
    private JFeed customersFeed;
    private JFeed jaxbElementFeed;

    private Customer customer = new Customer("cname", 666);

    @Before
    public void setup() throws JAXBException {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        jaxbContext = JAXBContext.newInstance(JFeed.class, JLink.class, Customer.class);

        DateTime dateTime = new DateTime(DateTimeZone.forTimeZone(TimeZone.getDefault())).withMillisOfSecond(0);

        stringsFeed = new JFeed();
        stringsFeed.setBase("http://www.example.org");
        stringsFeed.setId("urn:id:"+ UUID.randomUUID().toString());
        stringsFeed.setTitle("strings of life");
        stringsFeed.setUpdated(dateTime.withMillisOfSecond(0));
        JLink selfLink = new JLink("self", "foo");
        stringsFeed.getLinks().add(selfLink);
        stringsFeed.getEntries().add(new JEntry(new JContent<>("foo", "text/plain")));
        stringsFeed.getEntries().add(new JEntry(new JContent<>("<html><p>bla</p></html>", "text/html"))); //this will be escaped in xml but not in json
        stringsFeed.getEntries().add(new JEntry(new JContent<>("\n   ---foo---   \n   ---bar---   \n", "text/plain"))); // \n will be escaped in json not in xml
        stringsFeed.getEntries().add(new JEntry(new JContent<>("{'foo': 'bar'}", "application/json"))); // json text will be quoted in json not in xml

        customersFeed = new JFeed();
        customersFeed.setBase("http://www.example.org");
        customersFeed.setGenerator(new JGenerator("atomium", "http://github.com/WegenenVerkeer/atomium", "0.0.1"));
        customersFeed.setId("urn:id:" + UUID.randomUUID().toString());
        customersFeed.setTitle("customers");
        customersFeed.setUpdated(dateTime);
        customersFeed.getLinks().add(selfLink);
        customersFeed.getEntries().add(new JEntry(new JContent<>(customer, "application/xml")));

        jaxbElementFeed = new JFeed();
        jaxbElementFeed.setId("urn:id:" + UUID.randomUUID().toString());
        jaxbElementFeed.setUpdated(dateTime);
        JAXBElement<Integer> jaxbElement = new JAXBElement<>(new QName("http://www.w3.org/2001/XMLSchema-instance", "int"), Integer.class, 999);
        jaxbElementFeed.getEntries().add(new JEntry(new JContent(jaxbElement, "application/xml")));
        JAXBElement<Integer> jaxbElement2 = new JAXBElement<>(new QName("http://www.w3.org/2001/XMLSchema-instance", "int"), Integer.class, 1010);
        jaxbElementFeed.getEntries().add(new JEntry(new JContent(jaxbElement2, "application/xml")));

    }

    @Test
    public void testMarshallingJFeedWithStrings() throws JAXBException, IOException {
        checkXml(stringsFeed);
        checkJson(stringsFeed, new TypeReference<JFeed<String>>() { });
    }

    @Test
    public void testMarshallingJFeedWithCustomers() throws JAXBException, IOException {
        checkXml(customersFeed);
        checkJson(customersFeed, new TypeReference<JFeed<Customer>>() { });
    }

    @Test
    public void testMarshallingJFeedWithJaxbElement() throws JAXBException, IOException {
        checkXml(jaxbElementFeed);
        checkJson(jaxbElementFeed, new TypeReference<JFeed<Integer>>() { });
    }

    private void checkXml(JFeed feed) throws JAXBException {
        String xml = marshalToXml(feed);
        System.out.println(xml);
        JFeed feedFromXml = unmarshalFromXml(xml);
        JContent.setJAXBElementUnmarshaller(new JAXBElementUnmarshaller(jaxbContext, Integer.class));
        assertEquals(feed.getUpdated().getMillis(), feedFromXml.getUpdated().getMillis());
        feedFromXml.setUpdated(feed.getUpdated());
        assertEquals(feed, feedFromXml);
    }

    private void checkJson(JFeed feed, TypeReference typeReference) throws IOException {
        String json = marshalToJson(feed);
        System.out.println(json);
        JFeed feedFromJson = unmarshalFromJson(json, typeReference);
        assertEquals(feed.getUpdated().getMillis(), feedFromJson.getUpdated().getMillis());
        feedFromJson.setUpdated(feed.getUpdated());
        assertEquals(feed, feedFromJson);
    }

    private String marshalToJson(JFeed feed) throws IOException {
        StringWriter writer = new StringWriter();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, feed);
        return writer.toString();
    }

    private String marshalToXml(JFeed feed) throws JAXBException {
        StringWriter writer = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(feed, writer);
        return writer.toString();
    }

    private JFeed unmarshalFromXml(String xml) throws JAXBException {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return (JFeed) unmarshaller.unmarshal(new StringReader(xml));
    }

    private JFeed unmarshalFromJson(String json, TypeReference typeReference) throws IOException {
        return objectMapper.readValue(new StringReader(json), typeReference);
    }
}
