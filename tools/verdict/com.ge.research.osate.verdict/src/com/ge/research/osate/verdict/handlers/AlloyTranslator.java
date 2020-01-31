package com.ge.research.osate.verdict.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.outline.impl.EObjectNode;
import org.eclipse.xtext.ui.editor.utils.EditorUtils;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;
import org.osate.aadl2.ComponentImplementation;
import org.osate.aadl2.Connection;
import org.osate.aadl2.Element;
import org.osate.aadl2.ModalPropertyValue;
import org.osate.aadl2.NamedValue;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyAssociation;
import org.osate.aadl2.PropertyExpression;
import org.osate.aadl2.PropertyOwner;
import org.osate.aadl2.PropertySet;
import org.osate.aadl2.PublicPackageSection;
import org.osate.aadl2.Subcomponent;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.SystemType;
import org.osate.aadl2.impl.BooleanLiteralImpl;
import org.osate.aadl2.impl.ConnectionImpl;
import org.osate.aadl2.impl.EnumerationLiteralImpl;
import org.osate.aadl2.impl.IntegerLiteralImpl;
import org.osate.aadl2.impl.MetaclassReferenceImpl;
import org.osate.aadl2.impl.ModalPropertyValueImpl;
import org.osate.aadl2.impl.NamedValueImpl;
import org.osate.aadl2.impl.PropertyAssociationImpl;
import org.osate.aadl2.impl.PropertyImpl;
import org.osate.aadl2.impl.PropertySetImpl;
import org.osate.aadl2.impl.RealLiteralImpl;
import org.osate.aadl2.impl.StringLiteralImpl;
import org.osate.aadl2.instance.ConnectionInstance;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instantiation.InstantiateModel;
import org.osate.aadl2.properties.PropertyAcc;

import com.ge.research.osate.verdict.alloy.AadlAlloyTranslator;
import com.ge.research.osate.verdict.alloy.SysArchAlloyModel;
import com.ge.research.osate.verdict.alloy.ThreatLibrary;
import com.ge.research.osate.verdict.alloy.ThreatModelAlloyTranslator;
import com.ge.research.osate.verdict.alloy.ThreatModelParser;
import com.ge.research.osate.verdict.alloy.Util;

import edu.mit.csail.sdg.ast.Func;

/*
 * General comment: The eventual plan is to not make this part of
 * the plugin. It is currently part of the plugin because we don't
 * have everything fully integrated with UTRC's translator. You
 * should be able to re-use a lot of this code when migrating to use
 * UTRC's translator, but it will require significant re-working.
 */

public class AlloyTranslator extends AbstractHandler {
	public static List<Property> aadlProps = new ArrayList<Property>();
	
