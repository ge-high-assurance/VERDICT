(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
*)

(** @author Daniel Larraz *)

%{

module C = CommonAstTypes
module A = AADLAst

let mk_pos = Position.mk_position

type pkg_sec_header_item =
  | ImportedUnits of C.pname list
  | PackageRename of (C.pid * C.pname * bool)
  | RenameAll of C.pname

type pkg_sec_body_item =
  | Classifier of A.classifier
  | AnnexLibrary of A.aadl_annex

let mk_package_section header_items body_items =
  let imported_units, renamed_packages =
    List.fold_left (fun (ius, rps) hi ->
      match hi with
      | ImportedUnits i_units -> (i_units :: ius, rps)
      | RenameAll renamed_package ->
        let pkg_rename =
          { A.name = None; A.renamed_package; A.rename_all = true }
        in
        (ius, pkg_rename :: rps)
      | PackageRename (name, renamed_package, rename_all) ->
        (ius, { A.name = Some name; renamed_package; rename_all } :: rps)
    )
    ([], []) header_items
  in
  let classifiers, annex_libs =
    List.fold_left (fun (cls, als) bi ->
      match bi with
      | Classifier cl -> (cl :: cls, als)
      | AnnexLibrary al -> (cls, al :: als)
    )
    ([], []) body_items
  in
  {
    A.imported_units = List.flatten imported_units;
    A.renamed_packages = List.rev renamed_packages;
    A.classifiers = List.rev classifiers;
    A.annex_libs = List.rev annex_libs;
  }

%}

%token AADLBOOLEAN AADLSTRING AADLINTEGER AADLREAL ENUMERATION
%token PROPERTY SET IS APPLIES TO INHERIT EXTENDS
%token PACKAGE SYSTEM ABSTRACT IMPLEMENTATION FEATURES PROPERTIES
%token SUBCOMPONENTS CONNECTIONS CONNECTION
%token PROCESS THREAD SUBPROGRAM PROCESSOR MEMORY DEVICE
%token PROVIDES REQUIRES ACCESS PUBLIC PRIVATE RENAMES
%token TYPE NONE UNITS WITH OUT IN CONSTANT VIRTUAL GROUP EVENT
%token LIST OF
%token DATA PORT BUS
%token <Lexing.lexbuf>ANNEX
%token ANNEX_BLOCK_START "{**"
%token ALL END
%token R_ARROW "->"
%token LR_ARROW "<->"
%token EQ_ARROW "=>"
%token PEQ_ARROW "+=>"
%token DOT_DOT ".."
%token DOUBLE_COLON "::"
%token DOT "."
%token COMMA ","
%token COLON ":"
%token SEMICOLON ";"
%token LCURLY_BRACKET "{"
%token RCURLY_BRACKET "}"
%token LPAREN "("
%token RPAREN ")"
%token LSQUARE_BRACKET "["
%token RSQUARE_BRACKET "]"
%token PLUS "+"
%token MINUS "-"
%token STAR "*"
%token TRUE FALSE
%token <string>ID
%token <string>INTEGER_LIT
%token <string>REAL_LIT
%token <string>STRING
%token EOF

%type<AADLAst.property_expr> property_expression

%start<AADLAst.t> model_unit

%%

model_unit:
  | pkg = aadl_package EOF { A.AADLPackage (mk_pos $startpos, pkg) }
  | ps = property_set EOF { A.PropertySet (mk_pos $startpos, ps) }

(** AADL PACKAGE **)

aadl_package:
  PACKAGE pn = pname
  pbody = package_body
  option(package_properties)
  END pname ";"
  {
    { A.name = pn;
      A.public_sec = fst pbody;
      A.private_sec = snd pbody
    }
  }

ident: id = ID { (mk_pos $startpos, id) }

pname:
  | pid = ident { [pid] }
  | pid = ident "::" pn = pname { pid :: pn }

package_body:
  | pub_sec = public_package_section; priv_sec_opt = option(private_package_section)
    { (Some pub_sec, priv_sec_opt) }
  | priv_sec = private_package_section
    { (None, Some priv_sec) }

