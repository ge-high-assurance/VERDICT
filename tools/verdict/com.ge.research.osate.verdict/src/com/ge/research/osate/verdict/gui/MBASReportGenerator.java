package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
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
//this class performs the overall activities of generating report from CRV .xml
//to be called from MBAS handler (just before the handler returns for static implementation
//dynamic update can be implemented by creating two threads: one for MBAS tool and the other for this class)
public class MBASReportGenerator implements Runnable {
	private String fileName1;
	private String fileName2;
	public static IWorkbenchWindow window;
	private List<MissionAttributes> missions = new ArrayList<MissionAttributes>();

	public MBASReportGenerator(String fileName1, String fileName2, IWorkbenchWindow window) {
		this.fileName1 = fileName1;
		this.fileName2 = fileName2;
		MBASReportGenerator.window = window;
		IWorkbenchPage wp = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IViewPart myView1 = wp.findView(MBASResultsView.ID);
		if (myView1 != null) {
			wp.hideView(myView1);
		}
		IViewPart myView2 = wp.findView(CapecDefenseView.ID);
		if (myView2 != null) {
			wp.hideView(myView2);
		}
		MBASResultSummary result = new MBASResultSummary(fileName1, fileName2);
		missions = result.getMissions();
		showView(window);
	}

	@Override
	public void run() {
		new MBASResultSummary(fileName1, fileName2);
	}

	// invokes the MBAS Result viewer-tab in OSATE
	protected void showView(IWorkbenchWindow window) {
		/*
		 * This command is executed while the xtext document is locked. Thus it must be async
		 * otherwise we can get a deadlock condition if the UI tries to lock the document,
		 * e.g., to pull up hover information.
		 */
		window.getShell().getDisplay().asyncExec(() -> {
			try {
				MBASResultsView.missions = missions;
				window.getActivePage().showView(MBASResultsView.ID);
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		});
	}
}