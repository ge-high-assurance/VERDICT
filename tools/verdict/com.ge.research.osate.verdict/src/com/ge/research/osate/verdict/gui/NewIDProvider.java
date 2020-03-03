package com.ge.research.osate.verdict.gui;

import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/

// this class generates the Logical Definition editor dialog box for wizard
public class NewIDProvider extends ApplicationWindow {
	private Shell lastShell;
	private List<MissionInfo> missions;
	private List<WzrdTableRow> formulas;
	private String purpose;

	// Jface variables
	private Composite composite;
	private Rectangle sourceRect;

	public NewIDProvider(Shell shell, Rectangle sourceRect, List<MissionInfo> missions, List<WzrdTableRow> formulas,
			String purpose) {
		super(shell);
		lastShell = shell;
		this.sourceRect = sourceRect;
		this.missions = missions;
		this.purpose = purpose;
		this.formulas = formulas;
		MissionEditor.addIsValid = false;
	}

	public void run() {
		setShellStyle(SWT.TITLE | SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.MAX | SWT.RESIZE);
		setBlockOnOpen(true);
		open();
		if (!lastShell.isDisposed()) {
			lastShell.redraw();
			lastShell.setFocus();
		}
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		if (purpose.equals("mission")) {
			shell.setText("Wizard: New Mission");
		} else if (purpose.equals("relation")) {
			shell.setText("Wizard: New Cyber-relation");
		} else if (purpose.equals("requirement")) {
			shell.setText("Wizard: New Cyber-requirement");
		}
		shell.pack();
		double x = (sourceRect.width - 500) * 0.5;
		double y = (sourceRect.height - 500) * 0.5;
		shell.setLocation((int) x, (int) y);
		shell.setSize(500, 200);
	}

	// this defines all the widgets in the dialog-box
	@Override
	protected Control createContents(Composite parent) {
		composite = new Composite(parent, SWT.FILL);
		composite.setLayout(new GridLayout(1, false));

		Label begin = new Label(composite, SWT.FILL);
		begin.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));

		if (purpose.equals("mission")) {
			begin.setText("             Enter mission ID here:           ");
		} else if (purpose.equals("relation")) {
			begin.setText("             Enter cyber-relation ID here:           ");
		} else if (purpose.equals("requirement")) {
			begin.setText("             Enter cyber-requirement ID here:           ");
		}

		Text getName = new Text(composite, SWT.FILL | SWT.BORDER);
		getName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite ending = new Composite(composite, SWT.FILL);
		ending.setLayout(new GridLayout(2, false));
		ending.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));

		Button ok = new Button(ending, SWT.PUSH);
		ok.setText("Ok");

		Button cancel = new Button(ending, SWT.PUSH);
		cancel.setText("Cancel");

		// defines what activity to perform when button clicked
		cancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				MissionEditor.addIsValid = false;
				parent.getShell().close();
			}
		});

		// defines what activity to perform when button clicked
		ok.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				Boolean consistent = true;
				Boolean externalMatch = false;
				for (int i = 0; i < missions.size(); i++) {
					if (missions.get(i).getMissionID().equals(getName.getText())) {
						consistent = false;
						break;
					}
				}
				for (int i = 0; i < formulas.size(); i++) {
					if (formulas.get(i).getFormulaID().equals(getName.getText())) {
						consistent = false;
						break;
					}
				}

				// We will check this on save instead of here
//				if (consistent) {
//					if (idSet.contains(getName.getText())) {
//						externalMatch = true;
//					}
//				}
				if (getName.getText() == null || getName.getText().equals("")) {

					if (purpose.equals("mission")) {
						MessageDialog.openError(parent.getShell(), "VERDICT Mission Editor",
								"Entered mission-ID cannot be empty.");
					} else if (purpose.equals("relation")) {
						MessageDialog.openError(parent.getShell(), "VERDICT Cyber-property Editor",
								"Entered cyber-relation ID cannot be empty.");
					} else if (purpose.equals("requirement")) {
						MessageDialog.openError(parent.getShell(), "VERDICT Cyber-property Editor",
								"Entered cyber-requirement ID cannot be empty.");
					}

				} else if (!consistent) {

					if (purpose.equals("mission")) {
						MessageDialog.openError(parent.getShell(), "VERDICT Mission Editor",
								"Entered mission-ID matches the ID of an existing field. Try a different ID.");
					} else if (purpose.equals("relation")) {
						MessageDialog.openError(parent.getShell(), "VERDICT Cyber-property Editor",
								"Entered cyber-relation ID matches the ID of an existing field. Try a different ID.");
					} else if (purpose.equals("requirement")) {
						MessageDialog.openError(parent.getShell(), "VERDICT Cyber-property Editor",
								"Entered cyber-requirement ID matches the ID of an existing field. Try a different ID.");
					}

				} else if (externalMatch) {
					MessageDialog.openError(parent.getShell(), "VERDICT Mission Editor",
							"Entered mission-ID matches the ID of an existing field of another system/component in the same .aadl file. Try a different ID.");
				} else {
					if (purpose.equals("mission")) {
						MissionEditor.addIsValid = true;
						MissionEditor.newMissionName = getName.getText();
					} else {
						StatementEditor.addIsValid = true;
						StatementEditor.newPropertyName = getName.getText();
					}

					parent.getShell().close();
				}
			}
		});

		return composite;
	}

}