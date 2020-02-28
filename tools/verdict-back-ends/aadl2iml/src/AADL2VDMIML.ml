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

module AD = AADLAst
module AG = AGREEAst
module VE = VerdictAst
module C  = CommonAstTypes
module VI = VDMIML

let pp_print_pname_as_iml ppf pn =
  let pp_sep ppf () = Format.fprintf ppf "." in
  Format.pp_print_list ~pp_sep C.pp_print_id ppf pn

let aadl_pname_to_iml_pname pn =
  Format.asprintf "%a" pp_print_pname_as_iml pn

let data_repr_qpref = AD.mk_full_qpref "Data_Model" "Data_Representation"

let enumerators_qpref = AD.mk_full_qpref "Data_Model" "Enumerators"

let app_property_set = "VERDICT_Properties"

let get_bool_prop_value qpr properties =
  match AD.find_assoc qpr properties with
  | None -> None
  | Some {AD.value} -> (
    match value with
    | AD.BooleanLit b -> Some b
    | _ -> assert false
  )

let get_int_prop_value qpr properties =
  match AD.find_assoc qpr properties with
  | None -> None
  | Some {AD.value} -> (
    match value with
    | AD.IntegerTerm nt -> Some (C.numeric_literal_to_string nt)
    | _ -> assert false
  )

let get_string_prop_value qpr properties =
  match AD.find_assoc qpr properties with
  | None -> None
  | Some {AD.value} -> (
    match value with
    | AD.StringLit str -> Some str
    | _ -> assert false
  )

let get_enumerators properties =
  match AD.find_assoc enumerators_qpref properties with
  | None -> failwith ("No Enumerators property association was found")
  | Some {AD.value} -> (
    let get_string = function
      | AD.StringLit str -> str
      | _ ->  failwith ("An enum value that is not string was found")
    in
    match value with
    | AD.ListTerm l -> List.map get_string l
    | _ -> failwith ("Enumerators value is not a list")
  )

let equal_ids id1 id2 =
  String.lowercase_ascii id1 = String.lowercase_ascii id2

let get_data_type type_decls = function
  | ([pid], _) -> (
    let id = C.get_id pid in
    let ep =
      Utils.element_position
        (fun ({VI.name}: VI.type_declaration) -> equal_ids name id)
        type_decls
    in
    match ep with
    | None -> assert false
    | Some (_, pos) -> VI.UserDefinedType pos
  )
  | (pname, _) -> (
    match String.lowercase_ascii (C.pname_to_string pname) with
    | "base_types::boolean" -> VI.PlainType VI.Bool
    | "base_types::integer" -> VI.PlainType VI.Int
    | "base_types::integer_8" -> VI.PlainType VI.Int
    | "base_types::integer_16" -> VI.PlainType VI.Int
    | "base_types::integer_32" -> VI.PlainType VI.Int
    | "base_types::integer_64" -> VI.PlainType VI.Int
    | "base_types::unsigned_8" -> VI.PlainType VI.Int
    | "base_types::unsigned_16" -> VI.PlainType VI.Int
    | "base_types::unsigned_32" -> VI.PlainType VI.Int
    | "base_types::unsigned_64" -> VI.PlainType VI.Int
    | "base_types::natural" -> VI.PlainType VI.Int
    | "base_types::float" -> VI.PlainType VI.Real
    | "base_types::float_32" -> VI.PlainType VI.Real
    | "base_types::float_64" -> VI.PlainType VI.Real
    | _ -> failwith "Unsupported data type found" 
  )

let data_to_type_decls data_types data_impls =
  let type_decls_with_extension_id =
    let add_type ({AD.name; AD.type_extension; AD.properties}: AD.data_type) =
      match AD.find_assoc data_repr_qpref properties with
      | None -> ({VI.name = C.get_id name; VI.definition = None}, type_extension)
      | Some {AD.value} -> (
        match value with
        | AD.LiteralOrReference (None, (_, id)) when (equal_ids id "Enum") -> (
          let enumerators = get_enumerators properties in
          let enum_type = VI.EnumType enumerators in
          ({VI.name = C.get_id name; VI.definition = Some enum_type}, None)
        )
        | _ -> failwith ("Only Enum data definitions are supported by now")
      )
    in
    List.map add_type data_types
  in
  let type_decls = List.map fst type_decls_with_extension_id in
  let type_decls =
    let set_type_aliases ((type_decl, ext_id) : (VI.type_declaration * C.qcref option)) =
      match ext_id with
      | None -> type_decl
      | Some qcr ->
        { VI.name = type_decl.VI.name;
          VI.definition = Some (get_data_type type_decls qcr)
        }
    in
    List.map set_type_aliases type_decls_with_extension_id
  in
  let process_data_impl type_decls {AD.name; AD.subcomponents} =
    let type_name = C.get_id (fst name) in
    let process_subcomponent {AD.name; AD.type_ref} =
      match type_ref with
      | None -> failwith ("Record field definition without a type is not supported")
      | Some qcr -> (C.get_id name, get_data_type type_decls qcr)
    in
    let record_fields =
      List.map process_subcomponent subcomponents
    in
    let rec update_type l: VI.type_declaration list -> VI.type_declaration list =
    function
      | [] -> failwith ("Data implementation found without data type declaration")
      | { VI.name; VI.definition } :: tl when (equal_ids name type_name) -> (
        let td: VI.type_declaration =
          { name; VI.definition = Some (VI.RecordType record_fields) }
        in
        List.rev_append (td :: l) tl
      )
      | t :: tl -> update_type (t::l) tl
    in
    update_type [] type_decls
  in
  List.fold_left process_data_impl type_decls data_impls

let aadl_dir_to_iml_mode = function
  | AD.In -> VI.In
  | AD.Out -> VI.Out
  | AD.InOut -> failwith "Input-Output ports are not supported"

let aadl_port_to_iml_port type_decls {AD.name; AD.dir; AD.dtype; AD.properties } =
  {VI.name = C.get_id name;
   VI.mode = aadl_dir_to_iml_mode dir;
   VI.ptype = (
     match dtype with
     | None -> None
     | Some qcr -> Some (get_data_type type_decls qcr)
   );
   VI.probe = (
     let qpr = AD.mk_full_qpref app_property_set "probe" in
     match get_bool_prop_value qpr properties with
     | None -> false
     | Some v -> v
   ) 
  }

