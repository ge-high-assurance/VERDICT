package com.ge.verdict.mbas;

import java.util.Map;

import com.utc.utrc.hermes.iml.iml.NamedType;

public class IML2Alloy {
	IMLParser imlParser;
	
	public IML2Alloy (IMLParser imlParser) {
		this.imlParser = imlParser;
	}
	
	public void execute () {
		buildAlloyConstraints();
	}
	
	/**
	 * 
	 * */
	public void buildAlloyConstraints() {
		for(Map.Entry<String, NamedType> nameToNamedType : this.imlParser.nameToNamedType.entrySet()) {
			
		}		
	}
}
