package com.ge.research.osate.verdict.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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

//this class reads content of MBAS generated .xml file
public class MBASReadXMLFile {
	List<MissionAttributes> list = new ArrayList<MissionAttributes>();

	public MBASReadXMLFile(String fileName) {
		try {
			File fXmlFile = new File(fileName);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("Mission");

			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				MissionAttributes newMission = new MissionAttributes();
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					String missionName = eElement.getAttribute("label");
					newMission.setMission(missionName);
					newMission.setRequirements(extractRequirements(nNode));

					list.add(newMission);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// reads and stores contents of the "Requirement" elements
	protected List<RequirementAttributes> extractRequirements(Node node) {
		List<RequirementAttributes> list = new ArrayList<RequirementAttributes>();
		if (node.getNodeType() != Node.ELEMENT_NODE) {
			System.out.println("Something wrong in the xml");
			return null;
		}
		NodeList nList = ((Element) node).getElementsByTagName("Requirement");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			RequirementAttributes newRequirement = new RequirementAttributes();
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				newRequirement.setRequirement(eElement.getAttribute("label"));
				newRequirement.setLikelihood(eElement.getAttribute("computed_p"),
						eElement.getAttribute("acceptable_p"));
				newRequirement.setPaths(extractPaths(nNode));
				list.add(newRequirement);
			}
		}
		return list;
	}

	// reads and stores contents of the "Path" elements
	protected List<PathAttributes> extractPaths(Node node) {
		List<PathAttributes> list = new ArrayList<PathAttributes>();
		if (node.getNodeType() != Node.ELEMENT_NODE) {
			System.out.println("Something wrong in the xml");
			return null;
		}
		NodeList nList = ((Element) node).getElementsByTagName("Cutset");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			PathAttributes newPath = new PathAttributes();
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				newPath.setLikelihood(eElement.getAttribute("likelihood"));
				NodeList attacks = eElement.getElementsByTagName("Attack");
				if (attacks.getLength() > 0) {
					newPath.setComponentAttacks(extractComponents(attacks.item(0), "attack"));
				}
				NodeList defenses = eElement.getElementsByTagName("Defense");
				if (defenses.getLength() > 0) {
					newPath.setComponentSuggestedDefenses(extractComponents(defenses.item(0), "suggested"));
					newPath.setComponentSuggestedDefensesProfile(extractComponents(defenses.item(0), "profile"));
				}
				list.add(newPath);
			}
		}
		return list;
	}
	
	protected List<PathAttributes.ComponentData> extractComponents(Node node, String dataLabel) {
		List<PathAttributes.ComponentData> data = new ArrayList<>();
		if (node.getNodeType() != Node.ELEMENT_NODE) {
			System.out.println("Something wrong in the xml");
			return null;
		}
		NodeList nList = ((Element) node).getElementsByTagName("Component");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element elem = (Element) nNode;
				data.add(new PathAttributes.ComponentData(elem.getAttribute("comp"),
						elem.getAttribute(dataLabel)));
			}
		}
		return data;
	}

	protected List<MissionAttributes> getContent() {
		return list;
	}
}