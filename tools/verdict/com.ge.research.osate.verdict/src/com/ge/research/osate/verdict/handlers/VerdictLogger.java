package com.ge.research.osate.verdict.handlers;

/**
*
* @author Paul Meng
* Date: Jun 12, 2019
*
*/

/**
 *  Logging class
 * */

public class VerdictLogger {

	public static void info(String msg) {
		if (msg != null) {
			System.out.println("Info: " + msg);
		}
	}

	public static void warning(String msg) {
		if (msg != null) {
			System.out.println("Warning: " + msg);
		}
	}

	public static void severe(String msg) {
		if (msg != null) {
			System.out.println("Error: " + msg);
		}
	}

	public static void printHeader(String toolName, String input) {
		if (toolName != null && input != null) {
			System.out.println(
					"\n************************************************************************************************************************");
			System.out.println("      Invoking " + toolName + " on " + input);
			System.out.println(
					"************************************************************************************************************************\n");
		} else {
			System.out.println(
					"************************************************************************************************************************");
		}
	}

}
