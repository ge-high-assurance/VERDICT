/* See LICENSE in project directory */
package com.ge.verdict.lustre;

import org.antlr.v4.runtime.tree.ParseTreeWalker;

import verdict.vdm.vdm_data.ArrayType;
import verdict.vdm.vdm_data.DataType;
import verdict.vdm.vdm_data.EnumType;
import verdict.vdm.vdm_data.PlainType;
import verdict.vdm.vdm_data.RecordField;
import verdict.vdm.vdm_data.RecordType;
import verdict.vdm.vdm_data.SubrangeType;
import verdict.vdm.vdm_data.TupleType;
import verdict.vdm.vdm_data.TypeDeclaration;
import verdict.vdm.vdm_lustre.ArraySelection;
import verdict.vdm.vdm_lustre.BinaryOperation;
import verdict.vdm.vdm_lustre.ConstantDeclaration;
import verdict.vdm.vdm_lustre.Contract;
import verdict.vdm.vdm_lustre.ContractImport;
import verdict.vdm.vdm_lustre.ContractItem;
import verdict.vdm.vdm_lustre.ContractMode;
import verdict.vdm.vdm_lustre.ContractSpec;
import verdict.vdm.vdm_lustre.Expression;
import verdict.vdm.vdm_lustre.ExpressionList;
import verdict.vdm.vdm_lustre.FieldDefinition;
import verdict.vdm.vdm_lustre.IfThenElse;
import verdict.vdm.vdm_lustre.LustreProgram;
import verdict.vdm.vdm_lustre.MergeCase;
import verdict.vdm.vdm_lustre.MergeOperation;
import verdict.vdm.vdm_lustre.Node;
import verdict.vdm.vdm_lustre.NodeBody;
import verdict.vdm.vdm_lustre.NodeCall;
import verdict.vdm.vdm_lustre.NodeEquation;
import verdict.vdm.vdm_lustre.NodeEquationLHS;
import verdict.vdm.vdm_lustre.NodeParameter;
import verdict.vdm.vdm_lustre.NodeProperty;
import verdict.vdm.vdm_lustre.RecordLiteral;
import verdict.vdm.vdm_lustre.RecordProjection;
import verdict.vdm.vdm_lustre.SymbolDefinition;
import verdict.vdm.vdm_lustre.VariableDeclaration;
import verdict.vdm.vdm_model.Model;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Extract a Verdict data model from a Lustre parse tree. */
public class VerdictLustreListener extends LustreBaseListener {

    // Which Lustre file's parse tree we're currently walking.
    private final File source;

    // As we extract parse tree data, we will add it to this model.
    private final Model model;

    // We may need to find a type declaration quickly.
    private final Map<String, TypeDeclaration> typeDeclarations;

    // We may need to find a constant declaration quickly.
    private final Map<String, ConstantDeclaration> constantDeclarations;

    /** Instantiate a new listener for walking a Lustre file's parse tree. */
    public VerdictLustreListener(File inputFile) {
        source = inputFile;
        model = new Model();
        model.setName(inputFile.getName());
        model.setDataflowCode(new LustreProgram());
        typeDeclarations = new HashMap<>();
        constantDeclarations = new HashMap<>();
    }

    /** Instantiate another listener for walking an included Lustre file's parse tree. */
    private VerdictLustreListener(
            File currentFile,
            Model currentModel,
            Map<String, TypeDeclaration> currentTypeDeclarations,
            Map<String, ConstantDeclaration> currentConstantDeclarations) {
        source = currentFile;
        model = currentModel;
        typeDeclarations = currentTypeDeclarations;
        constantDeclarations = currentConstantDeclarations;
    }

    /** Return the model extracted from the parse tree. */
    public Model getModel() {
        return model;
    }

    /** Parse any included files in the parse tree. */
    @Override
    public void enterInclude(LustreParser.IncludeContext ctx) {
        ctx.includeFile =
                new File(source.getParent(), ctx.STRING().getText().replaceAll("^\"|\"$", ""));
        ctx.includeProgramContext = VerdictLustreTranslator.parseFromLustre(ctx.includeFile);
    }

    /** Extract data from an included Lustre file's parse tree. */
    @Override
    public void exitInclude(LustreParser.IncludeContext ctx) {
        VerdictLustreListener anotherExtractor =
                new VerdictLustreListener(
                        ctx.includeFile, model, typeDeclarations, constantDeclarations);
        ParseTreeWalker.DEFAULT.walk(anotherExtractor, ctx.includeProgramContext);
    }

