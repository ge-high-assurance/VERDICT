package com.ge.research.osate.verdict.dsl.ui.annex;

import com.ge.research.osate.verdict.dsl.parser.antlr.VerdictParser;
import com.ge.research.osate.verdict.dsl.services.VerdictGrammarAccess;
import com.ge.research.osate.verdict.dsl.ui.VerdictUiModule;
import com.ge.research.osate.verdict.dsl.ui.internal.VerdictActivator;
import com.ge.research.osate.verdict.dsl.verdict.VerdictContractLibrary;
import com.ge.research.osate.verdict.dsl.verdict.VerdictContractSubclause;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.osate.aadl2.AnnexLibrary;
import org.osate.aadl2.AnnexSubclause;
import org.osate.aadl2.modelsupport.errorreporting.ParseErrorReporter;
import org.osate.annexsupport.AnnexParseUtil;
import org.osate.annexsupport.AnnexParser;

/**
 * Parse the verdict annex; Osate extension point.
 *
 * <p>An annex library is outside of a system and contains threat models.
 *
 * <p>An annex subclause is inside of a system and contains cyber properties.
 */
public class VerdictAnnexParser implements AnnexParser {

    @Inject private VerdictParser parser;

    public VerdictAnnexParser() {
        Injector injector =
                VerdictActivator.getInstance().getInjector(VerdictUiModule.INJECTOR_NAME);
        injector.injectMembers(this);
    }

    protected VerdictGrammarAccess getGrammarAccess() {
        return parser.getGrammarAccess();
    }

    @Override
    public AnnexLibrary parseAnnexLibrary(
            String annexName,
            String source,
            String filename,
            int line,
            int column,
            ParseErrorReporter errReporter) {
        return (VerdictContractLibrary)
                AnnexParseUtil.parse(
                        parser,
                        source,
                        getGrammarAccess().getAnnexLibraryRule(),
                        filename,
                        line,
                        column,
                        errReporter);
    }

    @Override
    public AnnexSubclause parseAnnexSubclause(
            String annexName,
            String source,
            String filename,
            int line,
            int column,
            ParseErrorReporter errReporter) {
        return (VerdictContractSubclause)
                AnnexParseUtil.parse(
                        parser,
                        source,
                        getGrammarAccess().getAnnexSubclauseRule(),
                        filename,
                        line,
                        column,
                        errReporter);
    }
}
