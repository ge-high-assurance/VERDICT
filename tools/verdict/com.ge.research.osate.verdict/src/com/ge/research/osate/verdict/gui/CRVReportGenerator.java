package com.ge.research.osate.verdict.gui;

import java.util.List;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

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
		IWorkbenchPage wp = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IViewPart myView1 = wp.findView(CRVResultsView.ID);
		if (myView1 != null) {
			wp.hideView(myView1);
		}

		IViewPart myView2 = wp.findView(CounterExampleView.ID_COUNTER_EXAMPLE);
		if (myView2 != null) {
			wp.hideView(myView2);
		}
		IViewPart myView3 = wp.findView(CounterExampleView.ID_TEST_CASE);
		if (myView3 != null) {
			wp.hideView(myView3);
		}
		CRVResultSummary result = new CRVResultSummary(fileName1, fileName2);
		tableContents = result.getTableContents();
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