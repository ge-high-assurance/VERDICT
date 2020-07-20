package com.ge.verdict.attackdefensecollector;

/**
 * Wrapper around StringBuilder to support indentation of output.
 *
 * <p>To extract the string at the end, call toString().
 *
 * <p>Will throw exceptions if indentation levels are not matched correctly.
 *
 * <p>This class does not extend StringBuilder because StringBuilder is declared final. As such not
 * all of StringBuilder's methods are implemented.
 */
public class IndentedStringBuilder {
    private StringBuilder builder;
    private int indent;
    private int spacesPerIndent;

    public IndentedStringBuilder(int spacesPerIndent) {
        if (spacesPerIndent < 0) {
            throw new RuntimeException(
                    "Cannot have negative spaces per indent: " + spacesPerIndent);
        }

        builder = new StringBuilder();
        this.spacesPerIndent = spacesPerIndent;
    }

    public IndentedStringBuilder() {
        this(2);
    }

    public int getSpacesPerIndent() {
        return spacesPerIndent;
    }

    public void append(String str) {
        builder.append(str);
    }

    public void append(char chr) {
        builder.append(chr);
    }

    public void append(int i) {
        builder.append(i);
    }

    public void append(float f) {
        builder.append(f);
    }

    public void append(boolean bool) {
        builder.append(bool);
    }

    /**
     * Insert the newline character followed by a number of spaces corresponding to the current
     * indentation level.
     */
    public void newLine() {
        builder.append("\n");
        for (int i = 0; i < indent; i++) {
            builder.append(' ');
        }
    }

    /** Increase the indentation level. Must be followed with a corresponding unindent(). */
    public void indent() {
        indent += spacesPerIndent;
    }

    /** Decrease the indentation level. Must be preceded with a corresponding indent(). */
    public void unindent() {
        indent -= spacesPerIndent;

        if (indent < 0) {
            throw new RuntimeException("Unindented past zero");
        }
    }

    @Override
    public String toString() {
        if (indent != 0) {
            throw new RuntimeException(
                    "IndentedStringBuilder: Leftover indentation in toString: " + indent);
        }

        return builder.toString();
    }
}
