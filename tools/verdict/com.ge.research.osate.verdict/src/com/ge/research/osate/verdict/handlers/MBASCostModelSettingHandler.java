package com.ge.research.osate.verdict.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.ecore.EObject;

import com.ge.research.osate.verdict.gui.MBASCostModelView;

public class MBASCostModelSettingHandler extends AbstractHandler {
	private static MBASCostModelView mbasCostsModelWindow;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (mbasCostsModelWindow == null) {
			List<EObject> aadlObjs = new ArrayList<>();
			List<String> selection = VerdictHandlersUtils.getCurrentSelection(event);

			if(selection.size() >= 1) {
				aadlObjs = VerdictHandlersUtils.preprocessAadlFiles(new File(selection.get(0)));
			}

			File costModelFile = new File(selection.get(0), "costModel.xml");

			mbasCostsModelWindow = new MBASCostModelView(aadlObjs, costModelFile);
			mbasCostsModelWindow.run();
			mbasCostsModelWindow = null;
		} else {
			mbasCostsModelWindow.bringToFront(mbasCostsModelWindow.getShell());
		}
		return null;
	}
}
