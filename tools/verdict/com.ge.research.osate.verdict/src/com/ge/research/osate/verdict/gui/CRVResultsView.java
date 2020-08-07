package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
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

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/

//this class generates the CRV Results view-tab in OSATE
public class CRVResultsView extends ViewPart {
	private Composite composite;
	public static final String ID = "com.ge.research.osate.verdict.gui.crvResultsView";
	public static List<CRVSummaryRow> tableContents = new ArrayList<CRVSummaryRow>();


	public CRVResultsView() {
		super();
	}

	@Override
	public void setFocus() {
		if (composite != null) {
			composite.setFocus();
		}
	}

	private Image getIcon(String name) {
		return ResourceLocator.imageDescriptorFromBundle("com.ge.research.osate.verdict", "icons/" + name).get().createImage();
	}

	@Override
	public void createPartControl(Composite parent) {
		// Only show these columns if they have data
		// In practice, they will all be empty if blame assignment is turned off
		// and components/links will be selected based on the -C flag
		// (component/link-level instrumentation/blame assignment)
//		boolean showAttackType = tableContents.stream().anyMatch(CRVSummaryRow::hasAttackType);
//		boolean showCompromisedComponents = tableContents.stream().anyMatch(CRVSummaryRow::hasCompromisedComponents);
//		boolean showCompromisedLinks = tableContents.stream().anyMatch(CRVSummaryRow::hasCompromisedLinks);
//		boolean showUncompromisedComponents = tableContents.stream()
//				.anyMatch(CRVSummaryRow::hasUncompromisedComponents);
//		boolean showUncompromisedLinks = tableContents.stream().anyMatch(CRVSummaryRow::hasUncompromisedLinks);

		boolean showBlameAssignmentInfo =
				CRVSettingsPanel.isBlameAssignment &&
				tableContents.stream().anyMatch(CRVSummaryRow::hasCounterExample);;

		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		Table table = new Table(composite, SWT.MULTI | SWT.FULL_SELECTION);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setHeaderBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT));
		table.setHeaderForeground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_FOREGROUND));

		List<Integer> columnsToShow = new ArrayList<>();
		columnsToShow.addAll(Arrays.asList(0, 1));

		TableColumn columnOne = new TableColumn(table, SWT.CENTER | SWT.WRAP);
		columnOne.setText("Property");
		TableColumn columnTwo = new TableColumn(table, SWT.CENTER | SWT.WRAP);
		columnTwo.setText("Verification Result");
		columnTwo.setWidth(500);
		//if (showAttackType) {
		if (showBlameAssignmentInfo) {
			TableColumn columnThree = new TableColumn(table, SWT.CENTER | SWT.WRAP);
			columnThree.setText("Attack Type");
			columnsToShow.add(2);
		}
		//if (showCompromisedComponents) {
		if (showBlameAssignmentInfo && CRVSettingsPanel.componentLevel) {
			TableColumn columnFour = new TableColumn(table, SWT.CENTER | SWT.WRAP);
			columnFour.setText("Critical Components");
			columnsToShow.add(3);
		}
		//if (showCompromisedLinks) {
		if (showBlameAssignmentInfo && !CRVSettingsPanel.componentLevel) {
			TableColumn columnFive = new TableColumn(table, SWT.CENTER | SWT.WRAP);
			columnFive.setText("Critical Links (Ports)");
			columnsToShow.add(4);
		}
		//if (showUncompromisedComponents) {
		if (showBlameAssignmentInfo && CRVSettingsPanel.componentLevel) {
			TableColumn columnSix = new TableColumn(table, SWT.CENTER | SWT.WRAP);
			columnSix.setText("Selected Components");
			columnsToShow.add(5);
		}
		//if (showUncompromisedLinks) {
		if (showBlameAssignmentInfo && !CRVSettingsPanel.componentLevel) {
			TableColumn columnSeven = new TableColumn(table, SWT.CENTER | SWT.WRAP);
			columnSeven.setText("Selected Links (Ports)");
			columnsToShow.add(6);
		}

		List<String> keys = new ArrayList<String>();
		List<String> answers = new ArrayList<String>();

		for (int i = 0; i < tableContents.size(); i++) {
			TableItem item = new TableItem(table, SWT.NONE);
			List<String> currRow = tableContents.get(i).getRowContents();
			int j = 0; // display column counter
			// "column" is data column counter
			for (int column : columnsToShow) {
				item.setText(j++, currRow.get(column));
			}
			// we replace this column (verification result) with an image
			item.setText(1, "");
			keys.add(currRow.get(0));
			answers.add(currRow.get(1));
		}

		// this listener center aligns the pictorial results
		table.addListener(SWT.PaintItem, event -> {
		    if(event.index == 1) {
		    	Image image;
				int count = -1;
				for (int i = 0; i < keys.size(); i++) {
					if (keys.get(i).equals(((TableItem) event.item).getText(event.index - 1))) {
						count = i;
						break;
					}
				}
				if (answers.get(count).equals("falsifiable")) {
					image = getIcon("false.png");
				} else if (answers.get(count).equals("unknown")) {
					image = getIcon("fail.png");
				} else {
					image = getIcon("valid.png");
				}

		        int tmpX = 0;
		        int tmpY = 0;

		        int tmpWidth = table.getColumn(event.index).getWidth();
		        int tmpHeight = ((TableItem) event.item).getBounds().height;

		        tmpX = image.getBounds().width;
		        tmpX = (tmpWidth / 2 - tmpX / 2);
		        tmpY = image.getBounds().height;
		        tmpY = (tmpHeight / 2 - tmpY / 2);
		        if(tmpX <= 0) {
					tmpX = event.x;
				} else {
					tmpX += event.x;
				}
		        if(tmpY <= 0) {
					tmpY = event.y;
				} else {
					tmpY += event.y;
				}

		        event.gc.drawImage(image, tmpX, tmpY);
		    }
		});

		// this creates the context-menu for viewing blame-assignment/counter-example
		table.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent e) {
			}

			@Override
			public void mouseDown(MouseEvent e) {
				if (table.getSelectionIndex() == -1 || e.button != 3) {
					// If the table is empty or not right button
					return;
				}
				
				boolean invalidProperty =
					!tableContents.get(table.getSelectionIndex()).getCounterExample().isEmpty();
				
				if (invalidProperty || CRVSettingsPanel.isMeritAssignment) {
					Menu menu = new Menu(table.getShell(), SWT.POP_UP);
					
					if (!invalidProperty) {
						MenuItem meritAssignment = new MenuItem(menu, SWT.PUSH);
						meritAssignment.setText("View Inductive Validity Core");
						
						meritAssignment.addSelectionListener(new SelectionListener() {
							@Override
							public void widgetDefaultSelected(SelectionEvent e) {
								// Nothing here
							}
	
							@Override
							public void widgetSelected(SelectionEvent e) {
								if (!(table.getSelectionIndex() < 0)) {
									
									IWorkbenchPage wp = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
											.getActivePage();
									IViewPart myView1 = wp.findView(MeritAssignmentView.ID);
									if (myView1 != null) {
										wp.hideView(myView1);
									}
									
									CRVReportGenerator.window.getShell().getDisplay().asyncExec(() -> {
										try {
											CRVReportGenerator.window.getActivePage().showView(MeritAssignmentView.ID);
										} catch (PartInitException ex) {
											ex.printStackTrace();
										}
									});
								}
							}
						});
					}
					
					if (invalidProperty) {
						MenuItem counterExample = new MenuItem(menu, SWT.PUSH);
						counterExample.setText("View Counter-example");
						counterExample.addSelectionListener(new SelectionListener() {
							@Override
							public void widgetDefaultSelected(SelectionEvent e) {
								// Nothing here
							}
	
							@Override
							public void widgetSelected(SelectionEvent e) {
								if (!(table.getSelectionIndex() < 0)) {
									CETable ce = new CETable(Display.getCurrent(), parent.getShell(),
											tableContents.get(table.getSelectionIndex()).getCounterExample());
									IWorkbenchPage wp = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
											.getActivePage();
									IViewPart myView1 = wp.findView(CounterExampleView.ID_COUNTER_EXAMPLE);
									if (myView1 != null) {
										wp.hideView(myView1);
									}
									IViewPart myView2 = wp.findView(CounterExampleView.ID_TEST_CASE);
									if (myView2 != null) {
										wp.hideView(myView2);
									}
									String name = tableContents.get(table.getSelectionIndex()).getPropertyName();
									showView(CRVReportGenerator.window, ce.getTableContents(), false, name);
								}
							}
						});
					}

//					MenuItem testCase = new MenuItem(menu, SWT.PUSH);
//					testCase.setText("View Test Case");
//					testCase.setEnabled(!tableContents.get(table.getSelectionIndex()).getTestCase().isEmpty());

//					testCase.addSelectionListener(new SelectionListener() {
//						@Override
//						public void widgetDefaultSelected(SelectionEvent e) {
//							// Nothing here
//						}
//
//						@Override
//						public void widgetSelected(SelectionEvent e) {
//							if (!(table.getSelectionIndex() < 0)) {
//								CETable ce = new CETable(Display.getCurrent(), parent.getShell(),
//										tableContents.get(table.getSelectionIndex()).getTestCase());
//								IWorkbenchPage wp = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
//										.getActivePage();
//								IViewPart myView1 = wp.findView(CounterExampleView.ID_COUNTER_EXAMPLE);
//								if (myView1 != null) {
//									wp.hideView(myView1);
//								}
//								IViewPart myView2 = wp.findView(CounterExampleView.ID_TEST_CASE);
//								if (myView2 != null) {
//									wp.hideView(myView2);
//								}
//								String name = tableContents.get(table.getSelectionIndex()).getPropertyName();
//								showView(CRVReportGenerator.window, ce.getTableContents(), true, name);
//							}
//						}
//					});

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

		for (int col = 0; col < columnsToShow.size(); col++) {
			table.getColumn(col).pack();
		}
		table.pack();
		composite.pack();
	}

	// this invokes the Counter-example viewer-tab in OSATE
	protected void showView(IWorkbenchWindow window, List<CERow> ce, boolean testCase, String propertyName) {
		window.getShell().getDisplay().asyncExec(() -> {
			try {
				CounterExampleView.tableContents = ce;
				CounterExampleView.testCase = testCase;
				CounterExampleView.propertyName = propertyName;
				window.getActivePage()
						.showView(testCase ? CounterExampleView.ID_TEST_CASE : CounterExampleView.ID_COUNTER_EXAMPLE);
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		});
	}
}
