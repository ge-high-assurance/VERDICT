package com.ge.research.osate.verdict.gui;

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Item;

/**
 *
 * Author: Soumya Talukder
 * Date: Jul 18, 2019
 *
 */
// this class is used by jFace table-viewer for updating table-cell contents
// after user edits
public class WzrdTableCellModifier implements ICellModifier {
    private Viewer viewer; // the current viewer
    private WzrdTableRow tableRow; // the data-structure to contain contents of the current row
    private String[] props = new String[3]; // contains the header elements of the StatementEditor tableViewer

    public WzrdTableCellModifier(Viewer viewer, String[] strings) {
        this.viewer = viewer;
        props = strings;
    }

    @Override
    public boolean canModify(Object element, String property) {
        return true;
    }

    @Override
    public Object getValue(Object element, String property) {
        tableRow = (WzrdTableRow) element;
        if (props[0].equals(property)) {
            return tableRow.getFormulaID();
        } else if (props[1].equals(property)) {
            return tableRow.getFirstElement();
        } else if (props[2].equals(property)) {
            return tableRow.getSecondElement();
        } else {
            return tableRow.getThirdElement();
        }
    }

    @Override
    public void modify(Object element, String property, Object value) {
        if (element instanceof Item) {
            element = ((Item) element).getData();
        }

        tableRow = (WzrdTableRow) element;
        if (props[0].equals(property)) {
            tableRow.setFormulaID((String) value);
        } else if (props[1].equals(property)) {
            tableRow.setFirstElement((int) value);
        } else if (props[2].equals(property)) {
            tableRow.setSecondElement((int) value);
        } else {
            tableRow.setThirdElement((String) value);
        }

        viewer.refresh();
    }
}
