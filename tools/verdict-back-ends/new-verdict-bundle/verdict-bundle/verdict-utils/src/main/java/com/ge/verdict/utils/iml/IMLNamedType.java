package com.ge.verdict.utils.iml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ge.verdict.utils.Logger;

public class IMLNamedType {
	String name;
	String qualifiedName;
	boolean isImplementation;
	String implementedComponentName;
	List<String> exhibitedTraitName = new ArrayList<>();
	Map<String, String> subAndCompMap = new HashMap<>();
	Map<String, String> subAndImpMap = new HashMap<>();
	List<IMLConnector> connections = new ArrayList<>();
	List<VerdictProperty> verdictProps = new ArrayList<>();
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getQualifiedName() {
		return qualifiedName;
	}
	public void setQualifiedName(String qualifiedName) {
		this.qualifiedName = qualifiedName;
	}
	public boolean isImplementation() {
		return isImplementation;
	}
	public void setImplementation(boolean isImplementation) {
		this.isImplementation = isImplementation;
	}
	public String getImplementedComponentName() {
		return implementedComponentName;
	}
	public void setImplementedComponentName(String implementedTypeName) {
		this.implementedComponentName = implementedTypeName;
	}
	public List<String> getExhibitedTraitName() {
		return exhibitedTraitName;
	}
	public void setExhibitedTraitName(List<String> exhibitedTraitName) {
		this.exhibitedTraitName = exhibitedTraitName;
	}
	public void addAExhibitedTraitName(String exhibitedTraitName) {
		if(exhibitedTraitName != null) {
			this.exhibitedTraitName.add(exhibitedTraitName);
		}
	}
	public List<IMLConnector> getConnections() {
		return connections;
	}
	public void setConnections(List<IMLConnector> connections) {
		if(connections != null) {
			this.connections.addAll(connections);
		}		
	}
	public List<VerdictProperty> getVerdictProps() {
		return verdictProps;
	}
	public void setVerdictProps(List<VerdictProperty> verdictProps) {
		this.verdictProps = verdictProps;
	}
	public Map<String, String> getSubAndCompMap() {
		return subAndCompMap;
	}
	public void setSubAndCompMap(Map<String, String> subAndCompMap) {
		this.subAndCompMap = subAndCompMap;
	}
	public Map<String, String> getSubAndImpMap() {
		return subAndImpMap;
	}
	public void setSubAndImpMap(Map<String, String> subAndImpMap) {
		this.subAndImpMap = subAndImpMap;
	}
	public void addAConnection(IMLConnector connection) {
		if(connection != null) {
			this.connections.add(connection);
		}
	}	
	public void addAVerdictProperty(VerdictProperty prop) {
		if(prop != null) {
			this.verdictProps.add(prop);
		}
	}
	public void addASubAndCompPair(String subName, String subCompName) {
		if(subName != null && subCompName != null) {
			this.subAndCompMap.put(subName, subCompName);
		} else {
			Logger.warn("Either subcomponent name or its type name is null!");
		}
	}
	public void addASubAndImplPair(String subName, String subImplName) {
		if(subName != null && subImplName != null) {
			this.subAndCompMap.put(subName, subImplName);
		} else {
			Logger.warn("Either subcomponent name or its type name is null!");
		}
	}
}
