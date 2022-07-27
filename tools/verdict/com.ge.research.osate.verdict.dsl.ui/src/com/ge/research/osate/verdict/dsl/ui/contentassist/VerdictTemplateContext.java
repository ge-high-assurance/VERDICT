package com.ge.research.osate.verdict.dsl.ui.contentassist;

import java.lang.reflect.Field;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateTranslator;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ui.editor.templates.XtextTemplateContext;

/**
 * Custom context for templates. Required because Osate does not
 * support annex templates.
 *
 * See also VerdictTemplateProposalProvider and
 * VerdictTemplateVariableResolver.
 */
public class VerdictTemplateContext extends XtextTemplateContext {
    public VerdictTemplateContext(
            TemplateContextType type,
            IDocument document,
            Position position,
            ContentAssistContext contentAssistContext,
            IScopeProvider scopeProvider) {
        super(type, document, position, contentAssistContext, scopeProvider);
    }

    /**
     * @param context
     * @return the position extracted from the context
     */
    private static Position getContextPosition(DocumentTemplateContext context) {
        try {
            // It's private, so perform a little bit of coercion
            Field field = DocumentTemplateContext.class.getDeclaredField("fPosition");
            field.setAccessible(true);
            return (Position) field.get(context);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create a new verdict context from the provided context.
     *
     * @param context
     */
    public VerdictTemplateContext(TemplateContext context) {
        super(
                context.getContextType(),
                ((DocumentTemplateContext) context).getDocument(),
                getContextPosition((DocumentTemplateContext) context),
                ((XtextTemplateContext) context).getContentAssistContext(),
                ((XtextTemplateContext) context).getScopeProvider());
    }

    @Override
    protected TemplateTranslator createTemplateTranslator() {
        /*
         * This is the crucial method that needs to be overridden.
         *
         * The problem is that Xtext's implementation of this method
         * relies on the document metadata to determine indentation.
         *
         * AADL annexes get provided with a dummy document that does
         * not have this metadata, so the result is a crash.
         *
         * Therefore we avoid using the document metadata and
         * determine the indentation manually.
         */

        try {
            // Determine correct indentation

            int i = getStart();
            char c;
            StringBuilder indent = new StringBuilder();

            // Count whitespace characters at the beginning of the
            // current line
            while (true) {
                i--;
                if (i < 0) {
                    // Beginning of file
                    break;
                }
                c = getDocument().getChar(i);
                if (c == '\n' || c == '\r') {
                    // Reached beginning of line
                    break;
                }
                if (Character.isWhitespace(c)) {
                    // More indentation
                    indent.append(c);
                } else {
                    // We want to count only the whitespace
                    indent.setLength(0);
                }
            }

            return new VerdictIndentationAwareTemplateTranslator(
                    indent.reverse().toString(), null);
        } catch (BadLocationException e) {
            return new VerdictIndentationAwareTemplateTranslator("", null);
        }
    }

    // Copied from Xtext. They changed the constructor in 2.17 and we want to support
    // both 2.16 and 2.17, so here we go.
    public static class VerdictIndentationAwareTemplateTranslator extends TemplateTranslator {
        private final String indentation;
        private final String lineDelimiter;

        public VerdictIndentationAwareTemplateTranslator(String indentation, String lineDelimiter) {
            this.indentation = indentation;
            this.lineDelimiter = lineDelimiter == null ? System.lineSeparator() : lineDelimiter;
        }

        @Override
        public TemplateBuffer translate(Template template) throws TemplateException {
            return translate(template.getPattern());
        }

        @Override
        public TemplateBuffer translate(String string) throws TemplateException {
            return super.translate(string.replaceAll("(\r\n?)|(\n)", lineDelimiter + indentation));
        }
    }
}
