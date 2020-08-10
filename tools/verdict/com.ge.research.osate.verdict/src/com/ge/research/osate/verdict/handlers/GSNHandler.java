package com.ge.research.osate.verdict.handlers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroPart;

import com.ge.research.osate.verdict.aadl2csv.Aadl2CsvTranslator;
import com.ge.research.osate.verdict.aadl2vdm.Aadl2Vdm;
import com.ge.research.osate.verdict.gui.AssuranceCaseSettingsPanel;
import com.ge.research.osate.verdict.gui.BundlePreferences;
import com.ge.verdict.vdm.VdmTranslator;

import verdict.vdm.vdm_model.Model;

/**
 * 
 * @author Saswata Paul
 *
 */
public class GSNHandler extends AbstractHandler {
	static final String SEP = File.separator;
	
	/**
	 * a Tuple class used for packing two lists of strings 
	 * @author Saswata Paul
	 *
	 */
	protected class ListTuple{
		protected List<String> allReqs;
		protected List<String> cyberReqs;
		/**
		 * @return the allReqs
		 */
		protected List<String> getAllReqs() {
			return allReqs;
		}
		/**
		 * @param allReqs the allReqs to set
		 */
		protected void setAllReqs(List<String> allReqs) {
			this.allReqs = allReqs;
		}
		/**
		 * @return the cyberReqs
		 */
		protected List<String> getCyberReqs() {
			return cyberReqs;
		}
		/**
		 * @param cyberReqs the cyberReqs to set
		 */
		protected void setCyberReqs(List<String> cyberReqs) {
			this.cyberReqs = cyberReqs;
		}
		
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (VerdictHandlersUtils.startRun()) {
			// Print on console
			VerdictHandlersUtils.setPrintOnConsole("");
			IIntroPart introPart = PlatformUI.getWorkbench().getIntroManager().getIntro();
			PlatformUI.getWorkbench().getIntroManager().closeIntro(introPart);
			VerdictHandlersUtils.printGreeting();

			Thread createGsnThread = new Thread() {
				@Override
				public void run() {
					try {
						//Getting project directory
						List<String> selection = VerdictHandlersUtils.getCurrentSelection(event);
						File projectDir = new File(selection.get(0));
						
						
						/**
						 * Create the xml model for the GSN creator 
						 * in the GSN output directory as modelXML.xml
						 */
						Aadl2Vdm translatorObject = new Aadl2Vdm();
						Model model = translatorObject.execute(projectDir);
						
												
						//getting required input
						String userInput = decideCorrectInput(model);
																									
						
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
						File gsnOutputFolder = new File(stemProjPath, "gsn");
						
						
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
						
						
//						/**
//						 * For testing only
//						 */
//						boolean outputsGenerated = true;
						
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
							String gsnOutputDir = gsnOutputFolder.getCanonicalPath();
							String caseAadlDir = projectDir.getCanonicalPath();
							
							/**
							 * save the xml model for the GSN creator 
							 * in the GSN output directory as modelXML.xml
							 */
							File modelXml = new File(gsnOutputFolder, "modelXML.xml");
							VdmTranslator.marshalToXml(model, modelXml);

														
			                //send the arguments to the backend 				
							if (runBundle(bundleJar, dockerImage, rootId,  gsnOutputDir, soteriaOutputDir, caseAadlDir, projectDir.getName(), graphVizPath)) {
								//Show graphs in tab if option selected
								if(AssuranceCaseSettingsPanel.showInTab) {
									// Open SVG GSN files
									VerdictHandlersUtils.openSvgGraphsInDir(new File(stemProjPath, "gsn").getAbsolutePath());
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

		//if xml has been enabled by user
		String xmlKey = "";
		//if security cases has been enabled by user
		String securityCasesKey = "";
		
		if (AssuranceCaseSettingsPanel.generateXml) {
			xmlKey = "-x";
		}
		
		if (AssuranceCaseSettingsPanel.securityCases) {
			securityCasesKey = "-z";
		}
		
		VerdictBundleCommand command = new VerdictBundleCommand();
		/**
		 * Arguments: --gsn <rootId> <gsnOutputDir> <soteriaOutputDir> <caseAadlDir> 
		 *              xmlKey
		 *              securityCasesKey
		 */
		command
			.env("GraphVizPath", graphVizPath)
			.jarOrImage(bundleJar, dockerImage)
			.arg("--csv")
			.arg(projectName)
			.arg("--gsn")
			.arg(rootId)
			.argBind(gsnOutputDir, "/app/gsn")
			.argBind(soteriaOutputDir, "/app/Soteria_Output")
			.argBind(caseAadlDir, "/app/model")
		    .arg(xmlKey)
		    .arg(securityCasesKey); 

        
        
		int code = command.runJarOrImage();
		return code == 0;
		

	}
	
	
	/**
	 * A function that decides which requirement Ids should be sent to the bundle
	 * Expected Behavior:
	 * 1. Security disabled:
	 * 	a. If no input/typo/invalid -> Sends all mission reqs
	 *  b. If valid inputs -> Sends all input reqs
	 * 2. Security enabled:
	 *  a. If all Ids specified are valid -> sends all Ids
	 * 		(i) For every cyber req that is specified -> sends the cyber requirement 
	 *  	(ii) For every mission req that is specified -> sends the requirement and all supporting cyber reqs 
	 *  b. If no input/typo/invalid -> sends all mission reqs and supporting cyber reqs
	 *   
	 * 
	 * @param model
	 * @return
	 */
	public String decideCorrectInput(Model model) {
		String correctInput = "";
		
		boolean emptyFlag = false;
		boolean securityFlag = AssuranceCaseSettingsPanel.securityCases;

		//to store all Ids
		List<String> allIds = new ArrayList<>();
		
		//get all mission Ids
        List<String> missionIds = new ArrayList<>();
        for (verdict.vdm.vdm_model.Mission aMission : model.getMission()) {
            missionIds.add(aMission.getId());
            allIds.add(aMission.getId());
        }

		//get all cyber Ids
        List<String> cyberIds = new ArrayList<>();
        for (verdict.vdm.vdm_model.CyberReq aCyberReq : model.getCyberReq()) {
            cyberIds.add(aCyberReq.getId());
            allIds.add(aCyberReq.getId());
        }

		//get all safety Ids
        List<String> safetyIds = new ArrayList<>();
        for (verdict.vdm.vdm_model.SafetyReq aSafetyReq : model.getSafetyReq()) {
            safetyIds.add(aSafetyReq.getId());
            allIds.add(aSafetyReq.getId());
        }
        
        
		if (AssuranceCaseSettingsPanel.rootGoalId==null) {
			emptyFlag = true;
		} else if(AssuranceCaseSettingsPanel.rootGoalId.trim().length()==0) {
			emptyFlag = true;
		}
	
		if(emptyFlag) {
			System.out.println("Warning: No user specified requirement. Generating for all mission requirements.");
			//if security is enabled, input will have all mission reqs and all dependent cyber reqs
			if(securityFlag) {
				List<String> supportingCyberForAllMissions = new ArrayList<>();
				for(String missionId : missionIds) {
					supportingCyberForAllMissions.addAll(getAllSupportingCyberReqs(model, missionId));
					correctInput = correctInput + missionId+ ";";					
				}
				
				//removing duplicates from cupporting cybers
				List<String> duplicateFreeSupportingCyberForAllMissions= new ArrayList<>(new HashSet<>(supportingCyberForAllMissions));
				
				//addiing supportings to input
				for(String cyberId : duplicateFreeSupportingCyberForAllMissions) {
					correctInput = correctInput + cyberId+ ";";
				}
			} else { //if security is not enabled, input will have only mission reqs
				for(String missionId : missionIds) {
					correctInput = correctInput + missionId+ ";";					
				}
			}			
		} else {
			//split inputs by ";" and check if all inputs are valid Ids
			boolean validInputs = true;
			boolean cyberFlag = false;
			boolean missionFlag = false;
		
			//to collect any correct mission Id user has provided
			List<String> userProvidedMissions = new ArrayList<>();
			
			//splitting the input by ';' 
			String inputLine = AssuranceCaseSettingsPanel.rootGoalId.trim(); 
			String[] inputs = inputLine.split(";");
											
			//checking if input is valid and has a cyber requirement
			for(String in : inputs) {
				if (cyberIds.contains(in)) {
					cyberFlag = true;
				}
				if (missionIds.contains(in)) {
					missionFlag = true;
					userProvidedMissions.add(in);
				}
				if (!allIds.contains(in)) {
					validInputs = false;
				}
			}
			
			//if valid inpus, add all to correct input
			if(validInputs) {
				correctInput = inputLine;
				if(securityFlag) {					
					if(missionFlag) { //otherwise add only provided mission Ids
						List<String> supportingCyberForUserProvidedMissions = new ArrayList<>();
						for(String missionId : userProvidedMissions) {
							supportingCyberForUserProvidedMissions.addAll(getAllSupportingCyberReqs(model, missionId));
						}
						
						//removing duplicates from cupporting cybers
						List<String> duplicateFreeSupportingCyberForUserProvidedMissions= new ArrayList<>(new HashSet<>(supportingCyberForUserProvidedMissions));
						
						//addiing supportings to input
						for(String cyberId : duplicateFreeSupportingCyberForUserProvidedMissions) {
							correctInput = correctInput + cyberId+ ";";
						}
						
					} else { 
						if(!cyberFlag) {
							System.out.println("Warning: Only safety requirements specified. Will ignore \"Generate Security Cases\" selection."); 								
						}
					}
				}
			} else {
				System.out.println("Warning: Ill formed input. Generating for all mission requirements.");
				//if security is enabled, input will have all mission reqs and all dependent cyber reqs
				if(securityFlag) {
					List<String> supportingCyberForAllMissions = new ArrayList<>();
					for(String missionId : missionIds) {
						supportingCyberForAllMissions.addAll(getAllSupportingCyberReqs(model, missionId));	
						correctInput = correctInput + missionId+ ";";
					}
					
					//removing duplicates from cupporting cybers
					List<String> duplicateFreeSupportingCyberForAllMissions= new ArrayList<>(new HashSet<>(supportingCyberForAllMissions));
					
					//addiing supportings to input
					for(String cyberId : duplicateFreeSupportingCyberForAllMissions) {
						correctInput = correctInput + cyberId+ ";";
					}
				} else { //if security is not enabled, input will have only mission reqs
					for(String missionId : missionIds) {
						correctInput = correctInput + missionId+ ";";					
					}
				}
			}
		}

		return correctInput;
	}
	
	
	/**
	 * Retruns all supporting cyber reqs of a given mission req
	 * @param model
	 * @param missionId
	 * @return
	 */
	public List<String> getAllSupportingCyberReqs(Model model, String missionId) {
		List<String> supCyberReqs= new ArrayList<>();
		
		//get all cyber Ids
        List<String> cyberIds = new ArrayList<>();
        for (verdict.vdm.vdm_model.CyberReq aCyberReq : model.getCyberReq()) {
            cyberIds.add(aCyberReq.getId());
        }
		
		//get the object for that mission
        for (verdict.vdm.vdm_model.Mission aMission : model.getMission()) {
        	if(aMission.getId().equalsIgnoreCase(missionId)) {
        		//get supporting requirements of missionId
        		List<String> supportingReqs = aMission.getCyberReqs();
        		
        		//add only cyberReqs to the return list
        		for(String id : supportingReqs) {
        			if(cyberIds.contains(id)) {
        				supCyberReqs.add(id);
        			}
        		}        		
        	}
        }
		return supCyberReqs;
	}

	
	/**
	 * Calls Aadl2Csv translator
	 * @param dir
	 * @param stemOutputDir
	 * @param soteriaOutputDir
	 */
	public static void runAadl2Csv(File dir, String stemOutputDir, String soteriaOutputDir) {
		Aadl2CsvTranslator aadl2csv = new Aadl2CsvTranslator();
		aadl2csv.execute(dir, stemOutputDir, soteriaOutputDir);
	}

	/**
	 * runs MBAA for both cyber and safety requirements
	 * @param bundleJar
	 * @param dockerImage
	 * @param projectName
	 * @param stemProjectDir
	 * @param soteriaPpBin
	 * @param graphVizPath
	 * @return
	 * @throws IOException
	 */
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
	
	
}
