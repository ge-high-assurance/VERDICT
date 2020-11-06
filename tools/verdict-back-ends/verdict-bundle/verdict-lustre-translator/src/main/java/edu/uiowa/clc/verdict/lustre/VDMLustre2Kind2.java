package edu.uiowa.clc.verdict.lustre;

import edu.uiowa.kind2.lustre.ComponentBuilder;
import edu.uiowa.kind2.lustre.ContractBodyBuilder;
import edu.uiowa.kind2.lustre.ContractBuilder;
import edu.uiowa.kind2.lustre.Expr;
import edu.uiowa.kind2.lustre.ExprUtil;
import edu.uiowa.kind2.lustre.IdExpr;
import edu.uiowa.kind2.lustre.ImportedComponentBuilder;
import edu.uiowa.kind2.lustre.ModeBuilder;
import edu.uiowa.kind2.lustre.Program;
import edu.uiowa.kind2.lustre.ProgramBuilder;
import edu.uiowa.kind2.lustre.Type;
import edu.uiowa.kind2.lustre.TypeUtil;
import java.math.BigInteger;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import verdict.vdm.vdm_data.DataType;
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

public class VDMLustre2Kind2 {
    /**
     * Translate the VDM-Lustre AST into Kind2-java-api AST.
     *
     * @param vdmModel the VDM-Lustre AST
     * @return the Kind2-java-api AST.
     */
    public static Program translate(Model vdmModel) {
        return visit(vdmModel.getDataflowCode());
    }

    private static Program visit(LustreProgram modelProgram) {
        ProgramBuilder pb = new ProgramBuilder();

        for (TypeDeclaration modelTypeDecl : modelProgram.getTypeDeclaration()) {
            pb.defineType(modelTypeDecl.getName(), visit(modelTypeDecl.getDefinition()));
        }

        for (ConstantDeclaration modelConstDecl : modelProgram.getConstantDeclaration()) {
            if (modelConstDecl.getDataType() != null) {
                pb.createConst(
                        modelConstDecl.getName(),
                        visit(modelConstDecl.getDataType()),
                        visit(modelConstDecl.getDefinition()));
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
                    if (node.getBody().isIsMain() != null && node.getBody().isIsMain()) {
                        pb.setMain(node.getName());
                    }
                    pb.addNode(visitComponent(node));
                }
            }
        }

