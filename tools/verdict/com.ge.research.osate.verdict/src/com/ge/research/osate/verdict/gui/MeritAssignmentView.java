package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	public static List<Set<IVCNode>> mInvTreeContents = new ArrayList<>();
	public static List<Set<IVCNode>> aInvTreeContents = new ArrayList<>();
		
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
		
		if(!mInvTreeContents.isEmpty()) {
			for(int i = 0; i < mInvTreeContents.size(); ++i) {
				TreeItem topLevelNodeItem = new TreeItem (tree, 0);
				
				topLevelNodeItem.setText("Minimal Inductive Validity Core #" + (i+1));
				Set<IVCNode> mIvcs = mInvTreeContents.get(i);
				Map<Boolean, List<IVCNode>> partitioned = mIvcs.stream().collect(Collectors.partitioningBy(IVCNode::hasAssumption));
				fillTree(topLevelNodeItem, partitioned.get(true));
				fillTree(topLevelNodeItem, partitioned.get(false));			
			}
		}
		
		if(!aInvTreeContents.isEmpty()) {
			for(int i = 0; i < aInvTreeContents.size(); ++i) {
				TreeItem topLevelNodeItem = new TreeItem (tree, 0);
				
				topLevelNodeItem.setText("Inductive Validity Core #" + (i+1));
				Set<IVCNode> aIvcs = aInvTreeContents.get(i);
				Map<Boolean, List<IVCNode>> partitioned = aIvcs.stream().collect(Collectors.partitioningBy(IVCNode::hasAssumption));
				fillTree(topLevelNodeItem, partitioned.get(true));
				fillTree(topLevelNodeItem, partitioned.get(false));							
			}
		}		
		
		tree.pack();
		composite.pack();
	}
	
	private void fillTree(TreeItem topLevel, List<IVCNode> nodes) {
		for (IVCNode node : nodes) {
			TreeItem nodeItem = new TreeItem (topLevel, 0);
			nodeItem.setText("Component '" + node.getNodeName() + "'");
			for (IVCElement e : node.getNodeElements()) {
				TreeItem eItem = new TreeItem (nodeItem, 0);
				eItem.setText(e.getCategory().toString() + " '" + e.getName() + "'");
			}
		}
	}	
}
