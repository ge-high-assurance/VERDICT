package com.ge.research.osate.verdict.handlers;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.ui.editor.model.XtextDocument;
import org.eclipse.xtext.ui.editor.model.XtextDocumentProvider;
import org.osate.xtext.aadl2.Aadl2StandaloneSetup;
import org.osate.xtext.aadl2.ui.internal.Aadl2Activator;

import com.google.inject.Injector;

/**
*
* @author Paul Meng
* Date: Jun 12, 2019
*
*/
public class VerdictHandlersUtils {
	// SVG graphs
	private static final String SVG = "svg";

	// Ensure only one handler runs commands at a time
	private static boolean isRunningNow;

	public static void printGreeting() {
		System.out.println(
				"*******************************************************************************************************");
		System.out.println(
				"****  VERDICT: Verification Evidence and Resilient Design in Anticipation of Cybersecurity Threats ****");
		System.out.println(
				"*******************************************************************************************************");
		System.out.println();
	}

	/**
	 * Starts a new run if nothing else is running right now.
	 * @return False if something else is running now, otherwise true
	 */
	public static boolean startRun() {
		if (!isRunningNow) {
			isRunningNow = true;
			return true;
		}
		return false;
	}

	/**
	 * Tells us that a run has finished.
	 */
	public static void finishRun() {
		isRunningNow = false;
	}

	public static void setPrintOnConsole(String output) {
		String sysOut = (output == null || output == "") ? "VERDICT Output" : output;

		// Print messages on the plug-in console
		MessageConsole console = new MessageConsole(sysOut, null);
		ConsolePlugin.getDefault().getConsoleManager().addConsoles(new MessageConsole[] { console });
		ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
		MessageConsoleStream stream = console.newMessageStream();
		System.setOut(new PrintStream(stream));
		System.setErr(new PrintStream(stream));
	}

