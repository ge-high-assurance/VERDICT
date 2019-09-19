package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.osate.aadl2.impl.DefaultAnnexSubclauseImpl;
import org.osate.aadl2.impl.SystemImplementationImpl;
import org.osate.aadl2.impl.SystemTypeImpl;

import com.ge.research.osate.verdict.dsl.verdict.Statement;
import com.ge.research.osate.verdict.dsl.verdict.Verdict;
import com.ge.research.osate.verdict.dsl.verdict.VerdictContractSubclause;
import com.ge.research.osate.verdict.dsl.verdict.impl.CyberMissionImpl;
import com.ge.research.osate.verdict.dsl.verdict.impl.CyberRelImpl;
import com.ge.research.osate.verdict.dsl.verdict.impl.CyberReqImpl;

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/

//this class creates the Wizard table dialog-box
public class StatementEditor extends ApplicationWindow {
	private boolean systemLevel;
	private boolean manualMode = false;
	protected static CellEditor[] editors = new CellEditor[4];
	private SystemTypeImpl selectedSys;
	private DrpDnLists drpdn;
	private WzrdTableLoader existingData;
	protected static List<WzrdTableRow> tableContent;
	protected static List<WzrdTableRow> deletedExistingContent;
	private boolean rowSelected = false;
	protected static String logicDefined = ""; // this variable is accessed only by LogicDefinitionEditor
	private IPath modelFile;
	private String editorContentCol0;
	private int editorSelectedIndexCol1;
	private int editorSelectedIndexCol2;
	protected static String editorContentCol3;
	protected static List<MissionInfo> missions = new ArrayList<MissionInfo>();
	private String missionString = "The defined 'Missions' are:\t\t\t\n";
	private Boolean valid = true;
	protected static String newPropertyName = null;
	protected static Boolean addIsValid = false;
	private WzrdCellEditorListener listener = new WzrdCellEditorListener();
	private Set<String> idSet = new HashSet<String>();

	// J-Face widget variables
	private Button addNewRow;
	protected static Button delete;
	private Button save;
	protected static Button noInput;
	protected static Button addDefinition;
	private Button auto;
	private Button manual;
	protected static TableViewer tv;
	private Shell lastShell;
	private Composite composite;
	private Rectangle sourceRect;
	private Button addEditMission;
	private Composite missionInfo;
	private Label missionLabel;
	private String callingClass;

	public StatementEditor(SystemTypeImpl sys, IPath sourceFile, Shell lastShell, Rectangle sourceRect,
			String callingClass) {
		super(lastShell);
		this.callingClass = callingClass;
		selectedSys = reloadSystem(sys);
		systemLevel = checkIfSystem(selectedSys);
		drpdn = new DrpDnLists(sys);
		if (drpdn.outPorts.length == 0) {
			if (!systemLevel) {
				MessageDialog.openError(lastShell, "VERDICT Wizard Launcher",
						"No output defined for the selected component. Cannot enter cyber-relation");
				valid = false;
			} else {

				MessageDialog.openError(lastShell, "VERDICT Wizard Launcher",
						"No output defined for the selected system. Cannot enter cyber-requirement");
				valid = false;
			}
		}
		existingData = new WzrdTableLoader(sys, systemLevel, drpdn);
		tableContent = existingData.getTableContent();
		if (systemLevel) {
			missions = loadExistingMissions(sys);
			updateMissionString(-1, null);
		}
		deletedExistingContent = new ArrayList<WzrdTableRow>();
		modelFile = sourceFile;
		editors[0] = null;
		editors[1] = null;
		editors[2] = null;
		editors[3] = null;
		this.lastShell = lastShell;
		this.sourceRect = sourceRect;
	}

	public void run() {
		setShellStyle(SWT.TITLE | SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.MAX | SWT.RESIZE);
		if (!callingClass.equals("dashboard")) {
			lastShell.setEnabled(false);
		}
		setBlockOnOpen(true);
		setBlockOnOpen(true);
		open();
		if (!lastShell.isDisposed()) {
			lastShell.open();
			lastShell.setFocus();
			lastShell.setEnabled(true);
		}
	}