let agree_data_type_to_data_type type_decls = function
  | AG.UserType qcr -> get_data_type type_decls qcr
  | AG.IntType None -> VI.PlainType VI.Int
  | AG.IntType (Some (lb, ub)) ->
    let lb_str = C.numeric_literal_to_string lb in
    let ub_str =  C.numeric_literal_to_string ub in
    VI.SubrangeType (lb_str, ub_str)
  | AG.RealType _ -> VI.PlainType VI.Real
  | AG.BoolType -> VI.PlainType VI.Bool

let agree_binary_op_to_iml_binary_op = function
  | AG.Arrow -> VI.Arrow
  | AG.Impl -> VI.Impl
  | AG.Equiv -> VI.Equiv
  | AG.Or -> VI.Or
  | AG.And -> VI.And
  | AG.Lt -> VI.Lt
  | AG.Lte -> VI.Lte
  | AG.Gt -> VI.Gt
  | AG.Gte -> VI.Gte
  | AG.Eq -> VI.Eq
  | AG.Neq -> VI.Neq
  | AG.Plus -> VI.Plus
  | AG.Minus -> VI.Minus
  | AG.Times -> VI.Times
  | AG.Div -> VI.Div
  | AG.IntDiv -> VI.IntDiv
  | AG.Mod -> VI.Mod
  | AG.Exp -> failwith "AGREE ^ operator is not supported for now"

let agree_unary_op_to_iml_unary_op = function
  | AG.Not -> VI.Not
  | AG.UMinus -> VI.UMinus
  | AG.Pre -> VI.Pre
  | AG.FloorCast -> VI.ToInt
  | AG.RealCast -> VI.ToReal

let rec agree_expr_to_iml_expr = function
  | AG.BinaryOp (_, op, e1, e2) ->
    let op = agree_binary_op_to_iml_binary_op op in
    let e1 = agree_expr_to_iml_expr e1 in
    let e2 = agree_expr_to_iml_expr e2 in
    VI.BinaryOp (op, e1, e2)
  | AG.UnaryOp (_, op, e) ->
    let op = agree_unary_op_to_iml_unary_op op in
    let e = agree_expr_to_iml_expr e in
    VI.UnaryOp (op, e)
  | AG.Ite (_, e1, e2, e3) ->
    let e1 = agree_expr_to_iml_expr e1 in
    let e2 = agree_expr_to_iml_expr e2 in
    let e3 = agree_expr_to_iml_expr e3 in
    VI.Ite (e1, e2, e3)
  | AG.Proj (_, e, field) ->
    VI.Proj (agree_expr_to_iml_expr e, C.get_id field)
  | AG.Ident pn -> VI.Ident (C.pname_to_string pn)
  | AG.EnumValue (_, _, value) -> VI.Ident (C.get_id value)
  | AG.IntegerLit (_, nl) -> VI.IntegerLit nl
  | AG.RealLit (_, nl) -> VI.RealLit nl
  | AG.Call (_, e, args) ->
    let name =
      match agree_expr_to_iml_expr e with
      | VI.Ident id -> id
      | _ -> assert false
    in
    VI.Call (name, List.map agree_expr_to_iml_expr args)
  | AG.RecordExpr (_, e, fields) -> (
    let fields = fields |> List.map (fun (id, e) ->
        (C.get_id id, agree_expr_to_iml_expr e)
      )
    in
    match agree_expr_to_iml_expr e with
    | VI.Ident id -> VI.RecordExpr (id, fields)
    | VI.Proj (VI.Ident id, field) -> (
      let name = Format.asprintf "%s.%s" id field in
      VI.RecordExpr (name, fields)
    )
    | _ -> assert false
  )
  | AG.True _ -> VI.True
  | AG.False _ -> VI.False

let const_decls_to_symbol_defs type_decls (const_decls: AG.const_statement list) =
  let const_decl_to_symbol_def { AG.name; AG.dtype; AG.definition } =
    {VI.name = C.get_id name;
     VI.is_constant = true;
     VI.dtype = agree_data_type_to_data_type type_decls dtype;
     VI.definition = agree_expr_to_iml_expr definition;
    }
  in
  List.map const_decl_to_symbol_def const_decls

let var_decls_to_symbol_defs type_decls var_decls =
  let var_decl_to_symbol_def {AG.vars; AG.definition} =
    let def =
      match definition with
      | None -> assert false
      | Some def -> def
    in
    let name, dtype =
      match vars with
      | [arg] -> arg
      | [] -> assert false;
      | _ -> failwith "Only single variable definitions are allowed for now"
    in
    {VI.name = C.get_id name;
     VI.is_constant = false;
     VI.dtype = agree_data_type_to_data_type type_decls dtype;
     VI.definition = agree_expr_to_iml_expr def;
    }
  in
  var_decls
  |> List.filter (fun ({AG.definition}: AG.eq_statement) -> definition != None)
  |> List.map var_decl_to_symbol_def

let agree_contract_item_to_iml_contract_item c_items =
  let agree_contract_item_to_iml_contract_item { AG.id; AG.desc; AG.spec; } =
    ({VI.name = Some desc;
      VI.expression = agree_expr_to_iml_expr spec;
    } : VI.contract_item)
  in
  List.map agree_contract_item_to_iml_contract_item c_items

let agree_annex_to_contract_spec type_decls annex =
  let const_decls, var_decls, assumes, guarantees =
    List.fold_left (fun (cds, vds, ass, gts) -> function
      | AG.NamedSpecStatement (_, AG.Assume a) ->
        (cds, vds, a :: ass, gts)
      | AG.NamedSpecStatement (_, AG.Guarantee g) ->
        (cds, vds, ass, g :: gts)
      | AG.NamedSpecStatement (_, AG.Lemma l) ->
        (cds, vds, ass, l :: gts)
      | AG.ConstStatement (_, c) ->
        (c :: cds, vds, ass, gts)
      | AG.EqStatement (_, v) ->
        (cds, v :: vds, ass, gts)
      | AG.AssignStatement _
      | AG.NodeDefinition _
      | AG.AssertStatement _ -> assert false
    )
    ([], [], [], []) (List.rev annex)
  in
  {
    VI.constant_declarations =
      const_decls_to_symbol_defs type_decls const_decls;
    VI.variable_declarations =
      var_decls_to_symbol_defs type_decls var_decls;
    VI.assumes =
      agree_contract_item_to_iml_contract_item assumes;
    VI.guarantees =
      agree_contract_item_to_iml_contract_item guarantees;
    VI.modes = [];
    VI.imports = [];
  }

let verdict_cyber_cia_to_iml = function
  | VE.CIA_C -> VI.CyberC
  | VE.CIA_I -> VI.CyberI
  | VE.CIA_A -> VI.CyberA

let verdict_safety_ia_to_iml = function
  | VE.IA_I -> VI.SafetyI
  | VE.IA_A -> VI.SafetyA

