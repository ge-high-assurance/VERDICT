package com.ge.research.osate.verdict.gui;

import org.eclipse.jface.viewers.ICellEditorListener;

/** Author: Soumya Talukder Date: Jul 18, 2019 */

// this class implements the jface interface ICellEditorListener
public class WzrdCellEditorListener implements ICellEditorListener {

    @Override
    public void applyEditorValue() {
        if (StatementEditor.editorContentCol3 != (String) StatementEditor.editors[3].getValue()) {
            WzrdTableRow rowToDelete =
                    StatementEditor.tableContent.get(
                            StatementEditor.tv.getTable().getSelectionIndex());
            if (!rowToDelete.isNew()) {
                try {
                    StatementEditor.deletedExistingContent.add((WzrdTableRow) rowToDelete.clone());
                } catch (Exception e) {
                    System.out.println("Clone error in WzrdTableRow");
                    e.printStackTrace();
                }
            }
            rowToDelete.setNewRow(true);
            StatementEditor.editorContentCol3 = (String) StatementEditor.editors[3].getValue();
            StatementEditor.delete.setEnabled(true);
            StatementEditor.noInput.setEnabled(true);
            StatementEditor.addDefinition.setEnabled(true);
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
}
