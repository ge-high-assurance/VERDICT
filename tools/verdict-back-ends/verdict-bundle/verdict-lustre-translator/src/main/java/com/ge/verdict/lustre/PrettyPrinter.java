/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

   @author: M. Fareed Arif
*/

package com.ge.verdict.lustre;

import verdict.vdm.vdm_data.ArrayType;
import verdict.vdm.vdm_data.DataType;
import verdict.vdm.vdm_data.EnumType;
import verdict.vdm.vdm_data.PlainType;
import verdict.vdm.vdm_data.SubrangeType;
import verdict.vdm.vdm_data.TupleType;
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
import verdict.vdm.vdm_lustre.IfThenElse;
import verdict.vdm.vdm_lustre.LustreProgram;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PrettyPrinter {

    private static final String NL = System.getProperty("line.separator");
    private static final String TAB = "  ";

    // private static final String NL = "\n";
    // private static final String TAB = "\t";

    private StringBuilder sb;

    public PrettyPrinter() {
        this.sb = new StringBuilder();
    }

    Map<String, Set<String>> nodeContractsMap = new HashMap<>();

    public void dump(File outFile) {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
            writer.write(sb.toString());
        } catch (IOException exp) {

        }
    }

    public void printProgram(LustreProgram lusProg, File outFile) {
        visit(lusProg);
        dump(outFile);
    }

    // @Override
    public void visit(LustreProgram program) {

        // this.nodeContractsMap = program.nodeContractsMap;
        // List<LustreNode> cNodes = new ArrayList<>();
        //
        // for (LustreEnumType enumType : program.enumTypes) {
        // enumType.accept(this);
        // sb.append(NL);
        // }
        //
        // sb.append(NL);
        //
        // for (int i = 0; i < program.nodes.size(); ++i) {
        // LustreNode node = program.nodes.get(i);
        //
        // if (!nodeContractsMap.containsKey(node.name)) {
        // node.accept(this);
        // sb.append(NL).append(NL);
        // } else {
        // cNodes.add(node);
        // }
        // }
        //
        // sb.append(NL);

        // ContractDeclarataions
        for (Contract contract : program.getContractDeclaration()) {
            visit(contract);
            sb.append(NL);
        }

        sb.append(NL);

        for (Node node : program.getNodeDeclaration()) {
            if (node != null) {
                visit(node);
                sb.append(NL);
            }
        }
        // for (LustreNode node : cNodes) {
        // node.accept(this);
        // sb.append(NL).append(NL);
        // }
    }

    public void visit(Node node) {

        sb.append("node ");

        //        if (node.isIsImported()) {
        sb.append("imported ");
        //        }

        sb.append(node.getName());
        sb.append("(");
        sb.append(NL);

        for (NodeParameter param : node.getInputParameter()) {
            visit(param);
        }

        sb.append(")");
        sb.append(NL);

        sb.append("returns (");
        sb.append(NL);

        for (NodeParameter param : node.getOutputParameter()) {
            visit(param);
        }

        sb.append(");");
        sb.append(NL);

        ContractSpec contractSpec = node.getContract();

        if (contractSpec != null) {
            sb.append("(*@contract ");

            visit(contractSpec);

            sb.append("*)");
            //            sb.append(NL);
        }

        //        if (node.isIsFunction()) {
        NodeBody nodeBody = node.getBody();

        if (nodeBody != null) {
            //            sb.append("{");
            sb.append(NL);
            visit(nodeBody);
            //            sb.append("}");
            sb.append(NL);
        }
        //        }
    }

    public void visit(NodeBody nodeBody) {

        sb.append("let");
        sb.append(NL);

        if (nodeBody.getVariableDeclaration().size() > 0) {
            sb.append("var");
            sb.append(NL);
        }

        for (VariableDeclaration var : nodeBody.getVariableDeclaration()) {
            visit(var);
            sb.append(NL);
            sb.append(";");
        }

        for (ConstantDeclaration constant : nodeBody.getConstantDeclaration()) {
            sb.append("const");

            visit(constant);

            sb.append(NL);
            sb.append(";");
        }

        for (Expression expr : nodeBody.getAssertion()) {
            visit(expr);
        }

        for (NodeEquation neq : nodeBody.getEquation()) {
            visit(neq);
        }

        for (NodeProperty property : nodeBody.getProperty()) {
            visit(property);
        }

        sb.append("tel");
    }

    public void visit(VariableDeclaration var) {

        String var_name = var.getName();
        sb.append(var_name);

        sb.append(" : ");

        DataType dataType = var.getDataType();
        sb.append(dataType);
    }

    public void visit(ConstantDeclaration constant) {

        String var_name = constant.getName();
        sb.append(var_name);

        sb.append(" : ");

        DataType dataType = constant.getDataType();
        sb.append(dataType);
    }

    public void visit(NodeProperty property) {

        sb.append("--%PROPERTY ");
        String property_name = property.getName();
        sb.append(property_name);
        sb.append(TAB);

        Expression expr = property.getExpression();
        visit(expr);

        sb.append(";");
        sb.append(NL);
        //    	visit(expr);
    }

    public void visit(NodeEquation neq) {
        NodeEquationLHS leq = neq.getLhs();
        Expression expr = neq.getRhs();
    }

    //        @Override
    public void visit(IfThenElse expr) {

        sb.append(" (if ");
        visit(expr.getCondition());

        sb.append(" then ");
        visit(expr.getThenBranch());

        sb.append(" else ");
        visit(expr.getElseBranch());

        sb.append(")");
    }

    public void visit(Expression expr) {
        // sb.append(expr.value);
    }

    //	public void visit(RealExpr expr) {
    //		sb.append(expr);
    //	}
    //
    //	public void visit(UnaryExpr expr) {
    //		sb.append("(");
    //		sb.append(expr.op).append(" ");
    //		expr.expr.accept(this);
    //		sb.append(")");
    //	}

    public void visit(BinaryOperation op) {
        sb.append("(");
        visit(op.getLhsOperand());

        visit(op);
        //	 sb.append(" ");
        visit(op.getRhsOperand());
        sb.append(")");
    }

    public void visit(NodeParameter nodeparameter) {
        sb.append(nodeparameter.getName());
        sb.append(" : ");
        sb.append(nodeparameter.getDataType());
        sb.append("; ");
        sb.append(NL);
    }

    // @Override
    public void visit(Contract contract) {

        sb.append("contract ").append(contract.getName());
        sb.append(" (").append(NL);

        for (NodeParameter param : contract.getInputParameter()) {
            visit(param);
        }

        sb.append(")").append(NL);
        sb.append("returns (").append(NL);

        for (NodeParameter param : contract.getInputParameter()) {
            visit(param);
        }
        sb.append(");").append(NL);

        // Specifications
        sb.append("let").append(NL);

        for (ContractSpec contractSpec : contract.getSpecification()) {
            visit(contractSpec);
        }

        sb.append("tel");

        sb.append(NL);
    }

    public void visit(SymbolDefinition symbolDef) {

        String symbol_name = symbolDef.getName();
        sb.append(symbol_name);
        sb.append(TAB);

        DataType dataType = symbolDef.getDataType();
        sb.append(":");
        visit(dataType);

        sb.append(" = ");

        Expression expr = symbolDef.getDefinition();
        // visit(expr);
    }

    public void visit(ContractSpec contractSpec) {

        // Symbols
        for (SymbolDefinition symbolDef : contractSpec.getSymbol()) {
            sb.append(TAB);
            sb.append("var ");

            visit(symbolDef);

            sb.append(";");
            sb.append(NL);
        }

        // Assumuptions
        for (ContractItem contractItem : contractSpec.getAssume()) {
            sb.append(TAB).append("assume ");

            visit(contractItem);

            sb.append(";");
            sb.append(NL);
        }
        sb.append(NL);

        // Guarantees
        for (ContractItem contractItem : contractSpec.getGuarantee()) {
            sb.append(TAB).append("guarantee ");

            visit(contractItem);

            sb.append(";");
            sb.append(NL);
        }
        sb.append(NL);

        // Modes
        for (ContractMode contractMode : contractSpec.getMode()) {
            sb.append(TAB).append("mode ");
            sb.append(TAB).append(" (");
            sb.append(TAB).append(NL);

            visit(contractMode);

            sb.append(";");
            sb.append(NL);
        }

        // Imports
        for (ContractImport contractImport : contractSpec.getImport()) {
            sb.append(TAB);
            sb.append("import ");

            visit(contractImport);

            sb.append(";");
            sb.append(NL);
        }
    }

    public void visit(ContractImport contractImport) {
        sb.append(contractImport.getContractId());

        sb.append("( ");
        for (Expression expr : contractImport.getInputArgument()) {
            // visit(expr);
        }
        sb.append(" ) ");

        sb.append(" returns ");

        sb.append("( ");
        for (Expression expr : contractImport.getOutputArgument()) {
            // visit(expr);
        }
        sb.append(" )");
        sb.append(";");
    }

    public void visit(ContractItem contractItem) {

        sb.append(contractItem.getName());
        sb.append(NL);

        contractItem.getExpression();

        sb.append(NL);
    }

    public void visit(ContractMode contractMode) {

        contractMode.getName();
        sb.append(NL);

        // Ensure
        for (ContractItem contractItem : contractMode.getEnsure()) {
            sb.append(TAB);
            sb.append("require ");

            visit(contractItem);

            sb.append(";");
            sb.append(NL);
        }

        // Require
        for (ContractItem contractItem : contractMode.getRequire()) {
            sb.append(TAB);
            sb.append("ensure ");

            visit(contractItem);

            sb.append(";");
            sb.append(NL);
        }
    }

    public void visit(DataType dataType) {
        // dataType.getArrayType();

        visit(dataType.getPlainType());
    }

    public void visit(PlainType plainType) {
        //        sb.append(plainType.value());
        // PlainType.BOOL;
        // PlainType.REAL;
        // PlainType.INT;

    }

    public void visit(EnumType enumType) {}

    public void visit(SubrangeType subrange) {}

    public void visit(ArrayType arrayType) {}

    public void visit(TupleType tupleType) {}

    public void visit(NodeCall nodeCall) {}

    public void visit(RecordProjection recordProject) {}

    public void visit(ArraySelection arraySelection) {}

    public void visit(MergeOperation mergeOperation) {}

    public void visit(ExpressionList exprList) {}

    public void visit(RecordLiteral recordLit) {}

    // @Override
    // public void visit(MergeExpr mergeExpr) {
    // sb.append("(merge ");
    // mergeExpr.clock.accept(this);
    // sb.append(NL);
    // for(int i = 0; i < mergeExpr.exprs.size(); i++) {
    // sb.append(" ");
    // mergeExpr.exprs.get(i).accept(this);
    // sb.append(NL);
    // }
    // sb.append(")");
    // }

    // @Override
    // public void visit(LustreEq equation) {
    // if(equation.lhs.size() > 1) {
    // sb.append("(");
    // for(int i = 0; i < equation.lhs.size(); i++) {
    // equation.lhs.get(i).accept(this);
    // if(i < equation.lhs.size()-1) {
    // sb.append(", ");
    // }
    // }
    // sb.append(")");
    // } else if(equation.lhs.size() == 1) {
    // equation.lhs.get(0).accept(this);
    // }
    // sb.append(" = ");
    // for(LustreExpr expr : equation.rhs)
    // {
    // expr.accept(this);
    // }
    //// equation.rhs.accept(this);
    // sb.append(";");
    // }
    //
    // @Override
    // public void visit(LustreVar var) {
    // if(var.type instanceof PrimitiveType) {
    // if(((PrimitiveType)var.type).isConst) {
    // sb.append("const");
    // }
    // }
    // sb.append(" ").append(var.name).append(" : ");
    // if(var.type instanceof PrimitiveType) {
    // var.type.accept(this);
    // } else if(var.type instanceof LustreEnumType) {
    // sb.append(((LustreEnumType)var.type).name);
    // } else if(var.type instanceof ArrayType) {
    // var.type.accept(this);
    // }
    // }
    //
    // @Override
    // public void visit(VarIdExpr varId) {
    // sb.append(varId.id);
    // }
    //
    //
    // @Override
    // public void visit(LustreEnumType enumType) {
    // sb.append("type ").append(enumType.name).append(" = enum {");
    // for(int i = 0; i < enumType.values.size(); i++) {
    // sb.append(enumType.values.get(i));
    // if(i != enumType.values.size()-1) {
    // sb.append(", ");
    // }
    // }
    // sb.append("};");
    // }
    //
    // @Override
    // public void visit(TupleExpr tupExpr) {
    // if(tupExpr.elements.size() == 1) {
    // tupExpr.elements.get(0).accept(this);
    // } else {
    // sb.append("(");
    // for(int i = 0; i < tupExpr.elements.size(); i++) {
    // tupExpr.elements.get(i).accept(this);
    // if(i < tupExpr.elements.size()-1) {
    // sb.append(", ");
    // }
    // }
    // sb.append(")");
    // }
    // }
    //
    // @Override
    // public void visit(PrimitiveType type) {
    // sb.append(type.name);
    // }
    //
    // @Override
    // public void visit(ArrayType array) {
    // sb.append(array.type);
    // for(int i = array.dimensions.size()-1; i >=0; --i) {
    // sb.append("^").append(array.dimensions.get(i));
    // }
    // for(int i = array.sDimensions.size()-1; i >=0; --i) {
    // sb.append("^").append(array.sDimensions.get(i));
    // }
    // }
    //
    // @Override
    // public void visit(ArrayExpr arrayExpr) {
    // if(arrayExpr.name != null) {
    // sb.append(arrayExpr.name);
    // } else if(arrayExpr.expr != null){
    // arrayExpr.expr.accept(this);
    // }
    //
    // if(arrayExpr.exprs != null) {
    // visitArrayExpr(0, arrayExpr.intDims, arrayExpr.exprs);
    // } else {
    // for(int intD : arrayExpr.intDims) {
    // sb.append("[");
    // sb.append(intD);
    // sb.append("]");
    // }
    // for(String strD : arrayExpr.stringDims) {
    // sb.append("[");
    // sb.append(strD);
    // sb.append("]");
    // }
    // }
    // }
    //
    // public void visitArrayExpr(int curDim, List<Integer> intDims,
    // List<LustreExpr> exprs) {
    // if(curDim < intDims.size() - 1) {
    // int dim = intDims.get(curDim);
    // sb.append("[");
    // for(int i = 0; i < dim; ++i) {
    // visitArrayExpr(curDim+1, intDims, exprs);
    // if(i != dim-1) {
    // sb.append(", ");
    // }
    // }
    // sb.append("]");
    // } else if(curDim == intDims.size() - 1) {
    // int dim = intDims.get(curDim);
    //
    // sb.append("[");
    // for(int i = 0; i < dim && i < exprs.size(); ++i) {
    // exprs.get(i).accept(this);
    // if(i != dim-1) {
    // sb.append(", ");
    // }
    // }
    // sb.append("]");
    // //Remove the ones that have been printed
    // for(int i = 0; i < dim && i < exprs.size(); ++i) {
    // exprs.remove(0);
    // }
    // }
    // }
    //
    // @Override
    // public void visit(ArrayConst arrConst) {
    // if(arrConst.value != null) {
    // sb.append(arrConst.value);
    // }
    // if(arrConst.exprs.size() > 0) {
    // sb.append("[");
    // for(int i = 0; i < arrConst.exprs.size(); ++i) {
    // arrConst.exprs.get(i).accept(this);
    // if(i != arrConst.exprs.size()-1) {
    // sb.append(", ");
    // }
    // }
    // sb.append("]");
    // }
    // }
    //
    // @Override
    // public void visit(EnumConst enumConst) {
    // sb.append(enumConst.toString());
    // }

    //  @Override
    public String toString() {
        return sb.toString();
    }
}