let opt_bind f = function
  | Some x -> Some (f x)
  | None -> None

let verdict_cyber_severity_to_iml = function
  | VE.Severity_None -> VI.CyberNone
  | VE.Severity_Minor -> VI.CyberMinor
  | VE.Severity_Major -> VI.CyberMajor
  | VE.Severity_Hazardous -> VI.CyberHazardous
  | VE.Severity_Catastrophic -> VI.CyberCatastrophic

let verdict_cyber_port_to_iml (name, cia) =
  {VI.name; VI.cia = verdict_cyber_cia_to_iml cia}

let rec verdict_cyber_expr_to_iml = function
  | VE.LPort port -> VI.CyberPort (verdict_cyber_port_to_iml port)
  | VE.LAnd exprs ->
     VI.CyberAnd (List.map verdict_cyber_expr_to_iml exprs)
  | VE.LOr exprs ->
     VI.CyberOr (List.map verdict_cyber_expr_to_iml exprs)
  | VE.LNot expr -> VI.CyberNot (verdict_cyber_expr_to_iml expr)

let verdict_safety_port_to_iml (name, ia) =
  {VI.name; VI.ia = verdict_safety_ia_to_iml ia}

let rec verdict_safety_expr_to_iml = function
  | VE.SLPort port -> VI.SafetyPort (verdict_safety_port_to_iml port)
  | VE.SLFault id -> VI.SafetyFault id
  | VE.SLAnd exprs ->
     VI.SafetyAnd (List.map verdict_safety_expr_to_iml exprs)
  | VE.SLOr exprs ->
     VI.SafetyOr (List.map verdict_safety_expr_to_iml exprs)
  | VE.SLNot expr -> VI.SafetyNot (verdict_safety_expr_to_iml expr)

let verdict_cyber_req_to_iml
      id cia severity condition comment description phases extern =
  let open VI in
  {
    id;
    cia = opt_bind verdict_cyber_cia_to_iml cia;
    severity = verdict_cyber_severity_to_iml severity;
    condition = verdict_cyber_expr_to_iml condition;
    comment; description; phases; extern;
  }

let verdict_safety_req_to_iml
      id condition target_probability comment description =
  let open VI in
  {
    id; target_probability;
    condition = verdict_safety_expr_to_iml condition;
    comment; description
  }

let verdict_cyber_rel_to_iml
      id output inputs comment description phases extern =
  let open VI in
  {
    id;
    output = verdict_cyber_port_to_iml output;
    inputs =
      (match inputs with
       | Some expr -> Some (verdict_cyber_expr_to_iml expr)
       | None -> None);
    comment; description; phases; extern;
  }

let verdict_safety_rel_to_iml
      id output faultSrc comment description =
  let open VI in
  {
    id;
    output = verdict_safety_port_to_iml output;
    faultSrc =
      (match faultSrc with
       | Some expr -> Some (verdict_safety_expr_to_iml expr)
       | None -> None);
    comment; description
  }

let verdict_safety_event_to_iml
      id probability comment description =
  let open VI in
  {
    id; probability; comment; description
  }

let verdict_annex_to_cyber_rels annex =
  let open VE in
  List.fold_left
    begin
      fun acc st ->
      match st with
      | CyberRel
        {id; output; inputs; comment; description; phases; extern}
        -> (verdict_cyber_rel_to_iml id output inputs comment description phases extern) :: acc
      | _ -> acc
    end [] annex

let verdict_annex_to_safety_rels annex =
  let open VE in
  List.fold_left
    begin
      fun acc st ->
      match st with
      | SafetyRel
        {id; output; faultSrc; comment; description}
        -> (verdict_safety_rel_to_iml id output faultSrc comment description) :: acc
      | _ -> acc
    end [] annex

let verdict_annex_to_safety_events annex =
  let open VE in
  List.fold_left
    begin
      fun acc st ->
      match st with
      | SafetyEvent
        {id; probability; comment; description}
        -> (verdict_safety_event_to_iml id probability comment description) :: acc
      | _ -> acc
    end [] annex

let verdict_annex_to_cyber_reqs annex =
  let open VE in
  List.fold_left
    begin
      fun acc st ->
      match st with
      | CyberReq
        {id; cia; severity; condition; comment; description; phases; extern}
        -> (verdict_cyber_req_to_iml id cia severity condition comment description phases extern) :: acc
      | _ -> acc
    end [] annex

let verdict_annex_to_safety_reqs annex =
  let open VE in
  List.fold_left
    begin
      fun acc st ->
      match st with
      | SafetyReq
        {id; condition; target_probability; comment; description}
        -> (verdict_safety_req_to_iml id condition target_probability comment description) :: acc
      | _ -> acc
    end [] annex

let system_types_to_iml_comp_types type_decls sys_types =
  sys_types |> List.map
  (fun {AD.name; AD.ports; AD.annexes} ->
    let contract =
      begin
        match List.find_opt AD.is_agree_annex annexes with
        | None -> None
        | Some (AD.AGREEAnnex (_, annex)) ->
           Some (agree_annex_to_contract_spec type_decls annex)
        | _ -> assert false
      end in
    let cyber_rels =
      begin
        match List.find_opt AD.is_verdict_annex annexes with
        | None -> []
        | Some (AD.VerdictAnnex (_, annex)) ->
           verdict_annex_to_cyber_rels annex
        | _ -> assert false
      end in
    let safety_rels =
      begin
        match List.find_opt AD.is_verdict_annex annexes with
        | None -> []
        | Some (AD.VerdictAnnex (_, annex)) ->
           verdict_annex_to_safety_rels annex
        | _ -> assert false
      end in
    let safety_events =
      begin
        match List.find_opt AD.is_verdict_annex annexes with
        | None -> []
        | Some (AD.VerdictAnnex (_, annex)) ->
           verdict_annex_to_safety_events annex
        | _ -> assert false
      end in
    {VI.name = C.get_id name;
     VI.ports = List.map (aadl_port_to_iml_port type_decls) ports;
     contract;
     cyber_rels;
     safety_rels;
     safety_events;
    }
  )

let get_comp_type_and_index iml_comp_types id =
  let ep =
    Utils.element_position
      (fun ({VI.name}: VI.component_type) -> equal_ids name id)
      iml_comp_types
  in
  match ep with
  | None -> assert false
  | Some (ct, pos) -> (ct, pos)

let get_impl_index sys_impl comp_type comp_impl =
  let ep =
    Utils.element_position
      (fun ({AD.name = ((_,id1), (_,id2))}: AD.system_impl) ->
        (equal_ids id1 comp_type) && (equal_ids id2 comp_impl))
      sys_impl
  in
  match ep with
  | None -> assert false
  | Some (_, pos) -> pos

