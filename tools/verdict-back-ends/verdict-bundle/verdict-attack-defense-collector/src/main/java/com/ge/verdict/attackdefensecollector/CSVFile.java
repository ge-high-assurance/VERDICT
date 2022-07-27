package com.ge.verdict.attackdefensecollector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Load and access CSV data.
 *
 * <p>CSV files must have column headers; columns are accessed by column header. This allows the CSV
 * format to be changed by adding new columns and changing the position of columns without breaking
 * any code that parses the CSV data.
 */
public class CSVFile {
    /**
     * Thrown by malformed CSV input. Contains the line number of the CSV file on which the problem
     * occurred.
     */
    public static class MalformedInputException extends Exception {
        private static final long serialVersionUID = 1L;

        private int line;

        public MalformedInputException(String message, int line) {
            super(message);
            this.line = line;
        }

        public int getLine() {
            return line;
        }
    }

    /** Mapping from column headers to data storage index. */
    private Map<String, Integer> headerPos;
    /**
     * CSV data. Cells in a row are ordered based on the ordering of columns provided to the
     * constructor. These are the same indices which may be accessed through headerPos.
     */
    private List<String[]> rows;

    /**
     * See the CSVFile(InputStream, boolean, String...) constructor.
     *
     * @param filename name of file to load
     * @param headers
     * @throws IOException
     * @throws MalformedInputException
     */
    public CSVFile(String filename, String... headers) throws IOException, MalformedInputException {
        this(filename, false, headers);
    }

    /**
     * See the CSVFile(InputStream, boolean, String...) constructor.
     *
     * @param filename name of file to load
     * @param quoted
     * @param headers
     * @throws IOException
     * @throws MalformedInputException
     */
    public CSVFile(String filename, boolean quoted, String... headers) throws IOException, MalformedInputException {
        this(new File(filename), quoted, headers);
    }

    /**
     * See the CSVFile(InputStream, boolean, String...) constructor.
     *
     * @param file file to load
     * @param headers
     * @throws IOException
     * @throws MalformedInputException
     */
    public CSVFile(File file, String... headers) throws IOException, MalformedInputException {
        this(file, false, headers);
    }

    /**
     * See the CSVFile(InputStream, boolean, String...) constructor.
     *
     * @param file file to load
     * @param quoted
     * @param headers
     * @throws IOException
     * @throws MalformedInputException
     */
    public CSVFile(File file, boolean quoted, String... headers) throws IOException, MalformedInputException {
        this(file.getName(), new FileInputStream(file), quoted, headers);
    }

    /**
     * See the CSVFile(InputStream, boolean, String...) constructor.
     *
     * @param inputStream
     * @param headers
     * @throws IOException
     * @throws MalformedInputException
     */
    public CSVFile(String filename, InputStream inputStream, String... headers)
            throws IOException, MalformedInputException {
        this(filename, inputStream, false, headers);
    }

    /**
     * Load the CSV data from the specified input stream with the specified headers.
     *
     * <p>The set of headers need not match the headers declared in the file. Instead, each
     * specified header must be a header in the CSV file, in any order. Headers present in the CSV
     * file but not specified are ignored. The loaded data uses the ordering of the headers
     * specified here.
     *
     * <p>The quoted flag is used for handling data output by STEM. Specifically, if set, it is
     * expected that all cells in the CSV file begin and end with a double-quote character, which
     * will be removed when the data is read. The exception is for cells which contain exactly the
     * text "null" (but without the quotes), which will be read as the empty string ("").
     *
     * <p>Throws IOException for file IO errors. Throws MalformedInputException for any malformed
     * input (missing headers, missing cells, no quotes when expected quotes, etc.).
     *
     * @param inputStream the input stream to receive CSV data from
     * @param quoted whether or not each cell is quoted, as in output from STEM
     * @param headers the ordered headers
     * @throws IOException
     * @throws MalformedInputException
     */
    public CSVFile(String filename, InputStream inputStream, boolean quoted, String... headers)
            throws IOException, MalformedInputException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            // --- Load headers ---

            // Make sure reading won't cause problems
            if (!reader.ready()) {
                throw new IOException("Error parsing " + filename + ", end of file reached before headers");
            }

            // Store header re-mappings
            headerPos = new HashMap<>();
            int[] indexPos = new int[headers.length];

            // Read first line containing headers
            String[] csvHeaders = reader.readLine().split(",");

            Map<String, Integer> csvHeaderPos = new HashMap<>();
            int pos = 0;
            for (String csvHeader : csvHeaders) {
                // Strip quotes when we load
                csvHeaderPos.put(stripQuotes(quoted, csvHeader, 0, false), pos++);
            }

