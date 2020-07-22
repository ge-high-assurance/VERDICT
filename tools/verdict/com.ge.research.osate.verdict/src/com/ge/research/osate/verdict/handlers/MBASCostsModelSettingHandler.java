package com.ge.research.osate.verdict.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.ecore.EObject;

import com.ge.research.osate.verdict.gui.MBASCostsModelView;

public class MBASCostsModelSettingHandler extends AbstractHandler {
	private static MBASCostsModelView mbasCostsModelWindow;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (mbasCostsModelWindow == null) {
			List<EObject> aadlObjs = new ArrayList<>();
			List<String> selection = VerdictHandlersUtils.getCurrentSelection(event);
			
			if(selection.size() >= 1) {
				aadlObjs = VerdictHandlersUtils.preprocessAadlFiles(new File(selection.get(0)));
			}
			
			mbasCostsModelWindow = new MBASCostsModelView(aadlObjs);
			mbasCostsModelWindow.run();
			mbasCostsModelWindow = null;
		} else {
			mbasCostsModelWindow.bringToFront(mbasCostsModelWindow.getShell());
		}
		return null;		
	}
}