public_package_section: PUBLIC p_sec = package_section { p_sec }

private_package_section: PRIVATE p_sec = package_section { p_sec }

package_section:
  header_items = list(package_section_header_item)
  body_items = list(package_section_body_item)
  {
    mk_package_section header_items body_items
  }

package_section_header_item:
  | WITH i_units = separated_nonempty_list(",", imported_unit) ";"
  { ImportedUnits i_units }
  | RENAMES rp = renamed_package ";"
  { RenameAll rp }
  | pid = ident RENAMES PACKAGE rpa = renamed_package_opt_all ";"
  { PackageRename (pid, fst rpa, snd rpa) }

imported_unit: pn = pname { pn } (* aadl2::ModelUnit *)

renamed_package:
  pid = ident "::" rps = renamed_package_suffix
  { pid :: rps }

renamed_package_suffix:
  | (* Empty *) { [] }
  | ALL { [] }
  | pid = ident "::" rps = renamed_package_suffix { pid :: rps }

renamed_package_opt_all:
  pid = ident "::" rpas = renamed_package_opt_all_suffix
  { (pid :: (fst rpas), snd rpas) }

renamed_package_opt_all_suffix:
  | (* Empty *) { ([], true) }
  | ALL { ([], true) }
  | pid = ident { ([pid], false) }
  | pid = ident "::" rpas = renamed_package_opt_all_suffix
    { (pid :: (fst rpas), snd rpas) }

package_section_body_item:
  | c = classifier { Classifier c }
  | a = annex_library { AnnexLibrary a }

classifier:
  | ct = component_type { A.ComponentType ct }
  | ci = component_implementation { A.ComponentImpl ci }
  (* INCOMPLETE *)

component_type:
  | st = system_type { A.SystemType (mk_pos $startpos, st) }
  | dt = data_type { A.DataType (mk_pos $startpos, dt) }
  (* INCOMPLETE *)

system_type:
  system_or_like pid = ident
  fs = sys_features_section
  annexes = list(annex_subclause)
  (* INCOMPLETE *)
  END ID ";"
  {
    { A.name = pid;
      A.ports = fs;
      A.annexes = annexes;
    }
  }

sys_features_section:
  | { [] }
  | FEATURES NONE ";" { [] }
  | FEATURES fs = sys_feature_list { fs }

sys_feature_list:
  | dp = data_port { [dp] }
  | ep = event_port { [ep] }
  | dp = data_port; fs =  sys_feature_list { dp :: fs }
  | ep = event_port; fs =  sys_feature_list { ep :: fs }
  | data_access { [] }
  | data_access; fs =  sys_feature_list { fs }

(*
  | FEATURES fs = nonempty_list(sys_feature) { fs }

sys_feature:
  | dp = data_port { dp }
  | ep = event_port { ep }
  | da = data_access
  {
    let pdir =
      match da.A.adir with
      | A.Requires -> A.In
      | A.Provides -> A.Out
    in
    { A.name = da.A.name;
      A.dir = pdir;
      A.is_event = false;
      A.dtype = da.A.dtype;
      A.properties = da.A.properties;
    }
  }
  (* INCOMPLETE *)
*)

data_port:
  pid = port_id; pdir = port_direction;
  ie = boption(EVENT); DATA PORT
  qcr = option(qcref); (* aadl2::DataSubcomponentType *)
  array_dimension?
  prop_assocs = property_associations
  (* INCOMPLETE *)
  ";"
  {
    { A.name = pid;
      A.dir = pdir;
      A.is_event = ie;
      A.dtype = qcr;
      A.properties = prop_assocs;
    }
  }

event_port:
  pid = port_id; pdir = port_direction; EVENT PORT
  array_dimension?
  prop_assocs = property_associations
  (* INCOMPLETE *)
  ";"
  {
    { A.name = pid;
      A.dir = pdir;
      A.is_event = true;
      A.dtype = None;
      A.properties = prop_assocs;
    }
  }

