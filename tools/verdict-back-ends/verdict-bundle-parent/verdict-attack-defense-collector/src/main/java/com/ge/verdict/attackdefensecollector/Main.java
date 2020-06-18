package com.ge.verdict.attackdefensecollector;

import java.io.IOException;
import java.util.List;

import com.ge.verdict.attackdefensecollector.AttackDefenseCollector.Result;

public class Main {
    public static void main(String[] args) throws IOException, CSVFile.MalformedInputException {
        long start = System.currentTimeMillis();
        AttackDefenseCollector attackDefenseCollector = new AttackDefenseCollector(args[0]);
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
			Logger.println("Successful: " + (Prob.lte(result.prob, result.cyberReq.getSeverity()) ? "Yes" : "No"));
		}
        Logger.println("Total time: " + (System.currentTimeMillis() - start) + " milliseconds");
    }
}
