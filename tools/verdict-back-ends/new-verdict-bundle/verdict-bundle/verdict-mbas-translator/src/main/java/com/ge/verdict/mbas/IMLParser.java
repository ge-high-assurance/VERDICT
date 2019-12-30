/* See LICENSE in project directory */
package com.ge.verdict.mbas;

import com.ge.verdict.utils.Logger;
import com.ge.verdict.utils.iml.IMLBuiltInType;
import com.ge.verdict.utils.iml.IMLConnector;
import com.ge.verdict.utils.iml.IMLNamedType;
import com.ge.verdict.utils.iml.IMLTrait;
import com.ge.verdict.utils.iml.VerdictIMLType;
import com.ge.verdict.utils.iml.VerdictProperty;
import com.ge.verdict.utils.iml.IMLTypeLib;
import com.ge.verdict.utils.iml.IMLTraitLib;
import com.utc.utrc.hermes.iml.iml.AndExpression;
import com.utc.utrc.hermes.iml.iml.Annotation;
import com.utc.utrc.hermes.iml.iml.Assertion;
import com.utc.utrc.hermes.iml.iml.AtomicExpression;
import com.utc.utrc.hermes.iml.iml.Datatype;
import com.utc.utrc.hermes.iml.iml.ExpressionTail;
import com.utc.utrc.hermes.iml.iml.FloatNumberLiteral;
import com.utc.utrc.hermes.iml.iml.FolFormula;
import com.utc.utrc.hermes.iml.iml.ImlType;
import com.utc.utrc.hermes.iml.iml.Model;
import com.utc.utrc.hermes.iml.iml.NamedType;
import com.utc.utrc.hermes.iml.iml.NumberLiteral;
import com.utc.utrc.hermes.iml.iml.Property;
import com.utc.utrc.hermes.iml.iml.PropertyList;
import com.utc.utrc.hermes.iml.iml.Refinement;
import com.utc.utrc.hermes.iml.iml.Relation;
import com.utc.utrc.hermes.iml.iml.RelationKind;
import com.utc.utrc.hermes.iml.iml.SequenceTerm;
import com.utc.utrc.hermes.iml.iml.SignedAtomicFormula;
import com.utc.utrc.hermes.iml.iml.SimpleTypeReference;
import com.utc.utrc.hermes.iml.iml.Symbol;
import com.utc.utrc.hermes.iml.iml.SymbolDeclaration;
import com.utc.utrc.hermes.iml.iml.SymbolReferenceTerm;
import com.utc.utrc.hermes.iml.iml.TailedExpression;
import com.utc.utrc.hermes.iml.iml.TermExpression;
import com.utc.utrc.hermes.iml.iml.TermMemberSelection;
import com.utc.utrc.hermes.iml.iml.Trait;
import com.utc.utrc.hermes.iml.iml.TraitExhibition;
import com.utc.utrc.hermes.iml.iml.TruthValue;
import com.utc.utrc.hermes.iml.iml.TupleConstructor;
import com.utc.utrc.hermes.iml.iml.TypeWithProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.EList;

public class IMLParser {
    Map<String, Trait> libTraitNameToTrait = new HashMap<>();
    // Qualified trait name to trait mapping
    Map<String, Trait> nameToTrait = new HashMap<>();
    Map<String, IMLTrait> nameToIMLTrait = new HashMap<>();

    Map<String, NamedType> nameToNamedType = new HashMap<>();
    Map<NamedType, TraitExhibition> namedTypeToExhibits = new HashMap<>();
    Map<NamedType, Refinement> namedTypeToRefinements = new HashMap<>();

    List<NamedType> agreeNodes = new ArrayList<>();

    final String NODE = "node";