port_id:
  | pid = ident ":" { pid }
  (* INCOMPLETE *)

port_direction:
  | IN { A.In }
  | IN OUT { A.InOut }
  | OUT { A.Out }

component_implementation:
  | si = system_implementation { A.SystemImpl (mk_pos $startpos, si) }
  | di = data_implementation { A.DataImpl (mk_pos $startpos, di) }
  (* INCOMPLETE *)

system_implementation:
  system_or_like IMPLEMENTATION
  rlz = realization "." pid = iname
  subcomps = system_subcomponents_section
  connections = sys_connections_section
  annexes = list(annex_subclause)
  (* INCOMPLETE *)
  END full_iname ";"
  {
    { A.name = (rlz, pid);
      A.subcomponents = subcomps;
      A.connections = connections;
      A.annexes = annexes;
    }
  }

data_implementation:
  DATA IMPLEMENTATION
  rlz = realization "." pid = iname
  subcomps = data_subcomponents_section
  connections = data_connections_section
  (* INCOMPLETE *)
  END full_iname ";"
  {
    { A.name = (rlz, pid);
      A.subcomponents = subcomps;
    }
  }

data_connections_section:
  | { [] }
  | CONNECTIONS NONE ";" { [] }
  | CONNECTIONS cs = nonempty_list(data_connection) { cs }

data_connection:
  | ac = access_connection { ac }
  (* INCOMPLETE *)

access_connection:
  pid = ident ":"
  access_category
  ACCESS
  src = access_connection_end
  cdir = connection_direction
  dst = access_connection_end
  (* INCOMPLETE *)
  ";"
  {
    ()
  }

access_connection_end:
  ce = connected_element { ce }
  (* INCOMPLETE *)

access_category:
  | DATA {}
  | BUS {}
  | SUBPROGRAM {}
  | SUBPROGRAM GROUP {}
  | VIRTUAL BUS {}

realization: pid = ident { pid } (* aadl2::ComponentType *)

iname: pid = ident { pid }

full_iname: ID "." ID {}

system_subcomponents_section:
  | { [] }
  | SUBCOMPONENTS NONE ";" { [] }
  | SUBCOMPONENTS subcomps = nonempty_list(system_subcomponents_section_item)
    { List.filter (fun { A.category } -> A.is_system category) subcomps }

system_subcomponents_section_item:
  | ss = system_subcomponent { ss }
  | ds = data_subcomponent { ds }
  (* INCOMPLETE *)

data_subcomponents_section:
  | { [] }
  | SUBCOMPONENTS NONE ";" { [] }
  | SUBCOMPONENTS subcomps = nonempty_list(data_subcomponents_section_item)
    { subcomps }

data_subcomponents_section_item:
  | ds = data_subcomponent { ds }
  (* INCOMPLETE *)

data_subcomponent:
  pid = subcomponent_id DATA
  type_ref = option(subcomponent_type_ref)
  array_dimension?
  prop_assocs = contained_property_associations
  (* INCOMPLETE *)
  ";"
  {
    { A.name = pid;
      A.type_ref = type_ref;
      A.category = A.Data;
      A.properties = prop_assocs;
    }
  }

array_dimension:
  "[" array_size? "]" {}

array_size:
  | int_value {}
  | qpref {}

int_value: INTEGER_LIT {}

system_subcomponent:
  pid = subcomponent_id system_or_like
  type_ref = option(subcomponent_type_ref)
  prop_assocs = contained_property_associations
  (* INCOMPLETE *)
  ";"
  {
    { A.name = pid;
      A.type_ref = type_ref;
      A.category = A.System;
      A.properties = prop_assocs;
    }
  }

subcomponent_id:
  | pid = ident ":" { pid }
  (* INCOMPLETE *)

subcomponent_type_ref:
  qcr = qcref { qcr } (* aadl2::SystemSubcomponentType *)
  (* INCOMPLETE *)

qcref:
  | pn = pname
  {
    (pn, None)
  }
  | pn = pname "." pid = ident
  {
    (pn, Some pid)
  }

