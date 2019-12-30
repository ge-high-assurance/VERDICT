package com.ge.verdict.utils.iml;

public enum IMLBuiltInType implements VerdictIMLType {
	INT("Int"),
	REAL("Real"),
	STRING("String"),
	BOOL("Bool");
	
	private String typeName;
	IMLBuiltInType(String typeName) {
		this.typeName = typeName;
	}
	public String getTypeName() {
		return this.typeName;
	}
}
