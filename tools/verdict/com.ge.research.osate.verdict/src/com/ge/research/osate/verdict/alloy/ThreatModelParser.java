package com.ge.research.osate.verdict.alloy;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.ge.research.osate.verdict.dsl.VerdictStandaloneSetup;
import com.ge.research.osate.verdict.dsl.verdict.ThreatDatabase;
import com.ge.research.osate.verdict.dsl.verdict.ThreatDefense;
import com.ge.research.osate.verdict.dsl.verdict.ThreatModel;
import com.ge.research.osate.verdict.dsl.verdict.VerdictPackage;
import com.ge.research.osate.verdict.dsl.verdict.VerdictThreatModels;
import com.google.inject.Injector;

public class ThreatModelParser {
	public static ThreatLibrary parse(List<String> paths) {
		List<VerdictThreatModels> models = parseModels(paths);
		ThreatLibrary library =
				models.stream().map(ThreatModelParser::extractThreatModels)
				.collect(ThreatLibrary.collector());
		return library;
	}
	
	public static ThreatLibrary fromObjects(List<EObject> objects) {
		List<VerdictThreatModels> models = objects.stream()
				.map(ThreatModelParser::getVerdict)
				.flatMap(Util::streamOfOptional)
				.collect(Collectors.toList());
		ThreatLibrary library =
				models.stream().map(ThreatModelParser::extractThreatModels)
				.collect(ThreatLibrary.collector());
		return library;
	}
	
	protected static List<VerdictThreatModels> parseModels(List<String> paths) {
		// We invoke the xtext-generated verdict parser. This means that we can only parse .verdict
		// files. Parsing AADL files with a verdict annex is a harder problem.
		
		Injector injector = new VerdictStandaloneSetup().createInjectorAndDoEMFRegistration();
		XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
		VerdictPackage.eINSTANCE.eClass();
		resourceSet.addLoadOption(XtextResource.OPTION_RESOLVE_ALL, true);
		return paths.stream().map(path -> {
			Resource resource = resourceSet.getResource(URI.createFileURI(path), true);
			return getVerdict(resource.getContents().get(0));
		}).flatMap(Util::streamOfOptional).collect(Collectors.toList());
	}
	
	protected static Optional<VerdictThreatModels> getVerdict(EObject obj) {
		return Util.searchEObject(obj, VerdictThreatModels.class);
	}
	
	protected static ThreatLibrary extractThreatModels(VerdictThreatModels model) {
		List<ThreatDatabase> databases =
				model.getStatements().stream()
				.filter(ThreatDatabase.class::isInstance)
				.map(ThreatDatabase.class::cast)
				.collect(Collectors.toList());
		List<ThreatModel> threats =
				model.getStatements().stream()
				.filter(ThreatModel.class::isInstance)
				.map(ThreatModel.class::cast)
				.collect(Collectors.toList());
		List<ThreatDefense> defenses =
				model.getStatements().stream()
				.filter(ThreatDefense.class::isInstance)
				.map(ThreatDefense.class::cast)
				.collect(Collectors.toList());
		return new ThreatLibrary(databases, threats, defenses);
	}
}
