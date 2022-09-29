package com.ge.verdict.synthesis;

import com.ge.verdict.attackdefensecollector.AttackDefenseCollector;
import com.ge.verdict.attackdefensecollector.adtree.ADAnd;
import com.ge.verdict.attackdefensecollector.adtree.ADNot;
import com.ge.verdict.attackdefensecollector.adtree.ADOr;
import com.ge.verdict.attackdefensecollector.adtree.ADTree;
import com.ge.verdict.attackdefensecollector.adtree.Attack;
import com.ge.verdict.attackdefensecollector.adtree.Defense;
import com.ge.verdict.attackdefensecollector.adtree.DefenseCondition;
import com.ge.verdict.synthesis.dtree.ALeaf;
import com.ge.verdict.synthesis.dtree.DAnd;
import com.ge.verdict.synthesis.dtree.DCondition;
import com.ge.verdict.synthesis.dtree.DLeaf;
import com.ge.verdict.synthesis.dtree.DNot;
import com.ge.verdict.synthesis.dtree.DOr;
import com.ge.verdict.synthesis.dtree.DTree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Used for constructing defense trees. */
public class DTreeConstructor {
    /**
     * Construct a defense tree for multiple cyber requirements.
     *
     * @param results
     * @param costModel
     * @param usePartialSolution
     * @param factory
     * @return
     */
    public static DTree construct(
            List<AttackDefenseCollector.Result> results,
            ICostModel costModel,
            boolean usePartialSolution,
            boolean meritAssignment,
            DLeaf.Factory factory) {
        if (meritAssignment && !usePartialSolution) {
            throw new RuntimeException(
                    "Cannot enable merit assignment without also enabling partial solutions!");
        }
        // essentially, construct one dtree for each requirement and AND them together
        return new DAnd(
                results.stream()
                        .map(
                                result ->
                                        construct(
                                                result.adtree,
                                                costModel,
                                                result.cyberReq.getSeverityDal(),
                                                usePartialSolution,
                                                meritAssignment,
                                                factory))
                        .collect(Collectors.toList()));
    }

    /**
     * Construct a defense tree for one cyber requirement. Note: you should probably use the
     * multi-requirement version above.
     *
     * @param adtree
     * @param costModel
     * @param targetDal
     * @param usePartialSolution
     * @param factory
     * @return
     */
    public static DTree construct(
            ADTree adtree,
            ICostModel costModel,
            int targetDal,
            boolean usePartialSolution,
            boolean meritAssignment,
            DLeaf.Factory factory) {
        return (new DTreeConstructor(
                        costModel, targetDal, usePartialSolution, meritAssignment, factory))
                .perform(adtree);
    }

    /** the cost model */
    private final ICostModel costModel;
    /**
     * the dleaf factory, used to make sure there is only ever one instance of a given
     * component-defense pair
     */
    private final DLeaf.Factory factory;
    /** the target DAL for the requirement */
    private final int targetDal;
    /** whether to use partial solutions */
    private final boolean usePartialSolution;
    /** whether we are performing merit assignment */
    private final boolean meritAssignment;

    /** all defenses that have been found */
    private final Set<Defense> defenses;
    /** map from attacks to raw attack leaves */
    private final Map<Attack, Set<ALeaf>> attackALeafMap;
    /** all defense conditions that have been found */
    private final List<DCondition> dconditions;

    private DTreeConstructor(
            ICostModel costModel,
            int dal,
            boolean usePartialSolution,
            boolean meritAssignment,
            DLeaf.Factory factory) {
        this.costModel = costModel;
        this.factory = factory;
        this.targetDal = dal;
        this.usePartialSolution = usePartialSolution;
        this.meritAssignment = meritAssignment;

        defenses = new LinkedHashSet<>();
        attackALeafMap = new LinkedHashMap<>();
        dconditions = new ArrayList<>();
    }