    /** Extract a type declaration. */
    @Override
    public void exitOneTypeDecl(LustreParser.OneTypeDeclContext ctx) {
        TypeDeclaration typeDeclaration = new TypeDeclaration();
        if (ctx.type() != null) {
            typeDeclaration.setDefinition(ctx.type().dataType);
        }
        if (ctx.ID() != null) {
            String name = ctx.ID().getText();
            typeDeclaration.setName(name);
            typeDeclarations.put(name, typeDeclaration);
        }
        LustreProgram program = model.getDataflowCode();
        program.getTypeDeclaration().add(typeDeclaration);
    }

    /** Extract a plain int type. */
    @Override
    public void exitIntType(LustreParser.IntTypeContext ctx) {
        ctx.dataType = new DataType();
        ctx.dataType.setPlainType(PlainType.INT);
    }

    /** Extract a subrange type. */
    @Override
    public void exitSubrangeType(LustreParser.SubrangeTypeContext ctx) {
        SubrangeType subrangeType = new SubrangeType();
        subrangeType.setLowerBound(ctx.bound(0).getText());
        subrangeType.setType("int");
        subrangeType.setUpperBound(ctx.bound(1).getText());
        ctx.dataType = new DataType();
        ctx.dataType.setSubrangeType(subrangeType);
    }

    /** Extract a plain bool type. */
    @Override
    public void exitBoolType(LustreParser.BoolTypeContext ctx) {
        ctx.dataType = new DataType();
        ctx.dataType.setPlainType(PlainType.BOOL);
    }

    /** Extract a plain real type. */
    @Override
    public void exitRealType(LustreParser.RealTypeContext ctx) {
        ctx.dataType = new DataType();
        ctx.dataType.setPlainType(PlainType.REAL);
    }

    /** Extract an array type. */
    @Override
    public void exitArrayType(LustreParser.ArrayTypeContext ctx) {
        ArrayType arrayType = new ArrayType();
        arrayType.setDataType(ctx.type().dataType);
        arrayType.setDimension(ctx.expr().getText());
        ctx.dataType = new DataType();
        ctx.dataType.setArrayType(arrayType);
    }

    /** Extract a tuple type. */
    @Override
    public void exitTupleType(LustreParser.TupleTypeContext ctx) {
        TupleType tupleType = new TupleType();
        ctx.type().forEach(type -> tupleType.getDataType().add(type.dataType));
        ctx.dataType = new DataType();
        ctx.dataType.setTupleType(tupleType);
    }

    /** Extract a record type. */
    @Override
    public void exitRecordType(LustreParser.RecordTypeContext ctx) {
        RecordType recordType = new RecordType();
        ctx.field()
                .forEach(
                        field -> {
                            field.ID()
                                    .forEach(
                                            id -> {
                                                RecordField recordField = new RecordField();
                                                String name = id.getText();
                                                recordField.setName(name);
                                                recordField.setType(field.type().dataType);
                                                recordType.getRecordField().add(recordField);
                                            });
                        });
        ctx.dataType = new DataType();
        ctx.dataType.setRecordType(recordType);
    }

    /** Extract an enum type. */
    @Override
    public void exitEnumType(LustreParser.EnumTypeContext ctx) {
        EnumType enumType = new EnumType();
        ctx.ID().forEach(id -> enumType.getEnumValue().add(id.getText()));
        ctx.dataType = new DataType();
        ctx.dataType.setEnumType(enumType);
    }

    /** Extract an user defined type. */
    @Override
    public void exitUserType(LustreParser.UserTypeContext ctx) {
        ctx.dataType = new DataType();
        if (ctx.identifier() != null) {
            String name = ctx.identifier().getText();
            ctx.dataType.setUserDefinedType(name);
        }
    }

    /** Extract a bool literal. */
    @Override
    public void exitBoolExpr(LustreParser.BoolExprContext ctx) {
        ctx.expression = new Expression();
        ctx.expression.setBoolLiteral(Boolean.valueOf(ctx.BOOL().getText()));
    }

    /** Extract an int literal. */
    @Override
    public void exitIntExpr(LustreParser.IntExprContext ctx) {
        ctx.expression = new Expression();
        ctx.expression.setIntLiteral(new BigInteger(ctx.INT().getText()));
    }

