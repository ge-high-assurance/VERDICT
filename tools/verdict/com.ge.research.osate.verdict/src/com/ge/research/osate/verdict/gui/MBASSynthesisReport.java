package com.ge.research.osate.verdict.gui;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.ge.verdict.vdm.synthesis.ResultsInstance;

public class MBASSynthesisReport {
	public static void report(File xmlFile) {
		try {
			ResultsInstance results = ResultsInstance.fromFile(xmlFile);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
}
