/* See LICENSE in project directory */
package edu.uiowa.clc.verdict.vdm.translator;

import com.utc.utrc.hermes.iml.iml.Addition;
import com.utc.utrc.hermes.iml.iml.ArrayAccess;
import com.utc.utrc.hermes.iml.iml.Assertion;
import com.utc.utrc.hermes.iml.iml.AtomicExpression;
import com.utc.utrc.hermes.iml.iml.CardinalityRestriction;
import com.utc.utrc.hermes.iml.iml.CaseTermExpression;
import com.utc.utrc.hermes.iml.iml.EnumRestriction;
import com.utc.utrc.hermes.iml.iml.ExpressionTail;
import com.utc.utrc.hermes.iml.iml.FloatNumberLiteral;
import com.utc.utrc.hermes.iml.iml.FolFormula;
import com.utc.utrc.hermes.iml.iml.ImlType;
import com.utc.utrc.hermes.iml.iml.Import;
import com.utc.utrc.hermes.iml.iml.InstanceConstructor;
import com.utc.utrc.hermes.iml.iml.IteTermExpression;
import com.utc.utrc.hermes.iml.iml.Model;
import com.utc.utrc.hermes.iml.iml.Multiplication;
import com.utc.utrc.hermes.iml.iml.NamedType;
import com.utc.utrc.hermes.iml.iml.NumberLiteral;
import com.utc.utrc.hermes.iml.iml.OptionalTermExpr;
import com.utc.utrc.hermes.iml.iml.Property;
import com.utc.utrc.hermes.iml.iml.PropertyList;
import com.utc.utrc.hermes.iml.iml.SequenceTerm;
import com.utc.utrc.hermes.iml.iml.SignedAtomicFormula;
import com.utc.utrc.hermes.iml.iml.SimpleTypeReference;
import com.utc.utrc.hermes.iml.iml.StringLiteral;
import com.utc.utrc.hermes.iml.iml.Symbol;
import com.utc.utrc.hermes.iml.iml.SymbolDeclaration;
import com.utc.utrc.hermes.iml.iml.SymbolReferenceTerm;
import com.utc.utrc.hermes.iml.iml.TailedExpression;
import com.utc.utrc.hermes.iml.iml.TermExpression;
import com.utc.utrc.hermes.iml.iml.TermMemberSelection;
import com.utc.utrc.hermes.iml.iml.TruthValue;
import com.utc.utrc.hermes.iml.iml.TupleConstructor;
import com.utc.utrc.hermes.iml.iml.TypeWithProperties;

public interface IVisitor {

    public void visit(Import e);

    public void visit(Model e);

    public void visit(NamedType e);

    public void visit(Assertion e);

    public void visit(SymbolDeclaration e);

    public void visit(PropertyList e);

    public void visit(Property e);

    public void visit(SimpleTypeReference e);

    public void visit(SequenceTerm e);

    public void visit(TypeWithProperties e);

    public void visit(CardinalityRestriction e);

    public void visit(EnumRestriction e);

    public void visit(ImlType e);

    public void visit(FolFormula e);

    public void visit(AtomicExpression e);

    public void visit(Addition e);

    public void visit(Multiplication e);

    public void visit(TermMemberSelection e);

    public void visit(SymbolReferenceTerm e);

    public void visit(TailedExpression e);

    public void visit(TupleConstructor e);

    public void visit(SignedAtomicFormula e);

    public void visit(IteTermExpression e);

    public void visit(CaseTermExpression e);

    public void visit(OptionalTermExpr e);

    public void visit(Symbol e);

    public void visit(TermExpression e);

    public void visit(ExpressionTail e);

    public void visit(InstanceConstructor e);

    public void visit(NumberLiteral e);

    public void visit(FloatNumberLiteral e);

    public void visit(StringLiteral e);

    public void visit(TruthValue e);

    public void visit(ArrayAccess e);
}