	protected IStatus runJob(Element sel, URI uri, IProgressMonitor monitor) {
	  try {
	      if (sel instanceof ComponentImplementation) {
	          final ComponentImplementation compImpl = (ComponentImplementation) sel;
	          for(PropertyAssociation pa : compImpl.getAllPropertyAssociations()) {
	        	  System.out.println("pa.getProperty() = " + pa.getProperty().getFullName());
	          }
	          SystemInstance sysInst = InstantiateModel.buildInstanceModelFile(compImpl);
	          
	          for(ConnectionInstance ci : sysInst.getConnectionInstances()) {
	        	  System.out.println("ci.getFullName() = "+ci.getFullName());
	        	  System.out.println("&&&& ci.getOwnedPropertyAssociations().size()" + ci.getOwnedPropertyAssociations().size());
	        	  
	        	  for(PropertyAssociation pa : ci.getOwnedPropertyAssociations()) {
	        		  System.out.println("pa.getProperty().getFullName() = " + pa.getProperty().getFullName());
	        		  System.out.println("pa.getProperty().getQualifiedName() = " + pa.getProperty().getQualifiedName());	        		  
	        	  }
	          }
//	          for(Connection con : compImpl.getAllConnections()) {
//	        	  for(PropertyAssociation pa : con.getOwnedPropertyAssociations()) {
//	        		  System.out.println("pa.getProperty().getFullName() = " + pa.getProperty().getFullName());
//	        		  System.out.println("pa.getProperty().getQualifiedName() = " + pa.getProperty().getQualifiedName());
//	        	  }
//	          }
				System.out.println("****************************Translation Starts****************************");
				
	      } else {
	    	  System.out.println("it is not a component implementation!");
	      }
	  } catch (Throwable t) {
	      return Status.CANCEL_STATUS;
	  }
	  return Status.OK_STATUS;
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (VerdictHandlersUtils.startRun()) {
			IIntroPart introPart = PlatformUI.getWorkbench().getIntroManager().getIntro();
			PlatformUI.getWorkbench().getIntroManager().closeIntro(introPart);
			VerdictHandlersUtils.setPrintOnConsole("MBAA Output");
			
			Thread mbasAnalysisThread = new Thread() {
				@Override
				public void run() {
					File dir = new File(VerdictHandlersUtils.getCurrentSelection(event).get(0));
					
					XtextResourceSet resourceSet = new XtextResourceSet();
					
					// Get all AADL files in the project
					List<EObject> objects = new ArrayList<>();
					for (File file : dir.listFiles()) {
						if (file.getAbsolutePath().endsWith(".aadl")) {
							Resource resource = resourceSet.getResource(URI.createFileURI(file.getAbsolutePath()), true);
							objects.addAll(resource.getContents());
						}
					}
					AadlAlloyTranslator.translateFromAADLObjects(objects);
//					test(VerdictHandlersUtils.getCurrentSelection(event));
//					runOnModelInst(event);
				}
			};
			mbasAnalysisThread.start();	
			VerdictHandlersUtils.finishRun();
			
//			File dir = new File(selection.get(0));
//			
//			XtextResourceSet resourceSet = new XtextResourceSet();
//			
//			// Get all AADL files in the project
//			List<EObject> objects = new ArrayList<>();
//			for (File file : dir.listFiles()) {
//				if (file.getAbsolutePath().endsWith(".aadl")) {
//					Resource resource = resourceSet.getResource(URI.createFileURI(file.getAbsolutePath()), true);
//					objects.addAll(resource.getContents());
//				}
//			}
//			
//			// Will 2020-01-17:
//			// This code is attempting to produce an Alloy AST.
//			// See alloytest.als (left to Paul Meng on Box).
//			// I have not yet done a full test because it is incomplete.
//			
//			// Extract the system architecture
//			// This is still work in progress
//			SysArchAlloyModel.loadBuiltinConstructs();
//			AadlAlloyTranslator.translateFromAADLObjects(objects);
//			
//			// Extract all threat effect models
//			ThreatLibrary library = ThreatModelParser.fromObjects(objects);
//			List<Func> preds = ThreatModelAlloyTranslator.translate(library.getThreats());
//			for (Func pred : preds) {
//				System.out.println("translated pred: " + pred.label);
//			}
			
			
			// TODO
			
			// Assemble one big model, and check each predicate against each system.
			// I don't know what doing this looks like quite yet.
			// Port, system, connection totals should be reported in the result of AadlAlloyTranslator.
			// Those totals need to be specified as exact input constraints when solving the model.
			
				
		}
		return null;		
	}
	
	public void runOnModelInst(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		
		if (selection instanceof IStructuredSelection) {
            IStructuredSelection iss = (IStructuredSelection) selection;
            if (iss.size() == 1) {
            	EObjectNode node = (EObjectNode) iss.getFirstElement();
            	final URI uri = node.getEObjectURI();
            	final XtextEditor xtextEditor = EditorUtils.getActiveXtextEditor(event);

                WorkspaceJob job = new WorkspaceJob("AADL to Alloy Translation") {
                    @Override
                    public IStatus runInWorkspace(final IProgressMonitor monitor) {
                        return xtextEditor.getDocument().readOnly(
                                new IUnitOfWork<IStatus, XtextResource>() {
                                    @Override
                                    public IStatus exec(XtextResource resource) throws Exception {
                                        EObject eobj = resource.getResourceSet().getEObject(uri, true);
                                        if (eobj instanceof Element) {
                                            return runJob((Element) eobj, uri, monitor);
                                        } else {
                                            return Status.CANCEL_STATUS;
                                        }
                                    }
                                });
                    }
                };
                job.setRule(ResourcesPlugin.getWorkspace().getRoot());
                job.schedule();
            } else {
            	System.out.println("iss size is not 1!");
            }
        } else {
			System.out.println((ComponentImplementation)selection);
			VerdictLogger.warning("Selection is not recognized!");
		}	
	}
	
