package com.ge.verdict.attackdefensecollector;

import com.ge.verdict.attackdefensecollector.AttackDefenseCollector.Result;
import com.ge.verdict.attackdefensecollector.adtree.ADTree;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    private static final String USAGE =
            "java -jar {attack-defense-collector.jar} --vdm {STEM output dir} {vdm file} [--inference] [--cut-set]";

    public static void main(String[] args) throws IOException, CSVFile.MalformedInputException {
        if (arrayContains(args, "--help")) {
            System.out.println("Usage: " + USAGE);
            return;
        }

        long start = System.currentTimeMillis();
        // this argument parsing is pretty bad. but we shouldn't be using this anyway.
        boolean inference = arrayContains(args, "--inference");
        boolean cutSet = arrayContains(args, "--cut-set");
        AttackDefenseCollector attackDefenseCollector;

        // If "--vdm" is present, we will use the vdm file;
        // otherwise, we will load csv files
        if (arrayContains(args, "--vdm")) {
            attackDefenseCollector =
                    new AttackDefenseCollector(new File(args[1]), new File(args[0]), inference);
        } else {
            attackDefenseCollector = new AttackDefenseCollector(args[0], inference);
        }
        List<Result> results = attackDefenseCollector.perform();
        for (Result result : results) {
            // Convert adtree to cutset only if --cut-set is on.
            ADTree adtree = cutSet ? CutSetGenerator.generate(result.adtree) : result.adtree;

            Logger.println();
            // The default printer includes indentation for clean-ness
            Logger.println(adtree);
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
