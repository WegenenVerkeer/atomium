package be.vlaanderen.awv.atom;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.NONE)
public class JEntry<T> {

    @XmlElement
    private JContent<T> content;

    @XmlElement(name="link")
    private List<JLink> links = new ArrayList<>();

    public JEntry(JContent<T> content) {
        this(content, new ArrayList<JLink>());
    }

    public JEntry(JContent<T> content, List<JLink> links) {
        this.content = content;
        this.links = links;
    }

    public JEntry() {}
}
