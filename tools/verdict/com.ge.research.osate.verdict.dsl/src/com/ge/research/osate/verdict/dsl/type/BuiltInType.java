package com.ge.research.osate.verdict.dsl.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A type that is provided by default. Used in threat models.
 * Currently system, connection, port, and portDirection.
 */
public class BuiltInType implements VerdictType {
	private String fullName, shortName;
	private List<VerdictField> fields;

	/**
	 * If present, then this type is a list containing the type
	 * listChild.
	 */
	private Optional<BuiltInType> listChild;

	/**
	 * The list of value suggestions. Currently used only for
	 * enumeration types.
	 */
	private List<String> values;

	public BuiltInType(String fullName) {
		this.fullName = fullName;
		this.shortName = this.fullName;
		fields = new ArrayList<>();
		values = new ArrayList<>();
		listChild = Optional.empty();
	}

	public BuiltInType addField(String name, VerdictType type) {
		fields.add(new VerdictFieldImpl(name, type));
		return this;
	}

	public BuiltInType addValue(String name) {
		values.add(name);
		return this;
	}

	/**
	 * Prevent concurrency issues by making the internal
	 * lists unmodifiable.
	 *
	 * It is unclear why these issues happened in the first place,
	 * but this definitely makes the issue appear where it should
	 * (where lists are being modified, not where they are being
	 * used).
	 */
	public void lock() {
		values = Collections.unmodifiableList(values);
		fields = Collections.unmodifiableList(fields);
	}

	/**
	 * @return a new instance corresponding to the list type of
	 * this type
	 */
	private BuiltInType createListType() {
		BuiltInType base = new BuiltInType(fullName);
		base.listChild = Optional.of(this);
		base.fullName = "list of " + fullName;
		base.shortName = "list of " + shortName;
		return base;
	}

	@Override
	public String getFullName() {
		return fullName;
	}

	@Override
	public String getShortName() {
		return shortName;
	}

	@Override
	public boolean hasField(String fieldName) {
		return fields.stream().anyMatch(field -> field.getName().equals(fieldName));
	}

	@Override
	public List<VerdictField> getFields() {
		return fields;
	}

	@Override
	public boolean isValue(String value) {
		return values.contains(value);
	}

	@Override
	public boolean isList() {
		return listChild.isPresent();
	}

	/**
	 * This creates a new instance on every invocation.
	 */
	@Override
	public VerdictType getListType() {
		return createListType();
	}

	@Override
	public boolean isListOf(VerdictType type) {
		return this.equals(type.getListType());
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof BuiltInType) {
			BuiltInType otherBuiltIn = (BuiltInType) other;
			// Shallow field comparison to prevent infinite loops
			return otherBuiltIn.getFullName().equals(fullName)
					&& VerdictField.equalFields(fields, otherBuiltIn.getFields())
					&& otherBuiltIn.listChild.equals(listChild);
		}

		return false;
	}

	@Override
	public List<String> getValueSuggestions() {
		return values;
	}
}