let get_instance_index sys_impl iml_comp_types = function
  | Some ([(_, comp_type)], None) -> (
    VI.Specification (get_comp_type_and_index iml_comp_types comp_type |> snd)
  )
  | Some ([(_, comp_type)], Some (_, comp_impl)) -> (
    VI.Implementation (get_impl_index sys_impl comp_type comp_impl)
  )
  | _ -> assert false

let failwith_replace_property old_prop new_prop =
  Format.printf "%% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% @.";
  Format.printf "* VERDICT_Properties::%s is not supported anymore.@." old_prop; 
  Format.printf "* Please use VERDICT_Properties::%s instead.@." new_prop;
  Format.printf "%% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% %% @.";
  exit 0

let get_manufacturer_prop_value properties =
  let manufacturer_qpr = AD.mk_full_qpref app_property_set "manufacturer" in
  match AD.find_assoc manufacturer_qpr properties with
  | None -> None
  | Some {AD.value} -> (
    failwith_replace_property "manufacturer" "pedigree"
    (*match value with
    | AD.LiteralOrReference (None, (_, id)) -> (
      let v =
        match (String.lowercase_ascii id) with
        | "thirdparty" -> VI.ThirdParty
        | "inhouse" -> VI.InHouse
        | _ -> failwith "Unexpected Manufacturer value"
      in
      Some v
    )
    | _ -> failwith "Unexpected Manufacturer value"*)
  )

let get_pedigree_prop_value properties =
  let pedigree_qpr = AD.mk_full_qpref app_property_set "pedigree" in
  match AD.find_assoc pedigree_qpr properties with
  | None -> None
  | Some {AD.value} -> (
    match value with
    | AD.LiteralOrReference (None, (_, id)) -> (
      let v =
        match (String.lowercase_ascii id) with
        | "internallydeveloped" -> VI.InternallyDeveloped
        | "cots" -> VI.COTS
        | "sourced" -> VI.Sourced
        | _ -> failwith "Unexpected Pedigree value"
      in
      Some v
    )
    | _ -> failwith "Unexpected Pedigree value"
  )

let get_component_type_prop_value properties =
  let component_type_qpr = AD.mk_full_qpref app_property_set "componentType" in
  match AD.find_assoc component_type_qpr properties with
  | None -> None
  | Some {AD.value} -> (
    match value with
    | AD.LiteralOrReference (None, (_, id)) -> (
      let v =
        match (String.lowercase_ascii id) with
        | "software" -> VI.Software
        | "hardware" -> VI.Hardware
        | "human" -> VI.Human
        | "hybrid" -> VI.Hybrid
        | _ -> failwith "Unexpected componentType value"
      in
      Some v
    )
    | _ -> failwith "Unexpected componentType value"
  )

let get_situated_prop_value properties =
  let situated_qpr = AD.mk_full_qpref app_property_set "situated" in
  match AD.find_assoc situated_qpr properties with
  | None -> None
  | Some {AD.value} -> (
    match value with
    | AD.LiteralOrReference (None, (_, id)) -> (
      let v =
        match (String.lowercase_ascii id) with
        | "onboard" -> VI.OnBoard
        | "remote" -> VI.Remote
        | _ -> failwith "Unexpected Situated value"
      in
      Some v
    )
    | _ -> failwith "Unexpected Situated value"
  )

let subcomponent_to_comp_inst sys_impl iml_comp_types
  {AD.name; AD.type_ref; AD.properties }
