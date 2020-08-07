package com.ge.research.osate.verdict.aadl2vdm;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.osate.aadl2.AnnexSubclause;
import org.osate.aadl2.DataImplementation;
import org.osate.aadl2.DataPort;
import org.osate.aadl2.DataSubcomponent;
import org.osate.aadl2.DataSubcomponentType;
import org.osate.aadl2.DefaultAnnexSubclause;
import org.osate.aadl2.EnumerationLiteral;
import org.osate.aadl2.ModalPropertyValue;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.NamedValue;
import org.osate.aadl2.Port;
import org.osate.aadl2.PropertyAssociation;
import org.osate.aadl2.PropertyExpression;
import org.osate.aadl2.SystemType;
import org.osate.aadl2.impl.DataImplementationImpl;
import org.osate.aadl2.impl.DataPortImpl;
import org.osate.aadl2.impl.DataSubcomponentImpl;
import org.osate.aadl2.impl.DataTypeImpl;
import org.osate.aadl2.impl.EnumerationLiteralImpl;
import org.osate.aadl2.impl.ListValueImpl;
import org.osate.aadl2.impl.NamedValueImpl;
import org.osate.aadl2.impl.StringLiteralImpl;
import org.osate.pluginsupport.PluginSupportUtil;
import org.osate.xtext.aadl2.Aadl2StandaloneSetup;

import verdict.vdm.vdm_data.DataType;
import verdict.vdm.vdm_data.EnumType;
import verdict.vdm.vdm_data.RecordField;
import verdict.vdm.vdm_data.RecordType;
import verdict.vdm.vdm_data.TypeDeclaration;
import verdict.vdm.vdm_lustre.BinaryOperation;
import verdict.vdm.vdm_lustre.ConstantDeclaration;
import verdict.vdm.vdm_lustre.ContractItem;
import verdict.vdm.vdm_lustre.Expression;
import verdict.vdm.vdm_lustre.FieldDefinition;
import verdict.vdm.vdm_lustre.IfThenElse;
import verdict.vdm.vdm_lustre.LustreProgram;
import verdict.vdm.vdm_lustre.Node;
import verdict.vdm.vdm_lustre.NodeBody;
import verdict.vdm.vdm_lustre.NodeCall;
import verdict.vdm.vdm_lustre.NodeEquation;
import verdict.vdm.vdm_lustre.NodeEquationLHS;
import verdict.vdm.vdm_lustre.NodeParameter;
import verdict.vdm.vdm_lustre.RecordLiteral;
import verdict.vdm.vdm_lustre.RecordProjection;
import verdict.vdm.vdm_lustre.SymbolDefinition;
import verdict.vdm.vdm_model.ComponentType;
import verdict.vdm.vdm_model.Model;
import com.rockwellcollins.atc.agree.agree.Arg;
import com.rockwellcollins.atc.agree.agree.AssumeStatement;
import com.rockwellcollins.atc.agree.agree.BinaryExpr;
import com.rockwellcollins.atc.agree.agree.BoolLitExpr;
import com.rockwellcollins.atc.agree.agree.CallExpr;
import com.rockwellcollins.atc.agree.agree.ConstStatement;
import com.rockwellcollins.atc.agree.agree.DoubleDotRef;
import com.rockwellcollins.atc.agree.agree.EnumLitExpr;
import com.rockwellcollins.atc.agree.agree.AgreeContractSubclause;
import com.ge.verdict.vdm.VdmTranslator;
import com.google.inject.Injector;
import com.rockwellcollins.atc.agree.agree.AgreeContract;
import com.rockwellcollins.atc.agree.agree.EqStatement;
import com.rockwellcollins.atc.agree.agree.Expr;
import com.rockwellcollins.atc.agree.agree.GuaranteeStatement;
import com.rockwellcollins.atc.agree.agree.IfThenElseExpr;
import com.rockwellcollins.atc.agree.agree.IntLitExpr;
import com.rockwellcollins.atc.agree.agree.NamedElmExpr;
import com.rockwellcollins.atc.agree.agree.NodeBodyExpr;
import com.rockwellcollins.atc.agree.agree.NodeDef;
import com.rockwellcollins.atc.agree.agree.NodeEq;
import com.rockwellcollins.atc.agree.agree.NodeStmt;
import com.rockwellcollins.atc.agree.agree.PreExpr;
import com.rockwellcollins.atc.agree.agree.PrimType;
import com.rockwellcollins.atc.agree.agree.RealLitExpr;
import com.rockwellcollins.atc.agree.agree.RecordLitExpr;
import com.rockwellcollins.atc.agree.agree.SelectionExpr;
import com.rockwellcollins.atc.agree.agree.SpecStatement;
import com.rockwellcollins.atc.agree.agree.Type;
import com.rockwellcollins.atc.agree.agree.UnaryExpr;
import com.rockwellcollins.atc.agree.agree.impl.ArgImpl;
import com.rockwellcollins.atc.agree.agree.impl.NodeEqImpl;