    /** Extract a real literal. */
    @Override
    public void exitRealExpr(LustreParser.RealExprContext ctx) {
        ctx.expression = new Expression();
        ctx.expression.setRealLiteral(new BigDecimal(ctx.REAL().getText()));
    }

    /** Extract an identifier. */
    @Override
    public void exitIdExpr(LustreParser.IdExprContext ctx) {
        ctx.expression = new Expression();
        ctx.expression.setIdentifier(ctx.identifier().getText());
    }

    /** Extract an unary expression. */
    @Override
    public void exitUnaryExpr(LustreParser.UnaryExprContext ctx) {
        ctx.expression = new Expression();
        switch (ctx.op.getText()) {
            case "-":
                ctx.expression.setNegative(ctx.expr().expression);
                break;
            case "pre":
                ctx.expression.setPre(ctx.expr().expression);
                break;
            case "current":
                ctx.expression.setCurrent(ctx.expr().expression);
                break;
            case "not":
                ctx.expression.setNot(ctx.expr().expression);
                break;
        }
    }

    /** Extract a toInt or toReal expression. */
    @Override
    public void exitCastExpr(LustreParser.CastExprContext ctx) {
        ctx.expression = new Expression();
        if ("real".equals(ctx.op.getText())) {
            ctx.expression.setToReal(ctx.expr().expression);
        } else {
            // We know op must be "int"
            ctx.expression.setToInt(ctx.expr().expression);
        }
    }

    /** Extract a binary operation. */
    @Override
    public void exitBinaryExpr(LustreParser.BinaryExprContext ctx) {
        BinaryOperation binaryOperation = new BinaryOperation();
        binaryOperation.setLhsOperand(ctx.expr(0).expression);
        binaryOperation.setRhsOperand(ctx.expr(1).expression);
        ctx.expression = new Expression();
        switch (ctx.op.getText()) {
            case "^":
                ctx.expression.setCartesianExpression(binaryOperation);
                break;
            case "when":
                ctx.expression.setWhen(binaryOperation);
                break;
            case "*":
                ctx.expression.setTimes(binaryOperation);
                break;
            case "/":
                ctx.expression.setDiv(binaryOperation);
                break;
            case "%":
            case "mod":
                ctx.expression.setMod(binaryOperation);
                break;
            case "div":
                ctx.expression.setIntDiv(binaryOperation);
                break;
            case "+":
                ctx.expression.setPlus(binaryOperation);
                break;
            case "-":
                ctx.expression.setMinus(binaryOperation);
                break;
            case "<":
                ctx.expression.setLessThan(binaryOperation);
                break;
            case "<=":
                ctx.expression.setLessThanOrEqualTo(binaryOperation);
                break;
            case "=":
                ctx.expression.setEqual(binaryOperation);
                break;
            case ">=":
                ctx.expression.setGreaterThanOrEqualTo(binaryOperation);
                break;
            case ">":
                ctx.expression.setGreaterThan(binaryOperation);
                break;
            case "<>":
                ctx.expression.setNotEqual(binaryOperation);
                break;
            case "and":
                ctx.expression.setAnd(binaryOperation);
                break;
            case "or":
                ctx.expression.setOr(binaryOperation);
                break;
            case "xor":
                ctx.expression.setXor(binaryOperation);
                break;
            case "=>":
                ctx.expression.setImplies(binaryOperation);
                break;
            case "->":
            case "fby":
                ctx.expression.setArrow(binaryOperation);
                break;
            case "|":
                ctx.expression.setConcat(binaryOperation);
                break;
        }
    }

    /** Extract a conditional expression. */
    @Override
    public void exitIfThenElseExpr(LustreParser.IfThenElseExprContext ctx) {
        IfThenElse conditional = new IfThenElse();
        conditional.setCondition(ctx.expr(0).expression);
        conditional.setThenBranch(ctx.expr(1).expression);
        conditional.setElseBranch(ctx.expr(2).expression);
        ctx.expression = new Expression();
        ctx.expression.setConditionalExpression(conditional);
    }

