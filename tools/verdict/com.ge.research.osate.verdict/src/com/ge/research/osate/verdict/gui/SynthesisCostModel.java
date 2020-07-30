package com.ge.research.osate.verdict.gui;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Objects;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SynthesisCostModel {
	public static final String COMPONENT_ALL = "[all]";
	public static final String DEFENSE_PROP_ALL = "[all]";
	public static final String DAL_LINEAR = "[linear]";

	public static class Rule {
		private final int id;
		public final Optional<String> component;
		public final Optional<String> defenseProperty;
		public final Optional<Integer> dal;
		public final double value;

		private Rule(int id, Optional<String> component, Optional<String> defenseProperty, Optional<Integer> dal,
				double value) {
			this.id = id;
			this.component = component;
			this.defenseProperty = defenseProperty;
			this.dal = dal;
			this.value = value;
		}

		@Override
		public int hashCode() {
			return Objects.hash(id);
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof Rule && ((Rule) other).id == id;
		}

		public String getComponentStr() {
			return component.orElse(COMPONENT_ALL);
		}

		public String getDefensePropertyStr() {
			return defenseProperty.orElse(DEFENSE_PROP_ALL);
		}

		public String getDalStr() {
			return dal.map(i -> i.toString()).orElse(DAL_LINEAR);
		}

		public String getValueStr() {
			return Double.toString(value);
		}

		public Rule updateComponent(String str) {
			Optional<String> newComponent = COMPONENT_ALL.equals(str) ? Optional.empty() : Optional.of(str);
			return new Rule(id, newComponent, defenseProperty, dal, value);
		}

		public Rule updateDefenseProperty(String str) {
			Optional<String> newDefenseProperty = DEFENSE_PROP_ALL.equals(str) ? Optional.empty() : Optional.of(str);
			return new Rule(id, component, newDefenseProperty, dal, value);
		}

		public Rule updateDal(String str) {
			Optional<Integer> newDal = DAL_LINEAR.equals(str) ? Optional.empty() : Optional.of(Integer.parseInt(str));
			return new Rule(id, component, defenseProperty, newDal, value);
		}

		public Rule updateValue(String str) {
			double newValue = Double.parseDouble(str);
			return new Rule(id, component, defenseProperty, dal, newValue);
		}
	}

	public final IObservableList<Rule> rules;
	private int ruleIdCounter;

	public Rule createRule(Optional<String> component, Optional<String> defenseProperty, Optional<Integer> dal,
			double value) {
		return new Rule(ruleIdCounter++, component, defenseProperty, dal, value);
	}

	public void updateRule(Rule oldRule, Rule newRule) {
		int pos = rules.indexOf(oldRule);
		if (pos == -1) {
			throw new RuntimeException("old rule not in list");
		}
		rules.set(pos, newRule);
	}

	public SynthesisCostModel() {
		rules = new WritableList<>();
		ruleIdCounter = 0;
	}

	private static Optional<String> mkStrOpt(String str) {
		return str != null && str.length() > 0 ? Optional.of(str) : Optional.empty();
	}

	public void loadFileXml(File file) {
		rules.clear();
		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
			Element root = doc.getDocumentElement();

			NodeList nodes = root.getElementsByTagName("cost");
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				if (node instanceof Element) {
					Element elem = (Element) node;
					rules.add(createRule(mkStrOpt(elem.getAttribute("component")), mkStrOpt(elem.getAttribute(
							"defense")),
							mkStrOpt(elem.getAttribute("dal")).map(Integer::parseInt),
							Double.parseDouble(elem.getTextContent())));
				}
			}
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	private void toStreamResult(StreamResult target) {
		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element root = doc.createElement("cost-model");
			doc.appendChild(root);

			for (Rule rule : rules) {
				Element elem = doc.createElement("cost");
				root.appendChild(elem);

				if (rule.component.isPresent()) {
					elem.setAttribute("component", rule.component.get());
				}
				if (rule.defenseProperty.isPresent()) {
					elem.setAttribute("defense", rule.defenseProperty.get());
				}
				if (rule.dal.isPresent()) {
					elem.setAttribute("dal", rule.dal.get().toString());
				}
				elem.setTextContent(Double.toString(rule.value));
			}

			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(new DOMSource(doc), target);
		} catch (ParserConfigurationException | TransformerException e) {
			e.printStackTrace();
		}
	}

	public void toFileXml(File file) {
		toStreamResult(new StreamResult(file));
	}

	public void toPrintStream(PrintStream stream) {
		toStreamResult(new StreamResult(stream));
	}
}