sys_connections_section:
  | { [] }
  | CONNECTIONS NONE ";" { [] }
  | CONNECTIONS cs = sys_connection_list { cs }

sys_connection_list:
  | c = sys_connection
  { [ c ] }
  | access_connection { [] }
  | c = sys_connection; cs = sys_connection_list
  { c :: cs }
  | access_connection; cs = sys_connection_list
  { cs }

sys_connection:
  | pc = port_connection { pc }
  (* INCOMPLETE *)

port_connection:
  pct = port_connection_type prop_assocs = property_associations ";"
  {
    { pct with A.properties = prop_assocs }
  }
  (* INCOMPLETE *)

port_connection_type:
  pid = ident ":" PORT
  src = abstract_connection_end
  cdir = connection_direction
  dst = abstract_connection_end
  {
    { A.name = pid;
      A.dir = cdir;
      A.src = src;
      A.dst = dst;
      A.properties = [];
    }
  }
  (* INCOMPLETE *)

connection_direction:
  | "->" { A.Unidirectional }
  | "<->" { A.Bidirectional }

abstract_connection_end:
  ce = connected_element { ce }
  (* INCOMPLETE *)

connected_element:
 | pid = ident
 {
   (None, pid)
 }
 | ctx = ident "." pid = ident
 {
   (Some ctx, pid)
 }
 (* aadl2::Context - aadl2::ConnectionEnd *)

contained_property_associations:
 | { [] }
 | "{" ps = nonempty_list(contained_property_association) "}" { ps }

contained_property_association:
  pa = property_association { pa }
  (* INCOMPLETE *)

property_associations:
 | { [] }
 | "{" ps = nonempty_list(property_association) "}" { ps }

property_association:
  qpr = qpref
  bop = binding_symbol const = boption(CONSTANT)
  mp_vals = separated_nonempty_list(",", optional_modal_property_value)
  ";"
  {
    { A.name = qpr;
      A.bop = bop;
      A.const = const;
      A.value = match mp_vals with [v] -> v | _ -> A.ListTerm mp_vals ;
    }
  }
  (* INCOMPLETE *)

qpref:
  | pid = ident { (None, pid) }
  | cxt = ident "::" pid = ident { (Some cxt, pid) }

binding_symbol:
  | "=>" { A.SetBinding }
  | "+=>" { A.AppendBinding }

optional_modal_property_value:
  pe = property_expression { pe }
  (* INCOMPLETE *)

property_expression:
  (*
  | reference_term {}
  | record_term {}
  | component_classifier_term {}
  | computed_term {}
  | numeric_range_term {}
  *)
  | t = string_term { t }
  | t = real_term { t }
  | t = integer_term { t }
  | t = list_term { t }
  | t = boolean_literal { t }
  | t = literal_or_reference_term { t }

string_term: nq_str = no_quote_string { nq_str }

no_quote_string: str = STRING { A.StringLit str }

real_term:
  sr = signed_real u = option(unit_literal)
  {
    A.RealTerm
      {C.sign = fst sr;
       C.lit = snd sr;
       C.units = u;
      }
  }

integer_term:
  si = signed_int u = option(unit_literal)
  {
    A.IntegerTerm
      {C.sign = fst si;
       C.lit = snd si;
       C.units = u;
      }
  }

signed_int:
  s = plus_minus_opt lit = INTEGER_LIT { (s, lit) }

signed_real:
  s = plus_minus_opt lit = REAL_LIT { (s, lit) }

plus_minus_opt:
  | { C.NoSign }
  | s = plus_minus { s }

plus_minus:
  | "+" { C.Positive }
  | "-" { C.Negative }

unit_literal: pid = ident { pid }

list_term:
  "(" l = separated_list(",", property_expression) ")" { A.ListTerm l }

boolean_literal:
  | TRUE { A.BooleanLit true }
  | FALSE { A.BooleanLit false }

literal_or_reference_term: qpr = qpref { A.LiteralOrReference qpr }

annex_library: a = annex_header "{**" ";" { a }

annex_subclause: a = annex_header "{**" ";" { a }