    /**
     * This is the part that actually constructs the dtree.
     *
     * @param adtree
     * @return
     */
    private DTree perform(ADTree adtree) {
        Optional<DTree> resultOpt = constructInternal(adtree);

        if (!resultOpt.isPresent()) {
            return new DOr(Collections.emptyList());
        } else {
            DTree result = resultOpt.get();

            // remove raw attack leaves that are covered by a defense
            for (Defense defense : defenses) {
                if (!attackALeafMap.containsKey(defense.getAttack())) {
                    throw new RuntimeException(
                            "defense references undefined attack: "
                                    + defense.getAttack().getName());
                }
                // set each defended attack leaf to mitigated so that
                // it isn't included in the final tree
                for (ALeaf aleaf : attackALeafMap.get(defense.getAttack())) {
                    aleaf.setMitigated();
                }
            }

            // these are the raw attack leaves that aren't covered by a defense
            Set<ALeaf> unmitigated = new LinkedHashSet<>();
            for (Set<ALeaf> set : attackALeafMap.values()) {
                for (ALeaf aleaf : set) {
                    if (!aleaf.isMitigated()) {
                        unmitigated.add(aleaf);
                    }
                }
            }

            // it is probably a problem if this happens because STEM doesn't output
            // raw attacks without corresponding defenses
            for (ALeaf aleaf : unmitigated) {
                System.out.println("Warning: Unmitigated attack: " + aleaf.getAttack().toString());
            }

            // connect the defense condition to its corresponding dleaf
            for (DCondition dcond : dconditions) {
                Optional<DLeaf.ComponentDefense> compDef =
                        factory.lookup(
                                dcond.defenseCond.getAttackable().getParentName(),
                                dcond.defenseCond.getDefenseProperty());
                if (compDef.isPresent()) {
                    dcond.setCompDef(compDef.get());
                } else {
                    // This means that the defense isn't part of the attack-defense tree,
                    // so we need to create a dleaf (which implicitly creates an SMT variable)
                    // for it so that the DAL can get forced down to zero if necessary

                    // TODO this doesn't actually work. This is the screwy case that we need to fix.
                    DLeaf dleaf =
                            new DLeaf(
                                    dcond.defenseCond.getAttackable().getParentName(),
                                    dcond.defenseCond.getDefenseProperty(),
                                    "",
                                    dcond.defenseCond.getImplDal(),
                                    0,
                                    costModel,
                                    factory,
                                    usePartialSolution,
                                    meritAssignment);
                    dcond.setCompDef(dleaf.componentDefense);
                }
            }

            // important to call prepare - collapses nested NOTs and removes unmitigated raw attack
            // leaves
            Optional<DTree> prepared = result.prepare();
            return prepared.orElse(new DOr(Collections.emptyList()));
        }
    }

    /**
     * Inductively-defined function over attack-defense trees.
     *
     * <p>The mapping from attack-defense tree to defense tree is pretty straightforward. One of the
     * most important things to note is that AND and OR nodes are transposed in the transformation
     * because they mean opposite things in a defense tree compared to an attack-defense tree. (An
     * attack-defense tree is "how to attack", whereas a defense tree is "how to defend".)
     *
     * @param adtree
     * @return
     */
    private Optional<DTree> constructInternal(ADTree adtree) {
        if (adtree instanceof Attack) {
            Attack attack = (Attack) adtree;
            ALeaf aleaf = new ALeaf(attack);
            // keep track of all attack leaves
            if (!attackALeafMap.containsKey(attack)) {
                attackALeafMap.put(attack, new LinkedHashSet<>());
            }
            attackALeafMap.get(attack).add(aleaf);
            return Optional.of(aleaf);
        } else if (adtree instanceof Defense) {
            Defense defense = (Defense) adtree;
            defenses.add(defense);
            return Optional.of(new DNot(constructDefenseTree(defense)));
        } else if (adtree instanceof ADAnd) {
            ADAnd adand = (ADAnd) adtree;
            // Transpose and/or
            return Optional.of(
                    new DOr(
                            adand.children().stream()
                                    .map(this::constructInternal)
                                    .flatMap(
                                            elem ->
                                                    elem.isPresent()
                                                            ? Stream.of(elem.get())
                                                            : Stream.empty())
                                    .collect(Collectors.toList())));
        } else if (adtree instanceof ADOr) {
            ADOr ador = (ADOr) adtree;
            // Transpose and/or
            return Optional.of(
                    new DAnd(
                            ador.children().stream()
                                    .map(this::constructInternal)
                                    .flatMap(
                                            elem ->
                                                    elem.isPresent()
                                                            ? Stream.of(elem.get())
                                                            : Stream.empty())
                                    .collect(Collectors.toList())));
        } else if (adtree instanceof ADNot) {
            ADNot adnot = (ADNot) adtree;
            return constructInternal(adnot.child()).map(DNot::new);
        } else if (adtree instanceof DefenseCondition) {
            DCondition dcond = new DCondition((DefenseCondition) adtree);
            dconditions.add(dcond);
            return Optional.of(dcond);
        } else {
            throw new RuntimeException(
                    "got invalid adtree type: " + adtree.getClass().getCanonicalName());
        }
    }

    /**
     * The defense nodes in the attack-defense tree represent entire DNF defense subtrees. This
     * function produces a defense tree from that DNF.
     *
     * @param defense
     * @return
     */
    private DTree constructDefenseTree(Defense defense) {
        return new DOr(
                defense.getDefenseDnf().stream()
                        .map(
                                term ->
                                        new DAnd(
                                                term.stream()
                                                        .map(leaf -> constructDLeaf(defense, leaf))
                                                        .collect(Collectors.toList())))
                        .collect(Collectors.toList()));
    }

    /**
     * Produce a dleaf from a attack-defense tree defense leaf and its parent defense node.
     *
     * @param defense
     * @param leaf
     * @return
     */
    private DLeaf constructDLeaf(Defense defense, Defense.DefenseLeaf leaf) {
        String system = defense.getAttack().getAttackable().getParentName();
        String attack = defense.getAttack().getName();
        String defenseProp = leaf.left;
        int implDal = leaf.right.isPresent() ? leaf.right.get().right : 0;

        return new DLeaf(
                system,
                defenseProp,
                attack,
                implDal,
                targetDal,
                costModel,
                factory,
                usePartialSolution,
                meritAssignment);
    }
}
