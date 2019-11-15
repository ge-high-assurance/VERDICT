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
}

type port_mode = In | Out

type port = {
  name: identifier;
  mode: port_mode;
  ptype: data_type option
}

type binary_op =
  | Arrow | Impl | Equiv | Or | And | Lt | Lte | Gt | Gte | Eq | Neq
  | Plus | Minus | Times | Div | IntDiv | Mod

type unary_op = Not | UMinus | Pre

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

type cyber_severity =
  CyberNone | CyberMinor | CyberMajor
  | CyberHazardous | CyberCatastrophic

type cyber_port = {name: string; cia: cyber_cia}

type cyber_expr =
  | CyberPort of cyber_port
  | CyberAnd of cyber_expr list
  | CyberOr of cyber_expr list
  | CyberNot of cyber_expr

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

type cyber_rel = {
    id: string;
    output: cyber_port;
    inputs: cyber_expr option;
    comment: string option;
    description: string option;
    phases: string option;
    extern: string option;
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
}

type comp_type_ref = int

type comp_impl_ref = int

type instance_type =
  | Specification of comp_type_ref
  | Implementation of comp_impl_ref

type manufacturer_type = ThirdParty | InHouse

type kind_of_component = Software | Hardware | Human | Hybrid

type situated_type = OnBoard | Remote

type pedigree_type = InternallyDeveloped | COTS | Sourced 

type component_instance = {
  name: identifier;
  itype: instance_type;

  manufacturer: manufacturer_type option;
  pedigree: pedigree_type option;
  category: string option;
  component_type: kind_of_component option;
  situated: situated_type option;
  adversarially_tested: bool option;
  has_sensitive_info: bool option;
  inside_trusted_boundary: bool option;
  canReceiveConfigUpdate: bool option;
  canReceiveSWUpdate: bool option;
  controlReceivedFromUntrusted: bool option;
  controlSentToUntrusted: bool option;
  dataReceivedFromUntrusted: bool option;
  dataSentToUntrusted: bool option;

  configuration_Attack: bool option;
  physical_Theft_Attack: bool option;
  interception_Attack: bool option;
  hardware_Integrity_Attack: bool option;
  supply_Chain_Attack: bool option;
  brute_Force_Attack: bool option;
  fault_Injection_Attack: bool option;
  identity_Spoofing_Attack: bool option;
  excessive_Allocation_Attack: bool option;
  sniffing_Attack: bool option;
  buffer_Attack: bool option;
  flooding_Attack: bool option;

  anti_jamming: bool option;
  auditMessageResponses: bool option;
  deviceAuthentication: bool option;
  dosProtection: bool option;
  encryptedStorage: bool option;
  heterogeneity: bool option;
  inputValidation: bool option;
  logging: bool option;
  memoryProtection: bool option;
  physicalAccessControl: bool option;
  removeIdentifyingInformation: bool option;
  resourceAvailability: bool option;
  resourceIsolation: bool option;
  secureBoot: bool option;
  sessionAuthenticity: bool option;
  staticCodeAnalysis: bool option;
  strongCryptoAlgorithms: bool option;
  supplyChainSecurity: bool option;
  systemAccessControl: bool option;
  tamperProtection: bool option;
  userAuthentication: bool option;

  anti_jamming_dal: integer option;
  auditMessageResponsesDAL: integer option;
  deviceAuthenticationDAL: integer option;
  dosProtectionDAL: integer option;
  encryptedStorageDAL: integer option;
  heterogeneity_dal: integer option;
  inputValidationDAL: integer option;
  loggingDAL: integer option;
  memoryProtectionDAL: integer option;
  physicalAccessControlDAL: integer option;
  removeIdentifyingInformationDAL: integer option;
  resourceAvailabilityDAL: integer option;
  resourceIsolationDAL: integer option;
  secureBootDAL: integer option;
  sessionAuthenticityDAL: integer option;
  staticCodeAnalysisDAL: integer option;
  strongCryptoAlgorithmsDAL: integer option;
  supplyChainSecurityDAL: integer option;
  systemAccessControlDAL: integer option;
  tamperProtectionDAL: integer option;
  userAuthenticationDAL: integer option;

(*
  broadcast_from_outside_tb: bool option;
  wifi_from_outside_tb: bool option;
  encryption: bool option;
  anti_flooding: bool option;
  anti_fuzzing: bool option;
  encryption_dal: integer option;
  anti_flooding_dal: integer option;
  anti_fuzzing_dal: integer option;
*)
}

type flow_type = Xdata | Xcontrol | Xrequest

type port_ref = int

type comp_inst_ref = int

type connection_end =
  | ComponentCE of (comp_type_ref * port_ref)
  | SubcomponentCE of (comp_inst_ref * comp_type_ref * port_ref)

type connection_type = Local | Remote

type connection = {
  name: identifier;
  ftype: flow_type option;
  conn_type: connection_type option;
  authenticated: bool option;
  data_encrypted: bool option;
  trustedConnection: bool option;
  encryptedTransmission: bool option;
  encryptedTransmissionDAL: integer option;
  replayProtection: bool option;
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
  missions: mission list;
  threat_models: threat_model list;
  threat_defenses: threat_defense list;
}

type package = {
  name: string;
  model: model;
}

type t = package


let pp_print_imports ppf =
  Format.fprintf ppf "@,import iml.utils.*;@,import iml.verdict.*;@,"

let pp_print_plain_type ppf = function
  | Int -> Format.fprintf ppf "Integer"
  | Bool -> Format.fprintf ppf "Bool"
  | Real -> Format.fprintf ppf "Real"

let rec pp_print_data_type ind ppf = function
  | PlainType pt -> (
    Format.fprintf ppf "%aType@]" pp_print_plain_type pt
  )
  | SubrangeType (lb, up) -> (
    Format.fprintf ppf "some (dt: DataType) {@,";
    Format.fprintf ppf "dt.kind = DataTypeKind.Subrange &&@,";
    Format.fprintf ppf "dt.subrange_type = mk_subrange(%s, %s)" lb up;
    Format.fprintf ppf "@]@,}"
  )
  | EnumType enumerators -> (
    Format.fprintf ppf "some (dt: DataType) {@,";
    Format.fprintf ppf "dt.kind = DataTypeKind.Enum &&@,";
    Format.fprintf ppf "dt.enum_type.length = %d &&@,"
      (List.length enumerators);
    let pp_sep ppf () = Format.fprintf ppf " &&@," in
    Format.pp_print_list ~pp_sep
      (fun ppf (i, e) ->
         Format.fprintf ppf "dt.enum_type.element[%d] = \"%s\"" i e
      )
      ppf
      (List.mapi (fun i e -> (i, e)) enumerators);
    Format.fprintf ppf "@]@,}"
  )
  | RecordType fields -> (
    Format.fprintf ppf "some (dt: DataType) {@,";
    Format.fprintf ppf "dt.kind = DataTypeKind.Record &&@,";
    Format.fprintf ppf "dt.record_type.length = %d &&@,"
      (List.length fields);
    pp_print_record_field_list ind ppf fields;
    Format.fprintf ppf "@]@,}"
  )
  | UserDefinedType r -> (
    Format.fprintf ppf "some (dt: DataType) {@,";
    Format.fprintf ppf "dt.kind = DataTypeKind.UserDefined &&@,";
    Format.fprintf ppf "dt.user_defined_type = m.type_declarations.element[%d]@]" r;
    Format.fprintf ppf "@]@,}"
  )

and pp_print_record_field_list ind ppf fields =
  let pp_sep ppf () = Format.fprintf ppf " &&@," in
  Format.pp_print_list ~pp_sep
      (fun ppf (i, rf) ->
         Format.fprintf ppf "@[<v %d>dt.record_type.element[%d] = " ind i;
         Format.fprintf ppf "some (rf: RecordField) {@,";
         Format.fprintf ppf "%a@]@,}"
           (pp_print_record_field ind) rf
      )
      ppf
      (List.mapi (fun i rf -> (i, rf)) fields);

and pp_print_record_field ind ppf (id, dt) =
  Format.fprintf ppf "rf.name = \"%s\" &&@," id;
  Format.fprintf ppf "@[<v %d>rf.dtype = %a" ind
    (pp_print_data_type ind) dt

let pp_print_type_declaration ind ppf ({name; definition}: type_declaration) =
  Format.fprintf ppf "td.name = \"%s\" &&@," name;
  match definition with
  | None -> Format.fprintf ppf "td.definition = mk_none<DataType>"
  | Some dt -> (
    Format.fprintf ppf "@[<v %d>td.definition = mk_some<DataType>(%a)"
      ind (pp_print_data_type ind) dt
  )

