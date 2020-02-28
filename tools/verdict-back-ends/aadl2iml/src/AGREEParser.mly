(* Copyright (c) 2019 by the Board of Trustees of the University of Iowa

   Licensed under the Apache License, Version 2.0 (the "License"); you
   may not use this file except in compliance with the License.  You
   may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0 

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
   implied. See the License for the specific language governing
   permissions and limitations under the License. 

*)

(** @author Daniel Larraz *)

%{

module C = CommonAstTypes
module A = AGREEAst

let mk_pos = Position.mk_position

let add_range rg = function
  | A.IntType _ -> A.IntType rg
  | A.RealType _ -> A.RealType rg
  | pt -> pt

%}

%token ASSUME GUARANTEE LEMMA ASSIGN ASSERT
%token NODE RETURNS LET TEL VAR
%token CONST ENUM
%token REAL BOOL INT
%token NOT AND OR EQ
%token IF THEN ELSE PRE
%token ANNEX_BLOCK_END "**}"
%token EQUIV "<=>"
%token ARROW "->"
%token IMPLIES "=>"
%token DOUBLE_COLON "::"
%token LTE "<="
%token GTE ">="
%token NEQ "!="
%token LT "<"
%token GT ">"
%token EQUAL "="
%token INT_DIV MOD
%token TIMES "*"
%token DIV "/"
%token DOT "."
%token COMMA ","
%token COLON ":"
%token SEMICOLON ";"
%token LCURLY_BRACKET "{"
%token RCURLY_BRACKET "}"
%token LSQ_BRACKET "["
%token RSQ_BRACKET "]"
%token LPAREN "("
%token RPAREN ")"
%token PLUS "+"
%token MINUS "-"
%token POWER "^"
%token <string>ID
%token <string>INTEGER_LIT
%token <string>REAL_LIT
%token <string>STRING
%token TRUE FALSE

%nonassoc ELSE
%right "->"
%right "=>"
%left "<=>"
%left OR
%left AND
%left "<" "<=" ">" ">=" "=" "!="
%left "+" "-"
%left "*" "/" INT_DIV MOD
%left "^"
%nonassoc NOT
%left "(" "{"
%left "."

%start<AGREEAst.t> agree_annex

%%

agree_annex: statements = list(spec_statement) "**}"
  {
    statements
  }

spec_statement:
  | nss = named_spec_statement
  {
    A.NamedSpecStatement (mk_pos $startpos, nss)
  }
  | cs = const_statement
  {
    A.ConstStatement (mk_pos $startpos, cs)
  }
  | eqs = eq_statement
  {
    A.EqStatement (mk_pos $startpos, eqs)
  }
  | asg = assign_statement
  {
    A.AssignStatement (mk_pos $startpos, asg)
  }
  | nd = node_def
  {
    A.NodeDefinition (mk_pos $startpos, nd)
  }
  | ass = assert_statement
  {
    A.AssertStatement (mk_pos $startpos, ass)
  }
  (* INCOMPLETE *)

ident: id = ID { (mk_pos $startpos, id) }

pname:
  | pid = ident { [pid] }
  | pid = ident "::" pn = pname { pid :: pn }

node_def:
  NODE pid = ident "(" inputs = separated_list(",", arg) ")" RETURNS
  "(" outputs = separated_list(",", arg) ")" ";"
  body = node_body_expr
  {
    {A.name = pid;
     A.inputs = inputs;
     A.outputs = outputs;
     A.locals = fst body;
     A.equations = snd body;
    }
  }

node_body_expr:
  vars = variable_declarations_opt
  LET
  eqs = nonempty_list(node_statement)
  TEL ";"
  {
    (vars, eqs)
  }

variable_declarations_opt:
  | { [] }
  | vars = variable_declarations { vars }

variable_declarations:
  VAR args = nonempty_list(pair(arg, ";"))
  {
    List.map fst args
  }

node_statement:
  | lhs = separated_nonempty_list(",", ident) "=" rhs = expr ";"
  {
    {A.lhs = lhs;
     A.rhs = rhs;
    }
  }
  (*| LEMMA STRING ":" expr ";" {} *)

named_spec_statement:
  | ASSUME id = option(ident) desc = STRING ":" e = expr_or_pattern_statement ";"
  {
    A.Assume { A.id = id; A.desc = desc; A.spec = e }
  }
  | GUARANTEE id = option(ident) desc = STRING ":" e = expr_or_pattern_statement ";"
  {
    A.Guarantee { A.id = id; A.desc = desc; A.spec = e }
  }
  | LEMMA id = option(ident) desc = STRING ":" e = expr_or_pattern_statement ";"
  {
    A.Lemma { A.id = id; A.desc = desc; A.spec = e }
  }

assert_statement:
  ASSERT  e = expr_or_pattern_statement ";"
  {
    { A.expression = e }
  }

expr_or_pattern_statement:
  | e = expr { e }
  (*| pattern_statement {}*)

eq_statement:
  EQ vars = separated_nonempty_list(",",arg) def = eq_statement_rhs ";"
  {
    {A.vars = vars;
     A.definition = def;
    }
  }

eq_statement_rhs:
  | { None }
  | "=" e = expr { Some e }

assign_statement:
  ASSIGN v = qcpref "=" e = expr ";"
  {
    {A.var = v;
     A.definition = e;
    }
  }

