/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

   @author: M. Fareed Arif
*/

package edu.uiowa.clc.verdict.vdm.translator;

import com.utc.utrc.hermes.iml.iml.CharLiteral;
import com.utc.utrc.hermes.iml.iml.FloatNumberLiteral;
import com.utc.utrc.hermes.iml.iml.FolFormula;
import com.utc.utrc.hermes.iml.iml.FunctionType;
import com.utc.utrc.hermes.iml.iml.ImlType;
import com.utc.utrc.hermes.iml.iml.NamedType;
import com.utc.utrc.hermes.iml.iml.NumberLiteral;
import com.utc.utrc.hermes.iml.iml.SimpleTypeReference;
import com.utc.utrc.hermes.iml.iml.StringLiteral;
import com.utc.utrc.hermes.iml.iml.Symbol;
import com.utc.utrc.hermes.iml.iml.SymbolDeclaration;
import com.utc.utrc.hermes.iml.iml.SymbolReferenceTerm;
import com.utc.utrc.hermes.iml.iml.TruthValue;

public class Token {

    public Type type;

    public FolFormula value;

    public SymbolDeclaration sd;

    public Token(FolFormula value, Type type) {
        this.value = value;
        this.type = type;
    }

    public Token(FolFormula v) {

        if (v instanceof SymbolReferenceTerm) {

            SymbolReferenceTerm sr_ref = (SymbolReferenceTerm) v;
            Symbol s = sr_ref.getSymbol();
            SymbolDeclaration sd = (SymbolDeclaration) s;

            if (sd instanceof SymbolDeclaration) {
                sd = (SymbolDeclaration) sd;
                ImlType imltype = sd.getType();

                if (imltype instanceof SimpleTypeReference) {
                    SimpleTypeReference si_ref = (SimpleTypeReference) imltype;
                    NamedType nt = si_ref.getType();
                    this.type = Type.get(nt.getName());
                } else if (imltype instanceof FunctionType) {
                    imltype = ((FunctionType) imltype).getRange();
                    NamedType nt = ((SimpleTypeReference) imltype).getType();
                    this.type = Type.get(nt.getName());
                }
            }

            this.sd = sd;
        } else if (v instanceof SymbolDeclaration) {
            SymbolDeclaration sd = (SymbolDeclaration) v;
            ImlType imltype = sd.getType();

            if (imltype instanceof SimpleTypeReference) {
                SimpleTypeReference si_ref = (SimpleTypeReference) imltype;
                NamedType nt = si_ref.getType();
                this.type = Type.get(nt.getName());
            } else if (imltype instanceof FunctionType) {
                imltype = ((FunctionType) imltype).getRange();
                NamedType nt = ((SimpleTypeReference) imltype).getType();
                this.type = Type.get(nt.getName());
            }
        } else if (v instanceof StringLiteral) {
            this.type = Type.valueOf("String");
        } else if (v instanceof CharLiteral) {
            this.type = Type.valueOf("Char");
        } else if (v instanceof NumberLiteral) {
            this.type = Type.valueOf("Int");
        } else if (v instanceof FloatNumberLiteral) {
            this.type = Type.valueOf("Float");
        } else if (v instanceof TruthValue) {
            this.type = Type.valueOf("Boolean");
        }

        this.value = v;
    }

    public Token(SymbolDeclaration sd) {

        ImlType imltype = sd.getType();

        if (imltype instanceof SimpleTypeReference) {
            SimpleTypeReference si_ref = (SimpleTypeReference) imltype;
            NamedType nt = si_ref.getType();
            this.type = Type.get(nt.getName());
        } else if (imltype instanceof FunctionType) {
            imltype = ((FunctionType) imltype).getRange();
            NamedType nt = ((SimpleTypeReference) imltype).getType();
            this.type = Type.get(nt.getName());
        }

        this.sd = sd;
    }

    public Token(NamedType v) {
        this.type = Type.get(v.getName());
    }

    public String toString() {

        String out_value = null;

        if (this.value instanceof SymbolReferenceTerm) {
            SymbolReferenceTerm sr_ref = (SymbolReferenceTerm) this.value;
            Symbol s = sr_ref.getSymbol();
            SymbolDeclaration sd = (SymbolDeclaration) s;

            out_value = String.format("Token(%s, %s)", this.type, sd.getName());
        } else if (this.sd instanceof SymbolDeclaration) {
            out_value = String.format("Token(%s, %s)", this.type, sd.getName());
        } else if (this.type == Type.String) {
            out_value = String.format("Token(%s, %s)", this.type, getStringValue());
        } else if (this.type == Type.Char) {
            out_value = String.format("Token(%s, %s)", this.type, getCharValue());

        } else if (this.type == Type.Int) {
            out_value = String.format("Token(%s, %d)", this.type, getNumberValue());

        } else if (this.type == Type.Float) {
            out_value = String.format("Token(%s, %f)", this.type, getFloatingValue());

        } else if (this.type == Type.Boolean) {
            out_value = String.format("Token(%s, %b)", this.type, getTruthValue());
        }

        return out_value;
    }

    // @TODO:
    // Work & Support
    // sd.getDefinition
    // sd.PropertyList
    // sd.TypeParamerts
    public String getSymbolDeclarationValue() {
        SymbolDeclaration sd = (SymbolDeclaration) this.value;

        return sd.getName();
    }

    public String getStringValue() {

        StringLiteral v = (StringLiteral) this.value;
        String str_val = v.getValue();

        return str_val;
    }

    public int getNumberValue() {

        NumberLiteral v = (NumberLiteral) this.value;
        int num_val = v.getValue();

        return num_val;
    }

    public String getCharValue() {

        CharLiteral v = (CharLiteral) this.value;
        String char_val = v.getValue();

        return char_val;
    }

    public double getFloatingValue() {

        FloatNumberLiteral v = (FloatNumberLiteral) this.value;
        double float_val = v.getValue();

        return float_val;
    }

    public boolean getTruthValue() {

        boolean bool_val = false;

        TruthValue v = (TruthValue) this.value;

        if (v.isTRUE()) bool_val = true;

        return bool_val;
        //		SymbolDeclaration;

    }
}
