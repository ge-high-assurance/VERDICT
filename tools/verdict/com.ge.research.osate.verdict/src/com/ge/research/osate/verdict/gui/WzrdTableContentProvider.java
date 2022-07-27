package com.ge.research.osate.verdict.gui;

import java.util.List;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 *
 * Author: Soumya Talukder
 * Date: Jul 18, 2019
 *
 */

// this class is used by JFace table-viewer
public class WzrdTableContentProvider implements IStructuredContentProvider {

    @Override
    public Object[] getElements(Object inputElement) {
        return ((List<?>) inputElement).toArray();
    }

    @Override
    public void dispose() {
        // Do nothing
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // Ignore
    }
}
