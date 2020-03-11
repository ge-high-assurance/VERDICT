/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

   @author: M. Fareed Arif 
*/

package edu.uiowa.clc.verdict.blm;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for Attack complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Attack">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="AttackId" type="{http://www.example.org/BlameAssignment}AttackType"/>
 *         &lt;element name="AttackDescription" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "Attack",
        propOrder = {"attackId", "attackDescription"})
public class Attack {

    @XmlElement(name = "AttackId", required = true)
    @XmlSchemaType(name = "string")
    protected AttackType attackId;

    @XmlElement(name = "AttackDescription", required = true)
    protected String attackDescription;

    /**
     * Gets the value of the attackId property.
     *
     * @return possible object is {@link AttackType }
     */
    public AttackType getAttackId() {
        return attackId;
    }

    /**
     * Sets the value of the attackId property.
     *
     * @param value allowed object is {@link AttackType }
     */
    public void setAttackId(AttackType value) {
        this.attackId = value;
    }

    /**
     * Gets the value of the attackDescription property.
     *
     * @return possible object is {@link String }
     */
    public String getAttackDescription() {

        if (attackId == AttackType.LS) {
            attackDescription = "Location Spoofing";
        } else if (attackId == AttackType.LB) {
            attackDescription = "Logic Bomb";
        } else if (attackId == AttackType.HT) {
            attackDescription = "Hardware Trojan";
        } else if (attackId == AttackType.SV) {
            attackDescription = "Software Virus/Trojan";
        } else if (attackId == AttackType.RI) {
            attackDescription = "Remote Code Injection";
        } else if (attackId == AttackType.NI) {
            attackDescription = "Network Injection";
        } else if (attackId == AttackType.IT) {
            attackDescription = "Insider Threat";
        } else if (attackId == AttackType.OT) {
            attackDescription = "Outsider Threat";
        }

        return attackDescription;
    }

    /**
     * Sets the value of the attackDescription property.
     *
     * @param value allowed object is {@link String }
     */
    public void setAttackDescription(String value) {
        this.attackDescription = value;
    }

    public String toString() {

        if (attackDescription == null) {
            attackDescription = "";
        }

        return attackDescription;
    }
}
