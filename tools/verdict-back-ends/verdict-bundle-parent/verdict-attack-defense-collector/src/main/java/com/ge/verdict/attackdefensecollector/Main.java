package com.ge.verdict.attackdefensecollector;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, CSVFile.MalformedInputException {
        long start = System.currentTimeMillis();
        AttackDefenseCollector attackDefenseCollector = new AttackDefenseCollector(args[0]);
        attackDefenseCollector.perform();
        Logger.println("Total time: " + (System.currentTimeMillis() - start) + " milliseconds");
    }
}