            pos = 0;
            for (String header : headers) {
                Integer csvPos = csvHeaderPos.get(header);
                if (csvPos == null) {
                    // There is a header specified by the user that's not present in file
                    throw new MalformedInputException(
                            "Error parsing " + filename + ", missing expected header: " + header, 0);
                }
                headerPos.put(header, pos);
                indexPos[pos++] = csvPos;
            }

            // --- Load data ---

            rows = new ArrayList<>();

            // Headers line is the first line, so start counting at line one
            int lineNum = 1;
            while (reader.ready()) {
                String lineText = reader.readLine();

                // Skip blank lines
                if (lineText.trim().length() > 0) {
                    // Counting results from trim doesn't work if the line ends with a comma
                    // But this does
                    int cellCount = charCount(lineText, ',') + 1;

                    if (cellCount != csvHeaders.length) {
                        throw new MalformedInputException(
                                "Error parsing "
                                        + filename
                                        + ", incorrect number of columns in line #"
                                        + lineNum
                                        + ": got "
                                        + cellCount
                                        + ", expecting "
                                        + csvHeaders.length,
                                lineNum);
                    }

                    String[] line = lineText.split(",", csvHeaders.length);

                    // Map cells to correct positions based on order of specified headers
                    String[] convertedLine = new String[headers.length];
                    pos = 0;
                    for (int lookup : indexPos) {
                        convertedLine[pos++] = stripQuotes(quoted, line[lookup], lineNum, false);
                    }

                    rows.add(convertedLine);
                }

                lineNum++;
            }

            rows = Collections.unmodifiableList(rows);
        }
    }

    /**
     * Count the number of times that a character appears in a string.
     *
     * <p>There are more concise ways to do this (Regex, split, etc.), but they aren't as efficient.
     *
     * @param str the string
     * @param chr the character
     * @return the number of times that chr appears in str
     */
    private int charCount(String str, char chr) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (chr == str.charAt(i)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Helper function for stripping quotes in files where every value is quoted (*cough* STEM
     * *cough*). If quoted is false, returns original string; otherwise checks that string is in
     * fact quoted and returns string with the quotes stripped. Line number is used for throwing
     * informative error messages in case the string is not properly quoted.
     *
     * <p>If the string is expected to be quoted but it is instead exactly the text "null", then the
     * empty string is returned instead of throwing an exception. This is for STEM compatibility.
     *
     * @param quoted whether or not the string is expected to be quoted
     * @param str the string to strip quotes from
     * @param line the line number in which the string occurs
     * @param exception if true, throws exceptions on strings expected to be quoted but now;
     *     otherwise returns original string
     * @return if quoted, the string with quotes stripped; otherwise, the original string
     * @throws MalformedInputException if quoted and exception, but str is not quoted
     */
    private String stripQuotes(boolean quoted, String str, int line, boolean exception) throws MalformedInputException {
        if (quoted) {
            // Check for quotes
            if (!(str.length() > 2 && str.startsWith("\"") && str.endsWith("\""))) {
                if ("null".equals(str)) {
                    // This is a special case
                    return "";
                } else if (exception) {
                    throw new MalformedInputException(
                            "Expected quoted string on line #" + line + ", but got non-quoted: " + str, line);
                } else {
                    return str;
                }
            }
            // Strip quotes
            return str.substring(1, str.length() - 1);
        } else {
            return str;
        }
    }

    /**
     * @return the number of rows present in this CSV file
     */
    public int getRowCount() {
        return rows.size();
    }

    /**
     * @return the list of rows, as a string array
     */
    public List<String[]> getRows() {
        return rows;
    }

    /**
     * @param i
     * @return the row at index i, as a string array
     */
    public String[] getRow(int i) {
        return rows.get(i);
    }

    /**
     * @return the list of rows, as RowData
     */
    public List<RowData> getRowDatas() {
        return rows.stream().map(RowData::new).collect(Collectors.toList());
    }

    /**
     * @param i
     * @return the row at index i, as RowData
     */
    public RowData getRowData(int i) {
        return new RowData(rows.get(i));
    }

    /**
     * A row in a CSV file. Allows accessing cells by column header name in addition to
     * position/index.
     */
    public class RowData {
        private String[] data;

        private RowData(String[] data) {
            this.data = data;
        }

        /**
         * @param column
         * @return the data in cell from the specified column
         */
        public String getCell(int column) {
            return data[column];
        }

        /**
         * @param header
         * @return the data in the cell from the column with the specified header
         */
        public String getCell(String header) {
            Integer column = headerPos.get(header);
            if (column == null) {
                // Sanity check
                throw new RuntimeException("Invalid header: " + header);
            }
            return data[column];
        }
    }
}