let pp_print_type_declaration_list ind ppf type_decls =
  type_decls |> List.iteri (fun i td ->
    Format.fprintf ppf "@[<v %d>m.type_declarations.element[%d] = " ind i;
    Format.fprintf ppf "some (td: TypeDeclaration) {@,";
    Format.fprintf ppf "%a@]@,} &&@,"
      (pp_print_type_declaration ind) td
  )

let get_bin_op_kind_and_field = function
  | Arrow -> "Arrow", "arrow"
  | Impl -> "Implies", "implies"
  | Equiv -> "Equal", "equal"
  | Or -> "Or", "or"
  | And -> "And", "and"
  | Lt -> "LessThan", "less_than"
  | Lte -> "LessThanOrEqualTo", "less_than_or_equal_to"
  | Gt -> "GreaterThan", "greater_than"
  | Gte -> "GreaterThanOrEqualTo", "greater_than_or_equal_to"
  | Eq -> "Equal", "equal"
  | Neq -> "NotEqual", "not_equal"
  | Plus -> "Plus", "plus"
  | Minus -> "Minus", "minus"
  | Times -> "Times", "times"
  | Div -> "Div", "div"
  | IntDiv -> "IntDiv", "int_div"
  | Mod -> "Mod", "modulo"

let get_unary_op_kind_and_field = function
  | Not -> "Not", "not"
  | UMinus -> "Negative", "negative"
  | Pre -> "Pre", "pre"

let rec pp_print_expr ind ppf expr =
  Format.fprintf ppf "e.kind = ExpressionKind.";
  match expr with
  | BinaryOp (op, e1, e2) -> (
    let kind, field = get_bin_op_kind_and_field op in
    Format.fprintf ppf "%s &&@,@[<v %d>e.%s = " kind ind field;
    Format.fprintf ppf "some (bo: BinaryOperation) {@,";
    Format.fprintf ppf "%a@]@,}" (pp_print_binary_operation ind) (e1,e2)
  )
  | UnaryOp (op, e) -> (
    let kind, field = get_unary_op_kind_and_field op in
    Format.fprintf ppf "%s &&@,@[<v %d>e.%s = " kind ind field;
    Format.fprintf ppf "some (e: Expression) {@,";
    Format.fprintf ppf "%a@]@,}" (pp_print_expr ind) e
  )
  | Ite (e1, e2, e3) -> (
    Format.fprintf ppf "ConditionalExpr &&@,";
    Format.fprintf ppf "@[<v %d>e.conditional_expr = " ind;
    Format.fprintf ppf "some (ite: IfThenElse) {@,";
    Format.fprintf ppf "@[<v %d>ite.condition = some (e: Expression) {@," ind;
    Format.fprintf ppf "%a@]@,} &&@," (pp_print_expr ind) e1;
    Format.fprintf ppf "@[<v %d>ite.thenBranch = some (e: Expression) {@," ind;
    Format.fprintf ppf "%a@]@,} &&@," (pp_print_expr ind) e2;
    Format.fprintf ppf "@[<v %d>ite.elseBranch = some (e: Expression) {@," ind;
    Format.fprintf ppf "%a@]@,}" (pp_print_expr ind) e3;
    Format.fprintf ppf "@]@,}"
  )
  | Proj (e, field) -> (
    Format.fprintf ppf "RecordProjection &&@,";
    Format.fprintf ppf "@[<v %d>e.record_projection = " ind;
    Format.fprintf ppf "some (rp: RecordProjection) {@,";
    Format.fprintf ppf "@[<v %d>rp.record_reference = some (e: Expression) {@," ind;
    Format.fprintf ppf "%a@]@,} &&@," (pp_print_expr ind) e;
    Format.fprintf ppf "rp.field_id = \"%s\"" field;
    Format.fprintf ppf "@]@,}"
  )
  | Ident id -> (
    Format.fprintf ppf "Id &&@,";
    Format.fprintf ppf "e.identifier = \"%s\"" id
  )
  | IntegerLit l -> (
    Format.fprintf ppf "IntLiteral &&@,";
    Format.fprintf ppf "e.int_literal = %s" l
  )
  | RealLit l -> (
    Format.fprintf ppf "RealLiteral &&@,";
    Format.fprintf ppf "e.real_literal = %s" l
  )
  | Call (name, args) -> (
    Format.fprintf ppf "Call &&@,";
    Format.fprintf ppf "@[<v %d>e.call = " ind;
    Format.fprintf ppf "some (cll: NodeCall) {@,";
    Format.fprintf ppf "cll.node = \"%s\" &&@," name;
    Format.fprintf ppf "cll.arguments.length = %d" (List.length args);
    if args != [] then Format.fprintf ppf " &&@,";
    let pp_sep ppf () = Format.fprintf ppf " &&@," in
    Format.pp_print_list ~pp_sep
      (fun ppf (i, e) ->
         Format.fprintf ppf "@[<v %d>cll.arguments.element[%d] = " ind i;
         Format.fprintf ppf "some (e: Expression) {@,";
         Format.fprintf ppf "%a@]@,}" (pp_print_expr ind) e;
      )
      ppf
      (List.mapi (fun i e -> (i, e)) args);
    Format.fprintf ppf "@]@,}"
  )
  | RecordExpr (record_type, fields) -> (
    Format.fprintf ppf "RecordLiteral &&@,";
    Format.fprintf ppf "@[<v %d>e.record_literal = " ind;
    Format.fprintf ppf "some (rl: RecordLiteral) {@,";
    Format.fprintf ppf "rl.record_type = \"%s\" &&@," record_type;
    Format.fprintf ppf "rl.field_definitions.length = %d &&@," (List.length fields);
    let pp_sep ppf () = Format.fprintf ppf " &&@," in
    Format.pp_print_list ~pp_sep
      (fun ppf (i, f) ->
         Format.fprintf ppf "@[<v %d>rl.field_definitions.element[%d] = " ind i;
         Format.fprintf ppf "some (fd: FieldDefinition) {@,";
         Format.fprintf ppf "%a@]@,}" (pp_print_field_definition ind) f;
      )
      ppf
      (List.mapi (fun i f -> (i, f)) fields);
    Format.fprintf ppf "@]@,}"
  )
  | True -> (
    Format.fprintf ppf "BoolLiteral &&@,";
    Format.fprintf ppf "e.bool_literal = true"
  )
  | False -> (
    Format.fprintf ppf "BoolLiteral &&@,";
    Format.fprintf ppf "e.bool_literal = false"
  )

and pp_print_binary_operation ind ppf (e1,e2) =
  Format.fprintf ppf "@[<v %d>bo.lhs_operand = some (e: Expression) {@," ind;
  Format.fprintf ppf "%a@]@,} &&@," (pp_print_expr ind) e1;
  Format.fprintf ppf "@[<v %d>bo.rhs_operand = some (e: Expression) {@," ind;
  Format.fprintf ppf "%a@]@,}" (pp_print_expr ind) e2

and pp_print_field_definition ind ppf (id, e) =
  Format.fprintf ppf "fd.field_id = \"%s\" &&@," id;
  Format.fprintf ppf "@[<v %d>fd.field_value = some (e: Expression) {@," ind;
  Format.fprintf ppf "%a@]@,}" (pp_print_expr ind) e


let pp_print_port_mode ppf = function
  | In -> Format.fprintf ppf "In"
  | Out -> Format.fprintf ppf "Out"

let pp_print_port ind ppf { name; mode; ptype } =
  Format.fprintf ppf "p.name = \"%s\" &&@," name;
  Format.fprintf ppf "p.mode = PortMode.%a &&@," pp_print_port_mode mode;
  match ptype with
  | None -> Format.fprintf ppf "p.ptype = mk_none<DataType>"
  | Some dt -> (
    Format.fprintf ppf "@[<v %d>p.ptype = mk_some<DataType>(%a)"
      ind (pp_print_data_type ind) dt
  )

let pp_print_port_list ind ppf ports =
  ports |> List.iteri (fun i dp ->
    Format.fprintf ppf "@[<v %d>ct.ports.element[%d] = " ind i;
    Format.fprintf ppf "some(p: Port) {@,";
    Format.fprintf ppf "%a@]@,} &&@,"
      (pp_print_port ind) dp
  )

let pp_print_symbol_definition ind ppf
  ({name; dtype; definition}: symbol_definition)
