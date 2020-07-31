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

let is_subtype type_decls t1 t2 =
  match t1, t2 with
  | UserDefinedType idx1, UserDefinedType idx2 -> (
    match (List.nth type_decls idx1).parent with
    | None -> false
    | Some parent_idx -> parent_idx=idx2
  )
  | _, _ -> t1=t2

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

let pp_print_type_declaration ind ppf ({name; definition; _}: type_declaration) =
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
  | ToInt -> "ToInt", "to_int"
  | ToReal -> "ToReal", "to_real"
  | Event -> "Event", "event"

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

let pp_print_port ind ppf { name; mode; is_event; ptype; probe } =
  Format.fprintf ppf "p.name = \"%s\" &&@," name;
  Format.fprintf ppf "p.mode = PortMode.%a &&@," pp_print_port_mode mode;
  Format.fprintf ppf "p.is_event = %B &&@," is_event;
  Format.fprintf ppf "p.probe = %B &&@," probe;
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
  ({name; dtype; definition; _}: symbol_definition)
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

let pp_print_cyber_cia _ ppf cia =
  Format.fprintf ppf "CIA.%s"
    (match cia with
           | CyberC -> "Confidentiality"
           | CyberI -> "Integrity"
           | CyberA -> "Availability")

let pp_print_safety_ia _ ppf ia =
  Format.fprintf ppf "IA.%s"
    (match ia with
           | SafetyI -> "Integrity"
           | SafetyA -> "Availability")

let pp_print_cyber_severity _ ppf severity =
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

let pp_print_safety_port ind ppf {name; ia} =
  Format.fprintf ppf "some (port: IAPort) {@,";
  Format.fprintf ppf "port.name = \"%s\" &&@," name;
  Format.fprintf ppf "port.ia = %a@,"
    (pp_print_safety_ia ind) ia;
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

let rec pp_print_safety_expr ind ppf expr =
  Format.fprintf ppf "some (expr: SafetyExpr) {@,";
  let kind, val_fmt = match expr with
    | SafetyPort port
      -> "Port", fun () -> (Format.fprintf ppf
                              "@[<v %d>expr.port = %a@]@," ind
                              (pp_print_safety_port ind) port)

    | SafetyFault id
      -> "Fault", fun () -> (Format.fprintf ppf
                              "@[<v %d>expr.fault = \"%s\"@]@," ind id)
    | SafetyAnd es
      -> "And", fun ()
                -> pp_print_safety_expr_list "expr" "and" ind ppf es
    | SafetyOr es
      -> "Or", fun ()
               -> pp_print_safety_expr_list "expr" "or" ind ppf es
    | SafetyNot e
      -> "Not", fun () -> (Format.fprintf ppf
                             "@[<v %d>expr.not = %a@]@," ind
                             (pp_print_safety_expr ind) e) in
  Format.fprintf ppf "expr.kind = SafetyExprKind.%s &&@," kind;
  val_fmt ();
  Format.fprintf ppf "}@,"

