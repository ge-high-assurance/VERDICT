/* See LICENSE in project directory */
package com.ge.verdict.bundle;

import com.ge.verdict.attackdefensecollector.AttackDefenseCollector;
import com.ge.verdict.attackdefensecollector.CSVFile.MalformedInputException;
import com.ge.verdict.attackdefensecollector.Prob;
import com.ge.verdict.gsn.GSNInterface;
import com.ge.verdict.gsn.SecurityGSNInterface;
import com.ge.verdict.lustre.VerdictLustreTranslator;
import com.ge.verdict.stem.VerdictStem;
import com.ge.verdict.synthesis.CostModel;
import com.ge.verdict.synthesis.DTreeConstructor;
import com.ge.verdict.synthesis.VerdictSynthesis;
import com.ge.verdict.synthesis.dtree.DLeaf;
import com.ge.verdict.synthesis.dtree.DTree;
import com.ge.verdict.test.instrumentor.VerdictTestInstrumentor;
import com.ge.verdict.vdm.VdmTranslator;
import com.ge.verdict.vdm.synthesis.ResultsInstance;
import edu.uiowa.clc.verdict.blm.BlameAssignment;
import edu.uiowa.clc.verdict.crv.Instrumentor;
import edu.uiowa.clc.verdict.lustre.VDM2Lustre;
import edu.uiowa.clc.verdict.merit.MeritAssignment;
import edu.uiowa.clc.verdict.util.XMLProcessor;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.xml.sax.SAXException;
import verdict.vdm.vdm_model.Model;

public class App {
    private static class VerdictRunException extends Exception {
        private static final long serialVersionUID = 6913775561040946636L;

        public VerdictRunException(String message) {
            super(message);
        }

        public VerdictRunException(String message, Exception child) {
            super(message, child);
        }
    }

    private static final List<String> crvThreats =
            Arrays.asList(new String[] {"LS", "NI", "LB", "IT", "OT", "RI", "SV", "HT"});

    private static final List<String> soteria_pngs =
            Arrays.asList(new String[] {"and_gray", "and", "not_gray", "not", "or_gray", "or"});

