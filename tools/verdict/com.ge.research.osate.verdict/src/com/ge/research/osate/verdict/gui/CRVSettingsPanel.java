package com.ge.research.osate.verdict.gui;

import java.util.HashSet;
import java.util.Set;

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
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
*
* Author: Paul Meng
* Date: Jun 12, 2019
*
*/

public class CRVSettingsPanel extends ApplicationWindow {
	public static Set<String> selectedThreats = new HashSet<String>();
	public static boolean testCaseGeneration = false;
	public static boolean blameAssignment = false;
	public static boolean componentLevel = false;

	private static final String LS = "-LS";
	private static final String NI = "-NI";
	private static final String LB = "-LB";
	private static final String IT = "-IT";
	private static final String OT = "-OT";
	private static final String RI = "-RI";
	private static final String SV = "-SV";
	private static final String HT = "-HT";

	private Font font, boldFont;

	public CRVSettingsPanel() {
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
		shell.setText("CRV Settings");
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

		Label atgLabel = new Label(composite, SWT.NONE);
		atgLabel.setText("Test Case Generation");
		atgLabel.setFont(boldFont);

		Group atgGroup = new Group(composite, SWT.NONE);
		atgGroup.setLayout(new GridLayout(1, false));

		// The "Enable Test Case Generation (ATG)" section
		Button atgCheckBox = new Button(atgGroup, SWT.CHECK);
		atgCheckBox.setText("Enable Test Case Generation (ATG)");
		atgCheckBox.setFont(font);
		atgCheckBox.setImage(AbstractUIPlugin
				.imageDescriptorFromPlugin("com.ge.research.osate.verdict", "icons/atg.png").createImage());
		atgCheckBox.setSelection(testCaseGeneration);

		// The "Blame Assignment" section: component-level and link-level
		Label blameAssignmentLabel = new Label(composite, SWT.NONE);
		blameAssignmentLabel.setText("Blame Assignment");
		blameAssignmentLabel.setFont(boldFont);

		Group baGroup = new Group(composite, SWT.NONE);
		baGroup.setLayout(new GridLayout(1, false));

		Button baCheckBox = new Button(baGroup, SWT.CHECK);
		baCheckBox.setText("Enable Blame Assignment");
		baCheckBox.setFont(font);
		baCheckBox.setImage(AbstractUIPlugin.imageDescriptorFromPlugin("com.ge.research.osate.verdict", "icons/BA.ico")
				.createImage());
		baCheckBox.setSelection(blameAssignment);

		Composite compLinkGroup = new Composite(baGroup, SWT.NONE);
		compLinkGroup.setLayout(new RowLayout(SWT.HORIZONTAL));
		compLinkGroup.setEnabled(blameAssignment);

		Button compLevel = new Button(compLinkGroup, SWT.RADIO);
		compLevel.setText("Component-level");
		compLevel.setFont(font);
		compLevel.setSelection(componentLevel);
		compLevel.setEnabled(blameAssignment);

		Button linkLevel = new Button(compLinkGroup, SWT.RADIO);
		linkLevel.setText("Link-level");
		linkLevel.setFont(font);
		linkLevel.setSelection(!componentLevel);
		linkLevel.setEnabled(blameAssignment);

		// The "Enabled Threat Models" section: all the threats
		Label threatModelsLabel = new Label(composite, SWT.NONE);
		threatModelsLabel.setText("Enabled Threat Models");
		threatModelsLabel.setFont(boldFont);

		Group selectionButtonGroup = new Group(composite, SWT.NONE);
		selectionButtonGroup.setLayout(new RowLayout(SWT.VERTICAL));

		Button ls = new Button(selectionButtonGroup, SWT.CHECK);
		ls.setImage(AbstractUIPlugin.imageDescriptorFromPlugin("com.ge.research.osate.verdict", "icons/LS.png")
				.createImage());
		ls.setText("Location Spoofing");
		if (selectedThreats.contains(LS)) {
			ls.setSelection(true);
		}

		Button ni = new Button(selectionButtonGroup, SWT.CHECK);
		ni.setImage(AbstractUIPlugin.imageDescriptorFromPlugin("com.ge.research.osate.verdict", "icons/NI.png")
				.createImage());
		ni.setText("Network Injection");
		if (selectedThreats.contains(NI)) {
			ni.setSelection(true);
		}

		Button lb = new Button(selectionButtonGroup, SWT.CHECK);
		lb.setImage(AbstractUIPlugin.imageDescriptorFromPlugin("com.ge.research.osate.verdict", "icons/LB.png")
				.createImage());
		lb.setText("Logic Bomb");
		if (selectedThreats.contains(LB)) {
			lb.setSelection(true);
		}

		Button it = new Button(selectionButtonGroup, SWT.CHECK);
		it.setImage(AbstractUIPlugin.imageDescriptorFromPlugin("com.ge.research.osate.verdict", "icons/IT.png")
				.createImage());
		it.setText("Insider Threat");
		if (selectedThreats.contains(IT)) {
			it.setSelection(true);
		}

		Button ot = new Button(selectionButtonGroup, SWT.CHECK);
		ot.setImage(AbstractUIPlugin.imageDescriptorFromPlugin("com.ge.research.osate.verdict", "icons/OT.png")
				.createImage());
		ot.setText("Outside User Threat");
		if (selectedThreats.contains(OT)) {
			ot.setSelection(true);
		}

		Button ri = new Button(selectionButtonGroup, SWT.CHECK);
		ri.setImage(AbstractUIPlugin.imageDescriptorFromPlugin("com.ge.research.osate.verdict", "icons/RI.png")
				.createImage());
		ri.setText("Remote Code Injection");
		if (selectedThreats.contains(RI)) {
			ri.setSelection(true);
		}

		Button sv = new Button(selectionButtonGroup, SWT.CHECK);
		sv.setImage(AbstractUIPlugin.imageDescriptorFromPlugin("com.ge.research.osate.verdict", "icons/SV.png")
				.createImage());
		sv.setText("Software Virus/Malware/Worm/Trojan");
		if (selectedThreats.contains(SV)) {
			sv.setSelection(true);
		}

		Button ht = new Button(selectionButtonGroup, SWT.CHECK);
		ht.setImage(AbstractUIPlugin.imageDescriptorFromPlugin("com.ge.research.osate.verdict", "icons/HT.png")
				.createImage());
		ht.setText("Hardware Trojans");
		if (selectedThreats.contains(HT)) {
			ht.setSelection(true);
		}

		Group selDeAllButtons = new Group(composite, SWT.NONE);
		selDeAllButtons.setLayout(new RowLayout(SWT.HORIZONTAL));
		selDeAllButtons.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, true, true, 1, 1));

		Button selectAll = new Button(selDeAllButtons, SWT.PUSH);
		selectAll.setText("Select All");
		selectAll.setFont(font);
		selectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ls.setSelection(true);
				selectedThreats.add(LS);
				ni.setSelection(true);
				selectedThreats.add(NI);
				lb.setSelection(true);
				selectedThreats.add(LB);
				it.setSelection(true);
				selectedThreats.add(IT);
				ot.setSelection(true);
				selectedThreats.add(OT);
				ri.setSelection(true);
				selectedThreats.add(RI);
				sv.setSelection(true);
				selectedThreats.add(SV);
				ht.setSelection(true);
				selectedThreats.add(HT);
			}
		});

		Button deselectAll = new Button(selDeAllButtons, SWT.PUSH);
		deselectAll.setText("Deselect All");
		deselectAll.setFont(font);
		deselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ls.setSelection(false);
				ni.setSelection(false);
				lb.setSelection(false);
				it.setSelection(false);
				ot.setSelection(false);
				ri.setSelection(false);
				sv.setSelection(false);
				ht.setSelection(false);
				selectedThreats.clear();
			}
		});

		Composite closeButtons = new Composite(composite, SWT.NONE);
		closeButtons.setLayout(new RowLayout(SWT.HORIZONTAL));
		closeButtons.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, true, true, 1, 1));

		Button cancel = new Button(closeButtons, SWT.PUSH);
		cancel.setText("Cancel");
		cancel.setFont(font);

		Button save = new Button(closeButtons, SWT.PUSH);
		save.setText("Save Settings");
		save.setFont(font);

		// Set font for button text
		ls.setFont(font);
		ni.setFont(font);
		lb.setFont(font);
		it.setFont(font);
		ot.setFont(font);
		ri.setFont(font);
		sv.setFont(font);
		ht.setFont(font);
		save.setFont(font);

		// Set the preferred size
		Point bestSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
		getShell().setSize(bestSize);

		baCheckBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				compLinkGroup.setEnabled(baCheckBox.getSelection());
				compLevel.setEnabled(baCheckBox.getSelection());
				linkLevel.setEnabled(baCheckBox.getSelection());
			}
		});

		cancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				composite.getShell().close();
			}
		});

		save.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {

				if (ls.getSelection()) {
					selectedThreats.add(LS);
				} else if (selectedThreats.contains(LS)) {
					selectedThreats.remove(LS);
				}
				if (ni.getSelection()) {
					selectedThreats.add(NI);
				} else if (selectedThreats.contains(NI)) {
					selectedThreats.remove(NI);
				}
				if (lb.getSelection()) {
					selectedThreats.add(LB);
				} else if (selectedThreats.contains(LB)) {
					selectedThreats.remove(LB);
				}
				if (it.getSelection()) {
					selectedThreats.add(IT);
				} else if (selectedThreats.contains(IT)) {
					selectedThreats.remove(IT);
				}
				if (ot.getSelection()) {
					selectedThreats.add(OT);
				} else if (selectedThreats.contains(OT)) {
					selectedThreats.remove(OT);
				}
				if (ri.getSelection()) {
					selectedThreats.add(RI);
				} else if (selectedThreats.contains(RI)) {
					selectedThreats.remove(RI);
				}
				if (sv.getSelection()) {
					selectedThreats.add(SV);
				} else if (selectedThreats.contains(SV)) {
					selectedThreats.remove(SV);
				}
				if (ht.getSelection()) {
					selectedThreats.add(HT);
				} else if (selectedThreats.contains(HT)) {
					selectedThreats.remove(HT);
				}

				testCaseGeneration = atgCheckBox.getSelection();
				blameAssignment = baCheckBox.getSelection();
				componentLevel = compLevel.getSelection();

				composite.getShell().close();
			}
		});
		return composite;
	}
}