public class Agree2Vdm {
	/**
	 * The execute() method performs a set of tasks for translating AADL to VDM
	 *
	 * @param inputDir a reference to a directory
	 *
	 * */
	public Model execute(File inputDir){
		System.err.println("Successfully entered Agree2Vdm Translator! \n\n\n");
	    Model m = new Model();
	  	Aadl2Vdm aadl2vdm= new Aadl2Vdm();
	    //TODO: invoking this method from AADL2VDM translator for testing -to populate non-agree AADL objects
	  	//using preprocessAadlFiles method from AADL2VDM to get objects from the aadl files in the directory alone
	  	List<EObject> objectsFromAllFiles = preprocessAadlFiles(inputDir);//includes objects from imported aadl files
	  	List<EObject> objectsFromFilesInProjects = aadl2vdm.preprocessAadlFiles(inputDir);
	  	m = aadl2vdm.populateVDMFromAadlObjects(objectsFromAllFiles, objectsFromFilesInProjects, m);
	  	//using preprocessAadlFiles method in this class, to get objects in the aadl files in the directory along with the imported aadl files
		m = populateVDMFromAadlAgreeObjects(objectsFromAllFiles, m);
		System.err.println("Working Directory = " + System.getProperty("user.dir"));
		File testXml = new File("/Users/212810885/Desktop/testXML.xml");
		System.err.println("Created File object to store Xml");
		VdmTranslator.marshalToXml(m, testXml);
		System.err.println("Marshalled Model to XML");
		return m;
	}
	//method that processes all aadl files in the directory and the imported contributed aadl files
	public List<EObject> preprocessAadlFiles(File dir) {
		final Injector injector = new Aadl2StandaloneSetup().createInjectorAndDoEMFRegistration();
		final XtextResourceSet rs = injector.getInstance(XtextResourceSet.class);
		rs.addLoadOption(XtextResource.OPTION_RESOLVE_ALL, Boolean.TRUE);
		List<String> aadlFileNames = new ArrayList<>();
		// Obtain all AADL files contents in the project
		List<EObject> objects = new ArrayList<>();
		Aadl2Vdm aadl2vdm= new Aadl2Vdm();
		List<File> dirs = aadl2vdm.collectAllDirs(dir);
		for(File subdir: dirs) {
			for (File file : subdir.listFiles()) {
				if (file.getAbsolutePath().endsWith(".aadl")) {
					System.out.println(file.getAbsolutePath());
					aadlFileNames.add(file.getAbsolutePath());
				}
			}
		}
		for (int i = 0; i < aadlFileNames.size(); i++) {
			rs.getResource(URI.createFileURI(aadlFileNames.get(i)), true);
		}
		EcorePlugin.ExtensionProcessor.process(null);
		//getting aadl imported files
		final List<URI> contributed = PluginSupportUtil.getContributedAadl();
		for (final URI uri : contributed) {
			rs.getResource(uri, true);
		}
		// Load the resources
		Map<String,Boolean> options = new HashMap<String,Boolean>();
	    options.put(XtextResource.OPTION_RESOLVE_ALL, Boolean.TRUE);
		for (final Resource resource : rs.getResources()) {
			try {
				resource.load(options);
				IResourceValidator validator = ((XtextResource) resource).getResourceServiceProvider()
				        .getResourceValidator();
				List<Issue> issues = validator.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
				for (Issue issue : issues) {
				    System.out.println(issue.getMessage());
				}
				//EcoreUtil2.resolveAll(resource);
				//resource.load(null);
			} catch (final IOException e) {
				System.err.println("ERROR LOADING RESOURCE: " + e.getMessage());
			}
		}
		//EcoreUtil2.resolveAll(rs);
		for (final Resource resource : rs.getResources()) {
			resource.getAllContents().forEachRemaining(objects::add);
		}
		return objects;
	}
	/**
	 *  @param objects a List of AADL objects,
	 * 	@param model an empty VDM model to populate
	 *  @return a populated VDM model
	 *
	 * */
	public Model populateVDMFromAadlAgreeObjects(List<EObject> objects, Model model) {
		HashSet<String> dataTypeDecl = new HashSet<String>();
		HashSet<String> nodeDecl = new HashSet<String>();
		// variables for extracting data from the AADL object
		List<SystemType> systemTypes = new ArrayList<>();
		// extracting data from the AADLObject
		for(EObject obj : objects) {
			if (obj instanceof SystemType) {
				//obtaining system Types
				systemTypes.add((SystemType) obj);
			}
		} // end of extracting data from the AADLObjec
		/* Translating agree annex in System Types */
		model = translateAgreeAnnex(systemTypes, model, dataTypeDecl, nodeDecl);
		//return the final model
		return model;
	}

