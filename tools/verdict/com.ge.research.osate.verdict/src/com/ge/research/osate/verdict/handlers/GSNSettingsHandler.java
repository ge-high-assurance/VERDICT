package com.ge.research.osate.verdict.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.ge.research.osate.verdict.gui.GSNSettingsPanel;

/**
 * If the settings panel is not created yet, we create a new one;
 * otherwise, we bring the old panel to the front.
 */
public class GSNSettingsHandler extends AbstractHandler {
	private static GSNSettingsPanel gsnSettingsWindow;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (gsnSettingsWindow == null) {
			gsnSettingsWindow = new GSNSettingsPanel();
			gsnSettingsWindow.run();
			gsnSettingsWindow = null;
		} else {
			gsnSettingsWindow.bringToFront(gsnSettingsWindow.getShell());
		}
		return null;
	}
}
