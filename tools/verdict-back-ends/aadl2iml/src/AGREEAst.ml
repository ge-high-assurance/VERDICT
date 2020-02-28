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

type unary_op = Not | UMinus | Pre

type expr =
  | BinaryOp of Position.t * binary_op * expr * expr
  | UnaryOp of Position.t * unary_op * expr
  | Ite of Position.t * expr * expr * expr
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

let pp_print_range_opt ppf = function
  | None -> ()
  | Some (lb, ub) ->
    Format.fprintf ppf "[%a,%a]"
      pp_print_numeric_literal lb
      pp_print_numeric_literal ub

let pp_print_data_type ppf = function
  | UserType qcr ->
    pp_print_qcref ppf qcr
  | IntType rg ->
    Format.fprintf ppf "int%a" pp_print_range_opt rg
  | RealType rg ->
    Format.fprintf ppf "real%a" pp_print_range_opt rg
  | BoolType ->
    Format.fprintf ppf "bool"

let pp_print_arg ppf (pid, dtype) =
  Format.fprintf ppf "%a: %a"
     pp_print_id pid pp_print_data_type dtype

let rec pp_print_expr ppf = function
  | BinaryOp (_, Arrow, e1, e2) ->
    Format.fprintf ppf "(%a -> %a)"
      pp_print_expr e1 pp_print_expr e2
  | BinaryOp (_, Impl, e1, e2) ->
    Format.fprintf ppf "(%a => %a)"
      pp_print_expr e1 pp_print_expr e2
  | BinaryOp (_, Equiv, e1, e2) ->
    Format.fprintf ppf "(%a <=> %a)"
      pp_print_expr e1 pp_print_expr e2
  | BinaryOp (_, Or, e1, e2) ->
    Format.fprintf ppf "(%a or %a)"
      pp_print_expr e1 pp_print_expr e2
  | BinaryOp (_, And, e1, e2) ->
    Format.fprintf ppf "(%a and %a)"
      pp_print_expr e1 pp_print_expr e2
  | BinaryOp (_, Lt, e1, e2) ->
    Format.fprintf ppf "(%a < %a)"
      pp_print_expr e1 pp_print_expr e2
  | BinaryOp (_, Lte, e1, e2) ->
    Format.fprintf ppf "(%a <= %a)"
      pp_print_expr e1 pp_print_expr e2
  | BinaryOp (_, Gt, e1, e2) ->
    Format.fprintf ppf "(%a > %a)"
      pp_print_expr e1 pp_print_expr e2
  | BinaryOp (_, Gte, e1, e2) ->
    Format.fprintf ppf "(%a >= %a)"
      pp_print_expr e1 pp_print_expr e2
  | BinaryOp (_, Eq, e1, e2) ->
    Format.fprintf ppf "(%a = %a)"
      pp_print_expr e1 pp_print_expr e2
  | BinaryOp (_, Neq, e1, e2) ->
    Format.fprintf ppf "(%a != %a)"
      pp_print_expr e1 pp_print_expr e2
  | BinaryOp (_, Plus, e1, e2) ->
    Format.fprintf ppf "(%a + %a)"
      pp_print_expr e1 pp_print_expr e2
  | BinaryOp (_, Minus, e1, e2) ->
    Format.fprintf ppf "(%a - %a)"
      pp_print_expr e1 pp_print_expr e2
  | BinaryOp (_, Times, e1, e2) ->
    Format.fprintf ppf "(%a * %a)"
      pp_print_expr e1 pp_print_expr e2
  | BinaryOp (_, Div, e1, e2) ->
    Format.fprintf ppf "(%a / %a)"
      pp_print_expr e1 pp_print_expr e2
  | BinaryOp (_, IntDiv, e1, e2) ->
    Format.fprintf ppf "(%a div %a)"
      pp_print_expr e1 pp_print_expr e2
  | BinaryOp (_, Mod, e1, e2) ->
    Format.fprintf ppf "(%a mod %a)"
      pp_print_expr e1 pp_print_expr e2
  | BinaryOp (_, Exp, e1, e2) ->
    Format.fprintf ppf "(%a ^ %a)"
      pp_print_expr e1 pp_print_expr e2
  | UnaryOp (_, Not, e) ->
    Format.fprintf ppf "(not %a)"
      pp_print_expr e
  | UnaryOp (_, UMinus, e) ->
    Format.fprintf ppf "(-%a)"
      pp_print_expr e
  | UnaryOp (_, Pre, e) ->
    Format.fprintf ppf "(pre (%a))"
      pp_print_expr e
  | Ite (_, e1, e2, e3) ->
    Format.fprintf ppf "(if %a then %a else %a)"
      pp_print_expr e1 pp_print_expr e2 pp_print_expr e3
  | Proj (_, e, pid) ->
    Format.fprintf ppf "%a.%a"
      pp_print_expr e pp_print_id pid
  | Ident name ->
    pp_print_pname ppf name
  | EnumValue (_, qcr, pid) ->
    Format.fprintf ppf "(enum (%a, %a))"
      pp_print_qcref qcr pp_print_id pid
  | IntegerLit (_, lit) ->
    Format.fprintf ppf "%s" lit
  | RealLit (_, lit) ->
    Format.fprintf ppf "%s" lit
  | Call (_, e, args) ->
    let pp_sep ppf () = Format.fprintf ppf ", " in
    Format.fprintf ppf "(%a(%a))"
      pp_print_expr e (Format.pp_print_list ~pp_sep pp_print_expr) args
  | RecordExpr (_, e, fields) ->
    let pp_sep ppf () = Format.fprintf ppf "; " in
    Format.fprintf ppf "(%a {%a})"
      pp_print_expr e (Format.pp_print_list ~pp_sep pp_print_field) fields
  | True _ -> Format.fprintf ppf "true"
  | False _ -> Format.fprintf ppf "false"

