package com.ge.verdict.synthesis;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.ge.verdict.attackdefensecollector.AttackDefenseCollector;
import com.ge.verdict.attackdefensecollector.AttackDefenseCollector.Result;
import com.ge.verdict.attackdefensecollector.CSVFile.MalformedInputException;
import com.ge.verdict.synthesis.dtree.DLeaf;
import com.ge.verdict.synthesis.dtree.DTree;

public class App {
    /*
     * Note: currently must set LD_LIBRARY_PATH. For testing purposes
     * this can be done in Eclipse run configurations, but we will have
     * to figure this out in a cross-platform way and also set it up
     * in the Docker image.
     */

	public static void main(String[] args) {
		if (args.length < 2) {
			throw new RuntimeException("Must specify STEM output directory and cost model XML!");
        }

		long startTime = System.currentTimeMillis();

		String stemOutDir = args[0];
		String costModelXml = args[1];

		final CostModel costModel = timed("Load cost model", () -> new CostModel(new File(costModelXml)));

		AttackDefenseCollector collector = timed("Load CSV", () -> {
			try {
				return new AttackDefenseCollector(stemOutDir);
			} catch (IOException | MalformedInputException e) {
				throw new RuntimeException(e);
			}
		});
		List<Result> results = timed("Build attack-defense tree", () -> collector.perform());

		for (Result result : results) {
			System.out.println();
			System.out.println("Result for cyber req: " + result.cyberReq.getName());
			DTree dtree = timed("Construct defense tree",
					() -> DTreeConstructor.construct(result.adtree, costModel, result.cyberReq.getSeverityDal()));
			Optional<Set<DLeaf>> selected = timed("Perform synthesis", () -> VerdictSynthesis.performSynthesis(dtree));
			if (selected.isPresent()) {
				for (DLeaf leaf : selected.get()) {
					System.out.println("Selected leaf: " + leaf.prettyPrint());
				}
			}
		}

		System.out.println(" == Total time: " + (System.currentTimeMillis() - startTime) + " milliseconds");
	}

	public static <T> T timed(String title, Supplier<T> function) {
		long startTime = System.currentTimeMillis();
		T ret = function.get();
		System.out.println(
				" == " + title + ", time: " + (System.currentTimeMillis() - startTime) + " milliseconds");
		return ret;
	}
}