	private Model translateAgreeAnnex(List<SystemType> systemTypes, Model model, HashSet<String> dataTypeDecl, HashSet<String> nodeDecl) {
		LustreProgram lustreProgram = new LustreProgram();
		model.setDataflowCode(lustreProgram);//Initializing the lustre program in the VDM
		System.out.println("Processing "+systemTypes.size()+" SystemTypes for agree annexes");
		for(SystemType sysType : systemTypes) {
			// unpacking sysType
			for(AnnexSubclause annex : sysType.getOwnedAnnexSubclauses()) {				
				if(annex.getName().equalsIgnoreCase("agree")) {
					//annex is of type DefaultAnnexSubclause
					DefaultAnnexSubclause ddASC=(DefaultAnnexSubclause)annex;
					//AnnexSubclause aSC = ddASC.getParsedAnnexSubclause();
					AgreeContractSubclause agreeAnnex= (AgreeContractSubclause)ddASC.getParsedAnnexSubclause();
					//populating agree contracts in the vdm component type -- SHOULD ADD THIS CODE TO AADL2VDM
					verdict.vdm.vdm_lustre.ContractSpec contractSpec = new verdict.vdm.vdm_lustre.ContractSpec();
					EList<EObject> annexContents= agreeAnnex.eContents();
					if(annexContents.isEmpty()) {
						System.out.println("Empty Agree Annex.");
					}
					for(EObject clause : annexContents) {
						//mapping to AgreeContractclause
						AgreeContract agreeContract = (AgreeContract)clause;						
						//getting specStatements
						EList<SpecStatement> specStatements = agreeContract.getSpecs();
						for(SpecStatement specStatement : specStatements) {
							if (specStatement instanceof EqStatement) {
								EqStatement eqStmt = (EqStatement)specStatement;
								//translate EqStatement in Agree to SymbolDefinition in vdm
								SymbolDefinition symbDef = translateEqStatement(eqStmt, model, dataTypeDecl, nodeDecl);
								//Add agree variable/symbol definition to the contractSpec in vdm
								contractSpec.getSymbol().add(symbDef);
							} else if (specStatement instanceof GuaranteeStatement) {
								GuaranteeStatement guaranteeStmt = (GuaranteeStatement)specStatement;
								ContractItem contractItem = translateGuaranteeStatement(guaranteeStmt,  dataTypeDecl, nodeDecl, model);
								contractSpec.getGuarantee().add(contractItem);
							} else if (specStatement instanceof AssumeStatement) {
								AssumeStatement assumeStmt = (AssumeStatement)specStatement;
								ContractItem contractItem = translateAssumeStatement(assumeStmt, dataTypeDecl, nodeDecl, model);
								contractSpec.getAssume().add(contractItem);
							} else {
								System.out.println("Element not recognizable"+clause.eContents().toString());
							}
						}
					}
					List<ComponentType> vdmComponentTypes = model.getComponentType();
					for(ComponentType vdmComponentType: vdmComponentTypes) {
						//populating agree contract details in the corresponding componentType instance in vdm
						if (vdmComponentType.getName().equalsIgnoreCase(sysType.getName())) {
							vdmComponentType.setContract(contractSpec);
						}
					}
					//populating agree contract details in the componentType instance in vdm
					//packComponent.setContract(contractSpec);
				}
			}// End of unpacking sysType
		}
		return model;
	}
	private ContractItem translateAssumeStatement(AssumeStatement assumeStmt, HashSet<String> dataTypeDecl,
			HashSet<String> nodeDecl, Model model) {
		ContractItem contractItem = new ContractItem();
		contractItem.setName(assumeStmt.getStr());
		Expr agreeExpr = assumeStmt.getExpr();
		Expression vdmlustrExpr = getVdmExpressionFromAgreeExpression(agreeExpr, dataTypeDecl, nodeDecl, model);
		contractItem.setExpression(vdmlustrExpr);
		return contractItem;
	}
	//method to map agree statements of the type EqStatement 
	//that have the form: 'eq' Arg (',' Arg)* '=' Expr ';'
	//and create corresponding "SymbolDefinition" for the "ContractSpec" in the vdm model 
	private SymbolDefinition translateEqStatement(EqStatement eqStmt, Model model, HashSet<String> dataTypeDecl, HashSet<String> nodeDecl) {
		SymbolDefinition symbDef = new verdict.vdm.vdm_lustre.SymbolDefinition();		
		//get the right side/expression
		Expr agreeExpr = eqStmt.getExpr();
		Expression vdmExpression = getVdmExpressionFromAgreeExpression(agreeExpr, dataTypeDecl, nodeDecl, model);		
		//get left side of the equation
		EList<Arg> lhsArgs = eqStmt.getLhs();		
		for(Arg lhsArg : lhsArgs) {//left side has the variable names along with their types
			//set the id				
			symbDef.setName(lhsArg.getName());
			//need to parse the type of the variable and should map to appropriate DataType value (plainType, subrangeType, arrayType, tupleType, enumType, recordType, userDefinedType) of the symbol
			Type type = lhsArg.getType();
			//set just name of the type - but make sure you add the type declaration to the vdm model if not already added
			if(!(type instanceof PrimType)) {translateAgreeDataTypeToVdmDataType(type, dataTypeDecl, model);}//this call is mainly to define the type if not already defined
			verdict.vdm.vdm_data.DataType dtype = new verdict.vdm.vdm_data.DataType();
			dtype.setUserDefinedType(getDataTypeName(type));
			//set type name
			symbDef.setDataType(dtype);			
			//set the expression as the value/definition for each variable on the left
			symbDef.setDefinition(vdmExpression);
		}
		return symbDef;
	}
	private void translateAgreeDataTypeToVdmDataType(Type type, HashSet<String> dataTypeDecl, Model model) {
		if(type instanceof DoubleDotRef) {
			DoubleDotRef ddrefType = (DoubleDotRef)type;
			if (ddrefType.getElm() instanceof DataImplementation) {//if it is a AADL data implementation definition
				DataImplementation dataImplementationImpl = (DataImplementation)ddrefType.getElm();
				resolveAADLDataImplementationType(dataImplementationImpl,dataTypeDecl,model);	
			} else if (ddrefType.getElm() instanceof org.osate.aadl2.DataType) {//if it is a AADL data implementation definition
				org.osate.aadl2.DataType aadlDataType = (org.osate.aadl2.DataType)ddrefType.getElm();
				resolveAADLDataType(aadlDataType,dataTypeDecl,model);	
			} else {
				System.out.println("Unresolved data type "+ddrefType.getElm().getName()+" in doubledotref. Not AADL DataImplementation or DataType type.");
			}
		} else {
			System.out.println("Unresolved type value is "+type.toString());
		}
	}
	private String getDataTypeName(Type type) {
		String dtype = "";
		if(type instanceof PrimType) {
			PrimType primType = (PrimType)type;
			verdict.vdm.vdm_data.PlainType plaintype = verdict.vdm.vdm_data.PlainType.fromValue(primType.getName());
			dtype = plaintype.value();
		} else if(type instanceof DoubleDotRef) {
			DoubleDotRef ddrefType = (DoubleDotRef)type;
			if (ddrefType.getElm() instanceof DataImplementation) {//if it is a AADL data implementation definition
				DataImplementation dataImplementation = (DataImplementation)ddrefType.getElm();
				dtype = dataImplementation.getName();
			} else if (ddrefType.getElm() instanceof org.osate.aadl2.DataType) {//if it is a AADL data implementation definition
				org.osate.aadl2.DataType aadlDataType = (org.osate.aadl2.DataType)ddrefType.getElm();
				dtype = aadlDataType.getName();
			} else {
				System.out.println("Unresolved data type "+ddrefType.getElm().getName()+" in doubledotref. Not AADL DataImplementation or DataType type.");
			}
		} else {
			System.out.println("Unresolved type value is "+type.toString());
		}
		return dtype;
	}
	private void resolveAADLDataImplementationType(DataImplementation dataImplementationImpl, HashSet<String> dataTypeDecl, Model model) {
		verdict.vdm.vdm_data.DataType dtype = new verdict.vdm.vdm_data.DataType();
		//GET DETAILS OF THE DATA IMPLEMENTATION AND CREATE CORRESPONDING VDM DATATYPE
		EList<DataSubcomponent> subcomponents= dataImplementationImpl.getOwnedDataSubcomponents();
		if(!subcomponents.isEmpty()) {//if the dataType definition has subcomponents
			RecordType recType = new RecordType();
			for (DataSubcomponent dataSubComp: subcomponents) {
				RecordField recField = new RecordField();
				recField.setName(dataSubComp.getName());
				DataSubcomponentType dataSubCompType = dataSubComp.getDataSubcomponentType();
				if (dataSubCompType instanceof org.osate.aadl2.DataType){
					org.osate.aadl2.DataType aadlDataType = (org.osate.aadl2.DataType)dataSubCompType;
					//the line below is just to ensure that the type's declaration is added to VDM
					resolveAADLDataType(aadlDataType,dataTypeDecl,model); 
					verdict.vdm.vdm_data.DataType recFieldDtype = getVdmTypeFromAADLType(aadlDataType);
					recField.setType(recFieldDtype);	
				} else if (dataSubCompType instanceof DataImplementation){
					DataImplementation dataSubCompDataImplementation = (DataImplementation)dataSubCompType;
					//the line below is just to ensure that the type's declaration is added to VDM
					resolveAADLDataImplementationType(dataSubCompDataImplementation, dataTypeDecl, model);
					verdict.vdm.vdm_data.DataType recFieldDtype = new verdict.vdm.vdm_data.DataType();
					recFieldDtype.setUserDefinedType(dataSubCompDataImplementation.getName());
					recField.setType(recFieldDtype);	
				} else {
					System.out.println("Unexpected Data Subcomponent that is not a DataTypeImpl or DataImplementatioImpl.");
				}
				recType.getRecordField().add(recField);
			}
			dtype.setRecordType(recType);
		} else { //if the dataType is base type boolean or integer or char or string
			System.out.println("Unexpected data implementation type with no subcomponents");
		}
		//DEFINE DATA TYPE IN DECLARATIONS IF NOT ALREADY DEFINED
		String dataImplementationName = dataImplementationImpl.getName();
		if(!dataTypeDecl.contains(dataImplementationName)) {
			dataTypeDecl.add(dataImplementationName);
			//vdm data type declaration
			TypeDeclaration dataTypeVdm = new TypeDeclaration();
			dataTypeVdm.setName(dataImplementationName);
			dataTypeVdm.setDefinition(dtype);
			//add the typeDeclaration to the model
			model.getTypeDeclaration().add(dataTypeVdm);
		}
	}
	private verdict.vdm.vdm_data.DataType getVdmTypeFromAADLType(org.osate.aadl2.DataType aadlDataType) {
		verdict.vdm.vdm_data.DataType dtype = new verdict.vdm.vdm_data.DataType();
		if(aadlDataType.getName().contentEquals("Float")){
			dtype.setPlainType(verdict.vdm.vdm_data.PlainType.fromValue("real"));
		} else if(aadlDataType.getName().contentEquals("Integer")){
			dtype.setPlainType(verdict.vdm.vdm_data.PlainType.fromValue("int"));
		} else if(aadlDataType.getName().contentEquals("Boolean")){
			dtype.setPlainType(verdict.vdm.vdm_data.PlainType.fromValue("bool"));
		} else if (!(aadlDataType.getAllPropertyAssociations().isEmpty())){//if the dataType definition has properties
			EList<PropertyAssociation> properties= aadlDataType.getAllPropertyAssociations();
			updateVDMDatatypeUsingProperties(dtype,properties);
		} else {
			dtype.setUserDefinedType(aadlDataType.getName());
		}
		return dtype;
	}
	private void resolveAADLDataType(org.osate.aadl2.DataType aadlDataType, HashSet<String> dataTypeDecl, Model model) {
		verdict.vdm.vdm_data.DataType dtype = new verdict.vdm.vdm_data.DataType();
		if(aadlDataType.getName().contentEquals("Float")){
			dtype.setPlainType(verdict.vdm.vdm_data.PlainType.fromValue("real"));
		} else if(aadlDataType.getName().contentEquals("Integer")){
			dtype.setPlainType(verdict.vdm.vdm_data.PlainType.fromValue("int"));
		} else if(aadlDataType.getName().contentEquals("Boolean")){
			dtype.setPlainType(verdict.vdm.vdm_data.PlainType.fromValue("bool"));
		} else if (!(aadlDataType.getAllPropertyAssociations().isEmpty())){//if the dataType definition has properties
			EList<PropertyAssociation> properties= aadlDataType.getAllPropertyAssociations();
			updateVDMDatatypeUsingProperties(dtype,properties);
		} else {//not float or int or bool or enum
			System.out.println("Unresolved AADL Data type value is "+aadlDataType.getName());
		}
		//DEFINE DATA TYPE IN DECLARATIONS IF NOT ALREADY DEFINED
		String aadlDataTypeName = aadlDataType.getName();
		if(!dataTypeDecl.contains(aadlDataTypeName) && (!aadlDataTypeName.equalsIgnoreCase("Float")) && (!aadlDataTypeName.equalsIgnoreCase("Integer")) && (!aadlDataTypeName.equalsIgnoreCase("Boolean"))) {
			dataTypeDecl.add(aadlDataTypeName);
			//vdm data type declaration
			TypeDeclaration dataTypeVdm = new TypeDeclaration();
			dataTypeVdm.setName(aadlDataTypeName);
			dataTypeVdm.setDefinition(dtype);
			//add the typeDeclaration to the model
			model.getTypeDeclaration().add(dataTypeVdm);
		}
	}
	//this method checks if the property indicates if it is an enum definition
	//and gets information from the properties to define the enum in the VDM
	public void updateVDMDatatypeUsingProperties(verdict.vdm.vdm_data.DataType dtype, EList<PropertyAssociation> properties) {
		if(properties.size()==2) {
			//check if the property specifies it is enum type
			PropertyAssociation firstProperty = properties.get(0);
			EList<ModalPropertyValue> firstPropertyValues = firstProperty.getOwnedValues();
			if(firstPropertyValues.size()==1) {
				PropertyExpression ownedval = firstPropertyValues.get(0).getOwnedValue();
				if (ownedval instanceof NamedValueImpl) {
					NamedValue namedVal = (NamedValue)ownedval;
					if(namedVal.getNamedValue() instanceof EnumerationLiteralImpl) {
						EnumerationLiteral namedValEnumLit = (EnumerationLiteral)namedVal.getNamedValue();
						if (namedValEnumLit.getName().equalsIgnoreCase("Enum")) {
							EnumType vdmEnumType = new EnumType();
							//Fetch the enum values which are defined as the next property
							PropertyAssociation secondProperty = properties.get(1);
							EList<ModalPropertyValue> secondPropertyValues = secondProperty.getOwnedValues();
							if(firstPropertyValues.size()==1) {
								PropertyExpression secPropValue = secondPropertyValues.get(0).getOwnedValue();
								//enum should have multiple values so check if a list of values are defined
								if (secPropValue instanceof ListValueImpl) {
									ListValueImpl listValueImpl= (ListValueImpl)secPropValue;
									EList<PropertyExpression> listOfValues = listValueImpl.getOwnedListElements();
									for (PropertyExpression enumvalue :listOfValues) {
										if(enumvalue instanceof StringLiteralImpl) {
											StringLiteralImpl stringEnumVal = (StringLiteralImpl)enumvalue;
											vdmEnumType.getEnumValue().add(stringEnumVal.getValue());
										} else {
											System.out.println("Unexpected non-string value for data type of type enum.");
										}
									}
									dtype.setEnumType(vdmEnumType);
								} else {System.out.println("The second property of the data definition is not of ListValueImp type");}
							} else {System.out.println("Unresolved data property. The first property of the data definition has no values or multiple values.");}
						} else {System.out.println("Unresolved data property value. Property's owned value's named value does not contain Enum");}
					} else {System.out.println("Unresolved data property value. Property's owned value's named value is not EnumerationLiteralImpl type.");}
				} else {System.out.println("Unresolved data property value. Property's owned value is not NamedValueImpl type.");}
			} else {System.out.println("Unresolved data property with no values or multiple values.");}
		} else {System.out.println("Unresolved data property. Data definition has 0 or more than 2 properties associated with it");}
	}
	private ContractItem translateGuaranteeStatement(GuaranteeStatement guaranteeStmt, HashSet<String> dataTypeDecl, HashSet<String> nodeDecl, Model model) {
		ContractItem contractItem = new ContractItem();
		contractItem.setName(guaranteeStmt.getStr());
		Expr agreeExpr = guaranteeStmt.getExpr();
		Expression vdmlustrExpr = getVdmExpressionFromAgreeExpression(agreeExpr, dataTypeDecl, nodeDecl, model);
		contractItem.setExpression(vdmlustrExpr);
		return contractItem;
	}
	private void addNodeDefToTypeDeclarations(NodeDef nodeDef, HashSet<String> dataTypeDecl, HashSet<String> nodeDecl, Model model) {
		Node vdmNode = new Node();
		String agreeNodeName = nodeDef.getName();
		vdmNode.setName(agreeNodeName);
		//SETTING NODE INPUT PARAMETERS
		//get agree node's args and set them as input parameter
		EList<Arg> nodeInpArgs = nodeDef.getArgs();
		for(Arg arg : nodeInpArgs) {
			NodeParameter nodeParameter = new NodeParameter();
			nodeParameter.setName(arg.getName());
			//get types of each arg and define those types if needed
			Type argType = arg.getType();
			verdict.vdm.vdm_data.DataType dtype = new verdict.vdm.vdm_data.DataType();
			if(!(argType instanceof PrimType)) {translateAgreeDataTypeToVdmDataType(argType, dataTypeDecl, model);}//this call is mainly to define the type if not already defined
			dtype.setUserDefinedType(getDataTypeName(argType));
			nodeParameter.setDataType(dtype);
			vdmNode.getInputParameter().add(nodeParameter);
		}
		//SETTING NODE OUTPUT PARAMETERS
		EList<Arg> nodeReturnArgs = nodeDef.getRets();
		//get agree node's rets and set them as output parameter
		for(Arg arg : nodeReturnArgs) {
			NodeParameter nodeParameter = new NodeParameter();
			nodeParameter.setName(arg.getName());
			//get types of each arg and define those types if needed
			Type argType = arg.getType();
			verdict.vdm.vdm_data.DataType dtype = new verdict.vdm.vdm_data.DataType();
			if(!(argType instanceof PrimType)) {translateAgreeDataTypeToVdmDataType(argType, dataTypeDecl, model);}//this call is mainly to define the type if not already defined
			dtype.setUserDefinedType(getDataTypeName(argType));
			nodeParameter.setDataType(dtype);
			vdmNode.getOutputParameter().add(nodeParameter);
		}
		//SETTING NODE BODY
		NodeBody vdmNodeBody = new NodeBody();
		//get agree node's body
		NodeBodyExpr agreeNodeBody = nodeDef.getNodeBody();
		EList<NodeStmt> agreeNodeStmts = agreeNodeBody.getStmts();
		for (NodeStmt agreeNodeStmt : agreeNodeStmts) {
			if(agreeNodeStmt instanceof NodeEqImpl) {
				NodeEq agreeNodeEq = (NodeEq)agreeNodeStmt;
				//get all LHS identifiers in the statement and add it to the vdm node equation LHS
				NodeEquation vdmNodeEquation = new NodeEquation();
				EList<Arg> agreeLHSArgs = agreeNodeEq.getLhs();
				NodeEquationLHS vdmNodeEquationLHS = new NodeEquationLHS();//this type is just a list of strings
				for(Arg agreeLHSArg: agreeLHSArgs) {
					vdmNodeEquationLHS.getIdentifier().add(agreeLHSArg.getName());
				}
				vdmNodeEquation.setLhs(vdmNodeEquationLHS);
				//get the RHS i.e.expr of the agree NodeEq and set it as the vdm node equation's RHS
				vdmNodeEquation.setRhs(getVdmExpressionFromAgreeExpression(agreeNodeEq.getExpr(), dataTypeDecl, nodeDecl, model));
				vdmNodeBody.getEquation().add(vdmNodeEquation);
			} else {
				System.out.println("Node contains non-eq type statements");
			}
		}
		vdmNode.setBody(vdmNodeBody);
		vdmNode.setIsFunction(false);
		vdmNode.setIsImported(false);
		if(!nodeDecl.contains(agreeNodeName)) {
			nodeDecl.add(agreeNodeName);
			LustreProgram lustreProgram = model.getDataflowCode();
			lustreProgram.getNodeDeclaration().add(vdmNode);
			model.setDataflowCode(lustreProgram);
		}
	}
	//method to translate expression in Agree to expression in vdm
	private Expression getVdmExpressionFromAgreeExpression(Expr agreeExpr, HashSet<String> dataTypeDecl, HashSet<String> nodeDecl, Model model) {
		Expression vdmExpr = new Expression();
		if(agreeExpr instanceof IfThenElseExpr) {			
			IfThenElseExpr ifexpr = (IfThenElseExpr)agreeExpr;
			IfThenElse ifThenElseVal = new IfThenElse();//for vdm model
			Expression condExpr = getVdmExpressionFromAgreeExpression(ifexpr.getA(), dataTypeDecl, nodeDecl, model);
			ifThenElseVal.setCondition(condExpr);
			Expression thenBranchExpr = getVdmExpressionFromAgreeExpression(ifexpr.getB(), dataTypeDecl, nodeDecl, model);
			ifThenElseVal.setThenBranch(thenBranchExpr);
			Expression elseBranchExpr = getVdmExpressionFromAgreeExpression(ifexpr.getC(), dataTypeDecl, nodeDecl, model);
			ifThenElseVal.setElseBranch(elseBranchExpr);		
			vdmExpr.setConditionalExpression(ifThenElseVal);
		} else if(agreeExpr instanceof CallExpr) {
			CallExpr callExpr = (CallExpr)agreeExpr;
			DoubleDotRef ddref = (DoubleDotRef)callExpr.getRef();
			if (ddref.getElm() instanceof NodeDef) {
				NodeDef nodeDef = (NodeDef)ddref.getElm();
				addNodeDefToTypeDeclarations(nodeDef, dataTypeDecl, nodeDecl, model);
				//create node call in vdm model
				NodeCall vdmNodeCall = new NodeCall();
				//setting node name
				vdmNodeCall.setNodeId(nodeDef.getName());
				EList<Expr> callExprArgs = callExpr.getArgs();
				//below are the parameters passed to the function call
				for(Expr callExprArg: callExprArgs) {
					Expression argExpr = getVdmExpressionFromAgreeExpression(callExprArg, dataTypeDecl, nodeDecl, model);
					//setting node arguments
					vdmNodeCall.getArgument().add(argExpr);
				}
				vdmExpr.setCall(vdmNodeCall);
			} else {
				System.out.println("Unmapped Typed");
			}
		} else if(agreeExpr instanceof NamedElmExpr) {
			NamedElmExpr nmExpr = (NamedElmExpr)agreeExpr;
			vdmExpr.setIdentifier(nmExpr.getElm().getName());
			//define corresponding types in the VDM if not already defined
			if (nmExpr.getElm() instanceof Arg) {
				Arg nmElmArg = (Arg)nmExpr.getElm();
				//define corresponding type in the VDM if not already defined
				Type argType = nmElmArg.getType();
				defineDataTypeDataImplementationTypeInVDM(argType, dataTypeDecl, model);
			} else if(nmExpr.getElm() instanceof Port) {
				Port nmElmPort = (Port)nmExpr.getElm();
				//define corresponding type in the VDM if not already defined
				if(nmElmPort instanceof DataPortImpl) {
					DataPort nmElmDataPort = (DataPort)nmElmPort;
					DataSubcomponentType dSubCompType = nmElmDataPort.getDataFeatureClassifier();
					defineDataTypeDataImplementationTypeInVDM(dSubCompType, dataTypeDecl, model);
				} else {
					System.out.println("Unresolved Port Type");
				}
			} else if(nmExpr.getElm() instanceof ConstStatement) {
				ConstStatement nmElmConstStatement = (ConstStatement)nmExpr.getElm();
				String nmElmConstStatementName = nmElmConstStatement.getName();
				//add const declaration to VDM if not already defined
				if(!dataTypeDecl.contains(nmElmConstStatementName)) {
					dataTypeDecl.add(nmElmConstStatementName);
					ConstantDeclaration vdmConstDeclaration = new ConstantDeclaration();
					vdmConstDeclaration.setName(nmElmConstStatementName);
					vdmConstDeclaration.setDefinition(getVdmExpressionFromAgreeExpression(nmElmConstStatement.getExpr(), dataTypeDecl, nodeDecl, model));
					Type type = nmElmConstStatement.getType();
					if(!(type instanceof PrimType)) {translateAgreeDataTypeToVdmDataType(type, dataTypeDecl, model);}//this call is mainly to define the type if not already defined
					verdict.vdm.vdm_data.DataType dtype = new verdict.vdm.vdm_data.DataType();
					dtype.setUserDefinedType(getDataTypeName(type));
					vdmConstDeclaration.setDataType(dtype);
					LustreProgram lustreProgram = model.getDataflowCode();
					lustreProgram.getConstantDeclaration().add(vdmConstDeclaration);
					model.setDataflowCode(lustreProgram);
				}
			} else {
				System.out.println("Unresolved/unmapped NamedExprElm: "+nmExpr.getElm().getName());
			}
		} else if(agreeExpr instanceof SelectionExpr) {
			//selection expression corresponds to record projection in VDM
			RecordProjection vdmRecordProj = new RecordProjection();
			SelectionExpr selExpr = (SelectionExpr)agreeExpr;
			if(selExpr.getField()==null) {
				System.out.println("Null Selection Expr field: "+selExpr.getField());
			} else {
				NamedElement field = selExpr.getField();
				//set record-projection's reference
				if(selExpr.getTarget()!=null) { //target can be NamedElmExpr or a SelectionExpr
					vdmRecordProj.setRecordReference(getVdmExpressionFromAgreeExpression(selExpr.getTarget(), dataTypeDecl, nodeDecl, model));
				}
				//set record-projection's field id
				vdmRecordProj.setFieldId(field.getName());
				//Define type of the field if not already defined/declared in the VDM
				//also set the name of the field's type as the record-projection's record-type
				if(field instanceof DataPortImpl) {
					DataPort dport = (DataPort)field;
					DataSubcomponentType dSubCompType = dport.getDataFeatureClassifier();
					defineDataTypeDataImplementationTypeInVDM(dSubCompType, dataTypeDecl, model);
					vdmRecordProj.setRecordType(dSubCompType.getName());
				} else if(field instanceof DataSubcomponentImpl) {
					DataSubcomponent dSubComp = (DataSubcomponent)field;
					DataSubcomponentType dSubCompType = dSubComp.getDataSubcomponentType();
					defineDataTypeDataImplementationTypeInVDM(dSubCompType, dataTypeDecl, model);
					vdmRecordProj.setRecordType(dSubCompType.getName());
				} else if(field instanceof ArgImpl) {
					Arg arg = (Arg)field;
					Type argType = arg.getType();
					defineDataTypeDataImplementationTypeInVDM(argType, dataTypeDecl, model);
					if(argType instanceof PrimType) {
						vdmRecordProj.setRecordType(getDataTypeName(argType));
					} else {
						System.out.println("Unresolved Arg Type so not setting record-type in record-projection.");
					}
				} else {
					System.out.println("Unresolved type of field.");
				}
			}
			vdmExpr.setRecordProjection(vdmRecordProj);
		} else if (agreeExpr instanceof BinaryExpr) {
			BinaryExpr binExpr = (BinaryExpr)agreeExpr;
			BinaryOperation binoper = new BinaryOperation();//for vdm
			//set left operand
			Expression leftOperand = getVdmExpressionFromAgreeExpression(binExpr.getLeft(), dataTypeDecl, nodeDecl, model);
			binoper.setLhsOperand(leftOperand);
			//set right operand
			Expression rightOperand = getVdmExpressionFromAgreeExpression(binExpr.getRight(), dataTypeDecl, nodeDecl, model);
			binoper.setRhsOperand(rightOperand);
			//set appropriate operator
			String operator = binExpr.getOp();			
			if(operator.equalsIgnoreCase("->")) {
				vdmExpr.setArrow(binoper);
			} else if(operator.equalsIgnoreCase("=>")) {
				vdmExpr.setImplies(binoper);
			} else if(operator.equalsIgnoreCase("and")) {
				vdmExpr.setAnd(binoper);
			} else if(operator.equalsIgnoreCase("or")) {
				vdmExpr.setOr(binoper);
			} else if(operator.equalsIgnoreCase("=")) {
				vdmExpr.setEqual(binoper);
			} else if(operator.equalsIgnoreCase(">")) {
				vdmExpr.setGreaterThan(binoper);
			} else if(operator.equalsIgnoreCase("<")) {
				vdmExpr.setLessThan(binoper);
			} else if(operator.equalsIgnoreCase(">=")) {
				vdmExpr.setGreaterThanOrEqualTo(binoper);
			} else if(operator.equalsIgnoreCase("<=")) {
				vdmExpr.setLessThanOrEqualTo(binoper);
			} else if(operator.equalsIgnoreCase("+")) {
				vdmExpr.setPlus(binoper);
			} else if(operator.equalsIgnoreCase("-")) {
				vdmExpr.setMinus(binoper);
			} else if(operator.equalsIgnoreCase("!=")|| operator.equalsIgnoreCase("<>")) {
				vdmExpr.setNotEqual(binoper);
			} else if(operator.equalsIgnoreCase("/")) {
				vdmExpr.setDiv(binoper);
			} else if(operator.equalsIgnoreCase("*")) {
				vdmExpr.setTimes(binoper);
			} else {
				System.out.println("Unmapped binary operator: "+operator);
			}
		} else if (agreeExpr instanceof UnaryExpr) {
			UnaryExpr unExpr = (UnaryExpr)agreeExpr;
			Expression singleOperand = getVdmExpressionFromAgreeExpression(unExpr.getExpr(), dataTypeDecl, nodeDecl, model);
			String operator = unExpr.getOp();	
			if(operator.equalsIgnoreCase("this")) {
				vdmExpr.setCurrent(singleOperand);
			} else if(operator.equalsIgnoreCase("not")) {
				vdmExpr.setNot(singleOperand);
			} else if(operator.equalsIgnoreCase("-")) {
				vdmExpr.setNegative(singleOperand);
			} else {
				System.out.println("Unmapped unary operator.");
			}
		} else if (agreeExpr instanceof BoolLitExpr) {
			BoolLitExpr boolExpr = (BoolLitExpr)agreeExpr;			
			vdmExpr.setBoolLiteral(boolExpr.getVal().getValue());
		} else if (agreeExpr instanceof EnumLitExpr) {
			EnumLitExpr enumExpr = (EnumLitExpr)agreeExpr;
			//check if elm is DataImplementationImpl or DataTypeImpl -- if yes add definition to type declarations if not already present
			DoubleDotRef enumType = enumExpr.getEnumType();
			if(enumType.getElm() instanceof DataTypeImpl) {
				org.osate.aadl2.DataType aadlDType = (org.osate.aadl2.DataType)enumType.getElm();
				resolveAADLDataType(aadlDType,dataTypeDecl,model);
			} else {
				System.out.println("Unexpected Elm type for EnumLitExpr");
			}
			vdmExpr.setIdentifier(enumExpr.getValue());
		} else if (agreeExpr instanceof PreExpr) {
			PreExpr preExpr = (PreExpr)agreeExpr;
			Expression expr = getVdmExpressionFromAgreeExpression(preExpr.getExpr(),dataTypeDecl, nodeDecl, model);
			vdmExpr.setPre(expr);
		} else if (agreeExpr instanceof RecordLitExpr) {
			RecordLiteral vdmRecordLiteral = new RecordLiteral();
			RecordLitExpr recLitExpr = (RecordLitExpr)agreeExpr;
			if (recLitExpr.getRecordType() instanceof DoubleDotRef){
				DoubleDotRef recType = (DoubleDotRef)recLitExpr.getRecordType();
				if(recType.getElm().getName()!=null) {
					//check if elm is DataImplementationImpl -- if yes add definition to type declarations if not already present
					if(recType.getElm() instanceof DataImplementation) {
						org.osate.aadl2.DataImplementation aadlDImpl = (org.osate.aadl2.DataImplementation)recType.getElm();
						resolveAADLDataImplementationType(aadlDImpl,dataTypeDecl,model);
					} else {
						System.out.println("Unexpected Elm type for EnumLitExpr");
					}
					//set name of the record literal in the vdm model
					vdmRecordLiteral.setRecordType(recType.getElm().getName());
					//get args and arg-expr and set them as field identifier and value in the vdm model
					EList<NamedElement> recLitArgs = recLitExpr.getArgs();
					EList<Expr> recLitArgsExpr = recLitExpr.getArgExpr();
					//below are the values set to variable
					for(int ind=0; ind<recLitArgs.size();ind++) {
						FieldDefinition fieldDef = new FieldDefinition();
						fieldDef.setFieldIdentifier(recLitArgs.get(ind).getName());
						fieldDef.setFieldValue(getVdmExpressionFromAgreeExpression(recLitArgsExpr.get(ind), dataTypeDecl, nodeDecl, model));
						//set field definitions in the record literal in the vdm model
						vdmRecordLiteral.getFieldDefinition().add(fieldDef);
					}
					vdmExpr.setRecordLiteral(vdmRecordLiteral);
				} else {
					System.out.println("Unexpected Literal's Record Type that has null named elm.");
				}
			} else {
				System.out.println("Unresolved or unmapped record literal expression.");
			}
		} else if(agreeExpr instanceof IntLitExpr){
			IntLitExpr intLitExpr = (IntLitExpr)agreeExpr;
			BigInteger bigInt = new BigInteger(intLitExpr.getVal());
			vdmExpr.setIntLiteral(bigInt);			
		} else if(agreeExpr instanceof RealLitExpr){
			RealLitExpr realLitExpr = (RealLitExpr)agreeExpr;
			BigDecimal bigDecimal = new BigDecimal(realLitExpr.getVal());
			vdmExpr.setRealLiteral(bigDecimal);		
		} else {
			System.out.println("Unresolved/umapped agree expr"+agreeExpr.toString());
		}
		return vdmExpr;
	}
	private void defineDataTypeDataImplementationTypeInVDM(NamedElement nmElement, HashSet<String> dataTypeDecl, Model model) {
		if(nmElement instanceof DataTypeImpl) {
			org.osate.aadl2.DataType aadlDType = (org.osate.aadl2.DataType)nmElement;
			resolveAADLDataType(aadlDType,dataTypeDecl,model);
		} else if(nmElement instanceof DataImplementationImpl) {
			org.osate.aadl2.DataImplementation aadlDImpl = (org.osate.aadl2.DataImplementation)nmElement;
			resolveAADLDataImplementationType(aadlDImpl,dataTypeDecl,model);
		} else {
			System.out.println("Unresolved/unexpected Named Element.");
		}
	}
	private void defineDataTypeDataImplementationTypeInVDM(Type type, HashSet<String> dataTypeDecl, Model model) {
		if(type instanceof PrimType) {
			//type need not be defined/declared in the VDM
		} else if (type instanceof DoubleDotRef) {
			DoubleDotRef typeDoubleDotRef = (DoubleDotRef)type;
			defineDataTypeDataImplementationTypeInVDM(typeDoubleDotRef.getElm(), dataTypeDecl, model);
		} else {
			System.out.println("Unresolved/unexpected Type.");
		} 
	}
}
