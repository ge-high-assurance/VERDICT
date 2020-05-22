(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
*)

(** @author Daniel Larraz *)

open CommonAstTypes

type qpref = pid option * pid

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

type access_dir = Requires | Provides

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

type data_feature = {
  name: pid;
  adir: access_dir;
  dtype: qcref option;
}

type data_type = {
  name: pid;
  type_extension: qcref option;
  features: data_feature list;
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

type connection_end = qpref

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

(* ------------------------------------------------------------------------- *)

let equal_ids (_,id1) (_,id2) =
  String.lowercase_ascii id1 = String.lowercase_ascii id2

let equal_qprefs qpr1 qpr2 =
  match qpr1, qpr2 with
  | (None, pid1), (None, pid2) ->
    equal_ids pid1 pid2
  | (Some pkg1, pid1), (Some pkg2, pid2) ->
    (equal_ids pkg1 pkg2) && (equal_ids pid1 pid2)
  | _ -> false

let mk_full_qpref pkg id =
  (Some (Position.dummy_pos, pkg), (Position.dummy_pos, id))

let find_assoc qpr prop_assocs =
  List.find_opt
    (fun ({name} : property_association) -> equal_qprefs qpr name)
    prop_assocs

let is_component_type = function
  | ComponentType _ -> true
  | _ -> false

let is_system_type = function
  | SystemType _ -> true
  | _ -> false

let is_aadl_package = function
  | AADLPackage _ -> true
  | _ -> false

let is_agree_annex = function
  | AGREEAnnex _ -> true
  | _ -> false

let is_verdict_annex = function
  | VerdictAnnex _ -> true
  | _ -> false

let is_system = function
  | System -> true
  | _ -> false

let is_data = function
  | Data -> true
  | _ -> false

let get_imported_units = function
  | AADLPackage (_, { public_sec; private_sec }) -> (
    match public_sec, private_sec with
    | None, None -> []
    | None, Some { imported_units } -> imported_units
    | Some { imported_units }, None -> imported_units
    | Some pkg_sec1, Some pkg_sec2 -> (
      List.rev_append pkg_sec2.imported_units pkg_sec1.imported_units
    )
  )
  | PropertySet (_, { imported_units }) -> imported_units


let rec pp_print_imported_unit_list ppf = function
  | [] ->
      ()
  | [iu] ->
      Format.fprintf ppf "with %a;@,@," pp_print_pname iu
  | iu :: ius -> (
      Format.fprintf ppf "with %a;@," pp_print_pname iu;
      pp_print_imported_unit_list ppf ius
  )

let pp_print_renamed_package ppf { name; renamed_package; rename_all } =
  match name with
  | None ->
    Format.fprintf ppf "renames %a::all;" pp_print_pname renamed_package
  | Some id ->
    Format.fprintf ppf "%a renames package %a%t;"
      pp_print_id id
      pp_print_pname renamed_package
      (fun ppf -> if rename_all then Format.fprintf ppf "::all" else ())

let rec pp_print_renamed_packages_list ppf = function
  | [] ->
      ()
  | [rp] -> Format.fprintf ppf "%a@,@," pp_print_renamed_package rp
  | rp :: rps -> (
      Format.fprintf ppf "%a@," pp_print_renamed_package rp;
      pp_print_renamed_packages_list ppf rps
  )

let pp_print_port_dir ppf = function
  | In -> Format.fprintf ppf "in"
  | Out -> Format.fprintf ppf "out"
  | InOut -> Format.fprintf ppf "in out"

let pp_print_qcref_opt ppf = function
  | None -> ()
  | Some qcref ->
    Format.fprintf ppf " %a" pp_print_qcref qcref

let pp_print_port ppf { name; dir; dtype } =
  Format.fprintf ppf "%a: %a data port%a;"
    pp_print_id name pp_print_port_dir dir
    pp_print_qcref_opt dtype

let pp_print_qpref ppf = function
  | None, pid -> pp_print_id ppf pid
  | Some ctx, pid -> (
    Format.fprintf ppf "%a::%a" pp_print_id ctx pp_print_id pid
  )

let pp_print_const_modifier ppf const =
  if const then Format.fprintf ppf "constant " else ()

let pp_print_binding_op ppf = function
  | SetBinding -> Format.fprintf ppf "=>"
  | AppendBinding -> Format.fprintf ppf "+=>"

let rec pp_print_value ppf = function
  | StringLit str ->
      Format.fprintf ppf "\"%s\"" str
  | RealTerm nl -> pp_print_numeric_literal ppf nl
  | IntegerTerm nl -> pp_print_numeric_literal ppf nl
  | ListTerm l ->
      (*let pp_sep ppf () = Format.fprintf ppf ",@ " in
      Format.fprintf ppf "@[(%a)@]"*)
      let pp_sep ppf () = Format.fprintf ppf ", " in
      Format.fprintf ppf "(%a)"
        (Format.pp_print_list ~pp_sep pp_print_value) l
  | BooleanLit b ->
      Format.fprintf ppf (if b then "true" else "false")
  | LiteralOrReference qpr ->
      pp_print_qpref ppf qpr

let pp_print_property ppf { name; bop; const; value } =
  Format.fprintf ppf "%a %a %a%a;"
    pp_print_qpref name pp_print_binding_op bop
    pp_print_const_modifier const
    pp_print_value value

let pp_print_braced_properties ind ppf = function
  | [] -> ()
  | properties -> (
    let pp_sep ppf () = Format.fprintf ppf "@," in
    Format.fprintf ppf "@,{@[<v %d>@,%a@]@,}" ind
      (Format.pp_print_list ~pp_sep pp_print_property) properties
  )

let pp_print_properties ind ppf = function
  | [] -> ()
  | properties -> (
    let pp_sep ppf () = Format.fprintf ppf "@," in
    Format.fprintf ppf "@,@[<v %d>properties@,%a@]" ind
       (Format.pp_print_list ~pp_sep pp_print_property) properties
  )

let pp_print_features ind ppf = function
  | [] -> ()
  | ports -> (
    let pp_sep ppf () = Format.fprintf ppf "@," in
    Format.fprintf ppf "@[<v %d>features@,%a@]" ind
       (Format.pp_print_list ~pp_sep pp_print_port) ports
  )

let pp_print_annex ind ppf annex =
  let name, pp_print_annex_body =
    match annex with
    | AGREEAnnex (_, body) ->
      ("agree", (fun ppf -> AGREEAst.pp_print_ast_indent ind ppf body))
    | VerdictAnnex (_, body) ->
      ("verdict", fun ppf -> VerdictAst.pp_print_ast_indent ind ppf body)
    | _ -> assert false
  in
  Format.fprintf ppf "@[<v %d>annex %s {**@,%t@]@,**};"
    ind name pp_print_annex_body

let is_unsupported_annex = function
  | UnsupportedAnnex _ -> true
  | _ -> false

let pp_print_annexes ind ppf annexes =
  let annexes =
    List.filter (fun a -> not (is_unsupported_annex a)) annexes
  in
  match annexes with
  | [] -> ()
  | _ -> (
    let pp_sep ppf () = Format.fprintf ppf "@," in
    Format.fprintf ppf "@,%a"
      (Format.pp_print_list ~pp_sep (pp_print_annex ind)) annexes
  )

let pp_print_system_type ind ppf { name; ports; annexes } =
  Format.fprintf ppf "@[<v %d>system %a@,%a%a@]@,end %a;" ind
    pp_print_id name
    (pp_print_features ind) ports
    (pp_print_annexes ind) annexes
    pp_print_id name

let pp_print_data_type ind ppf ({ name; properties }: data_type) =
  Format.fprintf ppf "@[<v %d>data %a%a@]@,end %a;" ind
    pp_print_id name
    (pp_print_properties ind) properties
    pp_print_id name

let pp_print_component_type ind ppf = function
  | SystemType (_, st) -> pp_print_system_type ind ppf st
  | DataType (_, dt) -> pp_print_data_type ind ppf dt

let pp_print_full_iname ppf (realization, iname) =
  Format.fprintf ppf "%a.%a"
    pp_print_id realization pp_print_id iname

let pp_print_subcomponent ind comp_type ppf { name; type_ref; properties } =
  Format.fprintf ppf "%a: %s%a%a;"
    pp_print_id name comp_type pp_print_qcref_opt type_ref
    (pp_print_braced_properties ind) properties

let pp_print_subcomponents ind comp_type ppf = function
  | [] -> ()
  | subcomps -> (
    let pp_sep ppf () = Format.fprintf ppf "@," in
    Format.fprintf ppf "@[<v %d>subcomponents@,%a@]" ind
       (Format.pp_print_list ~pp_sep
          (pp_print_subcomponent ind comp_type)) subcomps
  )

let pp_print_connection_dir ppf = function
  | Unidirectional -> Format.fprintf ppf "->"
  | Bidirectional -> Format.fprintf ppf "<->"

let pp_print_connection_end ppf = function
  | None, pid -> pp_print_id ppf pid
  | Some ctx, pid -> (
    Format.fprintf ppf "%a.%a" pp_print_id ctx pp_print_id pid
  )

let pp_print_port_connection ind ppf { name; dir; src; dst; properties } =
  Format.fprintf ppf "%a: port %a %a %a%a;"
    pp_print_id name
    pp_print_connection_end src
    pp_print_connection_dir dir
    pp_print_connection_end dst
    (pp_print_braced_properties ind) properties

let pp_print_connections ind ppf = function
  | [] -> ()
  | connections -> (
    let pp_sep ppf () = Format.fprintf ppf "@," in
    Format.fprintf ppf "@,@[<v %d>connections@,%a@]" ind
       (Format.pp_print_list ~pp_sep (pp_print_port_connection ind))
       connections
  )

let pp_print_system_impl ind ppf { name; subcomponents; connections } =
  Format.fprintf ppf "@[<v %d>system implementation %a@,%a%a@]@,end %a;" ind
    pp_print_full_iname name
    (pp_print_subcomponents ind "system") subcomponents
    (pp_print_connections ind) connections
    pp_print_full_iname name

let pp_print_data_impl ind ppf { name; subcomponents } =
  Format.fprintf ppf "@[<v %d>data implementation %a@,%a@]@,end %a;" ind
    pp_print_full_iname name
    (pp_print_subcomponents ind "data") subcomponents
    pp_print_full_iname name

let pp_print_component_impl ind ppf = function
  | SystemImpl (_, si) -> pp_print_system_impl ind ppf si
  | DataImpl (_, di)-> pp_print_data_impl ind ppf di

let pp_print_classifier ind ppf = function
  | ComponentType ct -> pp_print_component_type ind ppf ct
  | ComponentImpl ci -> pp_print_component_impl ind ppf ci

let pp_print_pkg_sec ind ppf { imported_units; renamed_packages; classifiers; annex_libs } =
  let pp_sep ppf () = Format.fprintf ppf "@,@," in
  Format.fprintf ppf "%a%a%a%a%a"
    pp_print_imported_unit_list imported_units
    pp_print_renamed_packages_list renamed_packages
    (Format.pp_print_list ~pp_sep (pp_print_annex ind))
    annex_libs
    (fun ppf (l1, l2) ->
       if l1 = [] then ()
       else (if l2 = [] then Format.fprintf ppf "@,"
             else Format.fprintf ppf "@,@,")
    )
    (annex_libs, classifiers)
    (Format.pp_print_list ~pp_sep (pp_print_classifier ind))
    classifiers

let pp_print_pkg_sec_opt ind ppf = function
  | _, None -> ()
  | sec_header, Some pkg_sec ->
    Format.fprintf ppf "@[<v %d>%s@,%a@]@," ind
      sec_header (pp_print_pkg_sec ind) pkg_sec

let pp_print_model_unit ind ppf = function
  | AADLPackage (_, { name ; public_sec ; private_sec }) -> (
    Format.fprintf ppf "@[<v>package %a@,%a%aend %a;@]"
      pp_print_pname name
      (pp_print_pkg_sec_opt ind) ("public", public_sec)
      (pp_print_pkg_sec_opt ind) ("private", private_sec)
      pp_print_pname name
  )
  | PropertySet (_, {name}) -> (
    Format.fprintf ppf "@[<v>property set %a is@,end %a;@]"
      pp_print_id name pp_print_id name
  )

let pp_print_ast_indent = pp_print_model_unit

let pp_print_ast = pp_print_ast_indent 4

