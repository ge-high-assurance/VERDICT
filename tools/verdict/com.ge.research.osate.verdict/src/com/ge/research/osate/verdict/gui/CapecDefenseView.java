package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
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
		Composite composite = new Composite(parent, SWT.NONE);
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
		col4.setText("Suggested Defense");
		TableColumn col5 = new TableColumn(table, SWT.CENTER | SWT.WRAP);
		col5.setText("Implemented Defenses");

		int itemCount = tableContents.size();
		for (int i = 0; i < itemCount; i++) {
			TableItem item = new TableItem(table, SWT.NONE);
			List<String> currRow = tableContents.get(i).getRowContents();
			String[] itemSeq = new String[table.getColumnCount()];
			for (int j = 0; j < table.getColumnCount(); j++) {
				itemSeq[j] = currRow.get(j);
			}
			item.setText(itemSeq);
		}

		for (int i = 0; i < table.getColumnCount(); i++) {
			table.getColumn(i).pack();
		}

		table.pack();
		composite.pack();
	}
}
