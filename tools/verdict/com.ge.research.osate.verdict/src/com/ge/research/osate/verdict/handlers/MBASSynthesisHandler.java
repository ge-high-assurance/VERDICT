package com.ge.research.osate.verdict.handlers;

import com.ge.research.osate.verdict.aadl2vdm.Agree2Vdm;
import com.ge.research.osate.verdict.gui.BundlePreferences;
import com.ge.research.osate.verdict.gui.MBASSettingsPanel;
import com.ge.research.osate.verdict.gui.MBASSynthesisReport;
import com.ge.research.osate.verdict.vdm2csv.Vdm2Csv;
import com.ge.verdict.vdm.VdmTranslator;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.intro.IIntroPart;
import verdict.vdm.vdm_model.Model;

public class MBASSynthesisHandler extends AbstractHandler {
    static final String SEP = File.separator;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        if (VerdictHandlersUtils.startRun()) {
            // Print on console
            IIntroPart introPart = PlatformUI.getWorkbench().getIntroManager().getIntro();
            PlatformUI.getWorkbench().getIntroManager().closeIntro(introPart);
            final IWorkbenchWindow iWindow = HandlerUtil.getActiveWorkbenchWindow(event);
            VerdictHandlersUtils.setPrintOnConsole("MBAS Output");

            Thread mbasAnalysisThread = new Thread() {
                @Override
                public void run() {
                    try {
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

                        VerdictHandlersUtils.printGreeting();

                        // Create CSVData, Output, Graphs folders if they don't exist
                        // If they exist, delete all the csv and svg files
                        File dataFolder = new File(stemProjPath, "CSVData");
                        File outputFolder = new File(stemProjPath, "Output");
                        File graphsFolder = new File(stemProjPath, "Graphs");

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

                        List<String> selection = VerdictHandlersUtils.getCurrentSelection(event);
                        File projectDir = new File(selection.get(0));
                        File costModel = new File(projectDir, "costModel.xml");
                        if (!costModel.exists()) {
                            System.out.println(
                                    "You must specify costModel.xml in the project directory to use synthesis!");
                            return;
                        }

                        File vdmFile = new File(outputFolder, "verdict_model.xml");
                        runAadl2CsvWithSynthesis(
                                projectDir, vdmFile, dataFolder.getAbsolutePath(), outputFolder.getAbsolutePath());

                        File outputXml = new File(outputFolder, "synthesis_output.xml");
                        if (runBundle(
                                bundleJar,
                                dockerImage,
                                projectDir.getName(),
                                vdmFile.getCanonicalPath(),
                                stemProjPath,
                                soteriaPpBin,
                                graphVizPath,
                                costModel.getCanonicalPath(),
                                outputXml.getCanonicalPath())) {
                            if (outputXml.exists()) {
                                IProject project = VerdictHandlersUtils.getCurrentIProject(event);
                                MBASSynthesisReport.report(project, projectDir, outputXml, iWindow);
                            }
                        }
                    } catch (IOException e) {
                        VerdictLogger.severe(e.toString());
                    } finally {
                        VerdictHandlersUtils.finishRun();
                    }
                }
            };
            mbasAnalysisThread.start();
        }
        return null;
    }

    /**
     * Calls Aadl2Csv translator with synthesis enabled
     * @param dir
     * @param vdmFile
     * @param stemOutputDir
     * @param soteriaOutputDir
     */
    public static void runAadl2CsvWithSynthesis(File dir, File vdmFile, String stemOutputDir, String soteriaOutputDir) {
        Agree2Vdm agree2vdm = new Agree2Vdm();
        Model model = agree2vdm.execute(dir);
        VdmTranslator.marshalToXml(model, vdmFile);
        Vdm2Csv vdm2csv = new Vdm2Csv(true);
        vdm2csv.execute(model, stemOutputDir, soteriaOutputDir, dir.getName());
    }

    public static boolean runBundle(
            String bundleJar,
            String dockerImage,
            String projectName,
            String vdmFile,
            String stemProjectDir,
            String soteriaPpBin,
            String graphVizPath,
            String costModel,
            String outputXml)
            throws IOException {

        VerdictBundleCommand command = new VerdictBundleCommand();
        command.env("GraphVizPath", graphVizPath)
                .jarOrImage(bundleJar, dockerImage)
                .arg("--csv")
                .arg(projectName)
                .arg("--mbas")
                .argBind(stemProjectDir, "/app/STEM")
                .arg2(soteriaPpBin, "/app/soteria_pp")
                .arg("--synthesis")
                .argBind(vdmFile, "/app/vdm/verdict_model.xml")
                .argBind(costModel, "/app/costs/costModel.xml")
                .arg("-o")
                .argBind(outputXml, "/app/output/synthesis_output.xml");

        if (MBASSettingsPanel.synthesisCyberInference) {
            command.arg("-c");
        }
        if (MBASSettingsPanel.synthesisPartialSolution) {
            command.arg("-p");
        }

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
