/* See LICENSE in project directory */
package com.ge.research.osate.verdict.aadl2csv;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
/**
*
* @author Paul Meng
*
*/
/** Construct tabular data and output to CSV. */
public class Table {
    private static class Row {
        String[] vals;

        public Row(String[] vals) {
            this.vals = vals;
        }
    }

    private int columns;
    private List<Row> rows;

    // buildingRow and buildingRowIndex are used to build rows incrementally
    // See addValue() and capRow()
    private String[] buildingRow;
    private int buildingRowIndex;

    private boolean replaceDots;

    /** @return the number of columns in this table */
    public int getColumns() {
        return columns;
    }

    /**
     * Convert String list to String array.
     *
     * @param list
     * @return
     */
    private static String[] arrayOfList(List<String> list) {
        return list.toArray(new String[list.size()]);
    }

    /**
     * Combine two String arrays by copying.
     *
     * @param a
     * @param b
     * @return
     */
    private static String[] combineArrays(String[] a, String[] b) {
        String[] out = new String[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    /**
     * Construct a new table with the given column names.
     *
     * @param columnNames
     */
    public Table(String... columnNames) {
        columns = columnNames.length;
        rows = new ArrayList<>();
        rows.add(new Row(columnNames));

        buildingRow = new String[columns];
        buildingRowIndex = 0;

        replaceDots = false;
    }

    /**
     * Construct a new table with the given column names.
     *
     * @param columnNames
     */
    public Table(List<String> columnNames) {
        this(arrayOfList(columnNames));
    }

    /**
     * Construct a new table with the given column names (specified as two arrays that will be
     * merged together).
     *
     * @param columnNamesA
     * @param columnNamesB
     */
    public Table(String[] columnNamesA, String... columnNamesB) {
        this(combineArrays(columnNamesA, columnNamesB));
    }

    /**
     * If replaceDots is true, then all dots (".") will be replaced with underscores ("_") during
     * CSV output.
     *
     * @param replaceDots
     */
    public void setReplaceDots(boolean replaceDots) {
        this.replaceDots = replaceDots;
    }

    /**
     * See setReplaceDots().
     *
     * @return
     */
    public boolean isReplaceDots() {
        return replaceDots;
    }

    /**
     * Add a row to the table.
     *
     * <p>Requirement: vals.length is equal to getColumns().
     *
     * @param vals
     */
    public void addRow(String... vals) {
        if (vals.length != columns) {
            throw new RuntimeException(
                    "Attempt to add row with " + vals.length + " columns instead of " + columns);
        }
        rows.add(new Row(vals));
    }

    /**
     * Add a row to the table.
     *
     * <p>Requirement: vals.size() is equal to getColumns().
     *
     * @param vals
     */
    public void addRow(List<String> vals) {
        addRow(arrayOfList(vals));
    }

    /**
     * Add a row to the table (specified as two arrays that will be merged together).
     *
     * <p>Requirement: valsA.length + valsB.length is equal to getColumns().
     *
     * @param vals
     */
    public void addRow(String[] valsA, String... valsB) {
        addRow(combineArrays(valsA, valsB));
    }

    /**
     * Incrementally add one cell value to this table.
     *
     * <p>To build rows incrementally, call addValue() for each column and then call capRow() for
     * each row.
     *
     * <p>An exception will be thrown if attempting to add cells beyond the number of columns or if
     * attempting to cap an incomplete row.
     *
     * @param value
     */
    public void addValue(String value) {
        if (buildingRowIndex >= columns) {
            throw new RuntimeException("Attempt to add value beyond number of columns: " + columns);
        }
        buildingRow[buildingRowIndex++] = value;
    }

    /**
     * Incrementally add one row to this table, from the values added with addValue().
     *
     * <p>To build rows incrementally, call addValue() for each column and then call capRow() for
     * each row.
     *
     * <p>An exception will be thrown if attempting to add cells beyond the number of columns or if
     * attempting to cap an incomplete row.
     */
    public void capRow() {
        if (buildingRowIndex < columns) {
            throw new RuntimeException(
                    "Attempt to cap row before all columns filled: "
                            + buildingRowIndex
                            + "/"
                            + columns);
        }
        addRow(buildingRow);
        // Reset
        buildingRowIndex = 0;
        // Make new array because the old one is now used by the created row
        buildingRow = new String[columns];
    }

    /**
     * Convert this table to CSV format and return as a CharSequence.
     *
     * @return
     */
    public void toCsv(OutputStream out) throws IOException {
        Writer writer = new PrintWriter(out);
        for (Row row : rows) {
            for (int i = 0; i < row.vals.length; i++) {
                if (replaceDots) {
                    writer.write(row.vals[i].replace(".", "_"));
                } else {
                    writer.write(row.vals[i]);
                }

                if (i < row.vals.length - 1) {
                    writer.write(',');
                }
            }
            writer.write('\n');
            writer.flush();
        }
    }

    public String toCsvString() {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            toCsv(buffer);
            return buffer.toString("UTF-8");
        } catch (Exception e) {
            System.err.println("Failed to convert to CSV string");
            throw new RuntimeException(e);
        }
    }

    public void toCsvFile(File file) {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            toCsv(out);
            out.close();
            System.out.println("Info: Write to csv file: " + file.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to write file: " + file.getAbsolutePath());
            throw new RuntimeException(e);
        }
    }

    public void toCsvFile(String filePath) {
        toCsvFile(new File(filePath));
    }
}
