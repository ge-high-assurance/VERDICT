/* See LICENSE in project directory */
package com.ge.verdict.stem;

import com.ge.research.sadl.ide.handlers.SadlRunInferenceHandler;
import com.ge.research.sadl.preferences.SadlPreferences;
import com.ge.research.sadl.sADL.SADLPackage;
import com.google.inject.Injector;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.generator.GeneratorContext;
import org.eclipse.xtext.generator.GeneratorDelegate;
import org.eclipse.xtext.generator.JavaIoFileSystemAccess;
import org.eclipse.xtext.preferences.MapBasedPreferenceValues;
import org.eclipse.xtext.preferences.PreferenceValuesByLanguage;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Runs Verdict STEM on a project. */
public class VerdictStem {
    private static final Logger LOGGER = LoggerFactory.getLogger(VerdictStem.class);

    /**
     * Runs SADL on a Verdict STEM project.
     *
     * @param projectDir Path to Verdict STEM project
     * @param sadlFile File to run with SADL inference handler
     */
    public void runStem(File projectDir, File sadlFile) {
        // Initialize the SADL parser code
        Injector injector = new STEMStandaloneSetup().createInjectorAndDoEMFRegistration();
        SADLPackage.eINSTANCE.eClass();

        // Load any SADL configuration file's preferences
        Map<String, String> preferences =
                loadPreferences(new File(projectDir, "SadlConfiguration.xml"));
        PreferenceValuesByLanguage preferencesByLanguage = new PreferenceValuesByLanguage();
        preferencesByLanguage.put(
                "com.ge.research.sadl.SADL", new MapBasedPreferenceValues(preferences));
        XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
        preferencesByLanguage.attachToEmfObject(resourceSet);

        // Find all SADL files in the project
        List<File> sadlFiles = findFilesWithExtension(projectDir, ".sadl");
        List<File> sreqFiles = findFilesWithExtension(projectDir, ".sreq");
        List<File> sverFiles = findFilesWithExtension(projectDir, ".sver");

        // Parse all the SADL files and resolve interdependencies between resources
        resourceSet.addLoadOption(XtextResource.OPTION_RESOLVE_ALL, Boolean.TRUE);
        for (File file : sadlFiles) {
            resourceSet.getResource(URI.createFileURI(file.getAbsolutePath()), true);
        }
        for (File file : sreqFiles) {
            resourceSet.getResource(URI.createFileURI(file.getAbsolutePath()), true);
        }
        for (File file : sverFiles) {
            resourceSet.getResource(URI.createFileURI(file.getAbsolutePath()), true);
        }
        EcoreUtil2.resolveAll(resourceSet);

        // Generate model files for all the SADL files
        File modelDir = new File(projectDir, "OwlModels");
        List<File> allFiles = new ArrayList<>();
        allFiles.addAll(sadlFiles);
        allFiles.addAll(sreqFiles);
        allFiles.addAll(sverFiles);
        for (File file : allFiles) {
            Resource resource =
                    resourceSet.getResource(URI.createFileURI(file.getAbsolutePath()), true);
            if (resource instanceof XtextResource) {
                generateModel(modelDir, resource);
            }
        }

        // Get the file to be run and hand it off to SADL's run inference handler
        XtextResource resource =
                (XtextResource)
                        resourceSet.getResource(
                                URI.createFileURI(sadlFile.getAbsolutePath()), true);
        SadlRunInferenceHandler runInferenceHandler =
                injector.getInstance(SadlRunInferenceHandler.class);
        runInferenceHandler.run(sadlFile.toPath(), () -> resource, preferences);
        LOGGER.info("Run finished");
    }

