package com.ge.research.osate.verdict.alloy;

import java.util.Map.Entry;

import edu.mit.csail.sdg.alloy4.Pair;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.Sig;

public class AlloyPrettyPrinter {
	static StringBuilder sb = new StringBuilder();
	final static String NEWLINE = "\n";
	final static String TAB = "    ";
	public static void printToAlloy(SysArchAlloyModel alloyModel) {
		// Print top-level sigs
		for(Sig sig : alloyModel.topLevelSigs) {
			
			if(sig.isLone != null) {
				sb.append("lone ");
			}
			if(sig.isOne != null) {
				sb.append("one ");
			}
			if(sig.isSome != null) {
				sb.append("some ");
			}
			
			if(sig.isAbstract != null) {
				sb.append("abstract ");
			}
			sb.append("sig ");
			sb.append(sig.label).append(" {");
			processFields(sig, sb);
			sb.append("}").append(NEWLINE);
			processSigFacts(sig, sb);
		}
		
		// Print subset sigs
		for(Entry<Pair<String, Sig>, Sig> entry : alloyModel.subsetSigAndParent.entrySet()) {
			String mult = entry.getKey().a;
			Sig sig = entry.getKey().b;
			Sig parentSig = entry.getValue();
			
//			sb.append(mult + " ");
			if(sig.isOne != null) {
				sb.append("one ");
			} else {
				
			}
			if(sig.isAbstract != null) {
				sb.append("abstract ");
			}
			sb.append("sig ");
			sb.append(sig.label + " in ").append(parentSig.label).append(" {");			
			processFields(sig, sb);	
			sb.append("}").append(NEWLINE);
			processSigFacts(sig, sb);
		}	
		
		// Print sub-sigs
		for(Entry<Pair<String, Sig>, Sig> entry : alloyModel.subSigAndParent.entrySet()) {
			Sig sig = entry.getKey().b;
			Sig parentSig = entry.getValue();
			
			if(sig.isOne != null) {
				sb.append("one ");
			} else {
				
			}
			
			if(sig.isAbstract != null) {
				sb.append("abstract ");
			}
			sb.append("sig ");
			sb.append(sig.label + " extends ").append(parentSig.label).append(" {");
			processFields(sig, sb);	
			sb.append("}").append(NEWLINE);
			processSigFacts(sig, sb);
		}			
		System.out.println("************************************************");
		System.out.println(sb.toString());
		System.out.println("************************************************");
	}

	static void processSigFacts(Sig sig, StringBuilder sb) {
		if(!sig.getFacts().isEmpty()) {
			sb.append("{");
			for(Expr sigFact : sig.getFacts()) {
				sb.append(sigFact).append(NEWLINE);
			}
			sb.append("}");
			sb.append(NEWLINE).append(NEWLINE);
		}	
	}	
	
	static void processFields(Sig sig, StringBuilder sb) {
		sb.append(NEWLINE);
		for(int i = 0; i < sig.getFields().size(); ++i) {
			sb.append(TAB).append(sig.getFields().get(i).label).append(" : ").append(sig.getFields().get(i).decl().expr);
			if(i < sig.getFields().size()-1) {
				sb.append(",").append(NEWLINE);
			} else {
				sb.append(NEWLINE);
			}
		}		
	}

}
