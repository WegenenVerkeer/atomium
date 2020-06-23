package be.wegenenverkeer.atomium.client;

import javax.xml.bind.annotation.*;

// this is the Event model, annotations for XML deserialization
// and public accessors for JSON
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Event {

    @XmlElement
    public Double value;

    @XmlElement
    public String description;

    @XmlAttribute
    public Integer version;

    public Event(){
    }


    public String toString() {
        return "Event " + version + " " + "description " + " value: " + value;
    }

}