    public static void main(String[] args) throws IOException {
        // Metrics.addRegistry(new GraphiteMeterRegistry(GraphiteConfig.DEFAULT, Clock.SYSTEM));
        Metrics.addRegistry(new SimpleMeterRegistry(SimpleConfig.DEFAULT, Clock.SYSTEM));
        Options options = buildOptions();

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine opts = parser.parse(options, args);
            handleOpts(opts);
            printMetrics();
        } catch (ParseException e) {
            printHelp();

            System.exit(1);
        } catch (VerdictRunException e) {
            logError(e.getMessage());
            e.printStackTrace();

            System.exit(2);
        }
    }

    private static Options buildOptions() {
        Option csv =
                Option.builder()
                        .desc("CSV input")
                        .longOpt("csv")
                        .hasArg()
                        .argName("Model name")
                        .build();

        Option vdm =
                Option.builder()
                        .desc("VDM input")
                        .longOpt("vdm")
                        .hasArg()
                        .argName("VDM file")
                        .build();

        OptionGroup inputGroup = new OptionGroup();
        inputGroup.addOption(csv);
        inputGroup.addOption(vdm);
        inputGroup.setRequired(true);

        Option mbas =
                Option.builder()
                        .desc("Run MBAS")
                        .longOpt("mbas")
                        .numberOfArgs(2)
                        .argName("STEM project dir")
                        .argName("Soteria++ binary")
                        .build();

        Option crv =
                Option.builder()
                        .desc("Run CRV")
                        .longOpt("crv")
                        .numberOfArgs(2)
                        .argName("Kind2 output file (.xml or .json)")
                        .argName("kind2 binary")
                        .build();

        Option gsn =
                Option.builder()
                        .longOpt("gsn")
                        .numberOfArgs(5)
                        .argName("Root Goal Id")
                        .argName("GSN Output Directory")
                        .argName("Soteria++ Output Directory")
                        .argName("AADL Project Directory")
                        .argName("Host STEM Directory")
                        .build();

        OptionGroup group = new OptionGroup();
        group.addOption(mbas);
        group.addOption(crv);
        group.addOption(gsn);
        group.setRequired(true);

        Option debug =
                Option.builder("d")
                        .desc("Produce debug output")
                        .longOpt("debug")
                        .hasArg()
                        .argName("Intermediary XML output directory")
                        .build();

        Options options = new Options();
        options.addOptionGroup(inputGroup);
        options.addOptionGroup(group);
        options.addOption(debug);

        options.addOption("c", false, "Cyber Relations Inference");
        options.addOption("s", false, "Safety Relations Inference");

        // TODO don't have a good short option because "s" is already taken
        Option synthesis =
                Option.builder("y")
                        .desc("Perform synthesis instead of Soteria++")
                        .longOpt("synthesis")
                        .numberOfArgs(2)
                        .argName("vdm")
                        .argName("costModel")
                        .build();
        options.addOption(synthesis);
        options.addOption("o", "synthesis-output", true, "Synthesis output XML file");
        options.addOption("p", false, "Use partial solutions in synthesis");

        options.addOption("x", false, "Generate XML files for GSN");
        options.addOption("z", false, "Generate GSN Fragments for Security Cases");

        for (String opt : crvThreats) {
            options.addOption(opt, false, "");
        }

        options.addOption("MA", false, "Merit Assignment");
        options.addOption("OI", false, "One IVC");
        options.addOption("LC", false, "All MIVC");
        options.addOption("OC", false, "One MIVC");
        options.addOption("BA", false, "Blame Assignment");
        options.addOption("C", false, "Component-level Blame Assignment");
        options.addOption("G", false, "Global Blame Assignment");
        options.addOption("ATG", false, "Automatic Test-case Generation");

        return options;
    }

    /**
     * @return the name of the JAR file that was run to invoke the Verdict bundle
     */
    private static String getJarName() {
        return new File(App.class.getProtectionDomain().getCodeSource().getLocation().getPath())
                .getName();
    }

    private static void helpLine(String line, Object... args) {
        System.out.println(String.format(Locale.US, line, args));
    }

    private static void helpLine() {
        System.out.println();
    }

    /** Print detailed help message */
    private static void printHelp() {
        String jarName = getJarName();

        helpLine("Usage: %s (--csv <args> | --vdm <args>)", jarName);
        helpLine("       (--mbas <args> | --crv <args>) [-d, --debug <args>]");
        helpLine();
        helpLine("Input: Use CSV or VDM input");
        helpLine("  --csv <model name> ....... CSV input");
        helpLine("      <model name> ......... Model name");
        helpLine();
        helpLine("  --vdm <file> ............. VDM input");
        helpLine("      <file> ............... VDM file");
        helpLine();
        helpLine("Toolchain: MBAS (Model Based Architecture & Synthesis)");
        helpLine("  --mbas <stem dir> <soteria++ bin> [-c] [-s]");
        helpLine("      <stem dir> ........... STEM project directory");
        helpLine("      <soteria++ bin> ...... Soteria++ binary");
        helpLine("      -c ................... cyber relations inference");
        helpLine("      -s ................... safety relations inference");
        helpLine(
                "      --synthesis <vdm file> <cost model xml>"
                        + "                             perform synthesis instead of Soteria++");
        helpLine(
                "       -o ................... synthesis output XML (required if synthesis enabled)");
        helpLine("      -p ................... synthesis partial solutions");
        helpLine();
        helpLine("Toolchain: CRV (Cyber Resiliency Verifier)");
        helpLine("  --crv <out> <kind2 bin> [-ATG] [-MA] [-BA [-C] [-G]] <threats>");
        helpLine("      <out> ................ CRV output file (.xml or .json)");
        helpLine("      <kind2 bin> .......... Kind2 binary");
        helpLine("      -ATG ................. automatic test-case generation (ATG)");
        helpLine("      -MA .................. merit assignment");
        helpLine("      -OI .................. One IVC");
        helpLine("      -LC .................. All MIVC");
        helpLine("      -OC .................. One MIVC");
        helpLine("      -BA .................. blame assignment");
        helpLine(
                "       -C ................... component-level blame assignment (default link-level)");
        helpLine("      -G ................... global blame assignment (default local)");
        helpLine(
                "      <threats> ............. any combination of: [-LS] [-NI] [-LB] [-IT] [-OT] [-RI] [-SV] [-HT]");
        helpLine();
        helpLine("Toolchain: Assurance Case Fragment Generator");
        helpLine(
                "  --gsn <root id> <gsn out dir> <soteria out dir> <aadl project dir> <host STEM dir> [-x] [-z]");
        helpLine("   <root id> ............... the root goal id for the assurance case fragment");
        helpLine(
                "    <gsn out dir> ........... the directory where the gsn fragments should be created");
        helpLine("   <soteria out dir> ....... the directory where Soteria outputs are created");
        helpLine("   <aadl project dir> ...... the directory where the aadl files are present");
        helpLine("   <host STEM dir> ......... the host STEM directory address");
        helpLine("        -x ................. key to determine if xml should be created");
        helpLine(
                "         -z ................. key to determine if security assurance cases should be created");
        helpLine();
        helpLine("-d, --debug <dir> .......... Produce debug output");
        helpLine("      <dir> ................ Intermediary XML output directory");
    }

    private static void handleOpts(CommandLine opts) throws VerdictRunException {
        String debugDir = opts.hasOption('d') ? opts.getOptionValue('d') : null;

        String csvProjectName = null;
        String vdmPath = null;
        if (opts.hasOption("csv")) {
            csvProjectName = opts.getOptionValue("csv");
        } else if (opts.hasOption("vdm")) {
            vdmPath = opts.getOptionValue("vdm");
        } else {
            throw new VerdictRunException("Must specify CSV or VDM input");
        }

        String modelName =
                csvProjectName != null
                        ? csvProjectName
                        : new File(vdmPath).getName().replace(".xml", "");

        Timer.Sample sample = Timer.start(Metrics.globalRegistry);
        if (opts.hasOption("mbas")) {
            String[] mbasOpts = opts.getOptionValues("mbas");
            String stemProjectDir = mbasOpts[0];
            String soteriaPpBin = mbasOpts[1];

            boolean cyberInference = opts.hasOption("c");
            boolean safetyInference = opts.hasOption("s");

            if (opts.hasOption("y")) {
                if (!opts.hasOption("o")) {
                    throw new VerdictRunException("Must specify synthesis output XML");
                }

                String[] synthesisOpts = opts.getOptionValues("y");
                if (synthesisOpts.length != 2) {
                    throw new VerdictRunException("Missing --synthesis args");
                }

                String vdmFile = synthesisOpts[0];
                String costModelPath = synthesisOpts[1];
                String output = opts.getOptionValue("o");
                boolean partialSolution = opts.hasOption("p");

                runMbasSynthesis(
                        vdmFile,
                        modelName,
                        stemProjectDir,
                        debugDir,
                        soteriaPpBin,
                        cyberInference,
                        safetyInference,
                        partialSolution,
                        costModelPath,
                        output);
            } else {
                runMbas(
                        modelName,
                        stemProjectDir,
                        debugDir,
                        soteriaPpBin,
                        cyberInference,
                        safetyInference);
            }
            sample.stop(Metrics.timer("Timer.mbas", "model", modelName));

        } else if (opts.hasOption("crv")) {
            String instrPath =
                    new File(System.getProperty("java.io.tmpdir"), "VERDICT_output_instr.xml")
                            .getAbsolutePath();
            String lustrePath =
                    new File(System.getProperty("java.io.tmpdir"), "VERDICT_output.lus")
                            .getAbsolutePath();

            List<String> threats =
                    crvThreats.stream()
                            .filter(threat -> opts.hasOption(threat))
                            .collect(Collectors.toList());

            boolean meritAssignment = opts.hasOption("MA");
            boolean blameAssignment = opts.hasOption("BA");
            boolean componentLevel = opts.hasOption("C");
            boolean globalOptimization = opts.hasOption("G");
            boolean oneIVC = opts.hasOption("OI");
            boolean allMIVC = opts.hasOption("LC");
            boolean oneMIVC = opts.hasOption("OC");
            boolean atg = opts.hasOption("ATG");

            String[] crvOpts = opts.getOptionValues("crv");
            String outputPath = crvOpts[0];
            String kind2Bin = crvOpts[1];
            String outputBaPath = outputPath.replace(".xml", "").replace(".json", "") + "_ba.xml";

            runCrv(
                    vdmPath,
                    modelName,
                    instrPath,
                    lustrePath,
                    threats,
                    blameAssignment,
                    componentLevel,
                    globalOptimization,
                    atg,
                    meritAssignment,
                    oneIVC,
                    allMIVC,
                    oneMIVC,
                    outputPath,
                    outputBaPath,
                    debugDir,
                    kind2Bin);
            sample.stop(Metrics.timer("Timer.crv", "model", modelName));
        } else if (opts.hasOption("gsn")) {
            String[] gsnOpts = opts.getOptionValues("gsn");
            String rootGoalId = gsnOpts[0];
            String gsnOutputDir = gsnOpts[1];
            String soteriaOutputDir = gsnOpts[2];
            String modelAadlPath = gsnOpts[3];
            String hostSTEMDir = gsnOpts[4];
            boolean generateXml = false;
            boolean securityCases = false;
            if (opts.hasOption("x")) {
                generateXml = true;
            }

            if (opts.hasOption("z")) {
                securityCases = true;
            }

            runGsn(
                    rootGoalId,
                    gsnOutputDir,
                    soteriaOutputDir,
                    modelAadlPath,
                    generateXml,
                    securityCases,
                    modelName,
                    hostSTEMDir);
        }
    }

    private static void printMetrics() {
        Function<Timer, Integer> visitTimer =
                timer -> {
                    System.out.println(
                            timer.getId().getName()
                                    + " for "
                                    + timer.getId().getTag("model")
                                    + ": "
                                    + timer.totalTime(TimeUnit.SECONDS)
                                    + " secs");
                    return 0;
                };
        Metrics.globalRegistry.forEachMeter(
                meter -> meter.match(null, null, visitTimer, null, null, null, null, null, null));
    }

    private static void log(String msg) {
        System.out.println("Info: " + msg);
    }

    private static void logError(String msg) {
        System.err.println("Error: " + msg);
    }

    private static void logLine() {
        System.out.println(
                "******************************************************************"
                        + "******************************************************");
    }

    private static void logHeader(String header) {
        System.out.println();
        logLine();
        System.out.println("      " + header);
        logLine();
        System.out.println();
    }

    /**
     * call the GSN creation interface from verdict-assurance-case Behavior: 1. If security cases
     * have not been enabled - Creates normal GSN for every requirement specified 2. If security
     * cases have been enabled - creates a security GSN for every cyber requirement that is
     * specified - creates a normal GSN for all other requirements
     *
     * @param rootGoalId
     * @param gsnOutputDir
     * @param soteriaOutputDir
     * @param caseAadlPath
     */
    private static void runGsn(
            String inputLine,
            String gsnOutputDir,
            String soteriaOutputDir,
            String modelAadlPath,
            boolean generateXml,
            boolean securityCases,
            String modelName,
            String hostSTEMDir)
            throws VerdictRunException {
        logHeader("GSN");

        // The prefix for SOteria++ text outputs that are linked from solution nodes
        String soteriaOutputLinkPathPrefix = hostSTEMDir + "/Output/Soteria_Output/" + modelName;

        // Fetch the model first
        File modelXml = new File(gsnOutputDir, "modelXML.xml");

        // Fetch the DeliveryDrone model from the XML
        Model model = VdmTranslator.unmarshalFromXml(modelXml);

        // get all cyber Ids
        List<String> cyberIds = new ArrayList<>();
        for (verdict.vdm.vdm_model.CyberReq aCyberReq : model.getCyberReq()) {
            cyberIds.add(aCyberReq.getId());
        }

        // splitting the input by ';'
        String[] inputIds = inputLine.split(";");

        List<String> allIds = new ArrayList<>();

        for (String inputId : inputIds) {
            allIds.add(inputId);
        }

        // remove duplicates
        List<String> duplicateFreeIds = new ArrayList<>(new HashSet<>(allIds));

        for (String id : duplicateFreeIds) {
            // if cyberId
            if (cyberIds.contains(id)) {
                if (securityCases) { // if security is enabled
                    // calling the function to create security GSN artefacts
                    SecurityGSNInterface createGsnObj = new SecurityGSNInterface();

                    try {
                        createGsnObj.runGsnArtifactsGenerator(
                                id,
                                gsnOutputDir,
                                soteriaOutputDir,
                                modelAadlPath,
                                securityCases,
                                generateXml,
                                soteriaOutputLinkPathPrefix,
                                hostSTEMDir);
                    } catch (IOException | ParserConfigurationException | SAXException e) {
                        // TODO Auto-generated catch block
                        throw new VerdictRunException("Failed to create GSN fragments", e);
                    }
                } else {
                    // calling the function to create normal GSN artefacts
                    GSNInterface createGsnObj = new GSNInterface();

                    try {
                        createGsnObj.runGsnArtifactsGenerator(
                                id,
                                gsnOutputDir,
                                soteriaOutputDir,
                                modelAadlPath,
                                generateXml,
                                soteriaOutputLinkPathPrefix,
                                hostSTEMDir);
                    } catch (IOException | ParserConfigurationException | SAXException e) {
                        // TODO Auto-generated catch block
                        throw new VerdictRunException("Failed to create GSN fragments", e);
                    }
                }
            } else { // if not cyberId
                // calling the function to create normal GSN artefacts
                GSNInterface createGsnObj = new GSNInterface();

                try {
                    createGsnObj.runGsnArtifactsGenerator(
                            id,
                            gsnOutputDir,
                            soteriaOutputDir,
                            modelAadlPath,
                            generateXml,
                            soteriaOutputLinkPathPrefix,
                            hostSTEMDir);
                } catch (IOException | ParserConfigurationException | SAXException e) {
                    // TODO Auto-generated catch block
                    throw new VerdictRunException("Failed to create GSN fragments", e);
                }
            }
        }

        // if running inside docker
        if (isRunningInsideDocker()) {
            // sleep for three seconds to allow docker to exit gracefully
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                throw new VerdictRunException(
                        "Failed to create GSN fragments. Thread.sleep exception.", e);
            }
        }

        logHeader("Finished");
    }

    /**
     * Run MBAS with CSV input files
     *
     * @param modelName Name of model
     * @param stemDir output directory for STEM input files
     * @param soteriaDir output directory for Soteria++ input files
     * @throws VerdictRunException
     */
    public static void runMbas(
            String modelName,
            String stemProjectDir,
            String debugDir,
            String soteriaPpBin,
            boolean cyberInference,
            boolean safetyInference)
            throws VerdictRunException {

        String stemCsvDir = (new File(stemProjectDir, "CSVData")).getAbsolutePath();
        String stemOutputDir = (new File(stemProjectDir, "Output")).getAbsolutePath();
        String stemGraphsDir = (new File(stemProjectDir, "Graphs")).getAbsolutePath();
        String stemSadlFile = (new File(stemProjectDir, "Run.sadl")).getAbsolutePath();
        File soteriaOutputDir = new File(stemOutputDir, "Soteria_Output");
        soteriaOutputDir.mkdirs();
        String soteriaPpOutputDir = soteriaOutputDir.getAbsolutePath();

        checkFile(stemCsvDir, true, true, true, false, null);
        checkFile(stemOutputDir, true, true, true, false, null);
        checkFile(stemGraphsDir, true, true, true, false, null);
        checkFile(stemSadlFile, true, false, false, false, null);
        checkFile(soteriaPpOutputDir, true, true, true, false, null);
        checkFile(soteriaPpBin, true, false, false, true, null);

        deleteDirectoryContents(stemGraphsDir);
        deleteDirectoryContents(soteriaPpOutputDir);

        if (debugDir != null) {
            logHeader("DEBUGGING XML OUTPUT");
        }

        try {
            // Copy Soteria++ pngs
            for (String soteria_png : soteria_pngs) {
                Binary.copyResource(
                        "soteria_pngs/" + soteria_png + ".png",
                        new File(soteriaPpOutputDir, soteria_png + ".png"),
                        false);
            }
        } catch (Binary.ExecutionException e) {
            throw new VerdictRunException("Failed to copy Soteria++ pngs", e);
        }

        logHeader("STEM");

        log("STEM project directory: " + stemProjectDir);
        log("STEM output directory: " + stemOutputDir);
        log("STEM graphs directory: " + stemGraphsDir);
        log("STEM is running. Please be patient...");

        VerdictStem stemRunner = new VerdictStem();
        Metrics.timer("Timer.mbas.stem", "model", modelName)
                .record(
                        () ->
                                stemRunner.runStem(
                                        new File(stemProjectDir),
                                        new File(stemOutputDir),
                                        new File(stemGraphsDir)));

        log("STEM finished!");

        logHeader("Soteria++");

        log("Soteria++ input directory: " + stemOutputDir);
        log("Soteria++ output directory: " + soteriaPpOutputDir);
        log("Soteria++ is running. Please be patient...");

        // Soteria has optional arguments, so need to add all args to this list
        List<String> args = new ArrayList<>();
        args.add("-o");
        args.add(soteriaPpOutputDir);
        args.add(stemOutputDir);
        if (cyberInference) {
            args.add("-c");
        }
        if (safetyInference) {
            args.add("-s");
        }

        try {
            Timer.Sample sample = Timer.start(Metrics.globalRegistry);
            Binary.invokeBin(
                    soteriaPpBin,
                    soteriaPpOutputDir,
                    new PumpStreamHandler(),
                    args.toArray(new String[args.size()]));
            sample.stop(Metrics.timer("Timer.mbas.soteria_pp", "model", modelName));
        } catch (Binary.ExecutionException e) {
            throw new VerdictRunException("Failed to execute soteria_pp", e);
        }

        logHeader("Finished");
    }

    /**
     * Run MBAS Synthesis with CSV input files
     *
     * @param vdmPath VDM input file for synthesis
     * @param modelName Name of model
     * @param stemDir output directory for STEM input files
     * @param soteriaDir output directory for Soteria++ input files
     * @throws VerdictRunException
     */
    public static void runMbasSynthesis(
            String vdmPath,
            String modelName,
            String stemProjectDir,
            String debugDir,
            String soteriaPpBin,
            boolean cyberInference,
            boolean safetyInference,
            boolean partialSolution,
            String costModelPath,
            String outputPath)
            throws VerdictRunException {

        String stemCsvDir = (new File(stemProjectDir, "CSVData")).getAbsolutePath();
        String stemOutputDir = (new File(stemProjectDir, "Output")).getAbsolutePath();
        String stemGraphsDir = (new File(stemProjectDir, "Graphs")).getAbsolutePath();
        String stemSadlFile = (new File(stemProjectDir, "Run.sadl")).getAbsolutePath();
        File soteriaOutputDir = new File(stemOutputDir, "Soteria_Output");
        soteriaOutputDir.mkdirs();
        String soteriaPpOutputDir = soteriaOutputDir.getAbsolutePath();

        checkFile(stemCsvDir, true, true, true, false, null);
        checkFile(stemOutputDir, true, true, true, false, null);
        checkFile(stemGraphsDir, true, true, true, false, null);
        checkFile(stemSadlFile, true, false, false, false, null);
        checkFile(soteriaPpOutputDir, true, true, true, false, null);
        checkFile(soteriaPpBin, true, false, false, true, null);

        checkFile(vdmPath, true, false, false, false, "xml");
        checkFile(costModelPath, true, false, false, false, "xml");
        checkFile(outputPath, false, false, true, false, "xml");

        deleteDirectoryContents(stemGraphsDir);
        deleteDirectoryContents(soteriaPpOutputDir);

        if (debugDir != null) {
            logHeader("DEBUGGING XML OUTPUT");
        }

        try {
            // Copy Soteria++ pngs
            for (String soteria_png : soteria_pngs) {
                Binary.copyResource(
                        "soteria_pngs/" + soteria_png + ".png",
                        new File(soteriaPpOutputDir, soteria_png + ".png"),
                        false);
            }
        } catch (Binary.ExecutionException e) {
            throw new VerdictRunException("Failed to copy Soteria++ pngs", e);
        }

        logHeader("STEM");

        log("STEM project directory: " + stemProjectDir);
        log("STEM output directory: " + stemOutputDir);
        log("STEM graphs directory: " + stemGraphsDir);
        log("STEM is running. Please be patient...");

        VerdictStem stemRunner = new VerdictStem();
        Metrics.timer("Timer.mbas.stem", "model", modelName)
                .record(
                        () ->
                                stemRunner.runStem(
                                        new File(stemProjectDir),
                                        new File(stemOutputDir),
                                        new File(stemGraphsDir)));

        log("STEM finished!");

        logHeader("Synthesis");

        try {
            Timer.Sample sample = Timer.start(Metrics.globalRegistry);

            CostModel costModel = new CostModel(new File(costModelPath));

            AttackDefenseCollector collector =
                    new AttackDefenseCollector(
                            new File(vdmPath), new File(stemOutputDir), cyberInference);
            List<AttackDefenseCollector.Result> results = collector.perform();

            boolean sat =
                    results.stream()
                            .allMatch(
                                    result -> Prob.lte(result.prob, result.cyberReq.getSeverity()));
            boolean performMeritAssignment = partialSolution && sat;

            DLeaf.Factory factory = new DLeaf.Factory();
            DTree dtree =
                    DTreeConstructor.construct(
                            results, costModel, partialSolution, performMeritAssignment, factory);
            Optional<ResultsInstance> selected =
                    VerdictSynthesis.performSynthesisMultiple(
                            dtree,
                            factory,
                            costModel,
                            partialSolution,
                            sat,
                            performMeritAssignment,
                            false);

            if (selected.isPresent()) {
                if (performMeritAssignment) {
                    ResultsInstance withExtraDefProps =
                            VerdictSynthesis.addExtraImplDefenses(
                                    selected.get(), collector.getImplDal(), costModel);
                    withExtraDefProps.toFileXml(new File(outputPath));
                } else {
                    selected.get().toFileXml(new File(outputPath));
                }
                log("Synthesis results output to " + outputPath);
            } else {
                logError("Synthesis failed");
            }

            sample.stop(Metrics.timer("Timer.mbas.synthesis", "model", modelName));
        } catch (IOException | MalformedInputException e) {
            throw new VerdictRunException("Failed to execute synthesis", e);
        }

        logHeader("Finished");
    }

    /**
     * Run CRV.
     *
     * @param vdmPath VDM input file
     * @param modelName Name of model
     * @param instrPath temporary instrumented model file
     * @param lustrePath temporary Lustre file
     * @param threats list of threats to instrument (LB, NI, etc.)
     * @throws VerdictRunException
     */
    public static void runCrv(
            String vdmPath,
            String modelName,
            String instrPath,
            String lustrePath,
            List<String> threats,
            boolean blameAssignment,
            boolean componentLevel,
            boolean globalOptimization,
            boolean atg,
            boolean meritAssignment,
            boolean oneIVC,
            boolean allMIVC,
            boolean oneMIVC,
            String outputPath,
            String outputBaPath,
            String debugDir,
            String kind2Bin)
            throws VerdictRunException {

        checkFile(vdmPath, true, false, false, false, ".xml");
        checkFile(lustrePath, false, false, true, false, ".lus");
        checkFile(outputPath, false, false, true, false, null);

        String outputFormat;

        if (outputPath.endsWith(".xml")) {
            outputFormat = "-xml";
        } else if (outputPath.endsWith(".json")) {
            outputFormat = "-json";
        } else {
            throw new VerdictRunException("Output file must be .xml or .json");
        }

        if (debugDir != null) {
            logHeader("DEBUGGING XML OUTPUT");
        }

        deleteFile(instrPath);
        deleteFile(lustrePath);
        deleteFile(outputPath);
        if (outputBaPath != null) {
            deleteFile(outputBaPath);
        }

        File vdmFile = new File(vdmPath);
        Model vdmModel = VdmTranslator.unmarshalFromXml(vdmFile);

        logHeader("VDM Instrumentor");

        Instrumentor instrumentor = null;

        if (!threats.isEmpty()) {
            log("Instrumenting model");

            // Instrument loaded model
            Timer.Sample sample = Timer.start(Metrics.globalRegistry);
            instrumentor = new Instrumentor(vdmModel);
            instrumentor.instrument(vdmModel, threats, blameAssignment, componentLevel);
            sample.stop(Metrics.timer("Timer.crv.instrumentor", "model", modelName));
        } else {
            log("No threats selected, no instrumentation necessary");
        }

        debugOutVdm(debugDir, "VERDICT_output_debug_instr.xml", vdmModel);

        {
            // For some reason we need to do this...?
            VdmTranslator.marshalToXml(vdmModel, new File(instrPath));
            vdmModel = VdmTranslator.unmarshalFromXml(new File(instrPath));
        }

        debugOutVdm(debugDir, "VERDICT_output_debug_instr_reloaded.xml", vdmModel);

        logHeader("VDM2LUS");

        log("Converting instrumented Verdict data model to Lustre");

        // Build Lustre model
        Timer.Sample vdm2lusSample = Timer.start(Metrics.globalRegistry);
        VDM2Lustre vdm2lus = new VDM2Lustre(vdmModel);
        Model lustreModel = vdm2lus.translate();
        vdm2lusSample.stop(Metrics.timer("Timer.crv.vdm2lus", "model", modelName));

        debugOutVdm(debugDir, "VERDICT_output_debug_lus", lustreModel);

        if (atg) {
            logHeader("Verdict ATG");

            log("Verdict Automatic Test-case Generation");
            log("Generating opposite guarantees");

            // Why do we do this to vdmModel and not lustreModel?
            // Good question. I don't know, but it works.
            Timer.Sample sample = Timer.start(Metrics.globalRegistry);
            VerdictTestInstrumentor atgInstrumentor = new VerdictTestInstrumentor(vdmModel);
            atgInstrumentor.instrumentTests();
            sample.stop(Metrics.timer("Timer.crv.atgInstrumentor", "model", modelName));

            debugOutVdm(debugDir, "VERDICT_output_debug_lus_atg", lustreModel);
        }

        logHeader("Output Lustre");

        log("Output Lustre file: " + lustrePath);

        // Output Lustre model
        Timer.Sample verdictlustreSample = Timer.start(Metrics.globalRegistry);
        VerdictLustreTranslator.marshalToLustre(lustreModel, new File(lustrePath));
        verdictlustreSample.stop(Metrics.timer("Timer.crv.verdictlustre", "model", modelName));

        logHeader("Kind2");

        log("Running Kind2 model checker");
        log("Output XML file: " + outputPath);
        log("Kind2 is running. Please be patient...");

        if (atg) {
            log("Test cases are embedded in XML");
            if (!threats.isEmpty()) {
                log("Counter-examples/test cases may be induced by the presence of threats");
            }
        }

        boolean mustSet = meritAssignment && (oneMIVC || allMIVC);

        Timer.Sample kind2Sample = Timer.start(Metrics.globalRegistry);
        try {
            ExecuteStreamHandler redirect =
                    new PumpStreamHandler(new FileOutputStream(new File(outputPath)), System.err);
            if (blameAssignment
                    && instrumentor != null
                    && instrumentor.emptyIntrumentation() == false) {
                Binary.invokeBin(
                        kind2Bin,
                        null,
                        redirect,
                        outputFormat,
                        lustrePath,
                        "--check_subproperties",
                        "false",
                        "--enable",
                        "MCS",
                        "--print_mcs_legacy",
                        "true",
                        "--mcs_approximate",
                        Boolean.toString(!globalOptimization));
            } else {
                Binary.invokeBin(
                        kind2Bin,
                        null,
                        redirect,
                        outputFormat,
                        lustrePath,
                        "--check_subproperties",
                        "false",
                        "--ivc",
                        Boolean.toString(meritAssignment),
                        "--ivc_approximate",
                        Boolean.toString(oneIVC),
                        "--ivc_all",
                        Boolean.toString(allMIVC),
                        "--ivc_category",
                        "contracts",
                        "--ivc_must_set",
                        Boolean.toString(mustSet));
            }
        } catch (Binary.ExecutionException e) {
            // Kind2 does some weird things with exit codes
            if (e.getCode().isPresent()) {
                switch (e.getCode().get()) {
                    case 20:
                        // Success
                        log("All properties are valid");
                        break;
                    case 10:
                        if (atg) {
                            // Some properties invalid, but those might just be the ATG negative
                            // properties
                            log("Kind2 finished");
                        } else {
                            // Some properties invalid
                            log("Some properties are invalid");
                        }
                        break;
                    case 2:
                        log("Kind2 terminated with an error");
                        XMLProcessor.parseLog(new File(outputPath));
                        // Terminate the process?
                        break;
                    case 0:
                        log("Kind2 timed out");
                        break;
                    default:
                        throw new VerdictRunException("Failed to execute kind2", e);
                }
            } else {
                throw new VerdictRunException("Failed to execute kind2", e);
            }
        } catch (IOException e) {
            throw new VerdictRunException("Failed to execute kind2", e);
        } finally {
            kind2Sample.stop(Metrics.timer("Timer.crv.kind2", "model", modelName));
        }

        if (meritAssignment) {
            logHeader("Merit Assignment");

            MeritAssignment ma = new MeritAssignment(new File(outputPath));
            ma.readAndPrintInfo();
        }

        if (blameAssignment && instrumentor != null) {
            logHeader("Blame Assignment");

            // TODO Perform blame assignment post-analysis
            // I already passed the correct parameter to Kind2 above
            // Kind2 XML/JSON is in outputPath (it's probably OK to ignore JSON for now)
            // BA output XML should go to outputBaPath

            // VDM instrumentor instance is vdmInstrumentor
            // If it is null then there were no threat models selected for instrumentations

            log("Blame assignment output: " + outputBaPath);

            try {
                Timer.Sample sample = Timer.start(Metrics.globalRegistry);
                BlameAssignment ba = new BlameAssignment();
                ba =
                        ba.compute_blame_assignment(
                                new File(outputPath), instrumentor.getAttackMap(), componentLevel);
                XMLProcessor.dumpXML(ba, new File(outputBaPath));
                sample.stop(Metrics.timer("Timer.crv.blameassignment", "model", modelName));
            } catch (FileNotFoundException e) {
                throw new VerdictRunException("Failed to perform blame assignment", e);
            }
        }

        logHeader("Finished");
    }

    private static void debugOutVdm(String debugDir, String name, Model vdm) {
        if (debugDir != null) {
            File out = new File(debugDir, name);
            log("Debug output: " + out.getAbsolutePath());
            VdmTranslator.marshalToXml(vdm, out);
        }
    }

    /**
     * This doesn't check everything, but it checks a couple of things.
     *
     * @param path the path to the file
     * @param exists true if the file denoted by path should exist
     * @param dir true if the file denoted by path should be a directory
     * @param write true if the file denoted by path, or its parent if it need not exist, should be
     *     writable
     * @param extension the extension that path should have, or null
     * @throws VerdictRunException with the error message upon failing to meet one of the above
     *     assertions
     */
    private static void checkFile(
            String path,
            boolean exists,
            boolean dir,
            boolean write,
            boolean execute,
            String extension)
            throws VerdictRunException {

        // If the file need not exist, then at least its parent should exist
        File existingFile = exists ? new File(path) : (new File(path)).getParentFile();
        String fileOrDir = dir || !exists ? "Directory" : "File";

        if (!existingFile.exists()) {
            throw new VerdictRunException(fileOrDir + " does not exist: " + path);
        }

        if (exists && dir && !existingFile.isDirectory()) {
            throw new VerdictRunException("File is not a directory: " + path);
        } else if (exists && !dir && !existingFile.isFile()) {
            throw new VerdictRunException("File is a directory (not a file): " + path);
        }

        if (!existingFile.canRead()) {
            throw new VerdictRunException(fileOrDir + " is not readable: " + path);
        }

        if (write && !existingFile.canWrite()) {
            throw new VerdictRunException(fileOrDir + " is not writable: " + path);
        }

        if (execute && !existingFile.canExecute()) {
            throw new VerdictRunException(fileOrDir + " is not executable: " + path);
        }

        if (extension != null && !path.endsWith(extension)) {
            throw new VerdictRunException(
                    "File is missing extension \"" + extension + "\": " + path);
        }
    }

    /**
     * Delete a file if it exists.
     *
     * @param path
     */
    private static void deleteFile(String path) {
        File file = new File(path);
        if (file.exists() && !file.delete()) {
            logError("Failed to delete: " + path);
        }
    }

    /**
     * Delete all files in a directory (non-recursive).
     *
     * @param dirPath
     */
    private static void deleteDirectoryContents(
            String dirPath, Function<File, Boolean> deleteFunction) {
        File file = new File(dirPath);
        if (file.exists() && file.isDirectory()) {
            for (File f : file.listFiles()) {
                if (f.isFile() && deleteFunction.apply(f)) {
                    deleteFile(f.getAbsolutePath());
                }
            }
        } else {
            logError("Cannot delete contents of directory: " + dirPath);
        }
    }

    /**
     * Delete all files in a directory (non-recursive).
     *
     * @param dirPath
     */
    private static void deleteDirectoryContents(String dirPath) {
        deleteDirectoryContents(dirPath, file -> true);
    }

    /**
     * Checks if running inside docker
     *
     * @return
     */
    public static Boolean isRunningInsideDocker() {
        try (Stream<String> stream = Files.lines(Paths.get("/proc/1/cgroup"))) {
            return stream.anyMatch(line -> line.contains("/docker"));
        } catch (IOException e) {
            return false;
        }
    }
}
