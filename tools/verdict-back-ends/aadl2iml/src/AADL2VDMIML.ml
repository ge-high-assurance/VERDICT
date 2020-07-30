(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   Copyright (c) 2019-2020, General Electric Company.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
*)

(** @author Daniel Larraz
    @author William D. Smith
*)

module AD = AADLAst
module AG = AGREEAst
module VE = VerdictAst
module C  = CommonAstTypes
module VI = VDMIML

exception SemanticError of (Position.t * string)

let failwith_could_resolve_reference pos id =
  let msg =
    Format.asprintf "couldn't resolve reference to '%s'" id
  in
  raise (SemanticError (pos, msg))

let pp_print_pname_as_iml ppf pn =
  let pp_sep ppf () = Format.fprintf ppf "." in
  Format.pp_print_list ~pp_sep C.pp_print_id ppf pn

let aadl_pname_to_iml_pname pn =
  Format.asprintf "%a" pp_print_pname_as_iml pn

let data_repr_qpref = AD.mk_full_qpref "Data_Model" "Data_Representation"

let enumerators_qpref = AD.mk_full_qpref "Data_Model" "Enumerators"

let get_bool_prop_value qpr properties =
  match AD.find_assoc qpr properties with
  | None -> None
  | Some {AD.value; _} -> (
    match value with
    | AD.BooleanLit b -> Some b
    | _ -> assert false
  )

(* let get_int_prop_value qpr properties =
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
  ) *)

