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
    // public static List<IVCNode> treeContents = new ArrayList<IVCNode>();
    public static ModelSet mustSet = new ModelSet();
    public static List<Set<ModelNode>> mInvTreeContents = new ArrayList<>();
    public static List<Set<ModelNode>> aInvTreeContents = new ArrayList<>();

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

        Tree tree = new Tree(composite, SWT.BORDER);

        Set<ModelNode> mustSetNodes = mustSet.getNodes();
        if (!mustSetNodes.isEmpty()) {
            TreeItem topLevelNodeItem = new TreeItem(tree, 0);
            processNodes(topLevelNodeItem, "MUST set", mustSetNodes);
        }

        if (!mInvTreeContents.isEmpty()) {
            for (int i = 0; i < mInvTreeContents.size(); ++i) {
                TreeItem topLevelNodeItem = new TreeItem(tree, 0);

                Set<ModelNode> mIvcs = mInvTreeContents.get(i);
                processNodes(topLevelNodeItem, "Minimal Inductive Validity Core #" + (i + 1), mIvcs);
            }
        }

        if (!aInvTreeContents.isEmpty()) {
            for (int i = 0; i < aInvTreeContents.size(); ++i) {
                TreeItem topLevelNodeItem = new TreeItem(tree, 0);

                Set<ModelNode> aIvcs = aInvTreeContents.get(i);
                processNodes(topLevelNodeItem, "Inductive Validity Core #" + (i + 1), aIvcs);
            }
        }

        tree.pack();
        composite.pack();
    }

    private void processNodes(TreeItem topLevel, String title, Set<ModelNode> nodes) {
        topLevel.setText(title);
        Map<Boolean, List<ModelNode>> partitioned =
                nodes.stream().collect(Collectors.partitioningBy(ModelNode::hasAssumption));
        fillTree(topLevel, partitioned.get(true));
        fillTree(topLevel, partitioned.get(false));
    }

    private void fillTree(TreeItem topLevel, List<ModelNode> nodes) {
        for (ModelNode node : nodes) {
            TreeItem nodeItem = new TreeItem(topLevel, 0);
            nodeItem.setText("Component '" + node.getNodeName() + "'");
            for (ModelElement e : node.getNodeElements()) {
                TreeItem eItem = new TreeItem(nodeItem, 0);
                eItem.setText(e.getCategory().toString() + " '" + e.getName() + "'");
            }
        }
    }
}
