package com.ge.research.osate.verdict.handlers;

import java.io.File;
import java.io.IOException;
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
* @author Paul Meng
* Date: Jun 12, 2019
*
*/
public class CRVHandler extends AbstractHandler {
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
						String dockerImage = BundlePreferences.getDockerImage();
						String bundleJar = BundlePreferences.getBundleJar();
						if (dockerImage.isEmpty() && bundleJar.isEmpty()) {
							System.out.println("Please set Verdict Bundle Jar path in Preferences");
							return;
						}
						String aadl2imlBin = BundlePreferences.getAadl2imlBin();
						if (dockerImage.isEmpty() && aadl2imlBin.isEmpty()) {
							System.out.println("Please set aadl2iml binary path in Preferences");
							return;
						}
						String kind2Bin = BundlePreferences.getKind2Bin();
						if (dockerImage.isEmpty() && kind2Bin.isEmpty()) {
							System.out.println("Please set kind2 binary path in Preferences");
							return;
						}

						VerdictHandlersUtils.printGreeting();

						String outputPath = new File(System.getProperty("java.io.tmpdir"), "crv_output.xml")
								.getCanonicalPath();
						String outputPathBa = new File(System.getProperty("java.io.tmpdir"), "crv_output_ba.xml")
								.getCanonicalPath();

						List<String> selection = VerdictHandlersUtils.getCurrentSelection(event);

						if (runBundle(bundleJar, dockerImage, selection.get(0), outputPath, aadl2imlBin, kind2Bin)) {
							// Run this code on the UI thread
							mainThreadDisplay.asyncExec(() -> {
								new CRVReportGenerator(outputPath, outputPathBa, iWindow);
							});
						}
					} catch (IOException e) {
						VerdictLogger.severe(e.toString());
					} finally {
						VerdictHandlersUtils.finishRun();
					}
				}
			};
			crvAnalysisThread.start();
		}
		return null;
	}

	public static boolean runBundle(String bundleJar, String dockerImage, String inputPath, String outputPath,
			String aadl2imlbin, String kind2bin) throws IOException {

		VerdictBundleCommand command = new VerdictBundleCommand();
		command
			.jarOrImage(bundleJar, dockerImage)
			.arg("--aadl")
			.argBind(inputPath, "/app/model")
			.arg2(aadl2imlbin, "/app/aadl2iml")
			.arg("--crv")
			.argBind(outputPath, "/app/tmp/crv_output.xml")
			.arg2(kind2bin, "/app/kind2");

		for (String threat : CRVSettingsPanel.selectedThreats) {
			command.arg(threat);
		}
		if (CRVSettingsPanel.blameAssignment) {
			command.arg("-BA");
		}
		if (CRVSettingsPanel.testCaseGeneration) {
			command.arg("-ATG");
		}
		if (CRVSettingsPanel.componentLevel) {
			command.arg("-C");
		}

		int code = command.runJarOrImage();
		return code == 0;
	}
}
