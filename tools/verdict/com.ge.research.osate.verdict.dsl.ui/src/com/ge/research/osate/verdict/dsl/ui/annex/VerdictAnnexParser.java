package com.ge.research.osate.verdict.dsl.ui.annex;

import org.osate.aadl2.AnnexLibrary;
import org.osate.aadl2.AnnexSubclause;
import org.osate.aadl2.modelsupport.errorreporting.ParseErrorReporter;
import org.osate.annexsupport.AnnexParseUtil;
import org.osate.annexsupport.AnnexParser;

import com.ge.research.osate.verdict.dsl.parser.antlr.VerdictParser;
import com.ge.research.osate.verdict.dsl.services.VerdictGrammarAccess;
import com.ge.research.osate.verdict.dsl.ui.VerdictUiModule;
import com.ge.research.osate.verdict.dsl.ui.internal.VerdictActivator;
import com.ge.research.osate.verdict.dsl.verdict.VerdictContractLibrary;
import com.ge.research.osate.verdict.dsl.verdict.VerdictContractSubclause;
import com.google.inject.Injector;

/**
 * Parse the verdict annex; Osate extension point.
 *
 * An annex library is outside of a system and contains threat models.
 *
 * An annex subclause is inside of a system and contains cyber properties.
 */
public class VerdictAnnexParser implements AnnexParser {
	final private Injector injector = VerdictActivator.getInstance()
			.getInjector(VerdictUiModule.INJECTOR_NAME);

	private VerdictParser parser;

	protected VerdictParser getParser() {
		if (parser == null) {
			parser = injector.getInstance(VerdictParser.class);
		}
		return parser;
	}

	protected VerdictGrammarAccess getGrammarAccess() {
		return getParser().getGrammarAccess();
	}

	@Override
	public AnnexLibrary parseAnnexLibrary(String name, String source, String filename, int line, int column,
			ParseErrorReporter errReporter) {
		return (VerdictContractLibrary) AnnexParseUtil.parse(getParser(), source,
				getGrammarAccess().getAnnexLibraryRule(), filename, line, column, errReporter);
	}

	@Override
	public AnnexSubclause parseAnnexSubclause(String name, String source, String filename, int line, int column,
			ParseErrorReporter errReporter) {
		return (VerdictContractSubclause) AnnexParseUtil.parse(getParser(), source,
				getGrammarAccess().getAnnexSubclauseRule(), filename, line, column, errReporter);
	}
}
