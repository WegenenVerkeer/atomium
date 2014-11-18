package be.vlaanderen.awv.atom;

import be.vlaanderen.awv.atom.jformat.*;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONUnmarshaller;
import lombok.*;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class AtomJavaJaxbTest {

    @XmlRootElement
    @EqualsAndHashCode
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    @XmlAccessorType(XmlAccessType.NONE)
    public static class Customer {
        @XmlElement
        private String name;
        @XmlAttribute
        private int id;

    }

    private JSONJAXBContext jaxbContext;
    private JFeed stringsFeed;
    private JFeed customersFeed;
    private JFeed jaxbElementFeed;

    private Customer customer = new Customer("cname", 666);

    @Before
    public void setup() throws JAXBException {
        Map<String, String> jsonXml2JsonNs = new HashMap<>();
        jsonXml2JsonNs.put("http://www.w3.org/2005/Atom", "");
        jsonXml2JsonNs.put("http://www.w3.org/XML/1998/namespace", "");
        JSONConfiguration config = JSONConfiguration.mapped().rootUnwrapping(true).
                xml2JsonNs(jsonXml2JsonNs).
                arrays("link", "entry").build();
        jaxbContext = new JSONJAXBContext(config, JFeed.class, JLink.class, Customer.class);

        stringsFeed = new JFeed();
        stringsFeed.setBase("http://www.example.org");
        stringsFeed.setId("urn:id:"+ UUID.randomUUID().toString());
        stringsFeed.setTitle("strings of life");
        stringsFeed.setUpdated(new DateTime().withMillisOfSecond(0));
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
        customersFeed.setUpdated(new DateTime().withMillisOfSecond(0));
        customersFeed.getLinks().add(selfLink);
        customersFeed.getEntries().add(new JEntry(new JContent<>(customer, "application/xml")));

        jaxbElementFeed = new JFeed();
        jaxbElementFeed.setId("urn:id:" + UUID.randomUUID().toString());
        jaxbElementFeed.setUpdated(new DateTime().withMillisOfSecond(0));
        JAXBElement<Integer> jaxbElement = new JAXBElement<>(new QName("http://www.w3.org/2001/XMLSchema-instance", "int"), Integer.class, 999);
        jaxbElementFeed.getEntries().add(new JEntry(new JContent(jaxbElement, "application/xml")));
        JAXBElement<Integer> jaxbElement2 = new JAXBElement<>(new QName("http://www.w3.org/2001/XMLSchema-instance", "int"), Integer.class, 1010);
        jaxbElementFeed.getEntries().add(new JEntry(new JContent(jaxbElement2, "application/xml")));

    }

    @Test
    public void testMarshallingJFeedWithStrings() throws JAXBException {
        checkJson(stringsFeed);
        checkXml(stringsFeed);
    }

    @Test
    public void testMarshallingJFeedWithCustomers() throws JAXBException {
        checkJson(customersFeed);
        checkXml(customersFeed);
    }

    @Test
    public void testMarshallingJFeedWithJaxbElement() throws JAXBException {
        checkXml(jaxbElementFeed);
        checkJson(jaxbElementFeed);
    }

    private void checkXml(JFeed feed) throws JAXBException {
        String xml = marshalToXml(feed);
        System.out.println(xml);
        JFeed feedFromXml = unmarshalFromXml(xml);
        JContent.setJAXBElementUnmarshaller(new JAXBElementUnmarshaller(jaxbContext, Integer.class));
        assertEquals(feed, feedFromXml);
    }

    private void checkJson(JFeed feed) throws JAXBException {
        String json = marshalToJson(feed);
        System.out.println(json);
        JFeed feedFromJson = unmarshalFromJson(json);
        assertEquals(feed, feedFromJson);
    }

    private String marshalToJson(JFeed feed) throws JAXBException {
        StringWriter writer = new StringWriter();
        jaxbContext.createJSONMarshaller().marshallToJSON(feed, writer);
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

    private JFeed unmarshalFromJson(String json) throws JAXBException {
        JSONUnmarshaller unmarshaller = jaxbContext.createJSONUnmarshaller();
        return unmarshaller.unmarshalFromJSON(new StringReader(json), JFeed.class);
    }
}
