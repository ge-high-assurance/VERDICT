package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.part.ViewPart;

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/

//this class generates the vulnerability/defense viewer-tab in OSATE
public class CapecDefenseView extends ViewPart {
	private Composite composite;
	public static final String ID = "com.ge.research.osate.verdict.gui.capecDefenseView";
	public static List<CapecDefenseRow> tableContents = new ArrayList<CapecDefenseRow>();
	private Shell tooltipShell;

	public CapecDefenseView() {
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
		Display display = Display.getCurrent();

		composite.setSize(1130, 600);
		composite.setLayout(new FillLayout());

		Table table = new Table(composite, SWT.MULTI | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setHeaderBackground(display.getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT));
		table.setHeaderForeground(display.getSystemColor(SWT.COLOR_TITLE_FOREGROUND));

		TableColumn col1 = new TableColumn(table, SWT.CENTER | SWT.WRAP);
		col1.setText("Minimal Failure Path");
		TableColumn col2 = new TableColumn(table, SWT.CENTER | SWT.WRAP);
		col2.setText("Path Likelihood");
		TableColumn col3 = new TableColumn(table, SWT.CENTER | SWT.WRAP);
		col3.setText("Attack Type");
		TableColumn col4 = new TableColumn(table, SWT.CENTER | SWT.WRAP);
		col4.setText("Suggested Defenses");
		TableColumn col5 = new TableColumn(table, SWT.CENTER | SWT.WRAP);
		col5.setText("Suggested Defenses Profile");
		TableColumn col6 = new TableColumn(table, SWT.CENTER | SWT.WRAP);
		col6.setText("Implemented Defenses");

		int itemCount = tableContents.size();
		for (int i = 0; i < itemCount; i++) {
			TableItem item = new TableItem(table, SWT.NONE);
			List<String> currRow = tableContents.get(i).getRowContents();
			String[] itemSeq = new String[table.getColumnCount()];
			for (int j = 0; j < table.getColumnCount(); j++) {
				itemSeq[j] = currRow.get(j);
			}
			item.setText(itemSeq);
			item.setData(tableContents.get(i));
		}

		// Display a tooltip on hover
		table.addListener(SWT.MouseHover, event -> {
			TableItem item = table.getItem(new Point(event.x, event.y));
			CapecDefenseRow data = (CapecDefenseRow) item.getData();

			// Note: getTextBounds should (I think?) give just the bounds
			// of the text inside the cell, but it appears to be giving
			// the bounds for the whole cell

			String text = "";
			if (item.getTextBounds(2).contains(event.x, event.y)) {
				// Attack type (CAPEC)
				text = String.join("\n", data.getAttackHoverText());
			} else if (item.getTextBounds(4).contains(event.x, event.y)) {
				// Suggested defenses profile (NIST)
				text = String.join("\n", data.getDefenseHoverText());
			}
			if (text.length() > 0) {
				showTooltip(text, table.toDisplay(event.x, event.y));
			}
		});

		// Get rid of the tooltip when mouse moves or window loses focus
		Listener listener = event -> {
			if (tooltipShell != null) {
				tooltipShell.setVisible(false);
				tooltipShell.dispose();
				tooltipShell = null;
			}
		};
		table.addListener(SWT.MouseMove, listener);
		table.addListener(SWT.FocusOut, listener);

		for (int i = 0; i < table.getColumnCount(); i++) {
			table.getColumn(i).pack();
		}

		table.pack();
		composite.pack();
	}

	/**
	 * Display a tooltip at the given position.
	 *
	 * @param text
	 * @param pos
	 */
	private void showTooltip(String text, Point pos) {
		Display display = Display.getCurrent();
		display.asyncExec(() -> {
			// Clean up previous tooltip if it still exists
			if (tooltipShell != null) {
				tooltipShell.setVisible(false);
				tooltipShell.dispose();
				tooltipShell = null;
			}
			tooltipShell = new Shell(composite.getShell(), SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
			tooltipShell.addFocusListener(new FocusListener() {
				@Override
				public void focusGained(FocusEvent event) {}

				@Override
				public void focusLost(FocusEvent event) {
					tooltipShell.setVisible(false);
					display.asyncExec(tooltipShell::dispose);
				}
			});
			FillLayout layout = new FillLayout();
			layout.marginWidth = 2;
			tooltipShell.setLayout(layout);
			Label label = new Label(tooltipShell, SWT.NONE);
			label.setText(text);
			Point size = tooltipShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			tooltipShell.setBounds(pos.x, pos.y, size.x, size.y);
			tooltipShell.setVisible(true);
		});
	}
}
