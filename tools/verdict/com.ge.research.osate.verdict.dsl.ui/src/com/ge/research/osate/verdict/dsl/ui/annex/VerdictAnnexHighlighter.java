package com.ge.research.osate.verdict.dsl.ui.annex;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EnumLiteralDeclaration;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.INode;
import org.osate.aadl2.AnnexLibrary;
import org.osate.aadl2.AnnexSubclause;
import org.osate.annexsupport.AnnexHighlighter;
import org.osate.annexsupport.AnnexHighlighterPositionAcceptor;
import org.osate.annexsupport.AnnexParseUtil;
import org.osate.annexsupport.AnnexUtil;

/**
 * Perform syntax highlighting/coloring; Osate extension point.
 *
 * This file must be updated whenever the keywords change in the grammar.
 *
 * Osate does not expose the normal Xtext ways of doing this, so our
 * approach is a little bit hackier.
 *
 * We are able to highlight all lexed tokens in this manner.
 *
 * We cannot choose the colors; we are currently just using the default
 * set of colors.
 */
public class VerdictAnnexHighlighter implements AnnexHighlighter {
	// Styles maps
	private Map<String, String> stylesAll, stylesSubclause, stylesLibrary;

	// Styles
	public static final String STRING_ID = AnnexHighlighterPositionAcceptor.STRING_ID;
	public static final String KEYWORD_ID = AnnexHighlighterPositionAcceptor.KEYWORD_ID;
	public static final String COMMENT_ID = AnnexHighlighterPositionAcceptor.COMMENT_ID;

	public VerdictAnnexHighlighter() {
		/*
		 * We construct map keys as follows:
		 * - Keywords prefixed with "kw_"
		 * - Enums prefixed with "enum_"
		 * - Strings as "string"
		 * - Comments as "comment"
		 */

		stylesAll = new HashMap<>();
		stylesSubclause = new HashMap<>();
		stylesLibrary = new HashMap<>();

		stylesAll.put("string", STRING_ID);
		stylesAll.put("comment", COMMENT_ID);

		stylesSubclause.put("kw_MissionReq", KEYWORD_ID);
		stylesSubclause.put("kw_CyberRel", KEYWORD_ID);
		stylesSubclause.put("kw_CyberReq", KEYWORD_ID);

		stylesSubclause.put("kw_SafetyReq", KEYWORD_ID);
		stylesSubclause.put("kw_SafetyRel", KEYWORD_ID);
		stylesSubclause.put("kw_Event", KEYWORD_ID);
		stylesSubclause.put("kw_probability", KEYWORD_ID);
		stylesSubclause.put("kw_happens", KEYWORD_ID);
		stylesSubclause.put("kw_faultSrc", KEYWORD_ID);

		stylesSubclause.put("kw_id", KEYWORD_ID);
		stylesSubclause.put("kw_cia", KEYWORD_ID);
		stylesSubclause.put("kw_severity", KEYWORD_ID);
		stylesSubclause.put("kw_condition", KEYWORD_ID);
		stylesSubclause.put("kw_comment", KEYWORD_ID);
		stylesSubclause.put("kw_inputs", KEYWORD_ID);
		stylesSubclause.put("kw_output", KEYWORD_ID);
		stylesSubclause.put("kw_description", KEYWORD_ID);
		stylesSubclause.put("kw_targetLikelihood", KEYWORD_ID);
		stylesSubclause.put("kw_targetProbability", KEYWORD_ID);
		stylesSubclause.put("kw_justification", KEYWORD_ID);
		stylesSubclause.put("kw_assumption", KEYWORD_ID);
		stylesSubclause.put("kw_strategy", KEYWORD_ID);		
		stylesSubclause.put("kw_reqs", KEYWORD_ID);

		stylesSubclause.put("enum_None", KEYWORD_ID);
		stylesSubclause.put("enum_Minor", KEYWORD_ID);
		stylesSubclause.put("enum_Major", KEYWORD_ID);
		stylesSubclause.put("enum_Hazardous", KEYWORD_ID);
		stylesSubclause.put("enum_Catastrophic", KEYWORD_ID);

		stylesAll.put("enum_C", KEYWORD_ID);
		stylesAll.put("enum_I", KEYWORD_ID);
		stylesAll.put("enum_A", KEYWORD_ID);
		stylesAll.put("enum_Confidentiality", KEYWORD_ID);
		stylesAll.put("enum_Integrity", KEYWORD_ID);
		stylesAll.put("enum_Availability", KEYWORD_ID);

		stylesAll.put("kw_and", KEYWORD_ID);
		stylesAll.put("kw_&&", KEYWORD_ID);
		stylesAll.put("kw_/\\", KEYWORD_ID);
		stylesAll.put("kw_or", KEYWORD_ID);
		stylesAll.put("kw_||", KEYWORD_ID);
		stylesAll.put("kw_\\/", KEYWORD_ID);
		stylesAll.put("kw_not", KEYWORD_ID);
		stylesAll.put("kw_!", KEYWORD_ID);

		stylesLibrary.put("kw_ThreatEffect", KEYWORD_ID);
		stylesLibrary.put("kw_ThreatDefense", KEYWORD_ID);
		stylesLibrary.put("kw_ThreatDatabase", KEYWORD_ID);
		stylesLibrary.put("kw_contains", KEYWORD_ID);
		stylesLibrary.put("kw_forall", KEYWORD_ID);
		stylesLibrary.put("kw_exists", KEYWORD_ID);

		stylesLibrary.put("kw_true", KEYWORD_ID);
		stylesLibrary.put("kw_false", KEYWORD_ID);
		stylesLibrary.put("kw_in", KEYWORD_ID);
		stylesLibrary.put("kw_out", KEYWORD_ID);

		stylesLibrary.put("kw_system", KEYWORD_ID);
		stylesLibrary.put("kw_port", KEYWORD_ID);
		stylesLibrary.put("kw_connections", KEYWORD_ID);
		stylesLibrary.put("kw_subcomponents", KEYWORD_ID);

		stylesLibrary.put("kw_id", KEYWORD_ID);
		stylesLibrary.put("kw_entities", KEYWORD_ID);
		stylesLibrary.put("kw_description", KEYWORD_ID);
		stylesLibrary.put("kw_comment", KEYWORD_ID);
		stylesLibrary.put("kw_threats", KEYWORD_ID);
		stylesLibrary.put("kw_cia", KEYWORD_ID);
		stylesLibrary.put("kw_reference", KEYWORD_ID);
		stylesLibrary.put("kw_assumptions", KEYWORD_ID);
		stylesLibrary.put("kw_description", KEYWORD_ID);
		stylesLibrary.put("kw_comment", KEYWORD_ID);
		stylesLibrary.put("kw_justification", KEYWORD_ID);
		stylesLibrary.put("kw_assumption", KEYWORD_ID);
		stylesLibrary.put("kw_strategy", KEYWORD_ID);				

		stylesLibrary.put("enum_mutuallyExclusive", KEYWORD_ID);

		// stylesAll is included in both library and subclause
		stylesSubclause.putAll(stylesAll);
		stylesLibrary.putAll(stylesAll);
	}

