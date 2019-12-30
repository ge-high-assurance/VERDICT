/* See LICENSE in project directory */
package com.ge.verdict.mbas;

public class App {

    public static void main(String[] args) {
        // Check that we have two arguments
        if (args.length != 2) {
            System.err.println("Usage: java -jar verdict-iml2vdm.jar <input file> <output file>");
        } else {
        	IML2MBASTranslator iml2mbasTranslator = new IML2MBASTranslator();
        	iml2mbasTranslator.translateVerdictToMBAS(args[0], args[1]);
        }
    }
}
