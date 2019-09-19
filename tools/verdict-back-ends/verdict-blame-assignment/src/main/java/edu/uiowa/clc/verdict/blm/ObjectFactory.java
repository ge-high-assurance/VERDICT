/* See LICENSE in project directory */
package edu.uiowa.clc.verdict.blm;

import javax.xml.bind.annotation.XmlRegistry;

/**
 * This object contains factory methods for each Java content interface and Java element interface
 * generated in the edu.uiowa.clc.verdict.blm package.
 *
 * <p>An ObjectFactory allows you to programatically construct new instances of the Java
 * representation for XML content. The Java representation of XML content can consist of schema
 * derived interfaces and classes representing the binding of schema type definitions, element
 * declarations and model groups. Factory methods for each of these are provided in this class.
 */
@XmlRegistry
public class ObjectFactory {

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes
     * for package: edu.uiowa.clc.verdict.blm
     */
    public ObjectFactory() {}

    /** Create an instance of {@link ViolatedProperty } */
    public ViolatedProperty createViolatedProperty() {
        return new ViolatedProperty();
    }

    /** Create an instance of {@link MinA } */
    public MinA createMinA() {
        return new MinA();
    }

    /** Create an instance of {@link Attack } */
    public Attack createAttack() {
        return new Attack();
    }

    /** Create an instance of {@link Component } */
    public Component createComponent() {
        return new Component();
    }

    /** Create an instance of {@link Link } */
    public Link createLink() {
        return new Link();
    }

    /** Create an instance of {@link BlameType } */
    public BlameType createBlameType() {
        return new BlameType();
    }

    /** Create an instance of {@link BlameAssignment } */
    public BlameAssignment createBlameAssignment() {
        return new BlameAssignment();
    }
}
