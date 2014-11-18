package be.vlaanderen.awv.atom.jformat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.NONE)
public class JGenerator {

    @XmlValue
    private String text;

    @XmlAttribute
    private String uri;

    @XmlAttribute
    private String version;

}