    /** Extract a nary expression. */
    @Override
    public void exitNaryExpr(LustreParser.NaryExprContext ctx) {
        ExpressionList expressionList = new ExpressionList();
        ctx.expr().forEach(e -> expressionList.getExpression().add(e.expression));
        ctx.expression = new Expression();
        switch (ctx.op.getText()) {
            case "#":
                ctx.expression.setDiese(expressionList);
                break;
            case "nor":
                ctx.expression.setNor(expressionList);
                break;
        }
    }

    /** Extract a node call. */
    @Override
    public void exitCallExpr(LustreParser.CallExprContext ctx) {
        NodeCall nodeCall = new NodeCall();
        ctx.expr().forEach(e -> nodeCall.getArgument().add(e.expression));
        nodeCall.setNodeId(ctx.userOp().getText());
        ctx.expression = new Expression();
        ctx.expression.setCall(nodeCall);
    }

    /** Extract an array expression. */
    @Override
    public void exitArrayExpr(LustreParser.ArrayExprContext ctx) {
        ExpressionList expressionList = new ExpressionList();
        ctx.expr().forEach(e -> expressionList.getExpression().add(e.expression));
        ctx.expression = new Expression();
        ctx.expression.setArrayExpression(expressionList);
    }

    /** Extract an array selection expression. */
    @Override
    public void exitArraySelectionExpr(LustreParser.ArraySelectionExprContext ctx) {
        ArraySelection arraySelection = new ArraySelection();
        if (ctx.array != null) {
            arraySelection.setArray(ctx.array.expression);
        }
        if (ctx.selector != null) {
            arraySelection.setSelector(ctx.selector.expression);
        }
        if (ctx.trancheEnd != null) {
            arraySelection.setTrancheEnd(ctx.trancheEnd.expression);
        }
        if (ctx.sliceStep != null) {
            arraySelection.setSliceStep(ctx.sliceStep.expression);
        }
        ctx.expression = new Expression();
        ctx.expression.setArraySelection(arraySelection);
    }

    /** Extract a record literal. */
    @Override
    public void exitRecordExpr(LustreParser.RecordExprContext ctx) {
        RecordLiteral recordLiteral = new RecordLiteral();
        recordLiteral.setRecordType(ctx.ID().getText());
        ctx.fieldExpr().forEach(f -> recordLiteral.getFieldDefinition().add(f.fieldDefinition));
        ctx.expression = new Expression();
        ctx.expression.setRecordLiteral(recordLiteral);
    }

    /** Extract a record projection expression. */
    @Override
    public void exitRecordProjectionExpr(LustreParser.RecordProjectionExprContext ctx) {
        RecordProjection recordProjection = new RecordProjection();
        if (ctx.ID() != null) {
            recordProjection.setFieldId(ctx.ID().getText());
        }
        if (ctx.expr() != null) {
            recordProjection.setRecordReference(ctx.expr().expression);
            String identifier = ctx.expr().expression.getIdentifier();
            if (identifier != null) {
                ConstantDeclaration constantDeclaration = constantDeclarations.get(identifier);
                if (constantDeclaration != null) {
                    Expression definition = constantDeclaration.getDefinition();
                    if (definition != null) {
                        RecordLiteral recordLiteral = definition.getRecordLiteral();
                        if (recordLiteral != null) {
                            recordProjection.setRecordType(recordLiteral.getRecordType());
                        }
                    }
                }
            }
        }
        ctx.expression = new Expression();
        ctx.expression.setRecordProjection(recordProjection);
    }

    /** Extract a list expression. */
    @Override
    public void exitListExpr(LustreParser.ListExprContext ctx) {
        ExpressionList expressionList = new ExpressionList();
        ctx.expr().forEach(e -> expressionList.getExpression().add(e.expression));
        ctx.expression = new Expression();
        ctx.expression.setExpressionList(expressionList);
    }

    /** Extract a tuple expression. */
    @Override
    public void exitTupleExpr(LustreParser.TupleExprContext ctx) {
        ExpressionList expressionList = new ExpressionList();
        ctx.expr().forEach(e -> expressionList.getExpression().add(e.expression));
        ctx.expression = new Expression();
        ctx.expression.setTupleExpression(expressionList);
    }

