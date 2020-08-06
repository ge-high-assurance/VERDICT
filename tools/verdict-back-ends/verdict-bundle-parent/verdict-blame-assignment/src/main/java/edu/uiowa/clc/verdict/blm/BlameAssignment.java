/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

   @author: M. Fareed Arif
*/

package edu.uiowa.clc.verdict.blm;

import edu.uiowa.clc.verdict.util.LOGGY;
import edu.uiowa.clc.verdict.util.SummaryProcessor;
import edu.uiowa.clc.verdict.util.VerdictProperty;
import edu.uiowa.clc.verdict.util.WeakAssumption;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for BlameAssignment complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="BlameAssignment">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ViolatedProperties" type="{http://www.example.org/BlameAssignment}ViolatedProperty" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "BlameAssignment",
        propOrder = {"violatedProperties"})
@XmlRootElement
public class BlameAssignment {

    // private static final Logger LOGGER =
    // LoggerFactory.getLogger(BlameAssignment.class);
    private static final Pattern p = Pattern.compile("_port_([\\w]*)$");

    @XmlElement(name = "ViolatedProperties")
    protected List<ViolatedProperty> violatedProperties;

    /**
     * Gets the value of the violatedProperties property.
     *
     * <p>This accessor method returns a reference to the live list, not a snapshot. Therefore any
     * modification you make to the returned list will be present inside the JAXB object. This is
     * why there is not a <CODE>set</CODE> method for the violatedProperties property.
     *
     * <p>For example, to add a new item, do as follows:
     *
     * <pre>
     * getViolatedProperties().add(newItem);
     * </pre>
     *
     * <p>Objects of the following type(s) are allowed in the list {@link ViolatedProperty }
     */
    public List<ViolatedProperty> getViolatedProperties() {

        if (violatedProperties == null) {
            violatedProperties = new ArrayList<ViolatedProperty>();
        }
        return this.violatedProperties;
    }

    public BlameAssignment compute_blame_assignment(
            File kind2ResultFile,
            HashMap<String, HashSet<String>> intrumented_cmp_link,
            boolean component_level)
            throws FileNotFoundException {

        Vector<VerdictProperty> property_result = SummaryProcessor.readResults(kind2ResultFile);
        return blame_assignment(property_result, intrumented_cmp_link, component_level);
    }

    // Two Types of methods to run Blame-assignment
    // a. use XML file input and process result.
    // b. Get verdict Properties and compile result.
    public BlameAssignment blame_assignment(
            Vector<VerdictProperty> verdict_property,
            HashMap<String, HashSet<String>> intrumented_cmp_link,
            boolean component_level) {

        BlameAssignment blame_assignment = new BlameAssignment();

        // printThreats(intrumented_cmp_link);
        LOGGY.info("------------Violated Properties----------");

        for (VerdictProperty property : verdict_property) {

            if (property.isSAT()) {
                // True Property
            } else {
                // False Property

                if (!property.getAllWeakAssumptions().isEmpty()) {

                    ViolatedProperty violated_property = new ViolatedProperty();
                    // ID
                    violated_property.setPropertyID(property.getId());

                    // No weak Assumption Computed
                    // Vector<String> wk_assumptions =
                    // property.getFalseWeakAssumptions();

                    MinA mina = computeMinA(property.getAllWeakAssumptions(), component_level);
                    violated_property.setMinA(mina);

                    // Effected Components
                    for (Component cmp : mina.getComponents()) {

                        if (cmp.isCompromised()) {
                            List<Attack> attacks =
                                    applicableAttack(cmp.getComponentID(), intrumented_cmp_link);
                            //
                            // attack.setAttackDescription(attack.getAttackDescription());
                            //
                            // violated_property.setApplicableThreat(attack);
                            for (Attack attack_found : attacks) {
                                boolean present = false;
                                for (Attack eAttack : violated_property.getApplicableThreat()) {
                                    if (attack_found.getAttackId().equals(eAttack.getAttackId())) {
                                        present = true;
                                    }
                                }
                                if (!present) {
                                    violated_property.getApplicableThreat().add(attack_found);
                                }
                            }

                            // if (attack.getAttackId() != null) {
                            // break;
                            // }
                        }
                    }

                    // Effected Links
                    for (Link link : mina.getLinks()) {

                        // if (link.isCompromised()) {
                        // System.out.println("+++> LinkID --> :" +
                        // link.getLinkID());
                        if (link.isCompromised()) {

                            List<Attack> attacks =
                                    applicableAttack(
                                            link_to_port(link.getLinkID()), intrumented_cmp_link);
                            //
                            // attack.setAttackDescription(attack.getAttackDescription());
                            //
                            // violated_property.setApplicableThreat(attack);
                            for (Attack attack_found : attacks) {
                                boolean present = false;
                                for (Attack eAttack : violated_property.getApplicableThreat()) {
                                    if (attack_found.getAttackId().equals(eAttack.getAttackId())) {
                                        present = true;
                                    }
                                }
                                if (!present) {
                                    violated_property.getApplicableThreat().add(attack_found);
                                }
                            }

                            // if (attack.getAttackId() != null) {
                            // break;
                            // }
                        }
                    }

                    // Renaming link to support better readability
                    for (Link link : mina.getLinks()) {
                        rename_link(link);
                    }

                    LOGGY.info("Failed Property: " + violated_property.getPropertyID());

                    for (Attack atk : violated_property.getApplicableThreat()) {
                        LOGGY.info("Applicable Attack: " + atk.getAttackId());
                    }

                    LOGGY.info("MinA: " + violated_property.getMinA());
                    LOGGY.info("-------------------------------------------------------");

                    blame_assignment.getViolatedProperties().add(violated_property);
                }
            }
        }
        // LOGGY.info("------------Violated Properties----------");
        // rename_mina(blame_assignment);

        return blame_assignment;
    }

