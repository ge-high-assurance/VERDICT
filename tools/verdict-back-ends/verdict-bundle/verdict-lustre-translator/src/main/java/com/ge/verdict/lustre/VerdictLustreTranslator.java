/* See LICENSE in project directory */
package com.ge.verdict.lustre;

import com.ge.verdict.vdm.VdmTranslator;

import edu.uiowa.clc.verdict.lustre.VDMLustre2Kind2;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import verdict.vdm.vdm_model.Model;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Translate a Verdict data model to or from a Lustre file. */
public class VerdictLustreTranslator extends VdmTranslator {

    /**
     * Marshal a Verdict data model to a Lustre file.
     *
     * @param model Verdict data model to marshal
     * @param outputFile Lustre file to write to
     */
    public static void marshalToLustre(Model model, File outputFile) {
        // Skip and warn if output file can't be created
        if (canWrite(outputFile)) {
            // Open output stream to be written to
            try (OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                String text = VDMLustre2Kind2.translate(model).toString();
                // Last, write text to output stream
                output.write(text.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Unmarshal a Verdict data model from a Lustre file.
     *
     * @param inputFile Lustre file to unmarshal from
     * @return Verdict data model from Lustre file
     */
    public static Model unmarshalFromLustre(File inputFile) {
        // Parse the Lustre file into a parse tree
        LustreParser.ProgramContext programContext =
                VerdictLustreTranslator.parseFromLustre(inputFile);

        // Extract any data from the parse tree with our listener
        VerdictLustreListener extractor = new VerdictLustreListener(inputFile);
        ParseTreeWalker.DEFAULT.walk(extractor, programContext);

        // Return the model extracted from the parse tree
        Model model = extractor.getModel();
        return model;
    }

    /**
     * Parse a Lustre file into a parse tree. Not for public use; intended only for within-package
     * use by VerdictLustreTranslator and VerdictLustreListener.
     *
     * @param inputFile Lustre file to parse
     * @return Parse tree from Lustre file
     */
    static LustreParser.ProgramContext parseFromLustre(File inputFile) {
        try {
            CharStream input = CharStreams.fromFileName(inputFile.getPath());
            LustreLexer lexer = new LustreLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            LustreParser parser = new LustreParser(tokens);
            lexer.removeErrorListeners();
            lexer.addErrorListener(LustreErrorListener.INSTANCE);
            parser.removeErrorListeners();
            parser.addErrorListener(LustreErrorListener.INSTANCE);
            return parser.program();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
