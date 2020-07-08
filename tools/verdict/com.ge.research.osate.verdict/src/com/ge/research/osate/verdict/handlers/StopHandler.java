package com.ge.research.osate.verdict.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.HandlerEvent;

/**
 * Stops a running MBAS/CRV command. 
 */
public class StopHandler extends AbstractHandler {

	private static StopHandler instance = null;
	private static VerdictBundleCommand command = null;

	/** Constructs what should be our only StopHandler instance. */
	public StopHandler() {
		if (instance != null) {
			System.err.println("Something unexpected happened - more than one StopHandler instance has been created");
		}
		instance = this;
		setBaseEnabled(false); // disabled until first MBAS/CRV is run
	}
	
	/** Enables the StopHandler when we need it. */
	public static void enable(VerdictBundleCommand command) {
		StopHandler.command = command;
		instance.setBaseEnabled(true);
		// Refresh the icon's color more quickly in case it was already enabled
		instance.fireHandlerChanged(new HandlerEvent(instance, true, false));
	}

	/** Disables the StopHandler when we don't need it anymore. */
	public static void disable() {
		StopHandler.command = null;
		instance.setBaseEnabled(false);
		// Refresh the icon's color more quickly in case it was already disabled
		instance.fireHandlerChanged(new HandlerEvent(instance, true, false));
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (command != null) {
			command.stop();
			disable();
		}
		return null;
	}
}
