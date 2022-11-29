package com.ge.research.osate.verdict.dsl.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.osate.aadl2.Aadl2Factory;
import org.osate.aadl2.AadlBoolean;
import org.osate.aadl2.AadlInteger;
import org.osate.aadl2.AadlReal;
import org.osate.aadl2.AadlString;
import org.osate.aadl2.EnumerationType;
import org.osate.aadl2.ListType;
import org.osate.aadl2.NumberValue;
import org.osate.aadl2.PropertyType;
import org.osate.aadl2.RecordType;

/**
 * A type that is loaded from the type of a property, wrapping an existing PropertyType. Used in
 * threat models.
 */
public class AadlTypeWrapper implements VerdictType {
    private String fullName, shortName;
    private List<VerdictField> fields;
    private Function<String, Boolean> isValue;
    private boolean isList;
    private List<String> valueSuggestions;

    private PropertyType type;

    /**
     * Used for storing range information from number types.
     *
     * @param <T>
     */
    public static class Range<T> {
        public final T min;
        public final T max;

        public Range(T min, T max) {
            this.min = min;
            this.max = max;
        }
    }

    /**
     * @param type
     * @return the range from type
     */
    public static Optional<Range<Long>> getRange(AadlInteger type) {
        if (type.getRange().getLowerBound() instanceof NumberValue
                && type.getRange().getUpperBound() instanceof NumberValue) {

            long min = (long) ((NumberValue) type.getRange().getLowerBound()).getScaledValue();
            long max = (long) ((NumberValue) type.getRange().getUpperBound()).getScaledValue();
            return Optional.of(new Range<Long>(min, max));
        } else {
            return Optional.empty();
        }
    }

    /**
     * @param type
     * @return the range from type
     */
    public static Optional<Range<Double>> getRange(AadlReal type) {
        if (type.getRange().getLowerBound() instanceof NumberValue
                && type.getRange().getUpperBound() instanceof NumberValue) {

            double min = ((NumberValue) type.getRange().getLowerBound()).getScaledValue();
            double max = ((NumberValue) type.getRange().getUpperBound()).getScaledValue();
            return Optional.of(new Range<Double>(min, max));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Converts a possibly-exception-raising function call into one that returns an option that is
     * non-present upon an exception.
     *
     * <p>This isn't super-useful for what we're doing here, but it is cool.
     *
     * @param <I> input type
     * @param <O> output type
     * @param function the function to invoke
     * @param input the input to the function
     * @return the output of the function, or empty if the function threw an exception
     */
    private <I, O> Optional<O> succeeds(Function<I, O> function, I input) {
        try {
            return Optional.of(function.apply(input));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public AadlTypeWrapper(String name, PropertyType type) {
        String metaTypeInfo = type.eClass().getName().toLowerCase();

        // Display valid range for numeric values
        if (type instanceof AadlInteger) {
            Optional<Range<Long>> range = getRange((AadlInteger) type);
            if (range.isPresent()) {
                // Modify name to include type information
                metaTypeInfo += " " + range.get().min + " .. " + range.get().max;
            }
        } else if (type instanceof AadlReal) {
            Optional<Range<Double>> range = getRange((AadlReal) type);
            if (range.isPresent()) {
                // Modify name to include type information
                metaTypeInfo += " " + range.get().min + " .. " + range.get().max;
            }
        }

        fullName = name + " (" + metaTypeInfo + ")"; // e.g. "insideTrustedBoundary (aadlboolean)"
        shortName = fullName;
        fields = Collections.emptyList();
        isValue = value -> false;
        isList = false;
        valueSuggestions = Collections.emptyList();

        if (type instanceof EnumerationType) {
            // Valid value iff value is in the list of enumeration literals
            isValue =
                    value ->
                            value != null
                                    && ((EnumerationType) type)
                                            .getOwnedLiterals().stream()
                                                    .anyMatch(
                                                            literal ->
                                                                    value.equals(
                                                                            literal.getName()));
            // Suggest all enumeration literals
            valueSuggestions =
                    ((EnumerationType) type)
                            .getOwnedLiterals().stream()
                                    .map(enumVal -> enumVal.getName())
                                    .collect(Collectors.toList());

        } else if (type instanceof AadlBoolean) {
            // Like enumeration, but exactly two values: true and false
            isValue = value -> "true".equals(value) || "false".equals(value);
            valueSuggestions = Arrays.asList(new String[] {"true", "false"});

        } else if (type instanceof AadlString) {
            // Valid value iff quoted
            isValue =
                    value ->
                            value != null
                                    && ((value.startsWith("\"") && value.endsWith("\""))
                                            || (value.startsWith("'") && value.endsWith("'")));

        } else if (type instanceof AadlInteger) {
            final Optional<Range<Long>> range = getRange((AadlInteger) type);
            isValue =
                    value -> {
                        // Value must parse as a long
                        Optional<Long> result = succeeds(Long::parseLong, value);

                        // Value must be in range
                        if (!result.isPresent()
                                || (range.isPresent()
                                        && (range.get().min > result.get()
                                                || range.get().max < result.get()))) {
                            return false;
                        }
                        return true;
                    };

        } else if (type instanceof AadlReal) {
            final Optional<Range<Double>> range = getRange((AadlReal) type);
            isValue =
                    value -> {
                        // Value must parse as a double
                        Optional<Double> result = succeeds(Double::parseDouble, value);

                        // Value must be in range
                        if (!result.isPresent()
                                || (range.isPresent()
                                        && (range.get().min > result.get()
                                                || range.get().max < result.get()))) {
                            return false;
                        }
                        return true;
                    };

        } else if (type instanceof RecordType) {
            // Records contain other fields, each of which has its own type
            fields =
                    ((RecordType) type)
                            .getOwnedFields().stream()
                                    .map(
                                            field ->
                                                    new VerdictFieldImpl(
                                                            field.getName(),
                                                            new AadlTypeWrapper(
                                                                    field.getName(),
                                                                    field.getPropertyType())))
                                    .collect(Collectors.toList());

        } else if (type instanceof ListType) {
            // Pass

        } else {
            System.err.println("unknown type: " + type.getClass().getName());
        }

        // Prevent concurrency issues
        fields = Collections.unmodifiableList(fields);

        this.type = type;
    }

    public PropertyType getWrappedType() {
        return type;
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
        return isValue.apply(value);
    }

    @Override
    public boolean isList() {
        return isList;
    }

    /** Creates a new instance of every invocation. */
    @Override
    public VerdictType getListType() {
        ListType listType = Aadl2Factory.eINSTANCE.createListType();
        listType.setOwnedElementType(type);
        return new AadlTypeWrapper("list of " + fullName, listType);
    }

    @Override
    public boolean isListOf(VerdictType type) {
        return this.equals(type.getListType());
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof VerdictType) {
            VerdictType otherType = (VerdictType) other;
            // Shallow field comparison to prevent infinite loops
            return isList() == otherType.isList()
                    && getFullName().equals(otherType.getFullName())
                    && VerdictField.equalFields(fields, otherType.getFields());
        }

        return false;
    }

    @Override
    public List<String> getValueSuggestions() {
        return valueSuggestions;
    }
}
