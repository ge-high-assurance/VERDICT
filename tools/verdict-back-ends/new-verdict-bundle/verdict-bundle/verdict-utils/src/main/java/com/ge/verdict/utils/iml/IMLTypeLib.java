package com.ge.verdict.utils.iml;

public enum IMLTypeLib implements VerdictIMLType {
	Connector("Connector"),
	InDataPort("InDataPort"),
	OutDataPort("OutDataPort"),
	UserDefinedType("UserDefinedType");
	
	private String typeName;
	
	IMLTypeLib(String typeName) {
		this.typeName = typeName;
	}
	
	public String getTypeName() {
		return this.typeName;
	}
	
	public String toString() {
		return this.typeName;
	}
}