=
  Format.fprintf ppf "sd.name = \"%s\" &&@," name;
  Format.fprintf ppf "@[<v %d>sd.dtype = %a &&@," ind
    (pp_print_data_type ind) dtype;
  Format.fprintf ppf "@[<v %d>sd.definition = " ind;
  Format.fprintf ppf "some (e: Expression) {@,";
    Format.fprintf ppf "%a@]@,}" (pp_print_expr ind) definition

let pp_print_symbol_definition_list ind ppf var_decls =
  var_decls |> List.iteri (fun i v ->
    Format.fprintf ppf "@[<v %d>csp.variable_declarations.element[%d] = " ind i;
    Format.fprintf ppf "some (sd: SymbolDefinition) {@,";
    Format.fprintf ppf "%a@]@,} &&@,"
      (pp_print_symbol_definition ind) v
  )

let pp_print_contract_item ind ppf ({name; expression}: contract_item) =
  (match name with
  | None -> Format.fprintf ppf "ci.name = mk_none<Identifier> &&@,"
  | Some id -> Format.fprintf ppf "ci.name = mk_some<Identifier>(\"%s\") &&@," id);
  Format.fprintf ppf "@[<v %d>ci.expression = " ind;
  Format.fprintf ppf "some (e: Expression) {@,";
    Format.fprintf ppf "%a@]@,}" (pp_print_expr ind) expression

let pp_print_contract_item_list field ind ppf c_items =
  c_items |> List.iteri (fun i ci ->
    Format.fprintf ppf "@[<v %d>csp.%s.element[%d] = " ind field i;
    Format.fprintf ppf "some (ci: ContractItem) {@,";
    Format.fprintf ppf "%a@]@,} &&@,"
      (pp_print_contract_item ind) ci
  )

let pp_print_contract_spec ind ppf
  {constant_declarations;
   variable_declarations;
   assumes;
   guarantees;
   modes;
   imports;
  }
=
  assert (constant_declarations = []);
  assert (modes = [] && imports = []);
  Format.fprintf ppf "csp.constant_declarations.length = 0 &&@,";
  Format.fprintf ppf "csp.variable_declarations.length = %d &&@,"
    (List.length variable_declarations);
  pp_print_symbol_definition_list ind ppf variable_declarations;
  Format.fprintf ppf "csp.assumes.length = %d &&@,"
    (List.length assumes);
  pp_print_contract_item_list "assumes" ind ppf assumes;
  Format.fprintf ppf "csp.guarantees.length = %d &&@,"
    (List.length guarantees);
  pp_print_contract_item_list "guarantees" ind ppf guarantees;
  Format.fprintf ppf "csp.modes.length = 0 &&@,";
  Format.fprintf ppf "csp.imports.length = 0"

let pp_print_contract_spec_opt field ind ppf = function
  | None -> Format.fprintf ppf "%s.contract = mk_none<ContractSpec>" field
  | Some csp -> (
    Format.fprintf ppf "@[<v %d>%s.contract = mk_some<ContractSpec>(" ind field;
    Format.fprintf ppf "some (csp: ContractSpec) {@,";
    Format.fprintf ppf "%a@]@,})"
      (pp_print_contract_spec ind) csp
  )

let pp_print_opt ppf s = function
  | Some a -> Format.fprintf ppf s a
  | None -> Format.fprintf ppf s ""

let pp_print_cyber_cia ind ppf cia =
  Format.fprintf ppf "CIA.%s"
    (match cia with
           | CyberC -> "Confidentiality"
           | CyberI -> "Integrity"
           | CyberA -> "Availability")

let pp_print_cyber_severity ind ppf severity =
  Format.fprintf ppf "Severity.%s"
    (match severity with
                | CyberCatastrophic -> "Catastrophic"
                | CyberHazardous -> "Hazardous"
                | CyberMajor -> "Major"
                | CyberMinor -> "Minor"
                | CyberNone -> "None")

let pp_print_cyber_port ind ppf {name; cia} =
  Format.fprintf ppf "some (port: CIAPort) {@,";
  Format.fprintf ppf "port.name = \"%s\" &&@," name;
  Format.fprintf ppf "port.cia = %a@,"
    (pp_print_cyber_cia ind) cia;
  Format.fprintf ppf "}"

let rec pp_print_cyber_expr ind ppf expr =
  Format.fprintf ppf "some (expr: CyberExpr) {@,";
  let kind, val_fmt = match expr with
    | CyberPort port
      -> "Port", fun () -> (Format.fprintf ppf
                              "@[<v %d>expr.port = %a@]@," ind
                              (pp_print_cyber_port ind) port)
    | CyberAnd es
      -> "And", fun ()
                -> pp_print_cyber_expr_list "expr" "and" ind ppf es
    | CyberOr es
      -> "Or", fun ()
               -> pp_print_cyber_expr_list "expr" "or" ind ppf es
    | CyberNot e
      -> "Not", fun () -> (Format.fprintf ppf
                             "@[<v %d>expr.not = %a@]@," ind
                             (pp_print_cyber_expr ind) e) in
  Format.fprintf ppf "expr.kind = CyberExprKind.%s &&@," kind;
  val_fmt ();
  Format.fprintf ppf "}@,"

and pp_print_cyber_expr_list name lst ind ppf exprs =
  Format.fprintf ppf
    "%s.%s.length = %d &&@," name lst (List.length exprs);
  let pp_sep ppf () = Format.fprintf ppf " &&@," in
  Format.pp_print_list ~pp_sep
    (fun ppf (i, expr) ->
      Format.fprintf ppf "@[<v %d>%s.%s.element[%d] = %a@]"
        ind name lst i (pp_print_cyber_expr ind) expr
    )
    ppf
    (List.mapi (fun i expr -> (i, expr)) exprs)

let pp_print_cyber_req_body ind ppf
      {id; cia; severity; condition; comment; description; phases; extern} =
  Format.fprintf ppf "req.id = \"%s\" &&@," id;
  Format.fprintf ppf "req.severity = %a &&@,"
    (pp_print_cyber_severity ind) severity;
  Format.fprintf ppf "@[<v %d>req.condition = %a &&@]@," ind
    (pp_print_cyber_expr ind) condition;
  pp_print_opt ppf "req.comment = \"%s\" &&@," comment;
  pp_print_opt ppf "req.description = \"%s\" &&@," description;
  pp_print_opt ppf "req.phases = \"%s\" &&@," phases;
  pp_print_opt ppf "req.extern = \"%s\"@," extern;
  match cia with
  | Some cia_val -> Format.fprintf ppf "&& req.cia = %a@," (pp_print_cyber_cia ind) cia_val
  | None -> ()

let pp_print_cyber_rel_body ind ppf
  {id; output; inputs; comment; description; phases; extern} =
  Format.fprintf ppf "rel.id = \"%s\" &&@," id;
  Format.fprintf ppf "@[<v %d>rel.output = %a &&@]@," ind
    (pp_print_cyber_port ind) output;
  (match inputs with
  | Some expr -> Format.fprintf ppf
                   "@[<v %d>rel.inputs = %a &&@]@," ind
                   (pp_print_cyber_expr ind) expr
  | None -> ());
  pp_print_opt ppf "rel.comment = \"%s\" &&@," comment;
  pp_print_opt ppf "rel.description = \"%s\" &&@," description;
  pp_print_opt ppf "rel.phases = \"%s\" &&@," phases;
  pp_print_opt ppf "rel.extern = \"%s\"@," extern

let pp_print_cyber_reqs_list ind ppf cyber_reqs =
  let pp_sep ppf () = Format.fprintf ppf " &&@," in
  Format.pp_print_list ~pp_sep
    (fun ppf (i, req) ->
      Format.fprintf ppf "@[<v %d>m.cyber_requirements.element[%d] = " ind i;
      Format.fprintf ppf "some (req: CyberReq) {@,";
      Format.fprintf ppf "%a@]@,}"
        (pp_print_cyber_req_body ind) req
    )
    ppf
    (List.mapi (fun i req -> (i, req)) cyber_reqs)

let pp_print_cyber_rels_list ind ppf cyber_rels =
  let pp_sep ppf () = Format.fprintf ppf " &&@," in
  Format.pp_print_list ~pp_sep
    (fun ppf (i, rel) ->
      Format.fprintf ppf "@[<v %d>ct.cyber_relations.element[%d] = " ind i;
      Format.fprintf ppf "some (rel: CyberRel) {@,";
      Format.fprintf ppf "%a@]@,}"
        (pp_print_cyber_rel_body ind) rel
    )
    ppf
    (List.mapi (fun i rel -> (i, rel)) cyber_rels)

