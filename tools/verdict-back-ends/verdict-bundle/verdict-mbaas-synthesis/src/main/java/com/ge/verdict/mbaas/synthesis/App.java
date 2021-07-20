package com.ge.verdict.mbaas.synthesis;

import java.util.function.Supplier;

public class App {

    /**
     * This is just used for testing purposes. Uses CSV files produced by STEM.
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            throw new RuntimeException("Must specify STEM output directory and cost model XML!");
        }

        long startTime = System.currentTimeMillis();

        // Usage:
        String stemOutDir = args[0];
        String costModelXml = args[1];
        boolean inference = arrayContains(args, "--inference");
        boolean useImpledDefenses = arrayContains(args, "--use-implemented-defenses");
        boolean dumpSmtLib = arrayContains(args, "--dump-smtlib");

        if (dumpSmtLib) {
            System.out.println(
                    "Will dump SMT-LIB format to verdict-synthesis-dump.smtlib for debugging");
            System.out.println("Parent directory: " + System.getProperty("user.dir"));
        }

        //        final CostModel costModel =
        //                timed("Load cost model", () -> new CostModel(new File(costModelXml)));

        System.out.println(
                " == Total time: " + (System.currentTimeMillis() - startTime) + " milliseconds");
    }

    public static <T> T timed(String title, Supplier<T> function) {
        long startTime = System.currentTimeMillis();
        T ret = function.get();
        System.out.println(
                " == "
                        + title
                        + ", time: "
                        + (System.currentTimeMillis() - startTime)
                        + " milliseconds");
        return ret;
    }

    private static boolean arrayContains(String[] arr, String search) {
        for (String str : arr) {
            if (str.equals(search)) {
                return true;
            }
        }
        return false;
    }
}
