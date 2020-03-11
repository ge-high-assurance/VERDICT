/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

   @author: M. Fareed Arif 
*/

package edu.uiowa.clc.verdict.blm;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for AttackType.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <p>
 *
 * <pre>
 * &lt;simpleType name="AttackType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="NI"/>
 *     &lt;enumeration value="LG"/>
 *     &lt;enumeration value="LS"/>
 *     &lt;enumeration value="IT"/>
 *     &lt;enumeration value="OT"/>
 *     &lt;enumeration value="RI"/>
 *     &lt;enumeration value="SV"/>
 *     &lt;enumeration value="HT"/>
 *     &lt;enumeration value="LB"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
@XmlType(name = "AttackType")
@XmlEnum
public enum AttackType {
    NI,
    LG,
    LS,
    IT,
    OT,
    RI,
    SV,
    HT,
    LB;

    public String value() {
        return name();
    }

    public static AttackType fromValue(String v) {
        return valueOf(v);
    }
}
