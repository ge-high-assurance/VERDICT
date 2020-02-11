/* See LICENSE in project directory */
package com.ge.verdict.bundle;

import com.ge.verdict.lustre.VerdictLustreTranslator;
import com.ge.verdict.mbas.VDM2CSV;
import com.ge.verdict.stem.VerdictStem;
import com.ge.verdict.test.instrumentor.VerdictTestInstrumentor;
import com.ge.verdict.vdm.VdmTranslator;
import edu.uiowa.clc.verdict.blm.BlameAssignment;
import edu.uiowa.clc.verdict.crv.Instrumentor;
import edu.uiowa.clc.verdict.lustre.VDM2Lustre;
import edu.uiowa.clc.verdict.util.XMLProcessor;
import edu.uiowa.clc.verdict.vdm.utest.ResourceTest;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
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
        Option aadl =
                Option.builder()
                        .longOpt("aadl")
                        .numberOfArgs(2)
                        .argName("AADL input directory")
                        .argName("aadl2iml binary")
                        .build();

        Option iml = Option.builder().longOpt("iml").hasArg().argName("IML input file").build();

        OptionGroup inputGroup = new OptionGroup();
        inputGroup.addOption(aadl);
        inputGroup.addOption(iml);
        inputGroup.setRequired(true);

        Option mbas =
                Option.builder()
                        .longOpt("mbas")
                        .numberOfArgs(2)
                        .argName("STEM project dir")
                        .argName("Soteria++ binary")
                        .desc("Run MBAS")
                        .build();

        Option crv =
                Option.builder()
                        .longOpt("crv")
                        .numberOfArgs(2)
                        .argName("Kind2 output file (.xml or .json)")
                        .argName("kind2 binary")
                        .desc("Run CRV")
                        .build();

        OptionGroup group = new OptionGroup();
        group.addOption(mbas);
        group.addOption(crv);
        group.setRequired(true);

        Option debug =
                Option.builder("d")
                        .longOpt("debug")
                        .hasArg()
                        .argName("Produce intermediary debug XML")
                        .build();

        Options options = new Options();
        options.addOptionGroup(inputGroup);
        options.addOptionGroup(group);
        options.addOption(debug);

        for (String opt : crvThreats) {
            options.addOption(opt, false, "");
        }

        options.addOption("BA", false, "Blame Assignment");
        options.addOption("C", false, "Component-level Blame Assignment");
        options.addOption("ATG", false, "Automatic Test-case Generation");

        return options;
    }

    /** @return the name of the JAR file that was run to invoke the Verdict bundle */
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

        helpLine("Usage: %s (--aadl <args> | --iml <args>)", jarName);
        helpLine("       (--mbas <args> | --crv <args>) [-d, --debug <args>]");
        helpLine();
        helpLine("Input: AADL or IML");
        helpLine("  --aadl <dir> <aadl2iml> .. AADL project input");
        helpLine("      <dir> ................ project directory");
        helpLine();
        helpLine("  --iml <file> ............. IML file input");
        helpLine("      <file> ............... file");
        helpLine();
        helpLine("Toolchain: MBAS (Model Based Architecture & Synthesis)");
        helpLine("           or CRV (Cyber Resiliency Verifier)");
        helpLine("  --mbas <stem_dir> <soteria++ bin>");
        helpLine("      <stem_dir> ........... STEM project directory");
        helpLine("      <soteria++ bin> ...... Soteria++ binary");
        helpLine();
        helpLine("  --crv <out> <kind2 bin> [-ATG] [-BA [-C]] <threats>");
        helpLine("      <out> ................ CRV output file (.xml or .json)");
        helpLine("      <kind2 bin> .......... Kind2 binary");

        helpLine("      -ATG ................. automatic test-case generation (ATG)");
        helpLine("      -BA .................. blame assignment");
        helpLine(
                "      -C ................... component-level blame assignment (default link-level)");
        helpLine(
                "      <threats> ............. any combination of: [-LS] [-NI] [-LB] [-IT] [-OT] [-RI] [-SV] [-OT]");
        helpLine();
        helpLine("-d, --debug <dir> .......... debug output directory");
    }

    private static void handleOpts(CommandLine opts) throws VerdictRunException {
        String debugDir = opts.hasOption('d') ? opts.getOptionValue('d') : null;

        String aadlPath, imlPath, aadl2imlBin, modelName;

        if (opts.hasOption("aadl")) {
            String[] aadlOpts = opts.getOptionValues("aadl");
            aadlPath = aadlOpts[0];
            aadl2imlBin = aadlOpts[1];
            imlPath =
                    new File(System.getProperty("java.io.tmpdir"), "VERDICT_output.iml")
                            .getAbsolutePath();
            modelName = new File(aadlPath).getName();
        } else if (opts.hasOption("iml")) {
            aadlPath = null;
            aadl2imlBin = null;
            imlPath = opts.getOptionValue("iml");
            modelName = imlPath;
        } else {
            throw new VerdictRunException("Must specifiy either AADL or IML input");
        }

        Timer.Sample sample = Timer.start(Metrics.globalRegistry);
        if (opts.hasOption("mbas")) {
            String[] mbasOpts = opts.getOptionValues("mbas");
            String stemProjectDir = mbasOpts[0];
            String soteriaPpBin = mbasOpts[1];

            runMbas(aadlPath, aadl2imlBin, imlPath, stemProjectDir, debugDir, soteriaPpBin);
            sample.stop(Metrics.timer("timer.mbas", "model", modelName));
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

            boolean blameAssignment = opts.hasOption("BA");
            boolean componentLevel = opts.hasOption("C");
            boolean atg = opts.hasOption("ATG");

            String[] crvOpts = opts.getOptionValues("crv");
            String outputPath = crvOpts[0];
            String kind2Bin = crvOpts[1];
            String outputBaPath = outputPath.replace(".xml", "").replace(".json", "") + "_ba.xml";

            runCrv(
                    aadlPath,
                    aadl2imlBin,
                    imlPath,
                    instrPath,
                    lustrePath,
                    threats,
                    blameAssignment,
                    componentLevel,
                    atg,
                    outputPath,
                    outputBaPath,
                    debugDir,
                    kind2Bin);
            sample.stop(Metrics.timer("timer.crv", "model", modelName));
        }
    }

    private static void printMetrics() {
        Function<Timer, Integer> visitTimer =
                timer -> {
                    System.out.println(
                            timer.getId() + ": " + timer.totalTime(TimeUnit.SECONDS) + " secs");
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
     * Run MBAS.
     *
     * @param aadlPath AADL input file
     * @param imlPath temporary IML file
     * @param stemDir output directory for STEM input files
     * @param soteriaDir output directory for Soteria++ input files
     * @throws VerdictRunException
     */
    public static void runMbas(
            String aadlPath,
            String aadl2imlBin,
            String imlPath,
            String stemProjectDir,
            String debugDir,
            String soteriaPpBin)
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

        deleteDirectoryContents(stemCsvDir);
        deleteDirectoryContents(stemOutputDir);
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

        String modelName = imlPath;
        if (aadlPath != null) {
            Timer.Sample sample = Timer.start(Metrics.globalRegistry);
            checkFile(aadlPath, true, true, false, false, null);
            checkFile(aadl2imlBin, true, false, false, true, null);
            deleteFile(imlPath);
            runAadl2iml(aadlPath, imlPath, aadl2imlBin);
            modelName = new File(aadlPath).getName();
            sample.stop(Metrics.timer("timer.mbas.aadl2iml", "model", modelName));
        } else {
            checkFile(imlPath, true, false, false, false, ".iml");
        }

        logHeader("IML2VDM");

        log("Loading IML into Verdict data model");
        log("Input IML file: " + imlPath);

        Model vdmModel;
        try {
            // Translate the model from IML to VDM
            Timer.Sample sample = Timer.start(Metrics.globalRegistry);
            vdmModel = ResourceTest.setup(imlPath);
            sample.stop(Metrics.timer("timer.mbas.iml2vdm", "model", modelName));
        } catch (IOException e) {
            throw new VerdictRunException("Failed to translate IML to VDM", e);
        }

        // Try to produce the XML file if debugDir is given
        debugOutVdm(debugDir, "VERDICT_output_debug_vdm.xml", vdmModel);

        logHeader("VDM2CSV");

        log("Converting Verdict data model to CSV");
        log("Outputing STEM files to directory: " + stemCsvDir);
        log("Outputing Soteria++ files to directory: " + stemOutputDir);

        // Generate MBAS inputs
        VDM2CSV vdm2csv = new VDM2CSV();
        Metrics.timer("timer.mbas.vdm2csv", "model", modelName)
                .record(
                        () ->
                                vdm2csv.marshalToMbasInputs(
                                        vdmModel, imlPath, stemCsvDir, stemOutputDir));

        logHeader("STEM");

        log("STEM project directory: " + stemProjectDir);
        log("STEM output directory: " + stemOutputDir);
        log("STEM graphs directory: " + stemGraphsDir);

        //        hideErrorStream(
        //                () -> {
        //
        //                });
        log("STEM is running. Please be patient...");

        VerdictStem stemRunner = new VerdictStem();
        Metrics.timer("timer.mbas.stem", "model", modelName)
                .record(
                        () ->
                                stemRunner.runStem(
                                        new File(stemProjectDir),
                                        new File(stemOutputDir),
                                        new File(stemGraphsDir)));

        //        if (!stem_output_csv.stream()
        //                .allMatch(fname -> (new File(stemOutputDir, fname + ".csv").exists()))) {
        //            throw new VerdictRunException("STEM failed to generate all required files");
        //        }

        log("STEM finished!");

        logHeader("Soteria++");

        log("Soteria++ input directory: " + stemOutputDir);
        log("Soteria++ output directory: " + soteriaPpOutputDir);
        log("Soteria++ is running. Please be patient...");

        try {
            Timer.Sample sample = Timer.start(Metrics.globalRegistry);
            Binary.invokeBin(
                    soteriaPpBin,
                    soteriaPpOutputDir,
                    new PumpStreamHandler(),
                    "-o",
                    soteriaPpOutputDir,
                    stemOutputDir);
            sample.stop(Metrics.timer("timer.mbas.soteria_pp", "model", modelName));
        } catch (Binary.ExecutionException e) {
            throw new VerdictRunException("Failed to execute soteria_pp", e);
        }

        logHeader("Finished");
    }

    /**
     * Run CRV.
     *
     * @param aadlPath AADL input file
     * @param imlPath temporary IML file
     * @param instrPath temporary instrumented model file
     * @param lustrePath temporary Lustre file
     * @param threats list of threats to instrument (LB, NI, etc.)
     * @throws VerdictRunException
     */
    public static void runCrv(
            String aadlPath,
            String aadl2imlBin,
            String imlPath,
            String instrPath,
            String lustrePath,
            List<String> threats,
            boolean blameAssignment,
            boolean componentLevel,
            boolean atg,
            String outputPath,
            String outputBaPath,
            String debugDir,
            String kind2Bin)
            throws VerdictRunException {

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

        deleteFile(instrPath);
        deleteFile(lustrePath);
        deleteFile(outputPath);

        if (debugDir != null) {
            logHeader("DEBUGGING XML OUTPUT");
        }

        String modelName = imlPath;
        if (aadlPath != null) {
            Timer.Sample sample = Timer.start(Metrics.globalRegistry);
            checkFile(aadlPath, true, true, false, false, null);
            checkFile(aadl2imlBin, true, false, false, true, null);
            deleteFile(imlPath);
            runAadl2iml(aadlPath, imlPath, aadl2imlBin);
            modelName = new File(aadlPath).getName();
            sample.stop(Metrics.timer("timer.crv.aadl2iml", "model", modelName));
        } else {
            checkFile(imlPath, true, false, false, false, ".iml");
        }

        if (outputBaPath != null) {
            deleteFile(outputBaPath);
        }

        log("Loading IML into Verdict data model");
        logHeader("IML2VDM");

        log("IML file: " + imlPath);

        Model vdmModel;
        try {
            // Translate the model from IML to VDM
            Timer.Sample sample = Timer.start(Metrics.globalRegistry);
            vdmModel = ResourceTest.setup(imlPath);
            sample.stop(Metrics.timer("timer.crv.iml2vdm", "model", modelName));
        } catch (IOException e) {
            throw new VerdictRunException("Failed to translate IML to VDM", e);
        }

        debugOutVdm(debugDir, "VERDICT_output_debug_vdm.xml", vdmModel);

        logHeader("VDM Instrumentor");

        Instrumentor instrumentor = null;

        if (!threats.isEmpty()) {
            log("Instrumenting model");

            // Instrument loaded model
            Timer.Sample sample = Timer.start(Metrics.globalRegistry);
            instrumentor = new Instrumentor(vdmModel);
            instrumentor.instrument(vdmModel, threats, blameAssignment, componentLevel);
            sample.stop(Metrics.timer("timer.crv.instrumentor", "model", modelName));
        } else {
            log("No threats selected, no instrumentation necessary");
        }

        debugOutVdm(debugDir, "VERDICT_output_debug_instr.xml", vdmModel);

        {
            // For some reason we need to do this...?
            VdmTranslator translator = new VdmTranslator();
            translator.marshalToXml(vdmModel, new File(instrPath));
            vdmModel = translator.unmarshalFromXml(new File(instrPath));
        }

        debugOutVdm(debugDir, "VERDICT_output_debug_instr_reloaded.xml", vdmModel);

        logHeader("VDM2LUS");

        log("Converting instrumented Verdict data model to Lustre");

        // Build Lustre model
        Timer.Sample vdm2lusSample = Timer.start(Metrics.globalRegistry);
        VDM2Lustre vdm2lus = new VDM2Lustre(vdmModel);
        Model lustreModel = vdm2lus.translate();
        vdm2lusSample.stop(Metrics.timer("timer.crv.vdm2lus", "model", modelName));

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
            sample.stop(Metrics.timer("timer.crv.atgInstrumentor", "model", modelName));

            debugOutVdm(debugDir, "VERDICT_output_debug_lus_atg", lustreModel);
        }

        logHeader("Output Lustre");

        log("Output Lustre file: " + lustrePath);

        // Output Lustre model
        Timer.Sample verdictlustreSample = Timer.start(Metrics.globalRegistry);
        VerdictLustreTranslator lustreOutputer = new VerdictLustreTranslator();
        lustreOutputer.marshalToLustre(lustreModel, new File(lustrePath));
        verdictlustreSample.stop(Metrics.timer("timer.crv.verdictlustre", "model", modelName));

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

        Timer.Sample kind2Sample = Timer.start(Metrics.globalRegistry);
        try {
            ExecuteStreamHandler redirect =
                    new PumpStreamHandler(new FileOutputStream(new File(outputPath)), System.err);
            Binary.invokeBin(
                    kind2Bin,
                    null,
                    redirect,
                    outputFormat,
                    lustrePath,
                    "--max_weak_assumptions",
                    Boolean.toString(blameAssignment));
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
            kind2Sample.stop(Metrics.timer("timer.crv.kind2", "model", modelName));
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
                sample.stop(Metrics.timer("timer.crv.blameassignment", "model", modelName));
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
            VdmTranslator translator = new VdmTranslator();
            translator.marshalToXml(vdm, out);
        }
    }

    private static void runAadl2iml(String aadlPath, String imlPath, String aadl2imlBin)
            throws VerdictRunException {
        logHeader("AADL2IML");

        log("Converting AADL to IML");
        log("Input AADL project: " + aadlPath);
        log("Output IML file: " + imlPath);

        try {
            Binary.invokeBin(aadl2imlBin, "-o", imlPath, aadlPath);
        } catch (Binary.ExecutionException e) {
            throw new VerdictRunException("Failed to execute aadl2iml", e);
        }

        // For some reason, aadl2iml doesn't give a non-zero exit code when it fails
        // But we can detect failure like this:
        if (!(new File(imlPath)).exists()) {
            throw new VerdictRunException("Failed to execute aadl2iml, no output generated");
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
}
