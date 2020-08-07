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

//this class extracts the contents of CRV .xml and stores in the data-structures
public class CRVReadXMLFile {
	List<CRVResultAttributes> results = new ArrayList<CRVResultAttributes>();
	List<IVCNode> ivc = new ArrayList<IVCNode>();

	public CRVReadXMLFile(String fileName1, String fileName2) {
		try {
			File fXmlFile = new File(fileName1);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("Property");

			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				CRVResultAttributes newProperty = new CRVResultAttributes();
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					newProperty.setProperty(eElement.getAttribute("name"));
					newProperty.setSource(
							eElement.getElementsByTagName("Answer").item(0).getAttributes().getNamedItem("source")
									.getTextContent());
					newProperty.setAnswer(eElement.getElementsByTagName("Answer").item(0).getTextContent());
					if (eElement.getElementsByTagName("Answer").item(0).getTextContent().equals("falsifiable")) {
						newProperty.setCntExample(extractCE(eElement.getElementsByTagName("CounterExample").item(0)));
						newProperty.setBlameAssignment(
								extractBlameAssignment(eElement.getAttribute("name"), doc, fileName2));
					} else if (eElement.getElementsByTagName("Answer").item(0).getTextContent().equals("unknown")) {
						try {
							newProperty.setValidTill(eElement.getElementsByTagName("TrueFor").item(0).getTextContent());
						} catch (Exception e) {
							// do nothing
						}
					}
					results.add(newProperty);
				}
			}
			