    private Map<String, String> loadPreferences(File configFile) {
        Map<String, String> sadlPreferences = new LinkedHashMap<>();
        // Parse the configuration file
        DocumentBuilderFactory xmlDocFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder xmlDocBuilder;
        try {
            xmlDocBuilder = xmlDocFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            LOGGER.error("Error loading preferences", e);
            return sadlPreferences;
        }
        Document xmlDoc;
        try {
            xmlDoc = xmlDocBuilder.parse(configFile);
        } catch (SAXException | IOException e) {
            LOGGER.error("Error loading preferences", e);
            return sadlPreferences;
        }
        NodeList prefNodes = xmlDoc.getElementsByTagName("preferences");
        for (int i = 0; i < prefNodes.getLength(); i++) {
            Element prefElement = (Element) prefNodes.item(i);
            String prefAttribute = prefElement.getAttribute("id");
            // Load SADL preferences
            if (prefAttribute.equals("SADL")) {
                NodeList sadlNodes = prefElement.getElementsByTagName("setting");
                for (int x = 0; x < sadlNodes.getLength(); x++) {
                    Element sadlElement = (Element) sadlNodes.item(x);
                    String sadlLabel = sadlElement.getAttribute("label");
                    String sadlValue = sadlElement.getAttribute("value");
                    // If no value is set, skip and use default
                    if (sadlValue.isEmpty()) {
                        continue;
                    }
                    if (sadlLabel.equals("BaseURI")) {
                        sadlPreferences.put(SadlPreferences.SADL_BASE_URI.getId(), sadlValue);
                    } else if (sadlLabel.equals("SavedOWLModelFormat")) {
                        if (sadlValue.replace(" ", "").toLowerCase().equals("rdf/xml-abbrev")) {
                            sadlPreferences.put(
                                    SadlPreferences.OWL_MODEL_FORMAT.getId(),
                                    SadlPreferences.RDF_XML_ABBREV_FORMAT.getId());
                        } else if (sadlValue.replace(" ", "").toLowerCase().equals("rdf/xml")) {
                            sadlPreferences.put(
                                    SadlPreferences.OWL_MODEL_FORMAT.getId(),
                                    SadlPreferences.RDF_XML_FORMAT.getId());
                        } else if (sadlValue.replace(" ", "").toLowerCase().equals("n3")) {
                            sadlPreferences.put(
                                    SadlPreferences.OWL_MODEL_FORMAT.getId(),
                                    SadlPreferences.N3_FORMAT.getId());
                        } else if (sadlValue.replace(" ", "").toLowerCase().equals("n-triple")) {
                            sadlPreferences.put(
                                    SadlPreferences.OWL_MODEL_FORMAT.getId(),
                                    SadlPreferences.N_TRIPLE_FORMAT.getId());
                        } else if (sadlValue.replace(" ", "").toLowerCase().equals("jenatbd")) {
                            sadlPreferences.put(
                                    SadlPreferences.OWL_MODEL_FORMAT.getId(),
                                    SadlPreferences.JENA_TDB.getId());
                        }
                    } else if (sadlLabel.equals("ShowImportModelListAs")) {
                        if (sadlValue.replace(" ", "").toLowerCase().equals("modelnamespaces")) {
                            // TODO SadlPreference Key needed
                            sadlPreferences.put(
                                    "importBy", SadlPreferences.MODEL_NAMESPACES.getId());
                        } else if (sadlValue
                                .replace(" ", "")
                                .toLowerCase()
                                .equals("sadlfilenames")) {
                            // TODO SadlPreference Key needed
                            sadlPreferences.put(
                                    "importBy", SadlPreferences.SADL_FILE_NAMES.getId());
                        }
                    } else if (sadlLabel.equals(
                            "ShowPrefixesForImportedConceptsOnlyWhenNeededForDisambiguation")) {
                        sadlPreferences.put(
                                SadlPreferences.PREFIXES_ONLY_AS_NEEDED.getId(), sadlValue);
                    } else if (sadlLabel.equals("ValidateBeforeTesting")) {
                        sadlPreferences.put(
                                SadlPreferences.VALIDATE_BEFORE_TEST.getId(), sadlValue);
                    } else if (sadlLabel.equals("Test/QueryWithKnowledgeServer")) {
                        sadlPreferences.put(SadlPreferences.TEST_WITH_KSERVER.getId(), sadlValue);
                    } else if (sadlLabel.equals("ShowNamespacesInQueryResults")) {
                        sadlPreferences.put(
                                SadlPreferences.NAMESPACE_IN_QUERY_RESULTS.getId(), sadlValue);
                    } else if (sadlLabel.equals("ShowTimingInformation")) {
                        sadlPreferences.put(
                                SadlPreferences.SHOW_TIMING_INFORMATION.getId(), sadlValue);
                    } else if (sadlLabel.equals("InterpretDateAs")) {
                        if (sadlValue.replace(" ", "").toLowerCase().equals("mm/dd/yyyy")) {
                            // TODO SadlPreference Key needed
                            sadlPreferences.put("dmyOrder", SadlPreferences.DMY_ORDER_MDY.getId());
                        } else if (sadlValue.replace(" ", "").toLowerCase().equals("dd/mm/yyyy")) {
                            // TODO SadlPreference Key needed
                            sadlPreferences.put("dmyOrder", SadlPreferences.DMY_ORDER_DMY.getId());
                        }
                    } else if (sadlLabel.equals("DisableDeepValidationOfModel")) {
                        sadlPreferences.put(SadlPreferences.DEEP_VALIDATION_OFF.getId(), sadlValue);
                    } else if (sadlLabel.equals("GraphRendererPackageAndClass")) {
                        sadlPreferences.put(
                                SadlPreferences.GRAPH_RENDERER_CLASS.getId(), sadlValue);
                    } else if (sadlLabel.equals("TabularDataImporterClass")) {
                        sadlPreferences.put(
                                SadlPreferences.TABULAR_DATA_IMPORTER_CLASS.getId(), sadlValue);
                    } else if (sadlLabel.equals("IncludeImplicitElementsInGraph")) {
                        sadlPreferences.put(
                                SadlPreferences.GRAPH_IMPLICIT_ELEMENTS.getId(), sadlValue);
                    } else if (sadlLabel.equals("IncludeImplicitElementInstancesInGraph")) {
                        sadlPreferences.put(
                                SadlPreferences.GRAPH_IMPLICIT_ELEMENT_INSTANCES.getId(),
                                sadlValue);
                    } else if (sadlLabel.equals("CheckForAmbiguousNames")) {
                        sadlPreferences.put(
                                SadlPreferences.CHECK_FOR_AMBIGUOUS_NAMES.getId(), sadlValue);
                    } else if (sadlLabel.equals("CheckForCardinalityOfPropertyOnSpecificDomain")) {
                        sadlPreferences.put(
                                SadlPreferences.CHECK_FOR_CARDINALITY_OF_PROPERTY_IN_DOMAIN.getId(),
                                sadlValue);
                    } else if (sadlLabel.equals(
                            "UseIndefiniteAndDefiniteArticlesInValidationAndTranslation")) {
                        sadlPreferences.put(
                                SadlPreferences.P_USE_ARTICLES_IN_VALIDATION.getId(), sadlValue);
                    } else if (sadlLabel.equals("TypeCheckingWarningOnly")) {
                        sadlPreferences.put(
                                SadlPreferences.TYPE_CHECKING_WARNING_ONLY.getId(), sadlValue);
                    } else if (sadlLabel.equals("TypeCheckingRangeRequired")) {
                        sadlPreferences.put(
                                SadlPreferences.TYPE_CHECKING_RANGE_REQUIRED.getId(), sadlValue);
                    } else if (sadlLabel.equals("IgnoreUnittedQuantitiesDuringTranslation")) {
                        sadlPreferences.put(
                                SadlPreferences.IGNORE_UNITTEDQUANTITIES.getId(), sadlValue);
                    } else if (sadlLabel.equals(
                            "TranslateMultipleClassDomainOrRangeAsUnionClass")) {
                        sadlPreferences.put(
                                SadlPreferences.CREATE_DOMAIN_AND_RANGE_AS_UNION_CLASSES.getId(),
                                sadlValue);
                    } else if (sadlLabel.equals("GenerateMetricsReportDuringProjectCleanBuild")) {
                        sadlPreferences.put(
                                SadlPreferences.GENERATE_METRICS_REPORT_ON_CLEAN_BUILD.getId(),
                                sadlValue);
                    } else if (sadlLabel.equals("FileContainingMetricQueries")) {
                        sadlPreferences.put(
                                SadlPreferences.METRICS_QUERY_FILENAME.getId(), sadlValue);
                    } else if (sadlLabel.equals("GraphImplicitElements")) {
                        sadlPreferences.put(
                                SadlPreferences.GRAPH_IMPLICIT_ELEMENTS.getId(), sadlValue);
                    } else if (sadlLabel.equals("GraphImplicitElementInstances")) {
                        sadlPreferences.put(
                                SadlPreferences.GRAPH_IMPLICIT_ELEMENT_INSTANCES.getId(),
                                sadlValue);
                    }
                }
            }
        }
        return sadlPreferences;
    }

    private List<File> findFilesWithExtension(File projectDir, String fileExtension) {
        List<File> files = new ArrayList<>();
        for (File file : projectDir.listFiles()) {
            if (file.isDirectory()) {
                files.addAll(findFilesWithExtension(file, fileExtension));
            } else if (file.getName().toLowerCase().endsWith(fileExtension)) {
                files.add(file);
            }
        }
        return files;
    }

    private void generateModel(File modelDir, Resource resource) {
        // Validate the resource
        IResourceValidator validator =
                ((XtextResource) resource).getResourceServiceProvider().getResourceValidator();
        validator.validate(resource, CheckMode.NORMAL_AND_FAST, CancelIndicator.NullImpl);

        // Generate the model file
        GeneratorDelegate generator =
                ((XtextResource) resource)
                        .getResourceServiceProvider()
                        .get(GeneratorDelegate.class);
        JavaIoFileSystemAccess access =
                ((XtextResource) resource)
                        .getResourceServiceProvider()
                        .get(JavaIoFileSystemAccess.class);
        access.setOutputPath(modelDir.getAbsolutePath());
        GeneratorContext context = new GeneratorContext();
        context.setCancelIndicator(CancelIndicator.NullImpl);
        generator.generate(resource, access, context);
    }
}
