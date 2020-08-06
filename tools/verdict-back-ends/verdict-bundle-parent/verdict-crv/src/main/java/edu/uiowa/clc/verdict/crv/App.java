/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

   @author: M. Fareed Arif
*/

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
import java.util.ArrayList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import verdict.vdm.vdm_model.Model;

// Coordinate All processes.
// 0. IML2VDM - Disabled.
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

        // File eg_file = new File("hawkUAV/model_A.xml");
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

        boolean meritAssignment = false;
        if (cmdLine.hasOption("M")) {
            meritAssignment = true;
        }

        File lustreFile = null;
        File kind2_resultFile = null;
        File bm_outputFile = null;

        LOGGY.info("************************(VERDICT CRV)******************************");

        // Load DataModel

        if (vdmFile.canRead()) {
            String InputFile = vdmFile.getAbsolutePath();

            String fileExt = InputFile.substring(InputFile.lastIndexOf(".") + 1);
            Model vdm_model = null;

            if (fileExt.equals("iml")) {
                // Use IML model
                // vdm_model = ResourceTest.setup(InputFile);

                // Store VDM in a temporary file
                //                VerdictLustreTranslator.marshalToXml(vdm_model, new File(InputFile
                // + ".xml"));
            } else if (fileExt.equals("xml")) {
                // Use VDM model
                vdm_model = VerdictLustreTranslator.unmarshalFromXml(vdmFile);
            } else {

                LOGGY.warn("Invalid Model Input File: " + fileExt);
                System.exit(-1);
            }
            LOGGY.info("**********Instrumentation Invoked****************");

            Instrumentor instrumentor = new Instrumentor(vdm_model);

            vdm_model = instrumentor.instrument(vdm_model, cmdLine);

            {
                VdmTranslator.marshalToXml(vdm_model, new File(vdmTmpDumpFile));
                vdm_model = VerdictLustreTranslator.unmarshalFromXml(new File(vdmTmpDumpFile));
            }

            LOGGY.info("********Dataflow to Lustre code Printing*********");

            VDM2Lustre vdm2lus = new VDM2Lustre(vdm_model);
            Model lustreModel = vdm2lus.translate();

            // LOGGY.info("Done");
            // VDM2Lustre vdm2Lustre = new VDM2Lustre(vdm_model);
            // vdm_model = vdm2Lustre.translate();

            if (cmdLine.hasOption("o")) {
                String outputPath = cmdLine.getOptionValue("o");

                lustreFile = new File(outputPath);
                LOGGY.info(lustreFile.getAbsolutePath());
            }

            if (cmdLine.hasOption("r")) {
                String outputFile = cmdLine.getOptionValue("r");
                // LOGGY.info(outputFile);

                bm_outputFile = new File(outputFile);
            }

            if (cmdLine.hasOption("k")) {
                String outputFile = cmdLine.getOptionValue("k");
                // LOGGY.info(outputFile);
                kind2_resultFile = new File(outputFile);

                // if (kind2_resultFile.exists()) {
                // kind2_resultFile.createNewFile();
                // }
            }
            {
                kind2_resultFile = new File(kind2TmpDumpFile);
            }

            // VerdictLustreTranslator.marshalToLustre(lustreModel, lustreFile);

            VDMLustreTranslator.dumpLustre(lustreModel, lustreFile);

            // VerdictLustreTranslator.marshalToLustre(vdm_model, lustreFile);

            LOGGY.info("******************Executor***********************");

            int exitCode =
                    Exec.run_kind2(
                            lustreFile,
                            kind2_resultFile,
                            instrumentor.emptyIntrumentation(),
                            meritAssignment);

            //            LOGGY.info("Kind2 Exit Code:" + exitCode);

            if (exitCode == 20) {
                LOGGY.info("No Invalid Property Found.");
            } else if (exitCode == 10) {
                LOGGY.info("Found Invalid Properties.");
            } else if (exitCode == 0) {
                LOGGY.warn("Kind2 TIMED OUT!!!");
            } else if (exitCode == 2) {
                LOGGY.warn("Kind2 Failure, Log messages:");
                XMLProcessor.parseLog(kind2_resultFile);
            }

            if (meritAssignment) {
                LOGGY.info("*************Merit Assignment***********");

                MeritAssignmentResult.readAndPrintInfo(kind2_resultFile);

            } else {
                LOGGY.info("*************Blame Assignment***********");

                BlameAssignment bm = new BlameAssignment();
                bm =
                        bm.compute_blame_assignment(
                                kind2_resultFile, instrumentor.getAttackMap(), component_level);

                XMLProcessor.dumpXML(bm, bm_outputFile);
            }
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

        int args_attacks_count = 0;

        try {
            cmdLine = parser.parse(options, args);

            if (cmdLine.hasOption("AT")) {
                ArrayList<String> cmd_args = new ArrayList<String>();

                for (int i = 0; i < args.length; i++) {
                    cmd_args.add(args[i]);
                    //                    System.out.println("User Provided Arguments: " + args[i]);
                }

                String[] cmd_attacks = {"-LS", "-LB", "-NI", "-SV", "-RI", "-OT", "-IT", "-HT"};

                for (int i = 0; i < cmd_attacks.length; i++) {
                    String atk = cmd_attacks[i];
                    if (cmd_args.contains(atk) == false) {
                        cmd_args.add(atk);
                        //                        System.out.println("Added additional Arguments: "
                        // + atk);
                    } else {
                        args_attacks_count++;
                    }
                }

                int size = args.length + (cmd_attacks.length - args_attacks_count);

                args = cmd_args.toArray(new String[size]);
                cmdLine = parser.parse(options, args);
            }
        } catch (ParseException exp) {

            LOGGY.info("Invalid cmd arguments: " + exp.getMessage());

            formatter.printHelp("VERDICT-Instrumentor", options);
            System.exit(-1);
        }

        return cmdLine;
    }
}
