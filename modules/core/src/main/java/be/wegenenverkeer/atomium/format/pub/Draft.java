package be.wegenenverkeer.atomium.format.pub;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;

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