const_statement:
  CONST pid = ident ":" dt = dtype "=" e = expr ";"
  {
    {A.name = pid;
     A.dtype = dt;
     A.definition = e;
    }
  }

arg:
  pid = ident ":" dt = dtype
  {
    (pid, dt)
  }

dtype:
  bt = base_type { bt } (*list(array_size) {}*)

(* array_size:
  "[" INTEGER_LIT "]" {} *)

base_type:
  | pt = prim_type rg = option(range)
  {
    add_range rg pt
  }
  | ddr = double_dot_ref
  {
    A.UserType ddr
  }

minus_opt:
  | { C.NoSign }
  | "-" { C.Negative }

range: "[" lbs = minus_opt lbv = int_or_real_literal "," ubs = minus_opt ubv = int_or_real_literal "]"
  {
    let lower_bound = { C.sign = lbs; C.lit = lbv; C.units = None } in
    let upper_bound = { C.sign = ubs; C.lit = ubv; C.units = None } in
    (lower_bound, upper_bound)
  }

int_or_real_literal:
  | l = INTEGER_LIT { l }
  | l = REAL_LIT { l }

prim_type:
  | INT { A.IntType None }
  | REAL { A.RealType None }
  | BOOL { A. BoolType }

qcpref:
  | pn = pname
  {
    (pn, None)
  }
  | pn = pname "." pid = ident
  {
    (pn, Some pid)
  }

double_dot_ref: qcr = qcpref { qcr } (* aadl2::NamedElement *)

expr:
  | e1 = expr "->" e2 = expr
    { A.BinaryOp (mk_pos $startpos, A.Arrow, e1, e2) }
  | e1 = expr "=>" e2 = expr
    { A.BinaryOp (mk_pos $startpos, A.Impl, e1, e2) }
  | e1 = expr "<=>" e2 = expr
    { A.BinaryOp (mk_pos $startpos, A.Equiv, e1, e2) }
  | e1 = expr OR e2 = expr
    { A.BinaryOp (mk_pos $startpos, A.Or, e1, e2) }
  | e1 = expr AND e2 = expr
    { A.BinaryOp (mk_pos $startpos, A.And, e1, e2) }
  | e1 = expr "<" e2 = expr
    { A.BinaryOp (mk_pos $startpos, A.Lt, e1, e2) }
  | e1 = expr "<=" e2 = expr
    { A.BinaryOp (mk_pos $startpos, A.Lte, e1, e2) }
  | e1 = expr ">" e2 = expr
    { A.BinaryOp (mk_pos $startpos, A.Gt, e1, e2) }
  | e1 = expr ">=" e2 = expr
    { A.BinaryOp (mk_pos $startpos, A.Gte, e1, e2) }
  | e1 = expr "=" e2 = expr
    { A.BinaryOp (mk_pos $startpos, A.Eq, e1, e2) }
  | e1 = expr "!=" e2 = expr
    { A.BinaryOp (mk_pos $startpos, A.Neq, e1, e2) }
  | e1 = expr "+" e2 = expr
    { A.BinaryOp (mk_pos $startpos, A.Plus, e1, e2) }
  | e1 = expr "-" e2 = expr
    { A.BinaryOp (mk_pos $startpos, A.Minus, e1, e2) }
  | e1 = expr "*" e2 = expr
    { A.BinaryOp (mk_pos $startpos, A.Times, e1, e2) }
  | e1 = expr "/" e2 = expr
    { A.BinaryOp (mk_pos $startpos, A.Div, e1, e2) }
  | e1 = expr INT_DIV e2 = expr
    { A.BinaryOp (mk_pos $startpos, A.IntDiv, e1, e2) }
  | e1 = expr MOD e2 = expr
    { A.BinaryOp (mk_pos $startpos, A.Mod, e1, e2) }
  | e1 = expr "^" e2 = expr
    { A.BinaryOp (mk_pos $startpos, A.Exp, e1, e2) }
  | NOT e = expr
    { A.UnaryOp (mk_pos $startpos, A.Not, e) }
  | "-" e = expr
    { A.UnaryOp (mk_pos $startpos, A.UMinus, e) }
  | PRE "(" e = expr ")"
    { A.UnaryOp (mk_pos $startpos, A.Pre, e) }
  | IF e1 = expr THEN e2 = expr ELSE e3 = expr
    { A.Ite (mk_pos $startpos, e1, e2, e3) }
  | e = expr "." pid = ident
    { A.Proj (mk_pos $startpos, e, pid) }
  | id = dcid
    { A.Ident id }
  | ENUM "(" ddr = double_dot_ref "," pid = ident ")"
    { A.EnumValue (mk_pos $startpos, ddr, pid) }
  | l = INTEGER_LIT
    { A.IntegerLit (mk_pos $startpos, l) }
  | n = expr "(" args = separated_list(",", expr) ")"
    { A.Call (mk_pos $startpos, n, args) }
  | r = expr "{" fs = separated_nonempty_list(";", record_field_def) "}"
    { A.RecordExpr (mk_pos $startpos, r, fs) }
  | l = REAL_LIT
    { A.RealLit (mk_pos $startpos, l) }
  | b = boolean_literal
    { b }
  | "(" e = expr ")"
    { e }
  (* INCOMPLETE *)

record_field_def: pid = ident "=" e = expr { (pid, e) }

dcid: pn = pname { pn }

boolean_literal:
  | TRUE { A.True (mk_pos $startpos) }
  | FALSE { A.False (mk_pos $startpos) }

