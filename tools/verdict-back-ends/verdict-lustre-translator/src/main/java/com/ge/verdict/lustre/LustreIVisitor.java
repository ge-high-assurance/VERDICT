/* See LICENSE in project directory */
package com.ge.verdict.lustre;

import verdict.vdm.vdm_data.DataType;
import verdict.vdm.vdm_lustre.Contract;
import verdict.vdm.vdm_lustre.LustreProgram;
import verdict.vdm.vdm_lustre.Node;
import verdict.vdm.vdm_lustre.NodeParameter;

public interface LustreIVisitor {

    public void visit(LustreProgram program);

    public void visit(Contract contract);

    public void visit(Node node);

    public void visit(NodeParameter nodeparameter);

    public void visit(DataType dataType);
    //    public void visit(IteExpr expr);
    //    public void visit(IntExpr expr);
    //    public void visit(RealExpr expr);
    //    public void visit(UnaryExpr expr);
    //    public void visit(BinaryExpr expr);
    //    public void visit(BooleanExpr expr);
    //    public void visit(NodeCallExpr expr);
    //
    //    public void visit(LustreEq equation);
    //    public void visit(LustreVar var);
    //    public void visit(VarIdExpr varId);
    //    public void visit(MergeExpr mergeExpr);
    //    public void visit(LustreEnumType enumType);
    //
    //    public void visit(TupleExpr aThis);
    //
    //    public void visit(PrimitiveType aThis);
    //
    //    public void visit(ArrayType aThis);
    //
    //    public void visit(ArrayExpr aThis);
    //
    //    public void visit(LustreAutomaton aThis);
    //
    //    public void visit(AutomatonState aThis);
    //
    //    public void visit(AutomatonIteExpr aThis);
    //
    //    public void visit(ArrayConst aThis);
    //
    //    public void visit(EnumConst enumConst);

}
