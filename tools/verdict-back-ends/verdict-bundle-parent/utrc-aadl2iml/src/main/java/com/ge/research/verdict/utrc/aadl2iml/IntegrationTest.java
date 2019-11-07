/* See LICENSE in project directory */
package com.ge.research.verdict.utrc.aadl2iml;

import com.utc.utrc.hermes.aadl.gen.iml.translator.AadlTranslator;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.spi.RegistryStrategy;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.osate.xtext.aadl2.Aadl2StandaloneSetup;

/** Start modifying IntegrationTest to be just a wrapper for calling AadlTranslator. */
public class IntegrationTest {

    /** Start modifying translate to be a wrapper for calling AadlTranslator. */
    public void translate() {
        try {
            RegistryFactory.setDefaultRegistryProvider(
                    () -> {
                        Object masterToken = new Object();
                        IExtensionRegistry registry =
                                RegistryFactory.createRegistry(
                                        new RegistryStrategy(null, null), masterToken, null);
                        return registry;
                    });
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }

        // Setup
        Aadl2StandaloneSetup.doSetup();

        // IOWA - Delivery Drone
        String path = "models/IOWAmodels/DeliveryDrone/aadl/";
        String mainModule = "DeliveryDrone.aadl";
        String output_path = path.replace("/aadl/", "/iml-gen/");

        // Get Resources
        ResourceSet aadlResourceSet = new ResourceSetImpl();
        // -Standard libraries
        aadlResourceSet.getResource(URI.createURI("models/Base_Types.aadl"), true);
        aadlResourceSet.getResource(URI.createURI("models/Data_Model.aadl"), true);
        Resource aadlResource = loadAADLmodels(aadlResourceSet, path, mainModule);

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
