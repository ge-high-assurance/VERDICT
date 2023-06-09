package com.ge.research.osate.verdict.dsl.linking;

import java.util.List;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.nodemodel.INode;
import org.osate.xtext.aadl2.properties.linking.PropertiesLinkingService;

/**
 * Note: This is not currently being used at all because it causes Osate to crash. It is disabled in
 * the ui project's plugin.xml.
 */
public class VerdictLinkingService extends PropertiesLinkingService {
    // TODO potentially need to do things here?
    // See
    // https://github.com/loonwerks/formal-methods-workbench/blob/64f12ae81d37a34353e9b1f59759d16b9839c3a6/tools/agree/com.rockwellcollins.atc.agree/src/com/rockwellcollins/atc/agree/linking/AgreeLinkingService.java

    @Override
    public List<EObject> getLinkedObjects(EObject context, EReference reference, INode node) {
        return super.getLinkedObjects(context, reference, node);
    }
}