=
 {VI.name = C.get_id name;
  VI.itype = get_instance_index sys_impl iml_comp_types type_ref;
  VI.manufacturer = get_manufacturer_prop_value properties;
  VI.category = (
    let qpr = AD.mk_full_qpref app_property_set "category" in
    get_string_prop_value qpr properties);
  VI.pedigree = get_pedigree_prop_value properties;
  VI.component_type = get_component_type_prop_value properties;
  VI.situated = get_situated_prop_value properties;
  VI.adversarially_tested = (
    let qpr = AD.mk_full_qpref app_property_set "adversariallyTested" in
    get_bool_prop_value qpr properties);
  VI.has_sensitive_info = (
    let qpr = AD.mk_full_qpref app_property_set "hasSensitiveInfo" in
    get_bool_prop_value qpr properties);
  VI.inside_trusted_boundary = (
    let qpr = AD.mk_full_qpref app_property_set "insideTrustedBoundary" in
    get_bool_prop_value qpr properties);
  VI.canReceiveConfigUpdate = (
    let qpr = AD.mk_full_qpref app_property_set "canReceiveConfigUpdate" in
    get_bool_prop_value qpr properties);
  VI.canReceiveSWUpdate = (
    let qpr = AD.mk_full_qpref app_property_set "canReceiveSWUpdate" in
    get_bool_prop_value qpr properties);
  VI.controlReceivedFromUntrusted = (
    let qpr = AD.mk_full_qpref app_property_set "controlReceivedFromUntrusted" in
    get_bool_prop_value qpr properties);
  VI.controlSentToUntrusted = (
    let qpr = AD.mk_full_qpref app_property_set "controlSentToUntrusted" in
    get_bool_prop_value qpr properties);
  VI.dataReceivedFromUntrusted = (
    let qpr = AD.mk_full_qpref app_property_set "dataReceivedFromUntrusted" in
    get_bool_prop_value qpr properties);
  VI.dataSentToUntrusted = (
    let qpr = AD.mk_full_qpref app_property_set "dataSentToUntrusted" in
    get_bool_prop_value qpr properties);

  VI.configuration_Attack = (
    let qpr = AD.mk_full_qpref app_property_set "Configuration_Attack" in
    get_bool_prop_value qpr properties);
  VI.physical_Theft_Attack = (
    let qpr = AD.mk_full_qpref app_property_set "Physical_Theft_Attack" in
    get_bool_prop_value qpr properties);
  VI.interception_Attack = (
    let qpr = AD.mk_full_qpref app_property_set "Interception_Attack" in
    get_bool_prop_value qpr properties);
  VI.hardware_Integrity_Attack = (
    let qpr = AD.mk_full_qpref app_property_set "Hardware_Integrity_Attack" in
    get_bool_prop_value qpr properties);
  VI.supply_Chain_Attack = (
    let qpr = AD.mk_full_qpref app_property_set "Supply_Chain_Attack" in
    get_bool_prop_value qpr properties);
  VI.brute_Force_Attack = (
    let qpr = AD.mk_full_qpref app_property_set "Brute_Force_Attack" in
    get_bool_prop_value qpr properties);
  VI.fault_Injection_Attack = (
    let qpr = AD.mk_full_qpref app_property_set "Fault_Injection_Attack" in
    get_bool_prop_value qpr properties);
  VI.identity_Spoofing_Attack = (
    let qpr = AD.mk_full_qpref app_property_set "Identity_Spoofing_Attack" in
    get_bool_prop_value qpr properties);
  VI.excessive_Allocation_Attack = (
    let qpr = AD.mk_full_qpref app_property_set "Excessive_Allocation_Attack" in
    get_bool_prop_value qpr properties);
  VI.sniffing_Attack = (
    let qpr = AD.mk_full_qpref app_property_set "Sniffing_Attack" in
    get_bool_prop_value qpr properties);
  VI.buffer_Attack = (
    let qpr = AD.mk_full_qpref app_property_set "Buffer_Attack" in
    get_bool_prop_value qpr properties);
  VI.flooding_Attack = (
    let qpr = AD.mk_full_qpref app_property_set "Flooding_Attack" in
    get_bool_prop_value qpr properties);

  VI.anti_jamming = (
    let qpr = AD.mk_full_qpref app_property_set "antiJamming" in
    get_bool_prop_value qpr properties);

  VI.auditMessageResponses = (
    let qpr = AD.mk_full_qpref app_property_set "auditMessageResponses" in
    get_bool_prop_value qpr properties);
  VI.deviceAuthentication = (
    let qpr = AD.mk_full_qpref app_property_set "deviceAuthentication" in
    get_bool_prop_value qpr properties);
  VI.dosProtection = (
    let qpr = AD.mk_full_qpref app_property_set "dosProtection" in
    get_bool_prop_value qpr properties);
  VI.encryptedStorage = (
    let qpr = AD.mk_full_qpref app_property_set "encryptedStorage" in
    get_bool_prop_value qpr properties);

  VI.heterogeneity = (
    let qpr = AD.mk_full_qpref app_property_set "heterogeneity" in
    get_bool_prop_value qpr properties);
  VI.inputValidation = (
    let qpr = AD.mk_full_qpref app_property_set "inputValidation" in
    get_bool_prop_value qpr properties);
  VI.logging = (
    let qpr = AD.mk_full_qpref app_property_set "logging" in
    get_bool_prop_value qpr properties);
  VI.memoryProtection = (
    let qpr = AD.mk_full_qpref app_property_set "memoryProtection" in
    get_bool_prop_value qpr properties);
  VI.physicalAccessControl = (
    let qpr = AD.mk_full_qpref app_property_set "physicalAccessControl" in
    get_bool_prop_value qpr properties);
  VI.removeIdentifyingInformation = (
    let qpr = AD.mk_full_qpref app_property_set "removeIdentifyingInformation" in
    get_bool_prop_value qpr properties);
  VI.resourceAvailability = (
    let qpr = AD.mk_full_qpref app_property_set "resourceAvailability" in
    get_bool_prop_value qpr properties);
  VI.resourceIsolation = (
    let qpr = AD.mk_full_qpref app_property_set "resourceIsolation" in
    get_bool_prop_value qpr properties);
  VI.secureBoot = (
    let qpr = AD.mk_full_qpref app_property_set "secureBoot" in
    get_bool_prop_value qpr properties);
  VI.sessionAuthenticity = (
    let qpr = AD.mk_full_qpref app_property_set "sessionAuthenticity" in
    get_bool_prop_value qpr properties);
  VI.staticCodeAnalysis = (
    let qpr = AD.mk_full_qpref app_property_set "staticCodeAnalysis" in
    get_bool_prop_value qpr properties);
  VI.strongCryptoAlgorithms = (
    let qpr = AD.mk_full_qpref app_property_set "strongCryptoAlgorithms" in
    get_bool_prop_value qpr properties);
  VI.supplyChainSecurity = (
    let qpr = AD.mk_full_qpref app_property_set "supplyChainSecurity" in
    get_bool_prop_value qpr properties);
  VI.systemAccessControl = (
    let qpr = AD.mk_full_qpref app_property_set "systemAccessControl" in
    get_bool_prop_value qpr properties);
  VI.tamperProtection = (
    let qpr = AD.mk_full_qpref app_property_set "tamperProtection" in
    get_bool_prop_value qpr properties);
  VI.userAuthentication = (
    let qpr = AD.mk_full_qpref app_property_set "userAuthentication" in
    get_bool_prop_value qpr properties);

  VI.anti_jamming_dal = (
    let qpr = AD.mk_full_qpref app_property_set "antiJammingDAL" in
    get_int_prop_value qpr properties);
  VI.auditMessageResponsesDAL = (
    let qpr = AD.mk_full_qpref app_property_set "auditMessageResponsesDAL" in
    get_int_prop_value qpr properties);
  VI.deviceAuthenticationDAL = (
    let qpr = AD.mk_full_qpref app_property_set "deviceAuthenticationDAL" in
    get_int_prop_value qpr properties);
  VI.dosProtectionDAL = (
    let qpr = AD.mk_full_qpref app_property_set "dosProtectionDAL" in
    get_int_prop_value qpr properties);
  VI.encryptedStorageDAL = (
    let qpr = AD.mk_full_qpref app_property_set "encryptedStorageDAL" in
    get_int_prop_value qpr properties);
  VI.heterogeneity_dal = (
    let qpr = AD.mk_full_qpref app_property_set "heterogeneityDAL" in
    get_int_prop_value qpr properties);
  VI.inputValidationDAL = (
    let qpr = AD.mk_full_qpref app_property_set "inputValidationDAL" in
    get_int_prop_value qpr properties);
  VI.loggingDAL = (
    let qpr = AD.mk_full_qpref app_property_set "loggingDAL" in
    get_int_prop_value qpr properties);
  VI.memoryProtectionDAL = (
    let qpr = AD.mk_full_qpref app_property_set "memoryProtectionDAL" in
    get_int_prop_value qpr properties);
  VI.physicalAccessControlDAL = (
    let qpr = AD.mk_full_qpref app_property_set "physicalAccessControlDAL" in
    get_int_prop_value qpr properties);
  VI.removeIdentifyingInformationDAL = (
    let qpr = AD.mk_full_qpref app_property_set "removeIdentifyingInformationDAL" in
    get_int_prop_value qpr properties);
  VI.resourceAvailabilityDAL = (
    let qpr = AD.mk_full_qpref app_property_set "resourceAvailabilityDAL" in
    get_int_prop_value qpr properties);
  VI.resourceIsolationDAL = (
    let qpr = AD.mk_full_qpref app_property_set "resourceIsolationDAL" in
    get_int_prop_value qpr properties);
  VI.secureBootDAL = (
    let qpr = AD.mk_full_qpref app_property_set "secureBootDAL" in
    get_int_prop_value qpr properties);
  VI.sessionAuthenticityDAL = (
    let qpr = AD.mk_full_qpref app_property_set "sessionAuthenticityDAL" in
    get_int_prop_value qpr properties);
  VI.staticCodeAnalysisDAL = (
    let qpr = AD.mk_full_qpref app_property_set "staticCodeAnalysisDAL" in
    get_int_prop_value qpr properties);
  VI.strongCryptoAlgorithmsDAL = (
    let qpr = AD.mk_full_qpref app_property_set "strongCryptoAlgorithmsDAL" in
    get_int_prop_value qpr properties);
  VI.supplyChainSecurityDAL = (
    let qpr = AD.mk_full_qpref app_property_set "supplyChainSecurityDAL" in
    get_int_prop_value qpr properties);
  VI.systemAccessControlDAL = (
    let qpr = AD.mk_full_qpref app_property_set "systemAccessControlDAL" in
    get_int_prop_value qpr properties);
  VI.tamperProtectionDAL = (
    let qpr = AD.mk_full_qpref app_property_set "tamperProtectionDAL" in
    get_int_prop_value qpr properties);
  VI.userAuthenticationDAL = (
    let qpr = AD.mk_full_qpref app_property_set "userAuthenticationDAL" in
    get_int_prop_value qpr properties)

  (*
  VI.broadcast_from_outside_tb = (
    let qpr = AD.mk_full_qpref app_property_set "broadcastFromOutsideTB" in
    get_bool_prop_value qpr properties);
  VI.wifi_from_outside_tb = (
    let qpr = AD.mk_full_qpref app_property_set "wifiFromOutsideTB" in
    get_bool_prop_value qpr properties);
  VI.encryption = (
    let qpr = AD.mk_full_qpref app_property_set "encryption" in
    get_bool_prop_value qpr properties);
  VI.anti_flooding = (
    let qpr = AD.mk_full_qpref app_property_set "antiFlooding" in
    get_bool_prop_value qpr properties);
  VI.anti_fuzzing = (
    let qpr = AD.mk_full_qpref app_property_set "antiFuzzing" in
    get_bool_prop_value qpr properties);
  VI.anti_flooding_dal = (
    let qpr = AD.mk_full_qpref app_property_set "antiFloodingDAL" in
    get_int_prop_value qpr properties);
  VI.anti_fuzzing_dal = (
    let qpr = AD.mk_full_qpref app_property_set "antiFuzzingDAL" in
    get_int_prop_value qpr properties);
  VI.encryption_dal = (
    let qpr = AD.mk_full_qpref app_property_set "encryptionDAL" in
    get_int_prop_value qpr properties);

  *)
 }

