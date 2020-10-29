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

import com.ge.research.osate.verdict.aadl2vdm.Agree2Vdm;
import com.ge.research.osate.verdict.gui.BundlePreferences;
import com.ge.research.osate.verdict.gui.CRVReportGenerator;
import com.ge.research.osate.verdict.gui.CRVSettingsPanel;
import com.ge.verdict.vdm.VdmTranslator;

import verdict.vdm.vdm_model.Model;

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
			// Print on console
			IIntroPart introPart = PlatformUI.getWorkbench().getIntroManager().getIntro();
			PlatformUI.getWorkbench().getIntroManager().closeIntro(introPart);
			final IWorkbenchWindow iWindow = HandlerUtil.getActiveWorkbenchWindow(event);
			VerdictHandlersUtils.setPrintOnConsole("CRV Output");
			final Display mainThreadDisplay = Display.getCurrent();

			// Set up the thread to invoke the translators and Kind2
			Thread crvAnalysisThread = new Thread() {
				@Override
				public void run() {
					try {
						String dockerImage = BundlePreferences.getDockerImage();
						String bundleJar = BundlePreferences.getBundleJar();
						if (dockerImage.isEmpty() && bundleJar.isEmpty()) {
							System.out.println("Please set VERDICT Bundle Jar path in Preferences");
							return;
						}
						String kind2Bin = BundlePreferences.getKind2Bin();
						if (dockerImage.isEmpty() && kind2Bin.isEmpty()) {
							System.out.println("Please set kind2 binary path in Preferences");
							return;
						}

						VerdictHandlersUtils.printGreeting();

						List<String> selection = VerdictHandlersUtils.getCurrentSelection(event);
						File projectDir = new File(selection.get(0));
						File vdmFile = new File(System.getProperty("java.io.tmpdir"), projectDir.getName() + ".xml");
						runAadl2Vdm(projectDir, vdmFile);

						String outputPath = new File(System.getProperty("java.io.tmpdir"), "crv_output.xml")
								.getCanonicalPath();
						String outputPathBa = new File(System.getProperty("java.io.tmpdir"), "crv_output_ba.xml")
								.getCanonicalPath();

						if (runBundle(bundleJar, dockerImage, vdmFile.getCanonicalPath(), outputPath, kind2Bin)) {
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

	/**
	 * Calls Aadl2Vdm translator and writes model to vdmFile
	 * @param dir
	 * @param vdmFile
	 */
	public static void runAadl2Vdm(File dir, File vdmFile) {
		Agree2Vdm agree2vdm = new Agree2Vdm();
		Model model = agree2vdm.execute(dir);
		VdmTranslator.marshalToXml(model, vdmFile);
	}

	public static boolean runBundle(String bundleJar, String dockerImage, String vdmFile, String outputPath,
			String kind2bin) throws IOException {

		VerdictBundleCommand command = new VerdictBundleCommand();
		command
			.jarOrImage(bundleJar, dockerImage)
			.arg("--vdm")
			.argBind(vdmFile, "/app/tmp/verdict_model.xml")
			.arg("--crv")
			.argBind(outputPath, "/app/tmp/crv_output.xml")
			.arg2(kind2bin, "/app/kind2");

		for (String threat : CRVSettingsPanel.selectedThreats) {
			command.arg(threat);
		}
		if (CRVSettingsPanel.isBlameAssignment) {
			command.arg("-BA");
			if (CRVSettingsPanel.componentLevel) {
				command.arg("-C");
			}
			if (CRVSettingsPanel.isGlobal) {
				command.arg("-G");
			}			
		}
		if(CRVSettingsPanel.isMeritAssignment) {
			command.arg("-MA");
		}
		if (CRVSettingsPanel.testCaseGeneration) {
			command.arg("-ATG");
		}

		int code = command.runJarOrImage();
		return code == 0;
	}
}
