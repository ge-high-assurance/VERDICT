package com.ge.research.osate.verdict.gui;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/** A field editor for displaying a label without any other widget */
public class LabelFieldEditor extends FieldEditor {
    private Label label;

    public LabelFieldEditor(String label, Composite parent) {
        // All label field editors can use the same preference name
        // since they don't store any actual preference
        super("pref", label, parent);
    }

    @Override
    protected void adjustForNumColumns(int numColumns) {
        // Display the label field editor correctly for the given
        // number of columns
        ((GridData) label.getLayoutData()).horizontalSpan = numColumns;
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        // Fill the label field editor into the given parent
        label = getLabelControl(parent);
        GridData gridData = new GridData();
        gridData.horizontalSpan = numColumns;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = false;
        gridData.verticalAlignment = GridData.CENTER;
        gridData.grabExcessVerticalSpace = false;
        label.setLayoutData(gridData);
    }

    @Override
    protected void doLoad() {
        // Label field editors do not store any preferences
    }

    @Override
    protected void doLoadDefault() {
        // Label field editors do not store any preferences
    }

    @Override
    protected void doStore() {
        // Label field editors do not store any preferences
    }

    @Override
    public int getNumberOfControls() {
        // Only one thingy in the field editor
        return 1;
    }
}
