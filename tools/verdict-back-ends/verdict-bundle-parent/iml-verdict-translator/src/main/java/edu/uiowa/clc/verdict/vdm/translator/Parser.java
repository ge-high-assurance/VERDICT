/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

   @author: M. Fareed Arif 
*/

package edu.uiowa.clc.verdict.vdm.translator;

import java.util.ArrayList;

public class Parser {

    private ArrayList<Token> tokens;
    public Token token;
    //	public Token ahead_token;
    private int pos;

    public Parser(ArrayList<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
        this.token = tokens.get(pos);
        //			this.ahead_token = tokens.get(pos+1);
    }

    public Token next() {

        advance();

        return token;
    }

    public Token peek() {

        if ((this.pos + 1) < tokens.size()) {
            return tokens.get(pos + 1);
        }

        error("Look ahead out of boundError!!!");
        return null;
    }

    public void consume(Type type) {

        if (this.token.type == type || type == Type.get(this.token.sd.getName())) {
            this.token = next();
        } else {
            error("TOKEN EAT Type Error!!!");
        }
    }

    public void consume() {

        this.token = next();
    }

    private void advance() {

        this.pos += 1;

        if (this.pos < tokens.size()) {
            this.token = tokens.get(pos);
        } else if (this.token.type == Type.EOF) {
            error("TOKENS EOF");
        } else {
            error("TOKENS advancing Error!!!");
        }
    }

    public void error(String error_msg) {
        System.err.println(error_msg);
    }
}