	@Override
	// this defines all the widgets in the dialog-box
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Wizard: Cyber Property Editor for " + selectedSys.getFullName());
		shell.setSize(1300, 800);
		Rectangle branch = shell.getBounds();
		double x = (sourceRect.width - branch.width) * 0.5;
		double y = (sourceRect.height - branch.height) * 0.5;
		shell.setLocation((int) x, (int) y);
	}

	@Override
	protected Control createContents(Composite parent) {
		String[] props = new String[4];
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		Composite intro = new Composite(composite, SWT.BORDER);
		intro.setLayout(new GridLayout(1, false));

		if (!systemLevel) {
			Composite inPortComp = new Composite(intro, SWT.NONE);
			inPortComp.setLayout(new GridLayout(1, false));
			Label labelInPorts = new Label(inPortComp, SWT.NONE);
			labelInPorts.setText(drpdn.inStr);
			Composite outPortComp = new Composite(intro, SWT.NONE);
			outPortComp.setLayout(new GridLayout(1, false));
			Label labelOutPorts = new Label(outPortComp, SWT.NONE);
			labelOutPorts.setText(drpdn.outStr);
		} else {
			Label labelOutPorts = new Label(intro, SWT.NONE);
			labelOutPorts.setText(drpdn.outStr);
		}

		Composite missionSaveClose = new Composite(composite, SWT.BORDER);
		missionSaveClose.setLayout(new GridLayout(2, false));

		tv = new TableViewer(composite, SWT.BORDER | SWT.FULL_SELECTION);
		tv.setContentProvider(new WzrdTableContentProvider());
		tv.setLabelProvider(new WzrdTableLabelProvider(drpdn, systemLevel));
		tv.setInput(tableContent);
		tv.refresh();


		if (systemLevel) {
			missionInfo = new Composite(missionSaveClose, SWT.BORDER);
			missionInfo.setLayout(new GridLayout(2, true));
			missionLabel = new Label(missionInfo, SWT.NULL);
			missionLabel.setText("Total Missions: " + missions.size());
			missionLabel.setToolTipText(missionString);
			missionLabel.setLayoutData(new GridData(SWT.TOP, SWT.FILL, true, true));
			addEditMission = new Button(missionInfo, SWT.PUSH);
			addEditMission.setText("Add/Edit Mission");
			addEditMission.setToolTipText("Click here to add a new mission, or to edit existing missions");

			// defines what activity to perform when button clicked
			addEditMission.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					if (tableContent.size() > 0) {
						MissionEditor ldEditor = new MissionEditor(composite.getShell(), sourceRect, tableContent,
								missions, drpdn, idSet);
						composite.setEnabled(false);
						ldEditor.run();
						composite.setEnabled(true);
						updateMissionString(-1, null);
						missionLabel.setText("Total Missions: " + missions.size());
						missionLabel.setToolTipText(missionString);
						missionInfo.update();
						missionInfo.redraw();
					} else {
						MessageDialog.openError(parent.getShell(), "VERDICT Mission Editor",
								"In VERDICT, a mission is defined as a set of cyber-requirements. At least one cyber-requirement needs to be defined, prior defining a mission.");
					}
				}
			});
		} else {
			Composite autogen = new Composite(missionSaveClose, SWT.BORDER);
			autogen.setLayout(new GridLayout(5, false));
			Label autogenIntro = new Label(autogen, SWT.NULL);
			autogenIntro.setText("Auto-generate Cyber Relations: \t");
			GridData gd = new GridData();
			gd.horizontalSpan = 4;
			autogenIntro.setLayoutData(gd);
			Button autoGenerate = new Button(autogen, SWT.PUSH);
			autoGenerate.setText("Auto-Generate");
			autoGenerate.setToolTipText("Click to let Wizard auto-generate the cyber-relations for \n"
					+ "each output as disjunction of of all the inputs.");
			gd = new GridData(SWT.RIGHT, SWT.CENTER, true, true);
			gd.verticalSpan = 2;
			autoGenerate.setLayoutData(gd);
			Button confidentiality = new Button(autogen, SWT.CHECK);
			confidentiality.setText("Confidentiality (C)");
			Button integrity = new Button(autogen, SWT.CHECK);
			integrity.setText("Integrity (I)");
			Button availability = new Button(autogen, SWT.CHECK);
			availability.setText("Availability (A)");


			// defines what activity to perform when button clicked
			autoGenerate.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					Boolean ignore = false;
					List<Object> selections = new ArrayList<Object>();
					if (confidentiality.getSelection()) {
						selections.add(confidentiality.getText());
					}
					if (integrity.getSelection()) {
						selections.add(integrity.getText());
					}
					if (availability.getSelection()) {
						selections.add(availability.getText());
					}
					if (selections.size() == 0) {
						MessageDialog.openInformation(composite.getShell(), "VERDICT Wizard Info",
								"No CIA type selected. Select one (or more) CIA type(s) for auto-generation and then click \"Auto-Generate\"");
						return;
					}
					List<WzrdTableRow> addedRows = new ArrayList<WzrdTableRow>();
					for (Object str : selections.toArray()) {
						for (int i = 0; i < drpdn.outPorts.length; i++) {
							String formulaID = "";
							WzrdTableRow newRow = new WzrdTableRow();
							switch ((String) str) {
							case ("Confidentiality (C)"): {
								formulaID = selectedSys.getFullName() + "_" + drpdn.outPorts[i] + "_" + "C";
								break;
							}
							case ("Integrity (I)"): {
								formulaID = selectedSys.getFullName() + "_" + drpdn.outPorts[i] + "_" + "I";
								break;
							}
							case ("Availability (A)"): {
								formulaID = selectedSys.getFullName() + "_" + drpdn.outPorts[i] + "_" + "A";
								break;
							}
							}
							int result = -2;
							int j;
							for (j = 0; j < tableContent.size(); j++) {
								if (tableContent.get(j).getFormulaID().equals(formulaID)) {
									if (!ignore) {
										MessageDialog dialog = new MessageDialog(composite.getShell(), "Wizard Error",
												null,
												"Cyber-relation ID \"" + formulaID
														+ "\" generated by Wizard is already existing. "
														+ "Do you want to overwrite the existing content?",
												MessageDialog.ERROR,
												new String[] { "Yes to all", "Yes", "No", "Cancel" }, 0);
										result = dialog.open();
									} else {
										result = 1;
									}
									break;
								}
							}

							switch (result) {
							case (0): {
								ignore = true;
							}
							case (1): {
								WzrdTableRow rowToDelete = tableContent.get(j);
								if (!rowToDelete.isNew()) {
									try {
										deletedExistingContent.add((WzrdTableRow) rowToDelete.clone());
									} catch (Exception e) {
										System.out.println("Clone error in WzrdTableRow");
										e.printStackTrace();
									}
								}
								rowToDelete.setFormulaID(formulaID);
								rowToDelete.setFirstElement(drpdn.findIndex(drpdn.outPorts, drpdn.outPorts[i]));
								rowToDelete.setNewRow(true);
								switch ((String) str) {
								case ("Confidentiality (C)"): {
									rowToDelete.setSecondElement(drpdn.findIndex(drpdn.CIA, "Confidentiality (C)"));
									rowToDelete.setThirdElement(drpdn.autoGenConfidentiality);
									break;
								}
								case ("Integrity (I)"): {
									rowToDelete.setSecondElement(drpdn.findIndex(drpdn.CIA, "Integrity (I)"));
									rowToDelete.setThirdElement(drpdn.autoGenIntegrity);
									break;
								}
								case ("Availability (A)"): {
									rowToDelete.setSecondElement(drpdn.findIndex(drpdn.CIA, "Availability (A)"));
									rowToDelete.setThirdElement(drpdn.autoGenAvailability);
									break;
								}
								}
								continue;
							}
							case (2): {
								continue;
							}
							case (3): {
								return;
							}
							case (-1): {
								return;
							}
							}

							newRow.setFormulaID(formulaID);
							newRow.setFirstElement(drpdn.findIndex(drpdn.outPorts, drpdn.outPorts[i]));
							newRow.setNewRow(true);
							switch ((String) str) {
							case ("Confidentiality (C)"): {
								newRow.setSecondElement(drpdn.findIndex(drpdn.CIA, "Confidentiality (C)"));
								newRow.setThirdElement(drpdn.autoGenConfidentiality);
								break;
							}
							case ("Integrity (I)"): {
								newRow.setSecondElement(drpdn.findIndex(drpdn.CIA, "Integrity (I)"));
								newRow.setThirdElement(drpdn.autoGenIntegrity);
								break;
							}
							case ("Availability (A)"): {
								newRow.setSecondElement(drpdn.findIndex(drpdn.CIA, "Availability (A)"));
								newRow.setThirdElement(drpdn.autoGenAvailability);
								break;
							}
							}
							addedRows.add(newRow);
						}
					}
					for (int i = 0; i < addedRows.size(); i++) {
						tableContent.add(addedRows.get(i));
					}
					confidentiality.setSelection(false);
					integrity.setSelection(false);
					availability.setSelection(false);
					tableRefresh();
					delete.setEnabled(true);
					noInput.setEnabled(true);
					addDefinition.setEnabled(true);
				}
			});
		}

		save = new Button(missionSaveClose, SWT.PUSH);
		save.setText("Save + Close");
		save.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, true));
		save.setToolTipText(
				"Click here to save all the entries for '" + selectedSys.getFullName() + "'" + " and close Wizard");

		Composite cyberPropertyTools = new Composite(missionSaveClose, SWT.BORDER);
		cyberPropertyTools.setLayout(new GridLayout(1, false));
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.horizontalSpan = 3;
		cyberPropertyTools.setLayoutData(gridData);

		Composite addDelete = new Composite(cyberPropertyTools, SWT.NONE);
		addDelete.setLayout(new GridLayout(4, false));

		Label labelAddDelete = new Label(addDelete, SWT.NONE);
		if (systemLevel) {
			labelAddDelete.setText("Add/Delete Cyber Requirement:\t");
		} else {
			labelAddDelete.setText("Add/Delete Cyber Relation:\t");
		}

		addNewRow = new Button(addDelete, SWT.PUSH);
		addNewRow.setText("Add");
		if (systemLevel) {
			addNewRow.setToolTipText("Click here to add a new cyber requirement");
		} else {
			addNewRow.setToolTipText("Click here to add a new cyber relation");
		}

		delete = new Button(addDelete, SWT.PUSH);
		delete.setText("Delete");
		delete.setToolTipText("Click here to delete the selected row");

		Composite autoManual = new Composite(cyberPropertyTools, SWT.NONE);
		autoManual.setLayout(new GridLayout(3, false));
		Label labelAutoManual = new Label(autoManual, SWT.NONE);
		if (systemLevel) {
			labelAutoManual.setText("Preferred Way of Entering Cyber Requirement:\t");
		} else {
			labelAutoManual.setText("Preferred Way of Entering Cyber Relation:\t");
		}

		auto = new Button(autoManual, SWT.RADIO);
		auto.setText("GUI");
		auto.setSelection(true);
		if (systemLevel) {
			auto.setToolTipText("select 'GUI' to enter a cyber relation using wizard");
		} else {
			auto.setToolTipText("select 'GUI' to enter a cyber requirement using wizard");
		}

		manual = new Button(autoManual, SWT.RADIO);
		manual.setText("Manual");
		if (systemLevel) {
			manual.setToolTipText("select 'Manual' to enter a cyber relation manually");
		} else {
			manual.setToolTipText("select 'Manual' to enter a cyber requirement manually");
		}

		Composite definitionEditor;
		definitionEditor = new Composite(cyberPropertyTools, SWT.NONE);
		definitionEditor.setLayout(new GridLayout(3, false));
		if (systemLevel) {
			Label definitionEditorLabel = new Label(definitionEditor, SWT.NONE);
			definitionEditorLabel.setText("Edit CyberReq Content in GUI Mode:\t");
		} else {
			Label definitionEditorLabel = new Label(definitionEditor, SWT.NONE);
			definitionEditorLabel.setText("Edit CyberRel Content in GUI Mode:\t");
		}

		addDefinition = new Button(definitionEditor, SWT.PUSH);
		if (systemLevel) {
			addDefinition.setText("Edit CyberReq Content");
			addDefinition.setToolTipText("Click here to enter 'Logical Condition' wizard");
		} else {
			addDefinition.setText("Edit CyberRel Content");
			addDefinition.setToolTipText(
					"Click here to define cyber relation of selected OUTport security concern in terms of its INports");
		}

		noInput = new Button(definitionEditor, SWT.PUSH);
		noInput.setText("Always True");
		noInput.setToolTipText("Click here if the selected OUTport security concern is always true");

		if (systemLevel) {
			noInput.setVisible(false);
		}

		Table table = tv.getTable();
		table.setHeaderBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT));
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		if (systemLevel) {
			props[0] = "Cyber Requirement ID";
			props[1] = "Security Concern";
			props[2] = "Severity";
			props[3] = "CyberReq Content";
			editors[0] = new TextCellEditor(table);
			editors[1] = new ComboBoxCellEditor(table, drpdn.CIA, SWT.READ_ONLY);
			editors[2] = new ComboBoxCellEditor(table, drpdn.SEVERITY, SWT.READ_ONLY);
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Cyber Requirement ID");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Security Concern");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Severity");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("CyberReq Content");
		} else {
			props[0] = "Cyber Relation ID";
			props[1] = "OUT Port";
			props[2] = "Security Concern";
			props[3] = "CyberRel Content";
			editors[0] = new TextCellEditor(table);
			editors[1] = new ComboBoxCellEditor(table, drpdn.outPorts, SWT.READ_ONLY);
			editors[2] = new ComboBoxCellEditor(table, drpdn.CIA, SWT.READ_ONLY);
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Cyber Relation ID");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("OUT Port");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Security Concern");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("CyberRel Content");
		}


		table.getColumn(0).setWidth(200);
		table.getColumn(0).setAlignment(SWT.CENTER);
		table.getColumn(1).setWidth(200);
		table.getColumn(1).setAlignment(SWT.CENTER);
		table.getColumn(2).setWidth(200);
		table.getColumn(2).setAlignment(SWT.CENTER);
		table.getColumn(3).setWidth(700);
		table.getColumn(3).setAlignment(SWT.CENTER);

		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		tableRefresh();

		Label blank = new Label(composite, SWT.NULL);
		blank.setText("");

		// defines what activity to perform when button clicked
		addNewRow.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				WzrdTableRow addedRow = new WzrdTableRow();
				addedRow.setFormulaID("Enter ID");
				newPropertyName = null;
				addedRow.setFirstElement(Integer.valueOf("0"));
				addedRow.setSecondElement(Integer.valueOf("0"));
				if (manualMode) {
					addedRow.setThirdElement("");
				} else {
					if (!systemLevel) {
						addedRow.setThirdElement(
								"Click 'Define logically' or 'Always True' to add content through GUI, or select 'Manual' to enter manually");
					} else {
						addedRow.setThirdElement(
								"Click 'Define logically' to add content through GUI, or select 'Manual' to enter manually");
					}
				}
				tableContent.add(addedRow);
				tableRefresh();
			}
		});

		// defines what activity to perform when button clicked
		auto.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				manualMode = false;
				auto.setSelection(true);
				manual.setSelection(false);
				if (editors[3] != null) {
					editors[3].removeListener(listener);
					editors[3] = null;
				}
				editorContentCol3 = null;
				tableRefresh();
			}
		});

		// defines what activity to perform when button clicked
		manual.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				manualMode = true;
				auto.setSelection(false);
				manual.setSelection(true);
				editors[3] = new TextCellEditor(table);
				editorContentCol3 = (String) editors[3].getValue();
				editors[3].addListener(listener);
				tableRefresh();
			}
		});

		// defines what activity to perform when button clicked
		delete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// If nothing is selected, return;
				if (tv.getTable().getSelectionIndex() < 0) {
					MessageDialog.openInformation(lastShell, "VERDICT Wizard Info",
							"No row selected. Select a row and click \"Delete\" to delete the row.");
					return;
				}

				WzrdTableRow rowToDelete = tableContent.get(tv.getTable().getSelectionIndex());
				if (!rowToDelete.isNew()) {
					try {
						deletedExistingContent.add((WzrdTableRow) rowToDelete.clone());
					} catch (Exception e) {
						System.out.println("Clone error in WzrdTableRow");
						e.printStackTrace();
					}
				}
				tableContent.remove(tv.getTable().getSelectionIndex());
				if (tableContent.size() == 0) {
					rowSelected = false;
				}

				for (int i = 0; i < missions.size(); i++) {
					Set<Integer> rows = missions.get(i).getRow();
					Set<Integer> newRows = new HashSet<Integer>();
					if (rows.contains(tv.getTable().getSelectionIndex())) {
						rows.remove(tv.getTable().getSelectionIndex());
					}
					Iterator<Integer> elements = rows.iterator();
					while (elements.hasNext()) {
						int k = elements.next();
						if (k > tv.getTable().getSelectionIndex()) {
							newRows.add(k - 1);
						} else if (k == tv.getTable().getSelectionIndex()) {
							// do nothing
						} else {
							newRows.add(k);
						}
					}
					missions.get(i).setRow(newRows);
				}
				if (systemLevel) {
					updateMissionString(tv.getTable().getSelectionIndex(), "delete");
					missionLabel.setToolTipText(missionString);
					missionInfo.update();
					missionInfo.redraw();
				}
				tableRefresh();
			}
		});

		// defines what activity to perform when button clicked
		noInput.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {

				// If nothing is selected, return;
				if (tv.getTable().getSelectionIndex() < 0) {
					MessageDialog.openInformation(lastShell, "VERDICT Wizard Info",
							"No row selected. Select a row and click \"Always True\" to set the output security concern as always true.");
					return;
				}

				if (!checkConsistency(tableContent, tv.getTable().getSelectionIndex())) {
					MessageDialog.openError(composite.getShell(), "Wizard Error",
							"Two rows cannot have identical content in the first two columns");
				} else {
					WzrdTableRow rowToDelete = tableContent.get(tv.getTable().getSelectionIndex());
					if (!rowToDelete.isNew()) {
						try {
							deletedExistingContent.add((WzrdTableRow) rowToDelete.clone());
						} catch (Exception e) {
							System.out.println("Clone error in WzrdTableRow");
							e.printStackTrace();
						}
					}
					rowToDelete.setThirdElement("TRUE");
					rowToDelete.setNewRow(true);
				}
				tableRefresh();
			}
		});

		// defines what activity to perform when button clicked
		addDefinition.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// If nothing is selected, return;
				if (tv.getTable().getSelectionIndex() < 0) {
					if (systemLevel) {
						MessageDialog.openInformation(lastShell, "VERDICT Wizard Info",
								"No row selected. Select a row and click \"Edit CyberReq content\" to edit cyber-requirement.");
					} else {
						MessageDialog.openInformation(lastShell, "VERDICT Wizard Info",
								"No row selected. Select a row and click \"Edit CyberRel content\" to edit cyber-relation.");
					}
					return;
				}
				if (!checkConsistency(tableContent, tv.getTable().getSelectionIndex())) {
					MessageDialog.openError(composite.getShell(), "Wizard Error",
							"Two rows cannot have identical content in the first two columns");
				} else {
					WzrdTableRow rowToDelete = new WzrdTableRow();
					rowToDelete = tableContent.get(tv.getTable().getSelectionIndex());
					if (!rowToDelete.isNew()) {
						try {
							deletedExistingContent.add((WzrdTableRow) rowToDelete.clone());
						} catch (Exception e) {
							System.out.println("Clone error in WzrdTableRow");
							e.printStackTrace();
						}
					}

					LogicDefinitionEditor ldEditor = new LogicDefinitionEditor(composite.getShell(), drpdn, systemLevel,
							sourceRect);
					if (ldEditor.isValid()) {
						composite.setEnabled(false);
						ldEditor.run();
						composite.setEnabled(true);
					} else {
						return;
					}
					if (logicDefined != "") {
						tableContent.get(tv.getTable().getSelectionIndex()).setThirdElement(logicDefined);
						rowToDelete.setNewRow(true);
						logicDefined = "";
					}
				}
				tableRefresh();
			}
		});

		// defines what activity to perform when button clicked
		save.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				WzrdClosure closing = new WzrdClosure(selectedSys, tableContent, deletedExistingContent, modelFile,
						systemLevel, drpdn, composite, missions, parent.getShell());
				if (!closing.isAborted()) {
					composite.getShell().close();
				} else {
					tableContent = closing.getBackUp();
					tv.setInput(tableContent);
					tableRefresh();
				}
			}
		});

		tv.getTable().addListener(SWT.Selection, e -> {
			editorSelectedIndexCol1 = (int) editors[1].getValue();
			editorSelectedIndexCol2 = (int) editors[2].getValue();
			if (manual.getSelection()) {
				editorContentCol3 = (String) editors[3].getValue();
			}

			if (!rowSelected) {
				tableRefresh();
				rowSelected = true;
			}
		});

		editors[0].addListener(new ICellEditorListener() {
			private int previousSelection = -1;

			@Override
			public void applyEditorValue() {
				breakout: if (editorContentCol0 != (String) editors[0].getValue()) {

//					for (int i = 0; i < tableContent.size(); i++) {
//						if (i != previousSelection && i != tv.getTable().getSelectionIndex()
//								&& tableContent.get(i).getFormulaID().equals(editors[0].getValue())) {
//							if (systemLevel) {
//								MessageDialog.openError(composite.getShell(), "VERDICT Wizard Error",
//										"Entered ID is identical to one of the existing cyber-requirement IDs. Try a different ID.");
//								tv.refresh();
//								break breakout;
//							} else {
//								MessageDialog.openError(composite.getShell(), "VERDICT Wizard Error",
//										"Entered ID is identical to one of the existing cyber-relation IDs. Try a different ID.");
//								tv.refresh();
//								break breakout;
//							}
//						}
//					}

//					for (int i = 0; i < missions.size(); i++) {
//						if (i != previousSelection && i != tv.getTable().getSelectionIndex()
//								&& missions.get(i).getMissionID().equals(editors[0].getValue())) {
//							MessageDialog.openError(composite.getShell(), "VERDICT Wizard Error",
//									"Entered ID is identical to one of the existing mission IDs. Try a different ID.");
//							tv.refresh();
//							break breakout;
//
//						}
//					}

					String previousId = previousSelection != -1 && previousSelection < tableContent.size()
							? tableContent.get(previousSelection).getFormulaID()
							: null;
					boolean unchanged = previousId != null && previousId.equals(editors[0].getValue());

//					if (!unchanged && idSet.contains(editors[0].getValue())) {
//						MessageDialog.openError(composite.getShell(), "VERDICT Wizard Error",
//								"Entered ID matches the ID of an existing field of another system/component in the same .aadl file. Try a different ID.");
//						tv.refresh();
//						break breakout;
//					}

					WzrdTableRow rowToDelete = tableContent.get(tv.getTable().getSelectionIndex());
					if (!rowToDelete.isNew()) {
						try {
							deletedExistingContent.add((WzrdTableRow) rowToDelete.clone());
						} catch (Exception e) {
							System.out.println("Clone error in WzrdTableRow");
							e.printStackTrace();
						}
					}
					rowToDelete.setNewRow(true);
					if (systemLevel) {
						updateMissionString(tv.getTable().getSelectionIndex(), null);
						missionLabel.setToolTipText(missionString);
						missionInfo.update();
						missionInfo.redraw();
					}

					editorContentCol0 = (String) editors[0].getValue();
					delete.setEnabled(true);
					noInput.setEnabled(true);
					addDefinition.setEnabled(true);
				}

				previousSelection = tv.getTable().getSelectionIndex();
			}

			@Override
			public void cancelEditor() {
				// ignore
			}

			@Override
			public void editorValueChanged(boolean oldValidState, boolean newValidState) {
				// ignore
			}
		});

		// when entry of first column of previously entered row is changed,
		// delete the corresponding entry from the data-structure
		editors[1].addListener(new ICellEditorListener() {
			@Override
			public void applyEditorValue() {
				if (editorSelectedIndexCol1 != (int) editors[1].getValue()) {
					WzrdTableRow rowToDelete = tableContent.get(tv.getTable().getSelectionIndex());
					if (!rowToDelete.isNew()) {
						try {
							deletedExistingContent.add((WzrdTableRow) rowToDelete.clone());
						} catch (Exception e) {
							System.out.println("Clone error in WzrdTableRow");
							e.printStackTrace();
						}
					}
					rowToDelete.setNewRow(true);
					editorSelectedIndexCol1 = (int) editors[1].getValue();
					delete.setEnabled(true);
					noInput.setEnabled(true);
					addDefinition.setEnabled(true);
				}
			}

			@Override
			public void cancelEditor() {
				// ignore
			}

			@Override
			public void editorValueChanged(boolean oldValidState, boolean newValidState) {
				// ignore
			}
		});

		// when entry of second column of previously entered row is changed,
		// delete the corresponding entry from the data-structure
		editors[2].addListener(new ICellEditorListener() {
			@Override
			public void applyEditorValue() {
				if (editorSelectedIndexCol2 != (int) editors[2].getValue()) {
					WzrdTableRow rowToDelete = tableContent.get(tv.getTable().getSelectionIndex());
					if (!rowToDelete.isNew()) {
						try {
							deletedExistingContent.add((WzrdTableRow) rowToDelete.clone());
						} catch (Exception e) {
							System.out.println("Clone error in WzrdTableRow");
							e.printStackTrace();
						}
					}
					rowToDelete.setNewRow(true);
					editorSelectedIndexCol2 = (int) editors[2].getValue();
					delete.setEnabled(true);
					noInput.setEnabled(true);
					addDefinition.setEnabled(true);
				}
			}

			@Override
			public void cancelEditor() {
				// ignore
			}

			@Override
			public void editorValueChanged(boolean oldValidState, boolean newValidState) {
				// ignore
			}
		});

		tv.setColumnProperties(props);
		tv.setCellModifier(new WzrdTableCellModifier(tv, props));
		tv.setCellEditors(editors);

		return composite;
	}

	// check if the contents of first two columns of new row are identical to
	// those of a previous row
	private boolean checkConsistency(List<WzrdTableRow> tableContent, int id) {
		boolean consistency = true;
		for (int i = 0; i < tableContent.size(); i++) {
			if (i != id) {
				if ((tableContent.get(i).getFirstElement().equals(tableContent.get(id).getFirstElement()))
						& (tableContent.get(i).getSecondElement().equals(tableContent.get(id).getSecondElement()))) {
					consistency = false;
					break;
				}
			}
		}
		return consistency;
	}

	// check if a component is "top-level" system
	private boolean checkIfSystem(SystemTypeImpl sys) {
		boolean sysLevel = false;
		String systemName;
		TreeIterator<EObject> tree = sys.eResource().getAllContents();

		while (tree.hasNext()) {
			EObject tmp = tree.next();

			if (tmp instanceof SystemImplementationImpl) {
				systemName = ((SystemImplementationImpl) tmp).getTypeName();
				if (sys.getName().equals(systemName)) {
					sysLevel = true;
					break;
				}
			}
		}
		return sysLevel;
	}

	// refreshes all widgets in the dialog-box is desired way
	private void tableRefresh() {
		if (tv.getTable().getSelectionIndex() < 0) {
			delete.setEnabled(false);
			noInput.setEnabled(false);
			addDefinition.setEnabled(false);
		} else {
			if (tableContent.size() > 0) {
				delete.setEnabled(true);
				if (!manualMode) {
					addDefinition.setEnabled(true);
				} else {
					addDefinition.setEnabled(false);
				}
				if (!systemLevel) {
					if (!manualMode) {
						noInput.setEnabled(true);
					} else {
						noInput.setEnabled(false);
					}
				} else {
					noInput.setEnabled(false);
				}
				save.setEnabled(true);
			} else {
				delete.setEnabled(false);
				addDefinition.setEnabled(false);
				noInput.setEnabled(false);
				if (deletedExistingContent.size() > 0) {
					save.setEnabled(true);
				} else {
					save.setEnabled(false);
				}
			}
		}
		tv.refresh();
	}

	public Boolean isValid() {
		return valid;
	}

	// reload the system content to take care of any change saved by used in
	// .aadl script in course of current Wizard session
	private List<MissionInfo> loadExistingMissions(SystemTypeImpl sys) {
		List<MissionInfo> missions = new ArrayList<MissionInfo>();
		TreeIterator<EObject> tree = sys.eAllContents();
		while (tree.hasNext()) {
			EObject tmp = tree.next();
			if (tmp instanceof DefaultAnnexSubclauseImpl) {
				if (!((DefaultAnnexSubclauseImpl) tmp).getName().equals("verdict")) {
					continue;
				}
				Verdict vd = ((VerdictContractSubclause) ((DefaultAnnexSubclauseImpl) tmp).getParsedAnnexSubclause())
						.getContract();
				List<Statement> stmts = vd.getElements();
				for (int i = 0; i < stmts.size(); i++) {
					if (stmts.get(i) instanceof CyberMissionImpl) {
						MissionInfo newMission = new MissionInfo();
						newMission.setMissionID(((CyberMissionImpl) stmts.get(i)).getId());
						List<String> cyberReqs = ((CyberMissionImpl) stmts.get(i)).getCyberReqs();
						for (int j = 0; j < cyberReqs.size(); j++) {
							for (int k = 0; k < tableContent.size(); k++) {
								if (tableContent.get(k).getFormulaID().equals(cyberReqs.get(j))) {
									newMission.addToRow(k);
								}
							}
						}
						newMission.setComment(((CyberMissionImpl) stmts.get(i)).getNote());
						newMission.setDescription(((CyberMissionImpl) stmts.get(i)).getDescription());
						missions.add(newMission);
					}
				}

			}
		}
		return missions;
	}

	private void updateMissionString(int currIndex, String str) {
		missionString = "The defined 'Missions' are:\t\t\t\n";
		for (int i = 0; i < missions.size(); i++) {
			missionString = missionString + Integer.toString(i + 1) + ". " + missions.get(i).getMissionID();
			missionString = missionString + " (cyber-requirements: ";
			Iterator<Integer> iterator = missions.get(i).getRow().iterator();
			Boolean start = true;
			while (iterator.hasNext()) {
				if (!start) {
					missionString = missionString + ", ";
				}
				int index = iterator.next();
				if (currIndex > 0) {
					if (index == currIndex) {
						missionString = missionString + ((String) editors[0].getValue());
					} else {
						if (index > currIndex) {
							if (str != null) {
								missionString = missionString + tableContent.get(index - 1).getFormulaID();
							} else {
								missionString = missionString + tableContent.get(index).getFormulaID();
							}
						} else {
							missionString = missionString + tableContent.get(index).getFormulaID();
						}
					}
				} else if (index < tableContent.size()) {
					missionString = missionString + tableContent.get(index).getFormulaID();
				}
				start = false;
			}
			missionString = missionString + ")";
			if (i != missions.size() - 1) {
				missionString = missionString + "\n";
			}
		}
	}

	private SystemTypeImpl reloadSystem(SystemTypeImpl sys) {
		Resource oldResource = sys.eResource();
		ResourceSetImpl resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry();
		Resource resource = resourceSet.createResource(oldResource.getURI());
		try {
			resource.load(null);
		} catch (Exception e) {
			System.out.println("Error in reloading resource while saving content by Wizard.");
			e.printStackTrace();
		}
		TreeIterator<EObject> tree = resource.getAllContents();
		while (tree.hasNext()) {
			EObject anObject = tree.next();
			if (anObject instanceof SystemTypeImpl) {
				if (((SystemTypeImpl) anObject).getFullName().equals(sys.getFullName())) {
					sys = (SystemTypeImpl) anObject;
				}
			}

			// extract the existing set of IDs that are already used------------------------------------------------------
			if (anObject instanceof DefaultAnnexSubclauseImpl) {
				if (!((DefaultAnnexSubclauseImpl) anObject).getName().equals("verdict")) {
					continue;
				}
				Verdict vd = ((VerdictContractSubclause) ((DefaultAnnexSubclauseImpl) anObject)
						.getParsedAnnexSubclause()).getContract();
				List<Statement> stmts = vd.getElements();
				for (int i = 0; i < stmts.size(); i++) {
					if (stmts.get(i) instanceof CyberMissionImpl) {
						idSet.add(((CyberMissionImpl) stmts.get(i)).getId());
					} else if (stmts.get(i) instanceof CyberRelImpl) {
						idSet.add(((CyberRelImpl) stmts.get(i)).getId());
					} else if (stmts.get(i) instanceof CyberReqImpl) {
						idSet.add(((CyberReqImpl) stmts.get(i)).getId());
					}
				}
			}
			// ------------------------------------------------------------------------------------------------------------
		}
		return sys;
	}
}