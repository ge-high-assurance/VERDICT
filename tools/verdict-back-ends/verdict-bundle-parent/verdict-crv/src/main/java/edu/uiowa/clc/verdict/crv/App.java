/* See LICENSE in project directory */
package edu.uiowa.clc.verdict.crv;

import com.ge.verdict.lustre.VerdictLustreTranslator;
import com.ge.verdict.vdm.VdmTranslator;
import edu.uiowa.clc.verdict.blm.BlameAssignment;
import edu.uiowa.clc.verdict.lustre.VDM2Lustre;
import edu.uiowa.clc.verdict.util.Exec;
import edu.uiowa.clc.verdict.util.LOGGY;
import edu.uiowa.clc.verdict.util.XMLProcessor;
import java.io.File;
import java.io.IOException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import verdict.vdm.vdm_model.Model;

// Coordinate All processes.
// 1. Instrumentor
// 2. LustreTranslator
// 3. BlameAssignment
// 4. ResultProcessor

public class App {

    public static void main(String[] args) throws IOException, InterruptedException {
        runCRV(args);
    }

    public static void runCRV(String[] args) throws IOException {
        final String vdmTmpDumpFile = "tmp.xml";
        final String kind2TmpDumpFile = "tmp-kind2-result-dump.xml";

        CommandLine cmdLine = cmdLineOptions(args);

        //        File eg_file = new File("hawkUAV/model_A.xml");
        File vdmFile = null;
        if (cmdLine.hasOption("o")) {
            String inputPath = cmdLine.getOptionValue("i");
            LOGGY.info(inputPath);
            vdmFile = new File(inputPath);
        }

        boolean component_level = false;
        // Setting Blame assingment Level (Component Level & Link Level)
        if (cmdLine.hasOption("C")) {
            component_level = true;
        }

        File lustreFile = null;
        File kind2_resultFile = null;
        File bm_outputFile = null;

        LOGGY.info("************************(VERDICT CRV)******************************");
        // Loadl DataModel
        VerdictLustreTranslator translator = new VerdictLustreTranslator();

        if (vdmFile.canRead()) {

            Model vdm_model = translator.unmarshalFromXml(vdmFile);

            LOGGY.info("**********Instrumentation Invoked****************");

            Instrumentor instrumentor = new Instrumentor(vdm_model);

            vdm_model = instrumentor.instrument(vdm_model, cmdLine);

            {
                VdmTranslator tmp_translator = new VdmTranslator();
                tmp_translator.marshalToXml(vdm_model, new File(vdmTmpDumpFile));
                vdm_model = translator.unmarshalFromXml(new File(vdmTmpDumpFile));
            }

            LOGGY.info("********Dataflow to Lustre code Printing************");

            VDM2Lustre vdm2lus = new VDM2Lustre(vdm_model);
            Model lustreModel = vdm2lus.translate();

            //            LOGGY.info("Done");
            //            VDM2Lustre vdm2Lustre = new VDM2Lustre(vdm_model);
            //            vdm_model = vdm2Lustre.translate();

            if (cmdLine.hasOption("o")) {
                String outputPath = cmdLine.getOptionValue("o");

                lustreFile = new File(outputPath);
                LOGGY.info(lustreFile.getAbsolutePath());
            }

            if (cmdLine.hasOption("r")) {
                String outputFile = cmdLine.getOptionValue("r");
                //                LOGGY.info(outputFile);

                bm_outputFile = new File(outputFile);
            }

            if (cmdLine.hasOption("k")) {
                String outputFile = cmdLine.getOptionValue("k");
                //                LOGGY.info(outputFile);
                kind2_resultFile = new File(outputFile);

                //                if (kind2_resultFile.exists()) {
                //                    kind2_resultFile.createNewFile();
                //                }
            }
            {
                kind2_resultFile = new File(kind2TmpDumpFile);
            }

            VerdictLustreTranslator lustreOutputer = new VerdictLustreTranslator();
            lustreOutputer.marshalToLustre(lustreModel, lustreFile);

            //            translator.marshalToLustre(vdm_model, lustreFile);

            LOGGY.info("*************Executor*******************");

            int exitCode = Exec.run_kind2(lustreFile, kind2_resultFile);

            LOGGY.info("Kind2 Exit Code:" + exitCode);

            if (exitCode == 20) {
                LOGGY.info("No Invalid Property Found.");
            } else if (exitCode == 10) {
                LOGGY.info("Found Invalid Properties.");
            } else if (exitCode == 0) {
                LOGGY.warn("Kind2 TIMED OUT!!!");
            }

            LOGGY.info("*************Blame Assignment*******************");

            BlameAssignment bm = new BlameAssignment();
            bm =
                    bm.compute_blame_assignment(
                            kind2_resultFile, instrumentor.getAttackMap(), component_level);

            XMLProcessor.dumpXML(bm, bm_outputFile);

        } else {
            LOGGY.warn("ERROR Unable to read VDM Model File");
        }

        LOGGY.info("************************(VERDICT CRV)********************************");
    }

    private static CommandLine cmdLineOptions(String[] args) {

        Options options = Instrumentor.createOptions();

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        CommandLine cmdLine = null;

        try {
            cmdLine = parser.parse(options, args);

        } catch (ParseException exp) {

            LOGGY.info("Error:");
            LOGGY.info(exp.getMessage());

            formatter.printHelp("VERDICT-Instrumentor", options);
            System.exit(-1);
        }

        return cmdLine;
    }
}
