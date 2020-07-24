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

import com.ge.research.osate.verdict.aadl2csv.Aadl2CsvTranslator;
import com.ge.research.osate.verdict.gui.BundlePreferences;
import com.ge.research.osate.verdict.gui.MBASSettingsPanel;
import com.ge.research.osate.verdict.gui.GSNSettingsPanel;
import com.ge.research.osate.verdict.aadl2vdm.Aadl2Vdm;
import verdict.vdm.vdm_model.Model;
import com.ge.verdict.vdm.VdmTranslator;

public class GSNHandler extends AbstractHandler {
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
			
			//If no root goal Id has been selected
			if(GSNSettingsPanel.rootGoalId == null){
				Thread noIdSelectedThread = new Thread() {
					@Override
					public void run() {
						try {
							System.out.println("Please select a root goal ID for creating GSN fragment.");
							} finally {
							VerdictHandlersUtils.finishRun();
						}						
					}
				};
				noIdSelectedThread.start();
			}
			else {//If a root goal Id has been selected
				Thread createGsnThread = new Thread() {
					@Override
					public void run() {
						try {
							
							System.out.println("generating GSN for rootogoal: "+ GSNSettingsPanel.rootGoalId);
							
							//Checking if all necessary settings exist
							String stemProjPath = BundlePreferences.getStemDir();
							if (stemProjPath.isEmpty()) {
								System.out.println("Please set STEM directory path in Preferences");
								return;
							}
							String dockerImage = BundlePreferences.getDockerImage();
							String bundleJar = BundlePreferences.getBundleJar();
							if (dockerImage.isEmpty() && bundleJar.isEmpty()) {
								System.out.println("Please set VERDICT Bundle Jar path in Preferences");
								return;
							}
							String soteriaPpBin = BundlePreferences.getSoteriaPpBin();
							if (dockerImage.isEmpty() && soteriaPpBin.isEmpty()) {
								System.out.println("Please set soteria++ binary path in Preferences");
								return;
							}
							String graphVizPath = BundlePreferences.getGraphVizPath();
							if (dockerImage.isEmpty() && graphVizPath.isEmpty()) {
								System.out.println("Please set GraphViz path in Preferences");
								return;
							}

							//Directory tp store GSN related outputs
							File gsnOutputFolder = new File(stemProjPath, "GSN");
							
							if (gsnOutputFolder.exists() && gsnOutputFolder.isDirectory()) {
								deleteFilesInDir("svg", gsnOutputFolder);
								deleteFilesInDir("dot", gsnOutputFolder);
								deleteFilesInDir("xml", gsnOutputFolder);
							} else {
								gsnOutputFolder.mkdir();
							}
							
							
							/**
							 * The GSN creator backend needs:
							 * 	1. The rootId
							 *  2. The Gsn output directory
							 *  3. The Soteria++ Output directory
							 *  4. The path of aadl file with CASE properties
							 */
							String rootId = GSNSettingsPanel.rootGoalId;
							String soteriaOutputDir = stemProjPath + "/Output/Soteria_Output";
							String gsnOutputDir = gsnOutputFolder.getAbsolutePath();
							String caseAadlPath = ""; //Need to add the path for the aadl model here
							
							/**
							 * Create the xml model for the GSN creator 
							 * in the GSN output directory as modelXML.xml
							 */
							List<String> selection = VerdictHandlersUtils.getCurrentSelection(event);
							File projectDir = new File(selection.get(0));
							Aadl2Vdm translatorObject = new Aadl2Vdm();
							Model model = translatorObject.execute(projectDir);
							File modelXml = new File(gsnOutputFolder, "modelXML.xml");
							VdmTranslator.marshalToXml(model, modelXml);

														
			                //send the arguments to the backend 				
							if (runBundle(bundleJar, dockerImage, rootId,  gsnOutputDir, soteriaOutputDir, caseAadlPath, graphVizPath)) {
								
								//Nothing for now.
								
							}
							} catch (IOException e) {
								VerdictLogger.severe(e.toString());
							} finally {
							VerdictHandlersUtils.finishRun();
						}
					}
				};
				createGsnThread.start();
			}

		}
		return null;
	}

	public static void runAadl2Csv(File dir, String stemOutputDir, String soteriaOutputDir) {
		Aadl2CsvTranslator aadl2csv = new Aadl2CsvTranslator();
		aadl2csv.execute(dir, stemOutputDir, soteriaOutputDir);
	}

	public static boolean runBundle(String bundleJar, String dockerImage, String rootId, String gsnOutputDir, String soteriaOutputDir,
			String caseAadlPath, String graphVizPath) throws IOException {

		VerdictBundleCommand command = new VerdictBundleCommand();
		/**
		 * Arguments: --gsn <rootId> <gsnOutputDir> <soteriaOutputDir>
		 */
		command.env("GraphVizPath", graphVizPath).jarOrImage(bundleJar, dockerImage)
				.arg("--gsn").arg(rootId).arg(gsnOutputDir).arg(soteriaOutputDir).arg(caseAadlPath);

		int code = command.runJarOrImage();
		return code == 0;
	}

	/**
	 * Delete all files with given extension in given folder
	 */
	private static void deleteFilesInDir(String extension, File dir) {
		if (dir.exists()) {
			if (dir.isDirectory()) {
				for (File file : dir.listFiles()) {
					if (file.isFile()) {
						if (getFileExtension(file).equals(extension)) {
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
	 */
	private static String getFileExtension(File file) {
		String extension = "";
		if (file != null && file.exists()) {
			String name = file.getName();
			extension = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
		}
		return extension;

	}
}
