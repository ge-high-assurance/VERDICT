/* See LICENSE in project directory */
package edu.uiowa.clc.verdict.blm;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for ViolatedProperty complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ViolatedProperty">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="PropertyDescription" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="MinA" type="{http://www.example.org/BlameAssignment}MinA"/>
 *         &lt;element name="ApplicableThreat" type="{http://www.example.org/BlameAssignment}Attack"/>
 *       &lt;/sequence>
 *       &lt;attribute name="PropertyID" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Status" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "ViolatedProperty",
        propOrder = {"propertyDescription", "minA", "applicableThreat"})
public class ViolatedProperty {

    @XmlElement(name = "PropertyDescription")
    protected String propertyDescription;

    @XmlElement(name = "MinA", required = true)
    protected MinA minA;

    @XmlElement(name = "ApplicableThreat", required = true)
    protected Attack applicableThreat;

    @XmlAttribute(name = "PropertyID")
    protected String propertyID;

    @XmlAttribute(name = "Status")
    protected Boolean status;

    /**
     * Gets the value of the propertyDescription property.
     *
     * @return possible object is {@link String }
     */
    public String getPropertyDescription() {
        return propertyDescription;
    }

    /**
     * Sets the value of the propertyDescription property.
     *
     * @param value allowed object is {@link String }
     */
    public void setPropertyDescription(String value) {
        this.propertyDescription = value;
    }

    /**
     * Gets the value of the minA property.
     *
     * @return possible object is {@link MinA }
     */
    public MinA getMinA() {
        return minA;
    }

    /**
     * Sets the value of the minA property.
     *
     * @param value allowed object is {@link MinA }
     */
    public void setMinA(MinA value) {
        this.minA = value;
    }

    /**
     * Gets the value of the applicableThreat property.
     *
     * @return possible object is {@link Attack }
     */
    public Attack getApplicableThreat() {
        return applicableThreat;
    }

    /**
     * Sets the value of the applicableThreat property.
     *
     * @param value allowed object is {@link Attack }
     */
    public void setApplicableThreat(Attack value) {
        this.applicableThreat = value;
    }

    /**
     * Gets the value of the propertyID property.
     *
     * @return possible object is {@link String }
     */
    public String getPropertyID() {
        return propertyID;
    }

    /**
     * Sets the value of the propertyID property.
     *
     * @param value allowed object is {@link String }
     */
    public void setPropertyID(String value) {
        this.propertyID = value;
    }

    /**
     * Gets the value of the status property.
     *
     * @return possible object is {@link Boolean }
     */
    public Boolean isStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     *
     * @param value allowed object is {@link Boolean }
     */
    public void setStatus(Boolean value) {
        this.status = value;
    }
}