and pp_print_field ppf (pid, expr) =
  Format.fprintf ppf "%a = %a" pp_print_id pid pp_print_expr expr


let pp_print_id_opt ppf = function
  | None -> ()
  | Some pid -> Format.fprintf ppf "%a " pp_print_id pid

let pp_print_named_spec ppf {id; desc; spec} =
  Format.fprintf ppf "%a\"%s\" : %a"
    pp_print_id_opt id desc pp_print_expr spec

let pp_print_expr_opt ppf = function
  | None -> ()
  | Some e ->
    Format.fprintf ppf " = %a" pp_print_expr e

let pp_print_equation ppf { lhs; rhs } =
  let pp_sep ppf () = Format.fprintf ppf ", " in
  Format.fprintf ppf "%a = %a;"
    (Format.pp_print_list ~pp_sep pp_print_id) lhs
    pp_print_expr rhs

let pp_print_locals ppf = function
  | [] -> ()
  | vars -> (
    let pp_sep ppf () = Format.fprintf ppf ";@," in
    Format.fprintf ppf "var @[<v>%a;@]@,"
      (Format.pp_print_list ~pp_sep pp_print_arg) vars
  )

let pp_print_node_def ind ppf {name; inputs; outputs; locals; equations } =
  let pp_sep ppf () = Format.fprintf ppf ", " in
  Format.fprintf ppf "node %a (%a)@,returns (%a);@,"
    pp_print_id name (Format.pp_print_list ~pp_sep pp_print_arg) inputs
    (Format.pp_print_list ~pp_sep pp_print_arg) outputs;
  let pp_sep ppf () = Format.fprintf ppf "@," in
  Format.fprintf ppf "%a@[<v %d>let@,%a@]@,tel;"
    pp_print_locals locals ind
    (Format.pp_print_list ~pp_sep pp_print_equation) equations

let pp_print_spec_statement ind ppf = function
  | NamedSpecStatement (_, named_spec) -> (
    let name, body =
      match named_spec with
      | Assume body -> "assume", body
      | Guarantee body -> "guarantee", body
      | Lemma body -> "lemma", body
    in
    Format.fprintf ppf "%s %a;" name pp_print_named_spec body
  )
  | ConstStatement (_, {name; dtype; definition}) -> (
    Format.fprintf ppf "const %a: %a = %a;"
      pp_print_id name pp_print_data_type dtype
      pp_print_expr definition
  )
  | EqStatement (_, {vars; definition}) -> (
    let pp_sep ppf () = Format.fprintf ppf ", " in
    Format.fprintf ppf "eq %a%a;"
      (Format.pp_print_list ~pp_sep pp_print_arg) vars
      pp_print_expr_opt definition
  )
  | AssignStatement (_, {var; definition}) -> (
    Format.fprintf ppf "assign %a = %a;"
      pp_print_qcref var pp_print_expr definition
  )
  | NodeDefinition (_, node_def) -> (
    pp_print_node_def ind ppf node_def
  )
  | AssertStatement (_, {expression}) -> (
    Format.fprintf ppf "assert %a;" pp_print_expr expression
  )

let pp_print_agree_annex ind ppf annex =
  Format.pp_print_list (pp_print_spec_statement ind) ppf annex

let pp_print_ast_indent = pp_print_agree_annex

let pp_print_ast = pp_print_ast_indent 4

