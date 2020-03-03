package com.ge.research.osate.verdict.handlers;

import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
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

	public static void errAndExit(String err) {
		System.out.println("Error: " + err);
		System.exit(-1);
	}
}
