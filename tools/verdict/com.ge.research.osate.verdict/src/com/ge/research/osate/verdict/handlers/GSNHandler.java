package com.ge.research.osate.verdict.handlers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.ui.PlatformUI;
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
						
						//get Mission and cyber requirement Ids
						Aadl2Vdm objectVdmTran = new Aadl2Vdm();
						ListTuple idTuple = getRequirementIds(objectVdmTran.preprocessAadlFiles(projectDir)); 
						
						List<String> allReqIds = idTuple.getAllReqs();
						List<String> cyberIds = idTuple.getCyberReqs();						
						
						//getting input
						String userInput;
						
						//checking user input
						if (AssuranceCaseSettingsPanel.rootGoalId==null) {
							//if security cases have not been selected
							if(!AssuranceCaseSettingsPanel.securityCases) {
								System.out.println("Warning: No user specified requirement. Generating for all mission requirements.");
								userInput = "ALLMREQKEY"; //will be interpreted as All mission requirements by backend								
							} else {
								System.out.println("Warning: No user specified requirement. Generating security cases for all cyber requirements.");
								userInput = "ALLCREQKEY"; //will be interpreted as All cyber requirements by backend	
							}
						} else {
							if (AssuranceCaseSettingsPanel.rootGoalId.trim().length()==0) {
								//if security cases have not been selected
								if(!AssuranceCaseSettingsPanel.securityCases) {
									System.out.println("Warning: No user specified requirement. Generating for all mission requirements.");
									userInput = "ALLMREQKEY"; //will be interpreted as All mission requirements by backend								
								} else {
									System.out.println("Warning: No user specified requirement. Generating security cases for all cyber requirements.");
									userInput = "ALLCREQKEY"; //will be interpreted as All cyber requirements by backend	
								}
							} else {
								
								
								if(!AssuranceCaseSettingsPanel.securityCases) {
									boolean correctInputs = true;
									
									//splitting the input by ';' 
									String inputLine = AssuranceCaseSettingsPanel.rootGoalId.trim(); 
									String[] inputs = inputLine.split(";");
																	
									//checking if input is valid
									for(String in : inputs) {
										if (!allReqIds.contains(in)) {
											correctInputs = false;
										}
									}
									
									if (correctInputs) {
										userInput = inputLine;
									} else {
										System.out.println("Warning: Ill Formed Input. Generating for all mission requirements.");
										userInput = "ALLMREQKEY"; //will be interpreted as All mission requirements by backend
									}															
									
								} else {
									boolean correctInputs = true;
									
									//splitting the input by ';' 
									String inputLine = AssuranceCaseSettingsPanel.rootGoalId.trim(); 
									String[] inputs = inputLine.split(";");
																	
									//checking if input is valid
									for(String in : inputs) {
										if (!cyberIds.contains(in)) {
											correctInputs = false;
										}
									}
									
									if (correctInputs) {
										userInput = inputLine;
									} else {
										System.out.println("Warning: Ill Formed Input. Generating security cases for all cyber requirements.");
										userInput = "ALLCREQKEY"; //will be interpreted as All cyber requirements by backend
									}															
									
								}
								
								
							
							
							}

						}
																			
						
						
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
	
	
	/**
	 * A function that returns a list of all requirement IDs
	 * @param objects
	 * @return
	 */
	public ListTuple getRequirementIds(List<EObject> objects) { 				
		List<String> allReqIds = new ArrayList<>();
		
		List<String> allCyberReqIds = new ArrayList<>();
		
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
					allCyberReqIds.add(aCyberReq.getId());
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
        List<String> duplicateFreeAll = new ArrayList<>(new HashSet<>(allReqIds));
        List<String> duplicateFreeCyber = new ArrayList<>(new HashSet<>(allCyberReqIds));

        ListTuple tupleObj = new ListTuple();
        
        tupleObj.setAllReqs(duplicateFreeAll);
        tupleObj.setCyberReqs(duplicateFreeCyber);
        		
        return tupleObj; 
	}
	
}