    /** Collect input IML model hierarchy and relations */
    public void collectModelInfo(Model imlModel) {
        for (Symbol symbol : imlModel.getSymbols()) {

            if (symbol instanceof SymbolDeclaration) {
                Logger.errAndExit("Don't support the SymbolDeclaration yet: " + symbol);
                // A NamedType is a trait in IML
            } else if (symbol instanceof NamedType) {
                visitNamedType((NamedType) symbol);
            } else if (symbol instanceof Assertion) {
                Logger.errAndExit("Don't support the Assertion yet: " + symbol);
            } else {
                Logger.errAndExit("Cannot reach here!!");
            }
        }
    }

    public void visitNamedType(NamedType namedType) {

        if (namedType instanceof Trait) {
            Trait trait = (Trait) namedType;
            IMLTrait imlTrait = new IMLTrait();            
            String traitName = trait.getName();
            System.out.println("-- Trait Name: " + traitName);

            // Collect refinement relations of traits
            List<Relation> relations = trait.getRelations();
            if (relations != null) {
        		Set<String> refinementTypeNames = new HashSet<>();
        		
                for (Relation relation : relations) {
                    if (relation instanceof Refinement) {
                    	EList<TypeWithProperties> twProps = ((Refinement) relation).getRefinements();
                    	
                    	if(twProps != null) {                    		
                    		for(TypeWithProperties prop : twProps) {
                    			String refinementTypeName = getImlTypeName(prop.getType());
                    			
                    			if(refinementTypeName != null) {
                    				refinementTypeNames.add(refinementTypeName);
                    			} else {
                    				Logger.errAndExit("Unknown refinement type name for type: " + prop.getType());
                    			}
                    		}
                    	}                    	
                    } else {
                        Logger.errAndExit("Don't support the relation other than Refinement yet: " + relation);
                    }
                }
                // check whether the trait refines a component or a system and populate the traits' fields accordingly
                for(String refinementTypeName : refinementTypeNames) {
                	if(refinementTypeName.equals(IMLTraitLib.Component.getTraitName())) {
                		imlTrait.setComponentRefinement(true);	
                		Logger.info("------- trait refines: Component");
                	} else if(refinementTypeName.equals(IMLTraitLib.System.getTraitName())) {
                		imlTrait.setSystemRefinement(true);
                		Logger.info("------- trait refines: System");
                	}    				
                }
            }

            // symbolDecls is the body of the trait, which has events, cyber\safety relation and requirements 
            EList<SymbolDeclaration> symbolDecls = trait.getSymbols();
            
            if (symbolDecls != null) {
                for (SymbolDeclaration symbolDecl : symbolDecls) {
                    Logger.info("Trait body SymbolDeclaration name : " + symbolDecl.getName());             	
                	
                    PropertyList symbolDeclPropList = symbolDecl.getPropertylist();
                    
                    if (symbolDeclPropList != null) {
                        for (Property prop : symbolDeclPropList.getProperties()) {
                            ImlType propRef = prop.getRef();

                            if (propRef != null) {
                                if (propRef instanceof SimpleTypeReference) {
                                    NamedType propNamedType =
                                            ((SimpleTypeReference) propRef).getType();

                                    if (propNamedType != null) {
                                        String name = propNamedType.getName();
                                        System.out.println("Prop name = " + name);

                                        // Assumption: the propSymbolDecls list can only contain a
                                        // comment
                                        EList<SymbolDeclaration> propSymbolDecls =
                                                propNamedType.getSymbols();

                                        if (propSymbolDecls != null) {
                                            for (SymbolDeclaration propSd : propSymbolDecls) {                                            	
                                                System.out.println(
                                                        "propSd.getName() = " + propSd.getName());
                                                
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        } else if (namedType instanceof NamedType) {
            NamedType nt = (NamedType) namedType;
            IMLNamedType imlNamedType = new IMLNamedType();            
            List<Relation> relations = nt.getRelations();
            
            imlNamedType.setName(nt.getName());
            
            System.out.println("************* Named type: " + nt.getName());
            
            // Exhibitions of a named type
            if(relations != null) {
            	for(Relation rel : relations) {
            		if(rel instanceof TraitExhibition) {
            			EList<TypeWithProperties> exhibitions = ((TraitExhibition)rel).getExhibitions();
            			if(exhibitions != null) {
            				for(TypeWithProperties twp : exhibitions) {
            					String imlTypeName = getImlTypeName(twp.getType());
            					
            					// check if the type is a component implementation
            					if(imlTypeName.equals(IMLTraitLib.Implements.getTraitName())) {
            						String implementationBindingTypeName = getImplementationTypeBindingName(twp.getType());
            						imlNamedType.setImplementation(true);
            						imlNamedType.setImplementedComponentName(implementationBindingTypeName);
            					}
            				}
            			}
            		} else {
            			Logger.errAndExit("Unsupported named type relation: " + rel);
            		}
            	}
            }
            
            // symbolDecls is the body of the named type, which could have the connection definitions          
            EList<SymbolDeclaration> symbolDecls = nt.getSymbols();
            
            if (symbolDecls != null) {
                for (SymbolDeclaration symbolDecl : symbolDecls) {
                    Logger.info("****** IML named type body symbol name : " + symbolDecl.getName());
                    VerdictIMLType verdictIMLType = getVerdictImlType(symbolDecl.getType());
                   
                    // If verdictType is null, that means the symboDecl is an assertion (could potentially change)
                    // Skip translating IML assertions here.
                    if(verdictIMLType != null) {
                    	// Collect connections information
                    	if(verdictIMLType == IMLTypeLib.Connector) {                            
                            imlNamedType.addAConnection(collectConnectionsFromIMLSymbol(symbolDecl, imlNamedType));
                        // Collect subcomponent information
                    	} else if(verdictIMLType == IMLTypeLib.UserDefinedType) {                      		
                    		collectSubcomponentProps(symbolDecl, imlNamedType);
                    	} else {
                    		Logger.errAndExit("Unexpected verdict IML type: " + verdictIMLType);
                    	}
                    }
            }            
        } else if (namedType instanceof Datatype) {
        } else if (namedType instanceof Annotation) {
        }
      }   	
    }
    
    /**
     * Check if a named type is an implementation
     * */
    public String isImplementation(NamedType namedType) {
    	List<Relation> relations = namedType.getRelations();
    	
    	if(relations != null) {
    		for(Relation rel : relations) {
    			if(rel instanceof TraitExhibition) {
        			EList<TypeWithProperties> exhibitions = ((TraitExhibition)rel).getExhibitions();
        			if(exhibitions != null) {
        				for(TypeWithProperties twp : exhibitions) {
        					String imlTypeName = getImlTypeName(twp.getType());
        					
        					// check if the type is a component implementation
        					if(imlTypeName.equals(IMLTraitLib.Implements.getTraitName())) {
        						String implementationBindingTypeName = getImplementationTypeBindingName(twp.getType());
        						System.out.println("^^^^^^^^^^^^^^ isImplementation: true");
        						return implementationBindingTypeName;
        					}
        				}
        			}
        		}
    		}
    	}
    	return null;
    }
    
    /**
     * Only need to collect subcomponent properties 
     * */
	public void collectSubcomponentProps(SymbolDeclaration symbolDecl, IMLNamedType imlNamedType) {
		//This is where we will populate subcomponent-component pair or subcomponent-implementation pair
		ImlType imlType = symbolDecl.getType();
		
		if(imlType instanceof SimpleTypeReference) {
			NamedType subcomponentType = ((SimpleTypeReference)imlType).getType();			
			// We assume that a subcomponent can only be a type of component or a implementation
			String subcomponentTypeName = subcomponentType.getName();
			String implCompName = isImplementation(subcomponentType);
			
			if(implCompName != null) {
				imlNamedType.addASubAndCompPair(symbolDecl.getName(), implCompName);
				imlNamedType.addASubAndImplPair(symbolDecl.getName(), subcomponentTypeName);
			} else {
				System.out.println("^^^^^^^^^^^^^^ isImplementation: false");
				imlNamedType.addASubAndCompPair(symbolDecl.getName(), subcomponentTypeName);
			}
			System.out.println("******************** subcomponent type: " + subcomponentType);
		}
		
	    // This is where we obtain all the properties for the sub-components
	    PropertyList symbolDeclPropList = symbolDecl.getPropertylist();
	    
	    if (symbolDeclPropList != null) {
            // User might use different property files, thus properties will be shown 
            // in different property groups	    	
	        for (Property prop : symbolDeclPropList.getProperties()) {
	        	String propGroupName = null;
	            ImlType propRef = prop.getRef();
	            
	            // Set prop group name
	            if (propRef != null) {
	                if (propRef instanceof SimpleTypeReference) {
	                    NamedType propNamedType =
	                            ((SimpleTypeReference) propRef).getType();
	                   
	                    if (propNamedType != null) {
	                    	propGroupName = propNamedType.getName();
	                        System.out.println("subcomponent verdict prop group = " + propGroupName);	                        
	                    }
	                }
	            }
	            
	            // Below is to extract property names and values 
	            TermExpression termExpr = prop.getDefinition();
	            
	            if(termExpr instanceof SequenceTerm) {
	            	collectPropInfoFromTermExpr(((SequenceTerm)termExpr).getReturn(), propGroupName, imlNamedType);
	            } else {
	            	Logger.errAndExit("Unexpected IML term expression while extracting subcomponent information!");
	            }
	        }
	    }
	}
	
	/**
	 * Extract the property and value pair and add it to imlNamedType
	 * */
	public void extractPropAndVal(AtomicExpression atomicExpr, String propGroupName, IMLNamedType imlNamedType) {		
		RelationKind relKind = atomicExpr.getRel();
		
		if(relKind.getName().equalsIgnoreCase("EQ")) {
			// lhsExpr is the prop name
			VerdictProperty verdictProp = new VerdictProperty();
			FolFormula lhsExpr = atomicExpr.getLeft();        				
			verdictProp.setPropGroupName(propGroupName);
			
			if(lhsExpr instanceof SymbolReferenceTerm) {
				verdictProp.setPropName(((SymbolReferenceTerm) lhsExpr).getSymbol().getName());
				System.out.println("*** prop name: " + ((SymbolReferenceTerm) lhsExpr).getSymbol().getName());
			} else {
				Logger.errAndExit("No way to reach here");
			}
			
			// rhsExpr is the prop value; and it could be an enumerate value
			FolFormula rhsExpr = atomicExpr.getRight();
			
			if(rhsExpr instanceof TruthValue) {
				verdictProp.setBool(true);
				verdictProp.setPropValue(((TruthValue)rhsExpr).isTRUE()?"true":"false");
			} else if (rhsExpr instanceof FloatNumberLiteral) {
				verdictProp.setReal(true);
				verdictProp.setPropValue(String.valueOf(((FloatNumberLiteral)rhsExpr).getValue()));
			} else if(rhsExpr instanceof NumberLiteral) {
				verdictProp.setInt(true);
				verdictProp.setPropValue(String.valueOf(((NumberLiteral)rhsExpr).getValue()));
			// This is an enumerate typed property value 
			} else if(rhsExpr instanceof TermMemberSelection){
				// Here we only save the enumerate type's value
				TermMemberSelection rhsSelExpr = (TermMemberSelection)rhsExpr;
				verdictProp.setEnum(true);
				verdictProp.setPropValue(((SymbolReferenceTerm)rhsSelExpr.getMember()).getSymbol().getName());
//				System.out.println("rhsSelExpr.getMember(): " + ((SymbolReferenceTerm)rhsSelExpr.getMember()).getSymbol().getName());
			} else {
				Logger.errAndExit("Unsupported property value data type!");
			}
			// Add verdictProp to IML named type
			imlNamedType.addAVerdictProperty(verdictProp);
		} else {
			Logger.errAndExit("No way to reach here, and it is unsupported property-value pair expression!");
		}		
	}
	
	/**
	 * Recursively traverse the TermExpression
	 * */
	public void collectPropsFromTermExpr(TermExpression termExpr, String propGroupName, IMLNamedType imlNamedType) {
    	if(termExpr instanceof SequenceTerm) {
        	FolFormula folFormula1 = ((SequenceTerm)termExpr).getReturn();
        	
        	if(folFormula1 instanceof SignedAtomicFormula) {
        		FolFormula folFormula2 = ((SignedAtomicFormula)folFormula1).getLeft();
        		
        		if(folFormula2 instanceof AtomicExpression) {
        			extractPropAndVal((AtomicExpression)folFormula2, propGroupName, imlNamedType);
        		} else {
        			Logger.errAndExit("Unexpected IML expression!");
        		}
        	} else if(folFormula1 instanceof AndExpression) {
    			System.out.println(" &&&&&&& and expression left: " + ((AndExpression)folFormula1).getLeft());
    			System.out.println(" &&&&&&& and expression right: " + ((AndExpression)folFormula1).getRight());
    		}        		
    	}		
	}
	
	/**
	 * Collect property-value pairs from a term expression
	 * 
	 * There are two cases to consider for folFormula: 
	 *   1. termExpr is a conjunction of property-value equation pairs
	 *   2. termExpr is a single property-value equation pair
	 * */	
	public void collectPropInfoFromTermExpr(FolFormula folFormula, String propGroupName, IMLNamedType imlNamedType) {
		if(folFormula instanceof SignedAtomicFormula) {
    		FolFormula folFormula2 = ((SignedAtomicFormula)folFormula).getLeft();
    		
    		if(folFormula2 instanceof AtomicExpression) {
    			extractPropAndVal((AtomicExpression)folFormula2, propGroupName, imlNamedType);
    		} else {
    			Logger.errAndExit("Unexpected IML expression!");
    		}			
		} else if(folFormula instanceof AndExpression) {
			// lhsExpr will always be an And expression until the last expression
			FolFormula lhsExpr = ((AndExpression)folFormula).getLeft();
			collectPropInfoFromTermExpr(lhsExpr, propGroupName, imlNamedType);
			
			FolFormula rhsExpr = ((AndExpression)folFormula).getRight();
			collectPropInfoFromTermExpr(rhsExpr, propGroupName, imlNamedType);
		} else {
			Logger.info("********** FolFormula: " + folFormula);
		}
	}
		
    
    /**
     * Get the implementation type binding name for an input ImlType
     * */
    public String getImplementationTypeBindingName(ImlType imlType) {    	
		if(imlType instanceof SimpleTypeReference) {
			SimpleTypeReference simpleType = (SimpleTypeReference)imlType;
			EList<ImlType> bindingTypes = simpleType.getTypeBinding();
			
			if(bindingTypes != null && bindingTypes.size() == 1) {
				return ((SimpleTypeReference)bindingTypes.get(0)).getType().getName();
			} else {
				Logger.errAndExit("Unexpected implementation type binding!");
			}
		} else {
			Logger.errAndExit("Don't support the trait refinement type: " + imlType);
		}    	
    	return null;
    }
    
    /**
     * Get the type name for an input ImlType
     * */
    public String getImlTypeName(ImlType imlType) {
    	String imlTypeName = null;
    	
		if(imlType instanceof SimpleTypeReference) {
			SimpleTypeReference simpleType = (SimpleTypeReference)imlType;
			imlTypeName = simpleType.getType().getName();
		} else {
			Logger.errAndExit("Don't support the trait refinement type: " + imlType);
		}    	
    	return imlTypeName;
    }    
    
    /**
     * ONLY handle built-in types and types declared in IML libraries for now
     * */
    public VerdictIMLType getVerdictImlType(ImlType imlType) {
    	if(imlType == null) {
    		return null;
    	}
    	
    	VerdictIMLType verdictIMLType = null;
    	
    	if(imlType instanceof SimpleTypeReference) {
    		String typeName = getImlTypeName(imlType);
    		
    		if(typeName != null) {
	    		switch(typeName) {
	    			case "Connector":
	    				verdictIMLType = IMLTypeLib.Connector;
	    				break;
	    			case "InDataPort":
	    				verdictIMLType = IMLTypeLib.InDataPort;
	    				break;
	    			case "OutDataPort":
	    				verdictIMLType = IMLTypeLib.OutDataPort;
	    				break;
	    			case "Bool":
	    				verdictIMLType = IMLBuiltInType.BOOL;
	    				break;
	    			case "Int":
	    				verdictIMLType = IMLBuiltInType.INT;
	    				break;
	    			case "Real":
	    				verdictIMLType = IMLBuiltInType.REAL;
	    				break;
	    			case "String":
	    				verdictIMLType = IMLBuiltInType.STRING;
	    				break;
	    			default:
	    				verdictIMLType = IMLTypeLib.UserDefinedType;
	    				break;
	    		} 
    		} 
    	} else {
    		Logger.errAndExit("We don't suppport other ImlType yet: " + imlType);
    	}
    	return verdictIMLType;
    }
    
    /**
     * Assume: imlType is a Simple Type Reference in IML
     * Create a IMLConnector object from a ImlType 
     * */
    public IMLConnector collectConnectionsFromIMLSymbol(SymbolDeclaration symbolDecl, IMLNamedType imlNamedType) {
    	IMLConnector imlConnector = null;
        ImlType symbolDeclType = symbolDecl.getType();
        
        if(getVerdictImlType(symbolDeclType) == IMLTypeLib.Connector) {
        	imlConnector = new IMLConnector();
        	// set connector name
        	Logger.info("Connector name: " + symbolDecl.getName());
        	imlConnector.setConnectorName(symbolDecl.getName());
        	
        	// set connector src/dest port directions
        	if(symbolDeclType instanceof SimpleTypeReference) {
        		EList<ImlType> bindingTypes = ((SimpleTypeReference)symbolDeclType).getTypeBinding();
        		
        		// Assumption: There are only two binding types for a connector
        		if(bindingTypes != null && bindingTypes.size() == 2) {
        			VerdictIMLType srcBindingType = getVerdictImlType(bindingTypes.get(0));
        			VerdictIMLType destBindingType = getVerdictImlType(bindingTypes.get(1));
        			
        			if(srcBindingType == IMLTypeLib.InDataPort) {
        				imlConnector.setSrcPortDir(IMLConnector.PortDir.IN);
        			} else if(srcBindingType == IMLTypeLib.OutDataPort) {
        				imlConnector.setSrcPortDir(IMLConnector.PortDir.OUT);
        			} else {
        				Logger.errAndExit("Unsupported data port type!");
        			}
        			
        			if(destBindingType == IMLTypeLib.InDataPort) {
        				imlConnector.setDestPortDir(IMLConnector.PortDir.IN);
        			} else if(srcBindingType == IMLTypeLib.OutDataPort) {
        				imlConnector.setDestPortDir(IMLConnector.PortDir.OUT);
        			} else {
        				Logger.errAndExit("Unsupported data port type!");
        			}    			
        		} else {
        			Logger.errAndExit("Unexpected binding types for a connector!");
        		}
        	} 
        	
        	// This is to translate the definition body and folFormula 
        	// is an instance of SignedAtomicFormula 
        	// The left-hand side of folFormula is an instance of TailedExpression
        	// and the tail expression of folFormula is a tuple (src, dest)
        	// The right-hand side of folFormula is null
        	FolFormula folFormula = symbolDecl.getDefinition();
        	if(folFormula != null) {
        		if(folFormula instanceof SignedAtomicFormula) {
        			if(((SignedAtomicFormula)folFormula).getLeft() instanceof TailedExpression) {
        				ExpressionTail exprTail = ((TailedExpression)((SignedAtomicFormula)folFormula).getLeft()).getTail();
        				
        				if(exprTail instanceof TupleConstructor) {
        					EList<FolFormula> tupleElements = ((TupleConstructor)exprTail).getElements();
        					
        					// Expect the tuple to be a pair (src, dest) and they are all SignedAtomicFormula
        					if(tupleElements != null && tupleElements.size()==2) {
        						FolFormula srcExpr = ((SignedAtomicFormula)tupleElements.get(0)).getLeft();
        						FolFormula destExpr = ((SignedAtomicFormula)tupleElements.get(1)).getLeft();
        						
        						// srcExpr is an port of some component or component implementation
        						if(srcExpr instanceof TermMemberSelection) {
        							TermExpression recExpr = ((TermMemberSelection)srcExpr).getReceiver();
        							TermExpression memExpr = ((TermMemberSelection)srcExpr).getMember();
        							String srcInstName = ((SymbolDeclaration)((SymbolReferenceTerm)recExpr).getSymbol()).getName();
        							String srcPortName = ((SymbolDeclaration)((SymbolReferenceTerm)memExpr).getSymbol()).getName();
        							
        							ImlType srcInstImlType = ((SymbolDeclaration)((SymbolReferenceTerm)recExpr).getSymbol()).getType();
        							NamedType srcInstType = ((SimpleTypeReference)srcInstImlType).getType();        							
        							String srcInstImplTypeName = isImplementation(srcInstType);
        							
        							if(srcInstImplTypeName != null) {
        								imlConnector.setSrcImplName(srcInstImplTypeName);
        							}
        							
        							imlConnector.setSrcCompName(srcInstType.getName());
        							imlConnector.setSrcInstName(srcInstName);
        							imlConnector.setSrcPortName(srcPortName);
        						// srcExpr must be an port of this component implementation   
        						} else if(srcExpr instanceof SymbolReferenceTerm) {
        							Symbol symbol = ((SymbolReferenceTerm)srcExpr).getSymbol();
        							String srcPortName = ((SymbolDeclaration)symbol).getName();
        							
    								imlConnector.setSrcImplName(imlNamedType.getName());
    								imlConnector.setSrcCompName(imlNamedType.getImplementedComponentName());        							
        							imlConnector.setSrcPortName(srcPortName);
        						} else {
        							Logger.errAndExit("Unexpected tuple element expression: " + srcExpr);
        						}
        						
        						// destExpr is an port of some component or component implementation
        						if(destExpr instanceof TermMemberSelection) {
        							TermExpression recExpr = ((TermMemberSelection)destExpr).getReceiver();
        							TermExpression memExpr = ((TermMemberSelection)destExpr).getMember();
        							String destInstName = ((SymbolDeclaration)((SymbolReferenceTerm)recExpr).getSymbol()).getName();
        							String destPortName = ((SymbolDeclaration)((SymbolReferenceTerm)memExpr).getSymbol()).getName();

        							ImlType destInstImlType = ((SymbolDeclaration)((SymbolReferenceTerm)recExpr).getSymbol()).getType();
        							NamedType destInstType = ((SimpleTypeReference)destInstImlType).getType();        							
        							String destInstImplTypeName = isImplementation(destInstType);
        							
        							if(destInstImplTypeName != null) {
        								imlConnector.setDestImplName(destInstImplTypeName);
        							}
        							
        							imlConnector.setDestCompName(destInstType.getName());        							
        							imlConnector.setDestInstName(destInstName);
        							imlConnector.setDestPortName(destPortName);
    							// destExpr must be an port of this component implementation	
        						} else if(destExpr instanceof SymbolReferenceTerm) {
        							Symbol symbol = ((SymbolReferenceTerm)destExpr).getSymbol();
        							String destPortName = ((SymbolDeclaration)symbol).getName();
        							     							
        							imlConnector.setDestPortName(destPortName);
    								imlConnector.setDestImplName(imlNamedType.getName());
    								imlConnector.setDestCompName(imlNamedType.getImplementedComponentName());
        						} else {
        							Logger.errAndExit("Unexpected tuple element expression in IML connector: " + srcExpr);
        						}      						
        						
        					} else {
            					Logger.errAndExit("Unexpected tuple expression for connector:" + tupleElements);
            				}
        				} 
        			}
        		}
        	}
        }    	
    	

    	return imlConnector;
    }
}
