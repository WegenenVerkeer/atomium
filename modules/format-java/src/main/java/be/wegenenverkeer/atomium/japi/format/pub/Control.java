package be.wegenenverkeer.atomium.japi.format.pub;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class Control {

    @XmlElement(namespace = "http://www.w3.org/2007/app")
    private Draft draft;

    public Control() {
    }

    public Control(Draft draft) {
        this.draft = draft;
    }

    public Draft getDraft() {
        return draft;
    }

    public void setDraft(Draft draft) {
        this.draft = draft;
    }

    @Override
    public String toString() {
        return "Control{" +
                "draft='" + draft + '\'' +
                '}';
    }
}