	public static void openSvgGraphsInDir(String dirPath) {
		if (isValidDir(dirPath)) {
			File graphFolder = new File(dirPath);

			for (File f : graphFolder.listFiles()) {
				if (f.getName().endsWith(SVG)) {
					try {
						open(f.toURI().toURL(), Display.getDefault());
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private static void open(final URL url, Display display) {
		display.syncExec(() -> internalOpen(url, false));
	}

	private static void internalOpen(final URL url, final boolean useExternalBrowser) {
		BusyIndicator.showWhile(null, () -> {
			URL helpSystemUrl = PlatformUI.getWorkbench().getHelpSystem().resolve(url.toExternalForm(), true);
			try {
				IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
				IWebBrowser browser;
				if (useExternalBrowser) {
					browser = browserSupport.getExternalBrowser();
				} else {
					browser = browserSupport.createBrowser(null);
				}
				browser.openURL(helpSystemUrl);
			} catch (PartInitException ex) {
			}
		});
	}

	/**
	 * Check if a directory is valid
	 * */
	private static boolean isValidDir(String path) {
		if (path == null) {
			return false;
		}

		File pathDir = new File(path);

		if (!pathDir.exists()) {
			VerdictLogger.warning("Folder: " + path + " does not exist!");
			return false;
		}
		if (!pathDir.isDirectory()) {
			VerdictLogger.warning("Path: " + path + " is not a directory!");
			return false;
		}
		return true;
	}

	/** the current selection in the AADL model
	 *  Return a list of paths to the selected objects
	 */
	public static List<String> getCurrentSelection(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		List<String> paths = new ArrayList<>();

		if (selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() > 0) {
			Object[] selObjs = ((IStructuredSelection) selection).toArray();

			for (Object selObj : selObjs) {
				if (selObj instanceof IFile) {
					IFile selIFile = (IFile) selObj;
					paths.add(selIFile.getProject().getLocation().toFile().getAbsolutePath());
				} else if (selObj instanceof IProject) {
					IProject selIProject = (IProject) selObj;
					paths.add(selIProject.getLocation().toFile().getAbsolutePath());
				} else if (selObj instanceof IFolder) {
					IFolder selIFolder = (IFolder) selObj;
					paths.add(selIFolder.getProject().getLocation().toFile().getAbsolutePath());
				} else {
					VerdictLogger.warning("Selection is not recognized!");
				}
			}
		} else {
			VerdictLogger.warning("Selection is not recognized!");
		}

		return paths;
	}

	public static IProject getCurrentIProject(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);

		if (selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() > 0) {
			Object[] selObjs = ((IStructuredSelection) selection).toArray();

			for (Object selObj : selObjs) {
				if (selObj instanceof IFile) {
					IFile selIFile = (IFile) selObj;
					return selIFile.getProject();
				} else if (selObj instanceof IProject) {
					IProject selIProject = (IProject) selObj;
					return selIProject;
				} else if (selObj instanceof IFolder) {
					IFolder selIFolder = (IFolder) selObj;
					return selIFolder.getProject();
				}
			}
		}

		VerdictLogger.warning("Selection is not recognized!");
		return null;
	}

	/**
	 * Modify all AADL files in a project.
	 * Modifier function takes in the file name and  resource to modify.
	 *
	 * This is currently used in the synthesis AADL modifier.
	 *
	 * @param project
	 * @param modifier
	 */
	public static void modifyAadlDocuments(IProject project, BiConsumer<String, XtextResource> modifier) {
		Injector injector = Aadl2Activator.getInstance().getInjector(Aadl2Activator.ORG_OSATE_XTEXT_AADL2_AADL2);
		XtextDocumentProvider provider = injector.getInstance(XtextDocumentProvider.class);

		List<File> dirs = collectAllDirs(project.getLocation().toFile());
		List<String> aadlFileNames = new ArrayList<>();

		// Obtain all AADL files contents in the project

		String projectPath = project.getLocation().toFile().getAbsolutePath();

		for (File subdir : dirs) {
			for (File file : subdir.listFiles()) {
				if (file.getAbsolutePath().endsWith(".aadl")) {
					String absPath = file.getAbsolutePath();
					String relPath = absPath.substring(projectPath.length() + 1);
					aadlFileNames.add(relPath);
				}
			}
		}

		for (String fileName : aadlFileNames) {
			try {
				FileEditorInput file = new FileEditorInput(project.getFile(new Path(fileName)));
				FileEditorInput backupFile = new FileEditorInput(
						project.getFile(new Path("synthesis_backup/" + fileName + ".bak")));

				provider.connect(file);
				IDocument document = provider.getDocument(file);
				if (document instanceof XtextDocument) {
					XtextDocument xtextDocument = (XtextDocument) document;

					// save a backup
					provider.saveDocument(null, backupFile, xtextDocument, true);

					xtextDocument.modify(resource -> {
						modifier.accept(fileName, resource);
						return null;
					});
				}
				provider.saveDocument(null, file, document, true);
				// don't leak the document
				provider.disconnect(file);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	public static List<Resource> loadAadlFiles(File dir) {
		final Injector injector = new Aadl2StandaloneSetup().createInjectorAndDoEMFRegistration();
		final XtextResourceSet rs = injector.getInstance(XtextResourceSet.class);
		List<String> aadlFileNames = new ArrayList<>();

		// Obtain all AADL files contents in the project

		List<File> dirs = collectAllDirs(dir);

		for(File subdir: dirs) {
			for (File file : subdir.listFiles()) {
				if (file.getAbsolutePath().endsWith(".aadl")) {
					aadlFileNames.add(file.getAbsolutePath());
				}
			}
		}

		final List<Resource> resources = new ArrayList<>(aadlFileNames.size());
		for (int i = 0; i < aadlFileNames.size(); i++) {
			resources.add(rs.getResource(URI.createFileURI(aadlFileNames.get(i)), true));
		}

		// Load the resources
		for (final Resource resource : resources) {
			try {
				resource.load(null);
			} catch (final IOException e) {
				System.err.println("ERROR LOADING RESOURCE: " + e.getMessage());
			}
		}

		return resources;
	}

	/**
	 * Process an event corresponding to a selection of AADL project
	 * Translate an AADL project into objects
	 *
	 * */
	public static List<EObject> preprocessAadlFiles(File dir) {
		List<EObject> objects = new ArrayList<>();
		// Load all objects from resources
		for (final Resource resource : loadAadlFiles(dir)) {
			resource.getAllContents().forEachRemaining(objects::add);
		}
		return objects;
	}

	public static List<File> collectAllDirs(File dir) {
		List<File> allDirs = new ArrayList<File>();
		allDirs.add(dir);
		for(File file : dir.listFiles()) {
			if(file.isDirectory()) {
				allDirs.add(file);
				collectDir(file, allDirs);
			}
		}
		return allDirs;
	}
	public static void collectDir(File dir, List<File> allDirs) {
		for(File file : dir.listFiles()) {
			if(file.isDirectory()) {
				allDirs.add(file);
				collectDir(file, allDirs);
			}
		}
	}
}
