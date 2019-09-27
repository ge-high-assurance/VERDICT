/* See LICENSE in project directory */
package com.ge.verdict.vdm;

import java.io.File;
import java.io.IOException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import verdict.vdm.vdm_model.Model;
import verdict.vdm.vdm_model.ObjectFactory;

/** Translate a Verdict data model to or from an XML file. */
public class VdmTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(VdmTranslator.class);

    /**
     * Marshal a Verdict data model to an XML file.
     *
     * @param model Verdict data model to marshal
     * @param outputFile XML file to write to
     */
    public void marshalToXml(Model model, File outputFile) {
        // Skip and warn if output file can't be created
        if (canWrite(outputFile, LOGGER)) {
            try {
                // Set up model as element to marshal
                ObjectFactory factory = new ObjectFactory();
                JAXBElement<Model> element = factory.createModel(model);
                JAXBContext context = JAXBContext.newInstance(model.getClass());
                Marshaller marshaller = context.createMarshaller();

                // Marshal element to output file
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                marshaller.marshal(element, outputFile);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Unmarshal a Verdict data model from an XML file.
     *
     * @param inputFile XML file to unmarshal from
     * @return Verdict data model from XML file
     */
    public Model unmarshalFromXml(File inputFile) {
        try {
            // Set up input file as source to unmarshal from
            StreamSource source = new StreamSource(inputFile);
            JAXBContext context = JAXBContext.newInstance(Model.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            // Umarshall model from stream source
            JAXBElement<Model> element = unmarshaller.unmarshal(source, Model.class);
            Model model = element.getValue();
            return model;
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Check that a output file is writable; log a warning message if not.
     *
     * @param outputFile File to write to
     * @param logger Logger for any warning messages
     * @return true If file is writable, false otherwise
     */
    protected boolean canWrite(File outputFile, Logger logger) {
        File outputDir = outputFile.getParentFile();
        boolean canWrite = true;

        // Ensure output directory exists
        if (outputDir == null) {
            outputDir = new File(".");
        }
        if (canWrite && !outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                logger.error("Could not create directory '{}'", outputDir.getAbsolutePath());
                canWrite = false;
            }
        }

        // Ensure output file exists
        if (canWrite && !outputFile.exists()) {
            try {
                outputFile.createNewFile();
            } catch (IOException e) {
                logger.error("Error creating file '{}': {}", outputFile.getAbsolutePath(), e);
                canWrite = false;
            }
        }

        // Check output file is writable
        if (canWrite && !outputFile.canWrite()) {
            logger.error("Cannot write file '{}'", outputFile.getAbsolutePath());
            canWrite = false;
        }

        return canWrite;
    }
}
