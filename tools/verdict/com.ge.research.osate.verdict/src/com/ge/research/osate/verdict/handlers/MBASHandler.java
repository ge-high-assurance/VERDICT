package com.ge.research.osate.verdict.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.intro.IIntroPart;

import com.ge.research.osate.verdict.gui.BundlePreferences;
import com.ge.research.osate.verdict.gui.MBASReportGenerator;

/**
*
* Author: Paul Meng
* Date: Jun 12, 2019
*
*/

public class MBASHandler extends AbstractHandler {
	static final String SEP = File.separator;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (VerdictHandlersUtils.startRun()) {
			// Print on console
			IIntroPart introPart = PlatformUI.getWorkbench().getIntroManager().getIntro();
			PlatformUI.getWorkbench().getIntroManager().closeIntro(introPart);
			final IWorkbenchWindow iWindow = HandlerUtil.getActiveWorkbenchWindow(event);
			VerdictHandlersUtils.setPrintOnConsole("MBAS Output");
			Display mainThreadDisplay = Display.getCurrent();

			Thread mbasAnalysisThread = new Thread() {
				@Override
				public void run() {
					try {
						String bundleJar = BundlePreferences.getBundleJar();
						if (bundleJar.length() == 0) {
							System.out.println("Please set Verdict Bundle Jar path in Preferences");
							return;
						}
						String aadl2imlBin = BundlePreferences.getAadl2imlBin();
						if (aadl2imlBin.length() == 0) {
							System.out.println("Please set aadl2iml binary path in Preferences");
							return;
						}
						String stemProjPath = BundlePreferences.getStemDir();
						if (stemProjPath.length() == 0) {
							System.out.println("Please set STEM directory path in Preferences");
							return;
						}
						String soteriaPpBin = BundlePreferences.getSoteriaPpBin();
						if (soteriaPpBin.length() == 0) {
							System.out.println("Please set soteria++ binary path in Preferences");
							return;
						}

						VerdictHandlersUtils.printGreeting();
						List<String> selection = VerdictHandlersUtils.getCurrentSelection(event);

						// Create CSVData, Output, Graphs folders if they don't exist
						// If they exist, delete all the csv and svg files
						File CSVDataFolder = new File(stemProjPath, "CSVData");
						File outputFolder = new File(stemProjPath, "Output");
						File graphsFolder = new File(stemProjPath, "Graphs");

						if (CSVDataFolder.exists() && CSVDataFolder.isDirectory()) {
							deleteCsvFilesInDir(CSVDataFolder);
						} else {
							CSVDataFolder.mkdir();
						}
						if (outputFolder.exists() && outputFolder.isDirectory()) {
							deleteCsvFilesInDir(outputFolder);
						} else {
							outputFolder.mkdir();
						}
						if (graphsFolder.exists() && graphsFolder.isDirectory()) {
							deleteSvgFilesInDir(graphsFolder);
						} else {
							graphsFolder.mkdir();
						}

						if (runBundle(bundleJar, selection.get(0), aadl2imlBin, stemProjPath, soteriaPpBin)) {
							// Soteria++ output directory
							String soteriaOut = stemProjPath + SEP + "Output" + SEP + "Soteria_Output";

							// Open SVG file generated by STEM
							VerdictHandlersUtils
									.openSvgGraphsInDir(new File(stemProjPath, "Graphs").getAbsolutePath());
							// Run this code on the UI thread
							mainThreadDisplay.asyncExec(() -> {

								File applicableDefense = new File(soteriaOut, "ApplicableDefenseProperties.xml");
								File implProperty = new File(soteriaOut, "ImplProperties.xml");

								// Display cyber related xml
								if (applicableDefense.exists() && implProperty.exists()) {
									new MBASReportGenerator(applicableDefense.getAbsolutePath(),
											implProperty.getAbsolutePath(), iWindow);
								} else {
									System.err.println("Info: No Soteria++ output generated!");
								}
							});
							// Display safety related text.
							VerdictHandlersUtils.openSafetyTxtInDir(soteriaOut);
						}
					} finally {
						VerdictHandlersUtils.finishRun();
					}
				}
			};
			mbasAnalysisThread.start();
		}
		return null;
	}

	public static boolean runBundle(String bundleJar, String inputPath, String aadl2imlBin, String stemProjectDir,
			String soteriaPpBin) {
		List<String> args = new ArrayList<>();

		args.add(VerdictHandlersUtils.JAVA);
		args.add(VerdictHandlersUtils.JAR);
		args.add(bundleJar);

		args.add("--aadl");
		args.add(inputPath);
		args.add(aadl2imlBin);

		args.add("--mbas");
		args.add(stemProjectDir);
		args.add(soteriaPpBin);

		int code = VerdictHandlersUtils.run(args.toArray(new String[args.size()]), null);

		return code == 0;
	}

	/**
	 * Delete all csv files in a folder
	 * */
	static void deleteCsvFilesInDir(File dir) {
		if (dir.exists()) {
			if (dir.isDirectory()) {
				for (File file : dir.listFiles()) {
					if (file.isFile()) {
						if (getFileExtension(file).equals("csv")) {
							file.delete();
						}
					}
				}
			} else {
				dir.mkdirs();
			}
		} else {
			dir.mkdirs();
		}
	}

	/**
	 * Delete all svg files in a folder
	 * */
	static void deleteSvgFilesInDir(File dir) {
		if (dir.exists()) {
			if (dir.isDirectory()) {
				for (File file : dir.listFiles()) {
					if (file.isFile()) {
						if (getFileExtension(file).equals("svg")) {
							file.delete();
						}
					}
				}
			} else {
				dir.mkdirs();
			}
		} else {
			dir.mkdirs();
		}
	}

	/**
	 * Get the extension of a file
	 * */
	private static String getFileExtension(File file) {
		String extension = "";

		try {
			if (file != null && file.exists()) {
				String name = file.getName();
				extension = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
			}
		} catch (Exception e) {
			extension = "";
		}

		return extension;

	}

}
