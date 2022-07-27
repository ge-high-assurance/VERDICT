package com.ge.research.osate.verdict.handlers;

import com.ge.research.osate.verdict.gui.MBASSettingsPanel;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 * If the settings panel is not created yet, we create a new one;
 * otherwise, we bring the old panel to the front.
 */
public class MBASSettingsHandler extends AbstractHandler {
    private static MBASSettingsPanel mbasSettingsWindow;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        if (mbasSettingsWindow == null) {
            mbasSettingsWindow = new MBASSettingsPanel();
            mbasSettingsWindow.run();
            mbasSettingsWindow = null;
        } else {
            mbasSettingsWindow.bringToFront(mbasSettingsWindow.getShell());
        }
        return null;
    }
}
