package com.ge.research.osate.verdict.dsl.ui.annex;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.INode;
import org.osate.annexsupport.AnnexLinkingService;

import com.ge.research.osate.verdict.dsl.linking.VerdictLinkingService;
import com.ge.research.osate.verdict.dsl.linking.VerdictQualifiedNameProvider;
import com.ge.research.osate.verdict.dsl.ui.VerdictUiModule;
import com.ge.research.osate.verdict.dsl.ui.internal.VerdictActivator;
import com.google.inject.Injector;

/**
 * Linking. Osate extension point.
 *
 * Note: This extension is currently disabled in plugin.xml.
 * There is a strange bug causing a stack overflow and this isn't really necessary.
 */
public class VerdictAnnexLinkingService implements AnnexLinkingService {
	final private Injector injector = VerdictActivator.getInstance().getInjector(VerdictUiModule.INJECTOR_NAME);

	private VerdictLinkingService linkingService;
	private IQualifiedNameProvider nameProvider;

	protected VerdictLinkingService getLinkingService() {
		if (linkingService == null) {
			linkingService = injector.getInstance(VerdictLinkingService.class);
		}
		return linkingService;
	}

	protected IQualifiedNameProvider getNameProvider() {
		if (nameProvider == null) {
			nameProvider = injector.getInstance(VerdictQualifiedNameProvider.class);
		}
		return nameProvider;
	}

	@Override
	public List<EObject> resolveAnnexReference(String annexName, EObject context, EReference reference, INode node) {
		return getLinkingService().getLinkedObjects(context, reference, node);
	}

	@Override
	public QualifiedName getFullyQualifiedName(final EObject obj) {
		return getNameProvider().getFullyQualifiedName(obj);
	}
}
