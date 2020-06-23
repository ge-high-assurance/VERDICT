package com.ge.verdict.attackdefensecollector.adtree;

import com.ge.verdict.attackdefensecollector.IndentedStringBuilder;
import com.ge.verdict.attackdefensecollector.Pair;
import com.ge.verdict.attackdefensecollector.Prob;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * A defense to an attack on a system, a fundamental unit of the attack-defense tree.
 *
 * <p>A single Defense object actually contains multiple "defenses" as produced by STEM. This object
 * encapsulates all defenses associated with a single attack on a given system (so the same attack
 * on a different system has a different Defense object).
 *
 * <p>A defense must always refer to an existing attack on the same system. It is not directly
 * connected to the attack in the attack-defense tree, however, but is instead joined by the
 * following structure: (NOT defense) AND attack
 */
public class Defense extends ADTree {
    /** The attack defended by this defense. */
    private Attack attack;
    /** The DNF of defense leaves (see DefenseLeaf below). */
    private List<List<DefenseLeaf>> defenseDnf;

    /**
     * Each leaf contains a defense name and, if the property is implemented, a pair of the
     * implemented property name and the DAL to which it is implemented.
     */
    public static final class DefenseLeaf extends Pair<String, Optional<Pair<String, Prob>>> {
        public DefenseLeaf(String left, Optional<Pair<String, Prob>> right) {
            super(left, right);
        }
    }

    /**
     * Constructs a defense.
     *
     * <p>DNF clauses may be added with the addDefenseClause() method.
     *
     * <p>Note that the probability is the probability that the defense successfully defends the
     * attack. When loading a defense with a DAL (the probability of a successful attack given the
     * defense), one should first NOT that DAL probability.
     *
     * @param attack the attack defended by the defense
     * @param defenseImpliedProperty the implied property of the attack
     * @param prob the probability that the defense successfully defends the attack
     */
    public Defense(Attack attack) {
        this.attack = attack;
        defenseDnf = new ArrayList<>();
    }

    /**
     * Add a conjunction clause to the defense DNF. Each element is a (name, description) pair.
     *
     * @param clause the clause to add
     */
    public void addDefenseClause(List<DefenseLeaf> clause) {
        defenseDnf.add(clause);
    }

    /** @return the attack defended by this defense */
    public Attack getAttack() {
        return attack;
    }

    /**
     * A single Defense object comprises multiple "defenses" as output by STEM. The defenses are in
     * disjunctive-normal form (DNF), i.e. a disjunction of conjunctions of defenses.
     *
     * @return the list of name/description pairs of individual defenses
     */
    public List<List<DefenseLeaf>> getDefenseDnf() {
        return defenseDnf;
    }

    @Override
    public Defense crush() {
        // Fundamental, so no crushing to do
        return this;
    }

    @Override
    public Prob compute() {
        // We flip conjunction and disjunction because attack trees are the complement
        // of defense trees
        Prob total = Prob.certain();
        for (List<DefenseLeaf> term : defenseDnf) {
            Prob termTotal = Prob.impossible();
            for (DefenseLeaf leaf : term) {
                Prob prob = leaf.right.isPresent() ? leaf.right.get().right : Prob.certain();
                termTotal = Prob.or(termTotal, prob);
            }
            total = Prob.and(total, termTotal);
        }
        // negagte because this is complementary
        return Prob.not(total);
    }

    @Override
    public void prettyPrint(IndentedStringBuilder builder) {
        // systemName:(defenseImpliedPropertyFormula)
        builder.append(String.format(Locale.US, "%s:(", attack.getAttackable().getParentName()));

        // Print the DNF
        for (int i = 0; i < defenseDnf.size(); i++) {
            builder.append("(");

            List<DefenseLeaf> term = defenseDnf.get(i);
            for (int j = 0; j < term.size(); j++) {
                DefenseLeaf leaf = term.get(j);
                builder.append(leaf.left);
                builder.append("=");
                if (leaf.right.isPresent()) {
                    builder.append(leaf.right.get().right.toString());
                } else {
                    builder.append("n/a");
                }

                if (j < term.size() - 1) {
                    builder.append(" and ");
                }
            }

            builder.append(")");

            if (i < defenseDnf.size() - 1) {
                builder.append(" or ");
            }
        }

        builder.append(")");
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Defense) {
            Defense otherDefense = (Defense) other;
            return otherDefense.attack.equals(attack) && otherDefense.defenseDnf.equals(defenseDnf);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attack, defenseDnf);
    }
}