and pp_print_safety_expr_list name lst ind ppf exprs =
  Format.fprintf ppf
    "%s.%s.length = %d &&@," name lst (List.length exprs);
  let pp_sep ppf () = Format.fprintf ppf " &&@," in
  Format.pp_print_list ~pp_sep
    (fun ppf (i, expr) ->
      Format.fprintf ppf "@[<v %d>%s.%s.element[%d] = %a@]"
        ind name lst i (pp_print_safety_expr ind) expr
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

let pp_print_safety_req_body ind ppf
      {id; condition; target_probability; comment; description} =
  Format.fprintf ppf "req.id = \"%s\" &&@," id;
  Format.fprintf ppf "req.targetProbability = \"%s\" &&@," target_probability;
  Format.fprintf ppf "@[<v %d>req.condition = %a &&@]@," ind
    (pp_print_safety_expr ind) condition;
  pp_print_opt ppf "req.comment = \"%s\" &&@," comment;
  pp_print_opt ppf "req.description = \"%s\"@," description

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

let pp_print_safety_rel_body ind ppf
  {id; output; faultSrc; comment; description} =
  Format.fprintf ppf "rel.id = \"%s\" &&@," id;
  Format.fprintf ppf "@[<v %d>rel.output = %a &&@]@," ind
    (pp_print_safety_port ind) output;
  (match faultSrc with
  | Some expr -> Format.fprintf ppf
                   "@[<v %d>rel.faultSrc = %a &&@]@," ind
                   (pp_print_safety_expr ind) expr
  | None -> ());
  pp_print_opt ppf "rel.comment = \"%s\" &&@," comment;
  pp_print_opt ppf "rel.description = \"%s\"@," description

let pp_print_safety_event_body _ ppf
  {id; probability; comment; description} =
  Format.fprintf ppf "ev.id = \"%s\" &&@," id;
  Format.fprintf ppf "ev.probability = \"%s\" &&@," probability;
  pp_print_opt ppf "ev.comment = \"%s\" &&@," comment;
  pp_print_opt ppf "ev.description = \"%s\"@," description

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

let pp_print_safety_reqs_list ind ppf safety_reqs =
  let pp_sep ppf () = Format.fprintf ppf " &&@," in
  Format.pp_print_list ~pp_sep
    (fun ppf (i, req) ->
      Format.fprintf ppf "@[<v %d>m.safety_requirements.element[%d] = " ind i;
      Format.fprintf ppf "some (req: SafetyReq) {@,";
      Format.fprintf ppf "%a@]@,}"
        (pp_print_safety_req_body ind) req
    )
    ppf
    (List.mapi (fun i req -> (i, req)) safety_reqs)

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

let pp_print_safety_rels_list ind ppf safety_rels =
  let pp_sep ppf () = Format.fprintf ppf " &&@," in
  Format.pp_print_list ~pp_sep
    (fun ppf (i, rel) ->
      Format.fprintf ppf "@[<v %d>ct.safety_relations.element[%d] = " ind i;
      Format.fprintf ppf "some (rel: SafetyRel) {@,";
      Format.fprintf ppf "%a@]@,}"
        (pp_print_safety_rel_body ind) rel
    )
    ppf
    (List.mapi (fun i rel -> (i, rel)) safety_rels)

let pp_print_safety_events_list ind ppf safety_events =
  let pp_sep ppf () = Format.fprintf ppf " &&@," in
  Format.pp_print_list ~pp_sep
    (fun ppf (i, ev) ->
      Format.fprintf ppf "@[<v %d>ct.safety_events.element[%d] = " ind i;
      Format.fprintf ppf "some (ev: SafetyEvent) {@,";
      Format.fprintf ppf "%a@]@,}"
        (pp_print_safety_event_body ind) ev
    )
    ppf
    (List.mapi (fun i ev -> (i, ev)) safety_events)

(*let pp_print_bool_prop_value ppf = function
  | None -> Format.fprintf ppf "mk_none<Bool>"
  | Some v -> Format.fprintf ppf "mk_some<Bool>(%B)" v

let pp_print_int_prop_value ppf = function
  | None -> Format.fprintf ppf "mk_none<Int>"
  | Some i -> Format.fprintf ppf "mk_some<Int>(%s)" i *)

let pp_print_string_prop_value ppf = function
  | None -> Format.fprintf ppf "mk_none<String>"
  | Some s -> Format.fprintf ppf "mk_some<String>(\"%s\")" s

let pp_print_component_type ind ppf
  {name; ports; contract; cyber_rels; safety_rels; safety_events}
=
  Format.fprintf ppf "ct.name = \"%s\" &&@," name;
  Format.fprintf ppf "ct.ports.length = %d &&@," (List.length ports);
  pp_print_port_list ind ppf ports;
  Format.fprintf ppf "ct.compCateg = %a &&@,"
    pp_print_string_prop_value (Some "system");
  pp_print_contract_spec_opt "ct" ind ppf contract;
  Format.fprintf ppf " &&@,ct.cyber_relations.length = %d %s@,"
    (List.length cyber_rels)
    (match cyber_rels with _ :: _ -> "&&" | [] -> "");
  pp_print_cyber_rels_list ind ppf cyber_rels;
  Format.fprintf ppf " &&@,ct.safety_relations.length = %d %s@,"
    (List.length safety_rels)
    (match safety_rels with _ :: _ -> "&&" | [] -> "");
  pp_print_safety_rels_list ind ppf safety_rels;
  Format.fprintf ppf " &&@,ct.safety_events.length = %d %s@,"
    (List.length safety_events)
    (match safety_events with _ :: _ -> "&&" | [] -> "");
  pp_print_safety_events_list ind ppf safety_events

let pp_print_comp_types_list ind ppf comp_types =
  comp_types |> List.iteri (fun i ct ->
    Format.fprintf ppf "@[<v %d>m.component_types.element[%d] = " ind i;
    Format.fprintf ppf "some (ct: ComponentType) {@,";
    Format.fprintf ppf "%a@]@,} &&@,"
      (pp_print_component_type ind) ct
  )


let pp_print_generic_attribute _ ppf { name; atype; value } =
  Format.fprintf ppf "ga.name = \"%s\" &&@," name;
  Format.fprintf ppf "ga.atype = %s &&@,"
    (match atype with
     | Integer -> "Int"
     | Real -> "Real"
     | Bool -> "Bool"
     | String -> "String"
    );
  Format.fprintf ppf "ga.value = \"%s\"@," value

let pp_print_attributes ind ppf obj attributes =
  Format.fprintf ppf "%s.attributes.length = %d"
    obj (List.length attributes);
  attributes |> List.iteri (fun i ga ->
    Format.fprintf ppf " &&@, @[<v %d>%s.attributes.element[%d] = " ind obj i;
    Format.fprintf ppf "some (ga: GenericAttribute) {@,";
    Format.fprintf ppf "%a@]@,}"
      (pp_print_generic_attribute ind) ga
  )


let pp_print_comp_instance ind ppf ({name; itype; attributes}) =
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
  pp_print_attributes ind ppf "ci" attributes

let pp_print_subcomponent_list ind ppf subcomponents =
  subcomponents |> List.iteri (fun i sub ->
    Format.fprintf ppf "@[<v %d>imp.subcomponents.element[%d] = " ind i;
    Format.fprintf ppf "some (ci: ComponentInstance) {@,";
    Format.fprintf ppf "%a@]@,} &&@,"
      (pp_print_comp_instance ind) sub
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

let pp_print_connection ind ppf
  {name; attributes; source; destination }
=
  Format.fprintf ppf "c.name = \"%s\" &&@," name;
  pp_print_attributes ind ppf "c" attributes;
  Format.fprintf ppf " &&@,";
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
  {constant_declarations; node_declarations; _}
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
  | None -> Format.fprintf ppf "mk_none<%s>" t

let pp_print_list (f : Format.formatter -> 'a -> unit)
      parent name ind ppf (lst : 'a list) =
  Format.fprintf ppf "%s.%s.length = %d@,"
    parent name (List.length lst);
  List.iteri (fun i elem ->
      Format.fprintf ppf "&& @[<v %d>%s.%s.element[%d] = %a@]@,"
        ind parent name i f elem) lst

(*let pp_print_threat_intro _ ppf {var; var_type} =
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
    (List.mapi (fun i req -> (i, req)) threat_defenses) *)

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
        dataflow_code; comp_impl; cyber_reqs; safety_reqs; missions; _ }
=
  Format.fprintf ppf "m.name = \"%s\" &&@," name;
  Format.fprintf ppf "m.type_declarations.length = %d &&@,"
    (List.length type_declarations);
  pp_print_type_declaration_list ind ppf type_declarations;
  Format.fprintf ppf "m.component_types.length = %d &&@,"
    (List.length component_types);
  pp_print_comp_types_list ind ppf component_types;
  pp_print_dataflow_code_opt ind ppf dataflow_code;
  Format.fprintf ppf "m.component_impl.length = %d %s@,"
    (List.length comp_impl)
    (match comp_impl with [] -> "" | _ -> "&&");
  pp_print_comp_impl_list ind ppf comp_impl;
  Format.fprintf ppf " && @,m.cyber_requirements.length = %d %s@,"
    (List.length cyber_reqs)
    (match cyber_reqs with _ :: _ -> "&&" | [] -> "");
  pp_print_cyber_reqs_list ind ppf cyber_reqs;
  Format.fprintf ppf " && @,m.safety_requirements.length = %d %s@,"
    (List.length safety_reqs)
    (match safety_reqs with _ :: _ -> "&&" | [] -> "");
  pp_print_safety_reqs_list ind ppf safety_reqs;
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