annex_header:
  lexbuf = ANNEX annex = ID
  {
    (* Don't parse annex if next token is not ANNEX_BLOCK_START *)
    if Lexing.lexeme lexbuf = "{**" then (
      match annex with
      | "agree" ->
	 begin
	   let agree_ast = AGREEParser.agree_annex AGREELexer.token lexbuf in
           A.AGREEAnnex (mk_pos $startpos, agree_ast)
	 end
      | "verdict" ->
	 begin
	   let verdict_ast = VerdictParser.verdict_annex VerdictLexer.token lexbuf in
	   A.VerdictAnnex (mk_pos $startpos, verdict_ast)
	 end
      | _ ->
	 begin
	   AnnexLexer.ignore_annex lexbuf;
	   UnsupportedAnnex (mk_pos $startpos, annex)
	 end
    )
    else UnsupportedAnnex (mk_pos $startpos, "")
  }
  (* INCOMPLETE *)

package_properties:
  PROPERTIES NONE ";" {}
  (* INCOMPLE *)

data_type:
  DATA pid = ident; ext_qcr = option(type_extension);
  fs = data_features_section
  prop_assocs = component_properties
  (* INCOMPLETE *)
  END ID ";"
  {
    { A.name = pid;
      A.type_extension = ext_qcr;
      A.features = fs;
      A.properties = prop_assocs;
    }
  }

data_features_section:
  | { [] }
  | FEATURES NONE ";" { [] }
  | FEATURES fs = nonempty_list(data_feature) { fs }

data_feature:
  | da = data_access { da }
  (* INCOMPLETE *)

data_access:
  pid = ident ":" ad = access_direction;
  DATA ACCESS qcr = option(qcref) (* aadl2::DataSubcomponentType *)
  prop_assocs = component_properties
  (* INCOMPLETE *)
  ";"
  {
    { A.name = pid;
      A.adir = ad;
      A.dtype = qcr;
      A.properties = prop_assocs;
    }
  }

access_direction:
  | REQUIRES { A.Requires }
  | PROVIDES { A.Provides }

type_extension:
  EXTENDS qcr = qcref { qcr }

component_properties:
  | { [] }
  | PROPERTIES NONE ";" { [] }
  | PROPERTIES ps = nonempty_list(contained_property_association) { ps }

(** PROPERTY SETS **)

property_set:
  PROPERTY SET pid = ident IS
  i_units = list(property_set_imported_units)
  decls = list(property_set_body_item)
  list(annex_subclause)
  END ID ";"
  {
    {A.name = pid;
     A.imported_units = List.flatten i_units;
     A.declarations = decls;
    }
  }

property_set_imported_units:
  WITH i_units = separated_nonempty_list(",", imported_unit) ";" { i_units }

property_set_body_item:
  | property_type ";" { A.UnsupportedDecl }
  | def = property_definition ";" { A.PropertyDef def }
  | property_constant ";" { A.UnsupportedDecl }

property_type:
  | boolean_type {}
  | string_type {}
  | enumeration_type {}
  | units_type {}
  | real_type {}
  | integer_type {}
  (*| range_type {}
  | classifier_type {}
  | reference_type {}
  | record_type {}*)

boolean_type: ident ":" TYPE unnamed_boolean_type {}

string_type: ident ":" TYPE unnamed_string_type {}

enumeration_type: ident ":" TYPE unnamed_enumeration_type {}

units_type: ident ":" TYPE unnamed_units_type {}

real_type: ident ":" TYPE unnamed_real_type {}

integer_type: ident ":" TYPE unnamed_integer_type {}

property_definition:
  pid = ident ":" inh = boption(INHERIT)
  referenced_property_type_or_unnamed_property_type
  odv = opt_default_value
  APPLIES TO "(" ate = applies_to_element ")"
  {
    { A.name = pid;
      A.is_inheritable = inh;
      A.default_value = odv;
      A.applies_to = ate;
    }
  }

opt_default_value:
  | { None }
  | "=>" v = property_expression { Some v }

applies_to_element:
  | l = separated_nonempty_list(",", property_owner)
  {
    l |> List.fold_left (fun acc e ->
      match acc, e with
      | A.All, _ -> A.All
      | A.System, A.Connection -> A.All
      | A.System, _ -> A.System
      | A.Connection, A.System -> A.All
      | A.Connection, _ -> A.Connection
      | A.Other, _ -> e
    )
    A.Other
  }
  | all_reference
  { A.All }

property_owner:
  (* qm_reference rule has been expanded here to avoid shift/reduce conflicts *)
  | qm_annex_ref metaclass_name { A.Other } (* qm_reference *)
  | mn = metaclass_name { mn } (* qm_reference *)
  | qc_reference { A.Other }

qm_annex_ref:
  "{" ID "}" "*" "*" {}

qc_reference: fqcref {} (* aadl2::ComponentClassifier *)

fqcref: ID "::" separated_nonempty_list("::", ID) option(pair(".", ID)) {}

metaclass_name:
  | ck = core_keyword { ck }
  | ID { A.Other }

core_keyword:
  | system_or_like { A.System }
  | CONNECTION { A.Connection }
  | PORT { A.Other }
  (* INCOMPLETE *)

system_or_like:
  | SYSTEM {}
  | ABSTRACT {}
  | PROCESS {}
  | THREAD GROUP? {}
  | SUBPROGRAM GROUP? {}
  | DEVICE {}
  | VIRTUAL? BUS {}
  | VIRTUAL? PROCESSOR {}
  | MEMORY {}

(*
qm_reference:
  option(qm_annex_ref)
  nonempty_list(metaclass_name) {}
*)

all_reference: ALL {}

property_constant:
  ident ":" CONSTANT
  referenced_property_type_or_unnamed_property_type
  "=>" constant_property_expression
  {}

referenced_property_type_or_unnamed_property_type:
  | referenced_property_type {}
  | unnamed_property_type {}

referenced_property_type: qpref {} (* aadl2::PropertyType *)

unnamed_property_type:
  | list_type {}
  | unnamed_boolean_type {}
  | unnamed_string_type {}
  | unnamed_enumeration_type {}
  | unnamed_units_type {}
  | unnamed_real_type {}
  | unnamed_integer_type {}
  (*| unnamed_range_type {}
  | unnamed_classifier_type {}
  | unnamed_reference_type {}
  | unnamed_record_type {}*)

list_type:
  LIST OF referenced_property_type_or_unnamed_property_type {}

unnamed_boolean_type:
  AADLBOOLEAN {}

unnamed_string_type:
  AADLSTRING {}

unnamed_enumeration_type:
  ENUMERATION "(" separated_nonempty_list(",", enumeration_literal) ")" {}

enumeration_literal: ID {}

unnamed_units_type:
  UNITS
  "("
  unit_literal
  option(pair(",", separated_nonempty_list(",", unit_literal_conversion)))
  ")"
  {}

unit_literal_conversion:
  ID "=>" ID "*" number_value {}

number_value:
  | signed_real {}
  | signed_int {}

unnamed_real_type:
  AADLREAL option(real_range) option(units) {}

real_range: real_range_bound ".." real_range_bound {}

real_range_bound:
  | real_term {}
  | signed_constant {}
  | constant_value {}

unnamed_integer_type:
  AADLINTEGER option(integer_range) option(units) {}

integer_range: integer_range_bound ".." integer_range_bound {}

integer_range_bound:
  | integer_term {}
  | signed_constant {}
  | constant_value {}

signed_constant:
  plus_minus constant_value {}

constant_value: qpref {} (* aadl2::PropertyConstant *)

units:
  | unnamed_units_type {}
  | UNITS qpref {} (* aadl2::UnitsType *)

constant_property_expression:
  (*
  | record_term {}
  | component_classifier_term {}
  | computed_term {}
  | numeric_range_term {}
  *)
  | string_term {}
  | real_term {}
  | integer_term {}
  | list_term {}
  | boolean_literal {}
  | literal_or_reference_term {}

