package com.ge.verdict.main;

import com.ge.verdict.utils.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {

	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("v", false, "Translate AADL + VERDICT annex for MBAA");
		options.addOption("a", false, "Translate AADL + AGREE annex for CRV");
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse(options, args);
			if (cmd.hasOption("v")) {

			}
			if (cmd.hasOption("a")) {

			}
		} catch (ParseException e) {
			StringBuilder sb = new StringBuilder("");
			for (String arg : args) {
				sb.append(arg);
			}
			Logger.error("Unexpected command line arguments: " + sb.toString());
			printHelpMsg(options);
		}

	}

	/**
	 * Print Help
	 */
	static void printHelpMsg(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("VERDICT-Bundle", options);
	}
}