    private MinA computeMinA(Vector<WeakAssumption> wk_assumptions, boolean component_level) {

        MinA mina = new MinA();

        if (component_level) {
            for (WeakAssumption wk : wk_assumptions) {
                Component cmp = new Component();
                cmp.setComponentID(wk.getwId());
                // flip status
                boolean c_status = !wk.getStatus();
                cmp.setCompromised(c_status);
                mina.getComponents().add(cmp);
            }
        } else {
            for (WeakAssumption wk : wk_assumptions) {
                Link link = new Link();

                link.setLinkID(wk.getwId());
                boolean c_status = !wk.getStatus();
                link.setCompromised(c_status);
                mina.getLinks().add(link);
            }
        }
        return mina;
    }

    // AttackID, Component Set
    private List<Attack> applicableAttack(
            String cmp_link_id, HashMap<String, HashSet<String>> intrumented_cmp_link) {

        List<Attack> attacks = new ArrayList<Attack>();

        // System.out.println("Matching ID: " + cmp_link_id);
        for (String attack_id : intrumented_cmp_link.keySet()) {

            HashSet<String> comps_links = intrumented_cmp_link.get(attack_id);

            // System.out.println("Attack ID: " + cmp_link_id);

            // System.out.println(attack_id + " >>>>>>>>>>> " + cmp_link_id);

            if (comps_links.contains(cmp_link_id)) {

                // System.out.println("MATCHED: " + attack_id + " >>>>>>>>>>> " +
                // cmp_link_id);
                Attack app_attack = new Attack();

                AttackType type = AttackType.valueOf(attack_id);
                app_attack.setAttackId(type);
                app_attack.setAttackDescription(app_attack.getAttackDescription());

                attacks.add(app_attack);

                //                break;
            }
        }

        return attacks;
    }

    private void rename_link(Link selected_link) {

        String link_name = selected_link.getLinkID();

        link_name = link_name.replaceAll("_port_", ".");

        link_name = link_name.replace("_dot_", ".");

        selected_link.setLinkID(link_name);
    }

    private String link_to_port(String link_ID) {
        // System.out.print(link_ID);

        Matcher m = p.matcher(link_ID);
        if (m.find()) {
            link_ID = m.group(1);
        }

        // System.out.print(" <--> " + link_ID);

        return link_ID;
    }
}
