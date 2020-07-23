/* See LICENSE in project directory */
package com.ge.verdict.lustre;

import com.ge.verdict.vdm.VdmTranslator;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.htmlparser.jericho.Source;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import verdict.vdm.vdm_model.Model;
import verdict.vdm.vdm_model.ObjectFactory;

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
                // Set up model as source to read from
                JAXBContext context = JAXBContext.newInstance(model.getClass());
                ObjectFactory factory = new ObjectFactory();
                JAXBElement<Model> element = factory.createModel(model);
                JAXBSource source = new JAXBSource(context, element);

                // Set up string in memory as result to write to
                StringWriter html = new StringWriter();
                Result result = new StreamResult(html);

                // First, transform model to HTML using XSLT stylesheet and Saxon transformer
                StreamSource stylesheet =
                        new StreamSource(model.getClass().getResourceAsStream("/VdmToLustre.xslt"));
                Transformer transformer =
                        new net.sf.saxon.TransformerFactoryImpl().newTransformer(stylesheet);
                transformer.transform(source, result);

                // Second, convert HTML to text using Jericho HTML parser/renderer
                Source jericho = new Source(html.getBuffer());
                String text =
                        jericho.getRenderer()
                                .setMaxLineLength(0)
                                .setTableCellSeparator("")
                                .toString();

                // Last, write text to output stream
                output.write(text.getBytes(StandardCharsets.UTF_8));
            } catch (IOException | JAXBException | TransformerException e) {
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
