package com.ge.research.osate.verdict.gui;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class ModelSet implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    private Set<ModelNode> nodes = new HashSet<>();
    private boolean isApprox = true;

    public Set<ModelNode> getNodes() {
        return nodes;
    }

    public void setNodes(Set<ModelNode> nds) {
        nodes = nds;
    }

    public boolean isApprox() {
        return isApprox;
    }

    public void setIsApprox(boolean approx) {
        isApprox = approx;
    }
}
