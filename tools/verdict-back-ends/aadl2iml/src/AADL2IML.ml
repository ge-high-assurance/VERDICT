(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
*)

(** @author Daniel Larraz *)

module C = CommonAstTypes
module A = AADLAst

let pp_print_id ppf (_, id) = Format.fprintf ppf "%s" id

let pp_print_pname ppf pn =
  let pp_sep ppf () = Format.fprintf ppf "." in
  Format.fprintf ppf "%a"
    (Format.pp_print_list ~pp_sep:pp_sep pp_print_id) pn

let pp_print_imports ppf =
  Format.fprintf ppf "@,import iml.utils.*;@,import iml.verdict.*;@,"

let pp_print_port_dir ppf = function
  | A.In -> Format.fprintf ppf "In"
  | A.Out -> Format.fprintf ppf "Out"
  | A.InOut -> Format.fprintf ppf "InOut"

let pp_print_data_port ind ppf { A.name; A.dir; A.dtype } =
  Format.fprintf ppf "p.name = \"%a\" &&@," pp_print_id name;
  Format.fprintf ppf "p.mode = PortMode.%a &&@," pp_print_port_dir dir;
  Format.fprintf ppf "p.ptype = mk_none"

let pp_print_data_port_list ind ppf ports =
  ports |> List.iteri (fun i dp ->
    Format.fprintf ppf "@[<v %d>ct.ports.element[%d] = " ind i;
    Format.fprintf ppf "some(p: Port) {@,";
    Format.fprintf ppf "%a@]@,} &&@,"
      (pp_print_data_port ind) dp
  )

let pp_print_system_type ind ppf { A.name; A.ports } =
  Format.fprintf ppf "ct.name = \"%a\" &&@," pp_print_id name;
  Format.fprintf ppf "ct.ports.length = %d &&@," (List.length ports);
  pp_print_data_port_list ind ppf ports;
  Format.fprintf ppf "ct.contract = mk_none"

let pp_print_system_type_list ind ppf sys_types =
  sys_types |> List.iteri (fun i st ->
    Format.fprintf ppf "@[<v %d>m.component_types.element[%d] = " ind i;
    Format.fprintf ppf "some (ct: ComponentType) {@,";
    Format.fprintf ppf "%a@]@,} &&@,"
      (pp_print_system_type ind) st
  )

let pp_print_full_iname ppf (realization, iname) =
  Format.fprintf ppf "%a.%a"
    pp_print_id realization pp_print_id iname

let pp_print_comp_type_reference ppf (ct_name, sys_types) =
  let ep =
    Utils.element_position (fun {A.name; A.ports} -> A.equal_ids name ct_name) sys_types
  in
  (match ep with
   | None -> assert false
   | Some (_, i) -> Format.fprintf ppf "m.component_types.element[%d]" i
  )

let tryToMatchEnumLiteral = function
  | "Xdata" -> "FlowType.Xdata"
  | "Control" -> "FlowType.Control"
  | "Request" -> "FlowType.Request"
  | other -> other

let pp_print_qpref ppf = function
  | None, pid ->  Format.fprintf ppf "%s" (tryToMatchEnumLiteral (snd pid))
  | Some ctx, pid -> (
    Format.fprintf ppf "%a.%a" pp_print_id ctx pp_print_id pid
  )

let pp_print_sign ppf = function
  | C.NoSign -> ()
  | C.Positive -> Format.fprintf ppf "+"
  | C.Negative -> Format.fprintf ppf "-"

let pp_print_numeric_literal ppf { C.sign; C.lit; C.units } =
  assert (units = None);
  Format.fprintf ppf "%a%s" pp_print_sign sign lit

let pp_print_property_expr ppf = function
  | A.StringLit str -> Format.fprintf ppf "\"%s\"" str
  | A.RealTerm nl -> pp_print_numeric_literal ppf nl
  | A.IntegerTerm nl -> pp_print_numeric_literal ppf nl
  | A.ListTerm l -> assert false;
  | A.BooleanLit b -> Format.fprintf ppf (if b then "true" else "false")
  | A.LiteralOrReference qpr -> pp_print_qpref ppf qpr 

let pp_print_property_field ppf (_, id) =
  let is_uppercase c =
    let code = Char.code c in
    Char.code 'A' <= code && code <= Char.code 'Z'
  in
  (* From camelCase to snake_case *)
  let prev_was_capital = ref false in
  id |> String.iter (fun c ->
    if is_uppercase c then (
      (if !prev_was_capital then
        Format.pp_print_char ppf (Char.lowercase_ascii c)
      else
        Format.fprintf ppf "_%c" (Char.lowercase_ascii c));
      prev_was_capital := true
    )
    else (
      Format.pp_print_char ppf c;
      prev_was_capital := false
    )
  )

