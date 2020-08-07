package com.ge.research.osate.verdict.gui;

import java.util.List;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/

//this class performs the overall task of generating report from CRV .xml file
//need to be invoked from CRV handler (just before the handler returns for static implementation
//dynamic update can be implemented by creating parallel threads: one for CRV tool, one for the report-generator)
public class CRVReportGenerator implements Runnable {
	private String fileName1;
	private String fileName2;
	public List<CRVSummaryRow> tableContents;
	public static IWorkbenchWindow window;

	public CRVReportGenerator(String fileName1, String fileName2, IWorkbenchWindow window) {
		this.fileName1 = fileName1;
		this.fileName2 = fileName2;
		CRVReportGenerator.window = window;
		ResultsPageUtil.closePages();
		CRVResultSummary result = new CRVResultSummary(fileName1, fileName2);
		tableContents = result.getTableContents();
		MeritAssignmentView.treeContents = result.getIVC();
		showView(window);
	}

	@Override
	public void run() {
		new CRVResultSummary(fileName1, fileName2);
	}

	// invokes the view tab for CRV result
	protected void showView(IWorkbenchWindow window) {
		/*
		 * This command is executed while the xtext document is locked. Thus it must be async
		 * otherwise we can get a deadlock condition if the UI tries to lock the document,
		 * e.g., to pull up hover information.
		 */
		window.getShell().getDisplay().asyncExec(() -> {
			try {
				CRVResultsView.tableContents = tableContents;
				window.getActivePage().showView(CRVResultsView.ID);
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		});
	}
}