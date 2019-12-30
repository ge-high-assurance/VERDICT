package com.ge.verdict.utils;

public class Logger {
    public static void info(String msg) {
        System.out.println("Info: " + msg);
    }

    public static void warn(String msg) {
        System.out.println("Warning: " + msg);
    }

    public static void error(String msg) {
        System.out.println("Error: " + msg);
    }
    
    public static void plain(String msg) {
        System.out.println("       " + msg);
    }    

    public static void errAndExit(String msg) {
        System.out.println("Error: " + msg);
        System.exit(-1);
    }
}
