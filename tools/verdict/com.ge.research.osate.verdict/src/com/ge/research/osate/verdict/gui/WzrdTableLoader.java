package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.serializer.ISerializer;
import org.osate.aadl2.impl.DefaultAnnexSubclauseImpl;
import org.osate.aadl2.impl.SystemTypeImpl;

import com.ge.research.osate.verdict.dsl.serializer.VerdictSerializer;
import com.ge.research.osate.verdict.dsl.ui.VerdictUiModule;
import com.ge.research.osate.verdict.dsl.ui.internal.VerdictActivator;
import com.ge.research.osate.verdict.dsl.verdict.CyberRelInputLogic;
import com.ge.research.osate.verdict.dsl.verdict.CyberRelOutputLogic;
import com.ge.research.osate.verdict.dsl.verdict.CyberReq;
import com.ge.research.osate.verdict.dsl.verdict.LPort;
import com.ge.research.osate.verdict.dsl.verdict.Statement;
import com.ge.research.osate.verdict.dsl.verdict.Verdict;
import com.ge.research.osate.verdict.dsl.verdict.VerdictContractSubclause;
import com.ge.research.osate.verdict.dsl.verdict.impl.CyberRelImpl;
import com.ge.research.osate.verdict.dsl.verdict.impl.CyberReqConditionLogicImpl;
import com.ge.research.osate.verdict.dsl.verdict.impl.CyberReqImpl;

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/
//this class loads the Wizard Table contents with the existing Annex-statements in the .aadl script
public class WzrdTableLoader {

	private boolean noExistingVerdictAnnex = true; // boolean containing information about existence of existing verdict annex in the selected SystemTypeImpl in
													// invoking .aadl

	private List<WzrdTableRow> tableContent = new ArrayList<WzrdTableRow>(); // stores the extracted information
	private boolean systemLevel; // boolean containing information about whether selected SystemTypeImpl instance is a component or a system
	private DrpDnLists drpdn; // contains the drop-down list contents and information/header of the available INPorts/OUTPorts

	public WzrdTableLoader(SystemTypeImpl sys, Boolean sysLevel, DrpDnLists list) {
		systemLevel = sysLevel;
		List<Statement> stmts = getStatements(sys);
		drpdn = list;

		for (int i = 0; i < stmts.size(); i++) {
			WzrdTableRow tableRow;
			if (!systemLevel) {
				tableRow = getTableRow(stmts.get(i));
				tableContent.add(tableRow);
			} else {
				if (stmts.get(i) instanceof CyberReq) {
					tableRow = getTableRow((CyberReq) stmts.get(i));
					tableContent.add(tableRow);
				}
			}
		}
	}

	// Extract the existing statements of the current component from .aadl script
	private List<Statement> getStatements(SystemTypeImpl sys) {

		List<EObject> objs = sys.eContents();
		List<Statement> stmts = new ArrayList<Statement>();
		for (int i = 0; i < objs.size(); i++) {
			if (objs.get(i) instanceof DefaultAnnexSubclauseImpl) {
				if (!((DefaultAnnexSubclauseImpl) objs.get(i)).getName().equals("verdict")) {
					continue;
				}
				Verdict vd = ((VerdictContractSubclause) ((DefaultAnnexSubclauseImpl) objs.get(i))
						.getParsedAnnexSubclause()).getContract();
				stmts = vd.getElements();
				break;
			}
		}
		return stmts;
	}

	// Store existing cyber-relation into a data-structure
	private WzrdTableRow getTableRow(Statement stmt) {
		WzrdTableRow tableRow = new WzrdTableRow();

		tableRow.setFormulaID(stmt.getId());
		tableRow.setNewRow(false);

		tableRow.setComment(((CyberRelImpl) stmt).getComment());
		tableRow.setDescription(((CyberRelImpl) stmt).getDescription());
		tableRow.setExternal(((CyberRelImpl) stmt).getExternal());
		tableRow.setPhase(((CyberRelImpl) stmt).getPhases());

		List<EObject> objList = stmt.eContents();

		for (int j = 0; j < objList.size(); j++) {
			EObject objInst = objList.get(j);

			if (objInst instanceof CyberRelOutputLogic) {
				CyberRelOutputLogic rhs = (CyberRelOutputLogic) objInst;
				LPort lp = rhs.getValue();
				tableRow.setFirstElement(drpdn.findIndex(drpdn.outPorts, lp.getPort()));
				tableRow.setSecondElement(drpdn.findIndex(drpdn.CIA_ABBREV, lp.getCia().getLiteral()));

			} else if (objInst instanceof CyberRelInputLogic) {
				ISerializer serializer = VerdictActivator.getInstance().getInjector(VerdictUiModule.INJECTOR_NAME)
						.getInstance(VerdictSerializer.class);
				tableRow.setThirdElement(serializer.serialize(objInst));
			}
		}
		if (tableRow.getThirdElement() == null) {
			tableRow.setThirdElement("TRUE");
		}

		return tableRow;
	}

	// Store existing cyber-requirement into a data-structure
	private WzrdTableRow getTableRow(CyberReq stmt) {
		WzrdTableRow tableRow = new WzrdTableRow();

		tableRow.setFormulaID(stmt.getId());
		tableRow.setNewRow(false);

		tableRow.setComment(((CyberReqImpl) stmt).getComment());
		tableRow.setDescription(((CyberReqImpl) stmt).getDescription());
		tableRow.setExternal(((CyberReqImpl) stmt).getExternal());
		tableRow.setPhase(((CyberReqImpl) stmt).getPhases());

		tableRow.setFirstElement(drpdn.findIndex(drpdn.CIA_ABBREV, stmt.getCia().getLiteral()));
		tableRow.setSecondElement(drpdn.findIndex(drpdn.SEVERITY, stmt.getSeverity().getLiteral()));

		List<EObject> objList = stmt.eContents();

		for (int j = 0; j < objList.size(); j++) {
			EObject objInst = objList.get(j);

			if (objInst instanceof CyberReqConditionLogicImpl) {
				CyberReqConditionLogicImpl creq = (CyberReqConditionLogicImpl) objInst;
				ISerializer serializer = VerdictActivator.getInstance().getInjector(VerdictUiModule.INJECTOR_NAME)
						.getInstance(VerdictSerializer.class);
				tableRow.setThirdElement(serializer.serialize(creq));
			}
		}
		return tableRow;
	}

	public List<WzrdTableRow> getTableContent() {
		return tableContent;
	}

	public boolean isExistingInAadlFile() {
		return !noExistingVerdictAnnex;
	}

}