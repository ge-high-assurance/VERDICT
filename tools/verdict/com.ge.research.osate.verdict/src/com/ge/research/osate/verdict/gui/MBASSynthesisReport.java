package com.ge.research.osate.verdict.gui;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.xml.sax.SAXException;

import com.ge.verdict.vdm.synthesis.ResultsInstance;

public class MBASSynthesisReport {
	public static void report(File xmlFile, IWorkbenchWindow window) {
		try {
			ResultsInstance results = ResultsInstance.fromFile(xmlFile);

			window.getShell().getDisplay().asyncExec(() -> {
				try {
					ResultsPageUtil.closePages();
					MBASSynthesisResultsView.results = results;
					window.getActivePage().showView(MBASSynthesisResultsView.ID);
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			});
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
}
