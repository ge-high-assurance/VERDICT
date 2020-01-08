package com.ge.research.osate.verdict.handlers;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
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
* Author: Paul Meng
* Date: Jun 12, 2019
*
*/

public class VerdictHandlersUtils {
	// System parameters
	static final String SEP = File.separator;

	// Platform temporary directory
	static final String TEMPDIR = System.getProperty("java.io.tmpdir").endsWith(SEP)
			? System.getProperty("java.io.tmpdir")
			: System.getProperty("java.io.tmpdir") + SEP;
	static final String MACHINEOS = System.getProperty("os.name").toLowerCase();

	// Might need to add more OS
	static final String OS = MACHINEOS.startsWith("mac") ? "osx"
			: (MACHINEOS.startsWith("win") ? "win" : ((MACHINEOS.startsWith("linux") ? "glnx" : "unknown")));


	// SVG graphs
	static final String SVG = "svg";
	static final String TXT = "txt";
	static final String SAFETYTXT = "-safety.txt";

	// Java command
	static final String JAVA = "java";
	static final String JAR = "-jar";

	// Environment variables
	static final String MAC_PATH = "PATH=/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin";
	static final String[] MAC_ENV = new String[] { MAC_PATH };

	// Ensure only one handler runs commands at a time
	static boolean isRunningNow;

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