        return pb.build();
    }

    private static Type visit(DataType definition) {
        if (definition.getPlainType() != null) {
            return TypeUtil.mkNamedType(definition.getPlainType().value());
        }

        if (definition.getUserDefinedType() != null) {
            return TypeUtil.mkNamedType(definition.getUserDefinedType());
        }

        if (definition.getSubrangeType() != null) {
            SubrangeType subrange = definition.getSubrangeType();
            return TypeUtil.mkIntSubrangeType(
                    new BigInteger(subrange.getLowerBound()),
                    new BigInteger(subrange.getUpperBound()));
        }

        if (definition.getArrayType() != null) {
            return TypeUtil.mkArrayType(
                    visit(definition.getArrayType().getDataType()),
                    Integer.parseInt(definition.getArrayType().getDimension()));
        }

        if (definition.getTupleType() != null) {
            return TypeUtil.mkTupleType(
                    definition.getTupleType().getDataType().stream()
                            .map(dt -> visit(dt))
                            .collect(Collectors.toList()));
        }

        if (definition.getEnumType() != null) {
            return TypeUtil.mkEnumType(definition.getEnumType().getEnumValue());
        }

        if (definition.getRecordType() != null) {
            // fields in the map have the same order as the RecordFields in the RecordField
            // list
            Map<String, Type> fields = new TreeMap<>((x, y) -> -1);
            for (RecordField field : definition.getRecordType().getRecordField()) {
                fields.put(field.getName(), visit(field.getType()));
            }
            return TypeUtil.mkRecordType(fields);
        }

        throw new IllegalArgumentException(
                "Datatype should either be: plain, user-defined, subrange, array, tuple, enum, or record type.");
    }

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
            // return
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
            // return
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

        throw new UnsupportedOperationException("Not implemented, yet.");
    }

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

    private static ImportedComponentBuilder visitImportedComponent(Node node) {
        ImportedComponentBuilder icb = new ImportedComponentBuilder(node.getName());

        for (NodeParameter input : node.getInputParameter()) {
            if (input.isIsConstant() != null && input.isIsConstant()) {
                icb.createConstInput(input.getName(), visit(input.getDataType()));
            } else {
                icb.createVarInput(input.getName(), visit(input.getDataType()));
            }
        }

        for (NodeParameter output : node.getOutputParameter()) {
            icb.createVarOutput(output.getName(), visit(output.getDataType()));
        }

        if (node.getContract() != null) {
            icb.setContractBody(visit(node.getContract()));
        }

        return icb;
    }

    private static ComponentBuilder visitComponent(Node node) {
        ComponentBuilder nb = new ComponentBuilder(node.getName());

        for (NodeParameter input : node.getInputParameter()) {
            if (input.isIsConstant() != null && input.isIsConstant()) {
                nb.createConstInput(input.getName(), visit(input.getDataType()));
            } else {
                nb.createVarInput(input.getName(), visit(input.getDataType()));
            }
        }

        for (NodeParameter output : node.getOutputParameter()) {
            nb.createVarOutput(output.getName(), visit(output.getDataType()));
        }

        if (node.getContract() != null) {
            nb.setContractBody(visit(node.getContract()));
        }

        for (ConstantDeclaration constDecl : node.getBody().getConstantDeclaration()) {
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

        for (VariableDeclaration varDecl : node.getBody().getVariableDeclaration()) {
            nb.createLocalVar(varDecl.getName(), visit(varDecl.getDataType()));
        }

        for (NodeEquation equation : node.getBody().getEquation()) {
            nb.addEquation(
                    equation.getLhs().getIdentifier().stream()
                            .map(var -> ExprUtil.id(var))
                            .collect(Collectors.toList()),
                    visit(equation.getRhs()));
        }

        for (Expression assertion : node.getBody().getAssertion()) {
            nb.addAssertion(visit(assertion));
        }

        for (NodeProperty property : node.getBody().getProperty()) {
            nb.addProperty(property.getName(), visit(property.getExpression()));
        }

        return nb;
    }

    private static ContractBodyBuilder visit(ContractSpec contractSpec) {
        ContractBodyBuilder cbb = new ContractBodyBuilder();

        for (ContractImport contractImport : contractSpec.getImport()) {
            cbb.importContract(
                    contractImport.getContractId(),
                    contractImport.getInputArgument().stream()
                            .map(input -> (IdExpr) visit(input))
                            .collect(Collectors.toList()),
                    contractImport.getOutputArgument().stream()
                            .map(output -> (IdExpr) visit(output))
                            .collect(Collectors.toList()));
        }

        for (SymbolDefinition symbol : contractSpec.getSymbol()) {
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

        for (ContractItem assumption : contractSpec.getAssume()) {
            cbb.addAssumption(assumption.getName(), visit(assumption.getExpression()));
        }

        for (ContractItem guarantee : contractSpec.getGuarantee()) {
            cbb.addGuarantee(guarantee.getName(), visit(guarantee.getExpression()));
        }

        for (ContractMode mode : contractSpec.getMode()) {
            ModeBuilder mb = new ModeBuilder(mode.getName());
            for (ContractItem require : mode.getRequire()) {
                mb.addRequire(require.getName(), visit(require.getExpression()));
            }
            for (ContractItem ensure : mode.getEnsure()) {
                mb.addEnsure(ensure.getName(), visit(ensure.getExpression()));
            }
            cbb.addMode(mb);
        }

        return cbb;
    }
}
