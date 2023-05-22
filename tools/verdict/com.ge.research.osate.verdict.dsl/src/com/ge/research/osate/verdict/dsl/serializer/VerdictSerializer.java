package com.ge.research.osate.verdict.dsl.serializer;

import com.ge.research.osate.verdict.dsl.services.VerdictGrammarAccess;
import com.google.inject.Inject;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.serializer.ISerializationContext;
import org.eclipse.xtext.serializer.impl.Serializer;
import org.osate.aadl2.AnnexLibrary;
import org.osate.aadl2.AnnexSubclause;

/** Serialization/pretty-printing of ASTs back into text. Uses the formatter. */
@SuppressWarnings("restriction")
public class VerdictSerializer extends Serializer {
    @Inject private VerdictGrammarAccess grammarAccess;

    @Override
    protected ISerializationContext getIContext(EObject semanticObject) {
        if (semanticObject instanceof AnnexLibrary) {
            return null;
        } else if (semanticObject instanceof AnnexSubclause) {
            for (final ISerializationContext o :
                    contextFinder.findByContents(semanticObject, null)) {
                if (o.getParserRule() == grammarAccess.getVerdictSubclauseRule()) {
                    return o;
                }
            }
            return null;
        } else if (semanticObject instanceof AnnexLibrary) {
            for (final ISerializationContext o :
                    contextFinder.findByContents(semanticObject, null)) {
                if (o.getParserRule() == grammarAccess.getVerdictLibraryRule()) {
                    return o;
                }
            }
            return null;
        } else {
            return super.getIContext(semanticObject);
        }
    }
}
