package com.ge.research.osate.verdict.dsl.ui.contentassist;

import com.google.inject.Inject;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ui.editor.templates.ContextTypeIdHelper;
import org.eclipse.xtext.ui.editor.templates.DefaultTemplateProposalProvider;
import org.eclipse.xtext.ui.editor.templates.XtextTemplateProposal;

/**
 * Shim for supporting templates. Osate does not support annex
 * templates, so we have to do this ourselves.
 */
@SuppressWarnings("deprecation")
public class VerdictTemplateProposalProvider extends DefaultTemplateProposalProvider {
    private static final String CONTEXT_ID_CYBER_REL = "com.ge.research.osate.verdict.dsl.Verdict.CyberRel";
    private static final String CONTEXT_ID_CYBER_REQ = "com.ge.research.osate.verdict.dsl.Verdict.CyberReq";
    private static final String CONTEXT_ID_MISSION = "com.ge.research.osate.verdict.dsl.Verdict.CyberMission";
    private static final String CONTEXT_ID_MODEL = "com.ge.research.osate.verdict.dsl.Verdict.Model";

    @Inject
    public VerdictTemplateProposalProvider(
            TemplateStore templateStore, ContextTypeRegistry registry, ContextTypeIdHelper helper) {
        super(templateStore, registry, helper);

        // Allow Verdict to provide variable resolution

        VerdictTemplateVariableResolver resolver = new VerdictTemplateVariableResolver();
        registry.getContextType(CONTEXT_ID_CYBER_REL).addResolver(resolver);
        registry.getContextType(CONTEXT_ID_CYBER_REQ).addResolver(resolver);
        registry.getContextType(CONTEXT_ID_MISSION).addResolver(resolver);
        // We only need this one because the grammar is kind of hacky
        registry.getContextType(CONTEXT_ID_MODEL).addResolver(resolver);
    }

    @Override
    protected TemplateProposal doCreateProposal(
            Template template,
            TemplateContext templateContext,
            ContentAssistContext context,
            Image image,
            int relevance) {

        // Replace the context with our own context
        return new XtextTemplateProposal(
                template, new VerdictTemplateContext(templateContext), context.getReplaceRegion(), image, relevance);
    }
}
