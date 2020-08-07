package com.ge.research.osate.verdict.gui;

import java.io.Serializable;

/**
*
* Author: Daniel Larraz
* Date: Aug 6, 2020
*
*/

public class IVCElement implements Serializable, Cloneable {
	
	private static final long serialVersionUID = 1L;
	private Category categ;
	private String name;

	public enum Category {
		ASSERTION {
            @Override
            public String toString() {
                return "Assertion";
            }
        },
		ASSUMPTION {
            @Override
            public String toString() {
                return "Assumption";
            }
        },
		ENSURE {
            @Override
            public String toString() {
                return "Ensure";
            }
        },
		EQUATION {
            @Override
            public String toString() {
                return "Equation";
            }
        },
		GUARANTEE {
        	@Override
            public String toString() {
                return "Guarantee";
            }
        },
		NODE_CALL {
        	@Override
            public String toString() {
                return "Node call";
            }
        },
        REQUIRE {
        	@Override
            public String toString() {
                return "Require";
            }
        }
	}
	
	public void setCategory(Category categ) {
		this.categ = categ;
	}
	
	public Category getCategory() {
		return categ;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isAssumption() {
		return categ == Category.ASSUMPTION;
	}
}
