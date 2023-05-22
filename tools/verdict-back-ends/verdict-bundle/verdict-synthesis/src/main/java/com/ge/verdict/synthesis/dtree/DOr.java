package com.ge.verdict.synthesis.dtree;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A disjunction of defense tree nodes, indicating that at least one of its children must be
 * satisfied.
 */
public final class DOr implements DTree {
    public final List<DTree> children;

    public DOr(List<DTree> children) {
        this.children = Collections.unmodifiableList(children);
    }

    public DOr(DTree... children) {
        this.children = Arrays.asList(children);
    }

    @Override
    public String prettyPrint() {
        return "("
                + children.stream().map(DTree::prettyPrint).collect(Collectors.joining(" v "))
                + ")";
    }

    @Override
    public BoolExpr toZ3(Context context) {
        return context.mkOr(
                children.stream()
                        .map((child) -> child.toZ3(context))
                        .collect(Collectors.toList())
                        .toArray(new BoolExpr[0]));
    }

    @Override
    public BoolExpr toZ3Multi(Context context) {
        return context.mkOr(
                children.stream()
                        .map((child) -> child.toZ3Multi(context))
                        .collect(Collectors.toList())
                        .toArray(new BoolExpr[0]));
    }

    @Override
    public Formula toLogicNG(FormulaFactory factory) {
        return factory.or(
                children.stream()
                        .map(child -> child.toLogicNG(factory))
                        .collect(Collectors.toList()));
    }

    @Override
    public Optional<DTree> prepare() {
        return Optional.of(
                new DOr(
                        children.stream()
                                .map(DTree::prepare)
                                .flatMap(
                                        opt ->
                                                opt.isPresent()
                                                        ? Stream.of(opt.get())
                                                        : Stream.empty())
                                .collect(Collectors.toList())));
    }
}