    /** Extract a merge expression. */
    @Override
    public void exitMergeExpr(LustreParser.MergeExprContext ctx) {
        MergeOperation mergeOperation = new MergeOperation();
        mergeOperation.setClock(ctx.ID().getText());
        ctx.mergeCase()
                .forEach(
                        m -> {
                            MergeCase mergeCase = new MergeCase();
                            if (m.identifier() != null) {
                                mergeCase.setCase(m.identifier().getText());
                            } else {
                                mergeCase.setCase(m.BOOL().getText());
                            }
                            mergeCase.setExpr(m.expr().expression);
                            mergeOperation.getMergeCase().add(mergeCase);
                        });
        ctx.expression = new Expression();
        ctx.expression.setMerge(mergeOperation);
    }

    /** Extract a field definition. */
    @Override
    public void exitFieldExpr(LustreParser.FieldExprContext ctx) {
        ctx.fieldDefinition = new FieldDefinition();
        if (ctx.ID() != null) {
            ctx.fieldDefinition.setFieldIdentifier(ctx.ID().getText());
        }
        if (ctx.expr() != null) {
            ctx.fieldDefinition.setFieldValue(ctx.expr().expression);
        }
    }

    /** Extract a constant declaration. */
    @Override
    public void exitOneConstDecl(LustreParser.OneConstDeclContext ctx) {
        LustreProgram program = model.getDataflowCode();
        ctx.ID()
                .forEach(
                        id -> {
                            ConstantDeclaration constantDeclaration = new ConstantDeclaration();
                            if (ctx.type() != null) {
                                constantDeclaration.setDataType(ctx.type().dataType);
                            }
                            if (ctx.expr() != null) {
                                constantDeclaration.setDefinition(ctx.expr().expression);
                            }
                            String name = id.getText();
                            constantDeclaration.setName(name);
                            constantDeclarations.put(name, constantDeclaration);
                            program.getConstantDeclaration().add(constantDeclaration);
                        });
    }

    /** Extract a node declaration. */
    @Override
    public void exitNode(LustreParser.NodeContext ctx) {
        Node node = new Node();
        if ("function".equals(ctx.nodeType.getText())) {
            node.setIsFunction(true);
        }
        node.setName(ctx.ID().getText());
        if (ctx.input != null) {
            node.getInputParameter().addAll(ctx.input.nodeParameters);
        }
        if (ctx.output != null) {
            node.getOutputParameter().addAll(ctx.output.nodeParameters);
        }
        if (ctx.nodeBody() != null) {
            node.setBody(ctx.nodeBody().body);
        }
        if (ctx.inlineContract() != null) {
            node.setContract(ctx.inlineContract().spec);
        }
        if (!ctx.importedContract().isEmpty()) {
            if (node.getContract() == null) {
                node.setContract(new ContractSpec());
            }
            final ContractSpec spec = node.getContract();
            ctx.importedContract()
                    .forEach(importedContract -> spec.getImport().add(importedContract.imprt));
        }

        LustreProgram program = model.getDataflowCode();
        program.getNodeDeclaration().add(node);
    }

    /** Collect and flatten groups of node parameters. */
    @Override
    public void exitParamList(LustreParser.ParamListContext ctx) {
        ctx.nodeParameters = new ArrayList<>();
        ctx.paramGroup().forEach(group -> ctx.nodeParameters.addAll(group.nodeParameters));
    }

    /** Extract a group of node parameters. */
    @Override
    public void exitParamGroup(LustreParser.ParamGroupContext ctx) {
        ctx.nodeParameters = new ArrayList<>();
        ctx.ID()
                .forEach(
                        id -> {
                            NodeParameter nodeParameter = new NodeParameter();
                            nodeParameter.setName(id.getText());
                            if (ctx.type() != null) {
                                nodeParameter.setDataType(ctx.type().dataType);
                            }
                            if (ctx.isConst != null) {
                                nodeParameter.setIsConstant(true);
                            }
                            ctx.nodeParameters.add(nodeParameter);
                        });
    }

    /** Extract an inline contract spec. */
    @Override
    public void exitInlineContract(LustreParser.InlineContractContext ctx) {
        ctx.spec = new ContractSpec();
        ctx.symbol().forEach(symbol -> ctx.spec.getSymbol().add(symbol.def));
        ctx.assume().forEach(assume -> ctx.spec.getAssume().add(assume.item));
        ctx.guarantee().forEach(guarantee -> ctx.spec.getGuarantee().add(guarantee.item));
        ctx.contractMode().forEach(contractMode -> ctx.spec.getMode().add(contractMode.mode));
        ctx.contractImport()
                .forEach(contractImport -> ctx.spec.getImport().add(contractImport.imprt));
    }