let get_port_index {VI.ports} port =
  let ep =
    Utils.element_position
      (fun ({VI.name}: VI.port) -> equal_ids name port)
      ports
  in
  match ep with
  | None -> assert false
  | Some (_, idx) -> idx

let iml_connection_end ct ct_idx sys_impl iml_comp_types subcomps = function
  | None, (_, port) -> (
    VI.ComponentCE (ct_idx, get_port_index ct port)
  )
  | Some (_, comp_inst), (_, port) -> (
    let ci, ci_idx =
      let ep =
        Utils.element_position
        (fun ({VI.name}: VI.component_instance) -> equal_ids name comp_inst)
        subcomps
      in
      match ep with
      | None -> assert false
      | Some (ci, ci_idx) -> (ci, ci_idx)
    in
    let ci_ct, ci_ct_idx =
      match ci.VI.itype with
      | Specification idx -> List.nth iml_comp_types idx, idx
      | Implementation idx -> (
        let ({AD.name = ((_, comp_id), _)}: AD.system_impl) = List.nth sys_impl idx in
        get_comp_type_and_index iml_comp_types comp_id
      )  
    in
    VI.SubcomponentCE (ci_idx, ci_ct_idx, get_port_index ci_ct port)
  )

let get_flow_type_prop_value properties =
  let flow_type_qpr = AD.mk_full_qpref app_property_set "flowType" in
  match AD.find_assoc flow_type_qpr properties with
  | None -> None 
  | Some {AD.value} -> (
    match value with
    | AD.LiteralOrReference (None, (_, id)) -> (
      match (String.lowercase_ascii id) with
      | "xdata" -> Some VI.Xdata
      | "xcontrol" -> Some VI.Xcontrol
      | "xrequest" -> Some VI.Xrequest
      | _ -> failwith "Unexpected FlowType value"
    )
    | _ -> failwith "Unexpected FlowType value"
  )

let get_conn_type_prop_value properties =
  let conn_type_qpr = AD.mk_full_qpref app_property_set "connectionType" in
  match AD.find_assoc conn_type_qpr properties with
  | None -> None
  | Some {AD.value} -> (
    match value with
    | AD.LiteralOrReference (None, (_, id)) -> (
      let v =
        match (String.lowercase_ascii id) with
        | "local" -> VI.Local
        | "remote" -> VI.Remote
        | _ -> failwith "Unexpected Connection Type value"
      in
      Some v
    )
    | _ -> failwith "Unexpected Connection Type value"
  )

let port_connection_to_iml_connection ct ct_idx sys_impl iml_comp_types subcomps
  { AD.name; AD.dir; AD.src; AD.dst; AD.properties }
=
  assert (dir = AD.Unidirectional);
  {VI.name = C.get_id name;
   VI.ftype = get_flow_type_prop_value properties;
   VI.conn_type = get_conn_type_prop_value properties;
   VI.authenticated = (
     let qpr = AD.mk_full_qpref app_property_set "authenticated" in
     get_bool_prop_value qpr properties);
   VI.data_encrypted = (
     let qpr = AD.mk_full_qpref app_property_set "dataEncrypted" in
     match AD.find_assoc qpr properties with
     | None -> None
     | Some _ -> failwith_replace_property "dataEncrypted" "encryptedTransmission"
   );
   VI.trustedConnection = (
     let qpr = AD.mk_full_qpref app_property_set "trustedConnection" in
     get_bool_prop_value qpr properties);
   VI.encryptedTransmission = (
     let qpr = AD.mk_full_qpref app_property_set "encryptedTransmission" in
     get_bool_prop_value qpr properties);
   VI.encryptedTransmissionDAL = (
    let qpr = AD.mk_full_qpref app_property_set "encryptedTransmissionDAL" in
    get_int_prop_value qpr properties);
   VI.replayProtection = (
     let qpr = AD.mk_full_qpref app_property_set "replayProtection" in
     get_bool_prop_value qpr properties);
   VI.source = iml_connection_end ct ct_idx sys_impl iml_comp_types subcomps src;
   VI.destination = iml_connection_end ct ct_idx sys_impl iml_comp_types subcomps dst;
  }

