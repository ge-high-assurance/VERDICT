package com.ge.research.osate.verdict.dsl.type;

import java.util.LinkedHashMap;
import java.util.Optional;

import com.ge.research.osate.verdict.dsl.verdict.Intro;

public class VerdictVariableImpl implements VerdictVariable {
	public String id;
	public Optional<VerdictType> type;

	public VerdictVariableImpl(String id, Optional<VerdictType> type) {
		this.id = id;
		this.type = type;
	}

	/**
	 * Creates a new VerdictVariable from an Intro AST object.
	 *
	 * @param intro the Intro
	 * @param types the preloaded list of types
	 * @return the VerdictVariable
	 */
	public static VerdictVariableImpl fromIntro(Intro intro, LinkedHashMap<String, VerdictType> types) {
		Optional<VerdictType> type = Optional.ofNullable(types.get(intro.getType()));
		return new VerdictVariableImpl(intro.getId(), type);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Optional<VerdictType> getType() {
		return type;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof VerdictVariable) {
			VerdictVariable otherVar = (VerdictVariable) other;
			return otherVar.getId().equals(id) && otherVar.getType().equals(type);
		}

		return false;
	}
}
