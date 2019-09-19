/* See LICENSE in project directory */
package com.ge.verdict.bundle;

import com.ge.verdict.lustre.VerdictLustreTranslator;
import com.ge.verdict.mbas.VDM2CSV;
import com.ge.verdict.test.instrumentor.VerdictTestInstrumentor;
import com.ge.verdict.vdm.VdmTranslator;
import edu.uiowa.clc.verdict.blm.BlameAssignment;
import edu.uiowa.clc.verdict.crv.Instrumentor;
import edu.uiowa.clc.verdict.lustre.VDM2Lustre;
import edu.uiowa.clc.verdict.util.XMLProcessor;
import edu.uiowa.clc.verdict.vdm.utest.ResourceTest;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import verdict.vdm.vdm_model.Model;

public class App {
    private static class VerdictRunException extends Exception {
        public VerdictRunException(String message) {
            super(message);
        }

        public VerdictRunException(String message, Exception child) {
            super(message, child);
        }
    }

    private static List<String> crvThreats =
            Arrays.asList(new String[] {"LS", "NI", "LB", "IT", "OT", "RI", "SV", "HT"});

    private static Options buildOptions() {
        OptionBuilder.hasArgs(1);
        OptionBuilder.withArgName("AADL input directory");
        OptionBuilder.withLongOpt("aadl");
        Option aadl = OptionBuilder.create();

        OptionBuilder.hasArgs(1);
        OptionBuilder.withArgName("IML input file");
        OptionBuilder.withLongOpt("iml");
        Option iml = OptionBuilder.create();

        OptionGroup inputGroup = new OptionGroup();
        inputGroup.addOption(aadl);
        inputGroup.addOption(iml);
        inputGroup.setRequired(true);

        OptionBuilder.hasArgs(1);
        OptionBuilder.withArgName("STEM project dir");
        OptionBuilder.withDescription("Run MBAS");
        OptionBuilder.withLongOpt("mbas");
        Option mbas = OptionBuilder.create();

        OptionBuilder.hasArgs(1);
        OptionBuilder.withArgName("Kind2 output file (.xml or .json)");
        OptionBuilder.withDescription("Run CRV");
        OptionBuilder.withLongOpt("crv");
        Option crv = OptionBuilder.create();

        OptionGroup group = new OptionGroup();
        group.addOption(mbas);
        group.addOption(crv);
        group.setRequired(true);

        OptionBuilder.hasArgs(1);
        OptionBuilder.withArgName("Debug directory");
        OptionBuilder.withDescription("Produce intermediary debug XML");
        OptionBuilder.withLongOpt("debug");
        Option debug = OptionBuilder.create('d');

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
        helpLine("  --aadl <dir> ...... AADL project input");
        helpLine("      <dir> ......... project directory");
        helpLine();
        helpLine("  --iml <file> ...... IML file input");
        helpLine("      <file> ........ file");
        helpLine();
        helpLine("Toolchain: MBAS (Model Based Architecture & Synthesis)");
        helpLine("           or CRV (Cyber Resiliency Verifier)");
        helpLine("  --mbas <stem_dir>");
        helpLine("      <stem_dir> .... STEM project directory");
        helpLine();
        helpLine("  --crv <out> [-ATG] [-BA [-C]] <threats>");
        helpLine("      <out> ......... CRV output file (.xml or .json)");
        helpLine("      -ATG .......... automatic test-case generation (ATG)");
        helpLine("      -BA ........... blame assignment");
        helpLine("      -C ............ component-level blame assignment (default link-level)");
        helpLine(
                "      <threats> ..... any combination of: [-LS] [-NI] [-LB] [-IT] [-OT] [-RI] [-SV] [-OT]");
        helpLine();
        helpLine("-d, --debug <dir> ... debug output directory");
    }

    private static Binary AADL2IML = new Binary("aadl2iml");
    private static Binary KIND2 = new Binary("kind2");
    private static Binary SOTERIA_PP = new Binary("soteria_pp");

    public static void main(String[] args) throws IOException {
        Options options = buildOptions();

        try {
            CommandLineParser parser = new GnuParser();
            CommandLine opts = parser.parse(options, args);
            handleOpts(opts);
        } catch (ParseException e) {
            printHelp();

            System.exit(1);
        } catch (VerdictRunException e) {
            logError(e.getMessage());
            e.printStackTrace();

            System.exit(2);
        }
    }