			readIVC(doc);
			
		} catch (Exception e) {
			System.out.println("Error in loading .xml file");
			e.printStackTrace();
		}
	}

	protected void readIVC(Document doc) {
		NodeList MESList = doc.getElementsByTagName("ModelElementSet");

        for (int i = 0; i < MESList.getLength(); ++i) {
            Node MESNode = MESList.item(i);

            if (MESNode.getNodeType() != Node.ELEMENT_NODE) continue;

            Element MESElement = (Element) MESNode;
            NodeList nList = MESElement.getElementsByTagName("Node");

            for (int j = 0; j < nList.getLength(); ++j) {
                Node nNode = nList.item(j);

                if (nNode.getNodeType() != Node.ELEMENT_NODE) continue;

                Element nodeElement = (Element) nNode;

                String element_name = nodeElement.getAttribute("name");
                element_name = element_name.replace("_dot_", ".");
                
                IVCNode ivcNode = new IVCNode();
                ivcNode.setNodeName(element_name);
                
                NodeList eList = nodeElement.getElementsByTagName("Element");

                for (int k = 0; k < eList.getLength(); ++k) {
                    Node eNode = eList.item(k);

                    if (eNode.getNodeType() != Node.ELEMENT_NODE) continue;

                    Element elementElement = (Element) eNode;
                    
                    IVCElement ivcElement = new IVCElement();
                    String category = elementElement.getAttribute("category").toUpperCase();
                    ivcElement.setCategory(IVCElement.Category.valueOf(category));
                    ivcElement.setName(elementElement.getAttribute("name"));
                    
                    ivcNode.getNodeElements().add(ivcElement);
                }
                
                ivc.add(ivcNode);
            }
        }
	}
	
	// extracts content of a counter-example from the .xml
	protected List<CounterExampleAttributes> extractCE(Node node) {
		List<CounterExampleAttributes> list = new ArrayList<CounterExampleAttributes>();
		if (node.getNodeType() != Node.ELEMENT_NODE) {
			System.out.println("Something wrong in the xml");
			return null;
		}
		NodeList nList = ((Element) node).getElementsByTagName("Node");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			CounterExampleAttributes newNode = new CounterExampleAttributes();
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				newNode.setNodeName(eElement.getAttribute("name").replace("_dot_", "."));
				newNode.setNodeAttr(extractCENode(eElement.getElementsByTagName("Stream")));
				list.add(newNode);
			}
		}
		return list;
	}

	// extracts content related to blame-assignment from the .xml
	protected BlameAssignmentInfo extractBlameAssignment(String propertyName, Document doc, String fileName2) {
		BlameAssignmentInfo blm = new BlameAssignmentInfo();
		if (fileName2 != null) {
			try {
				File fXmlFile = new File(fileName2);
				if (fXmlFile.exists()) {
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					doc = dBuilder.parse(fXmlFile);
					doc.getDocumentElement().normalize();
				}
			} catch (Exception e) {
				System.out.println("Error in loading blame-assignment .xml file");
				e.printStackTrace();
			}
		}
		NodeList nList = doc.getElementsByTagName("ViolatedProperties");
		Element eElement = null;
		for (int i = 0; i < nList.getLength(); i++) {
			Node nNode = nList.item(i);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				eElement = (Element) nNode;
				if (eElement.getAttribute("PropertyID").equals(propertyName)) {
					break;
				}
			}
		}
		if (eElement != null) {
			NodeList minList = eElement.getElementsByTagName("MinA");
			if (minList.getLength() > 1) {
				System.out.println("Output generated by CRVReportGenerator is not complete. Currently ReportGenerator"
						+ " can only handle one minA instance");
			}
			Node nMinA = minList.item(0);
			if (nMinA != null) {
				NodeList componentList = ((Element) nMinA).getElementsByTagName("Components");
				for (int i = 0; i < componentList.getLength(); i++) {
					Node nComponent = componentList.item(i);
					String componentName = nComponent.getAttributes().getNamedItem("ComponentID").getTextContent();
					if ("true".equals(nComponent.getAttributes().getNamedItem("Compromised").getTextContent())) {
						blm.addComponent(componentName);
					} else {
						blm.addComponentUncompromised(componentName);
					}
				}
				NodeList linkList = ((Element) nMinA).getElementsByTagName("Links");
				for (int i = 0; i < linkList.getLength(); i++) {
					Node nLink = linkList.item(i);
					String linkName = nLink.getAttributes().getNamedItem("LinkID").getTextContent();
					if ("true".equals(nLink.getAttributes().getNamedItem("Compromised").getTextContent())) {
						blm.addLink(linkName);
					} else {
						blm.addLinkUncompromised(linkName);
					}
				}
				NodeList threatList = eElement.getElementsByTagName("ApplicableThreat");
				
				for (int i = 0; i < threatList.getLength(); i++) {
					Node nThreat = threatList.item(i);
					if (nThreat != null) {
						NodeList descriptionsList = ((Element) nThreat).getElementsByTagName("AttackDescription");
						if (descriptionsList.getLength() > 1) {
							System.out
									.println(
											"Output generated by CRVReportGenerator is not complete. Currently ReportGenerator"
													+ " can only handle one AttackDescription instance");
						}
						Node nDescription = descriptionsList.item(0);
						if (nDescription != null) {
							blm.addThreat(nDescription.getTextContent());
						}
					}
				}
			}
		}
		return blm;
	}

	// extracts content of a counter-example "NODE" from the .xml
	protected List<CENode> extractCENode(NodeList nList) {
		List<CENode> list = new ArrayList<CENode>();

		for (int i = 0; i < nList.getLength(); i++) {
			Node nNode = nList.item(i);
			CENode newString = new CENode();
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				newString.setVarName(eElement.getAttribute("name").replace("_dot_", "."));
				if (eElement.getAttribute("type").equals("enum")) {
					newString.setVarType(eElement.getAttribute("enumName"));
				} else {
					newString.setVarType(eElement.getAttribute("type"));
				}
				newString.setVarClass(eElement.getAttribute("class"));
				NodeList values = eElement.getElementsByTagName("Value");
				List<String> stringValues = new ArrayList<String>();
				List<String> stringInst = new ArrayList<String>();
				for (int j = 0; j < values.getLength(); j++) {
					if (values.item(j).getNodeType() == Node.ELEMENT_NODE) {
						stringInst.add(((Element) values.item(j)).getAttribute("instant"));
						stringValues.add(((Element) values.item(j)).getTextContent());
					}

				}
				newString.setVarInst(stringInst);
				newString.setVarValue(stringValues);
				list.add(newString);
			}
		}
		return list;
	}

	protected List<CRVResultAttributes> getResults() {
		return results;
	}
	
	protected List<IVCNode> getIVC() {
		return ivc;
	}
}