let agree_const_decl_to_iml_const_decl type_decls { AG.name; AG.dtype; AG.definition } =
  {VI.name = C.get_id name;
   VI.dtype = agree_data_type_to_data_type type_decls dtype;
   VI.definition = Some (agree_expr_to_iml_expr definition);
  }

let agree_const_decls_to_iml_const_decls type_decls const_decls =
  List.map (agree_const_decl_to_iml_const_decl type_decls) const_decls

let agree_node_def_to_iml_node_decl type_decls
    {AG.name; AG.inputs; AG.outputs; AG.locals; AG.equations}
  =
   let input_parameters =
     inputs |> List.map (fun (id, dtype) ->
       {VI.name = C.get_id id;
        VI.is_constant = false;
        VI.dtype = agree_data_type_to_data_type type_decls dtype;
       }
     )
   in
   let output_parameters =
     outputs |> List.map (fun (id, dtype) ->
       {VI.name = C.get_id id;
        VI.dtype = agree_data_type_to_data_type type_decls dtype;
       }
     )
   in
   let dataflow_impl =
     let variable_declarations =
       locals |> List.map (fun (id, dtype) ->
         ({VI.name = C.get_id id;
          VI.dtype = agree_data_type_to_data_type type_decls dtype;
         } : VI.variable_declaration)
       )
     in
     let equations =
       equations |> List.map (fun {AG.lhs; AG.rhs} ->
         {VI.lhs = List.map C.get_id lhs;
          VI.rhs = agree_expr_to_iml_expr rhs;
         }
       )
     in
     {VI.constant_declarations = [];
      variable_declarations;
      VI.assertions = [];
      equations;
      VI.properties = [];
     }
   in
   {VI.name = C.get_id name;
    VI.is_function = false;
    VI.is_main = false;
    input_parameters;
    output_parameters;
    contract = None;
    body = Some dataflow_impl;
   }

let agree_node_defs_to_iml_node_decls type_decls node_defs =
  List.map (agree_node_def_to_iml_node_decl type_decls) node_defs

let agree_assign_statement_to_iml_equation { AG.var; AG.definition } =
  let id =
    match var with
    | [pid], None -> C.get_id pid
    | _ -> assert false 
  in
  {VI.lhs = [id];
   VI.rhs = agree_expr_to_iml_expr definition;
  }

