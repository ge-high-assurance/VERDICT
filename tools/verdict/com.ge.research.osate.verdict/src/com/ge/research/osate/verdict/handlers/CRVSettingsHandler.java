package com.ge.research.osate.verdict.handlers;

import com.ge.research.osate.verdict.gui.CRVSettingsPanel;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 *
 * @author Paul Meng
 * Date: Jun 12, 2019
 *
 */
public class CRVSettingsHandler extends AbstractHandler {

    private static CRVSettingsPanel crvSettingsWindow;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        // If the threats model panel is not created yet, we create a new one;
        // otherwise, we bring the old panel to the front.
        if (crvSettingsWindow == null) {
            crvSettingsWindow = new CRVSettingsPanel();
            crvSettingsWindow.run();
            crvSettingsWindow = null;
        } else {
            crvSettingsWindow.bringToFront(crvSettingsWindow.getShell());
        }
        return null;
    }
}
