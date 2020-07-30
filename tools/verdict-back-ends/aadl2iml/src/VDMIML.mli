(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   Copyright (c) 2019-2020, General Electric Company.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
*)

(** @author Daniel Larraz
    @author William D. Smith
*)

type identifier = string

type plain_type = Int | Real | Bool

type integer = string

type type_decl_ref = int

type data_type =
  | PlainType of plain_type
  | SubrangeType of integer * integer
  | EnumType of identifier list
  | RecordType of (identifier * data_type) list
  | UserDefinedType of type_decl_ref

type type_declaration = {
  name: identifier;
  definition: data_type option;
  parent: type_decl_ref option;
}

type port_mode = In | Out

type port = {
  name: identifier;
  mode: port_mode;
  is_event: bool;
  ptype: data_type option;
  probe: bool;
}

type binary_op =
  | Arrow | Impl | Equiv | Or | And | Lt | Lte | Gt | Gte | Eq | Neq
  | Plus | Minus | Times | Div | IntDiv | Mod

type unary_op = Not | UMinus | Pre | ToInt | ToReal | Event

type expr =
  | BinaryOp of binary_op * expr * expr
  | UnaryOp of unary_op * expr
  | Ite of expr * expr * expr
  | Proj of expr * identifier
  | Ident of identifier
  | IntegerLit of string
  | RealLit of string
  | Call of identifier * expr list
  | RecordExpr of identifier * field list
  | True
  | False
and field = identifier * expr

type constant_declaration = {
  name: identifier;
  dtype: data_type;
  definition: expr option;
}

type variable_declaration = {
  name: identifier;
  dtype: data_type;
}

type input_parameter = {
  name: identifier;
  is_constant: bool;
  dtype: data_type;
}

type output_parameter = {
  name: identifier;
  dtype: data_type;
}

type symbol_definition = {
  name: identifier;
  is_constant: bool;
  dtype: data_type;
  definition: expr;
}

type contract_item = {
  name: identifier option;
  expression: expr;
}

type contract_mode = {
  requires: contract_item list;
  ensures: contract_item list;
}

type contract_import = {
  contract: identifier;
  input_parameters: input_parameter list;
  output_parameters: output_parameter list;
}

type contract_spec = {
  constant_declarations: symbol_definition list;
  variable_declarations: symbol_definition list;
  assumes: contract_item list;
  guarantees: contract_item list;
  modes: contract_mode list;
  imports: contract_import list;
}

type node_equation = {
  lhs: identifier list;
  rhs: expr;
}

type node_property = {
  name: identifier option;
  expression: expr;
}

type node_body = {
  constant_declarations: constant_declaration list;
  variable_declarations: variable_declaration list;
  assertions: expr list;
  equations: node_equation list;
  properties: node_property list;
}

type node = {
  name: identifier;
  is_function: bool;
  is_main: bool;
  input_parameters: input_parameter list;
  output_parameters: output_parameter list;
  contract: contract_spec option;
  body: node_body option;
}

type contract = {
  name: identifier;
  input_parameters: input_parameter list;
  output_parameters: output_parameter list;
  specification: contract_spec;
}

type dataflow_model = {
  type_declarations: type_declaration list;
  constant_declarations: constant_declaration list;
  contract_declarations: contract list;
  node_declarations: node list;
  }

type cyber_cia = CyberC | CyberI | CyberA

type safety_ia = SafetyI | SafetyA

type cyber_severity =
  CyberNone | CyberMinor | CyberMajor
  | CyberHazardous | CyberCatastrophic

type cyber_port = {name: string; cia: cyber_cia}

type safety_port = {name: string; ia: safety_ia}

type cyber_expr =
  | CyberPort of cyber_port
  | CyberAnd of cyber_expr list
  | CyberOr of cyber_expr list
  | CyberNot of cyber_expr

type safety_expr =
  | SafetyPort of safety_port
  | SafetyFault of string
  | SafetyAnd of safety_expr list
  | SafetyOr of safety_expr list
  | SafetyNot of safety_expr

type cyber_req = {
    id: string;
    cia: cyber_cia option;
    severity: cyber_severity;
    condition: cyber_expr;
    comment: string option;
    description: string option;
    phases: string option;
    extern: string option;
  }

type safety_req = {
    id: string;
    condition: safety_expr;
    target_probability: string;
    comment: string option;
    description: string option;
  }

type cyber_rel = {
    id: string;
    output: cyber_port;
    inputs: cyber_expr option;
    comment: string option;
    description: string option;
    phases: string option;
    extern: string option;
  }

type safety_rel = {
    id: string;
    output: safety_port;
    faultSrc: safety_expr option;
    comment: string option;
    description: string option;
  }

type safety_event = {
    id: string;
    probability: string;
    comment: string option;
    description: string option;
  }

type mission = {
    id: string;
    description: string option;
    comment: string option;
    reqs: string list;
  }

type component_type = {
  name: identifier;
  ports: port list;
  contract: contract_spec option;
  cyber_rels: cyber_rel list;
  safety_rels: safety_rel list;
  safety_events: safety_event list;
}

type comp_type_ref = int

type comp_impl_ref = int

type instance_type =
  | Specification of comp_type_ref
  | Implementation of comp_impl_ref

type attribute_type = Integer | Real | Bool | String

type generic_attribute = {
  name: string;
  atype: attribute_type;
  value: string;
}

type component_instance = {
  name: identifier;
  itype: instance_type;
  attributes: generic_attribute list;
}

type port_ref = int

type comp_inst_ref = int

type connection_end =
  | ComponentCE of (comp_type_ref * port_ref)
  | SubcomponentCE of (comp_inst_ref * comp_type_ref * port_ref)

type connection = {
  name: identifier;
  attributes: generic_attribute list;
  source: connection_end;
  destination: connection_end;
}

type block_impl = {
  subcomponents: component_instance list;
  connections: connection list;
}

type implementation =
  | BlockImpl of block_impl
  | DataflowImpl of node_body

type component_impl = {
  name: identifier;
  ctype: comp_type_ref;
  impl: implementation;
  }

type threat_intro = {
    var : string;
    var_type : string;
  }

type threat_var_pair = {
    left : string list;
    right : string list;
  }

type threat_expr =
  | TEqual of threat_var_pair
  | TContains of threat_var_pair
  | TForall of threat_quantifier
  | TExists of threat_quantifier
  | TImplies of threat_implies
  | TOr of threat_expr list
  | TAnd of threat_expr list
  | TNot of threat_expr

and threat_quantifier = {
    intro : threat_intro;
    expr : threat_expr;
  }

and threat_implies = {
    antecedent : threat_expr;
    consequent : threat_expr;
  }

type threat_model = {
    id : string;
    intro : threat_intro;
    expr : threat_expr;
    cia : cyber_cia;
    reference : string option;
    assumptions : string list;
    description : string option;
    comment : string option;
  }

type threat_defense = {
    id : string;
    threats : string list;
    description : string option;
    comment : string option;
  }

type model = {
  name: string;
  type_declarations: type_declaration list;
  component_types: component_type list;
  dataflow_code: dataflow_model option;
  comp_impl: component_impl list;
  cyber_reqs: cyber_req list;
  safety_reqs: safety_req list;
  missions: mission list;
  threat_models: threat_model list;
  threat_defenses: threat_defense list;
}

type package = {
  name: string;
  model: model;
}

type t = package

val is_subtype: type_declaration list -> data_type -> data_type -> bool

val pp_print_vdm_iml: Format.formatter -> t -> unit

