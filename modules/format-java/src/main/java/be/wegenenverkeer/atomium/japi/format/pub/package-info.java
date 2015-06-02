@XmlSchema(
        namespace = "http://www.w3.org/2007/app",
        elementFormDefault = XmlNsForm.QUALIFIED,
        xmlns = {
                @XmlNs(prefix="app", namespaceURI="http://www.w3.org/2007/app")
        }
)
package be.wegenenverkeer.atomium.japi.format.pub;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;