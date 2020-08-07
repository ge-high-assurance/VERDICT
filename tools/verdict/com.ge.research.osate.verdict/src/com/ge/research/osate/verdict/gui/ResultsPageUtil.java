package com.ge.research.osate.verdict.gui;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

public class ResultsPageUtil {
	public static void closePages() {
		IWorkbenchPage wp = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IViewPart myView1 = wp.findView(MBASResultsView.ID);
		if (myView1 != null) {
			wp.hideView(myView1);
		}
		IViewPart myView2 = wp.findView(CapecDefenseView.ID);
		if (myView2 != null) {
			wp.hideView(myView2);
		}
		IViewPart myView3 = wp.findView(SafetyCutsetsView.ID);
		if (myView3 != null) {
			wp.hideView(myView3);
		}
		IViewPart myView4 = wp.findView(MBASSynthesisResultsView.ID);
		if (myView4 != null) {
			wp.hideView(myView4);
		}
		IViewPart myView5 = wp.findView(CRVResultsView.ID);
		if (myView5 != null) {
			wp.hideView(myView5);
		}
		IViewPart myView6 = wp.findView(CounterExampleView.ID_COUNTER_EXAMPLE);
		if (myView6 != null) {
			wp.hideView(myView6);
		}
		IViewPart myView7 = wp.findView(CounterExampleView.ID_TEST_CASE);
		if (myView7 != null) {
			wp.hideView(myView7);
		}
		IViewPart myView8 = wp.findView(MeritAssignmentView.ID);
		if (myView8 != null) {
			wp.hideView(myView8);
		}
	}
}
