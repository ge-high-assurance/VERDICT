(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
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

type component_category =
  | System
  | Data

type subcomponent = {
  name: pid;
  type_ref: qcref option;
  category: component_category;
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

type package_rename = {
  name: pid option;
  renamed_package: pname;
  rename_all: bool;
}

type package_section = {
  imported_units: pname list;
  renamed_packages: package_rename list;
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

val qpref_to_string : qpref -> string

val find_assoc: qpref -> property_association list -> property_association option

val is_component_type: classifier -> bool

val is_system_type: component_type -> bool

val is_aadl_package: model_unit -> bool

val is_agree_annex: aadl_annex -> bool

val is_verdict_annex: aadl_annex -> bool

val is_system: component_category -> bool

val is_data: component_category -> bool

val get_imported_units: model_unit -> pname list

val pp_print_ast_indent: int -> Format.formatter -> t -> unit

val pp_print_ast: Format.formatter -> t -> unit

