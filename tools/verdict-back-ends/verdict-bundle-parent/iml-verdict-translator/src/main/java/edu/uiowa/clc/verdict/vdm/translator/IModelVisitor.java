/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

   @author: M. Fareed Arif
*/

package edu.uiowa.clc.verdict.vdm.translator;

import com.utc.utrc.hermes.iml.iml.Addition;
import com.utc.utrc.hermes.iml.iml.ArrayAccess;
import com.utc.utrc.hermes.iml.iml.Assertion;
import com.utc.utrc.hermes.iml.iml.AtomicExpression;
import com.utc.utrc.hermes.iml.iml.CardinalityRestriction;
import com.utc.utrc.hermes.iml.iml.CaseTermExpression;
import com.utc.utrc.hermes.iml.iml.CharLiteral;
import com.utc.utrc.hermes.iml.iml.EnumRestriction;
import com.utc.utrc.hermes.iml.iml.ExpressionTail;
import com.utc.utrc.hermes.iml.iml.FloatNumberLiteral;
import com.utc.utrc.hermes.iml.iml.FolFormula;
import com.utc.utrc.hermes.iml.iml.FunctionType;
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
import com.utc.utrc.hermes.iml.iml.Relation;
import com.utc.utrc.hermes.iml.iml.RelationKind;
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
import com.utc.utrc.hermes.iml.iml.TypeRestriction;
import com.utc.utrc.hermes.iml.iml.TypeWithProperties;
import com.utc.utrc.hermes.iml.services.ImlGrammarAccess.TupleOrExpressionTypeElements;
import java.util.ArrayList;
import java.util.List;

public class IModelVisitor implements IVisitor {

    public ArrayList<Token> iml_tokens = new ArrayList<Token>();

    @Override
    public void visit(Import e) {
        // TODO Auto-generated method stub
        String namespace = e.getImportedNamespace();
        //		System.out.println(namespace);
    }

    @Override
    public void visit(Model e) {
        // TODO Auto-generated method stub
        for (Import imp : e.getImports()) {
            visit(imp);
        }
        for (Symbol symbol : e.getSymbols()) {
            visit(symbol);
        }
    }

    @Override
    public void visit(Symbol e) {
        // Type of Symbols
        if (e instanceof SymbolDeclaration) {
            // 1. SymbolDeclaration
            //			iml_stream.add((FolFormula)e);
            visit((SymbolDeclaration) e);
        } else if (e instanceof NamedType) {
            // 2. NamedType
            visit((NamedType) e);
        } else if (e instanceof Assertion) {
            // 3. Assertion
            visit((Assertion) e);
        }
    }

    @Override
    public void visit(NamedType e) {
        // TODO Auto-generated method stub
        //		System.out.println(" : " + e.getName());

        PropertyList pList = e.getPropertylist();

        List<Relation> relList = e.getRelations();

        TypeRestriction tr = e.getRestriction();

        for (SymbolDeclaration sd : e.getSymbols()) {
            // visit(sd);
            //			System.out.println(sd.getName());
        }
    }

    @Override
    public void visit(Assertion e) {
        // TODO Auto-generated method stub
        System.out.println("***NOT SUPPORTED YET***");
    }

    @Override
    public void visit(SymbolDeclaration e) {
        // TODO Auto-generated method stub
        this.iml_tokens.add(new Token(e));
        //		System.out.println(e.getName());
        ImlType it = e.getType();
        // visit(it);

        FolFormula fml = e.getDefinition();

        //		iml_stream.add(fml);

        if (fml != null) {
            visit(fml);
        }

        //		ImlType imlType = e.getType();
        //
        //		if(imlType != null) {
        //			visit(imlType);
        //		}

        for (NamedType nt : e.getTypeParameter()) {
            visit(nt);
        }

        PropertyList pList = e.getPropertylist();

        if (pList != null) {
            visit(pList);
        }

        //		System.out.println(e);
    }

