package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
//import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/
//this class creates the MBAS Result viewer-tab in OSATE
public class MBASResultsView extends ViewPart {
	private Composite composite;
	public static final String ID = "com.ge.research.osate.verdict.gui.mbasResultsView";
	public static List<MissionAttributes> missions = new ArrayList<MissionAttributes>();
	public static Map<String, List<MBASSafetyResult>> safetyResults;


	public MBASResultsView() {
		super();
	}

	@Override
	public void setFocus() {
		if (composite != null) {
			composite.setFocus();
		}
	}

	private Image getSuccessOrFailImage(boolean success) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("com.ge.research.osate.verdict",
				success ? "icons/valid.png" : "icons/false.png").createImage();
	}

	@Override
	public void createPartControl(Composite parent) {

		ScrolledComposite scrollArea = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);

		composite = new Composite(scrollArea, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		scrollArea.setContent(composite);

		for (int ii = 0; ii < missions.size(); ii++) {

			Label label = new Label(composite, SWT.BOLD);
			FontDescriptor descriptor = FontDescriptor.createFrom(label.getFont());
			descriptor = descriptor.setStyle(SWT.BOLD);
			label.setFont(descriptor.createFont(Display.getCurrent()));
			label.setText("Mission # " + (ii + 1) + ": "
					+ missions.get(ii).getMission() + " -> " + missions.get(ii).getMissionStatus());
			if (missions.get(ii).getMissionStatus().equals("Succeeded")) {
				label.setForeground(new Color(Display.getCurrent(), 0, 102, 51));
			} else {
				label.setForeground(new Color(Display.getCurrent(), 204, 0, 0));
			}

			List<MBASSummaryRow> tableContents = missions.get(ii).getTableContents();

			String missionName = missions.get(ii).getMission();
			List<MBASSafetyResult> safetyItems;
			if (safetyResults.containsKey(missionName)) {
				safetyItems = safetyResults.get(missionName);
			} else {
				safetyItems = Collections.emptyList();
			}

			if (tableContents.size() > 0) {
				Table table = new Table(composite, SWT.MULTI | SWT.FULL_SELECTION);
				table.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				table.setHeaderBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT));
				table.setHeaderForeground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_FOREGROUND));

				new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Cyber Requirement");
				new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Acceptable Likelihood of Successful Attack");
				new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Calculated Likelihood of Successful Attack");
				new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Analysis Result");

				List<String> keys = new ArrayList<String>();
				List<String> answers = new ArrayList<String>();

				for (int i = 0; i < tableContents.size(); i++) {
					TableItem item = new TableItem(table, SWT.NONE);
					List<String> currRow = tableContents.get(i).getRowContents();
					item.setText(new String[] { currRow.get(0), currRow.get(1), currRow.get(2), " " });
					keys.add(currRow.get(0));
					answers.add(currRow.get(3));
				}

				// this listener center-aligns the pictorial results in table
				table.addListener(SWT.PaintItem, event -> {
					if (event.index == 3) {
						Image image = null;
						for (int i = 0; i < missions.size(); i++) {
							List<RequirementAttributes> requirements = missions.get(i).getRequirements();
							for (int j = 0; j < requirements.size(); j++) {
								if (requirements.get(j).getRequirement().equals(((TableItem) event.item).getText(0))) {
									image = getSuccessOrFailImage(requirements.get(j).hasSucceeded());
									break;
								}
							}
						}

						int tmpX = 0;
						int tmpY = 0;

						int tmpWidth = table.getColumn(event.index).getWidth();
						int tmpHeight = ((TableItem) event.item).getBounds().height;

						tmpX = image.getBounds().width;
						tmpX = (tmpWidth / 2 - tmpX / 2);
						tmpY = image.getBounds().height;
						tmpY = (tmpHeight / 2 - tmpY / 2);
						if (tmpX <= 0) {
							tmpX = event.x;
						} else {
							tmpX += event.x;
						}
						if (tmpY <= 0) {
							tmpY = event.y;
						} else {
							tmpY += event.y;
						}

						event.gc.drawImage(image, tmpX, tmpY);
					}
				});

				// this listener creates context menu for viewing vulnerability/defense
				table.addMouseListener(new MouseListener() {
					@Override
					public void mouseUp(MouseEvent e) {
					}

					@Override
					public void mouseDown(MouseEvent e) {
						if (table.getSelectionIndex() == -1) {
							// If the table is empty
							return;
						}
						if (e.button == 3) {
							Menu menu = new Menu(table.getShell(), SWT.POP_UP);
							MenuItem item1 = new MenuItem(menu, SWT.PUSH);
							item1.setText("View failure paths");

							if (answers.get(table.getSelectionIndex()).equals("Failed to Satisfy")) {
								item1.setEnabled(true);
							} else {
								item1.setEnabled(false);
							}

							item1.addSelectionListener(new SelectionListener() {
								@Override
								public void widgetDefaultSelected(SelectionEvent e) {
									// Nothing here
								}

								@Override
								public void widgetSelected(SelectionEvent e) {
									if (!(table.getSelectionIndex() < 0)) {
										CapecDefenseTable cd = new CapecDefenseTable(Display.getCurrent(),
												parent.getShell(),
												tableContents.get(table.getSelectionIndex()).getPaths());
										IWorkbenchPage wp = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
												.getActivePage();
										IViewPart myView = wp.findView(CapecDefenseView.ID);
										if (myView != null) {
											wp.hideView(myView);
										}
										showView(MBASReportGenerator.window, cd.getTableContents());
									}
								}

							});

							// draws pop up menu:
							Point pt = new Point(e.x, e.y);
							pt = table.toDisplay(pt);
							menu.setLocation(pt.x, pt.y);
							menu.setVisible(true);
						}
					}

					@Override
					public void mouseDoubleClick(MouseEvent e) {
					}
				});

				table.setHeaderVisible(true);
				table.setLinesVisible(true);
				for (int col = 0; col < table.getColumnCount(); col++) {
					table.getColumn(col).pack();
				}
				table.pack();
			}

			if (safetyItems.size() > 0) {
				Table safetyTable = new Table(composite, SWT.MULTI | SWT.FULL_SELECTION);
				safetyTable.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				safetyTable
						.setHeaderBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT));
				safetyTable.setHeaderForeground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_FOREGROUND));

				new TableColumn(safetyTable, SWT.CENTER | SWT.WRAP).setText("Safety Requirement");
				new TableColumn(safetyTable, SWT.CENTER | SWT.WRAP).setText("Acceptable Probability of Failure");
				new TableColumn(safetyTable, SWT.CENTER | SWT.WRAP).setText("Calculated Probability of Failure");
				new TableColumn(safetyTable, SWT.CENTER | SWT.WRAP).setText("Analysis Result");

				for (MBASSafetyResult safetyItem : safetyItems) {
					TableItem item = new TableItem(safetyTable, SWT.NONE);
					item.setText(new String[] { safetyItem.getReqName(), safetyItem.getAcceptableLikelihood(),
							safetyItem.getComputedLikelihood(), " " });
				}

				safetyTable.addListener(SWT.PaintItem, event -> {
					if (event.index == 3) {
						Image image = null;
						for (MBASSafetyResult safetyResult : safetyItems) {
							// This is screwy but it appears to be the best we can do?
							// There will only be issues if there are requirements with duplicate names
							if (safetyResult.getReqName().equals(((TableItem) event.item).getText(0))) {
								image = getSuccessOrFailImage(safetyResult.isSuccessful());
								break;
							}
						}

						int tmpX = 0;
						int tmpY = 0;

						int tmpWidth = safetyTable.getColumn(event.index).getWidth();
						int tmpHeight = ((TableItem) event.item).getBounds().height;

						tmpX = image.getBounds().width;
						tmpX = (tmpWidth / 2 - tmpX / 2);
						tmpY = image.getBounds().height;
						tmpY = (tmpHeight / 2 - tmpY / 2);
						if (tmpX <= 0) {
							tmpX = event.x;
						} else {
							tmpX += event.x;
						}
						if (tmpY <= 0) {
							tmpY = event.y;
						} else {
							tmpY += event.y;
						}

						event.gc.drawImage(image, tmpX, tmpY);
					}
				});

				safetyTable.addMouseListener(new MouseListener() {
					@Override
					public void mouseDoubleClick(MouseEvent e) {
					}

					@Override
					public void mouseDown(MouseEvent e) {
						if (safetyTable.getSelectionIndex() == -1) {
							// Table is empty
							return;
						}
						if (e.button == 3) {
							Menu menu = new Menu(safetyTable.getShell(), SWT.POP_UP);
							MenuItem viewFailurePaths = new MenuItem(menu, SWT.PUSH);
							viewFailurePaths.setText("View failure paths");
							viewFailurePaths
									.setEnabled(!safetyItems.get(safetyTable.getSelectionIndex()).isSuccessful());
							viewFailurePaths.addSelectionListener(new SelectionListener() {
								@Override
								public void widgetSelected(SelectionEvent e) {
									if (safetyTable.getSelectionIndex() >= 0) {
										List<MBASSafetyResult.CutsetResult> cutsets = safetyItems
												.get(safetyTable.getSelectionIndex()).getCutsets();
										IWorkbenchPage wp = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
												.getActivePage();
										IViewPart view = wp.findView(SafetyCutsetsView.ID);
										if (view != null) {
											wp.hideView(view);
										}
										showSafetyView(MBASReportGenerator.window, cutsets);
									}
								}

								@Override
								public void widgetDefaultSelected(SelectionEvent e) {
								}
							});

							Point point = safetyTable.toDisplay(new Point(e.x, e.y));
							menu.setLocation(point);
							menu.setVisible(true);
						}
					}

					@Override
					public void mouseUp(MouseEvent e) {
					}
				});

				safetyTable.setHeaderVisible(true);
				safetyTable.setLinesVisible(true);
				for (int col = 0; col < safetyTable.getColumnCount(); col++) {
					safetyTable.getColumn(col).pack();
				}
				safetyTable.pack();

				composite.pack();
			}
		}
	}

	// this invokes the Vulnerability/Defense viewer-tab
	protected void showView(IWorkbenchWindow window, List<CapecDefenseRow> cd) {
		window.getShell().getDisplay().asyncExec(() -> {
			try {
				CapecDefenseView.tableContents = cd;
				window.getActivePage().showView(CapecDefenseView.ID);
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		});
	}

	protected void showSafetyView(IWorkbenchWindow window, List<MBASSafetyResult.CutsetResult> cutsets) {
		window.getShell().getDisplay().asyncExec(() -> {
			try {
				SafetyCutsetsView.cutsets = cutsets;
				window.getActivePage().showView(SafetyCutsetsView.ID);
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		});
	}
}
