package com.ge.research.osate.verdict.gui;

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

import java.util.List;

/** Author: Soumya Talukder Date: Jul 18, 2019 */

// this class generates the Logical Definition editor dialog box for wizard
public class NewMissionName extends ApplicationWindow {
    private Shell lastShell;
    private List<MissionInfo> missions;

    // Jface variables
    private Composite composite;
    private Rectangle sourceRect;

    public NewMissionName(Shell shell, Rectangle sourceRect, List<MissionInfo> missions) {
        super(shell);
        lastShell = shell;
        this.sourceRect = sourceRect;
        this.missions = missions;
        MissionEditor.addIsValid = false;
    }

    public void run() {
        lastShell.setEnabled(false);
        setBlockOnOpen(true);
        open();
        if (!lastShell.isDisposed()) {
            lastShell.redraw();
            lastShell.setEnabled(true);
        }
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Wizard: New Mission");
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
        begin.setText("             Enter mission-name here:           ");

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
        cancel.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        MissionEditor.addIsValid = false;
                        parent.getShell().close();
                    }
                });

        // defines what activity to perform when button clicked
        ok.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        Boolean consistent = true;
                        for (int i = 0; i < missions.size(); i++) {
                            if (missions.get(i).getMissionID().equals(getName.getText())) {
                                consistent = false;
                                break;
                            }
                        }
                        if (getName.getText().equals("") || getName.getText().equals(null)) {
                            MessageDialog.openError(
                                    parent.getShell(),
                                    "VERDICT Mission Editor",
                                    "Entered mission-name cannot be empty.");
                        } else if (!consistent) {
                            MessageDialog.openError(
                                    parent.getShell(),
                                    "VERDICT Mission Editor",
                                    "Entered mission-name matches the name of an existing mission. Try a different name.");
                        } else {
                            MissionEditor.addIsValid = true;
                            MissionEditor.newMissionName = getName.getText();
                            parent.getShell().close();
                        }
                    }
                });

        return composite;
    }
}