let get_enumerators properties =
  match AD.find_assoc enumerators_qpref properties with
  | None -> failwith ("No Enumerators property association was found")
  | Some {AD.value; _} -> (
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

let failwith_unsupported_data_type pos qcr =
  let msg =
    Format.asprintf "data type '%a' not found" C.pp_print_qcref qcr
  in
  raise (SemanticError (pos, msg))

let realization_full_name (r_pid, i_pid) =
  Format.asprintf "%a.%a" C.pp_print_id r_pid C.pp_print_id i_pid

let get_data_type type_decls = function
  | ([pid], i_pid) as qcr -> (
    let id =
      match i_pid with
      | None -> C.get_id pid
      | Some i_pid -> realization_full_name (pid, i_pid)
    in
    let ep =
      Utils.element_position
        (fun ({VI.name; _}: VI.type_declaration) -> equal_ids name id)
        type_decls
    in
    match ep with
    | None -> (
      match String.lowercase_ascii id with
      | "boolean" -> VI.PlainType VI.Bool
      | "integer" -> VI.PlainType VI.Int
      | "integer_8" -> VI.PlainType VI.Int
      | "integer_16" -> VI.PlainType VI.Int
      | "integer_32" -> VI.PlainType VI.Int
      | "integer_64" -> VI.PlainType VI.Int
      | "unsigned_8" -> VI.PlainType VI.Int
      | "unsigned_16" -> VI.PlainType VI.Int
      | "unsigned_32" -> VI.PlainType VI.Int
      | "unsigned_64" -> VI.PlainType VI.Int
      | "natural" -> VI.PlainType VI.Int
      | "float" -> VI.PlainType VI.Real
      | "float_32" -> VI.PlainType VI.Real
      | "float_64" -> VI.PlainType VI.Real
      | "character" -> VI.PlainType VI.Int
      | "string" -> VI.PlainType VI.Int
      | _ -> failwith_unsupported_data_type (C.get_pos pid) qcr
    )
    | Some (_, pos) -> VI.UserDefinedType pos
  )
  | (pname, _) as qcr -> (
    let pname_str = C.pname_to_string pname in
    match String.lowercase_ascii pname_str with
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
    | "base_types::character" -> VI.PlainType VI.Int
    | "base_types::string" -> VI.PlainType VI.Int
    | _ -> (
      match pname with
      | pid :: _ -> failwith_unsupported_data_type (C.get_pos pid) qcr
      | _ -> assert false
    )
  )

let data_to_type_decls data_types data_impls =
  let type_decls_with_extension_id =
    let add_type ({AD.name; AD.type_extension; AD.properties; _}: AD.data_type) =
      match type_extension with
      | Some _ ->
        ({VI.name = C.get_id name; VI.definition = None; VI.parent = None}, type_extension)
      | None -> (
        let def =
          match AD.find_assoc data_repr_qpref properties with
          | None -> None
          | Some {AD.value; _} -> (
            match value with
            | AD.LiteralOrReference (None, (_, id)) -> (
              match String.lowercase_ascii id with
              | "enum" -> (
                let enumerators = get_enumerators properties in
                Some (VI.EnumType enumerators)
              )
              | "boolean" -> Some (VI.PlainType VI.Bool)
              | "integer" -> Some (VI.PlainType VI.Int)
              | "float" -> Some (VI.PlainType VI.Real)
              | "character" -> Some (VI.PlainType VI.Int)
              | "string" -> Some (VI.PlainType VI.Int)
              | "fixed" -> Some (VI.PlainType VI.Real)
              | _ -> None
            )
            | _ -> failwith ("Found unexpected data representation value")
          )
        in
        ({VI.name = C.get_id name; VI.definition = def; VI.parent = None}, None)
      )
    in
    List.map add_type data_types
  in
  let type_decls_with_extension_id =
    let tdwei =
      data_impls |> List.map (fun ({AD.name; _} : AD.data_impl) ->
        let parent_id = fst name |> C.get_id in
        let name = realization_full_name name in
        let parent =
          Utils.element_position
            (fun (({VI.name; _}, _): (VI.type_declaration * C.qcref option)) ->
              equal_ids name parent_id
            )
            type_decls_with_extension_id
          |> (function None -> None | Some (_, p) -> Some p)
        in
        ({name; VI.definition = None; parent}, None)
      )
    in
    List.rev_append (List.rev type_decls_with_extension_id) tdwei
  in
  let type_decls = List.map fst type_decls_with_extension_id in
  let type_decls =
    let set_type_aliases ((type_decl, ext_id)
      : (VI.type_declaration * C.qcref option)) =
      match ext_id with
      | None -> type_decl
      | Some qcr -> (
        let dtype = get_data_type type_decls qcr in
        { VI.name = type_decl.VI.name;
          VI.definition = Some dtype;
          VI.parent =
            match dtype with
            | VI.UserDefinedType idx -> (List.nth type_decls idx).VI.parent
            | _ -> None
        }
      )
    in
    List.map set_type_aliases type_decls_with_extension_id
  in
  let process_data_impl type_decls {AD.name; AD.subcomponents} =
    let type_name = realization_full_name name in
    let process_subcomponent {AD.name; AD.type_ref; _} =
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
      | { VI.name; VI.definition; VI.parent } :: tl when (equal_ids name type_name) -> (
        let td: VI.type_declaration =
          let def =
            match definition with
            | None -> Some (VI.RecordType record_fields)
            | _ -> definition
          in
          { name; VI.definition = def; parent }
        in
        List.rev_append (td :: l) tl
      )
      | t :: tl -> update_type (t::l) tl
    in
    match record_fields with
    | [] -> type_decls
    | _  -> update_type [] type_decls
  in
  List.fold_left process_data_impl type_decls data_impls

let aadl_dir_to_iml_mode = function
  | AD.In -> VI.In
  | AD.Out -> VI.Out
  | AD.InOut -> failwith "Input-Output ports are not supported"

let aadl_port_to_iml_port prop_set_name type_decls
  {AD.name; AD.dir; AD.is_event; AD.dtype; AD.properties }
=
  {VI.name = C.get_id name;
   VI.mode = aadl_dir_to_iml_mode dir;
   VI.is_event = is_event;
   VI.ptype = (
     match dtype with
     | None -> None
     | Some qcr -> Some (get_data_type type_decls qcr)
   );
   VI.probe = (
     let qpr = AD.mk_full_qpref prop_set_name "probe" in
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
  | AG.Event -> VI.Event

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
  | AG.Prev (_, e1, e2) ->
    let delay = agree_expr_to_iml_expr e1 in
    let init = agree_expr_to_iml_expr e2 in
    VI.BinaryOp (VI.Arrow, init, VI.UnaryOp (VI.Pre, delay))
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
  |> List.filter (fun ({AG.definition; _}: AG.eq_statement) -> definition != None)
  |> List.map var_decl_to_symbol_def

let agree_contract_item_to_iml_contract_item c_items =
  let agree_contract_item_to_iml_contract_item { AG.desc; AG.spec; _ } =
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

let system_types_to_iml_comp_types prop_set_name type_decls sys_types =
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
     VI.ports = List.map (aadl_port_to_iml_port prop_set_name type_decls) ports;
     contract;
     cyber_rels;
     safety_rels;
     safety_events;
    }
  )

let get_comp_type_and_index iml_comp_types id =
  let ep =
    Utils.element_position
      (fun ({VI.name; _}: VI.component_type) -> equal_ids name id)
      iml_comp_types
  in
  match ep with
  | None -> assert false
  | Some (ct, pos) -> (ct, pos)

let get_impl_index sys_impl comp_type comp_impl =
  let ep =
    Utils.element_position
      (fun ({AD.name = ((_,id1), (_,id2)); _}: AD.system_impl) ->
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


module AttrMap = Map.Make(struct
  type t = string
  let compare = (fun s1 s2 ->
    String.compare
      (String.lowercase_ascii s1)
      (String.lowercase_ascii s2)
  )
end
)

let get_iml_prop_value = function
  | AD.BooleanLit b ->
    (VI.Bool, if b then "true" else "false")
  | AD.IntegerTerm nl ->
    (VI.Integer, C.numeric_literal_to_string nl)
  | AD.RealTerm nl ->
    (VI.Integer, C.numeric_literal_to_string nl)
  | AD.StringLit str ->
    (VI.String, str)
  | AD.LiteralOrReference qpr ->
    (VI.String, AD.qpref_to_string qpr)
  | AD.ListTerm _ -> failwith "List-value properties are not supported yet"


let get_attributes prop_set_name _e_name default_props properties =
  properties |> List.fold_left (fun acc { AD.name; AD.value; _ } ->
    let name =
      match name with
      | Some (_, ps_name), (_, id) when equal_ids ps_name prop_set_name -> id
      | _ -> AD.qpref_to_string name
    in
    acc |> AttrMap.update name (fun e ->
      match e with
      | None -> None (* Not a VERDICT Property, ignore it *)
      | Some _ -> Some (Some (get_iml_prop_value value))
    )
  )
  default_props
  |> AttrMap.bindings
  |> List.fold_left (fun acc (name, v) ->
       match v with
       | None -> acc
       | Some (atype, value) -> { VI.name = name; atype; value } :: acc
     )
     []
(*
  |> List.map (fun (name, v) ->
     match v with
     | None -> (
       let msg =
         Format.asprintf
           "Mandatory VERDICT Property '%s' not set in '%s'"
             name e_name
       in
       failwith msg
     )
     | Some (atype, value) -> { VI.name = name; atype; value }
  )
*)

let split_property_set_and_get_map { AD.name; AD.declarations; _ } =
  let name = C.get_id name in
  declarations |> List.map (function
    | AD.UnsupportedDecl ->
      failwith ("Unsupported declaration in property set '" ^ name ^ "'!")
    | AD.PropertyDef def -> def
  )
  |> List.fold_left
    (fun (comp, conn) { AD.name; AD.default_value; AD.applies_to; _ } ->
      let name = C.get_id name in
      let value =
        match default_value with
        | None -> None
        | Some expr -> Some (get_iml_prop_value expr)
      in
      match applies_to with
      | All -> (AttrMap.add name value comp, AttrMap.add name value conn)
      | System -> (AttrMap.add name value comp, conn)
      | Connection -> (comp, AttrMap.add name value conn)
      | Other -> (comp, conn)
    )
    (AttrMap.empty, AttrMap.empty)


let subcomponent_to_comp_inst prop_set_name comp_props sys_impl iml_comp_types
  {AD.name; AD.type_ref; AD.properties; _ }
=
 let name = C.get_id name in
 {VI.name = name;
  VI.itype = get_instance_index sys_impl iml_comp_types type_ref;
  VI.attributes = get_attributes prop_set_name name comp_props properties
 }

let get_port_and_index {VI.ports; _} (pos, port) =
  let ep =
    Utils.element_position
      (fun ({VI.name; _}: VI.port) -> equal_ids name port)
      ports
  in
  match ep with
  | None -> failwith_could_resolve_reference pos port
  | Some pi -> pi


let port_and_iml_connection_end ct ct_idx sys_impl iml_comp_types subcomps = function
  | None, pos_port -> (
    let (port, idx) = get_port_and_index ct pos_port in
    port, VI.ComponentCE (ct_idx, idx)
  )
  | Some (pos, comp_inst), pos_port -> (
    let ci, ci_idx =
      let ep =
        Utils.element_position
        (fun ({VI.name; _}: VI.component_instance) -> equal_ids name comp_inst)
        subcomps
      in
      match ep with
      | None -> failwith_could_resolve_reference pos comp_inst
      | Some (ci, ci_idx) -> (ci, ci_idx)
    in
    let ci_ct, ci_ct_idx =
      match ci.VI.itype with
      | Specification idx -> List.nth iml_comp_types idx, idx
      | Implementation idx -> (
        let ({AD.name = ((_, comp_id), _); _}: AD.system_impl) = List.nth sys_impl idx in
        get_comp_type_and_index iml_comp_types comp_id
      )  
    in
    let (port, idx) = get_port_and_index ci_ct pos_port in
    port, VI.SubcomponentCE (ci_idx, ci_ct_idx, idx)
  )

let failwith_incompatible_types pos id1 id2 =
  let msg =
    Format.asprintf "'%s' and '%s' have incompatible data types" id1 id2
  in
  raise (SemanticError (pos, msg))

let port_connection_to_iml_connection
  prop_set_name conn_props ct ct_idx sys_impl iml_comp_types subcomps type_decls
  { AD.name; AD.dir; AD.src; AD.dst; AD.properties }
=
  assert (dir = AD.Unidirectional);
  let src_port, source =
    port_and_iml_connection_end ct ct_idx sys_impl iml_comp_types subcomps src
  in
  let dst_port, destination =
    port_and_iml_connection_end ct ct_idx sys_impl iml_comp_types subcomps dst
  in
  (match src_port.VI.ptype, dst_port.VI.ptype with
   | Some src_ptype, Some dst_ptype -> (
     if src_ptype<>dst_ptype then (
       let src_subtype_of_dst =
         VI.is_subtype type_decls src_ptype dst_ptype
       in
       let dst_subtype_of_src =
         VI.is_subtype type_decls dst_ptype src_ptype
       in
       match src_subtype_of_dst, dst_subtype_of_src with
       | true, false ->
         let pos = C.get_pos (snd dst) in
         raise (SemanticError (pos, "subtyping is not supported"))
       | false, true
       | false, false ->
         failwith_incompatible_types
           (C.get_pos (snd src)) (C.get_id (snd src)) (C.get_id (snd dst))
       | true, true -> ()
     )
   )
   | Some _, None ->
     raise (SemanticError
       (C.get_pos (snd dst), "source port has a type but destination port does not"))
   | None, Some _ ->
     raise (SemanticError
       (C.get_pos (snd src), "destination port has a type but source port does not"))
   | _ -> ()
  );
  let name = C.get_id name in
  {VI.name = name;
   VI.attributes = get_attributes prop_set_name name conn_props properties;
   source;
   destination;
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

let system_impl_to_iml_comp_impl v_props type_decls iml_comp_types sys_impl =
  let comp_props, conn_props =
    split_property_set_and_get_map v_props
  in
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
        let prop_set_name = C.get_id v_props.AD.name in
        let b_impl =
          let subcomponents =
            subcomponents |> List.map 
              (subcomponent_to_comp_inst prop_set_name comp_props sys_impl iml_comp_types)
          in
          let connections =
            connections |> List.map
              (port_connection_to_iml_connection prop_set_name conn_props
                ct ct_idx sys_impl iml_comp_types subcomponents type_decls)
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
      fun acc ({annexes; _} : AD.system_type) ->
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
      fun acc ({annexes; _} : AD.system_type) ->
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
  |> List.map (fun ({AD.annexes; _} : AD.system_type) -> annexes)
  |> List.flatten
  |> statements_of_annex_list
  |> List.map (function
         | VE.Mission {id; reqs; description; comment}
           -> Some (iml_of_mission id reqs description comment)
         | _ -> None) |> filter_opt

let pkg_sec_to_model v_props name { AD.classifiers; AD.annex_libs; _ } =
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
  let prop_set_name = C.get_id v_props.AD.name in
  let iml_comp_types =
    system_types_to_iml_comp_types prop_set_name type_decls sys_types
  in
  let iml_comp_impl =
    system_impl_to_iml_comp_impl v_props type_decls iml_comp_types sys_impls
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

let aadl_ast_to_vdm_iml v_props = function
  | AD.AADLPackage (_, { AD.name ; AD.public_sec; _ }) -> (
    match public_sec with
    | None -> None
    | Some sec -> try (
      let pkg =
        let pkg_name = aadl_pname_to_iml_pname name in
        { VI.name = pkg_name;
          VI.model = pkg_sec_to_model v_props pkg_name sec;
        }
      in
      Some pkg
    )
    with SemanticError (pos, msg) -> (
      Format.eprintf "%a: error: %s@." Position.pp_print_position pos msg;
      None
    )
  )
  | _ -> None
