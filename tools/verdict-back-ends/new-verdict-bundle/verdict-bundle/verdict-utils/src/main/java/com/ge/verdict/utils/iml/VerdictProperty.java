package com.ge.verdict.utils.iml;

/**
 * Assumption: we don't need to care about the type of properties
 * 			   so we use propValue of String type to just store 
 * 			   the String representation of the value. 
 * */
public class VerdictProperty {
	public String propGroupName;
	public String propName;
	public String propValue;
	public boolean isBool;
	public boolean isInt;
	public boolean isReal;
	public boolean isEnum;
	
	public boolean isBool() {
		return isBool;
	}
	public void setBool(boolean isBool) {
		this.isBool = isBool;
	}
	public boolean isInt() {
		return isInt;
	}
	public void setInt(boolean isInt) {
		this.isInt = isInt;
	}
	public boolean isReal() {
		return isReal;
	}
	public void setReal(boolean isReal) {
		this.isReal = isReal;
	}
	public boolean isEnum() {
		return isEnum;
	}
	public void setEnum(boolean isEnum) {
		this.isEnum = isEnum;
	}	
	public String getPropGroupName() {
		return propGroupName;
	}
	public void setPropGroupName(String propPkgName) {
		this.propGroupName = propPkgName;
	}	
	public String getPropName() {
		return propName;
	}
	public void setPropName(String propName) {
		this.propName = propName;
	}
	public String getPropValue() {
		return propValue;
	}
	public void setPropValue(String propValue) {
		this.propValue = propValue;
	}	
}
