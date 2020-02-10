package com.ge.research.osate.verdict.handlers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.outline.impl.EObjectNode;
import org.eclipse.xtext.ui.editor.utils.EditorUtils;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;
import org.osate.aadl2.ComponentImplementation;
import org.osate.aadl2.Connection;
import org.osate.aadl2.Element;
import org.osate.aadl2.ModalPropertyValue;
import org.osate.aadl2.NamedValue;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyAssociation;
import org.osate.aadl2.PropertyExpression;
import org.osate.aadl2.PropertyOwner;
import org.osate.aadl2.PropertySet;
import org.osate.aadl2.PublicPackageSection;
import org.osate.aadl2.Subcomponent;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.SystemType;
import org.osate.aadl2.impl.BooleanLiteralImpl;
import org.osate.aadl2.impl.ConnectionImpl;
import org.osate.aadl2.impl.EnumerationLiteralImpl;
import org.osate.aadl2.impl.IntegerLiteralImpl;
import org.osate.aadl2.impl.MetaclassReferenceImpl;
import org.osate.aadl2.impl.ModalPropertyValueImpl;
import org.osate.aadl2.impl.NamedValueImpl;
import org.osate.aadl2.impl.PropertyAssociationImpl;
import org.osate.aadl2.impl.PropertyImpl;
import org.osate.aadl2.impl.PropertySetImpl;
import org.osate.aadl2.impl.RealLiteralImpl;
import org.osate.aadl2.impl.StringLiteralImpl;
import org.osate.aadl2.instance.ConnectionInstance;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instantiation.InstantiateModel;
import org.osate.aadl2.properties.PropertyAcc;
import org.osate.xtext.aadl2.Aadl2StandaloneSetup;

import com.ge.research.osate.verdict.alloy.AadlAlloyTranslator;
import com.ge.research.osate.verdict.alloy.AlloyPrettyPrinter;
import com.ge.research.osate.verdict.alloy.SysArchAlloyModel;
import com.ge.research.osate.verdict.alloy.ThreatLibrary;
import com.ge.research.osate.verdict.alloy.ThreatModelAlloyTranslator;
import com.ge.research.osate.verdict.alloy.ThreatModelParser;
import com.ge.research.osate.verdict.alloy.Util;
import com.google.inject.Injector;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.alloy4viz.VizGUI;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

/*
 * General comment: The eventual plan is to not make this part of
 * the plugin. It is currently part of the plugin because we don't
 * have everything fully integrated with UTRC's translator. You
 * should be able to re-use a lot of this code when migrating to use
 * UTRC's translator, but it will require significant re-working.
 */

public class AADL2AlloyHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (VerdictHandlersUtils.startRun()) {
			IIntroPart introPart = PlatformUI.getWorkbench().getIntroManager().getIntro();
			PlatformUI.getWorkbench().getIntroManager().closeIntro(introPart);
			VerdictHandlersUtils.setPrintOnConsole("MBAA Output");
			
			Thread mbasAnalysisThread = new Thread() {
				@Override
				public void run() {
//					runFromFile("/Users/baoluomeng/Desktop/test/alloy/test1.als");
					final Injector injector = new Aadl2StandaloneSetup().createInjectorAndDoEMFRegistration();
					final XtextResourceSet rs = injector.getInstance(XtextResourceSet.class);					
					File dir = new File(VerdictHandlersUtils.getCurrentSelection(event).get(0));									
					List<String> aadlFileNames = new ArrayList<>();
					
					// Get all AADL files contents in the project
					List<EObject> objects = new ArrayList<>();
					
					for (File file : dir.listFiles()) {
						if (file.getAbsolutePath().endsWith(".aadl")) {
							aadlFileNames.add(file.getAbsolutePath());
						}
					}
					
					final Resource[] resources = new Resource[aadlFileNames.size()];
					for (int i = 0; i < aadlFileNames.size(); i++) {
						resources[i] = rs.getResource(URI.createFileURI(aadlFileNames.get(i)), true);
					}
					
					// Load the resources
					for (final Resource resource : resources) {
						try {
							resource.load(null);
						} catch (final IOException e) {
							System.err.println("ERROR LOADING RESOURCE: " + e.getMessage());
						}
					}
					
					// Load all objects from resources
					for (final Resource resource : resources) {
						resource.getAllContents().forEachRemaining(objects::add);
					}
					
					// Start the translation and invoke the solver
					SysArchAlloyModel sysArchAlloyModel = new SysArchAlloyModel();
					sysArchAlloyModel.loadBuiltinConstructs();
					
					AadlAlloyTranslator aadlAlloyTranslator = new AadlAlloyTranslator(sysArchAlloyModel);
					aadlAlloyTranslator.translateFromAADLObjects(objects);
//					AlloyPrettyPrinter.printToAlloy(sysArchAlloyModel);					
					sysArchAlloyModel.execute();
					
				}
			};
			mbasAnalysisThread.start();	
			VerdictHandlersUtils.finishRun();
				
		}
		return null;		
	}
	
	public void runFromFile(String filename) throws Err{

        // The visualizer (We will initialize it to nonnull when we visualize an
        // Alloy solution)
        VizGUI viz = null;

        // Alloy4 sends diagnostic messages and progress reports to the
        // A4Reporter.
        // By default, the A4Reporter ignores all these events (but you can
        // extend the A4Reporter to display the event for the user)
        A4Reporter rep = new A4Reporter() {

            // For example, here we choose to display each "warning" by printing
            // it to System.out
            @Override
            public void warning(ErrorWarning msg) {
                System.out.print("Relevance Warning:\n" + (msg.toString().trim()) + "\n\n");
                System.out.flush();
            }
        };
        // Parse+typecheck the model
        System.out.println("=========== Parsing+Typechecking " + filename + " =============");
        CompModule world = CompUtil.parseEverything_fromFile(rep, null, filename);

        // Choose some default options for how you want to execute the
        // commands
        A4Options options = new A4Options();

        options.solver = A4Options.SatSolver.SAT4J;

        for (Command command : world.getAllCommands()) {
            // Execute the command
            System.out.println("============ Command " + command + ": ============");
            A4Solution ans = TranslateAlloyToKodkod.execute_command(rep, world.getAllReachableSigs(), command, options);
            // Print the outcome
            System.out.println(ans);
            // If satisfiable...
            if (ans.satisfiable()) {
                // You can query "ans" to find out the values of each set or
                // type.
                // This can be useful for debugging.
                //
                // You can also write the outcome to an XML file
                ans.writeXML("alloy_example_output.xml");
                //
                // You can then visualize the XML file by calling this:
                if (viz == null) {
                    viz = new VizGUI(false, "alloy_example_output.xml", null);
                } else {
                    viz.loadXML("alloy_example_output.xml", true);
                }
            }
        }
}	
	
}
