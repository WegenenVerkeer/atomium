package be.wegenenverkeer.atomium.format.pub;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Control control = (Control) o;

        return draft == control.draft;

    }

    @Override
    public int hashCode() {
        return draft != null ? draft.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Control{" +
                "draft='" + draft + '\'' +
                '}';
    }
}
