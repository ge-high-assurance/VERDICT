/* See LICENSE in project directory */
package com.ge.research.verdict.utrc.aadl2iml;

import com.google.inject.Injector;
import com.rockwellcollins.atc.agree.AgreeStandaloneSetup;
import com.rockwellcollins.atc.agree.agree.AgreePackage;
import com.utc.utrc.hermes.aadl.gen.iml.translator.AadlTranslator;
import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Map;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.osate.aadl2.Aadl2Package;
import org.osate.aadl2.instance.InstancePackage;
import org.osate.aadl2.instance.textual.InstanceStandaloneSetup;
import org.osate.xtext.aadl2.Aadl2StandaloneSetup;

/** Start modifying IntegrationTest to be just a wrapper for calling AadlTranslator. */
public class IntegrationTest {

    /** Start modifying translate to be a wrapper for calling AadlTranslator. */
    public void translate() {
        // Setup
        EcorePlugin.ExtensionProcessor.process(null);
        Injector injector = new Aadl2StandaloneSetup().createInjectorAndDoEMFRegistration();
        InstanceStandaloneSetup.doSetup();
        AgreeStandaloneSetup.doSetup();
        Aadl2Package.eINSTANCE.eClass();
        InstancePackage.eINSTANCE.eClass();
        AgreePackage.eINSTANCE.eClass();

        // IOWA - Delivery Drone
        String path = "models/IOWAmodels/DeliveryDrone/aadl/";
        String mainModule = "DeliveryDrone.aadl";
        String output_path = path.replace("/aadl/", "/iml-gen/");

        // Get ResourceSet
        ResourceSet aadlResourceSet = injector.getInstance(XtextResourceSet.class);
        // -Standard libraries
        aadlResourceSet.getResource(URI.createURI("models/Base_Types.aadl"), true);
        aadlResourceSet.getResource(URI.createURI("models/Data_Model.aadl"), true);

        // Load model files and validate the model to check for errors or cross-reference issues
        Resource aadlResource = loadAADLmodels(aadlResourceSet, path, mainModule);
        IResourceValidator validator =
                ((XtextResource) aadlResource).getResourceServiceProvider().getResourceValidator();
        List<Issue> issues =
                validator.validate(aadlResource, CheckMode.ALL, CancelIndicator.NullImpl);
        for (Issue issue : issues) {
            System.out.println(issue.getMessage());
        }

        // Translation
        AadlTranslator translator = new AadlTranslator();
        // Option 1: Return the text
        Map<String, String> imlFiles = translator.getTranslatedIMLTextualModels(aadlResource);
        // Option 2: save the files in the given path
        translator.getTranslatedIMLFiles(aadlResource, output_path);
        System.out.println("Translation completed; " + imlFiles.size() + " files written");
    }

    /**
     * Add all AADL models from the given folder. This function is only required when the translator
     * is not called from the plugin.
     *
     * @param aadlResourceSet
     * @param path
     * @param mainFile
     * @return Resource
     */
    Resource loadAADLmodels(ResourceSet aadlResourceSet, String path, String mainFile) {
        Resource aadlResource = null;
        File dir = new File(path);
        File[] files =
                dir.listFiles(
                        new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                return name.endsWith(".aadl");
                            }
                        });

        for (File xmlfile : files) {
            String filename = xmlfile.toString();
            System.out.println(filename);

            if (!filename.contains(mainFile)) {
                aadlResourceSet.getResource(URI.createURI(xmlfile.toString()), true);
            } else {
                aadlResource = aadlResourceSet.getResource(URI.createURI(xmlfile.toString()), true);
            }
        }

        return aadlResource;
    }
}
