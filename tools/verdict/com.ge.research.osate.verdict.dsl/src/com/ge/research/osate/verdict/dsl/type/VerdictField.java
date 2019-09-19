package com.ge.research.osate.verdict.dsl.type;

import java.util.List;

/**
 * The field of a type, used in threat models.
 */
public interface VerdictField {
	/**
	 * @return the name of this field
	 */
	public String getName();

	/**
	 * @return the type of this field
	 */
	public VerdictType getType();

	/**
	 * Performs shallow equality test on the fields of a and b, i.e.
	 * compares the names of each of their fields (in order).
	 *
	 * Comparing just names, not values, allows us to have cycles in the
	 * type graph (otherwise, performing deep equality would cause an
	 * infinite loop)
	 *
	 * @param a
	 * @param b
	 * @return true iff the fields of a and b are shallowly equal
	 */
	public static boolean equalFields(List<VerdictField> a, List<VerdictField> b) {
		if (a.size() != b.size()) {
			return false;
		}

		for (int i = 0; i < a.size(); i++) {
			if (!a.get(i).getName().equals(b.get(i).getName())) {
				return false;
			}
		}

		return true;
	}
}
