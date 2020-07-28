package com.ge.research.osate.verdict.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.intro.IIntroPart;
import org.osate.aadl2.AbstractType;
import org.osate.aadl2.AnnexSubclause;
import org.osate.aadl2.BusType;
import org.osate.aadl2.ComponentType;
import org.osate.aadl2.DeviceType;
import org.osate.aadl2.MemoryType;
import org.osate.aadl2.ProcessType;
import org.osate.aadl2.ProcessorType;
import org.osate.aadl2.SubprogramType;
import org.osate.aadl2.SystemType;
import org.osate.aadl2.ThreadGroupType;
import org.osate.aadl2.ThreadType;
import org.osate.aadl2.VirtualProcessorType;

import com.ge.research.osate.verdict.aadl2csv.Aadl2CsvTranslator;
import com.ge.research.osate.verdict.aadl2vdm.Aadl2Vdm;
import com.ge.research.osate.verdict.dsl.VerdictUtil;
import com.ge.research.osate.verdict.dsl.verdict.CyberMission;
import com.ge.research.osate.verdict.dsl.verdict.CyberReq;
import com.ge.research.osate.verdict.dsl.verdict.SafetyReq;
import com.ge.research.osate.verdict.dsl.verdict.Statement;
import com.ge.research.osate.verdict.dsl.verdict.Verdict;
import com.ge.research.osate.verdict.gui.AssuranceCaseSettingsPanel;
import com.ge.research.osate.verdict.gui.BundlePreferences;
import com.ge.research.osate.verdict.gui.MBASSettingsPanel;
import com.ge.verdict.vdm.VdmTranslator;

import verdict.vdm.vdm_model.Model;