    @Override
    public void visit(PropertyList e) {
        // TODO Auto-generated method stub
        for (Property p : e.getProperties()) {
            visit(p);
        }
    }

    @Override
    public void visit(Property e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visit(SimpleTypeReference e) {
        // TODO Auto-generated method stub
        NamedType namedType = e.getType();

        visit(namedType);

        for (ImlType imltype : e.getTypeBinding()) {
            visit(imltype);
        }
    }

    @Override
    public void visit(SequenceTerm e) {
        // TODO Auto-generated method stub
        FolFormula st = e.getReturn();
        visit(st);
    }

    @Override
    public void visit(TypeWithProperties e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visit(CardinalityRestriction e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visit(EnumRestriction e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visit(ImlType e) {
        // TODO Auto-generated method stub
        if (e instanceof SimpleTypeReference) {
            visit((SimpleTypeReference) e);
        } else if (e instanceof FunctionType) {
            e = ((FunctionType) e).getRange();
            visit(((SimpleTypeReference) e).getType());
        } else if (e instanceof TupleOrExpressionTypeElements) {
            // 2. TupleOrExpressionType
            // [OptionalTermExpr]
        }
    }

    @Override
    public void visit(FolFormula e) {
        // TODO Auto-generated method stub
        if (e != null) {

            // Binary Formula
            if (e.getOp() != null) {

                FolFormula leftFml = e.getLeft();
                visit(leftFml);

                if ((e.getOp().equals("=>")
                        || e.getOp().equals("<=>")
                        || e.getOp().equals("&&")
                        || e.getOp().equals("||"))) {
                    //						System.out.println(" " + e.getOp());
                }

                FolFormula rightFml = e.getRight();
                visit(rightFml);

            } // Unary Signed Atomic Formula
            else if (e instanceof SignedAtomicFormula) {
                SignedAtomicFormula signedFml = (SignedAtomicFormula) e;
                visit(signedFml);

            } else if (e instanceof AtomicExpression) {
                AtomicExpression ae = (AtomicExpression) e;
                visit(ae);
            } else if (e instanceof TermExpression) {
                visit((TermExpression) e);
            }
        }
    }

    @Override
    public void visit(SignedAtomicFormula e) {
        // TODO Auto-generated method stub
        boolean sign = e.isNeg();
        //		System.out.println("AtomicFormula Signed" + sign);
        visit(e.getLeft());
        //		visit(atmExpr);

    }

    @Override
    public void visit(AtomicExpression e) {
        // TODO Auto-generated method stub

        RelationKind rel = e.getRel();

        // RelationKind.EQ : RelationKind
        // RelationKind.EQ_VALUE : int

        // RelationKind.NEQ : RelationKind
        // RelationKind.NEQ_VALUE : int

        // RelationKind.LEQ : RelationKind
        // RelationKind.LEQ_VALUE : int

        // RelationKind.GEQ : RelationKind
        // RelationKind.GEQ_VALUE : int

        //		System.out.println("REL: " + rel.getName());
        //		System.out.println("REL: " + rel.getValue());
        //		System.out.println("REL: " + rel.getLiteral());

        visit(e.getLeft());
        visit(e.getRight());
        //		TermExpression termExpr = (TermExpression) e;
        //		visit(termExpr);

    }

    @Override
    public void visit(TermExpression e) {

        if (e instanceof SymbolReferenceTerm) {
            visit((SymbolReferenceTerm) e);
        } else if (e instanceof InstanceConstructor) {
            visit((InstanceConstructor) e);
        } else if (e instanceof IteTermExpression) {
            visit((IteTermExpression) e);
        } else if (e instanceof CaseTermExpression) {
            visit((CaseTermExpression) e);
        } else if (e instanceof NumberLiteral) {
            visit((NumberLiteral) e);
        } else if (e instanceof StringLiteral) {
            visit((StringLiteral) e);
        } else if (e instanceof FloatNumberLiteral) {
            visit((FloatNumberLiteral) e);
        } else if (e instanceof CharLiteral) {
            visit((CharLiteral) e);
        } else if (e instanceof TruthValue) {
            visit((TruthValue) e);
        } else if (e instanceof SequenceTerm) {
            visit((SequenceTerm) e);
        } else if (e instanceof TermMemberSelection) {
            visit((TermMemberSelection) e);
        } else if (e instanceof TailedExpression) {
            visit((TailedExpression) e);
        } else if (e instanceof Addition) {
            Addition a = (Addition) e;
            visit(a);
        } else if (e instanceof Multiplication) {
            Multiplication m = (Multiplication) e;
            visit(m);
        }
    }

    @Override
    public void visit(Addition e) {
        // TODO Auto-generated method stub
        String op = e.getOp();
        //		System.out.println(op);

        Multiplication leftTerm = (Multiplication) e.getLeft();
        visit(leftTerm);

        Multiplication rightTerm = (Multiplication) e.getRight();
        visit(rightTerm);
    }

    @Override
    public void visit(Multiplication e) {
        // TODO Auto-generated method stub
        String op = e.getOp();
        //		System.out.println(op);

        if (e instanceof TailedExpression) {
            TailedExpression leftTail = (TailedExpression) e.getLeft();
            visit(leftTail);
            TailedExpression rightTail = (TailedExpression) e.getRight();
            visit(rightTail);
        }
    }

    @Override
    public void visit(TailedExpression e) {
        // TODO Auto-generated method stub
        FolFormula fml = e.getLeft();
        visit(fml);
        ExpressionTail et = e.getTail();
        visit(et);
    }

    @Override
    public void visit(ExpressionTail e) {
        if (e instanceof TupleConstructor) {
            TupleConstructor tc = (TupleConstructor) e;
            visit(tc);
        } else if (e instanceof ArrayAccess) {
            visit((ArrayAccess) e);
        }
    }

    @Override
    public void visit(ArrayAccess e) {
        // TODO Auto-generated method stub
        FolFormula fml = e.getIndex();
        visit(fml);
    }

    @Override
    public void visit(TupleConstructor e) {
        // TODO Auto-generated method stub
        for (FolFormula fml : e.getElements()) {
            visit(fml);
        }
    }

    @Override
    public void visit(TermMemberSelection e) {
        // TODO Auto-generated method stub
        TermExpression r = e.getReceiver();
        visit(r);
        TermExpression m = e.getMember();
        visit(m);
    }

    @Override
    public void visit(SymbolReferenceTerm e) {

        // TODO Auto-generated method stub

        Symbol symbol = e.getSymbol();

        if (symbol instanceof SymbolDeclaration) {
            visit((SymbolDeclaration) symbol);
        }
    }

    @Override
    public void visit(IteTermExpression e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visit(CaseTermExpression e) {
        // TODO Auto-generated method stub
    }

    @Override
    public void visit(OptionalTermExpr e) {
        // TODO Auto-generated method stub
        TermExpression te = e.getTerm();
        visit(te);
    }

    @Override
    public void visit(InstanceConstructor e) {

        SymbolDeclaration sd = e.getRef();
        visit(sd);

        TermExpression te = e.getDefinition();
        visit(te);
    }

    @Override
    public void visit(NumberLiteral e) {
        int v = e.getValue();
        //		System.out.print(" " + v);
        this.iml_tokens.add(new Token(e));
    }

    @Override
    public void visit(FloatNumberLiteral e) {
        float v = e.getValue();
        //		System.out.print(" " + v);
        this.iml_tokens.add(new Token(e));
    }

    public void visit(StringLiteral e) {

        String v = e.getValue();
        //		System.out.println(" " + v);
        this.iml_tokens.add(new Token(e));
    }

    public void visit(TruthValue e) {
        e.isFALSE();
        e.isTRUE();
        this.iml_tokens.add(new Token(e));
    }
    // public abstract void accept(Visitor v);
}
