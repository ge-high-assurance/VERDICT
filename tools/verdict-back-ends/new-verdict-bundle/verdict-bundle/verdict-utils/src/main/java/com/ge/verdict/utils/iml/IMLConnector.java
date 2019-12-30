package com.ge.verdict.utils.iml;

import java.util.ArrayList;
import java.util.List;

public class IMLConnector {
	public enum PortDir {IN, OUT, INOUT};
	String connectorName;
	String srcPortName;
	String srcCompName;
	String srcImplName;
	String srcInstName;
	PortDir srcPortDir;
	
	String destPortName;
	String destCompName;
	String destImplName;	
	String destInstName;	
	PortDir destPortDir;
	
	List<VerdictProperty> verdictProps = new ArrayList<>();
	
	public String getConnectorName() {
		return connectorName;
	}
	public void setConnectorName(String connectorName) {
		this.connectorName = connectorName;
	}
	public String getSrcPortName() {
		return srcPortName;
	}
	public void setSrcPortName(String srcPortName) {
		this.srcPortName = srcPortName;
	}
	public String getSrcCompName() {
		return srcCompName;
	}
	public void setSrcCompName(String srcCompName) {
		this.srcCompName = srcCompName;
	}
	public String getSrcImplName() {
		return srcImplName;
	}
	public void setSrcImplName(String srcImplName) {
		this.srcImplName = srcImplName;
	}
	public String getSrcInstName() {
		return srcInstName;
	}
	public void setSrcInstName(String srcInstName) {
		this.srcInstName = srcInstName;
	}
	public PortDir getSrcPortDir() {
		return srcPortDir;
	}
	public void setSrcPortDir(PortDir srcPortDir) {
		this.srcPortDir = srcPortDir;
	}
	public String getDestPortName() {
		return destPortName;
	}
	public void setDestPortName(String destPortName) {
		this.destPortName = destPortName;
	}
	public String getDestCompName() {
		return destCompName;
	}
	public void setDestCompName(String destCompName) {
		this.destCompName = destCompName;
	}
	public String getDestImplName() {
		return destImplName;
	}
	public void setDestImplName(String destImplName) {
		this.destImplName = destImplName;
	}
	public String getDestInstName() {
		return destInstName;
	}
	public void setDestInstName(String destInstName) {
		this.destInstName = destInstName;
	}
	public PortDir getDestPortDir() {
		return destPortDir;
	}
	public void setDestPortDir(PortDir destPortDir) {
		this.destPortDir = destPortDir;
	}
	public List<VerdictProperty> getVerdictProps() {
		return verdictProps;
	}
	public void setVerdictProps(List<VerdictProperty> verdictProps) {
		this.verdictProps = verdictProps;
	}
	public void addAVerdictProperty(VerdictProperty prop) {
		if(prop != null) {
			this.verdictProps.add(prop);
		}
	}		
}
