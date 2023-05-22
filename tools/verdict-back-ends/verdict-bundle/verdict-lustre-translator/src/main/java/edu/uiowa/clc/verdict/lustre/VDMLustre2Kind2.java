package edu.uiowa.clc.verdict.lustre;

import edu.uiowa.cs.clc.kind2.lustre.ComponentBuilder;
import edu.uiowa.cs.clc.kind2.lustre.ContractBodyBuilder;
import edu.uiowa.cs.clc.kind2.lustre.ContractBuilder;
import edu.uiowa.cs.clc.kind2.lustre.Expr;
import edu.uiowa.cs.clc.kind2.lustre.ExprUtil;
import edu.uiowa.cs.clc.kind2.lustre.IdExpr;
import edu.uiowa.cs.clc.kind2.lustre.ImportedComponentBuilder;
import edu.uiowa.cs.clc.kind2.lustre.ModeBuilder;
import edu.uiowa.cs.clc.kind2.lustre.Program;
import edu.uiowa.cs.clc.kind2.lustre.ProgramBuilder;
import edu.uiowa.cs.clc.kind2.lustre.Type;
import edu.uiowa.cs.clc.kind2.lustre.TypeUtil;

import verdict.vdm.vdm_data.DataType;
import verdict.vdm.vdm_data.PlainType;
import verdict.vdm.vdm_data.RecordField;
import verdict.vdm.vdm_data.SubrangeType;
import verdict.vdm.vdm_data.TypeDeclaration;
import verdict.vdm.vdm_lustre.ConstantDeclaration;
import verdict.vdm.vdm_lustre.Contract;
import verdict.vdm.vdm_lustre.ContractImport;
import verdict.vdm.vdm_lustre.ContractItem;
import verdict.vdm.vdm_lustre.ContractMode;
import verdict.vdm.vdm_lustre.ContractSpec;
import verdict.vdm.vdm_lustre.Expression;
import verdict.vdm.vdm_lustre.FieldDefinition;
import verdict.vdm.vdm_lustre.LustreProgram;
import verdict.vdm.vdm_lustre.Node;
import verdict.vdm.vdm_lustre.NodeEquation;
import verdict.vdm.vdm_lustre.NodeParameter;
import verdict.vdm.vdm_lustre.NodeProperty;
import verdict.vdm.vdm_lustre.SymbolDefinition;
import verdict.vdm.vdm_lustre.VariableDeclaration;
import verdict.vdm.vdm_model.Model;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class VDMLustre2Kind2 {
    /**
     * Translate the VDM model to Kind 2 Lustre program.
     *
     * @param vdmModel the VDM model
     * @return the Kind 2 Lustre program
     */
    public static Program translate(Model vdmModel) {
        return visit(vdmModel.getDataflowCode()).build();
    }

    /**
     * Translate VDM model program to Kind 2 Lustre program.
     *
     * @param modelProgram the VDM model program
     * @return the Kind 2 Lustre program builder
     */
    private static ProgramBuilder visit(LustreProgram modelProgram) {
        ProgramBuilder pb = new ProgramBuilder();

        for (TypeDeclaration modelTypeDecl : modelProgram.getTypeDeclaration()) {
            if (modelTypeDecl.getDefinition() == null) {
                pb.defineType(modelTypeDecl.getName());
            } else {
                pb.defineType(modelTypeDecl.getName(), visit(modelTypeDecl.getDefinition()));
            }
        }

        for (ConstantDeclaration modelConstDecl : modelProgram.getConstantDeclaration()) {
            if (modelConstDecl.getDataType() != null) {
                if (modelConstDecl.getDefinition() != null) {
                    pb.createConst(
                            modelConstDecl.getName(),
                            visit(modelConstDecl.getDataType()),
                            visit(modelConstDecl.getDefinition()));
                } else {
                    pb.createConst(modelConstDecl.getName(), visit(modelConstDecl.getDataType()));
                }
            } else {
                pb.createConst(modelConstDecl.getName(), visit(modelConstDecl.getDefinition()));
            }
        }

        for (Contract modelContract : modelProgram.getContractDeclaration()) {
            pb.addContract(visit(modelContract));
        }

        for (Node node : modelProgram.getNodeDeclaration()) {
            if (node.isIsFunction() != null && node.isIsFunction()) {
                if (node.isIsImported() != null && node.isIsImported()) {
                    pb.importFunction(visitImportedComponent(node));
                } else {
                    if (node.getBody().isIsMain() != null && node.getBody().isIsMain()) {
                        pb.setMain(node.getName());
                    }
                    pb.addFunction(visitComponent(node));
                }
            } else {
                if (node.isIsImported() != null && node.isIsImported()) {
                    pb.importNode(visitImportedComponent(node));
                } else {
                    if (node.getBody() != null
                            && node.getBody().isIsMain() != null
                            && node.getBody().isIsMain()) {
                        pb.setMain(node.getName());
                    }
                    pb.addNode(visitComponent(node));
                }
            }
        }

        return pb;
    }

    /**
     * Translate VDM model datatype to Kind 2 Lustre type.
     *
     * @param modelType the VDM model type
     * @return the Kind 2 Lustre type
     */
    private static Type visit(DataType modelType) {
        if (modelType == null) {
            // Change Null DataType to Integer Type
            modelType = new DataType();
            modelType.setPlainType(PlainType.INT);
        }
        if (modelType.getPlainType() != null) {
            return TypeUtil.named(modelType.getPlainType().value());
        } else if (modelType.getUserDefinedType() != null) {
            return TypeUtil.named(modelType.getUserDefinedType());
        } else if (modelType.getSubrangeType() != null) {
            SubrangeType subrange = modelType.getSubrangeType();
            return TypeUtil.intSubrange(subrange.getLowerBound(), subrange.getUpperBound());
        } else if (modelType.getArrayType() != null) {
            return TypeUtil.array(
                    visit(modelType.getArrayType().getDataType()),
                    Integer.parseInt(modelType.getArrayType().getDimension()));
        } else if (modelType.getTupleType() != null) {
            return TypeUtil.tuple(
                    modelType.getTupleType().getDataType().stream()
                            .map(dt -> visit(dt))
                            .collect(Collectors.toList()));
        } else if (modelType.getEnumType() != null) {
            return TypeUtil.enumeration(modelType.getEnumType().getEnumValue());
        } else if (modelType.getRecordType() != null) {
            // fields in the map have the same order as the RecordFields in the RecordField
            // list
            Map<String, Type> fields = new TreeMap<>((x, y) -> -1);
            for (RecordField field : modelType.getRecordType().getRecordField()) {
                fields.put(field.getName(), visit(field.getType()));
            }
            return TypeUtil.record(fields);
        } else {
            System.out.println("Warning: Bogus DataType detected! Replaced with Integer type");
            modelType.setPlainType(PlainType.INT);
            return TypeUtil.named(modelType.getPlainType().value());
        }
        // throw new IllegalArgumentException(
        //        "Datatype should either be: plain, user-defined, subrange, array, tuple, enum, or
        // record type.");
    }

    /**
     * Translate VDM model expression to Kind 2 Lustre expression.
     *
     * @param modelExpr the VDM model expression
     * @return the Kind 2 Lustre expression
     */
    private static Expr visit(Expression modelExpr) {
        if (modelExpr.getIdentifier() != null) {
            return ExprUtil.id(modelExpr.getIdentifier());
        }

        if (modelExpr.isBoolLiteral() != null) {
            return modelExpr.isBoolLiteral() ? ExprUtil.TRUE : ExprUtil.FALSE;
        }

        if (modelExpr.getIntLiteral() != null) {
            return ExprUtil.integer(modelExpr.getIntLiteral().intValue());
        }

        if (modelExpr.getRealLiteral() != null) {
            return ExprUtil.real(modelExpr.getRealLiteral().toString());
        }

        if (modelExpr.getExpressionList() != null) {
            return ExprUtil.list(
                    modelExpr.getExpressionList().getExpression().stream()
                            .map(expr -> visit(expr))
                            .collect(Collectors.toList()));
        }

        if (modelExpr.getRecordLiteral() != null) {
            // fields in the map have the same order as the FieldDefinitions in the
            // FieldDefinition list
            Map<String, Expr> fields = new TreeMap<>((x, y) -> -1);
            for (FieldDefinition field : modelExpr.getRecordLiteral().getFieldDefinition()) {
                fields.put(field.getFieldIdentifier(), visit(field.getFieldValue()));
            }
            return ExprUtil.recordLiteral(modelExpr.getRecordLiteral().getRecordType(), fields);
        }

        if (modelExpr.getArrayExpression() != null) {
            return ExprUtil.array(
                    modelExpr.getArrayExpression().getExpression().stream()
                            .map(expr -> visit(expr))
                            .collect(Collectors.toList()));
        }

        if (modelExpr.getCartesianExpression() != null) {
            throw new UnsupportedOperationException(
                    "Error: Cartesian expressions are not supported!");
        }

        if (modelExpr.getTupleExpression() != null) {
            throw new UnsupportedOperationException("Error: Tuple expressions are not supported!");
        }

        if (modelExpr.getNegative() != null) {
            return ExprUtil.negative(visit(modelExpr.getNegative()));
        }

        if (modelExpr.getPre() != null) {
            return ExprUtil.pre(visit(modelExpr.getPre()));
        }

        if (modelExpr.getCurrent() != null) {
            throw new UnsupportedOperationException(
                    "Error: current expressions are not supported!");
        }

        if (modelExpr.getToInt() != null) {
            return ExprUtil.castInt(visit(modelExpr.getToInt()));
        }

        if (modelExpr.getToReal() != null) {
            return ExprUtil.castReal(visit(modelExpr.getToReal()));
        }

        if (modelExpr.getWhen() != null) {
            throw new UnsupportedOperationException("Error: When expressions are not supported!");
        }

        if (modelExpr.getTimes() != null) {
            return ExprUtil.multiply(
                    visit(modelExpr.getTimes().getLhsOperand()),
                    visit(modelExpr.getTimes().getRhsOperand()));
        }

        if (modelExpr.getDiv() != null) {
            return ExprUtil.divide(
                    visit(modelExpr.getDiv().getLhsOperand()),
                    visit(modelExpr.getDiv().getRhsOperand()));
        }

        if (modelExpr.getMod() != null) {
            return ExprUtil.mod(
                    visit(modelExpr.getMod().getLhsOperand()),
                    visit(modelExpr.getMod().getRhsOperand()));
        }

        if (modelExpr.getIntDiv() != null) {
            return ExprUtil.intDivide(
                    visit(modelExpr.getIntDiv().getLhsOperand()),
                    visit(modelExpr.getIntDiv().getRhsOperand()));
        }

        if (modelExpr.getPlus() != null) {
            return ExprUtil.plus(
                    visit(modelExpr.getPlus().getLhsOperand()),
                    visit(modelExpr.getPlus().getRhsOperand()));
        }

        if (modelExpr.getMinus() != null) {
            return ExprUtil.minus(
                    visit(modelExpr.getMinus().getLhsOperand()),
                    visit(modelExpr.getMinus().getRhsOperand()));
        }

        if (modelExpr.getNot() != null) {
            return ExprUtil.not(visit(modelExpr.getNot()));
        }

        if (modelExpr.getLessThan() != null) {
            return ExprUtil.less(
                    visit(modelExpr.getLessThan().getLhsOperand()),
                    visit(modelExpr.getLessThan().getRhsOperand()));
        }

        if (modelExpr.getLessThanOrEqualTo() != null) {
            return ExprUtil.lessEqual(
                    visit(modelExpr.getLessThanOrEqualTo().getLhsOperand()),
                    visit(modelExpr.getLessThanOrEqualTo().getRhsOperand()));
        }

        if (modelExpr.getEqual() != null) {
            return ExprUtil.equal(
                    visit(modelExpr.getEqual().getLhsOperand()),
                    visit(modelExpr.getEqual().getRhsOperand()));
        }

        if (modelExpr.getGreaterThanOrEqualTo() != null) {
            return ExprUtil.greaterEqual(
                    visit(modelExpr.getGreaterThanOrEqualTo().getLhsOperand()),
                    visit(modelExpr.getGreaterThanOrEqualTo().getRhsOperand()));
        }

        if (modelExpr.getGreaterThan() != null) {
            return ExprUtil.greater(
                    visit(modelExpr.getGreaterThan().getLhsOperand()),
                    visit(modelExpr.getGreaterThan().getRhsOperand()));
        }

        if (modelExpr.getNotEqual() != null) {
            return ExprUtil.notEqual(
                    visit(modelExpr.getNotEqual().getLhsOperand()),
                    visit(modelExpr.getNotEqual().getRhsOperand()));
        }

        if (modelExpr.getAnd() != null) {
            return ExprUtil.and(
                    visit(modelExpr.getAnd().getLhsOperand()),
                    visit(modelExpr.getAnd().getRhsOperand()));
        }

        if (modelExpr.getOr() != null) {
            return ExprUtil.or(
                    visit(modelExpr.getOr().getLhsOperand()),
                    visit(modelExpr.getOr().getRhsOperand()));
        }

        if (modelExpr.getXor() != null) {
            return ExprUtil.xor(
                    visit(modelExpr.getXor().getLhsOperand()),
                    visit(modelExpr.getXor().getRhsOperand()));
        }

        if (modelExpr.getImplies() != null) {
            return ExprUtil.implies(
                    visit(modelExpr.getImplies().getLhsOperand()),
                    visit(modelExpr.getImplies().getRhsOperand()));
        }

        if (modelExpr.getArrow() != null) {
            return ExprUtil.arrow(
                    visit(modelExpr.getArrow().getLhsOperand()),
                    visit(modelExpr.getArrow().getRhsOperand()));
        }

        if (modelExpr.getConcat() != null) {
            throw new UnsupportedOperationException("Error: Concat expressions are not supported!");
        }

        if (modelExpr.getDiese() != null) {
            throw new UnsupportedOperationException("Error: Diese expressions are not supported!");
        }

        if (modelExpr.getNor() != null) {
            throw new UnsupportedOperationException("Error: Nor expressions are not supported!");
        }

        if (modelExpr.getConditionalExpression() != null) {
            return ExprUtil.ite(
                    visit(modelExpr.getConditionalExpression().getCondition()),
                    visit(modelExpr.getConditionalExpression().getThenBranch()),
                    visit(modelExpr.getConditionalExpression().getElseBranch()));
        }

        if (modelExpr.getCall() != null) {
            return ExprUtil.nodeCall(
                    ExprUtil.id(modelExpr.getCall().getNodeId()),
                    modelExpr.getCall().getArgument().stream()
                            .map(expr -> visit(expr))
                            .collect(Collectors.toList()));
        }

        if (modelExpr.getRecordProjection() != null) {
            return ExprUtil.recordAccess(
                    visit(modelExpr.getRecordProjection().getRecordReference()),
                    modelExpr.getRecordProjection().getFieldId());
        }

        if (modelExpr.getArraySelection() != null) {
            throw new UnsupportedOperationException(
                    "Error: Array selection expressions are not supported!");
        }

        if (modelExpr.getMerge() != null) {
            throw new UnsupportedOperationException("Error: Merge expressions are not supported!");
        }

        if (modelExpr.getEvent() != null) {
            throw new UnsupportedOperationException(
                    "Error: Cannot convert Event expressions to Lustre!");
        }

        throw new UnsupportedOperationException("Error: Expression are not supported!");
    }

    /**
     * Translate VDM model contract to Kind 2 Lustre contract.
     *
     * @param modelContract the VDM model contract
     * @return the Kind 2 Lustre contract builder
     */
    private static ContractBuilder visit(Contract modelContract) {
        ContractBuilder cb = new ContractBuilder(modelContract.getName());

        for (NodeParameter inputParam : modelContract.getInputParameter()) {
            if (inputParam.isIsConstant() != null && inputParam.isIsConstant()) {
                cb.createVarInput(inputParam.getName(), visit(inputParam.getDataType()));
            } else {
                cb.createConstInput(inputParam.getName(), visit(inputParam.getDataType()));
            }
        }

        for (NodeParameter outputParam : modelContract.getOutputParameter()) {
            cb.createVarOutput(outputParam.getName(), visit(outputParam.getDataType()));
        }

        cb.setContractBody(visit(modelContract.getSpecification().get(0)));

        return cb;
    }

    /**
     * Translate VDM model node to Kind 2 Lustre imported component.
     *
     * @param modelNode the VDM model node
     * @return the Kind 2 Lustre imported component builder
     */
    private static ImportedComponentBuilder visitImportedComponent(Node modelNode) {
        ImportedComponentBuilder icb = new ImportedComponentBuilder(modelNode.getName());

        for (NodeParameter input : modelNode.getInputParameter()) {
            if (input.isIsConstant() != null && input.isIsConstant()) {
                icb.createConstInput(input.getName(), visit(input.getDataType()));
            } else {
                icb.createVarInput(input.getName(), visit(input.getDataType()));
            }
        }

        for (NodeParameter output : modelNode.getOutputParameter()) {
            icb.createVarOutput(output.getName(), visit(output.getDataType()));
        }

        if (modelNode.getContract() != null) {
            icb.setContractBody(visit(modelNode.getContract()));
        }

        return icb;
    }

    /**
     * Translate VDM model node to Kind 2 Lustre component.
     *
     * @param modelNode the VDM model node
     * @return the Kind 2 Lustre component builder
     */
    private static ComponentBuilder visitComponent(Node modelNode) {
        ComponentBuilder nb = new ComponentBuilder(modelNode.getName());

        for (NodeParameter input : modelNode.getInputParameter()) {
            if (input.isIsConstant() != null && input.isIsConstant()) {
                nb.createConstInput(input.getName(), visit(input.getDataType()));
            } else {
                nb.createVarInput(input.getName(), visit(input.getDataType()));
            }
        }

        for (NodeParameter output : modelNode.getOutputParameter()) {
            nb.createVarOutput(output.getName(), visit(output.getDataType()));
        }

        if (modelNode.getContract() != null) {
            nb.setContractBody(visit(modelNode.getContract()));
        }

        if (modelNode.getBody() != null) {
            for (ConstantDeclaration constDecl : modelNode.getBody().getConstantDeclaration()) {
                if (constDecl.getDataType() != null) {
                    if (constDecl.getDefinition() != null) {
                        nb.createLocalConst(
                                constDecl.getName(),
                                visit(constDecl.getDataType()),
                                visit(constDecl.getDefinition()));
                    } else {
                        nb.createLocalConst(constDecl.getName(), visit(constDecl.getDataType()));
                    }
                } else {
                    nb.createLocalConst(constDecl.getName(), visit(constDecl.getDefinition()));
                }
            }

            for (VariableDeclaration varDecl : modelNode.getBody().getVariableDeclaration()) {
                nb.createLocalVar(varDecl.getName(), visit(varDecl.getDataType()));
            }

            for (NodeEquation equation : modelNode.getBody().getEquation()) {
                nb.addEquation(
                        equation.getLhs().getIdentifier().stream()
                                .map(var -> ExprUtil.id(var))
                                .collect(Collectors.toList()),
                        visit(equation.getRhs()));
            }

            for (Expression assertion : modelNode.getBody().getAssertion()) {
                nb.addAssertion(visit(assertion));
            }

            for (NodeProperty property : modelNode.getBody().getProperty()) {
                nb.addProperty(property.getName(), visit(property.getExpression()));
            }
        }

        return nb;
    }

    /**
     * Translate VDM model contract specification to Kind 2 Lustre contract body.
     *
     * @param modelSpec the VDM model contract spec
     * @return the Kind 2 Lustre contract body builder
     */
    private static ContractBodyBuilder visit(ContractSpec modelSpec) {
        ContractBodyBuilder cbb = new ContractBodyBuilder();

        for (ContractImport contractImport : modelSpec.getImport()) {
            cbb.importContract(
                    contractImport.getContractId(),
                    contractImport.getInputArgument().stream()
                            .map(input -> (IdExpr) visit(input))
                            .collect(Collectors.toList()),
                    contractImport.getOutputArgument().stream()
                            .map(output -> (IdExpr) visit(output))
                            .collect(Collectors.toList()));
        }

        for (SymbolDefinition symbol : modelSpec.getSymbol()) {
            if (symbol.isIsConstant() != null && symbol.isIsConstant()) {
                if (symbol.getDataType() != null) {
                    if (symbol.getDefinition() != null) {
                        cbb.createConstant(
                                symbol.getName(),
                                visit(symbol.getDataType()),
                                visit(symbol.getDefinition()));
                    } else {
                        cbb.createConstant(symbol.getName(), visit(symbol.getDataType()));
                    }
                } else {
                    cbb.createConstant(symbol.getName(), visit(symbol.getDefinition()));
                }
            } else {
                cbb.createVarDef(
                        symbol.getName(),
                        visit(symbol.getDataType()),
                        visit(symbol.getDefinition()));
            }
        }

        for (ContractItem assumption : modelSpec.getWeaklyassume()) {
            cbb.weaklyAssume(assumption.getName(), visit(assumption.getExpression()));
        }

        for (ContractItem assumption : modelSpec.getAssume()) {
            cbb.assume(assumption.getName(), visit(assumption.getExpression()));
        }

        for (ContractItem guarantee : modelSpec.getGuarantee()) {
            cbb.guarantee(guarantee.getName(), visit(guarantee.getExpression()));
        }

        for (ContractMode mode : modelSpec.getMode()) {
            ModeBuilder mb = new ModeBuilder(mode.getName());
            for (ContractItem require : mode.getRequire()) {
                mb.require(require.getName(), visit(require.getExpression()));
            }
            for (ContractItem ensure : mode.getEnsure()) {
                mb.ensure(ensure.getName(), visit(ensure.getExpression()));
            }
            cbb.addMode(mb);
        }

        return cbb;
    }
}