    /** Extract a symbol definition. */
    @Override
    public void exitSymbol(LustreParser.SymbolContext ctx) {
        ctx.def = new SymbolDefinition();
        if ("const".equals(ctx.keyword.getText())) {
            ctx.def.setIsConstant(true);
        }
        ctx.def.setName(ctx.ID().getText());
        if (ctx.type() != null) {
            ctx.def.setDataType(ctx.type().dataType);
        }
        if (ctx.expr() != null) {
            ctx.def.setDefinition(ctx.expr().expression);
        }
    }

    /** Extract an assume contract item. */
    @Override
    public void exitAssume(LustreParser.AssumeContext ctx) {
        ctx.item = new ContractItem();
        ctx.item.setExpression(ctx.expr().expression);
    }

    /** Extract a guarantee contract item. */
    @Override
    public void exitGuarantee(LustreParser.GuaranteeContext ctx) {
        ctx.item = new ContractItem();
        if (ctx.STRING() != null) {
            ctx.item.setName(ctx.STRING().getText());
        }
        ctx.item.setExpression(ctx.expr().expression);
    }

    /** Extract a contract mode. */
    @Override
    public void exitContractMode(LustreParser.ContractModeContext ctx) {
        ctx.mode = new ContractMode();
        ctx.mode.setName(ctx.ID().getText());
        ctx.require.forEach(
                require -> {
                    ContractItem contractItem = new ContractItem();
                    contractItem.setExpression(require.expression);
                    ctx.mode.getRequire().add(contractItem);
                });
        ctx.ensure.forEach(
                ensure -> {
                    ContractItem contractItem = new ContractItem();
                    contractItem.setExpression(ensure.expression);
                    ctx.mode.getEnsure().add(contractItem);
                });
    }

    /** Extract a contract import. */
    @Override
    public void exitContractImport(LustreParser.ContractImportContext ctx) {
        ctx.imprt = new ContractImport();
        ctx.imprt.setContractId(ctx.ID().getText());
        ctx.inputArg.forEach(inputArg -> ctx.imprt.getInputArgument().add(inputArg.expression));
        ctx.outputArg.forEach(outputArg -> ctx.imprt.getOutputArgument().add(outputArg.expression));
    }

    /** Extract an imported contract. */
    @Override
    public void exitImportedContract(LustreParser.ImportedContractContext ctx) {
        ctx.imprt = new ContractImport();
        ctx.imprt.setContractId(ctx.ID().getText());
        ctx.inputArg.forEach(inputArg -> ctx.imprt.getInputArgument().add(inputArg.expression));
        ctx.outputArg.forEach(outputArg -> ctx.imprt.getOutputArgument().add(outputArg.expression));
    }

    /** Extract a node body. */
    @Override
    public void exitNodeBody(LustreParser.NodeBodyContext ctx) {
        ctx.body = new NodeBody();
        ctx.localDecl()
                .forEach(
                        local -> {
                            ctx.body.getConstantDeclaration().addAll(local.constantDeclarations);
                            ctx.body.getVariableDeclaration().addAll(local.variableDeclarations);
                        });
        ctx.definition()
                .forEach(
                        definition -> {
                            if (definition.equation() != null) {
                                ctx.body.getEquation().add(definition.equation().equa);
                            }
                            if (definition.assertion() != null) {
                                ctx.body
                                        .getAssertion()
                                        .add(definition.assertion().expr().expression);
                            }
                            if (definition.property() != null) {
                                ctx.body.getProperty().add(definition.property().prop);
                            }
                            if (definition.main() != null) {
                                ctx.body.setIsMain(true);
                            }
                        });
    }

    /** Extract local variable or constant declarations. */
    @Override
    public void exitLocalDecl(LustreParser.LocalDeclContext ctx) {
        ctx.variableDeclarations = new ArrayList<>();
        ctx.constantDeclarations = new ArrayList<>();
        if (ctx.localVarDeclList() != null) {
            ctx.localVarDeclList()
                    .forEach(local -> ctx.variableDeclarations.addAll(local.variableDeclarations));
        }
        if (ctx.localConstDecl() != null) {
            ctx.localConstDecl()
                    .forEach(local -> ctx.constantDeclarations.add(local.constantDeclaration));
        }
    }

