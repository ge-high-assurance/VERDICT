package com.ge.verdict.gsn;

import com.ge.verdict.vdm.VdmTranslator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import verdict.vdm.vdm_model.CyberReq;
import verdict.vdm.vdm_model.Model;

/**
 * This class is an interface for generating security assurance cases It is still under development
 *
 * @author Saswata Paul
 */
public class SecurityGSNInterface {
    protected final String SEP = File.separator;
    protected static String gsnOutputDirectory;
    protected static boolean CreateXmlFlag;

    /**
     * The interface for creating security GSN artifacts
     *
     * @param userInput
     * @param gsnOutputDir
     * @param soteriaOutputDir
     * @param caseAadlPath
     * @param securityCaseFlag
     * @param xmlFlag
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public void runGsnArtifactsGenerator(
            String userInput,
            String gsnOutputDir,
            String soteriaOutputDir,
            String caseAadlPath,
            boolean securityCaseFlag,
            boolean xmlFlag)
            throws IOException, ParserConfigurationException, SAXException {

        File modelXml = new File(gsnOutputDir, "modelXML.xml");
        File cyberOutput = new File(soteriaOutputDir, "ImplProperties.xml");
        File safetyOutput = new File(soteriaOutputDir, "ImplProperties-safety.xml");
        gsnOutputDirectory = gsnOutputDir;
        CreateXmlFlag = xmlFlag;

        // Fetch the DeliveryDrone model from the XML
        Model xmlModel = VdmTranslator.unmarshalFromXml(modelXml);

        // List of all mission ids
        List<String> cyberIds = new ArrayList<>();
        for (CyberReq aCyberReq : xmlModel.getCyberReq()) {
            cyberIds.add(aCyberReq.getId());
        }

        // List of ids to create fragments for
        List<String> forIds = new ArrayList<>();

        if (userInput.equals("ALLCREQKEY")) {
            forIds.addAll(cyberIds);
        } else {

            // get individual IDs from the user input
            String[] inputs = userInput.split(";");

            // adding each Id to the list
            for (String id : inputs) {
                forIds.add(id);
            }
        }

        // creating fragments
        for (String rootGoalId : forIds) {
            // create the GSN fragment
            CreateSecurityGSN objCreateGSN = new CreateSecurityGSN();
            GsnNode gsnFragment =
                    objCreateGSN.gsnCreator(
                            xmlModel,
                            cyberOutput,
                            safetyOutput,
                            caseAadlPath,
                            rootGoalId,
                            securityCaseFlag);
            System.out.println("Info: Created Security GSN fragments for " + rootGoalId);

            // creating artifacts
            createArtifactFiles(gsnFragment, rootGoalId);
        }
    }

    /**
     * Takes a GSN fragment and rootgoalID and creates the following artifact files: dot svg xml
     * (optional)
     *
     * @param gsnFragment
     * @param rootGoalId
     * @throws IOException
     */
    public String createArtifactFiles(GsnNode gsnFragment, String rootGoalId) throws IOException {
        // Filenames
        String xmlFilename = rootGoalId + "_GsnFragment.xml";
        String dotFilename = rootGoalId + "_GsnFragment.dot";
        String svgFilename = rootGoalId + "_GsnFragment.svg";

        if (CreateXmlFlag) {
            // Create a file and print the GSN XML
            File gsnXmlFile = new File(gsnOutputDirectory, xmlFilename);
            Gsn2Xml objGsn2Xml = new Gsn2Xml();
            objGsn2Xml.convertGsnToXML(gsnFragment, gsnXmlFile);
            System.out.println(
                    "Info: Written GSN to xml for "
                            + rootGoalId
                            + ": "
                            + gsnXmlFile.getAbsolutePath());
        }

        // Create a file and print the dot
        File gsnDotFile = new File(gsnOutputDirectory, dotFilename);
        Gsn2Dot objGsn2Dot = new Gsn2Dot();
        objGsn2Dot.createDot(gsnFragment, gsnDotFile);
        System.out.println(
                "Info: Written GSN to dot for " + rootGoalId + ": " + gsnDotFile.getAbsolutePath());

        // generate the svg file using graphviz
        String graphDestination = gsnOutputDirectory + SEP + svgFilename;
        String dotFileSource = gsnDotFile.getAbsolutePath();

        Dot2GraphViz objDot2GraphViz = new Dot2GraphViz();
        objDot2GraphViz.generateGraph(dotFileSource, graphDestination);
        System.out.println("Info: Written GSN to svg for " + rootGoalId + ": " + graphDestination);

        return graphDestination;
    }
}
