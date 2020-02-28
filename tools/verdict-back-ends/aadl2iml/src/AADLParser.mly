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
module A = AADLAst

let mk_pos = Position.mk_position

type pkg_sec_header_item =
  | ImportedUnits of C.pname list

type pkg_sec_body_item =
  | Classifier of A.classifier
  | AnnexLibrary of A.aadl_annex

let mk_package_section header_items body_items =
  (* No partition of header_items is necessary for now *)
  let imported_units =
    header_items |> List.map (fun hi ->
      match hi with
      | ImportedUnits i_units -> i_units
    )
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
    A.classifiers = List.rev classifiers;
    A.annex_libs = List.rev annex_libs;
  }

%}

%token AADLBOOLEAN AADLSTRING AADLINTEGER AADLREAL ENUMERATION
%token PROPERTY SET IS APPLIES TO INHERIT EXTENDS
%token PACKAGE SYSTEM IMPLEMENTATION FEATURES PROPERTIES
%token SUBCOMPONENTS CONNECTIONS
%token PUBLIC PRIVATE
%token TYPE NONE UNITS WITH OUT IN CONSTANT
%token LIST OF
%token DATA PORT
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
  WITH i_units = separated_nonempty_list(",", imported_unit) ";"
  { ImportedUnits i_units }
  (* INCOMPLETE *)

imported_unit: pn = pname { pn } (* aadl2::ModelUnit *)

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
  SYSTEM pid = ident
  fs = features_section
  annexes = list(annex_subclause)
  (* INCOMPLETE *)
  END ID ";"
  {
    { A.name = pid;
      A.ports = fs;
      A.annexes = annexes;
    }
  }

features_section:
  | { [] }
  | FEATURES NONE ";" { [] }
  | FEATURES fs = nonempty_list(feature) { fs }

feature:
  | dp = data_port { dp }
  (* INCOMPLETE *)

data_port:
  pid = port_id; pdir = port_direction; DATA PORT
  qcr = option(qcref); (* aadl2::DataSubcomponentType *)
  prop_assocs = property_associations
  (* INCOMPLETE *)
  ";"
  {
    { A.name = pid;
      A.dir = pdir;
      A.dtype = qcr;
      A.properties = prop_assocs;
    }
  }
  (* INCOMPLETE *)

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
  SYSTEM IMPLEMENTATION
  rlz = realization "." pid = iname
  subcomps = system_subcomponents_section
  connections = connections_section
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
  (* INCOMPLETE *)
  END full_iname ";"
  {
    { A.name = (rlz, pid);
      A.subcomponents = subcomps;
    }
  }

realization: pid = ident { pid } (* aadl2::ComponentType *)

iname: pid = ident { pid }

full_iname: ID "." ID {}

system_subcomponents_section:
  | { [] }
  | SUBCOMPONENTS NONE ";" { [] }
  | SUBCOMPONENTS subcomps = nonempty_list(system_subcomponents_section_item)
    { subcomps }

system_subcomponents_section_item:
  | ss = system_subcomponent { ss }
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
  prop_assocs = contained_property_associations
  (* INCOMPLETE *)
  ";"
  {
    { A.name = pid;
      A.type_ref = type_ref;
      A.properties = prop_assocs;
    }
  }

system_subcomponent:
  pid = subcomponent_id SYSTEM
  type_ref = option(subcomponent_type_ref)
  prop_assocs = contained_property_associations
  (* INCOMPLETE *)
  ";"
  {
    { A.name = pid;
      A.type_ref = type_ref;
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

connections_section:
  | { [] }
  | CONNECTIONS NONE ";" { [] }
  | CONNECTIONS cs = nonempty_list(connection) { cs }

connection:
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
  prop_assocs = component_properties
  (* INCOMPLETE *)
  END ID ";"
  {
    { A.name = pid;
      A.type_extension = ext_qcr;
      A.properties = prop_assocs;
    }
  }

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
  list(property_set_body_item)
  list(annex_subclause)
  END ID ";"
  {
    {A.name = pid;
     A.imported_units = List.flatten i_units;
    }
  }

property_set_imported_units:
  WITH i_units = separated_nonempty_list(",", imported_unit) ";" { i_units }

property_set_body_item:
  | property_type ";" {}
  | property_definition ";" {}
  | property_constant ";" {}

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

boolean_type: ID ":" TYPE unnamed_boolean_type {}

string_type: ID ":" TYPE unnamed_string_type {}

enumeration_type: ID ":" TYPE unnamed_enumeration_type {}

units_type: ID ":" TYPE unnamed_units_type {}

real_type: ID ":" TYPE unnamed_real_type {}

integer_type: ID ":" TYPE unnamed_integer_type {}

property_definition:
  ID ":" option(INHERIT)
  referenced_property_type_or_unnamed_property_type
  option(pair("=>", property_expression))
  APPLIES TO "(" applies_to_element ")"
  {}

applies_to_element:
  | separated_nonempty_list(",", property_owner) {}
  | all_reference {}

property_owner:
  (* qm_reference rule has been expanded here to avoid shift/reduce conflicts *)
  | qm_annex_ref nonempty_list(metaclass_name) {} (* qm_reference *)
  | nonempty_list(metaclass_name) {} (* qm_reference *)
  | qc_reference {}

qm_annex_ref:
  "{" ID "}" "*" "*" {}

qc_reference: fqcref {} (* aadl2::ComponentClassifier *)

fqcref: ID "::" separated_nonempty_list("::", ID) option(pair(".", ID)) {}

metaclass_name:
  | core_keyword {}
  | ID {}

core_keyword:
  | SYSTEM {}
  | CONNECTIONS {}
  | PORT {}
  (* INCOMPLETE *)

(*
qm_reference:
  option(qm_annex_ref)
  nonempty_list(metaclass_name) {}
*)

all_reference: ALL {}

property_constant:
  ID ":" CONSTANT
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

