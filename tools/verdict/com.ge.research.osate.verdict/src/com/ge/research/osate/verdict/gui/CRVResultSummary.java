package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/

//this class creates the CRV Results table contents from the contents extracted from .xml
public class CRVResultSummary {

	private List<CRVSummaryRow> tableContents;
	private List<IVCNode> ivc;

	public CRVResultSummary(String fileName1, String fileName2) {
		CRVReadXMLFile xmlReader = new CRVReadXMLFile(fileName1, fileName2);
		tableContents = loadTableContents(xmlReader.getResults());
		ivc = xmlReader.getIVC();
	}

	private List<CRVSummaryRow> loadTableContents(List<CRVResultAttributes> attributes) {
		try {
			return loadTableContentsAtg(attributes);
		} catch (InvalidAtgException e) {
			return loadTableContentsNormal(attributes);
		}
	}

	public List<CRVSummaryRow> loadTableContentsNormal(List<CRVResultAttributes> attributes) {
		List<CRVSummaryRow> list = new ArrayList<CRVSummaryRow>();
		for (int i = 0; i < attributes.size(); i++) {
			if ("wamax".equals(attributes.get(i).getSource())) {
				continue;
			}
			CRVSummaryRow newRow = new CRVSummaryRow();
			newRow.setPropertyName(attributes.get(i).getProperty());
			newRow.addRow(attributes.get(i).getProperty());
			newRow.addRow(attributes.get(i).getAnswer());
			if (attributes.get(i).getBlameAssignment() != null) {
				newRow.addRow(attributes.get(i).getBlameAssignment().getThreats());
				newRow.addRow(attributes.get(i).getBlameAssignment().getComponents());
				newRow.addRow(attributes.get(i).getBlameAssignment().getLinks());
				newRow.addRow(attributes.get(i).getBlameAssignment().getComponentsUncompromised());
				newRow.addRow(attributes.get(i).getBlameAssignment().getLinksUncompromised());
			} else {
				newRow.addRow("");
				newRow.addRow("");
				newRow.addRow("");
				newRow.addRow("");
				newRow.addRow("");
			}
			newRow.setCounterExample(attributes.get(i).getCntExample());
			newRow.setTestCase(Collections.emptyList());
			newRow.setValidTill(attributes.get(i).getValidTill());
			list.add(newRow);
		}
		return list;
	}

	private static class PosNegPair {
		CRVResultAttributes pos, neg;
	}

	private static class InvalidAtgException extends Exception {
		private static final long serialVersionUID = 1L;
		public InvalidAtgException(String message) {
			super(message);
		}
	}

	public List<CRVSummaryRow> loadTableContentsAtg(List<CRVResultAttributes> attributes) throws InvalidAtgException {
		// Match pairs of pos, neg guarantees together
		Map<String, PosNegPair> pairs = new LinkedHashMap<>();
		for (CRVResultAttributes lustreRow : attributes) {
			if ("wamax".equals(lustreRow.getSource())) {
				continue;
			}
			boolean pos;
			if (lustreRow.getProperty().startsWith("pos_")) {
				pos = true;
			} else if (lustreRow.getProperty().startsWith("neg_")) {
				pos = false;
			} else {
				throw new InvalidAtgException(
						"got a row without valid 'pos_' or 'neg_' prefix: " + lustreRow.getProperty());
			}

			// Chop prefix, remove '[#]' suffix
			int lastOpenBracket = lustreRow.getProperty().lastIndexOf('[');
			String realName = lustreRow.getProperty().substring(4, lastOpenBracket);
			lustreRow.setProperty(realName);

			PosNegPair pair;

			if (pairs.containsKey(realName)) {
				pair = pairs.get(realName);
			} else {
				pair = new PosNegPair();
				pairs.put(realName, pair);
			}

			if (pos) {
				pair.pos = lustreRow;
			} else {
				pair.neg = lustreRow;
			}
		}

		List<CRVSummaryRow> list = new ArrayList<CRVSummaryRow>();

		for (String prop : pairs.keySet()) {
			PosNegPair pair = pairs.get(prop);
			if (pair.pos == null || pair.neg == null) {
				throw new InvalidAtgException("Mission either pos or neg guarantee for property: " + prop);
			}

			CRVSummaryRow newRow = new CRVSummaryRow();
			newRow.setPropertyName(prop);
			newRow.addRow(prop);
			newRow.addRow(pair.pos.getAnswer());
			if (pair.pos.getBlameAssignment() != null) {
				newRow.addRow(pair.pos.getBlameAssignment().getThreats());
				newRow.addRow(pair.pos.getBlameAssignment().getComponents());
				newRow.addRow(pair.pos.getBlameAssignment().getLinks());
				newRow.addRow(pair.pos.getBlameAssignment().getComponentsUncompromised());
				newRow.addRow(pair.pos.getBlameAssignment().getLinksUncompromised());
			} else {
				newRow.addRow("");
				newRow.addRow("");
				newRow.addRow("");
				newRow.addRow("");
				newRow.addRow("");
			}

			newRow.setCounterExample(pair.pos.getCntExample());
			newRow.setValidTill(pair.pos.getValidTill());

			if (!"falsifiable".equals(pair.pos.getAnswer())) {
				// Valid
				newRow.setTestCase(pair.neg.getCntExample());
			} else {
				// Invalid
				newRow.setTestCase(pair.pos.getCntExample());
			}

			list.add(newRow);
		}

		return list;
	}

	public List<CRVSummaryRow> getTableContents() {
		return tableContents;
	}
	
	public List<IVCNode> getIVC() {
		return ivc;
	}
}