let pp_print_property_assoc inst ppf {A.name; A.bop; A.const; A.value } =
  let ctx =
    match fst name with
    | Some c -> c
    | _ -> assert false
  in
  assert (not const && bop = A.SetBinding && snd ctx = "VERDICT_Properties");
  Format.fprintf ppf "%s.%a = %a &&@,"
    inst pp_print_property_field (snd name) pp_print_property_expr value

let pp_print_system_subcomponent ind sys_types ppf 
  { A.name; A.type_ref; A.properties }
=
  Format.fprintf ppf "ci.name = \"%a\" &&@," pp_print_id name;
  let qcr =
    match type_ref with
    | Some qcr -> qcr
    | _ -> assert false
  in
  assert (snd qcr = None);
  Format.fprintf ppf "ci.kind = ComponentInstanceKind.Specification &&@,";
  let pp_sep ppf () = Format.fprintf ppf "" in
  Format.pp_print_list ~pp_sep (pp_print_property_assoc "ci") ppf properties;
  let ct_name =
    match qcr with
    | ([pid], None) -> pid
    | _ -> assert false
  in
  Format.fprintf ppf "ci.specification = %a"
    pp_print_comp_type_reference (ct_name, sys_types)

let pp_print_system_subcomponent_list ind sys_types ppf subcomponents =
  subcomponents |> List.iteri (fun i sub ->
    Format.fprintf ppf "@[<v %d>imp.subcomponents.element[%d] = " ind i;
    Format.fprintf ppf "some (ci: ComponentInstance) {@,";
    Format.fprintf ppf "%a@]@,} &&@,"
      (pp_print_system_subcomponent ind sys_types) sub
  )

let pp_print_comp_port_reference sys_types ppf (ct_name, port) =
  let ep =
    Utils.element_position (fun {A.name; A.ports} -> A.equal_ids name ct_name) sys_types
  in
  (match ep with
   | None -> assert false
   | Some ({A.ports}, i) ->
     let ep =
       Utils.element_position (fun {A.name; A.dtype } -> A.equal_ids name port) ports
     in
     (match ep with
      | None -> assert false
      | Some (_, j) ->
        Format.fprintf ppf "m.component_types.element[%d].ports.element[%d]" i j
     )
  )

let pp_print_subcomp_port_reference sys_types subcomponents ppf (comp_inst, port) =
  let ep =
    Utils.element_position
      (fun {A.name; A.type_ref} -> A.equal_ids name comp_inst)
      subcomponents
  in
  (match ep with
   | None -> assert false
   | Some ({A.type_ref}, i) ->
     Format.fprintf ppf "sp.subcomponent = m.component_impl.element[%d] &&@," i;
     let ct_name = match type_ref with
       | Some ([name], None) -> name
       | _ -> assert false
     in
     Format.fprintf ppf "sp.port = %a"
       (pp_print_comp_port_reference sys_types) (ct_name, port)
  )

let pp_print_connection_end ind inst impl sys_types subcomponents ppf = function
  | None, port -> (
    Format.fprintf ppf "%s.kind = ConnectionEndKind.ComponentCE &&@," inst;
    Format.fprintf ppf "%s.component_port = %a"
      inst (pp_print_comp_port_reference sys_types) (impl, port)
  )
  | Some comp_inst, port -> (
    Format.fprintf ppf "%s.kind = ConnectionEndKind.SubcomponentCE &&@," inst;
    Format.fprintf ppf "@[<v %d>%s.subcomponent_port = " ind inst;
    Format.fprintf ppf "some (sp: CompInstPort) {@,";
    Format.fprintf ppf "%a@]@,}"
      (pp_print_subcomp_port_reference sys_types subcomponents) (comp_inst, port)
  ) 

let pp_print_connection ind impl sys_types subcomponents ppf
  { A.name; A.dir; A.src; A.dst; A.properties } =
  assert (dir = A.Unidirectional);
  Format.fprintf ppf "c.name = \"%a\" &&@," pp_print_id name;
  let pp_sep ppf () = Format.fprintf ppf "" in
  Format.pp_print_list ~pp_sep (pp_print_property_assoc "c") ppf properties;
  Format.fprintf ppf "@[<v %d>c.source = some (src: ConnectionEnd) {@," ind;
  Format.fprintf ppf "%a@]@,} &&@,"
    (pp_print_connection_end ind "src" impl sys_types subcomponents) src;
  Format.fprintf ppf "@[<v %d>c.destination = some (dst: ConnectionEnd) {@," ind;
  Format.fprintf ppf "%a@]@,}"
    (pp_print_connection_end ind "dst" impl sys_types subcomponents) dst