	public static void test(List<String> selection) {
		
		File dir = new File(selection.get(0));
		XtextResourceSet resourceSet = new XtextResourceSet();
		// Get all AADL files in the project
		List<EObject> objects = new ArrayList<>();
		for (File file : dir.listFiles()) {
			
			if (file.getAbsolutePath().endsWith(".aadl")) {
				System.out.println("****** File name : " + file.getAbsolutePath());
				Resource resource = resourceSet.getResource(URI.createFileURI(file.getAbsolutePath()), true);
				objects.addAll(resource.getContents());
			}
		}
//		AadlAlloyTranslator.translateFromAADLObjects(objects);
		translateFromAADLObjects(objects);
	}
	
	public static void translateFromAADLObjects(Collection<EObject> objects) {
		List<PublicPackageSection> models = objects.stream()
				.map(AlloyTranslator::getModel)
				.flatMap(Util::streamOfOptional)
				.collect(Collectors.toList());
		
		List<SystemType> systems = new ArrayList<>();
		List<SystemImplementation> systemImpls = new ArrayList<>();
		List<PropertySet> properties = new ArrayList<>();
		
		// Collect component type and implementation 
		for (PublicPackageSection model : models) {
			TreeIterator<EObject> it = model.eAllContents();
			while (it.hasNext()) {
				EObject obj = it.next();
				if (obj instanceof SystemType) {
					systems.add((SystemType) obj);
					it.prune();
				} else if (obj instanceof SystemImplementation) {
					systemImpls.add((SystemImplementation) obj);
					it.prune();
				} else {
					it.prune();
					System.out.println(obj.toString());
				}
			}
		}
		
		
		for(EObject obj : objects) {
			if(obj instanceof PropertySetImpl) {
				properties.add((PropertySetImpl)obj);
				System.out.println("Property set name: " + ((PropertySetImpl)obj).getName());
				for(Property p : ((PropertySetImpl)obj).getOwnedProperties()) {
					System.out.println("******** Property name : "+((PropertyImpl)p).getName());
					
					for(PropertyOwner po : p.getAppliesTos()) {
						System.out.println("property owner:" + po);
						System.out.println("0 ((MetaclassReferenceImpl)po).getMetaclass().getName() = " + ((MetaclassReferenceImpl)po).getMetaclass().getName());
						
						if(((MetaclassReferenceImpl)po).getMetaclass().getName().equalsIgnoreCase("connection")) {
							System.out.println("1 ((MetaclassReferenceImpl)po).getMetaclass().getName() = " + ((MetaclassReferenceImpl)po).getMetaclass().getName());	
							aadlProps.add(p);		
						}
					}
					
					
//					if(((PropertyImpl)p).getType() instanceof EnumerationTypeImpl) {
//						for(NamedElement ne : ((EnumerationTypeImpl)((PropertyImpl)p).getType()).getMembers()) {
//							System.out.println("** EnumValue: " + ne.getName());
//						}
//					} else if(((PropertyImpl)p).getType() instanceof AadlIntegerImpl) {
//						NumericRange range = ((AadlIntegerImpl)((PropertyImpl)p).getType()).getRange();
//						PropertyExpression lbExpr = range.getLowerBound();
//						System.out.println("Range lower bound expr: " + lbExpr);
//						System.out.println("Range lower bound: " + lbExpr.toString());
//						
//						PropertyExpression ubExpr = range.getUpperBound();
//						System.out.println("Range upper bound: " + ubExpr.toString());
//						
//					}
				}
			} 			
		}
		
		
		for (SystemImplementation systemImpl : systemImpls) {
			translateSystemImpl(systemImpl);
		}
	}	
	
