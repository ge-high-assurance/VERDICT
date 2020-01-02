package com.ge.research.osate.verdict.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/
//this class performs the overall activities of generating report from CRV .xml
//to be called from MBAS handler (just before the handler returns for static implementation
//dynamic update can be implemented by creating two threads: one for MBAS tool and the other for this class)
public class MBASReportGenerator implements Runnable {
	private String fileName1;
	private String fileName2;
	public static IWorkbenchWindow window;
	private List<MissionAttributes> missions = new ArrayList<MissionAttributes>();
	private Map<String, List<MBASSafetyResult>> safetyResults;

	public MBASReportGenerator(String applicableDefense, String implProperty, String safetyApplicableDefense,
			String safetyImplProperty, IWorkbenchWindow window) {
		this.fileName1 = applicableDefense;
		this.fileName2 = implProperty;
		MBASReportGenerator.window = window;
		IWorkbenchPage wp = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IViewPart myView1 = wp.findView(MBASResultsView.ID);
		if (myView1 != null) {
			wp.hideView(myView1);
		}
		IViewPart myView2 = wp.findView(CapecDefenseView.ID);
		if (myView2 != null) {
			wp.hideView(myView2);
		}
		IViewPart myView3 = wp.findView(SafetyCutsetsView.ID);
		if (myView3 != null) {
			wp.hideView(myView3);
		}
		MBASResultSummary result = new MBASResultSummary(applicableDefense, implProperty);
		missions = result.getMissions();
		safetyResults = loadSafetyResults(safetyApplicableDefense, safetyImplProperty);
		result.updateMissionsWithSafety(safetyResults);

		showView(window);
	}

	@Override
	public void run() {
		new MBASResultSummary(fileName1, fileName2);
	}

	// invokes the MBAS Result viewer-tab in OSATE
	protected void showView(IWorkbenchWindow window) {
		/*
		 * This command is executed while the xtext document is locked. Thus it must be async
		 * otherwise we can get a deadlock condition if the UI tries to lock the document,
		 * e.g., to pull up hover information.
		 */
		window.getShell().getDisplay().asyncExec(() -> {
			try {
				MBASResultsView.missions = missions;
				MBASResultsView.safetyResults = safetyResults;
				window.getActivePage().showView(MBASResultsView.ID);
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		});
	}

	private Map<String, List<MBASSafetyResult>> loadSafetyResults(String applicableDefenseFile,
			String implPropertyFile) {
		Map<String, List<MBASSafetyResult>> results = new LinkedHashMap<>();

		// We don't use both files right now because AFAIK they are totally identical

		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new File(applicableDefenseFile));
			doc.getDocumentElement().normalize();

			NodeList missionNodes = doc.getElementsByTagName("Mission");

			for (int i = 0; i < missionNodes.getLength(); i++) {
				Node missionNode = missionNodes.item(i);
				if (missionNode.getNodeType() == Node.ELEMENT_NODE) {
					Element missionElem = (Element) missionNode;
					String missionName = missionElem.getAttribute("label");
					List<MBASSafetyResult> requirements = new ArrayList<>();

					NodeList reqNodes = missionElem.getElementsByTagName("Requirement");
					for (int j = 0; j < reqNodes.getLength(); j++) {
						Node reqNode = reqNodes.item(j);
						if (reqNode.getNodeType() == Node.ELEMENT_NODE) {
							Element reqElem = (Element) reqNode;
							String reqName = reqElem.getAttribute("label");
							String defenseType = reqElem.getAttribute("defenseType");
							String computedLikelihood = reqElem.getAttribute("computed_p");
							String acceptableLikelihood = reqElem.getAttribute("acceptable_p");
							List<MBASSafetyResult.CutsetResult> cutsets = new ArrayList<>();

							NodeList cutsetNodes = reqElem.getElementsByTagName("Cutset");
							for (int k = 0; k < cutsetNodes.getLength(); k++) {
								Node cutsetNode = cutsetNodes.item(k);
								if (cutsetNode.getNodeType() == Node.ELEMENT_NODE) {
									Element cutsetElem = (Element) cutsetNode;
									String likelihood = cutsetElem.getAttribute("probability");
									List<MBASSafetyResult.CutsetResult.Event> events = new ArrayList<>();

									NodeList componentNodes = cutsetElem.getElementsByTagName("Component");
									for (int m = 0; m < componentNodes.getLength(); m++) {
										Node componentNode = componentNodes.item(m);
										if (componentNode.getNodeType() == Node.ELEMENT_NODE) {
											Element componentElem = (Element) componentNode;
											String componentName = componentElem.getAttribute("name");

											NodeList eventNodes = componentElem.getElementsByTagName("Event");
											if (eventNodes.getLength() > 0
													&& eventNodes.item(0).getNodeType() == Node.ELEMENT_NODE) {
												Element eventElem = (Element) eventNodes.item(0);
												String eventName = eventElem.getAttribute("name");

												events.add(new MBASSafetyResult.CutsetResult.Event(componentName,
														eventName));
											}
										}
									}

									cutsets.add(new MBASSafetyResult.CutsetResult(likelihood, events));
								}
							}

							requirements.add(
									new MBASSafetyResult(reqName, defenseType, computedLikelihood, acceptableLikelihood,
											cutsets));
						}
					}
					results.put(missionName, requirements);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return results;
	}
}