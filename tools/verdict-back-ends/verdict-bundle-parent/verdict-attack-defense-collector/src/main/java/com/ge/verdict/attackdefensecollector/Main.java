package com.ge.verdict.attackdefensecollector;

import com.ge.verdict.attackdefensecollector.AttackDefenseCollector.Result;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, CSVFile.MalformedInputException {
        long start = System.currentTimeMillis();
        // this argument parsing is pretty bad. but we shouldn't be using this anyway.
        boolean inference = arrayContains(args, "--inference");
        AttackDefenseCollector attackDefenseCollector;
        if (arrayContains(args, "--vdm")) {
            attackDefenseCollector =
                    new AttackDefenseCollector(new File(args[1]), new File(args[0]), inference);
        } else {
            attackDefenseCollector = new AttackDefenseCollector(args[0], inference);
        }
        List<Result> results = attackDefenseCollector.perform();
        for (Result result : results) {
            Logger.println();
            // The default printer includes indentation for clean-ness
            Logger.println(result.adtree);
            Logger.println();
            Logger.println("CyberReq: " + result.cyberReq.getName());
            Logger.println("Mission: " + result.cyberReq.getMission());
            Logger.println("Severity: " + result.cyberReq.getSeverity());
            Logger.println("Computed: " + result.prob);
            Logger.println(
                    "Successful: "
                            + (Prob.lte(result.prob, result.cyberReq.getSeverity())
                                    ? "Yes"
                                    : "No"));
        }
        Logger.println("Total time: " + (System.currentTimeMillis() - start) + " milliseconds");
    }

    private static boolean arrayContains(String[] arr, String val) {
        for (String elem : arr) {
            if (elem.equals(val)) {
                return true;
            }
        }
        return false;
    }
}
