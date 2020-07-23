package com.ge.research.osate.verdict.handlers;

import java.io.File;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.ge.research.osate.verdict.gui.GSNSettingsPanel;
import com.ge.research.osate.verdict.aadl2vdm.Aadl2Vdm;
import verdict.vdm.vdm_model.Model;

/**
 * If the settings panel is not created yet, we create a new one;
 * otherwise, we bring the old panel to the front.
 */
public class GSNSettingsHandler extends AbstractHandler {
	private static GSNSettingsPanel gsnSettingsWindow;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (gsnSettingsWindow == null) {
			/**
			 * Create the VDM model here
			 * and send it to populate the panel
			 */
			List<String> selection = VerdictHandlersUtils.getCurrentSelection(event);
			File projectDir = new File(selection.get(0));
			Aadl2Vdm aadl2vdm = new Aadl2Vdm();
			Model m = aadl2vdm.execute(projectDir);
			//sending model to populate GSN settings panel
			gsnSettingsWindow = new GSNSettingsPanel(m);
			gsnSettingsWindow.run();
			gsnSettingsWindow = null;
		} else {
			gsnSettingsWindow.bringToFront(gsnSettingsWindow.getShell());
		}
		return null;
	}
}
