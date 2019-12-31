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
import com.ge.research.osate.verdict.gui.CRVReportGenerator;
import com.ge.research.osate.verdict.gui.CRVSettingsPanel;

/**
*
* Author: Paul Meng
* Date: Jun 12, 2019
*
*/

public class CRVHandler extends AbstractHandler {

	static final String VDMINST = VerdictHandlersUtils.BINDIR + "vdminstrumentor.jar";
	static final String VDM2LUS = VerdictHandlersUtils.BINDIR + "vdm2lus.jar";

	// Kind2 related command line arguments
	static final String KIND2 = VerdictHandlersUtils.OSDIR + "kind2";
	static final String COLOR = "--color";
	static final String FALSE = "false";
	static final String TRUE = "true";

	static final String LUS = ".lus";
	static final String XML = ".xml";
	static final String BA = "-B";
	static final String BAOPT = "--max_weak_assumptions";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (VerdictHandlersUtils.startRun()) {
			IIntroPart introPart = PlatformUI.getWorkbench().getIntroManager().getIntro();
			PlatformUI.getWorkbench().getIntroManager().closeIntro(introPart);
			IWorkbenchWindow iWindow = HandlerUtil.getActiveWorkbenchWindow(event);

			VerdictHandlersUtils.setPrintOnConsole("CRV Output");

			Display mainThreadDisplay = Display.getCurrent();

			// Set up the thread to invoke the translators and Kind2
			Thread crvAnalysisThread = new Thread() {
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
						String kind2Bin = BundlePreferences.getKind2Bin();
						if (kind2Bin.length() == 0) {
							System.out.println("Please set kind2 binary path in Preferences");
							return;
						}

						VerdictHandlersUtils.printGreeting();

						String outputPath = new File(System.getProperty("java.io.tmpdir"), "crv_output.xml")
								.getAbsolutePath();
						String outputPathBa = new File(System.getProperty("java.io.tmpdir"), "crv_output_ba.xml")
								.getAbsolutePath();

						List<String> selection = VerdictHandlersUtils.getCurrentSelection(event);

						if (runBundle(bundleJar, selection.get(0), outputPath, aadl2imlBin, kind2Bin)) {
							// Run this code on the UI thread
							mainThreadDisplay.asyncExec(() -> {
								new CRVReportGenerator(outputPath, outputPathBa, iWindow);
							});
						}
					} finally {
						VerdictHandlersUtils.finishRun();
					}
				}
			};
			crvAnalysisThread.start();
		}
		return null;
	}

	public static boolean runBundle(String bundleJar, String inputPath, String outputPath, String aadl2imlbin,
			String kind2bin) {
		List<String> args = new ArrayList<>();

		args.add(VerdictHandlersUtils.JAVA);
		args.add(VerdictHandlersUtils.JAR);
		args.add(bundleJar);

		args.add("--aadl");
		args.add(inputPath);
		args.add(aadl2imlbin);

		args.add("--crv");
		args.add(outputPath);
		args.add(kind2bin);

		args.addAll(CRVSettingsPanel.selectedThreats);

		if (CRVSettingsPanel.blameAssignment) {
			args.add("-BA");
		}

		if (CRVSettingsPanel.testCaseGeneration) {
			args.add("-ATG");
		}

		if (CRVSettingsPanel.componentLevel) {
			args.add("-C");
		}

		int code = VerdictHandlersUtils.run(args.toArray(new String[args.size()]), null);

		return code == 0;
	}
}
