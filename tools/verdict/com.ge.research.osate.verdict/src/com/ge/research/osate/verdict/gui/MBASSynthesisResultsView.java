package com.ge.research.osate.verdict.gui;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.part.ViewPart;

import com.ge.verdict.vdm.synthesis.ResultsInstance;

public class MBASSynthesisResultsView extends ViewPart {
	public static final String ID = "com.ge.research.osate.verdict.gui.mbasSynthesisResultsView";
	private Composite composite;
	public static ResultsInstance results;

	@Override
	public void setFocus() {
		if (composite != null) {
			composite.setFocus();
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		ScrolledComposite scrollArea = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		scrollArea.setExpandHorizontal(true);

		composite = new Composite(scrollArea, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		scrollArea.setContent(composite);

		// If we don't have results yet, don't crash please
		if (results == null) {
			return;
		}

		{
			Label header = new Label(composite, SWT.BOLD);
			FontDescriptor descriptor = FontDescriptor.createFrom(header.getFont());
			descriptor = descriptor.setStyle(SWT.BOLD);
			header.setFont(descriptor.createFont(Display.getCurrent()));

			// All possible output states
			if (results.partialSolution) {
				if (results.inputSat) {
					header.setForeground(new Color(Display.getCurrent(), 0, 102, 51));
					if (results.meritAssignment) {
						if (results.inputCost != results.outputCost) {
							header.setText("Synthesis results: Partial solution SAT, merit assignment, cost: "
									+ results.inputCost
									+ " -> "
									+ results.outputCost + " (reduction of " + (results.inputCost - results.outputCost)
									+ ")");
						} else {
							header.setText(
									"Synthesis results: Partial solution SAT, merit assignment already minimal, total cost: "
									+ results.outputCost);
						}
					} else {
						// This one doesn't actually happen because there is no option to disable merit assignment
						header.setText("Synthesis results: Partial solution SAT, no merit assignment, total cost: "
								+ results.outputCost);
					}
				} else {
					header.setForeground(new Color(Display.getCurrent(), 204, 0, 0));
					header.setText("Synthesis results: Partial solution UNSAT, cost: " + results.inputCost
							+ " -> "
							+ results.outputCost
							+ " (increase of " + (results.outputCost - results.inputCost) + ")");
				}
			} else {
				header.setText("Synthesis results: No partial solution, total cost: " + results.outputCost);
			}
		}

		Table table = new Table(composite, SWT.MULTI | SWT.FULL_SELECTION);
		table.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		table.setHeaderBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT));
		table.setHeaderForeground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_FOREGROUND));

		if (results.partialSolution) {
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Action");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Component");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Defense Property");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Original DAL");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Target DAL");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Original Cost");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Target Cost");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Delta Cost");

			for (ResultsInstance.Item item : results.items) {
				if (!results.partialSolution || item.inputDal != item.outputDal) {
					double deltaCost = item.outputCost - item.inputCost;
					String change;
					if (item.outputDal > item.inputDal) {
						if (item.inputDal == 0) {
							change = "Implement";
						} else {
							change = "Upgrade";
						}
					} else if (item.outputDal < item.inputDal) {
						if (item.outputDal == 0) {
							change = "Remove";
						} else {
							change = "Downgrade";
						}
					} else {
						change = "N/A";
					}

					TableItem row = new TableItem(table, SWT.NONE);
					row.setText(new String[] { change, item.component, item.defenseProperty,
							Integer.toString(item.inputDal), Integer.toString(item.outputDal),
							Double.toString(item.inputCost), Double.toString(item.outputCost),
							Double.toString(deltaCost), });
				}
			}
		} else {
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Component");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Defense Property");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Target DAL");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Target Cost");

			for (ResultsInstance.Item item : results.items) {
				if (item.outputDal != 0) {
					TableItem row = new TableItem(table, SWT.NONE);
					row.setText(new String[] { item.component, item.defenseProperty,
							Integer.toString(item.outputDal), Double.toString(item.outputCost) });
				}
			}
		}

		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		for (int col = 0; col < table.getColumnCount(); col++) {
			table.getColumn(col).pack();
		}
		table.pack();
		composite.pack();
	}
}
