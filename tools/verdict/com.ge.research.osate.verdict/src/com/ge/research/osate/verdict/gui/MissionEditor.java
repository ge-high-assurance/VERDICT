package com.ge.research.osate.verdict.gui;

import java.util.List;
import java.util.Set;
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

/**
 *
 * Author: Soumya Talukder
 * Date: Jul 18, 2019
 *
 */

// this class generates the Logical Definition editor dialog box for wizard
public class MissionEditor extends ApplicationWindow {
    private Shell lastShell;
    private DrpDnLists drpdn;
    private List<WzrdTableRow> tableContents;
    private List<MissionInfo> missions;
    protected static String newMissionName = null;
    protected static Boolean addIsValid = false;
    private Composite[] compositeSet;
    private Button[] requirements;
    private Set<String> idSet;

    // Jface variables
    private Composite composite;
    private Rectangle sourceRect;
    private Composite rootComposite;

    public MissionEditor(
            Shell shell,
            Rectangle sourceRect,
            List<WzrdTableRow> tableContents,
            List<MissionInfo> missions,
            DrpDnLists drpdn,
            Set<String> idSet) {
        super(shell);
        lastShell = shell;
        this.sourceRect = sourceRect;
        this.tableContents = tableContents;
        this.missions = missions;
        this.drpdn = drpdn;
        this.idSet = idSet;
        compositeSet = new Composite[missions.size()];
        requirements = new Button[tableContents.size()];
    }

    public MissionEditor(
            Shell shell,
            Shell generatingShell,
            Rectangle sourceRect,
            List<WzrdTableRow> tableContents,
            List<MissionInfo> missions,
            DrpDnLists drpdn) {
        super(shell);
        generatingShell.close();
        lastShell = shell;
        lastShell.setFocus();
        this.sourceRect = sourceRect;
        this.tableContents = tableContents;
        this.missions = missions;
        this.drpdn = drpdn;
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
        shell.setText("Wizard: Mission Editor");
        double x = (sourceRect.width - 500) * 0.5;
        double y = (sourceRect.height - 500) * 0.5;
        shell.setLocation((int) x, (int) y);
    }

    // this defines all the widgets in the dialog-box
    @Override
    protected Control createContents(Composite parent) {
        parent.getShell().setFocus();
        rootComposite = parent;
        composite = new Composite(parent, SWT.FILL);
        composite.setLayout(new GridLayout(1, false));

        if (missions.size() == 0) {
            Label emptyLabel = new Label(composite, SWT.NULL);
            emptyLabel.setText("No mission defined. Click 'Add' to add a new mission");
        } else {
            for (int i = 0; i < missions.size(); i++) {
                createMissionComposite(missions.get(i), i);
            }
        }

        Composite ending = new Composite(composite, SWT.NULL);
        ending.setLayout(new GridLayout(2, true));
        ending.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));

        Button add = new Button(ending, SWT.PUSH);
        add.setText("Add");
        add.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));

        Button done = new Button(ending, SWT.PUSH);
        done.setText("Done");
        done.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));

        // defines what activity to perform when button clicked
        add.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                (new NewIDProvider(composite.getShell(), sourceRect, missions, tableContents, "mission")).run();
                if (addIsValid) {
                    MissionInfo newMission = new MissionInfo();
                    newMission.setMissionID(newMissionName);
                    missions.add(newMission);
                    parent.getShell().close();
                    MissionEditor ldEditor =
                            new MissionEditor(lastShell, sourceRect, tableContents, missions, drpdn, idSet);
                    ldEditor.run();
                    addIsValid = false;
                    newMissionName = null;
                }
            }
        });

        // defines what activity to perform when button clicked
        done.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                StatementEditor.missions = missions;
                parent.getShell().close();
            }
        });

        parent.update();
        return composite;
    }

    private void createMissionComposite(MissionInfo mission, int ii) {
        Composite missionComposite = new Composite(composite, SWT.BORDER);
        missionComposite.setLayout(new GridLayout(2, true));
        missionComposite.setData("missionID", mission.getMissionID());

        Composite lhs = new Composite(missionComposite, SWT.NULL);
        lhs.setLayout(new GridLayout(1, false));
        lhs.setLayoutData(new GridData(SWT.TOP, SWT.FILL, true, true));

        Label missionName = new Label(lhs, SWT.TOP);
        missionName.setText(mission.getMissionID());
        missionName.setLayoutData(new GridData(SWT.TOP, SWT.FILL, true, true));

        Button delete = new Button(lhs, SWT.NULL);
        delete.setText("Delete");
        delete.setData(mission.getMissionID());

        Composite requirementSet = new Composite(missionComposite, SWT.NULL);
        requirementSet.setLayout(new GridLayout(1, false));

        for (int j = 0; j < tableContents.size(); j++) {
            Button aButton = new Button(requirementSet, SWT.CHECK);
            String buttonText = tableContents.get(j).getFormulaID();
            aButton.setText(buttonText);
            aButton.setData(mission.getMissionID());
            if (mission.getRow().contains(j)) {
                aButton.setSelection(true);
            }
            requirements[j] = aButton;

            // defines what activity to perform when button clicked
            aButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    int idInTable = -1;
                    int idInMissionList = -1;

                    for (int i = 0; i < requirements.length; i++) {
                        if (requirements[i].getText().equals(aButton.getText())) {
                            idInTable = i;
                            break;
                        }
                    }

                    for (int i = 0; i < compositeSet.length; i++) {
                        if (compositeSet[i].getData("missionID").equals(aButton.getData())) {
                            idInMissionList = i;
                            break;
                        }
                    }

                    if (aButton.getSelection()) {
                        missions.get(idInMissionList).addToRow(idInTable);
                    } else {
                        missions.get(idInMissionList).removeRow(idInTable);
                    }
                }
            });
        }

        // defines what activity to perform when button clicked
        delete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                int idInMissionList = -1;
                for (int i = 0; i < compositeSet.length; i++) {
                    if (compositeSet[i].getData("missionID").equals(delete.getData())) {
                        idInMissionList = i;
                        break;
                    }
                }
                missions.remove(idInMissionList);
                rootComposite.getShell().close();
                MissionEditor ldEditor =
                        new MissionEditor(lastShell, sourceRect, tableContents, missions, drpdn, idSet);
                ldEditor.run();
            }
        });
        compositeSet[ii] = missionComposite;
    }
}