let agree_annex_to_dataflow_impl type_decls annex =
  let const_decls, var_decls, eqs, assertions =
    List.fold_left (fun (cds, vds, eqs, ass) -> function
      | AG.ConstStatement (_, const_decl) ->
        let c = agree_const_decl_to_iml_const_decl type_decls const_decl in
        (c::cds, vds, eqs, ass)
      | AG.AssignStatement (_, assign_st) ->
        let eq = agree_assign_statement_to_iml_equation assign_st in
        (cds, vds, eq :: eqs, ass)
      | AG.EqStatement (_, {AG.vars; AG.definition}) ->
        let vds' =
          List.fold_left (fun acc (id,dtype) ->
           ({VI.name = C.get_id id;
             VI.dtype = agree_data_type_to_data_type type_decls dtype;
            }: VI.variable_declaration) :: acc
          ) 
          [] vars
        in
        let eq =
          let def =
            match definition with
            | None -> assert false
            | Some def -> def
          in
          {VI.lhs = List.map (fun (id,_) -> C.get_id id) vars;
           VI.rhs = agree_expr_to_iml_expr def;
          }
        in
        (cds, List.rev_append vds' vds, eq :: eqs, ass)
      | AG.AssertStatement (_, { AG.expression }) ->
        let e = agree_expr_to_iml_expr expression in
        (cds, vds, eqs, e :: ass)
      | AG.NamedSpecStatement _
      | AG.NodeDefinition _ -> assert false
    )
    ([], [], [], []) (List.rev annex)
  in
  {VI.constant_declarations = const_decls;
   VI.variable_declarations = var_decls;
   assertions;
   VI.equations = eqs;
   VI.properties = [];
  }

let empty_dataflow_impl =
  {VI.constant_declarations = [];
   VI.variable_declarations = [];
   VI.assertions = [];
   VI.equations = [];
   VI.properties = [];
  }

let system_impl_to_iml_comp_impl type_decls iml_comp_types sys_impl =
  sys_impl |> List.map
  (fun {AD.name; AD.subcomponents; AD.connections; AD.annexes} ->
    let iml_name =
      Format.asprintf "%a.%a"
        C.pp_print_id (fst name) C.pp_print_id (snd name)
    in
    let id = C.get_id (fst name) in
    let ct, ct_idx = get_comp_type_and_index iml_comp_types id in
    let impl =
      if subcomponents = [] then (
        match List.find_opt AD.is_agree_annex annexes with
        | Some (AD.AGREEAnnex (_, annex)) ->
          VI.DataflowImpl (agree_annex_to_dataflow_impl type_decls annex)
        | _ ->
          VI.DataflowImpl (empty_dataflow_impl)
      )
      else (
        let b_impl =
          let subcomponents =
            subcomponents |>
            List.map (subcomponent_to_comp_inst sys_impl iml_comp_types)
          in
          let connections =
            connections |> List.map
              (port_connection_to_iml_connection
                ct ct_idx sys_impl iml_comp_types subcomponents)
          in
          ({ subcomponents; connections } : VI.block_impl)
        in
        VI.BlockImpl b_impl
      )
    in
    {VI.name = iml_name;
     VI.ctype = ct_idx;
     VI.impl = impl;
    }
  )

let agree_annex_to_dataflow_model type_decls annex =
  let const_decls, node_defs =
    List.fold_left (fun (cds, nds)  -> function
      | AG.ConstStatement (_, c) -> (c :: cds, nds)
      | AG.NodeDefinition (_, n) -> (cds, n :: nds)
      | AG.NamedSpecStatement _
      | AG.EqStatement _
      | AG.AssignStatement _
      | AG.AssertStatement _ -> assert false
    )
    ([], []) (List.rev annex)
  in
  {VI.type_declarations = [];
   VI.constant_declarations =
     agree_const_decls_to_iml_const_decls type_decls const_decls;
   VI.contract_declarations = [];
   VI.node_declarations =
     agree_node_defs_to_iml_node_decls type_decls node_defs
  }

let verdict_cyber_reqs_of_system_types sys_types =
  (* we assume that there is only one system type with cyber reqs *)
  List.fold_left
    begin
      fun acc ({annexes} : AD.system_type) ->
      List.fold_left
        begin
          fun acc' annex ->
                match annex with
                | AD.VerdictAnnex (_, verdict)
                  -> verdict_annex_to_cyber_reqs verdict @ acc'
                | _ -> acc'
        end acc annexes
    end [] sys_types

let verdict_safety_reqs_of_system_types sys_types =
  (* we assume that there is only one system type with cyber reqs *)
  List.fold_left
    begin
      fun acc ({annexes} : AD.system_type) ->
      List.fold_left
        begin
          fun acc' annex ->
                match annex with
                | AD.VerdictAnnex (_, verdict)
                  -> verdict_annex_to_safety_reqs verdict @ acc'
                | _ -> acc'
        end acc annexes
    end [] sys_types

let filter_opt (lst : 'a option list) : 'a list =
  List.fold_left
    (fun acc nxt ->
      match nxt with
      | Some v -> v :: acc
      | None -> acc) [] lst

let statements_of_annex_list annexes =
  List.map (function
      | AD.VerdictAnnex (_, ast) -> Some ast
      | _ -> None) annexes |> filter_opt |> List.flatten

let iml_of_tintro {VE.var; VE.var_type} =
  {VI.var; VI.var_type}

let rec iml_of_texpr = function
  | VE.TEqual (left, right) -> VI.TEqual {VI.left; VI.right}
  | VE.TContains (left, right) -> VI.TContains {VI.left; VI.right}
  | VE.TForall (intro, expr) ->
     VI.TForall {VI.intro = iml_of_tintro intro;
                 VI.expr = iml_of_texpr expr}
  | VE.TExists (intro, expr) ->
     VI.TExists {VI.intro = iml_of_tintro intro;
                 VI.expr = iml_of_texpr expr}
  | VE.TImplies (left, right) ->
     VI.TImplies {antecedent = iml_of_texpr left;
                  consequent = iml_of_texpr right}
  | VE.TOr exprs -> VI.TOr (List.map iml_of_texpr exprs)
  | VE.TAnd exprs -> VI.TAnd (List.map iml_of_texpr exprs)
  | VE.TNot expr -> VI.TNot (iml_of_texpr expr)

let iml_of_threat_model
      id intro expr cia reference
      assumptions description comment =
  {
    VI.id;
    VI.intro = iml_of_tintro intro;
    VI.expr = iml_of_texpr expr;
    VI.cia = verdict_cyber_cia_to_iml cia;
    VI.reference;
    VI.assumptions;
    VI.description;
    VI.comment;
  }

let iml_of_threat_defense id threats description comment =
  {VI.id; VI.threats; VI.description; VI.comment}

let iml_of_mission id reqs description comment =
  {VI.id; VI.reqs; VI.description; VI.comment}

let threat_models_of_annexes annexes =
  statements_of_annex_list annexes
  |> List.map (function
         | VE.ThreatModel
           {id; intro; expr; cia; reference;
            assumptions; description; comment}
           -> Some (iml_of_threat_model id intro expr cia reference
                      assumptions description comment)
         | _ -> None) |> filter_opt

let threat_defenses_of_annexes annexes =
  statements_of_annex_list annexes
  |> List.map (function
         | VE.ThreatDefense {id; threats; description; comment}
           -> Some (iml_of_threat_defense
                      id threats description comment)
         | _ -> None) |> filter_opt

let missions_of_system_types sys_types =
  (* Find all missions declared in all system types.
     We assume that there are only actually missions in the
     top-level system type. *)
  sys_types
  |> List.map (fun ({AD.annexes} : AD.system_type) -> annexes)
  |> List.flatten
  |> statements_of_annex_list
  |> List.map (function
         | VE.Mission {id; reqs; description; comment}
           -> Some (iml_of_mission id reqs description comment)
         | _ -> None) |> filter_opt

let pkg_sec_to_model name { AD.classifiers; AD.annex_libs } =
  let comp_types, comp_impls =
    List.fold_left (fun (cts, cis) -> function
      | AD.ComponentType ct -> (ct :: cts, cis)
      | AD.ComponentImpl ci -> (cts, ci :: cis)
    )
    ([], []) classifiers
  in
  let sys_types, data_types =
    List.fold_left (fun (sts, dts) -> function
      | AD.SystemType (_, st) -> (st :: sts, dts)
      | AD.DataType (_, dt) -> (sts, dt :: dts)
    )
    ([], []) comp_types
  in
  let sys_impls, data_impls =
    List.fold_left (fun (sis, dis) -> function
      | AD.SystemImpl (_, si) -> (si :: sis, dis)
      | AD.DataImpl (_, di) -> (sis, di :: dis)
    )
    ([], []) comp_impls
  in
  let type_decls =
    data_to_type_decls data_types data_impls
  in
  let iml_comp_types =
    system_types_to_iml_comp_types type_decls sys_types
  in
  let iml_comp_impl =
    system_impl_to_iml_comp_impl type_decls iml_comp_types sys_impls
  in
  let dataflow_code =
    match List.find_opt AD.is_agree_annex annex_libs with
    | None -> None
    | Some (AD.AGREEAnnex (_, annex)) ->
      Some (agree_annex_to_dataflow_model type_decls annex)
    | _ -> assert false
  in
  let cyber_reqs = verdict_cyber_reqs_of_system_types sys_types
  in
  let safety_reqs = verdict_safety_reqs_of_system_types sys_types
  in
  let missions = missions_of_system_types sys_types
  in
  {VI.name = name;
   VI.type_declarations = type_decls;
   VI.component_types = iml_comp_types;
   dataflow_code;
   VI.comp_impl = iml_comp_impl;
   VI.cyber_reqs = cyber_reqs;
   VI.safety_reqs = safety_reqs;
   VI.threat_models = threat_models_of_annexes annex_libs;
   VI.threat_defenses = threat_defenses_of_annexes annex_libs;
   VI.missions = missions
  }

let aadl_ast_to_vdm_iml = function
  | AD.AADLPackage (_, { AD.name ; AD.public_sec }) -> (
    match public_sec with
    | None -> None
    | Some sec -> (
      let pkg =
        let pkg_name = aadl_pname_to_iml_pname name in
        { VI.name = pkg_name;
          VI.model = pkg_sec_to_model pkg_name sec;
        }
      in
      Some pkg
    )
  )
  | _ -> None
