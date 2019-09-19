package com.ge.research.osate.verdict.dsl.ui.annex;

import org.eclipse.xtext.serializer.ISerializer;
import org.osate.aadl2.AnnexLibrary;
import org.osate.aadl2.AnnexSubclause;
import org.osate.annexsupport.AnnexUnparser;

import com.ge.research.osate.verdict.dsl.serializer.VerdictSerializer;
import com.ge.research.osate.verdict.dsl.ui.VerdictUiModule;
import com.ge.research.osate.verdict.dsl.ui.internal.VerdictActivator;
import com.google.inject.Injector;

/**
 * Serialize/pretty-print the verdict annex; Osate extension point.
 */
public class VerdictAnnexUnparser implements AnnexUnparser {
	final private Injector injector = VerdictActivator.getInstance().getInjector(VerdictUiModule.INJECTOR_NAME);

	private ISerializer serializer;

	protected ISerializer getSerializer() {
		if (serializer == null) {
			serializer = injector.getInstance(VerdictSerializer.class);
		}
		return serializer;
	}

	@Override
	public String unparseAnnexLibrary(AnnexLibrary library, String indent) {
		library.setName("null");
		return indent + getSerializer().serialize(library);
	}

	@Override
	public String unparseAnnexSubclause(AnnexSubclause subclause, String indent) {
		subclause.setName("null");
		return indent + getSerializer().serialize(subclause);
	}
}
