package com.ge.research.osate.verdict.gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvMapReader {
	/**
	 * Used to read a CSV file with at least two columns, and build a map
	 * from one column (the keys) to another (the values). The key and value
	 * columns are identified by headers. Cells may contain semicolons to separate
	 * multiple key-value pairs in the same row.
	 * 
	 * This is currently used for reading CAPEC.csv and Defenses2NIST.csv.
	 * 
	 * @param file
	 * @param keyColumn
	 * @param valueColumn
	 * @return
	 */
	public static Map<String, String> readCsvMap(File file, String keyColumn, String valueColumn) {
		List<String> lines;
		try {
			lines = Files.readAllLines(file.toPath());
		} catch (IOException e) {
			System.err.println("Failed to read CSV: " + file.getAbsolutePath());
			e.printStackTrace();
			return Collections.emptyMap();
		}
		if (lines.isEmpty()) {
			System.err.println("Failed to read CSV, empty: " + file.getAbsolutePath());
			return Collections.emptyMap();
		}
		
		int keyIndex = -1, valueIndex = -1;
		String[] headers = lines.get(0).split(",");
		for (int i = 0; i < headers.length; i++) {
			if (keyColumn.equals(headers[i])) {
				keyIndex = i;
			} else if (valueColumn.equals(headers[i])) {
				valueIndex = i;
			}
		}
		if (keyIndex == -1 || valueIndex == -1) {
			System.err.println("Failed to read CSV, missing expected header: " + file.getAbsolutePath());
			return Collections.emptyMap();
		}
		
		Map<String, String> result = new LinkedHashMap<>();
		
		for (int i = 1; i < lines.size(); i++) {
			String[] cells = lines.get(i).split(",");
			if (keyIndex >= cells.length || valueIndex >= cells.length) {
				System.err.println("Failed to read CSV, missing cells on line " + i + ": " + file.getAbsolutePath());
				continue;
			}
			String[] keys = cells[keyIndex].split(";");
			String[] values = cells[valueIndex].split(";");
			if (keys.length != values.length) {
				System.err.println("Failed to read CSV, inequal keys and values on line " + i + ": " + file.getAbsolutePath());
				continue;
			}
			for (int j = 0; j < keys.length; j++) {
				result.put(keys[j].replace("\"", ""), values[j].replace("\"", ""));
			}
		}
		
		return result;
	}
}
