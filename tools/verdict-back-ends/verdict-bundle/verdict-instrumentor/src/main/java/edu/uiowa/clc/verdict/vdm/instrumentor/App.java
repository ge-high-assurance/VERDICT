/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

   @author: M. Fareed Arif
*/

package edu.uiowa.clc.verdict.vdm.instrumentor;

import com.ge.verdict.vdm.VdmTranslator;
import java.io.File;
import java.io.IOException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import verdict.vdm.vdm_model.Model;

public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    // @SuppressWarnings("static-access")
    private static Options createOptions() {

        final Options options = new Options();

        Option input_opt = new Option("i", "VDM Model", true, "Input Model File");
        Option output_opt = new Option("o", "Instrumented Model", true, "Instrumented Model File");

        options.addOption(input_opt);
        options.addOption(output_opt);

        //
        // "Attacks Library:\n "
        // + "1. LS = Location Spoofing \n"
        // + "2. NI = NetWork Injection \n "
        // + "3. LB = Logic Bomb \n"
        // + "4. IT = Insider Threat \n"
        // + "5. OT = Outside User Threat \n"
        // + "6. RI = Remote Code Injection \n"
        // + "7. SV = Software virus/malware/worm/trojan \n"
        // + "8. HT = Hardware Trojans \n"
        // + "9. BG = Benign (Default) \n");

        // Attack Library Options
        Option ls_opt =
                new Option(
                        "LS",
                        "Location Spoofing",
                        false,
                        "Location Spoofing attack Instrumentation");
        Option ni_opt =
                new Option("NI", "Network Injection", false, "Network Injection Instrumentation");
        Option lb_opt = new Option("LB", "Logic Bomb", false, "Logic Bomb Instrumentation");
        Option ht_opt =
                new Option("HT", "Harware Trojan", false, "Harware Trojans Instrumentation");

        Option sv_opt =
                new Option(
                        "SV",
                        "Software Virus/malware/worm/trojan",
                        false,
                        "Software Virus/malware/worm/trojan Instrumentation");

        Option ri_opt =
                new Option(
                        "RI",
                        "Remotet Code Injection",
                        false,
                        "Remotet Code Injection Instrumentation");

        Option ot_opt =
                new Option("OT", "Outsider Threat", false, "Outsider Threat Instrumentation");

        Option it_opt = new Option("IT", "Insider Threat", false, "Insider Threat Instrumentation");

        Option bn_opt = new Option("BN", "Benign", false, "Benign (Default)");

        Option all_opt = new Option("AT", "ALL Threats", false, "Enable all attacks");

        Option bm_opt =
                new Option("B", "Blame Assignment", false, "Blame Assignment (Link Level) Default");

        Option bl_opt =
                new Option(
                        "C",
                        "Blame Assignment (Component)",
                        false,
                        "Blame Assignment (Link Level)");

        options.addOption(ls_opt);
        options.addOption(ni_opt);
        options.addOption(lb_opt);
        options.addOption(ht_opt);
        options.addOption(sv_opt);
        options.addOption(ri_opt);
        options.addOption(ot_opt);
        options.addOption(it_opt);

        options.addOption(all_opt);

        options.addOption(bn_opt);
        options.addOption(bm_opt);
        options.addOption(bl_opt);

        return options;
    }

    /*
    private static String[] enable_bm(String[] array) {
        String[] result = Arrays.copyOf(array, array.length + 1);
        result[array.length] = "-B";
        return result;
        }*/

    public static void main(String[] args) throws IOException {

        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        File inputFile = null;
        File outputFile = null;

        Model verdictDataModel = null;
        VDMInstrumentor threat_instrumentor = null;

        try {
            CommandLine cmdLine = parser.parse(options, args);

            // Load VDM model
            if (cmdLine.hasOption("i")) {
                String inputPath = cmdLine.getOptionValue("i");
                LOGGER.info(inputPath);

                inputFile = new File(inputPath);

                verdictDataModel = VdmTranslator.unmarshalFromXml(inputFile);
                threat_instrumentor = new VDMInstrumentor(verdictDataModel);
            }
            if (cmdLine.hasOption("o")) {
                String outputPath = cmdLine.getOptionValue("o");
                LOGGER.info(outputPath);
                outputFile = new File(outputPath);

                threat_instrumentor.instrument(verdictDataModel, cmdLine);

                // else {
                // LOGGER.info("Benign (Default)");
                // }

                VdmTranslator.marshalToXml(verdictDataModel, outputFile);
            }

        } catch (ParseException exp) {

            LOGGER.error("Error:");
            LOGGER.error(exp.getMessage());

            formatter.printHelp("VERDICT-Instrumentor", options);
            System.exit(-1);
        }
    }
}
