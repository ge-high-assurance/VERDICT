package com.ge.research.osate.verdict.dsl.type;

import java.util.Optional;

/**
 * An introduced variable, used in threat models.
 */
public interface VerdictVariable {
	/**
	 * @return the identifier of this variable
	 */
	public String getId();

	/**
	 * Types may be empty if the user is still editing the introduction
	 *
	 * @return the type of this variable if present, otherwise empty
	 */
	public Optional<VerdictType> getType();
}
