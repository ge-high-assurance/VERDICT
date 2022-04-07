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
public class MBASSettingsPanel extends ApplicationWindow {
	public static boolean openGraphs = true;
	public static boolean cyberInference = false; // not shown in UI anymore
	public static boolean safetyInference = false; // not shown in UI anymore
	public static boolean synthesisCyberInference = false; // not shown in UI anymore
	public static boolean synthesisPartialSolution = false;

	private Font font;
	private Font boldFont;

	public MBASSettingsPanel() {
		super(null);

		font = new Font(null, "Helvetica", 12, SWT.NORMAL);
		boldFont = new Font(null, "Helvetica", 12, SWT.BOLD);
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
		shell.setText("MBAA/MBAS Settings");
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
		analysisLabel.setText("Model Based Architecture Analysis");
		analysisLabel.setFont(boldFont);

		Group mbaaSelectionButtonGroup = new Group(composite, SWT.NONE);
		mbaaSelectionButtonGroup.setLayout(new RowLayout(SWT.VERTICAL));

		Button openGraphButton = new Button(mbaaSelectionButtonGroup, SWT.CHECK);
		openGraphButton.setText("Show Graphs in New Tabs");
		openGraphButton.setFont(font);
		openGraphButton.setSelection(openGraphs);		
		
		Label synthesisLabel = new Label(composite, SWT.NONE);
		synthesisLabel.setText("Model Based Architecture Synthesis");
		synthesisLabel.setFont(boldFont);

		Group mbasSelectionButtonGroup = new Group(composite, SWT.NONE);
		mbasSelectionButtonGroup.setLayout(new RowLayout(SWT.VERTICAL));

		Button partialSolution = new Button(mbasSelectionButtonGroup, SWT.CHECK);
		partialSolution.setText("Use Implemented Defenses");
		partialSolution.setFont(font);
		partialSolution.setSelection(synthesisPartialSolution);

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
				openGraphs = openGraphButton.getSelection();
				synthesisPartialSolution = partialSolution.getSelection();
				composite.getShell().close();
			}
		});
		return composite;
	}
}