    private static void handleOpts(CommandLine opts) throws VerdictRunException {
        String debugDir = opts.hasOption('d') ? opts.getOptionValue('d') : null;

        String aadlPath, imlPath;

        if (opts.hasOption("aadl")) {
            aadlPath = opts.getOptionValue("aadl");
            imlPath =
                    new File(System.getProperty("java.io.tmpdir"), "VERDICT_output.iml")
                            .getAbsolutePath();
        } else if (opts.hasOption("iml")) {
            aadlPath = null;
            imlPath = opts.getOptionValue("iml");
        } else {
            throw new VerdictRunException("Must specifiy either AADL or IML input");
        }

        if (opts.hasOption("mbas")) {
            String[] mbasOpts = opts.getOptionValues("mbas");
            String stemProjectDir = mbasOpts[0];

            runMbas(aadlPath, imlPath, stemProjectDir, debugDir);
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

            String outputPath = opts.getOptionValue("crv");
            String outputBaPath = outputPath.replace(".xml", "").replace(".json", "") + "_ba.xml";

            runCrv(
                    aadlPath,
                    imlPath,
                    instrPath,
                    lustrePath,
                    threats,
                    blameAssignment,
                    componentLevel,
                    atg,
                    outputPath,
                    outputBaPath,
                    debugDir);
        }
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
     * Run an action while concealing System.err. Don't try this at home, kids.
     *
     * @param action
     */
    private static void hideErrorStream(Runnable action) throws VerdictRunException {
        ByteArrayOutputStream store = new ByteArrayOutputStream();

        PrintStream systemErr = System.err;
        System.setErr(new PrintStream(store));

        action.run();

        // TODO: do this when the thing fails so that we still get the error output
        //		System.err.println(new String(store.toByteArray()));

        System.setErr(systemErr);
    }

    private static final List<String> soteria_pngs =
            Arrays.asList(new String[] {"and_gray", "and", "not_gray", "not", "or_gray", "or"});

    private static final List<String> stem_output_csv =
            Arrays.asList(new String[] {"CAPEC", "Defenses"});

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
            String aadlPath, String imlPath, String stemProjectDir, String debugDir)
            throws VerdictRunException {

        String stemCsvDir = (new File(stemProjectDir, "CSVData")).getAbsolutePath();
        String stemOutputDir = (new File(stemProjectDir, "Output")).getAbsolutePath();
        String stemGraphsDir = (new File(stemProjectDir, "Graphs")).getAbsolutePath();
        String soteriaPpOutputDir = (new File(stemOutputDir, "Soteria_Output")).getAbsolutePath();

        checkFile(stemCsvDir, true, true, true, null);
        checkFile(stemOutputDir, true, true, true, null);
        checkFile(stemGraphsDir, true, true, true, null);
        checkFile(soteriaPpOutputDir, true, true, true, null);

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

        if (aadlPath != null) {
            checkFile(aadlPath, true, true, false, null);
            deleteFile(imlPath);
            runAadl2iml(aadlPath, imlPath);
        } else {
            checkFile(imlPath, true, false, false, ".iml");
        }

        logHeader("IML2VDM");

        log("Loading IML into Verdict data model");
        log("Input IML file: " + imlPath);

        Model vdmModel;
        try {
            // Translate the model from IML to VDM
            vdmModel = ResourceTest.setup(imlPath);
        } catch (IOException e) {
            throw new VerdictRunException("Failed to translate IML to VDM", e);
        }

        debugOutVdm(debugDir, "VERDICT_output_debug_vdm.xml", vdmModel);

        logHeader("VDM2CSV");

        log("Converting Verdict data model to CSV");
        log("Outputing STEM files to directory: " + stemCsvDir);
        log("Outputing Soteria++ files to directory: " + stemOutputDir);

        // Generate MBAS inputs
        VDM2CSV vdm2csv = new VDM2CSV();
        vdm2csv.marshalToMbasInputs(vdmModel, imlPath, stemCsvDir, stemOutputDir);

        //        logHeader("STEM");
        //
        //        log("STEM project directory: " + stemProjectDir);
        //        log("STEM output directory: " + stemOutputDir);
        //        log("STEM graphs directory: " + stemGraphsDir);
        //
        //        hideErrorStream(
        //                () -> {
        //                    VerdictStem stemRunner = new VerdictStem();
        //                    stemRunner.runStem(
        //                            new File(stemProjectDir),
        //                            new File(stemOutputDir),
        //                            new File(stemGraphsDir));
        //                });
        //
        //        if (!stem_output_csv.stream()
        //                .allMatch(fname -> (new File(stemOutputDir, fname + ".csv").exists()))) {
        //            throw new VerdictRunException("STEM failed to generate all required files");
        //        }
        //
        //        logHeader("Soteria++");
        //
        //        log("Soteria++ input directory: " + stemOutputDir);
        //        log("Soteria++ output directory: " + soteriaPpOutputDir);
        //
        //        try {
        //            SOTERIA_PP.invoke(
        //                    soteriaPpOutputDir,
        //                    new PumpStreamHandler(),
        //                    "-o",
        //                    soteriaPpOutputDir,
        //                    stemOutputDir);
        //        } catch (Binary.ExecutionException e) {
        //            throw new VerdictRunException("Failed to execute soteria_pp", e);
        //        }
        //
        //        logHeader("Finished");
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
            String imlPath,
            String instrPath,
            String lustrePath,
            List<String> threats,
            boolean blameAssignment,
            boolean componentLevel,
            boolean atg,
            String outputPath,
            String outputBaPath,
            String debugDir)
            throws VerdictRunException {

        checkFile(lustrePath, false, false, true, ".lus");
        checkFile(outputPath, false, false, true, null);

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

        if (aadlPath != null) {
            checkFile(aadlPath, true, true, false, null);
            deleteFile(imlPath);
        } else {
            checkFile(imlPath, true, false, false, ".iml");
        }

        if (outputBaPath != null) {
            deleteFile(outputBaPath);
        }

        if (debugDir != null) {
            logHeader("DEBUGGING XML OUTPUT");
        }

        if (aadlPath != null) {
            runAadl2iml(aadlPath, imlPath);
        }

        log("Loading IML into Verdict data model");
        logHeader("IML2VDM");

        log("IML file: " + imlPath);

        Model vdmModel;
        try {
            // Translate the model from IML to VDM
            vdmModel = ResourceTest.setup(imlPath);
        } catch (IOException e) {
            throw new VerdictRunException("Failed to translate IML to VDM", e);
        }

        debugOutVdm(debugDir, "VERDICT_output_debug_vdm.xml", vdmModel);

        logHeader("VDM Instrumentor");

        Instrumentor instrumentor = null;

        if (!threats.isEmpty()) {
            log("Instrumenting model");

            // Instrument loaded model
            instrumentor = new Instrumentor(vdmModel);
            instrumentor.instrument(vdmModel, threats, blameAssignment, componentLevel);
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
        VDM2Lustre vdm2lus = new VDM2Lustre(vdmModel);
        Model lustreModel = vdm2lus.translate();

        debugOutVdm(debugDir, "VERDICT_output_debug_lus", lustreModel);

        if (atg) {
            logHeader("Verdict ATG");

            log("Verdict Automatic Test-case Generation");
            log("Generating opposite guarantees");

            // Why do we do this to vdmModel and not lustreModel?
            // Good question. I don't know, but it works.
            VerdictTestInstrumentor atgInstrumentor = new VerdictTestInstrumentor(vdmModel);
            atgInstrumentor.instrumentTests();

            debugOutVdm(debugDir, "VERDICT_output_debug_lus_atg", lustreModel);
        }

        logHeader("Output Lustre");

        log("Output Lustre file: " + lustrePath);

        // Output Lustre model
        VerdictLustreTranslator lustreOutputer = new VerdictLustreTranslator();
        lustreOutputer.marshalToLustre(lustreModel, new File(lustrePath));

        logHeader("KIND2");

        log("Running Kind2 model checker");
        log("Output XML file: " + outputPath);

        if (atg) {
            log("Test cases are embedded in XML");
            if (!threats.isEmpty()) {
                log("Counter-examples/test cases may be induced by the presence of threats");
            }
        }

        try {
            ExecuteStreamHandler redirect =
                    new PumpStreamHandler(new FileOutputStream(new File(outputPath)), System.err);
            KIND2.invoke(
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
                BlameAssignment ba = new BlameAssignment();
                ba =
                        ba.compute_blame_assignment(
                                new File(outputPath), instrumentor.getAttackMap(), componentLevel);
                XMLProcessor.dumpXML(ba, new File(outputBaPath));
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

    private static void runAadl2iml(String aadlPath, String imlPath) throws VerdictRunException {
        logHeader("AADL2IML");

        log("Converting AADL to IML");
        log("Input AADL project: " + aadlPath);
        log("Output IML file: " + imlPath);

        try {
            AADL2IML.invoke("-o", imlPath, aadlPath);
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
            String path, boolean exists, boolean dir, boolean write, String extension)
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
