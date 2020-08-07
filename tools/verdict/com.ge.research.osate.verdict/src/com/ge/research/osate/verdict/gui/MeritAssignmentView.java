package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;

/**
*
* Author: Daniel Larraz
* Date: Aug 6, 2020
*
*/

public class MeritAssignmentView extends ViewPart {

	private Composite composite;
	public static final String ID = "com.ge.research.osate.verdict.gui.meritAssignmentView";
	public static List<IVCNode> treeContents = new ArrayList<IVCNode>();
		
	@Override
	public void setFocus() {
		if (composite != null) {
			composite.setFocus();
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		
		composite = new Composite(parent, SWT.NONE);
		
		composite.setSize(1130, 600);
		composite.setLayout(new FillLayout());
		
		Tree tree = new Tree (composite, SWT.BORDER);
		
		Map<Boolean, List<IVCNode>> partitioned =
				  treeContents.stream().collect(Collectors.partitioningBy(IVCNode::hasAssumption));
		
		List<IVCNode> withAssumptions = partitioned.get(true);
		fillTree(tree, withAssumptions);
		
		List<IVCNode> withoutAssumptions = partitioned.get(false);
		fillTree(tree, withoutAssumptions);
		
		tree.pack();
		composite.pack();
	}

	private void fillTree(Tree tree, List<IVCNode> nodes) {
		for (IVCNode node : nodes) {
			TreeItem nodeItem = new TreeItem (tree, 0);
			nodeItem.setText("Component '" + node.getNodeName() + "'");
			for (IVCElement e : node.getNodeElements()) {
				TreeItem eItem = new TreeItem (nodeItem, 0);
				eItem.setText(e.getCategory().toString() + " '" + e.getName() + "'");
			}
		}
	}
	
}
