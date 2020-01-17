package com.ge.research.osate.verdict.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.ge.research.osate.verdict.alloy.AadlAlloyTranslator;
import com.ge.research.osate.verdict.alloy.ThreatLibrary;
import com.ge.research.osate.verdict.alloy.ThreatModelAlloyTranslator;
import com.ge.research.osate.verdict.alloy.ThreatModelParser;

import edu.mit.csail.sdg.ast.Func;

/*
 * General comment: The eventual plan is to not make this part of
 * the plugin. It is currently part of the plugin because we don't
 * have everything fully integrated with UTRC's translator. You
 * should be able to re-use a lot of this code when migrating to use
 * UTRC's translator, but it will require significant re-working.
 */

public class AlloyTranslator extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<String> selection = VerdictHandlersUtils.getCurrentSelection(event);
		
		File dir = new File(selection.get(0));
		
		XtextResourceSet resourceSet = new XtextResourceSet();
		
		// Get all AADL files in the project
		List<EObject> objects = new ArrayList<>();
		for (File file : dir.listFiles()) {
			if (file.getAbsolutePath().endsWith(".aadl")) {
				Resource resource = resourceSet.getResource(URI.createFileURI(file.getAbsolutePath()), true);
				objects.addAll(resource.getContents());
			}
		}
		
		// Will 2020-01-17:
		// This code is attempting to produce an Alloy AST.
		// Seet alloytest.als (left to Paul Meng on Box).
		// I have not yet done a full test because it is incomplete.
		
		// Extract all threat effect models
		ThreatLibrary library = ThreatModelParser.fromObjects(objects);
		List<Func> preds = ThreatModelAlloyTranslator.translate(library.getThreats());
		for (Func pred : preds) {
			System.out.println("translated pred: " + pred.label);
		}
		
		// Extract the system architecture
		// This is still work in progress
		AadlAlloyTranslator.Result model = AadlAlloyTranslator.fromObjects(objects);
		
		// TODO
		
		// Assemble one big model, and check each predicate against each system.
		// I don't know what doing this looks like quite yet.
		// Port, system, connection totals should be reported in the result of AadlAlloyTranslator.
		// Those totals need to be specified as exact input constraints when solving the model.
		
		return null;
	}

}