	protected static void translateSystemImpl(SystemImplementation systemImpl) {
		for(Connection conn : systemImpl.getOwnedConnections()) {
			System.out.println("############## Connection Full Name: " + conn.getFullName() + " ##############");
			
			for(Property prop : aadlProps) {
				
				System.out.println("--------- Property Name: " + prop.getFullName());
//				System.out.println(" ---------- property simple value" + ((ConnectionImpl)conn).getSimplePropertyValue(prop));
				System.out.println("--------- Property Valu 1: " + conn.getPropertyValue(prop));
//				System.out.println("--------- Property Value conn.getNonModalPropertyValue(prop): " + getPropertyValue(conn.getNonModalPropertyValue(prop)));
				
//				PropertyAcc pa = conn.getPropertyValue(prop);
//				
//				if(pa.getAssociations().size() > 0) {
//					for(PropertyAssociation propAss : pa.getAssociations()) {
//						if(propAss.getOwnedValues().size() == 0) {
//							System.out.println("------- 0 Property value is null! ");
//						} else {
//							for(ModalPropertyValue propVal: propAss.getOwnedValues()) {
//								PropertyExpression propExpr = propVal.getOwnedValue();
//								if(propExpr == null) {
//									System.out.println("------- 1 Property value is null! ");
//								} else {
//									System.out.println("------- 2 Property value = " + propExpr);
//								}
//							}
//						}
//					}	
//				} else {
//					System.out.println("pa.getAssociations().size() = " + pa.getAssociations().size());
//				}
//				
			}
			
			for(PropertyAssociation pa : ((ConnectionImpl)conn).getOwnedPropertyAssociations()) {
//				PropertySet ps = pa.getProperty();
				PropertyAssociationImpl paImpl = ((PropertyAssociationImpl)pa);
				
				System.out.println("paImpl.getChildren() = " + paImpl.getChildren());
				System.out.println("paImpl.getChildren().size() = " + paImpl.getChildren().size());
				
				for(Element e : paImpl.getChildren()) {
					System.out.println("((ModalPropertyValueImpl)e).getFullName() = "+((ModalPropertyValueImpl)e).getFullName());
					System.out.println("&^%$^&& ((ModalPropertyValueImpl)e).getOwnedValue() = " + getPropertyValue(((ModalPropertyValueImpl)e).getOwnedValue()));
					
				}
//				System.out.println("paImpl.basicGetProperty() = " + paImpl.basicGetProperty());
//				System.out.println("paImpl.getProperty() = " + paImpl.getProperty());
//				System.out.println("conn.getPropertyValue(pa.getProperty()) = " + conn.getPropertyValue(pa.getProperty()));
//				if(pa.getProperty() instanceof PropertyImpl) {
//					System.out.println("**** ((PropertyImpl)pa.getProperty()) = " + ((PropertyImpl)(pa.getProperty())));
//					System.out.println("**** ((PropertyImpl)pa.getProperty()).getFullName() = " + ((PropertyImpl)(pa.getProperty())).getFullName());
//					System.out.println("**** ((PropertyImpl)pa.getProperty()).getQualifiedName() = " + ((PropertyImpl)(pa.getProperty())).getQualifiedName());
//				}
//				
				for(ModalPropertyValue propVal: paImpl.getOwnedValues()) {
//					System.out.println("propVal.getNamespace() = " + propVal.getNamespace());
//					System.out.println("propVal.getName() = " + propVal.getName());
//					System.out.println("propVal.getQualifiedName() = " + propVal.getQualifiedName());
					
					PropertyExpression propExpr = propVal.getOwnedValue();
					System.out.println("**** propExpr = "+ getPropertyValue(propExpr));
				}
//				System.out.println("ModalPropertyValue.size = " + pa.getOwnedValues().size());				
//				System.out.println("pa.getProperty() = " + pa.getProperty());
//				System.out.println("pa.getProperty().getDefaultValue() = " + pa.getProperty().getDefaultValue());
//				System.out.println("pa.getProperty().getName(): " + pa.getProperty().getName());
//				System.out.println("pa.getProperty().getPropertyValue(pa.getProperty()): " + pa.getProperty().getPropertyValue(pa.getProperty()));
			}
//			
//			System.out.println("------ Src Connection End");
//			ConnectionEnd srcConnectionEnd = conn.getAllSource();
//			
//			if(srcConnectionEnd.getContainingComponentImpl() != null) {
//				System.out.println("srcConnectionEnd.getContainingComponentImpl().getFullName() = " + srcConnectionEnd.getContainingComponentImpl().getFullName());
//			} else {
//				System.out.println("srcConnectionEnd.getContainingComponentImpl().getFullName() = " + null);
//			}
//			System.out.println("srcConnectionEnd.getContainingClassifier().getFullName() = " + srcConnectionEnd.getContainingClassifier().getFullName());
//			System.out.println("srcConnectionEnd.getFullName() = " + srcConnectionEnd.getFullName());
//			System.out.println("srcConnectionEnd.getQualifiedName() = " + srcConnectionEnd.getQualifiedName());
//			System.out.println("srcConnectionEnd.getName() = " + srcConnectionEnd.getName());
//			
//			System.out.println("------ Dest Connection End");
//			
//			ConnectionEnd destConnectionEnd = conn.getAllDestination();
//			
//			if(conn.getAllDestinationContext() == null) {
//				System.out.println("conn.getAllDestinationContext().getFullName() = " + null);
//			} else {
//				System.out.println("conn.getAllDestinationContext().getFullName() = " + conn.getAllDestinationContext().getFullName());	
//			}
//			
//			for(Element element : destConnectionEnd.getOwnedElements()) {
//				System.out.println("destConnectionEnd.getOwnedElements() = " + element.toString());
//			}
//			if(destConnectionEnd.getContainingComponentImpl() != null) {
//				System.out.println("destConnectionEnd.getContainingComponentImpl().getFullName() = " + destConnectionEnd.getContainingComponentImpl().getFullName());
//			} else {
//				System.out.println("destConnectionEnd.getContainingComponentImpl().getFullName() = " + null);
//			}
//			System.out.println("destConnectionEnd.getElementRoot().getFullName() = " + destConnectionEnd.getElementRoot().getFullName());
//			System.out.println("destConnectionEnd.getContainingClassifier().getFullName() = " + destConnectionEnd.getContainingClassifier().getFullName());
//			System.out.println("destConnectionEnd.getFullName() = " + destConnectionEnd.getFullName());
//			System.out.println("destConnectionEnd.getQualifiedName() = " + destConnectionEnd.getQualifiedName());
//			System.out.println("destConnectionEnd.getName() = " + destConnectionEnd.getName());	
//			System.out.println("************** subcomponents **************");
//			System.out.println("************** subcomponents **************");
//			
//			for(Subcomponent sub : systemImpl.getOwnedSubcomponents()) {
//				System.out.println(" sub.getName() = " + sub.getName());
////				System.out.println("sub.getOwnedPropertyAssociations() = " + sub.getOwnedPropertyAssociations());
//				
//				for(PropertyAssociation pa : sub.getOwnedPropertyAssociations()) {
//					if(pa instanceof PropertyAssociationImpl) {
////						System.out.println("((PropertyAssociationImpl)pa).basicGetProperty().qualifiedName() = " + ((PropertyAssociationImpl)pa).basicGetProperty().qualifiedName());
//						System.out.println(("(PropertyAssociationImpl)pa) =  " + (PropertyAssociationImpl)pa));
//						System.out.println(" ((PropertyAssociationImpl)pa).getElementRoot() = " + ((PropertyAssociationImpl)pa).getElementRoot().getFullName());
//						System.out.println("((PropertyAssociationImpl)pa).getOwner() = " + ((PropertyAssociationImpl)pa).getOwner());
//						System.out.println("((PropertyAssociationImpl)pa).getProperty() =" + ((PropertyAssociationImpl)pa).getProperty());
//					}
//				}
//			}

		}
	}
	
