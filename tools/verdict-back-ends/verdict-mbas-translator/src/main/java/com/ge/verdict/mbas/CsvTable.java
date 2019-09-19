/* See LICENSE in project directory */
package com.ge.verdict.mbas;

import java.util.ArrayList;
import java.util.List;

public class CsvTable {
    private String[] tags;
    private List<String[]> contents;

    public CsvTable(String[] tags) {
        this.tags = tags;
        this.contents = new ArrayList<>();
    }

    /**
     * Write a line to the CSV table.
     *
     * @param row Line of strings to be written to the table
     * @return Successfully written or not
     */
    public boolean writeToTable(String[] row) {
        // Check to see if the entered row is consistent with the tags.
        if (row == null || row.length != tags.length) {
            return false;
        }
        contents.add(row);
        return true;
    }

    /**
     * Print the CsvTable object to CSV string.
     *
     * @return CSV string
     */
    public String printToCSV() {
        StringBuilder sb = new StringBuilder();
        // Print tags first
        sb.append(printLine(tags));
        // Then print contents row by row
        for (String[] row : contents) {
            sb.append(printLine(row));
        }
        return sb.toString();
    }

    /**
     * Print array of strings to CSV string.
     *
     * @param row Line of strings to be printed
     * @return CSV string
     */
    private String printLine(String[] row) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < row.length; i++) {
            String element = row[i];
            sb.append(element);
            if (i != row.length - 1) {
                sb.append(',');
            } else {
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
