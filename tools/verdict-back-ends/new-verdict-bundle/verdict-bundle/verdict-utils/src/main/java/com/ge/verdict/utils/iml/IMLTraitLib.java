package com.ge.verdict.utils.iml;

import java.util.HashSet;
import java.util.Set;

public enum IMLTraitLib {
	Contract("Contract"),
	Port("Port"),
	Data("Data"),
	System("System"),
	Implements("Implements"),
	Component("Component"),
	Process("Process"),
	Thread("Thread"),
	SubProgram("SubProgram");
	
	private String traitName;
	Set<String> builtinTraits = new HashSet<>();
	
	IMLTraitLib(String traitName) {
		this.traitName = traitName;
		this.builtinTraits.add(traitName);
	}
	
	public String getTraitName() {
		return this.traitName;
	}
	
	public String toString() {
		return this.traitName;
	}
	public boolean isFromTraitLib(String traitName) {
		return builtinTraits.contains(traitName);
	}	
}