	public static String getPropertyValue(PropertyExpression exp)
	{
		if(exp == null) {
			return null;
		}
		
		String value = "";
		
		if (exp instanceof BooleanLiteralImpl)
		{
			BooleanLiteralImpl bool = ((BooleanLiteralImpl) exp);
			
			value = Boolean.toString(bool.getValue());
		}
		else if (exp instanceof IntegerLiteralImpl)
		{
			IntegerLiteralImpl inte = ((IntegerLiteralImpl) exp);
			value = Long.toString(inte.getValue());
		}
		else if (exp instanceof RealLiteralImpl)
		{
			RealLiteralImpl real = ((RealLiteralImpl) exp);
			value = Float.toString((float) (real.getValue()));
		}
		else if (exp instanceof StringLiteralImpl)
		{
			value = ((StringLiteralImpl) exp).getValue();
			
		}
		else
		{
			NamedValueImpl namedValue =((NamedValueImpl) exp);
			
			if (namedValue.getNamedValue() instanceof EnumerationLiteralImpl)
			{
				EnumerationLiteralImpl enu = ((EnumerationLiteralImpl) namedValue.getNamedValue());
				value = enu.getName();
			}
			else if (namedValue.getNamedValue() instanceof PropertyImpl)
			{
				PropertyImpl namedValProp = (PropertyImpl)namedValue.getNamedValue();
				System.out.println("namedValProp.basicGetPropertyType() = " + namedValProp.basicGetPropertyType());
				System.out.println("namedValProp.basicGetType() = " + namedValProp.basicGetType());
				System.out.println("namedValProp.basicGetType() = " + namedValProp.getType());
				System.out.println("******* namedValProp.getFullName() = " + namedValProp.getFullName());
			} else {
				System.out.println("*&*&*&*&  namedValue.getNamedValue() = " + namedValue.getNamedValue());
			}
		}
		
		return value;
	}	
	
	protected static Optional<PublicPackageSection> getModel(EObject obj) {
		return Util.searchEObject(obj, PublicPackageSection.class);
	}
	

}
