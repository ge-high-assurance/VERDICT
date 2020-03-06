(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
*)

(** @author Daniel Larraz *)

open CommonAstTypes

type range = numeric_literal * numeric_literal

type data_type =
  | UserType of qcref
  | IntType of range option 
  | RealType of range option
  | BoolType

type arg = pid * data_type

type binary_op =
  | Arrow | Impl | Equiv | Or | And | Lt | Lte | Gt | Gte | Eq | Neq
  | Plus | Minus | Times | Div | IntDiv | Mod | Exp

type unary_op = Not | UMinus | Pre | FloorCast | RealCast

type expr =
  | BinaryOp of Position.t * binary_op * expr * expr
  | UnaryOp of Position.t * unary_op * expr
  | Ite of Position.t * expr * expr * expr
  | Prev of Position.t * expr * expr
  | Proj of Position.t * expr * pid
  | Ident of pname
  | EnumValue of Position.t * qcref * pid
  | IntegerLit of Position.t * string 
  | RealLit of Position.t * string
  | Call of Position.t * expr * expr list
  | RecordExpr of Position.t * expr * field list
  | True of Position.t
  | False of Position.t

and field = pid * expr

type eq_statement = {
  vars: arg list;
  definition: expr option; 
}

type named_spec_body = {
  id: pid option;
  desc: string;
  spec: expr;
}

type named_spec_statement =
  | Assume of named_spec_body 
  | Guarantee of named_spec_body
  | Lemma of named_spec_body

type const_statement = {
  name: pid;
  dtype: data_type;
  definition: expr;
}

type equation = {
  lhs: pid list;
  rhs: expr;
}

type node_definition = {
  name: pid;
  inputs: arg list;
  outputs: arg list;
  locals: arg list;
  equations: equation list;
}

type assign_statement = {
  var: qcref;
  definition: expr;
}

type assert_statement = {
  expression: expr;
}

type spec_statement =
  | NamedSpecStatement of Position.t * named_spec_statement
  | ConstStatement of Position.t * const_statement
  | EqStatement of Position.t * eq_statement
  | AssignStatement of Position.t * assign_statement
  | NodeDefinition of Position.t * node_definition
  | AssertStatement of Position.t * assert_statement

type agree_annex = spec_statement list

type t = agree_annex

val pp_print_ast_indent: int -> Format.formatter -> t -> unit

val pp_print_ast: Format.formatter -> t -> unit

