package com.ge.verdict.utils;

import java.io.File;

public class Utilities {
	/**
	 * Check if a path is a valid directory
	 * */
    public boolean isValidDir(String path) {
        if (path != null) {
            File dir = new File(path);

            if (dir.exists() && dir.isDirectory()) {
                return true;
            }
        }

        return false;
    }
}