    /** Extract a list of local variable declarations. */
    @Override
    public void exitLocalVarDeclList(LustreParser.LocalVarDeclListContext ctx) {
        ctx.variableDeclarations = new ArrayList<>();
        ctx.ID()
                .forEach(
                        id -> {
                            VariableDeclaration variableDeclaration = new VariableDeclaration();
                            String name = id.getText();
                            variableDeclaration.setName(name);
                            if (ctx.type() != null) {
                                variableDeclaration.setDataType(ctx.type().dataType);
                            }
                            ctx.variableDeclarations.add(variableDeclaration);
                        });
    }

    /** Extract a local constant declaration. */
    @Override
    public void exitLocalConstDecl(LustreParser.LocalConstDeclContext ctx) {
        ctx.constantDeclaration = new ConstantDeclaration();
        String name = ctx.ID().getText();
        ctx.constantDeclaration.setName(name);
        if (ctx.type() != null) {
            ctx.constantDeclaration.setDataType(ctx.type().dataType);
        }
        if (ctx.expr() != null) {
            ctx.constantDeclaration.setDefinition(ctx.expr().expression);
        }
    }

    /** Extract an imported node declaration. */
    @Override
    public void exitImportedNode(LustreParser.ImportedNodeContext ctx) {
        Node node = new Node();
        node.setIsImported(true);
        if ("function".equals(ctx.nodeType.getText())) {
            node.setIsFunction(true);
        }
        node.setName(ctx.ID().getText());
        if (ctx.input != null) {
            node.getInputParameter().addAll(ctx.input.nodeParameters);
        }
        if (ctx.output != null) {
            node.getOutputParameter().addAll(ctx.output.nodeParameters);
        }
        if (ctx.inlineContract() != null) {
            node.setContract(ctx.inlineContract().spec);
        }

        LustreProgram program = model.getDataflowCode();
        program.getNodeDeclaration().add(node);
    }

    /** Extract a contract. */
    @Override
    public void exitContractNode(LustreParser.ContractNodeContext ctx) {
        Contract contract = new Contract();
        contract.setName(ctx.ID().getText());
        if (ctx.input != null) {
            contract.getInputParameter().addAll(ctx.input.nodeParameters);
        }
        if (ctx.output != null) {
            contract.getOutputParameter().addAll(ctx.output.nodeParameters);
        }
        contract.getSpecification().add(ctx.contractSpec().spec);

        LustreProgram program = model.getDataflowCode();
        program.getContractDeclaration().add(contract);
    }

    /** Extract a contract spec. */
    @Override
    public void exitContractSpec(LustreParser.ContractSpecContext ctx) {
        ctx.spec = new ContractSpec();
        ctx.symbol().forEach(symbol -> ctx.spec.getSymbol().add(symbol.def));
        ctx.assume().forEach(assume -> ctx.spec.getAssume().add(assume.item));
        ctx.guarantee().forEach(guarantee -> ctx.spec.getGuarantee().add(guarantee.item));
        ctx.contractMode().forEach(contractMode -> ctx.spec.getMode().add(contractMode.mode));
        ctx.contractImport()
                .forEach(contractImport -> ctx.spec.getImport().add(contractImport.imprt));
    }

    /** Extract an equation. */
    @Override
    public void exitEquation(LustreParser.EquationContext ctx) {
        ctx.equa = new NodeEquation();
        if (ctx.leftList() != null) {
            ctx.equa.setLhs(ctx.leftList().lhs);
        }
        if (ctx.expr() != null) {
            ctx.equa.setRhs(ctx.expr().expression);
        }
    }

    /** Extract an equation's left hand side. */
    @Override
    public void exitLeftList(LustreParser.LeftListContext ctx) {
        ctx.lhs = new NodeEquationLHS();
        ctx.left().forEach(left -> ctx.lhs.getIdentifier().add(left.getText()));
    }

    /** Extract a property. */
    @Override
    public void exitProperty(LustreParser.PropertyContext ctx) {
        ctx.prop = new NodeProperty();
        if (ctx.expr() != null) {
            ctx.prop.setExpression(ctx.expr().expression);
        } else if (ctx.equation() != null) {
            ctx.prop.setExpression(ctx.equation().equa.getRhs());
        }
        if (ctx.STRING() != null) {
            ctx.prop.setName(ctx.STRING().getText());
        }
    }
}
