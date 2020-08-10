package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;

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

	private static enum Action {
		// the order defines the ordering in the sort
		IMPLEMENT("Implement"), UPGRADE("Upgrade"), REMOVE("Remove"), DOWNGRADE("Downgrade"), NONE("N/A");

		public final String name;

		private Action(String name) {
			this.name = name;
		}

		public static Action fromItem(ResultsInstance.Item item) {
			if (item.outputDal > item.inputDal) {
				if (item.inputDal == 0) {
					return IMPLEMENT;
				} else {
					return UPGRADE;
				}
			} else if (item.outputDal < item.inputDal) {
				if (item.outputDal == 0) {
					return REMOVE;
				} else {
					return DOWNGRADE;
				}
			} else {
				return NONE;
			}
		}

		public String checkDitto(Action prev) {
			return this.equals(prev) ? "-do-" : name;
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
						if (!results.inputCost.equals(results.outputCost)) {
							header.setText(
									"Synthesis results: Existing solution SAT, merit assignment, cost: "
									+ results.inputCost
											.doubleValue()
									+ " -> "
									+ results.outputCost.doubleValue()
									+ " (reduction of "
									+ (results.inputCost.subtract(results.outputCost).doubleValue())
									+ ")");
						} else {
							header.setText(
									"Synthesis results: Existing solution SAT, merit assignment already minimal, total cost: "
											+ results.outputCost.doubleValue());
						}
					} else {
						// This one doesn't actually happen because there is no option to disable merit assignment
						header.setText(
								"Synthesis results: Existing solution SAT, no merit assignment, total cost: "
								+ results.outputCost.doubleValue());
					}
				} else {
					header.setForeground(new Color(Display.getCurrent(), 204, 0, 0));
					header.setText("Synthesis results: Existing solution UNSAT, cost: "
							+ results.inputCost
							.doubleValue()
							+ " -> "
							+ results.outputCost
									.doubleValue()
							+ " (increase of " + (results.outputCost.subtract(results.inputCost).doubleValue()) + ")");
				}
			} else {
				header.setText(
						"Synthesis results (not using existing solution): total cost: "
								+ results.outputCost.doubleValue());
			}
		}

		Table table = new Table(composite, SWT.MULTI | SWT.FULL_SELECTION);
		table.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		table.setHeaderBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT));
		table.setHeaderForeground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_FOREGROUND));

		if (results.partialSolution) {
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Action");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Component/Connection");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Defense Property");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Original DAL");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Target DAL");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Original Cost");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Target Cost");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Delta Cost");

			List<ResultsInstance.Item> sortedItems = new ArrayList<>(results.items);
			sortedItems.sort((a, b) -> Action.fromItem(a).compareTo(Action.fromItem(b))); // enum implements Comparable

			Action prev = null;
			for (ResultsInstance.Item item : sortedItems) {
				if (!results.partialSolution || item.inputDal != item.outputDal) {
					double deltaCost = item.outputCost.subtract(item.inputCost).doubleValue();
					Action action = Action.fromItem(item);

					TableItem row = new TableItem(table, SWT.NONE);
					row.setText(new String[] { action.checkDitto(prev), item.component,
							item.defenseProperty,
							Integer.toString(item.inputDal), Integer.toString(item.outputDal),
							Double.toString(item.inputCost.doubleValue()), Double.toString(item.outputCost
									.doubleValue()),
							Double.toString(deltaCost), });

					prev = action;
				}
			}
		} else {
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Action");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Component/Connection");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Defense Property");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Target DAL");
			new TableColumn(table, SWT.CENTER | SWT.WRAP).setText("Target Cost");

			Action prev = null;
			for (ResultsInstance.Item item : results.items) {
				if (item.outputDal != 0) {
					TableItem row = new TableItem(table, SWT.NONE);
					Action action = Action.IMPLEMENT;

					row.setText(new String[] { action.checkDitto(prev), item.component,
							item.defenseProperty,
							Integer.toString(item.outputDal), Double.toString(item.outputCost.doubleValue()) });

					prev = action;
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
