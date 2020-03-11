/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

   @author: M. Fareed Arif 
*/

package edu.uiowa.clc.verdict.blm;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for Component complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Component">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="ComponentID" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Compromised" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Component")
public class Component {

    @XmlAttribute(name = "ComponentID")
    protected String componentID;

    @XmlAttribute(name = "Compromised")
    protected Boolean compromised;

    /**
     * Gets the value of the componentID property.
     *
     * @return possible object is {@link String }
     */
    public String getComponentID() {
        return componentID;
    }

    /**
     * Sets the value of the componentID property.
     *
     * @param value allowed object is {@link String }
     */
    public void setComponentID(String value) {
        this.componentID = value;
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
