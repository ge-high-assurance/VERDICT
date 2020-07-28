package com.ge.verdict.gsn;

import com.ge.verdict.vdm.VdmTranslator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import verdict.vdm.vdm_model.CyberReq;
import verdict.vdm.vdm_model.Mission;
import verdict.vdm.vdm_model.Model;
import verdict.vdm.vdm_model.SafetyReq;

/** @author Saswata Paul */
public class GSNInterface {
    static final String SEP = File.separator;

    /**
     * The interface for creating GSN artefacts
     *
     * @param rootGoalIdInput -- the GUI input root goal of the GSN fragment
     * @param gsnOutputDir -- the directory where outputs will be stored
     * @param soteriaOutputDir -- the directory containing Soteria outputs
     * @param caseAadlPath -- the directory containing the AADL files
     * @param xmlFla -- determines if xml should be created
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public void runGsnArtifactsGenerator(
            String rootGoalIdInput,
            String gsnOutputDir,
            String soteriaOutputDir,
            String caseAadlPath,
            boolean xmlFlag)
            throws IOException, ParserConfigurationException, SAXException {

        File modelXml = new File(gsnOutputDir, "modelXML.xml");
        File cyberOutput = new File(soteriaOutputDir, "ImplProperties.xml");
        File safetyOutput = new File(soteriaOutputDir, "ImplProperties-safety.xml");

        // Fetch the DeliveryDrone model from the XML
        Model xmlModel = VdmTranslator.unmarshalFromXml(modelXml);

        // List of all mission ids
        List<String> missionIds = new ArrayList<>();
        for (Mission aMission : xmlModel.getMission()) {
            missionIds.add(aMission.getId());
        }

        // List of all cyber req ids
        List<String> cyberIds = new ArrayList<>();
        for (CyberReq aCReq : xmlModel.getCyberReq()) {
            cyberIds.add(aCReq.getId());
        }

        // List of all safety req ids
        List<String> safetyIds = new ArrayList<>();
        for (SafetyReq aSReq : xmlModel.getSafetyReq()) {
            safetyIds.add(aSReq.getId());
        }

        // List of ids to create fragments for
        List<String> forIds = new ArrayList<>();

        if (rootGoalIdInput.equals("ALLMREQKEY")) {
            forIds.addAll(missionIds);
            forIds.addAll(cyberIds);
            forIds.addAll(safetyIds);
        } else {
            forIds.add(rootGoalIdInput);
        }

        // creating fragments
        for (String rootGoalId : forIds) {
            // create the GSN fragment
            GsnNode gsnFragment =
                    CreateGSN.gsnCreator(
                            xmlModel, cyberOutput, safetyOutput, caseAadlPath, rootGoalId);
            System.out.println("Info: Created Gsn fragment for " + rootGoalId);

            // Filenames
            String xmlFilename = rootGoalId + "_GsnFragment.xml";
            String dotFilename = rootGoalId + "_GsnFragment.dot";
            String svgFilename = rootGoalId + "_GsnFragment.svg";

            if (xmlFlag) {
                // Create a file and print the GSN XML
                File gsnXmlFile = new File(gsnOutputDir, xmlFilename);
                Gsn2Xml.convertGsnToXML(gsnFragment, gsnXmlFile);
                System.out.println(
                        "Info: Written Gsn to xml for "
                                + rootGoalId
                                + ": "
                                + gsnXmlFile.getAbsolutePath());
            }

            // Create a file and print the dot
            File gsnDotFile = new File(gsnOutputDir, dotFilename);
            Gsn2Dot.createDot(gsnFragment, gsnDotFile);
            System.out.println(
                    "Info: Written Gsn to dot for "
                            + rootGoalId
                            + ": "
                            + gsnDotFile.getAbsolutePath());

            // generate the svg file using graphviz
            String graphDestination = gsnOutputDir + SEP + svgFilename;
            String dotFileSource = gsnDotFile.getAbsolutePath();

            Dot2GraphViz.generateGraph(dotFileSource, graphDestination);
            System.out.println(
                    "Info: Written Gsn to svg for " + rootGoalId + ": " + graphDestination);
        }
    }
}