	/**
	 * Runs an executable without printing the output
	 *
	 * @param cmdline
	 *            command line arguments
	 * @return exit code of process
	 */
	public static int runWithoutMsg(String[] cmdline, String workingDir) {
		OutputStream dummyOut = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				// Method intended to swallow whatever comes at it
			}
		};
		Execute executor = new Execute(new PumpStreamHandler(dummyOut));
		return antRun(executor, cmdline, workingDir);
	}

	/**
	 * Runs an executable and print the output using process builder
	 *
	 * @param cmdline
	 *            command line arguments
	 * @return output of process
	 */
	public static int run(String[] cmdline, String workingDir, String[] env) {
		OutputStream outStream = System.out;
		OutputStream errStream = System.err;
		Execute executor = new Execute(new PumpStreamHandler(outStream, errStream));
		if (env != null) {
			String[] prevEnv = executor.getEnvironment();
			if (prevEnv != null) {
				String[] newEnv = Arrays.copyOf(prevEnv, prevEnv.length + env.length);
				System.arraycopy(env, 0, newEnv, prevEnv.length, env.length);
				executor.setEnvironment(newEnv);
			} else {
				executor.setEnvironment(env);
			}
		}
		return antRun(executor, cmdline, workingDir);
	}

	/**
	 * Runs an executable using the Ant Execute class
	 *
	 * @param cmdline
	 *            command line arguments
	 * @return exit code of process
	 */
	protected static int antRun(Execute executor, String[] cmdline, String workingDir) {
		if (OS.equals("osx")) {
			VerdictLogger.info("Setting environment variables...");
			executor.setEnvironment(MAC_ENV);
		} else if (OS.equals("glnx")) {
			// Don't have any environment variables to set for now
		} else if (OS.equals("win") || OS.equals("unknown")) {
			VerdictLogger.severe("We don't support your OS yet!");
			return 1;
		}

		// Set working directory
		if (VerdictHandlersUtils.isValidDir(workingDir)) {
			executor.setWorkingDirectory(new File(workingDir));
			VerdictLogger.info("Set the working directory to: " + executor.getWorkingDirectory());
		}

		executor.setCommandline(cmdline);
		VerdictLogger.info("Running command: " + arrayToString(cmdline));

		int err = 0;
		try {
			err = executor.execute();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return err;
	}



	static void deleteFileIfExists(File outputFile) {
		if (outputFile.exists()) {
			outputFile.delete();
		}
	}

	/**
	 * Open graphs in Eclipse internal browser
	 * Opens an URL with the default settings
	 * @param url
	 * @return
	 */

	static void openSvgGraphsAndTxtInDir(String dirPath) {
		if (isValidDir(dirPath)) {
			File graphFolder = new File(dirPath);

			for (File f : graphFolder.listFiles()) {
				if (f.getName().endsWith(SVG)) {
					try {
						open(f.toURI().toURL(), Display.getDefault());
						File txtFile = constructTxtFileName(f);
						if (txtFile != null) {
							open(txtFile.toURI().toURL(), Display.getDefault());
						}
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	static File constructTxtFileName(File svgFile) {
		File txtFile = null;
		String svgFilePath = svgFile.getAbsolutePath();

		if (svgFilePath.endsWith(SVG)) {
			int len = svgFilePath.length();
			String txtFilePath = svgFilePath.substring(0, len - 3);
			txtFile = new File(txtFilePath + TXT);
		}
		return txtFile;
	}
	static void openSvgGraphsInDir(String dirPath) {
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

	static void openTxtInDir(String dirPath) {
		if (isValidDir(dirPath)) {
			File graphFolder = new File(dirPath);

			for (File f : graphFolder.listFiles()) {
				if (f.getName().endsWith(TXT)) {
					try {
						open(f.toURI().toURL(), Display.getDefault());
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	static void openSafetyTxtInDir(String dirPath) {
		if (isValidDir(dirPath)) {
			File graphFolder = new File(dirPath);

			for (File f : graphFolder.listFiles()) {
				if (f.getName().endsWith(SAFETYTXT)) {
					try {
						open(f.toURI().toURL(), Display.getDefault());
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static void open(final URL url, Display display) {
		display.syncExec(() -> internalOpen(url, false));
	}

	public static void openExternal(final URL url, Display display) {
		display.syncExec(() -> internalOpen(url, true));
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
	 *
	 * @param execFilePath
	 * 			the executable file path
	 * @return True, if it exists and can be executed. Otherwise, return false.
	 */
	static boolean isValidExecutable(String execFilePath) {
		if (System.getenv("VERDICT_EXTERN") == null) {
			VerdictLogger.severe("Please set a valid value for the environment variable VERDICT_EXTERN on your OS!");
			return false;
		}

		File exeFile = new File(execFilePath);

		if (!exeFile.exists()) {
			VerdictLogger.warning("The executable file does not exist: " + execFilePath);
			return false;
		}
		return true;
	}

	/**
	 * Check if a directory is valid
	 * */
	static boolean isValidDir(String path) {
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

	/**
	 * Check if a file is valid
	 * */
	static boolean isValidFile(String filePath) {
		if (filePath == null) {
			VerdictLogger.warning("File path is null!");
			return false;
		}

		File f = new File(filePath);

		if (!f.exists()) {
			VerdictLogger.warning("File: " + filePath + " does not exist!");
			return false;
		}
		if (!f.isFile()) {
			VerdictLogger.warning(filePath + " is not a file!");
			return false;
		}
		return true;
	}

	/**
	 * Check if the input directory is valid and has files
	 * */
	static boolean isValidDirWithFiles(String dirPath) {
		if (dirPath == null) {
			return false;
		}

		File dir = new File(dirPath);

		if (!dir.exists()) {
			return false;
		}
		if (!dir.isDirectory()) {
			return false;
		}
		if (dir.list().length == 0) {
			return false;
		}
		return true;
	}

	/**
	 * Print an input String array into a single String
	 * */
	static String arrayToString(String[] arr) {
		StringBuilder sb = new StringBuilder();

		if (arr != null) {
			for (int i = 0; i < arr.length; ++i) {
				sb.append(arr[i] + " ");
			}
		}

		return sb.toString();
	}


	/** the current selection in the AADL model
	 *  Return a list of paths to the selected objects
	 */
	static List<String> getCurrentSelection(ExecutionEvent event) {
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

	/**
	 * Combine two arrays into one
	 * */
	static String[] combine(String[] a, String[] b) {
		int length = a.length + b.length;
		String[] result = new String[length];
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}

	/**
	 * Check if a file is valid and non-empty
	 * */
	static boolean isValidNonemptyFile(String filePath) {
		if (filePath == null) {
			return false;
		}

		File f = new File(filePath);

		if (!f.exists()) {
			return false;
		}
		if (!f.isFile()) {
			return false;
		}
		if (f.length() == 0) {
			return false;
		}
		return true;
	}

	public static void errAndExit(String err) {
		System.out.println("Error: " + err);
		System.exit(-1);
	}
}
