package com.ge.research.osate.verdict.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.osate.aadl2.Aadl2Package;
import org.osate.aadl2.Connection;
import org.osate.aadl2.DataSubcomponent;
import org.osate.aadl2.IntegerLiteral;
import org.osate.aadl2.ModalPropertyValue;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyAssociation;
import org.osate.aadl2.Subcomponent;

import com.ge.verdict.vdm.synthesis.ResultsInstance;

public class SynthesisAadlWriter {
	
	public static boolean saveEditor() {
		Shell shell = new Shell();
		// save the invoking .aadl editor if it has unsaved content
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorPart openEditor = page.getActiveEditor();

		if (openEditor != null) {
			boolean response = page.saveEditor(openEditor, true);
			if (response && openEditor.isDirty()) {
				MessageDialog.openError(shell, "VERDICT MBAS",
						"Please save unsaved content in the file before proceeding.");
				return false;
			}
		}	
		return true;
	}
	
	public static void perform(IProject project, File projectDir, ResultsInstance results) {
		if(!saveEditor()) return;
		Map<String, Property> props = new LinkedHashMap<>();

		{
			List<EObject> objs = VerdictHandlersUtils.preprocessAadlFiles(projectDir);
			for (EObject obj : objs) {
				if (obj instanceof Property) {
					Property prop = (Property) obj;
					props.put(prop.getFullName(), prop);
				}
			}
		}

		Map<String, List<ResultsInstance.Item>> elemChanges = new LinkedHashMap<>();

		for (ResultsInstance.Item item : results.items) {
			if (!elemChanges.containsKey(item.component)) {
				elemChanges.put(item.component, new ArrayList<>());
			}
			elemChanges.get(item.component).add(item);
		}

		VerdictHandlersUtils.modifyAadlDocuments(project, (file, resource) -> {
			if (resource != null) {
				resource.getAllContents().forEachRemaining(obj -> {
					if (obj instanceof Subcomponent && !(obj instanceof DataSubcomponent)) {
						Subcomponent comp = (Subcomponent) obj;
						applyChangesToElem(comp, props, elemChanges);
					} else if (obj instanceof Connection) {
						Connection conn = (Connection) obj;
						applyChangesToElem(conn, props, elemChanges);
					}
				});
			} else {
				System.err.println("Error: resource is null for file: " + file);
			}
		});
	}

	private static void applyChangesToElem(NamedElement elem, Map<String, Property> props,
			Map<String, List<ResultsInstance.Item>> elemChanges) {
		if (elemChanges.containsKey(elem.getFullName())) {
			for (ResultsInstance.Item item : elemChanges.get(elem.getFullName())) {
				Property prop = props.get(item.defenseProperty);

				// TODO perhaps figure out how to remove a defense if the DAL goes to 0?

				boolean existing = false;

				// try modifying existing prop if it exists
				for (PropertyAssociation assoc : elem.getOwnedPropertyAssociations()) {
					if (prop.equals(assoc.getProperty())) {
						if (assoc.getOwnedValues().size() != 1) {
							throw new RuntimeException("defense property has a list value, component: " + item.component
									+ ", defense property: " + item.defenseProperty);
						}

						ModalPropertyValue propVal = assoc.getOwnedValues().get(0);
						IntegerLiteral val = (IntegerLiteral) propVal
								.createOwnedValue(Aadl2Package.eINSTANCE.getIntegerLiteral());
						val.setValue(item.outputDal);

						existing = true;
						break;
					}
				}

				// add the prop if it doesn't exist
				if (!existing && item.outputDal > 0) {
					PropertyAssociation assoc = elem.createOwnedPropertyAssociation();
					assoc.setProperty(prop);
					ModalPropertyValue propVal = assoc.createOwnedValue();
					IntegerLiteral val = (IntegerLiteral) propVal
							.createOwnedValue(Aadl2Package.eINSTANCE.getIntegerLiteral());
					val.setValue(item.outputDal);
				}
			}
		}
	}
}
