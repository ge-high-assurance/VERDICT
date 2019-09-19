package com.ge.research.osate.verdict.gui;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/

//this class is used by JFace table-viewer. It provides the drop-down contents
//to the table cells
public class WzrdTableLabelProvider implements ITableLabelProvider {

	private boolean systemLevel;
	private DrpDnLists drpdn;

	public WzrdTableLabelProvider(DrpDnLists list, boolean sysLevel) {
		systemLevel = sysLevel;
		drpdn = list;
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		WzrdTableRow tableRow = (WzrdTableRow) element;
		switch (columnIndex) {
		case 0:
			return tableRow.getFormulaID();
		case 1:
			if (systemLevel) {
				return drpdn.CIA[tableRow.getFirstElement().intValue()];
			} else {
				return drpdn.outPorts[tableRow.getFirstElement().intValue()];
			}
		case 2:
			if (systemLevel) {
				return drpdn.SEVERITY[tableRow.getSecondElement().intValue()];
			} else {
				return drpdn.CIA[tableRow.getSecondElement().intValue()];
			}
		case 3:
			return tableRow.getThirdElement();
		}
		return null;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		// Ignore it
	}

	@Override
	public void dispose() {
		// Nothing to dispose
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		// Ignore
	}
}