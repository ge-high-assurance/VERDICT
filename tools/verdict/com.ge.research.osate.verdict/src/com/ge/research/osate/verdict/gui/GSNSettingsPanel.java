package com.ge.research.osate.verdict.gui;

import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

/**
* Let user enable cyber or safety relations inference when running MBAA.
*/
public class GSNSettingsPanel extends ApplicationWindow {
	/**
	 * variables to control granularity og GSN
	 * missionFragments -  mission requirements
	 * cyberFragments -  cyber requirements 
	 * cyberFragments -  safety requirements
	 * allFragments - all requirements   
	 */
	public static boolean missionFragments = false;
	public static boolean cyberFragments = false;
	public static boolean safetyFragments = false;
	public static boolean allFragments = false;

	
	private Font font;
	private Font boldFont;

	public GSNSettingsPanel() {
		super(null);

		font = new Font(null, "Helvetica", 11, SWT.NORMAL);
		boldFont = new Font(null, "Helvetica", 11, SWT.BOLD);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		Display display = shell.getDisplay();
		Monitor primary = display.getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = shell.getBounds();

		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;

		shell.setLocation(x, y);
		shell.setText("GSN Settings");
		shell.setFont(font);
	}

	public void run() {
		setBlockOnOpen(true);
		open();
	}

	public void bringToFront(Shell shell) {
		shell.setActive();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		Label analysisLabel = new Label(composite, SWT.NONE);
		analysisLabel.setText("GSN Settings");
		analysisLabel.setFont(boldFont);

		Group selectionButtonGroup = new Group(composite, SWT.NONE);
		selectionButtonGroup.setLayout(new RowLayout(SWT.VERTICAL));

		Button missionButton = new Button(selectionButtonGroup, SWT.CHECK);
		missionButton.setText("Generate Fragments for All Mission Requirements");
		missionButton.setFont(font);
		missionButton.setSelection(missionFragments);
		
		Button cyberButton = new Button(selectionButtonGroup, SWT.CHECK);
		cyberButton.setText("Generate Fragments for All Cyber Requirements");
		cyberButton.setFont(font);
		cyberButton.setSelection(cyberFragments);

		Button safetyButton = new Button(selectionButtonGroup, SWT.CHECK);
		safetyButton.setText("Generate Fragments for All Safety Requirements");
		safetyButton.setFont(font);
		safetyButton.setSelection(safetyFragments);

		Button allButton = new Button(selectionButtonGroup, SWT.CHECK);
		allButton.setText("Generate Fragments for All Requirements");
		allButton.setFont(font);
		allButton.setSelection(allFragments);

		
		Composite closeButtons = new Composite(composite, SWT.NONE);
		closeButtons.setLayout(new RowLayout(SWT.HORIZONTAL));
		closeButtons.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, true, true, 1, 1));

		Button cancel = new Button(closeButtons, SWT.PUSH);
		cancel.setText("Cancel");
		cancel.setFont(font);

		Button save = new Button(closeButtons, SWT.PUSH);
		save.setText("Save Settings");
		save.setFont(font);

		// Set the preferred size
		Point bestSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
		getShell().setSize(bestSize);

		cancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				composite.getShell().close();
			}
		});

		save.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				missionFragments = missionButton.getSelection();
				cyberFragments = cyberButton.getSelection();
				safetyFragments = safetyButton.getSelection();
				allFragments = allButton.getSelection();
				composite.getShell().close();
			}
		});
		return composite;
	}
}