let pp_print_component_type ind ppf {name; ports; contract; cyber_rels} =
  Format.fprintf ppf "ct.name = \"%s\" &&@," name;
  Format.fprintf ppf "ct.ports.length = %d &&@," (List.length ports);
  pp_print_port_list ind ppf ports;
  pp_print_contract_spec_opt "ct" ind ppf contract;
  Format.fprintf ppf " &&@,ct.cyber_relations.length = %d %s@,"
    (List.length cyber_rels)
    (match cyber_rels with _ :: _ -> "&&" | [] -> "");
  pp_print_cyber_rels_list ind ppf cyber_rels

let pp_print_comp_types_list ind ppf comp_types =
  comp_types |> List.iteri (fun i ct ->
    Format.fprintf ppf "@[<v %d>m.component_types.element[%d] = " ind i;
    Format.fprintf ppf "some (ct: ComponentType) {@,";
    Format.fprintf ppf "%a@]@,} &&@,"
      (pp_print_component_type ind) ct
  )

let pp_print_bool_prop_value ppf = function
  | None -> Format.fprintf ppf "mk_none<Bool>"
  | Some v -> Format.fprintf ppf "mk_some<Bool>(%B)" v

let pp_print_int_prop_value ppf = function
  | None -> Format.fprintf ppf "mk_none<Int>"
  | Some i -> Format.fprintf ppf "mk_some<Int>(%s)" i

let pp_print_string_prop_value ppf = function
  | None -> Format.fprintf ppf "mk_none<String>"
  | Some s -> Format.fprintf ppf "mk_some<String>(\"%s\")" s

let pp_print_manufacturer_type ppf = function
  | ThirdParty -> Format.fprintf ppf "ManufacturerType.ThirdParty"
  | InHouse -> Format.fprintf ppf "ManufacturerType.InHouse"

let pp_print_manufacturer_prop_value ppf = function
  | None -> Format.fprintf ppf "mk_none<ManufacturerType>"
  | Some m -> (
    Format.fprintf ppf "mk_some<ManufacturerType>(%a)"
      pp_print_manufacturer_type m;
  )

let pp_print_pedigree_type ppf = function
  | InternallyDeveloped -> Format.fprintf ppf "PedigreeType.InternallyDeveloped"
  | COTS -> Format.fprintf ppf "PedigreeType.COTS"
  | Sourced -> Format.fprintf ppf "PedigreeType.Sourced"

let pp_print_pedigree_prop_value ppf = function
  | None -> Format.fprintf ppf "mk_none<PedigreeType>"
  | Some m -> (
    Format.fprintf ppf "mk_some<PedigreeType>(%a)"
      pp_print_pedigree_type m;
  )

let pp_print_kind_of_component ppf = function
  | Software -> Format.fprintf ppf "KindOfComponent.Software"
  | Hardware -> Format.fprintf ppf "KindOfComponent.Hardware"
  | Human -> Format.fprintf ppf "KindOfComponent.Human"
  | Hybrid -> Format.fprintf ppf "KindOfComponent.Hybrid"

let pp_print_component_type_prop_value ppf = function
  | None -> Format.fprintf ppf "mk_none<KindOfComponent>"
  | Some m -> (
    Format.fprintf ppf "mk_some<KindOfComponent>(%a)"
      pp_print_kind_of_component m;
  )

let pp_print_situated_type ppf = function
  | OnBoard -> Format.fprintf ppf "SituatedType.OnBoard"
  | Remote -> Format.fprintf ppf "SituatedType.Remote"

let pp_print_situated_prop_value ppf = function
  | None -> Format.fprintf ppf "mk_none<SituatedType>"
  | Some m -> (
    Format.fprintf ppf "mk_some<SituatedType>(%a)"
      pp_print_situated_type m;
  )

let pp_print_comp_instance_properties ppf
  {manufacturer;
   pedigree;
   category;
   component_type;
   situated;
   adversarially_tested;
   has_sensitive_info;
   inside_trusted_boundary;
   canReceiveConfigUpdate;
   canReceiveSWUpdate;
   controlReceivedFromUntrusted;
   controlSentToUntrusted;
   dataReceivedFromUntrusted;
   dataSentToUntrusted;

   configuration_Attack;
   physical_Theft_Attack;
   interception_Attack;
   hardware_Integrity_Attack;
   supply_Chain_Attack;
   brute_Force_Attack;
   fault_Injection_Attack;
   identity_Spoofing_Attack;
   excessive_Allocation_Attack;
   sniffing_Attack;
   buffer_Attack;
   flooding_Attack;

   anti_jamming;
   auditMessageResponses;
   deviceAuthentication;
   dosProtection;
   encryptedStorage;
   heterogeneity;
   inputValidation;
   logging;
   memoryProtection;
   physicalAccessControl;
   removeIdentifyingInformation;
   resourceAvailability;
   resourceIsolation;
   secureBoot;
   sessionAuthenticity;
   staticCodeAnalysis;
   strongCryptoAlgorithms;
   supplyChainSecurity;
   systemAccessControl;
   tamperProtection;
   userAuthentication;

   anti_jamming_dal;
   auditMessageResponsesDAL;
   deviceAuthenticationDAL;
   dosProtectionDAL;
   encryptedStorageDAL;
   heterogeneity_dal;
   inputValidationDAL;
   loggingDAL;
   memoryProtectionDAL;
   physicalAccessControlDAL;
   removeIdentifyingInformationDAL;
   resourceAvailabilityDAL;
   resourceIsolationDAL;
   secureBootDAL;
   sessionAuthenticityDAL;
   staticCodeAnalysisDAL;
   strongCryptoAlgorithmsDAL;
   supplyChainSecurityDAL;
   systemAccessControlDAL;
   tamperProtectionDAL;
   userAuthenticationDAL

(*
   broadcast_from_outside_tb;
   wifi_from_outside_tb;
   encryption;
   anti_flooding;
   anti_fuzzing;
   encryption_dal;
   anti_flooding_dal;
   anti_fuzzing_dal;
*)
  }