let pp_print_connection_list ind impl sys_types subcomponents ppf connections =
  connections |> List.iteri (fun i conn ->
    Format.fprintf ppf " &&@,@[<v %d>imp.connections.element[%d] = " ind i;
    Format.fprintf ppf "some (c: Connection) {@,";
    Format.fprintf ppf "%a@]@,}"
      (pp_print_connection ind impl sys_types subcomponents) conn
  )

let pp_print_system_impl ind sys_types ppf { A.name; A.subcomponents; A.connections } =
  Format.fprintf ppf "ci.name = \"%a\" &&@," pp_print_full_iname name;
  Format.fprintf ppf "ci.ctype = %a &&@,"
    pp_print_comp_type_reference (fst name, sys_types);
  let len_subcomponents = List.length subcomponents in
  let len_connections = List.length connections in
  assert (len_subcomponents>0);
  Format.fprintf ppf "ci.kind = ComponentImplKind.Block_Impl &&@,";
  Format.fprintf ppf "@[<v %d>ci.block_impl = some (imp: BlockImpl) {@," ind;
  Format.fprintf ppf "imp.subcomponents.length = %d &&@," len_subcomponents;
  pp_print_system_subcomponent_list ind sys_types ppf subcomponents;
  Format.fprintf ppf "imp.connections.length = %d" len_connections;
  pp_print_connection_list ind (fst name) sys_types subcomponents ppf connections;
  Format.fprintf ppf "@]@,}"

let pp_print_system_impl_list ind sys_types ppf sys_impls =
  sys_impls |> List.iteri (fun i si ->
    Format.fprintf ppf "@[<v %d>m.component_impl.element[%d] = " ind i;
    Format.fprintf ppf "some (ci: ComponentImpl) {@,";
    Format.fprintf ppf "%a@]@,} &&@,"
      (pp_print_system_impl ind sys_types) si
  )

let pp_print_pkg_sec ind ppf { A.classifiers } =
  let comp_types, comp_impls =
    List.fold_left (fun (cts, cis) -> function
      | A.ComponentType ct -> (ct :: cts, cis)
      | A.ComponentImpl ci -> (cts, ci :: cis)
    )
    ([], []) classifiers
  in
  let sys_types, data_types =
    List.fold_left (fun (sts, dts) -> function
      | A.SystemType (_, st) -> (st :: sts, dts)
      | A.DataType _ -> (sts, dts)
    )
    ([], []) comp_types
  in
  let sys_impls, data_impls =
    List.fold_left (fun (sis, dis) -> function
      | A.SystemImpl (_, si) -> (si :: sis, dis)
      | A.DataImpl _ -> (sis, dis)
    )
    ([], []) comp_impls
  in
  Format.fprintf ppf "m.type_declarations.length = 0 &&@,";
  Format.fprintf ppf "m.component_types.length = %d &&@,"
    (List.length comp_types);
  pp_print_system_type_list ind ppf sys_types;
  Format.fprintf ppf "m.component_impl.length = %d &&@,"
    (List.length comp_impls);
  pp_print_system_impl_list ind sys_types ppf sys_impls; 
  Format.fprintf ppf "m.dataflow_code = mk_none"
  

let pp_print_pkg_sec_opt ind ppf = function
  | None -> ()
  | Some pkg_sec -> (
    Format.fprintf ppf "@[<v %d>model: Model := some (m: Model) {@,%a@]@,};"
     ind (pp_print_pkg_sec ind) pkg_sec
  )

let pp_print_aadl_ast_as_iml_indent ind ppf = function
  | A.AADLPackage (_, { A.name ; A.public_sec }) ->
    Format.fprintf ppf "@[<v>package %a;@,%t@,%a@]"
      pp_print_pname name pp_print_imports
      (pp_print_pkg_sec_opt ind) public_sec
  | _ -> ()

let pp_print_aadl_ast_as_iml = pp_print_aadl_ast_as_iml_indent 2

let pp_print_aadl_input_as_iml ppf input =
  List.iter (pp_print_aadl_ast_as_iml ppf) input

