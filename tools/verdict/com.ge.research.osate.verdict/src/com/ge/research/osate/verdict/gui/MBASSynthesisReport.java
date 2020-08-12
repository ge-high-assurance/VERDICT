package com.ge.research.osate.verdict.gui;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.xml.sax.SAXException;

import com.ge.research.osate.verdict.handlers.SynthesisAadlWriter;
import com.ge.verdict.vdm.synthesis.ResultsInstance;

public class MBASSynthesisReport {
	public static void report(IProject project, File projectDir, File xmlFile, IWorkbenchWindow window) {
		try {
			ResultsInstance results = ResultsInstance.fromFile(xmlFile);

			window.getShell().getDisplay().asyncExec(() -> {
				try {
					ResultsPageUtil.closePages();
					MBASSynthesisResultsView.results = results;
					MBASSynthesisResultsView.applyToProject = () -> SynthesisAadlWriter.perform(project, projectDir,
							results);
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