=
  Format.fprintf ppf "ci.manufacturer = %a &&@,"
    pp_print_manufacturer_prop_value manufacturer;
  Format.fprintf ppf "ci.pedigree = %a &&@,"
    pp_print_pedigree_prop_value pedigree;
  Format.fprintf ppf "ci.category = %a &&@,"
    pp_print_string_prop_value category;
  Format.fprintf ppf "ci.component_type = %a &&@,"
    pp_print_component_type_prop_value component_type;
  Format.fprintf ppf "ci.situated = %a &&@,"
    pp_print_situated_prop_value situated;
  Format.fprintf ppf "ci.adversarially_tested = %a &&@,"
    pp_print_bool_prop_value adversarially_tested;
  Format.fprintf ppf "ci.has_sensitive_info = %a &&@,"
    pp_print_bool_prop_value has_sensitive_info;
  Format.fprintf ppf "ci.inside_trusted_boundary = %a &&@,"
    pp_print_bool_prop_value inside_trusted_boundary;

  Format.fprintf ppf "ci.canReceiveConfigUpdate = %a &&@,"
    pp_print_bool_prop_value canReceiveConfigUpdate;
  Format.fprintf ppf "ci.canReceiveSWUpdate = %a &&@,"
    pp_print_bool_prop_value canReceiveSWUpdate;
  Format.fprintf ppf "ci.controlReceivedFromUntrusted = %a &&@,"
    pp_print_bool_prop_value controlReceivedFromUntrusted;
  Format.fprintf ppf "ci.controlSentToUntrusted = %a &&@,"
    pp_print_bool_prop_value controlSentToUntrusted;
  Format.fprintf ppf "ci.dataReceivedFromUntrusted = %a &&@,"
    pp_print_bool_prop_value dataReceivedFromUntrusted;
  Format.fprintf ppf "ci.dataSentToUntrusted = %a &&@,"
    pp_print_bool_prop_value dataSentToUntrusted;

  Format.fprintf ppf "ci.Configuration_Attack = %a &&@,"
    pp_print_bool_prop_value configuration_Attack;
  Format.fprintf ppf "ci.Physical_Theft_Attack = %a &&@,"
    pp_print_bool_prop_value physical_Theft_Attack;
  Format.fprintf ppf "ci.Interception_Attack = %a &&@,"
    pp_print_bool_prop_value interception_Attack;
  Format.fprintf ppf "ci.Hardware_Integrity_Attack = %a &&@,"
    pp_print_bool_prop_value hardware_Integrity_Attack;
  Format.fprintf ppf "ci.Supply_Chain_Attack = %a &&@,"
    pp_print_bool_prop_value supply_Chain_Attack;
  Format.fprintf ppf "ci.Brute_Force_Attack = %a &&@,"
    pp_print_bool_prop_value brute_Force_Attack;
  Format.fprintf ppf "ci.Fault_Injection_Attack = %a &&@,"
    pp_print_bool_prop_value fault_Injection_Attack;
  Format.fprintf ppf "ci.Identity_Spoofing_Attack = %a &&@,"
    pp_print_bool_prop_value identity_Spoofing_Attack;
  Format.fprintf ppf "ci.Excessive_Allocation_Attack = %a &&@,"
    pp_print_bool_prop_value excessive_Allocation_Attack;
  Format.fprintf ppf "ci.Sniffing_Attack = %a &&@,"
    pp_print_bool_prop_value sniffing_Attack;
  Format.fprintf ppf "ci.Buffer_Attack = %a &&@,"
    pp_print_bool_prop_value buffer_Attack;
  Format.fprintf ppf "ci.Flooding_Attack = %a &&@,"
    pp_print_bool_prop_value flooding_Attack;

  Format.fprintf ppf "ci.anti_jamming = %a &&@,"
    pp_print_bool_prop_value anti_jamming;
  Format.fprintf ppf "ci.auditMessageResponses = %a &&@,"
    pp_print_bool_prop_value auditMessageResponses;
  Format.fprintf ppf "ci.deviceAuthentication = %a &&@,"
    pp_print_bool_prop_value deviceAuthentication;
  Format.fprintf ppf "ci.dosProtection = %a &&@,"
    pp_print_bool_prop_value dosProtection;
  Format.fprintf ppf "ci.encryptedStorage = %a &&@,"
    pp_print_bool_prop_value encryptedStorage;
  Format.fprintf ppf "ci.heterogeneity = %a &&@,"
    pp_print_bool_prop_value heterogeneity;

  Format.fprintf ppf "ci.inputValidation = %a &&@,"
    pp_print_bool_prop_value inputValidation;
  Format.fprintf ppf "ci.logging = %a &&@,"
    pp_print_bool_prop_value logging;
  Format.fprintf ppf "ci.memoryProtection = %a &&@,"
    pp_print_bool_prop_value memoryProtection;
  Format.fprintf ppf "ci.physicalAccessControl = %a &&@,"
    pp_print_bool_prop_value physicalAccessControl;
  Format.fprintf ppf "ci.removeIdentifyingInformation = %a &&@,"
    pp_print_bool_prop_value removeIdentifyingInformation;
  Format.fprintf ppf "ci.resourceAvailability = %a &&@,"
    pp_print_bool_prop_value resourceAvailability;
  Format.fprintf ppf "ci.resourceIsolation = %a &&@,"
    pp_print_bool_prop_value resourceIsolation;
  Format.fprintf ppf "ci.secureBoot = %a &&@,"
    pp_print_bool_prop_value secureBoot;
  Format.fprintf ppf "ci.sessionAuthenticity = %a &&@,"
    pp_print_bool_prop_value sessionAuthenticity;
  Format.fprintf ppf "ci.staticCodeAnalysis = %a &&@,"
    pp_print_bool_prop_value staticCodeAnalysis;
  Format.fprintf ppf "ci.strongCryptoAlgorithms = %a &&@,"
    pp_print_bool_prop_value strongCryptoAlgorithms;
  Format.fprintf ppf "ci.supplyChainSecurity = %a &&@,"
    pp_print_bool_prop_value supplyChainSecurity;
  Format.fprintf ppf "ci.systemAccessControl = %a &&@,"
    pp_print_bool_prop_value systemAccessControl;
  Format.fprintf ppf "ci.tamperProtection = %a &&@,"
    pp_print_bool_prop_value tamperProtection;
  Format.fprintf ppf "ci.userAuthentication = %a &&@,"
    pp_print_bool_prop_value userAuthentication;

  Format.fprintf ppf "ci.anti_jamming_dal = %a &&@,"
    pp_print_int_prop_value anti_jamming_dal;

  Format.fprintf ppf "ci.auditMessageResponsesDAL = %a &&@,"
    pp_print_int_prop_value auditMessageResponsesDAL;
  Format.fprintf ppf "ci.deviceAuthenticationDAL = %a &&@,"
    pp_print_int_prop_value deviceAuthenticationDAL;
  Format.fprintf ppf "ci.dosProtectionDAL = %a &&@,"
    pp_print_int_prop_value dosProtectionDAL;
  Format.fprintf ppf "ci.encryptedStorageDAL = %a &&@,"
    pp_print_int_prop_value encryptedStorageDAL;
  Format.fprintf ppf "ci.heterogeneity_dal = %a &&@,"
    pp_print_int_prop_value heterogeneity_dal;

  Format.fprintf ppf "ci.inputValidationDAL = %a &&@,"
    pp_print_int_prop_value inputValidationDAL;
  Format.fprintf ppf "ci.loggingDAL = %a &&@,"
    pp_print_int_prop_value loggingDAL;
  Format.fprintf ppf "ci.memoryProtectionDAL = %a &&@,"
    pp_print_int_prop_value memoryProtectionDAL;
  Format.fprintf ppf "ci.physicalAccessControlDAL = %a &&@,"
    pp_print_int_prop_value physicalAccessControlDAL;
  Format.fprintf ppf "ci.removeIdentifyingInformationDAL = %a &&@,"
    pp_print_int_prop_value removeIdentifyingInformationDAL;
  Format.fprintf ppf "ci.resourceAvailabilityDAL = %a &&@,"
    pp_print_int_prop_value resourceAvailabilityDAL;
  Format.fprintf ppf "ci.resourceIsolationDAL = %a &&@,"
    pp_print_int_prop_value resourceIsolationDAL;
  Format.fprintf ppf "ci.secureBootDAL = %a &&@,"
    pp_print_int_prop_value secureBootDAL;
  Format.fprintf ppf "ci.sessionAuthenticityDAL = %a &&@,"
    pp_print_int_prop_value sessionAuthenticityDAL;
  Format.fprintf ppf "ci.staticCodeAnalysisDAL = %a &&@,"
    pp_print_int_prop_value staticCodeAnalysisDAL;
  Format.fprintf ppf "ci.strongCryptoAlgorithmsDAL = %a &&@,"
    pp_print_int_prop_value strongCryptoAlgorithmsDAL;
  Format.fprintf ppf "ci.supplyChainSecurityDAL = %a &&@,"
    pp_print_int_prop_value supplyChainSecurityDAL;
  Format.fprintf ppf "ci.systemAccessControlDAL = %a &&@,"
    pp_print_int_prop_value systemAccessControlDAL;
  Format.fprintf ppf "ci.tamperProtectionDAL = %a &&@,"
    pp_print_int_prop_value tamperProtectionDAL;
  Format.fprintf ppf "ci.userAuthenticationDAL = %a"
    pp_print_int_prop_value userAuthenticationDAL


(*
  Format.fprintf ppf "ci.broadcast_from_outside_tb = %a &&@,"
    pp_print_bool_prop_value broadcast_from_outside_tb;
  Format.fprintf ppf "ci.wifi_from_outside_tb = %a &&@,"
    pp_print_bool_prop_value wifi_from_outside_tb;
  Format.fprintf ppf "ci.encryption = %a &&@,"
    pp_print_bool_prop_value encryption;
  Format.fprintf ppf "ci.anti_flooding = %a &&@,"
    pp_print_bool_prop_value anti_flooding;
  Format.fprintf ppf "ci.anti_fuzzing = %a &&@,"
    pp_print_bool_prop_value anti_fuzzing;
  Format.fprintf ppf "ci.encryption_dal = %a &&@,"
    pp_print_int_prop_value encryption_dal;
  Format.fprintf ppf "ci.anti_fuzzing_dal = %a"
    pp_print_int_prop_value anti_fuzzing_dal;
  Format.fprintf ppf "ci.anti_flooding_dal = %a &&@,"
    pp_print_int_prop_value anti_flooding_dal;
*)

