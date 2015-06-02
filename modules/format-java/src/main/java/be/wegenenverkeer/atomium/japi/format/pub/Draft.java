package be.wegenenverkeer.atomium.japi.format.pub;

import com.fasterxml.jackson.annotation.JsonValue;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

@XmlEnum

public enum Draft {

    @XmlEnumValue("yes")
    YES,
    @XmlEnumValue("no")
    NO;

    @JsonValue
    public String getValue() {
        return name().toLowerCase();
    }
}
