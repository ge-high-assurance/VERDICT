package com.ge.research.osate.verdict.dsl.type;

public class VerdictFieldImpl implements VerdictField {
	private String name;
	private VerdictType type;

	public VerdictFieldImpl(String name, VerdictType type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public VerdictType getType() {
		return type;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof VerdictField) {
			VerdictField otherField = (VerdictField) other;
			return otherField.getName().equals(name) && otherField.getType().equals(type);
		}

		return false;
	}
}