let pp_print_comp_instance ind ppf ({name; itype} as ci) =
  Format.fprintf ppf "ci.name = \"%s\" &&@," name;
  (match itype with
  | Specification idx -> (
    Format.fprintf ppf "ci.kind = ComponentInstanceKind.Specification &&@,";
    Format.fprintf ppf "ci.specification = m.component_types.element[%d] &&@," idx
  )
  | Implementation idx -> (
    Format.fprintf ppf "ci.kind = ComponentInstanceKind.Implementation &&@,";
    Format.fprintf ppf "ci.implementation = m.component_impl.element[%d] &&@," idx
  ));
  pp_print_comp_instance_properties ppf ci

let pp_print_subcomponent_list ind ppf subcomponents =
  subcomponents |> List.iteri (fun i sub ->
    Format.fprintf ppf "@[<v %d>imp.subcomponents.element[%d] = " ind i;
    Format.fprintf ppf "some (ci: ComponentInstance) {@,";
    Format.fprintf ppf "%a@]@,} &&@,"
      (pp_print_comp_instance ind) sub
  )

let pp_print_flow_type ppf = function
  | Xdata -> Format.fprintf ppf "FlowType.Xdata"
  | Xcontrol -> Format.fprintf ppf "FlowType.Xcontrol"
  | Xrequest -> Format.fprintf ppf "FlowType.Xrequest"

let pp_print_flow_type_prop_value ppf = function
  | None -> Format.fprintf ppf "mk_none<FlowType>"
  | Some m -> (
    Format.fprintf ppf "mk_some<FlowType>(%a)"
      pp_print_flow_type m;
  )

let pp_print_comp_inst_port ppf ref =
  let ci_idx, ct_idx, port_idx = ref in
  Format.fprintf ppf "sp.subcomponent = imp.subcomponents.element[%d] &&@," ci_idx;
  Format.fprintf ppf "sp.port = ";
  Format.fprintf ppf "m.component_types.element[%d].ports.element[%d]"
    ct_idx port_idx

let pp_print_connection_end ind inst ppf = function
  | ComponentCE (ct_idx, port_idx) -> (
    Format.fprintf ppf "%s.kind = ConnectionEndKind.ComponentCE &&@," inst;
    Format.fprintf ppf "%s.component_port = " inst;
    Format.fprintf ppf "m.component_types.element[%d].ports.element[%d]"
      ct_idx port_idx
  )
  | SubcomponentCE ref -> (
    Format.fprintf ppf "%s.kind = ConnectionEndKind.SubcomponentCE &&@," inst;
    Format.fprintf ppf "@[<v %d>%s.subcomponent_port = " ind inst;
    Format.fprintf ppf "some (sp: CompInstPort) {@,";
    Format.fprintf ppf "%a@]@,}"
    pp_print_comp_inst_port ref
  )

let pp_print_connection_type ppf = function
  | Local -> Format.fprintf ppf "ConnectionType.Local"
  | Remote -> Format.fprintf ppf "ConnectionType.Remote"

let pp_print_conn_type_prop_value ppf = function
  | None -> Format.fprintf ppf "mk_none<ConnectionType>"
  | Some t -> (
    Format.fprintf ppf "mk_some<ConnectionType>(%a)"
      pp_print_connection_type t;
  )

let pp_print_connection ind ppf
  {name;
   ftype;
   conn_type;
   authenticated;
   data_encrypted;
   trustedConnection;
   encryptedTransmission;
   encryptedTransmissionDAL;
   replayProtection;
   source;
   destination
 }
=
  Format.fprintf ppf "c.name = \"%s\" &&@," name;
  Format.fprintf ppf "c.conn_type = %a &&@,"
    pp_print_conn_type_prop_value conn_type; 
  Format.fprintf ppf "c.flow_type = %a &&@,"
    pp_print_flow_type_prop_value ftype;
  Format.fprintf ppf "c.data_encrypted = %a &&@,"
    pp_print_bool_prop_value data_encrypted;
  Format.fprintf ppf "c.authenticated = %a &&@,"
    pp_print_bool_prop_value authenticated;
  Format.fprintf ppf "c.trustedConnection = %a &&@,"
    pp_print_bool_prop_value trustedConnection;
  Format.fprintf ppf "c.encryptedTransmission = %a &&@,"
    pp_print_bool_prop_value encryptedTransmission;
  Format.fprintf ppf "c.encryptedTransmissionDAL = %a &&@,"
    pp_print_int_prop_value encryptedTransmissionDAL;
  Format.fprintf ppf "c.replayProtection = %a &&@,"
    pp_print_bool_prop_value replayProtection;
  Format.fprintf ppf "@[<v %d>c.source = some (src: ConnectionEnd) {@," ind;
  Format.fprintf ppf "%a@]@,} &&@,"
    (pp_print_connection_end ind "src") source;
  Format.fprintf ppf "@[<v %d>c.destination = some (dst: ConnectionEnd) {@," ind;
  Format.fprintf ppf "%a@]@,}"
    (pp_print_connection_end ind "dst") destination

let pp_print_connection_list ind ppf connections =
  let pp_sep ppf () = Format.fprintf ppf " &&@," in
  Format.pp_print_list ~pp_sep
      (fun ppf (i, c) ->
         Format.fprintf ppf "@[<v %d>imp.connections.element[%d] = " ind i;
         Format.fprintf ppf "some (c: Connection) {@,";
         Format.fprintf ppf "%a@]@,}"
           (pp_print_connection ind) c
      )
      ppf
      (List.mapi (fun i c -> (i, c)) connections)

let pp_print_variable_declaration ind ppf
  ({name; dtype}: variable_declaration)
=
  Format.fprintf ppf "vd.name = \"%s\" &&@," name;
  Format.fprintf ppf "@[<v %d>vd.dtype = %a" ind
    (pp_print_data_type ind) dtype

let pp_print_variable_declaration_list ind ppf var_decls =
  var_decls |> List.iteri (fun i v ->
    Format.fprintf ppf "@[<v %d>nb.variable_declarations.element[%d] = " ind i;
    Format.fprintf ppf "some (vd: VariableDeclaration) {@,";
    Format.fprintf ppf "%a@]@,} &&@,"
      (pp_print_variable_declaration ind) v
  )

let pp_print_assertion_list ind ppf assertions =
  assertions |> List.iteri (fun i e ->
    Format.fprintf ppf "@[<v %d>nb.assertions.element[%d] = " ind i;
    Format.fprintf ppf "some (e: Expression) {@,";
    Format.fprintf ppf "%a@]@,} &&@,"
      (pp_print_expr ind) e
  )

let pp_print_equation ind ppf {lhs; rhs} =
  Format.fprintf ppf "eq.lhs.length = %d &&@,"
    (List.length lhs);
  lhs |> List.iteri (fun i id ->
    Format.fprintf ppf "eq.lhs.element[%d] = \"%s\" &&@," i id
  );
  Format.fprintf ppf "@[<v %d>eq.rhs = some (e: Expression) {@," ind;
  Format.fprintf ppf "%a@]@,}"
    (pp_print_expr ind) rhs

let pp_print_equation_list ind ppf equations =
  equations |> List.iteri (fun i eq ->
    Format.fprintf ppf "@[<v %d>nb.equations.element[%d] = " ind i;
    Format.fprintf ppf "some (eq: NodeEquation) {@,";
    Format.fprintf ppf "%a@]@,} &&@,"
      (pp_print_equation ind) eq
  )

let pp_print_constant_declaration ind ppf
  ({name; dtype; definition}: constant_declaration)
=
  Format.fprintf ppf "ctd.name = \"%s\" &&@," name;
  Format.fprintf ppf "@[<v %d>ctd.dtype = %a" ind
    (pp_print_data_type ind) dtype;
  match definition with
  | None -> Format.fprintf ppf " &&@,ctd.definition = mk_none<Expression>"
  | Some e -> (
    Format.fprintf ppf " &&@,@[<v %d>ctd.definition = mk_some<Expression>(" ind;
    Format.fprintf ppf "some (e: Expression) {@,";
    Format.fprintf ppf "%a@]@,})" (pp_print_expr ind) e
  )

let pp_print_constant_declaration_list field ind ppf const_decls =
  const_decls |> List.iteri (fun i ctd ->
    Format.fprintf ppf "@[<v %d>%s.constant_declarations.element[%d] = "
      ind field i;
    Format.fprintf ppf "some (ctd: ConstantDeclaration) {@,";
    Format.fprintf ppf "%a@]@,} &&@,"
      (pp_print_constant_declaration ind) ctd
  )

let pp_print_node_body ind ppf
  {constant_declarations;
   variable_declarations;
   assertions;
   equations;
   properties
  }
