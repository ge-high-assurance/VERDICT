/* See LICENSE in project directory */
package edu.uiowa.clc.verdict.blm;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for Link complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Link">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="LinkID" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Compromised" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Link")
public class Link {

    @XmlAttribute(name = "LinkID")
    protected String linkID;

    @XmlAttribute(name = "Compromised")
    protected Boolean compromised;

    /**
     * Gets the value of the linkID property.
     *
     * @return possible object is {@link String }
     */
    public String getLinkID() {
        return linkID;
    }

    /**
     * Sets the value of the linkID property.
     *
     * @param value allowed object is {@link String }
     */
    public void setLinkID(String value) {
        this.linkID = value;
    }

    /**
     * Gets the value of the compromised property.
     *
     * @return possible object is {@link Boolean }
     */
    public Boolean isCompromised() {
        return compromised;
    }

    /**
     * Sets the value of the compromised property.
     *
     * @param value allowed object is {@link Boolean }
     */
    public void setCompromised(Boolean value) {
        this.compromised = value;
    }
}
