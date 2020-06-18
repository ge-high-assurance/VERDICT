package com.ge.verdict.synthesis;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.ge.verdict.attackdefensecollector.AttackDefenseCollector;
import com.ge.verdict.attackdefensecollector.AttackDefenseCollector.Result;
import com.ge.verdict.attackdefensecollector.CSVFile.MalformedInputException;
import com.ge.verdict.synthesis.dtree.DAnd;
import com.ge.verdict.synthesis.dtree.DLeaf;
import com.ge.verdict.synthesis.dtree.DOr;
import com.ge.verdict.synthesis.dtree.DTree;

public class App {
    /*
     * Note: currently must set LD_LIBRARY_PATH. For testing purposes
     * this can be done in Eclipse run configurations, but we will have
     * to figure this out in a cross-platform way and also set it up
     * in the Docker image.
     */

	public static void main(String[] args) throws IOException, MalformedInputException {
		if (args.length < 2) {
			throw new RuntimeException("Must specify STEM output directory and cost model XML!");
        }

		String stemOutDir = args[0];
		String costModelXml = args[1];

		CostModel costModel = new CostModel(new File(costModelXml));

		AttackDefenseCollector collector = new AttackDefenseCollector(stemOutDir);
		List<Result> results = collector.perform();

		for (Result result : results) {
			System.out.println();
			System.out.println("Result for cyber req: " + result.cyberReq.getName());
			DTree dtree = DTreeConstructor.construct(result.adtree, costModel, result.cyberReq.getSeverityDal());
			Optional<Set<DLeaf>> selected = VerdictSynthesis.performSynthesis(dtree);
			if (selected.isPresent()) {
				for (DLeaf leaf : selected.get()) {
					System.out.println("Selected leaf: " + leaf.prettyPrint());
				}
			}
		}
	}

	private static DTree buildDemo() {
        return new DOr(
                Arrays.asList(
                        new DAnd(
                                Arrays.asList(
                                        DLeaf.createIfNeeded("A-comp", "A-def", "A-att", 1),
                                        DLeaf.createIfNeeded("B-comp", "B-def", "B-att", 1))),
                        new DAnd(
                                Arrays.asList(
                                        DLeaf.createIfNeeded("B-comp", "B-def", "B-att", 1),
                                        DLeaf.createIfNeeded("C-comp", "C-def", "C-att", 1))),
                        new DAnd(
                                Arrays.asList(
                                        DLeaf.createIfNeeded("A-comp", "A-def", "A-att", 1),
                                        DLeaf.createIfNeeded("C-comp", "C-def", "C-att", 1)))));
    }
}
