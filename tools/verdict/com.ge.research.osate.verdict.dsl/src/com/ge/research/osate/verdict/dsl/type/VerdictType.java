package com.ge.research.osate.verdict.dsl.type;

import java.util.List;

/**
 * The type of a variable or field in threat models.
 */
public interface VerdictType {
	/**
	 * @return the name of the class, e.g. AadlInteger
	 */
	public String getFullName();

	/**
	 * @return the name that the user may type, e.g. aadlinteger
	 */
	public String getShortName();

	/**
	 * @param fieldName
	 * @return true iff this type has a field with name fieldName
	 */
	public boolean hasField(String fieldName);

	/**
	 * @return all fields present for this type
	 */
	public List<VerdictField> getFields();

	/**
	 * @param value
	 * @return true iff value is a valid value for this type
	 */
	public boolean isValue(String value);

	/**
	 * @return true iff this type is a list type
	 */
	public boolean isList();

	/**
	 * Note that this may be used even on lists.
	 *
	 * E.g. getListType() on "list of aadlstring" yields
	 * "list of list of aadlstring".
	 *
	 * @return the type corresponding to a list of this type
	 */
	public VerdictType getListType();

	/**
	 * @param type
	 * @return true iff this type is the list type corresponding to type
	 */
	public boolean isListOf(VerdictType type);

	/**
	 * Some types can have infinitely many possible values, so it
	 * doesn't make sense to list all of them. This method is used
	 * by content assist to provide suggestions. In particular,
	 * the values of enumeration types are provided here.
	 *
	 * @return a list of possible values for this type
	 */
	public List<String> getValueSuggestions();
}
