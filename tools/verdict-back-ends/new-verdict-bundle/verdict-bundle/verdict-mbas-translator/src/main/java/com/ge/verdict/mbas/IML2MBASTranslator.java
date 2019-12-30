package com.ge.verdict.mbas;
import com.ge.verdict.utils.Logger;
import com.google.inject.Injector;
import com.utc.utrc.hermes.iml.ImlParseHelper;
import com.utc.utrc.hermes.iml.ImlStandaloneSetup;
import com.utc.utrc.hermes.iml.iml.Model;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

public class IML2MBASTranslator {
//    private final String AGREE = "agree";
    private final String VERDICT = "verdict";

    // 12 IML libs
    public List<String> imlLibFiles =
            Arrays.asList(
                    new String[] {
                        "contracts",
                        "graphs",
                        "lang-utils",
                        "lang",
                        "ports",
                        "queries",
                        "sms",
                        "software",
                        "synchdflight",
                        "synchrony",
                        "systems",
                        "verdict"
                    });

    /** Main entry point */
    public void translateVerdictToMBAS(String imlDirPath, String mainIMLFileName) {
        Injector injector = ImlStandaloneSetup.getInjector();
        ImlParseHelper imlParseHelper = injector.getInstance(ImlParseHelper.class);

        ResourceSet rs =
                imlParseHelper.parseDir(new File(imlDirPath, VERDICT).getAbsolutePath(), false);

        // Load IML common libraries
        loadIMLLibs(rs);
        // Find the main IML model to translate
        Model mainImlModel = null;
        for (Resource r : rs.getResources()) {
            System.out.println(
                    "Loaded resource: " + ((Model) r.getContents().get(0)).getName());
            if (((Model) r.getContents().get(0))
                    .getName()
                    .equals(VERDICT + "." + mainIMLFileName)) {
                mainImlModel = (Model) r.getContents().get(0);
                break;
            }
        }
        
        // Translate the main model in verdict folder
        if (mainImlModel != null) {
            IMLParser imlParser = new IMLParser();
            imlParser.collectModelInfo(mainImlModel);
        } else {
        	Logger.error("Cannot find the main IML model in the loaded resource: " + mainIMLFileName);
        }
        // Validating loaded IML models; if there are any error, program exits.
//        List<Issue> errors = imlParseHelper.checkErrors(rs);
//        if (!errors.isEmpty()) {
//            Logger.error("errors reported in IML model!");
//            for (Issue issue : errors) {
//                System.err.println(issue);
//            }
//        } else {
//            // Find the main IML model to translate
//            Model mainImlModel = null;
//            for (Resource r : rs.getResources()) {
//                System.out.println(
//                        "Loaded resource: " + ((Model) r.getContents().get(0)).getName());
//                if (((Model) r.getContents().get(0))
//                        .getName()
//                        .equals(VERDICT + "." + mainIMLFileName)) {
//                    mainImlModel = (Model) r.getContents().get(0);
//                    break;
//                }
//            }
//            
//            // Translate the main model in verdict folder
//            if (mainImlModel != null) {
//                IMLParser imlParser = new IMLParser();
//                imlParser.collectModelInfo(mainImlModel);
//            } else {
//            	Logger.error("Cannot find the main IML model in the loaded resource: " + mainIMLFileName);
//            }
//        }
    }
    /** Load IML libraries */
    public void loadIMLLibs(ResourceSet rs) {
        // Put all loaded IML in a set
        Set<String> loadedIMLNames = new HashSet<>();
        for (Resource r : rs.getResources()) {
            loadedIMLNames.add(((Model) r.getContents().get(0)).getName());
        }

        // Load IML standard libraries
        for (String imlLibFileName : imlLibFiles) {
            // check if a IML lib is loaded or not, if not, then load
            if (!loadedIMLNames.contains("iml." + imlLibFileName)) {
                rs.getResource(
                        URI.createURI("classpath:/iml-libs/" + imlLibFileName + ".iml"), true);
            }
        }
    }

}
