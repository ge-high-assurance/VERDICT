package com.ge.verdict.gsn;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.File;

/**
 * @author Saswata Paul
 */
public class Gsn2Xml {

    /**
     * Creates an XML file from a GsnNode
     *
     * @param fragment
     * @param outputFile
     */
    public void convertGsnToXML(GsnNode fragment, File outputFile) {
        try {
            // Create JAXB Context
            JAXBContext jaxbContext = JAXBContext.newInstance(GsnNode.class);

            // Create Marshaller
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // Required formatting??
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            // Writes XML file to file-system
            jaxbMarshaller.marshal(fragment, outputFile);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
