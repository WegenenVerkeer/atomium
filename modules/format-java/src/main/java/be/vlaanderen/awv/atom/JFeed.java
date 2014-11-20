package be.vlaanderen.awv.atom;

import be.vlaanderen.awv.atom.Adapters.AtomDateTimeAdapter;
import lombok.Data;
import org.joda.time.DateTime;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;

@Data
@XmlRootElement(namespace = "http://www.w3.org/2005/Atom", name = "feed")
@XmlType(propOrder = {"base", "id", "title", "generator", "updated", "links", "entries"})
@XmlAccessorType(XmlAccessType.NONE)
public class JFeed<T> {

    @XmlElement(required = true)
    private String id;

    @XmlAttribute(name="base", namespace = "http://www.w3.org/XML/1998/namespace")
    private String base;

    @XmlElement
    private String title;

    @XmlElement
    private JGenerator generator;

    @XmlElement @XmlJavaTypeAdapter(AtomDateTimeAdapter.class)
    private DateTime updated;

    @XmlElement(name = "link")
    private List<JLink> links = new ArrayList<>();

    @XmlElement(name = "entry")
    private List<JEntry<T>> entries = new ArrayList<>();
}