public class GSNHandler extends AbstractHandler {
	static final String SEP = File.separator;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (VerdictHandlersUtils.startRun()) {
			// Print on console
			IIntroPart introPart = PlatformUI.getWorkbench().getIntroManager().getIntro();
			PlatformUI.getWorkbench().getIntroManager().closeIntro(introPart);
			final IWorkbenchWindow iWindow = HandlerUtil.getActiveWorkbenchWindow(event);
//			VerdictHandlersUtils.setPrintOnConsole("MBAS Output");
			VerdictHandlersUtils.printGreeting();
			Display mainThreadDisplay = Display.getCurrent();
			

			Thread createGsnThread = new Thread() {
				@Override
				public void run() {
					try {
						//Getting project directory
						List<String> selection = VerdictHandlersUtils.getCurrentSelection(event);
						File projectDir = new File(selection.get(0));
						
						//Check if input Id is correct, finish otherwise
						Aadl2Vdm objectVdmTran = new Aadl2Vdm();
						List<String> allReqIds = getRequirementIds(objectVdmTran.preprocessAadlFiles(projectDir)); 
						
						//getting input
						String userInput;
						
						//checking if input is valid
						if (AssuranceCaseSettingsPanel.rootGoalId==null) {
							System.out.println("Warning: Invalid input. Generating for all mission requirements.");
							userInput = "ALLMREQKEY"; //will be interpreted as All mission requirements by backend
						} else if (!allReqIds.contains(AssuranceCaseSettingsPanel.rootGoalId.trim())){
							System.out.println("Warning: Invalid input. Generating for all mission requirements.");
							userInput = "ALLMREQKEY"; //will be interpreted as All mission requirements by backend
						} else {
							userInput = AssuranceCaseSettingsPanel.rootGoalId.trim();
						}
						
						VerdictHandlersUtils.setPrintOnConsole("Generating GSN for : "+ AssuranceCaseSettingsPanel.rootGoalId);
						
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

						// Create CSVData, Output, Graphs, GSN folders if they don't exist
						// If they exist, delete all unnecessary files
						File dataFolder = new File(stemProjPath, "CSVData");
						File outputFolder = new File(stemProjPath, "Output");
						File graphsFolder = new File(stemProjPath, "Graphs");
						File gsnOutputFolder = new File(stemProjPath, "GSN");
						
						
						if (dataFolder.exists() && dataFolder.isDirectory()) {
							deleteFilesInDir("csv", dataFolder);
						} else {
							dataFolder.mkdir();
						}
						if (outputFolder.exists() && outputFolder.isDirectory()) {
							deleteFilesInDir("csv", outputFolder);
						} else {
							outputFolder.mkdir();
						}
						if (graphsFolder.exists() && graphsFolder.isDirectory()) {
							deleteFilesInDir("svg", graphsFolder);
						} else {
							graphsFolder.mkdir();
						}
						if (gsnOutputFolder.exists() && gsnOutputFolder.isDirectory()) {
							deleteFilesInDir("svg", gsnOutputFolder);
							deleteFilesInDir("dot", gsnOutputFolder);
							deleteFilesInDir("xml", gsnOutputFolder);
						} else {
							gsnOutputFolder.mkdir();
						}
						

						//Running MBAS First to create the soteria++ outputs
						Aadl2CsvTranslator aadl2csv = new Aadl2CsvTranslator();

						aadl2csv.execute(projectDir, dataFolder.getAbsolutePath(), outputFolder.getAbsolutePath());
						
						//running MBAS 
						boolean outputsGenerated = runBundleMBAS(bundleJar, dockerImage, projectDir.getName(), stemProjPath, soteriaPpBin, graphVizPath);

						/** TEMPORARY! CHANGE BEFORE FINAL */
						//boolean outputsGenerated = true;
						
                        // if MBAS succeeded, proceed to GSN
						if (outputsGenerated){
							
							/**
							 * The GSN creator backend needs:
							 * 	1. The rootId
							 *  2. The Gsn output directory
							 *  3. The Soteria++ Output directory
							 *  4. The path of the project directory 
							 *     which contains the aadl files with CASE properties
							 */
							String rootId = userInput;
							String soteriaOutputDir = stemProjPath + SEP + "Output" + SEP + "Soteria_Output";
							String gsnOutputDir = gsnOutputFolder.getAbsolutePath();
							String caseAadlDir = projectDir.getAbsolutePath();
							
							/**
							 * Create the xml model for the GSN creator 
							 * in the GSN output directory as modelXML.xml
							 */
							Aadl2Vdm translatorObject = new Aadl2Vdm();
							Model model = translatorObject.execute(projectDir);
							File modelXml = new File(gsnOutputFolder, "modelXML.xml");
							VdmTranslator.marshalToXml(model, modelXml);

														
			                //send the arguments to the backend 				
							if (runBundle(bundleJar, dockerImage, rootId,  gsnOutputDir, soteriaOutputDir, caseAadlDir, projectDir.getName(), graphVizPath)) {
								//Show graphs in tab if option selected
								if(AssuranceCaseSettingsPanel.showInTab) {
									// Open SVG GSN files
									VerdictHandlersUtils.openSvgGraphsInDir(new File(stemProjPath, "GSN").getAbsolutePath());
									
								}
							}
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
		return null;
	}


	/**
	 * 
	 * @param bundleJar
	 * @param dockerImage
	 * @param rootId
	 * @param gsnOutputDir
	 * @param soteriaOutputDir
	 * @param caseAadlDir
	 * @param projectName
	 * @param graphVizPath
	 * @return
	 * @throws IOException
	 */
	public static boolean runBundle(String bundleJar, String dockerImage, String rootId, String gsnOutputDir, String soteriaOutputDir,
			String caseAadlDir, String projectName, String graphVizPath) throws IOException {

		//if xml has been asked by user
		String xmlKey = "";
		if (AssuranceCaseSettingsPanel.generateXml) {
			xmlKey = "-x";
		}
		
		VerdictBundleCommand command = new VerdictBundleCommand();
		/**
		 * Arguments: --gsn <rootId> <gsnOutputDir> <soteriaOutputDir> <caseAadlDir> 
		 *              xmlKey
		 */
		command.env("GraphVizPath", graphVizPath).jarOrImage(bundleJar, dockerImage)
				.arg("--csv").arg(projectName)
				.arg("--gsn").arg(rootId).arg(gsnOutputDir).arg(soteriaOutputDir).arg(caseAadlDir)
		        .arg(xmlKey); 
		

		
//		//Since cannot test via plugin, just printing into a file for now.
//		String args = "--csv "+projectName+" --gsn "+rootId+" "+gsnOutputDir+" "+soteriaOutputDir+" "+caseAadlDir+" "+xmlKey;
//		File printArguments = new File("/Users/212807042/Desktop/pluginArgs.txt");
//        FileOutputStream fos = new FileOutputStream(printArguments);
//        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
//        bw.write(args);
//        bw.close();


		int code = command.runJarOrImage();
		return code == 0;
//        return true;
	}

	
	
	public static void runAadl2Csv(File dir, String stemOutputDir, String soteriaOutputDir) {
		Aadl2CsvTranslator aadl2csv = new Aadl2CsvTranslator();
		aadl2csv.execute(dir, stemOutputDir, soteriaOutputDir);
	}

	public static boolean runBundleMBAS(String bundleJar, String dockerImage, String projectName,
			String stemProjectDir, String soteriaPpBin, String graphVizPath) throws IOException {

		VerdictBundleCommand command = new VerdictBundleCommand();
		command
			.env("GraphVizPath", graphVizPath)
			.jarOrImage(bundleJar, dockerImage)
			.arg("--csv")
			.arg(projectName)
			.arg("--mbas")
			.argBind(stemProjectDir, "/app/STEM")
			.arg2(soteriaPpBin, "/app/soteria_pp");

			command.arg("-c");
			command.arg("-s");

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
	
	
	/**
	 * A function that returns a list of all requirement IDs
	 * @param objects
	 * @return
	 */
	public List<String> getRequirementIds(List<EObject> objects) { 				
		List<String> allReqIds = new ArrayList<>();
		
		List<ComponentType> componentTypes = new ArrayList<>();

		for(EObject obj : objects) {
			if (obj instanceof SystemType) {
				componentTypes.add((SystemType) obj);
			} else if (obj instanceof BusType) {
				componentTypes.add((BusType)obj);
			} else if (obj instanceof SubprogramType) {
				componentTypes.add((SubprogramType)obj);
			} else if (obj instanceof ThreadType) {
				componentTypes.add((ThreadType)obj);
			} else if (obj instanceof MemoryType) {
				componentTypes.add((MemoryType)obj);
			} else if (obj instanceof DeviceType) {
				componentTypes.add((DeviceType)obj);
			} else if (obj instanceof AbstractType) {
				componentTypes.add((AbstractType)obj);
			} else if (obj instanceof ProcessType) {
				componentTypes.add((ProcessType)obj);
			} else if (obj instanceof ThreadGroupType) {
				componentTypes.add((ThreadGroupType)obj);
			} else if (obj instanceof VirtualProcessorType) {
				componentTypes.add((VirtualProcessorType)obj);
			} else if (obj instanceof ProcessorType) {
				componentTypes.add((ProcessorType)obj);
			}
		}
			
		for(ComponentType compType : componentTypes) {
			List<CyberMission> missionReqs = new ArrayList<>();
			List<CyberReq> cyberReqs = new ArrayList<>();
			List<SafetyReq> safetyReqs = new ArrayList<>();			
			
			for(AnnexSubclause annex : compType.getOwnedAnnexSubclauses()) {
				if(annex.getName().equalsIgnoreCase("verdict")) {
					Verdict verdictAnnex = VerdictUtil.getVerdict(annex);

					for (Statement statement : verdictAnnex.getElements()) {
						if(statement instanceof CyberMission) {
							missionReqs.add((CyberMission)statement);
						} else if(statement instanceof CyberReq) {
							cyberReqs.add((CyberReq)statement);
						} else if(statement instanceof SafetyReq) {
							safetyReqs.add((SafetyReq)statement);
						}
					}
				}
			}
			
			if(!missionReqs.isEmpty()) {
				for(CyberMission aMission : missionReqs) {
					allReqIds.add(aMission.getId());
				}
			
			}
			if(!cyberReqs.isEmpty()) {
				for(CyberReq aCyberReq : cyberReqs) {
					allReqIds.add(aCyberReq.getId());
				}
			
			}
			if(!missionReqs.isEmpty()) {
				for(CyberMission aMission : missionReqs) {
					allReqIds.add(aMission.getId());
				}
			
			}
			if(!safetyReqs.isEmpty()) {
				for(SafetyReq aSafetyReq : safetyReqs) {
					allReqIds.add(aSafetyReq.getId());
				}
			
			}
		}
			
		// Removing duplicates
        List<String> duplicateFree = new ArrayList<>(new HashSet<>(allReqIds));

        return duplicateFree; 
	}
	
}