=
  assert (properties = []);
  Format.fprintf ppf "nb.constant_declarations.length = %d &&@,"
    (List.length constant_declarations);
  pp_print_constant_declaration_list "nb" ind ppf constant_declarations;
  Format.fprintf ppf "nb.variable_declarations.length = %d &&@,"
    (List.length variable_declarations);
  pp_print_variable_declaration_list ind ppf variable_declarations;
  Format.fprintf ppf "nb.assertions.length = %d &&@,"
    (List.length assertions);
  pp_print_assertion_list ind ppf assertions;
  Format.fprintf ppf "nb.equations.length = %d &&@,"
    (List.length equations);
  pp_print_equation_list ind ppf equations;
  Format.fprintf ppf "nb.properties.length = 0"

let pp_print_node_body_opt ind ppf = function
  | None -> Format.fprintf ppf "nd.body = mk_none<NodeBody>"
  | Some body -> (
    Format.fprintf ppf "@[<v %d>nd.body = mk_some<NodeBody>(" ind;
    Format.fprintf ppf "some (nb: NodeBody) {@,";
    Format.fprintf ppf "%a@]@,})"
      (pp_print_node_body ind) body
  )

let pp_print_comp_impl ind ppf {name; ctype; impl} =
  Format.fprintf ppf "ci.name = \"%s\" &&@," name;
  Format.fprintf ppf "ci.ctype = m.component_types.element[%d] &&@," ctype;
  (match impl with
  | BlockImpl {subcomponents; connections} -> (
    Format.fprintf ppf "ci.kind = ComponentImplKind.Block_Impl &&@,";
    Format.fprintf ppf "@[<v %d>ci.block_impl = some (imp: BlockImpl) {@," ind;
    Format.fprintf ppf "imp.subcomponents.length = %d &&@,"
      (List.length subcomponents);
    pp_print_subcomponent_list ind ppf subcomponents;
    match connections with
    | [] -> Format.fprintf ppf "imp.connections.length = 0"
    | _ -> (
      Format.fprintf ppf "imp.connections.length = %d &&@,"
        (List.length connections);
      pp_print_connection_list ind ppf connections
    )
  )
  | DataflowImpl body -> (
    Format.fprintf ppf "ci.kind = ComponentImplKind.Dataflow_Impl &&@,";
    Format.fprintf ppf "@[<v %d>ci.dataflow_impl = some (nb: NodeBody) {@," ind;
    Format.fprintf ppf "%a" (pp_print_node_body ind) body
  ));
  Format.fprintf ppf "@]@,}"

let pp_print_comp_impl_list ind ppf comp_impl =
  let pp_sep ppf () = Format.fprintf ppf " &&@," in
  Format.pp_print_list ~pp_sep
      (fun ppf (i, ci) ->
         Format.fprintf ppf "@[<v %d>m.component_impl.element[%d] = " ind i;
         Format.fprintf ppf "some (ci: ComponentImpl) {@,";
         Format.fprintf ppf "%a@]@,}"
           (pp_print_comp_impl ind) ci
      )
      ppf
      (List.mapi (fun i ci -> (i, ci)) comp_impl)


let pp_print_input_parameter ind ppf
  ({name; dtype; is_constant}: input_parameter)
=
  Format.fprintf ppf "ip.name = \"%s\" &&@," name;
  Format.fprintf ppf "@[<v %d>ip.dtype = %a" ind
    (pp_print_data_type ind) dtype;
  Format.fprintf ppf " &&@,ip.is_constant = %B" is_constant

let pp_print_input_parameter_list ind ppf input_params =
  input_params |> List.iteri (fun i ip ->
    Format.fprintf ppf "@[<v %d>nd.input_parameters.element[%d] = " ind i;
    Format.fprintf ppf "some (ip: InputParameter) {@,";
    Format.fprintf ppf "%a@]@,} &&@,"
      (pp_print_input_parameter ind) ip
  )

let pp_print_output_parameter ind ppf
  ({name; dtype}: output_parameter)
=
  Format.fprintf ppf "op.name = \"%s\" &&@," name;
  Format.fprintf ppf "@[<v %d>op.dtype = %a" ind
    (pp_print_data_type ind) dtype

let pp_print_output_parameter_list ind ppf output_params =
  output_params |> List.iteri (fun i op ->
    Format.fprintf ppf "@[<v %d>nd.output_parameters.element[%d] = " ind i;
    Format.fprintf ppf "some (op: OutputParameter) {@,";
    Format.fprintf ppf "%a@]@,} &&@,"
      (pp_print_output_parameter ind) op
  )

let pp_print_node ind ppf
  {name;
   is_function;
   is_main;
   input_parameters;
   output_parameters;
   contract;
   body
  }
=
  Format.fprintf ppf "nd.name = \"%s\" &&@," name;
  Format.fprintf ppf "nd.is_function = %B &&@," is_function;
  Format.fprintf ppf "nd.is_main = %B &&@," is_main;
  Format.fprintf ppf "nd.input_parameters.length = %d &&@,"
    (List.length input_parameters);
  pp_print_input_parameter_list ind ppf input_parameters;
  Format.fprintf ppf "nd.output_parameters.length = %d &&@,"
    (List.length output_parameters);
  pp_print_output_parameter_list ind ppf output_parameters;
  pp_print_contract_spec_opt "nd" ind ppf contract;
  Format.fprintf ppf " &&@,";
  pp_print_node_body_opt ind ppf body

let pp_print_node_declaration_list ind ppf node_decls =
  let pp_sep ppf () = Format.fprintf ppf " &&@," in
  Format.pp_print_list ~pp_sep
      (fun ppf (i, nd) ->
         Format.fprintf ppf "@[<v %d>dfm.node_declarations.element[%d] = " ind i;
         Format.fprintf ppf "some (nd: Node) {@,";
         Format.fprintf ppf "%a@]@,}"
           (pp_print_node ind) nd
      )
      ppf
      (List.mapi (fun i nd -> (i, nd)) node_decls)

let pp_print_dataflow_model ind ppf
  {constant_declarations; node_declarations}
=
  Format.fprintf ppf "dfm.type_declarations.length = 0 &&@,";
  Format.fprintf ppf "dfm.constant_declarations.length = %d &&@,"
    (List.length constant_declarations);
  pp_print_constant_declaration_list "dfm" ind ppf constant_declarations;
  Format.fprintf ppf "dfm.contract_declarations.length = 0 &&@,";
  Format.fprintf ppf "dfm.node_declarations.length = %d"
    (List.length node_declarations);
  if node_declarations != [] then Format.fprintf ppf " &&@,";
  pp_print_node_declaration_list ind ppf node_declarations

let pp_print_dataflow_code_opt ind ppf = function
  | None -> Format.fprintf ppf "m.dataflow_code = mk_none<LustreProgram> &&@,";
  | Some dfm -> (
    Format.fprintf ppf "@[<v %d>m.dataflow_code = mk_some<LustreProgram>(" ind;
    Format.fprintf ppf "some (dfm: LustreProgram) {@,";
    Format.fprintf ppf "%a@]@,}) &&@,"
      (pp_print_dataflow_model ind) dfm
  )

let pp_print_str ppf s =
  Format.fprintf ppf "\"%s\"" s

let pp_print_option f t ppf = function
  | Some a -> Format.fprintf ppf "mk_some<%s>(%a)" t f a
  | None -> Format.fprintf ppf "mk_none<%s>()" t

