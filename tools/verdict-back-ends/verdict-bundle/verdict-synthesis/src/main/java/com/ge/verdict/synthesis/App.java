package com.ge.verdict.synthesis;

import com.ge.verdict.attackdefensecollector.AttackDefenseCollector;
import com.ge.verdict.attackdefensecollector.AttackDefenseCollector.Result;
import com.ge.verdict.attackdefensecollector.CSVFile.MalformedInputException;
import com.ge.verdict.synthesis.dtree.DLeaf;
import com.ge.verdict.synthesis.dtree.DTree;
import com.ge.verdict.synthesis.impl.MonotonicCostModelTree;
import com.ge.verdict.vdm.synthesis.ResultsInstance;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class App {
    /**
     * This is just used for testing purposes. Uses CSV, not VDM (so out of date). See below for
     * invocation.
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
        boolean meritAssignment = arrayContains(args, "--merit-assignment");
        boolean partialSolution = arrayContains(args, "--partial-solution") || meritAssignment;
        boolean dumpSmtLib = arrayContains(args, "--dump-smtlib");

        if (dumpSmtLib) {
            System.out.println(
                    "Will dump SMT-LIB format to verdict-synthesis-dump.smtlib for debugging");
            System.out.println("Parent directory: " + System.getProperty("user.dir"));
        }

        final ICostModel costModel =
                timed("Load cost model", () -> MonotonicCostModelTree.load(new File(costModelXml)));

        AttackDefenseCollector collector =
                timed(
                        "Load CSV",
                        () -> {
                            try {
                                return new AttackDefenseCollector(stemOutDir, inference);
                            } catch (IOException | MalformedInputException e) {
                                throw new RuntimeException(e);
                            }
                        });
        List<Result> results = timed("Build attack-defense tree", () -> collector.perform());

        // This part is for the single cyber requirement version
        // disabled because it doesn't work anymore
        //        for (Result result : results) {
        //            System.out.println();
        //            System.out.println("Result for cyber req: " + result.cyberReq.getName());
        //            DLeaf.Factory factory = new DLeaf.Factory();
        //            DTree dtree =
        //                    timed(
        //                            "Construct defense tree",
        //                            () ->
        //                                    DTreeConstructor.construct(
        //                                            result.adtree,
        //                                            costModel,
        //                                            result.cyberReq.getSeverityDal(),
        //                                            partialSolution,
        //                                            false,
        //                                            factory));
        //            Optional<Pair<Set<ComponentDefense>, Double>> selected =
        //                    timed(
        //                            "Perform synthesis",
        //                            () ->
        //                                    VerdictSynthesis.performSynthesisSingle(
        //                                            dtree,
        //                                            result.cyberReq.getSeverityDal(),
        //                                            factory,
        //                                            VerdictSynthesis.Approach.MAXSMT));
        //            if (selected.isPresent()) {
        //                for (ComponentDefense pair : selected.get().left) {
        //                    System.out.println("Selected leaf: " + pair.toString());
        //                }
        //                System.out.println("Total cost: " + selected.get().right);
        //            }
        //        }

        System.out.println("\n\n\n");

        {
            DLeaf.Factory factory = new DLeaf.Factory();
            DTree dtree =
                    timed(
                            "Construct defense tree",
                            () ->
                                    DTreeConstructor.construct(
                                            results,
                                            costModel,
                                            partialSolution,
                                            meritAssignment,
                                            factory));
            Optional<ResultsInstance> selected =
                    timed(
                            "Perform synthesis",
                            () ->
                                    VerdictSynthesis.performSynthesisMultiple(
                                            dtree,
                                            factory,
                                            costModel,
                                            partialSolution,
                                            true,
                                            meritAssignment,
                                            dumpSmtLib));
            if (selected.isPresent()) {
                selected.get().toStreamXml(System.out);
            } else {
                System.out.println("Failure?");
            }
        }

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
