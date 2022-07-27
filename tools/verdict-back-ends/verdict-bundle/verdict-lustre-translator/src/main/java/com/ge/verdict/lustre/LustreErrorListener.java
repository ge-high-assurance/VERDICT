/* See LICENSE in project directory */
package com.ge.verdict.lustre;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/** Include the source filename and source line in parser error messages. */
public class LustreErrorListener extends BaseErrorListener {
    /** Provides a default instance of {@link LustreErrorListener}. */
    public static final LustreErrorListener INSTANCE = new LustreErrorListener();

    @Override
    public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String msg,
            RecognitionException e) {
        String source = recognizer.getInputStream().getSourceName();
        StringBuilder sb = new StringBuilder();

        addMessage(sb, line, charPositionInLine, msg, source);
        addSourceLine(sb, source, line);
        addPointer(sb, charPositionInLine);
        System.err.println(sb.toString());
    }

    private void addMessage(StringBuilder sb, int line, int charPositionInLine, String msg, String source) {
        sb.append(source)
                .append(":")
                .append(line)
                .append(":")
                .append(charPositionInLine)
                .append(" ")
                .append(msg)
                .append("\n");
    }

    private void addSourceLine(StringBuilder sb, String source, int errorLineNumber) {
        String currentLine = "";
        try (Scanner fileScanner = new Scanner(Paths.get(source))) {
            int currentLineNumber = 1;
            while (fileScanner.hasNextLine()) {
                currentLine = fileScanner.nextLine();
                if (currentLineNumber == errorLineNumber) {
                    sb.append(currentLine);
                    break;
                }
                currentLineNumber++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            sb.append("\n");
        }
    }

    private void addPointer(StringBuilder sb, int charPositionInLine) {
        for (int i = 0; i < charPositionInLine; i++) {
            sb.append(" ");
        }
        sb.append("^");
    }
}