let pp_print_list (f : Format.formatter -> 'a -> unit)
      parent name ind ppf (lst : 'a list) =
  Format.fprintf ppf "%s.%s.length = %d@,"
    parent name (List.length lst);
  List.iteri (fun i elem ->
      Format.fprintf ppf "&& @[<v %d>%s.%s.element[%d] = %a@]@,"
        ind parent name i f elem) lst

let pp_print_threat_intro ind ppf {var; var_type} =
  Format.fprintf ppf "some (ti: ThreatIntro) {@,";
  Format.fprintf ppf "ti.var_name = \"%s\" &&@," var;
  Format.fprintf ppf "ti.var_type = \"%s\"@," var_type;
  Format.fprintf ppf "}"

let pp_print_threat_var_pair ind ppf {left; right} =
  Format.fprintf ppf "some (vp: ThreatVarPair) {@,";
  pp_print_list pp_print_str "vp" "left" ind ppf left;
  Format.fprintf ppf " && ";
  pp_print_list pp_print_str "vp" "right" ind ppf right;
  Format.fprintf ppf "}"

let rec pp_print_threat_expr ind ppf expr =
  Format.fprintf ppf "some (expr: ThreatExpr) {@,";
  let kind, val_fmt = match expr with
    | TEqual var_pair ->
       "Equal", fun () -> Format.fprintf ppf
                            "@[<v %d>expr.equal = %a@,@]" ind
                            (pp_print_threat_var_pair ind) var_pair
    | TContains var_pair ->
       "Contains", fun () -> Format.fprintf ppf
                               "@[<v %d>expr.contains = %a@,@]" ind
                               (pp_print_threat_var_pair ind) var_pair
    | TForall quant ->
       "Forall", fun () -> Format.fprintf ppf
                             "@[<v %d>expr.forall_val = %a@,@]" ind
                             (pp_print_threat_quant ind) quant
    | TExists quant ->
       "Exists", fun () -> Format.fprintf ppf
                             "@[<v %d>expr.exists_val = %a@,@]" ind
                             (pp_print_threat_quant ind) quant
    | TImplies implies ->
       "Implies", fun () -> Format.fprintf ppf
                              "@[<v %d>expr.implies_val = %a@,@]" ind
                              (pp_print_threat_implies ind) implies
    | TOr exprs ->
       "Or", fun () -> pp_print_list (pp_print_threat_expr ind)
                         "expr" "or" ind ppf exprs
    | TAnd exprs ->
       "And", fun () -> pp_print_list (pp_print_threat_expr ind)
                          "expr" "and" ind ppf exprs
    | TNot expr ->
       "Not", fun () -> Format.fprintf ppf
                          "@[<v %d>expr.not = %a@,@]" ind
                          (pp_print_threat_expr ind) expr
  in
  Format.fprintf ppf "expr.kind = ThreatExprKind.%s &&@," kind;
  val_fmt ();
  Format.fprintf ppf "}"

and pp_print_threat_quant ind ppf {intro; expr} =
  Format.fprintf ppf "some (q: ThreatQuantifier) {@,";
  Format.fprintf ppf "q.intro = %a &&@,"
    (pp_print_threat_intro ind) intro;
  Format.fprintf ppf "q.expr = %a@,"
    (pp_print_threat_expr ind) expr;
  Format.fprintf ppf "}"

and pp_print_threat_implies ind ppf {antecedent; consequent} =
  Format.fprintf ppf "some (i: ThreatImplies) {@,";
  Format.fprintf ppf "i.antecedent = %a &&@,"
    (pp_print_threat_expr ind) antecedent;
  Format.fprintf ppf "i.consequent = %a@,"
    (pp_print_threat_expr ind) consequent;
  Format.fprintf ppf "}"

let pp_print_threat_model_body ind ppf
      {id; intro; expr; cia; reference; assumptions;
       description; comment} =
  Format.fprintf ppf "tm.id = \"%s\" &&@," id;
  Format.fprintf ppf "@[<v %d>tm.intro = %a &&@]@,"
    ind (pp_print_threat_intro ind) intro;
  Format.fprintf ppf "@[<v %d>tm.expr = %a &&@]@,"
    ind (pp_print_threat_expr ind) expr;
  Format.fprintf ppf "tm.cia = %a &&@,"
    (pp_print_cyber_cia ind) cia;
  Format.fprintf ppf "@[<v %d>tm.reference = %a &&@]@,"
    ind (pp_print_option pp_print_str "String") reference;
  pp_print_list pp_print_str "tm" "assumptions" ind ppf assumptions;
  Format.fprintf ppf " && @,";
  Format.fprintf ppf "@[<v %d>tm.description = %a &&@]@,"
    ind (pp_print_option pp_print_str "String") description;
  Format.fprintf ppf "@[<v %d>tm.comment = %a@]@,"
    ind (pp_print_option pp_print_str "String") comment

let pp_print_threat_models_list ind ppf threat_models =
  let pp_sep ppf () = Format.fprintf ppf " &&@," in
  Format.pp_print_list ~pp_sep
    (fun ppf (i, threat) ->
      Format.fprintf ppf
        "@[<v %d>m.threat_models.element[%d] = " ind i;
      Format.fprintf ppf "some (tm: ThreatModel) {@,";
      Format.fprintf ppf "%a@]@,}"
        (pp_print_threat_model_body ind) threat
    )
    ppf
    (List.mapi (fun i req -> (i, req)) threat_models)

let pp_print_threat_defense_body ind ppf
      {id; threats; description; comment} =
  Format.fprintf ppf "td.id = \"%s\" &&@," id;
  Format.fprintf ppf "td.description = %a &&@,"
    (pp_print_option pp_print_str "String") description;
  Format.fprintf ppf "td.comment = %a &&@,"
    (pp_print_option pp_print_str "String") comment;
  pp_print_list pp_print_str "td" "threats" ind ppf threats

let pp_print_threat_defenses_list ind ppf threat_defenses =
  let pp_sep ppf () = Format.fprintf ppf " &&@," in
  Format.pp_print_list ~pp_sep
    (fun ppf (i, defense) ->
      Format.fprintf ppf
        "@[<v %d>m.threat_defenses.element[%d] = " ind i;
      Format.fprintf ppf "some (td: ThreatDefense) {@,";
      Format.fprintf ppf "%a@]@,}"
        (pp_print_threat_defense_body ind) defense
    )
    ppf
    (List.mapi (fun i req -> (i, req)) threat_defenses)

let pp_print_mission_body ind ppf
      {id; description; comment; reqs} =
  Format.fprintf ppf "mi.id = \"%s\" && @," id;
  Format.fprintf ppf "mi.description = %a && @,"
    (pp_print_option pp_print_str "String") description;
  Format.fprintf ppf "mi.comment = %a && @,"
    (pp_print_option pp_print_str "String") comment;
  pp_print_list pp_print_str "mi" "reqs" ind ppf reqs

let pp_print_missions_list ind ppf missions =
  let pp_sep ppf () = Format.fprintf ppf " &&@," in
  Format.pp_print_list ~pp_sep
    (fun ppf (i, mission) ->
      Format.fprintf ppf
        "@[<v %d>m.missions.element[%d] = " ind i;
      Format.fprintf ppf "some (mi: Mission) {@,";
      Format.fprintf ppf "%a@]@,}"
        (pp_print_mission_body ind) mission
    )
    ppf
    (List.mapi (fun i req -> (i, req)) missions)

let pp_print_model_body ind ppf
      { name; type_declarations; component_types;
        dataflow_code; comp_impl; cyber_reqs; missions;
        threat_models; threat_defenses }
=
  Format.fprintf ppf "m.name = \"%s\" &&@," name;
  Format.fprintf ppf "m.type_declarations.length = %d &&@,"
    (List.length type_declarations);
  pp_print_type_declaration_list ind ppf type_declarations;
  Format.fprintf ppf "m.component_types.length = %d &&@,"
    (List.length component_types);
  pp_print_comp_types_list ind ppf component_types;
  pp_print_dataflow_code_opt ind ppf dataflow_code;
  Format.fprintf ppf "m.component_impl.length = %d &&@,"
    (List.length comp_impl);
  pp_print_comp_impl_list ind ppf comp_impl;
  Format.fprintf ppf " && @,m.cyber_requirements.length = %d %s@,"
    (List.length cyber_reqs)
    (match cyber_reqs with _ :: _ -> "&&" | [] -> "");
  pp_print_cyber_reqs_list ind ppf cyber_reqs;
  Format.fprintf ppf " && @,m.missions.length = %d %s@,"
    (List.length missions)
    (match missions with _ :: _ -> "&&" | [] -> "");
  pp_print_missions_list ind ppf missions
  (* ;
  Format.fprintf ppf " && @,m.threat_models.length = %d %s@,"
    (List.length threat_models)
    (match threat_models with _ :: _ -> "&&" | [] -> "");
  pp_print_threat_models_list ind ppf threat_models;
  Format.fprintf ppf " && @,m.threat_defenses.length = %d %s@,"
    (List.length threat_defenses)
    (match threat_defenses with _ :: _ -> "&&" | [] -> "");
  pp_print_threat_defenses_list ind ppf threat_defenses
   *)

let pp_print_model ind ppf model =
  Format.fprintf ppf "@[<v %d>model: Model := some (m: Model) {@,%a@]@,};"
    ind (pp_print_model_body ind) model

let pp_print_vdm_iml_indent ind ppf {name; model} =
  Format.fprintf ppf "@[<v>package %s;@,%t@,%a@]"
    name pp_print_imports (pp_print_model ind) model

let pp_print_vdm_iml = pp_print_vdm_iml_indent 2
