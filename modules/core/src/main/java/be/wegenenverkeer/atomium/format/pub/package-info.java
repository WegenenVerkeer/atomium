@XmlSchema(
        namespace = "http://www.w3.org/2007/app",
        elementFormDefault = XmlNsForm.QUALIFIED,
        xmlns = {
                @XmlNs(prefix="app", namespaceURI="http://www.w3.org/2007/app")
        }
)
package be.wegenenverkeer.atomium.format.pub;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;