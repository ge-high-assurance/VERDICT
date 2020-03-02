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

type qpref = pid option * pid (* (pid '::')? pid *)

type property_expr =
  | StringLit  of string
  | RealTerm of numeric_literal
  | IntegerTerm of numeric_literal
  | ListTerm of property_expr list
  | BooleanLit of bool
  | LiteralOrReference of qpref

type binding_op = SetBinding | AppendBinding

type property_association = {
  name: qpref;
  bop: binding_op;
  const: bool;
  value: property_expr;
}

type aadl_annex =
  | AGREEAnnex of Position.t * AGREEAst.t
  | VerdictAnnex of Position.t * VerdictAst.t
  | UnsupportedAnnex of Position.t * string

type port_dir = In | Out | InOut

type data_port = {
  name: pid;
  dir: port_dir;
  dtype: qcref option;
  properties: property_association list;
}

type system_type = {
  name: pid;
  ports: data_port list;
  annexes: aadl_annex list;
}

type data_type = {
  name: pid;
  type_extension: qcref option;
  properties: property_association list;
}

type component_type =
  | SystemType of Position.t * system_type
  | DataType of Position.t * data_type

type subcomponent = {
  name: pid;
  type_ref: qcref option;
  properties: property_association list;
}

type connection_direction = Unidirectional | Bidirectional

type connection_end = pid option * pid (* (pid '.')? pid *)

type port_connection = {
  name: pid;
  dir: connection_direction;
  src: connection_end;
  dst: connection_end;
  properties: property_association list;
}

type system_impl = {
  name: pid * pid;
  subcomponents: subcomponent list;
  connections: port_connection list;
  annexes: aadl_annex list;
}

type data_impl = {
  name: pid * pid;
  subcomponents: subcomponent list;
}

type component_impl =
  | SystemImpl of Position.t * system_impl
  | DataImpl of Position.t * data_impl

type classifier =
  | ComponentType of component_type
  | ComponentImpl of component_impl

type package_section = {
  imported_units: pname list;
  classifiers: classifier list;
  annex_libs: aadl_annex list;
}

type aadl_package = {
  name: pname;
  public_sec: package_section option;
  private_sec: package_section option;
}

type property_set = {
  name: pid;
  imported_units: pname list;
}

type model_unit =
  | AADLPackage of Position.t * aadl_package
  | PropertySet of Position.t * property_set

type t = model_unit

val equal_ids: pid -> pid -> bool

val equal_qprefs: qpref -> qpref -> bool

val mk_full_qpref: string -> string -> qpref

val find_assoc: qpref -> property_association list -> property_association option

val is_component_type: classifier -> bool

val is_system_type: component_type -> bool

val is_aadl_package: model_unit -> bool

val is_agree_annex: aadl_annex -> bool

val is_verdict_annex: aadl_annex -> bool

val get_imported_units: model_unit -> pname list

val pp_print_ast_indent: int -> Format.formatter -> t -> unit

val pp_print_ast: Format.formatter -> t -> unit

