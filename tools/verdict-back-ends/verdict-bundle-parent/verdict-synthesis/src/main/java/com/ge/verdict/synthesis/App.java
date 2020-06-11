package com.ge.verdict.synthesis;

import java.util.Arrays;

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

    public static void main(String[] args) {
		System.out.println(System.getenv("LD_LIBRARY_PATH"));
		VerdictSynthesis.performSynthesis(buildDemo());
    }

	private static DTree buildDemo() {
		return new DOr(Arrays.asList(
				new DAnd(
						Arrays.asList(DLeaf.createIfNeeded("A-comp", "A-def", "A-att"), DLeaf.createIfNeeded(
								"B-comp",
								"B-def", "B-att"))),
				new DAnd(Arrays.asList(DLeaf.createIfNeeded("B-comp", "B-def", "B-att"),
						DLeaf.createIfNeeded("C-comp", "C-def", "C-att"))),
				new DAnd(Arrays.asList(DLeaf.createIfNeeded("A-comp", "A-def", "A-att"),
						DLeaf.createIfNeeded("C-comp", "C-def", "C-att")))
		));
	}
}
