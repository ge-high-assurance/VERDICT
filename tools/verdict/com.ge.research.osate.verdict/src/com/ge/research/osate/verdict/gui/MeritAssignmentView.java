package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.HashSet;
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
				TreeItem level0NodeItem = new TreeItem (tree, 0);
				
				level0NodeItem.setText("Minimal Inductive Validity Core #" + (i+1));
				Set<IVCNode> mIvcs = mInvTreeContents.get(i);
				
				for(IVCNode mIvc : mIvcs) {
					TreeItem level1NodeItem = new TreeItem (level0NodeItem, 0);
					level1NodeItem.setText("Component '" + mIvc.getNodeName() + "'");
					for (IVCElement e : mIvc.getNodeElements()) {
						TreeItem eItem = new TreeItem (level1NodeItem, 0);
						eItem.setText(e.getCategory().toString() + " '" + e.getName() + "'");
					}
				}
			}
		}
		
		if(!aInvTreeContents.isEmpty()) {
			for(int i = 0; i < aInvTreeContents.size(); ++i) {
				TreeItem level0NodeItem = new TreeItem (tree, 0);
				
				level0NodeItem.setText("Inductive Validity Core #" + (i+1));
				Set<IVCNode> aIvcs = aInvTreeContents.get(i);
				
				for(IVCNode aIvc : aIvcs) {
					TreeItem level1NodeItem = new TreeItem (level0NodeItem, 0);
					level1NodeItem.setText("Component '" + aIvc.getNodeName() + "'");
					for (IVCElement e : aIvc.getNodeElements()) {
						TreeItem eItem = new TreeItem (level1NodeItem, 0);
						eItem.setText(e.getCategory().toString() + " '" + e.getName() + "'");
					}
				}
			}
		}		
		
		tree.pack();
		composite.pack();
	}	
}