	@Override
	public void highlightAnnexLibrary(AnnexLibrary library, AnnexHighlighterPositionAcceptor acceptor) {
		highlightAnnex(library, acceptor, stylesLibrary);
	}

	@Override
	public void highlightAnnexSubclause(AnnexSubclause subclause, AnnexHighlighterPositionAcceptor acceptor) {
		highlightAnnex(subclause, acceptor, stylesSubclause);
	}

	private void highlightAnnex(EObject annex, AnnexHighlighterPositionAcceptor acceptor,
			Map<String, String> styles) {
		EObject saved = AnnexUtil.getParsedAnnex(annex);
		if (annex != null && saved != null) {
			int offset = AnnexUtil.getAnnexOffset(annex);
			if (AnnexParseUtil.getParseResult(saved) != null) {
				highlight(AnnexParseUtil.getParseResult(saved).getRootNode(), acceptor, offset, styles);
			}
		}
	}

	private boolean isStringLiteral(String str) {
		return str.startsWith("\"") || str.startsWith("'");
	}

	private boolean isComment(String str) {
		return str.startsWith("--");
	}

	private void highlight(ICompositeNode rootNode, AnnexHighlighterPositionAcceptor acceptor, int offset,
			Map<String, String> styles) {
		String lookup;
		// Process all nodes in the parse tree
		for (INode node : rootNode.getAsTreeIterable()) {
			lookup = null;
			// Build keys for things that we want to highlight
			// There probably won't be anything else for the forseeable future
			if (node.getGrammarElement() instanceof Keyword) {
				lookup = "kw_" + ((Keyword) node.getGrammarElement()).getValue();
			} else if (node.getGrammarElement() instanceof EnumLiteralDeclaration) {
				lookup = "enum_" + ((EnumLiteralDeclaration) node.getGrammarElement()).getLiteral().getValue();
			} else if (isStringLiteral(node.getText())) {
				lookup = "string";
			} else if (isComment(node.getText())) {
				lookup = "comment";
			}

			if (lookup != null && styles.containsKey(lookup)) {
				// Highlight token
				acceptor.addPosition(node.getTotalOffset() - offset, node.getTotalLength(), styles.get(lookup));
			}
		}
	}
}
