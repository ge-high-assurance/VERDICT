package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
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


	public MBASResultsView() {
		super();
	}

	@Override
	public void setFocus() {
		if (composite != null) {
			composite.setFocus();
		}
	}

	@Override
	public void createPartControl(Composite parent) {

		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

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

			Table table = new Table(composite, SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL);
			table.setLayoutData(new GridData(GridData.FILL_BOTH));
			table.setHeaderBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT));
			table.setHeaderForeground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_FOREGROUND));

			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Cyber Requirement");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Acceptable Failure Likelihood");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Computed Failure Likelihood");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Analysis Result");

			List<String> keys = new ArrayList<String>();
			List<String> answers = new ArrayList<String>();

			for (int i = 0; i < tableContents.size(); i++) {
				TableItem item = new TableItem(table, SWT.NONE);
				List<String> currRow = tableContents.get(i).getRowContents();
				item.setText(
						new String[] { currRow.get(0), currRow.get(1), currRow.get(2), " " });
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
								if (!requirements.get(j).hasSucceeded()) {
									image = AbstractUIPlugin.imageDescriptorFromPlugin("com.ge.research.osate.verdict",
											"icons/false.png").createImage();
								} else {
									image = AbstractUIPlugin.imageDescriptorFromPlugin("com.ge.research.osate.verdict",
											"icons/valid.png").createImage();
								}
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
											parent.getShell(), tableContents.get(table.getSelectionIndex()).getPaths());
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

			for (int col = 0; col < 4; col++) {
				table.getColumn(col).pack();
			}
			table.pack();
			composite.pack();
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
}
