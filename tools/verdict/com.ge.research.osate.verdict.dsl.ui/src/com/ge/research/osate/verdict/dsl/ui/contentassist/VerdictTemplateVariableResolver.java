package com.ge.research.osate.verdict.dsl.ui.contentassist;

import com.ge.research.osate.verdict.dsl.VerdictUtil;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.xtext.ui.editor.templates.AbstractTemplateVariableResolver;
import org.eclipse.xtext.ui.editor.templates.XtextTemplateContext;
import org.osate.aadl2.DirectionType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom variable resolution for templates. Required because Osate does not support annex
 * templates.
 *
 * <p>Resolves template variables named 'VerdictTemplateVar' and uses the first parameter to
 * determine direction.
 *
 * <p>See also VerdictTemplateProposalProvider and VerdictTemplateContext.
 */
public class VerdictTemplateVariableResolver extends AbstractTemplateVariableResolver {
    private static final String TYPE = "VerdictTemplateVar";

    public VerdictTemplateVariableResolver() {
        super(TYPE, "Verdict");
    }

    @Override
    public List<String> resolveValues(
            TemplateVariable variable, XtextTemplateContext xtextTemplateContext) {
        // Only resolve our own variables
        if (variable.getType().equals(TYPE) && !variable.getVariableType().getParams().isEmpty()) {
            EObject model = xtextTemplateContext.getContentAssistContext().getCurrentModel();

            switch (variable.getVariableType().getParams().get(0)) {
                case "CyberRel.in":
                    return resolvePorts(model, DirectionType.IN);
                case "CyberRel.out":
                    return resolvePorts(model, DirectionType.OUT);
                case "CyberReq.cond":
                    return resolvePorts(model, DirectionType.OUT);
                case "Mission.req":
                    return resolveReqs(model);
            }
        }

        return new ArrayList<>();
    }

    private List<String> resolvePorts(EObject model, DirectionType dir) {
        VerdictUtil.AvailablePortsInfo info = VerdictUtil.getAvailablePorts(model, true, dir);
        return info.availablePorts;
    }

    private List<String> resolveReqs(EObject model) {
        return VerdictUtil.getAllReqs(model).stream()
                .map(req -> "\"" + req + "\"")
                .collect(Collectors.toList());
    }
}
