(* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.
*)

(** @author Daniel Larraz *)

module AD = AADLAst
module AG = AGREEAst
module C = CommonAstTypes

type input = AADLAst.t list

type parse_error =
  | UnexpectedChar of Position.t * char
  | SyntaxError of Position.t

let parse_buffer lexbuf =
  try
    Ok [AADLParser.model_unit AADLLexer.token lexbuf]
  with 
  | AADLLexer.Unexpected_Char c
  | AGREELexer.Unexpected_Char c ->
    let pos = Position.get_position lexbuf in Error (UnexpectedChar (pos, c))
  | AADLLexer.Unexpected_EOF
  | AGREELexer.Unexpected_EOF ->
    let pos = Position.get_position lexbuf in Error (SyntaxError pos)
  | AADLParser.Error
  | AGREEParser.Error
  | VerdictParser.Error ->
    let pos = Position.get_position lexbuf in Error (SyntaxError pos)

let from_channel in_ch =
  parse_buffer (Lexing.from_channel in_ch)


let from_file filename =
  let in_ch = open_in filename in
  try (
    let lexbuf = Lexing.from_channel in_ch in
    lexbuf.Lexing.lex_curr_p <- { lexbuf.Lexing.lex_curr_p with
                                  Lexing.pos_fname = filename };
    let input = parse_buffer lexbuf in
    close_in in_ch;
    input
  )
  with e -> (
    close_in_noerr in_ch;
    raise e
  )


type sort_error =
  | CycleFound

module UnitSet = Set.Make(struct
  type t = C.pname
  let compare = C.compare_pnames
end
)

module UnitMap = Map.Make(struct
  type t = C.pname
  let compare = C.compare_pnames
end
)

let sort_model_units input =

  (* Map from model unit identifiers to 3-tuples (is_perm, deps, um_or_none)
     such that:
       is_perm is true if the model unit has a permanent mark, or
                  false if it has a temporary mark (default value);
       deps is a list of ids of model units that have imported the model unit;
       um_or_none is None if it is a standard model unit, or
                     Some um if the model unit (um) was included in the given input
  *)
  let unit_map =
    let add_dependency dp map pn =
      let update_value = function
        | None -> Some (false, [dp], None)
        | Some (is_perm, deps, u) -> Some (is_perm, dp :: deps, u)
      in
      UnitMap.update pn update_value map
    in
    let add_unit_model um = function
      | None -> Some (false, [], Some um)
      | Some (is_perm, deps, None) -> Some (is_perm, deps, Some um)
      | _ -> assert false
    in
    let f map = function
      | AD.AADLPackage (_, { AD.name }) as um -> (
        let imported_units = AD.get_imported_units um in
        List.fold_left (add_dependency name) map imported_units
        |> UnitMap.update name (add_unit_model um)
      )
      | AD.PropertySet (_, { AD.name; AD.imported_units }) as um -> (
        let pname = [name] in
        List.fold_left (add_dependency pname) map imported_units
        |> UnitMap.update pname (add_unit_model um)
      )
    in
    List.fold_left f UnitMap.empty input
  in

  (* List of model units that are pending of be processed *)
  let pending =
    UnitMap.fold (fun k _ set -> UnitSet.add k set) unit_map UnitSet.empty
  in

  (* Depth-first search topological sort *)

  let rec visit iu l unit_map pending =
    match UnitMap.find_opt iu unit_map with
    | None -> assert false
    | Some (is_perm, deps, um_or_none) -> (
      let pending' = UnitSet.remove iu pending in
      if pending' == pending then ( (* Requires OCaml > 4.03 *)
        (* iu was not a pending model unit *)
        if is_perm then Ok (l, unit_map, pending)
        else Error CycleFound
      )
      else (
        (* ui is not a pending model unit anymore, and
           it has a temporary mark (default value)
        *)
        let rec visit_dependent l unit_map pending' = function
          | [] -> Ok (l, unit_map, pending')
          | dp :: deps -> (
            match visit dp l unit_map pending' with
            | Ok (l, unit_map, pending') -> (
              visit_dependent l unit_map pending' deps
            )
            | Error err -> Error err
          )
        in
        match visit_dependent l unit_map pending' deps with
        | Ok (l, unit_map, pending') -> (
          let l' =
            match um_or_none with
            | None -> l (* Standard unit models are omitted *)
            | Some um -> um :: l
          in
          (* Set permanent mark *)
          Ok (l', UnitMap.add iu (true, deps, um_or_none) unit_map, pending')
        )
        | Error err -> Error err
      )
    )
  in

  let rec process_pending l unit_map pending =
    if UnitSet.is_empty pending then Ok l
    else (
      let iu = UnitSet.choose pending in
      match visit iu l unit_map pending with
      | Ok (l, unit_map, pending) -> (
        process_pending l unit_map pending
      )
      | Error err -> Error err
    )
  in

  process_pending [] unit_map pending


module PkgNameSet = Set.Make(struct
  type t = C.pname
  let compare = C.compare_pnames
end)

let merge_packages input =

  let package_names =
    let get_name set = function
      | AD.AADLPackage (_, { AD.name } ) -> (
        PkgNameSet.add name set
      )
      | AD.PropertySet _ -> set
    in
    List.fold_left get_name PkgNameSet.empty input
  in

  let aadl_packages, property_sets  =
    List.partition AD.is_aadl_package input
  in

  let flatten_names um =

    let remove_mu imported_units =
      let add_iu acc iu =
        match PkgNameSet.find_opt iu package_names with
        | None -> iu :: acc
        | _ -> acc
      in
      List.fold_left add_iu [] imported_units
    in

    let flatten_pname pname =
      let rev_pname = List.rev pname in
      let pkg =
        rev_pname |> List.tl |> List.rev
      in
      match PkgNameSet.find_opt pkg package_names with
      | None -> pname
      | Some _ -> [List.hd rev_pname]
    in

    let flatten_proj pos (pname, pid) =
      match PkgNameSet.find_opt pname package_names with
      | None -> AG.Proj (pos, AG.Ident (flatten_pname pname), pid)
      | Some _ -> AG.Ident [pid]
    in

    let flatten_qcref (pname, pid_opt) = (flatten_pname pname, pid_opt) in

    let rec flatten_agree_expr = function
      | AG.BinaryOp (pos, op, e1, e2) ->
        AG.BinaryOp (pos, op, flatten_agree_expr e1, flatten_agree_expr e2)
      | AG.UnaryOp (pos, op, e) ->
        AG.UnaryOp (pos, op, flatten_agree_expr e)
      | AG.Ite (pos, e1, e2, e3) ->
        let e1 = flatten_agree_expr e1 in
        let e2 = flatten_agree_expr e2 in
        let e3 = flatten_agree_expr e3 in
        AG.Ite (pos, e1, e2, e3)
      | AG.Prev (pos, e1, e2) ->
        AG.Prev (pos, flatten_agree_expr e1, flatten_agree_expr e2)
      | AG.Proj (pos, e, pid) -> (
        match e with
        | AG.Ident name -> flatten_proj pos (name, pid)
        | _ -> AG.Proj (pos, flatten_agree_expr e, pid)
      )
      | AG.Ident name -> AG.Ident (flatten_pname name)
      | AG.EnumValue (pos, qcr, pid) ->
        AG.EnumValue (pos, flatten_qcref qcr, pid)
      | AG.IntegerLit _ as e -> e
      | AG.RealLit _ as e -> e
      | AG.Call (pos, e, args) ->
        AG.Call (pos, flatten_agree_expr e, List.map flatten_agree_expr args)
      | AG.RecordExpr (pos, e, fields) ->
        AG.RecordExpr (pos, flatten_agree_expr e, List.map flatten_field fields)
      | AG.True _ as e -> e
      | AG.False _ as e -> e
    and flatten_field (pid, expr) =
      (pid, flatten_agree_expr expr)
    in

    let flatten_named_spec_statement = function
      | AG.Assume {AG.id; AG.desc; AG.spec} ->
        AG.Assume {id; desc; AG.spec = flatten_agree_expr spec}
      | AG.Guarantee {AG.id; AG.desc; AG.spec} ->
        AG.Guarantee {id; desc; AG.spec = flatten_agree_expr spec}
      | AG.Lemma {AG.id; AG.desc; AG.spec} ->
        AG.Lemma {id; desc; AG.spec = flatten_agree_expr spec}
    in

    let flatten_agree_data_type = function
      | AG.UserType qcr -> AG.UserType (flatten_qcref qcr)
      | dt -> dt
    in

    let flatten_const_statement { AG.name; AG.dtype; AG.definition } =
      {name;
       AG.dtype = flatten_agree_data_type dtype;
       AG.definition = flatten_agree_expr definition;
      }
    in

    let flatten_arg (pid, dt) = (pid, flatten_agree_data_type dt) in

    let flatten_eq_statement { AG.vars; AG.definition } =
      {AG.vars = List.map flatten_arg vars;
       AG.definition =
        (match definition with
        | None -> None
        | Some e -> Some (flatten_agree_expr e));
      }
    in

    let flatten_assign_statement { AG.var; AG.definition} =
      {AG.var = flatten_qcref var;
       AG.definition = flatten_agree_expr definition;
      }
    in

    let flatten_eq { AG.lhs; AG.rhs } =
      { lhs; AG.rhs = flatten_agree_expr rhs; }
    in

    let flatten_node_def
      { AG.name; AG.inputs; AG.outputs; AG.locals; AG.equations } =
      {name;
       AG.inputs = List.map flatten_arg inputs;
       AG.outputs = List.map flatten_arg outputs;
       AG.locals = List.map flatten_arg locals;
       AG.equations = List.map flatten_eq equations;
      }
    in

    let flatten_assertion { AG.expression } =
      { AG.expression = flatten_agree_expr expression }
    in

    let flatten_spec_statement = function
      | AG.NamedSpecStatement (pos, n) ->
        AG.NamedSpecStatement (pos, flatten_named_spec_statement n)
      | AG.ConstStatement (pos, c) ->
        AG.ConstStatement (pos, flatten_const_statement c)
      | AG.EqStatement (pos, e) ->
        AG.EqStatement (pos, flatten_eq_statement e)
      | AG.NodeDefinition (pos, n) ->
        AG.NodeDefinition (pos, flatten_node_def n)
      | AG.AssignStatement (pos, a) ->
        AG.AssignStatement (pos, flatten_assign_statement a)
      | AG.AssertStatement (pos, a) ->
        AG.AssertStatement (pos, flatten_assertion a)
    in

    let flatten_agree_annex agree_annex =
      List.map flatten_spec_statement agree_annex
    in

    let flatten_verdict_annex verdict_annex =
      (* TODO actually flatten? *)
      verdict_annex
    in

    let flatten_annex = function
      | AD.AGREEAnnex (pos, ast) ->
        AD.AGREEAnnex (pos, flatten_agree_annex ast)
      | AD.VerdictAnnex (pos, ast) ->
        AD.VerdictAnnex (pos, flatten_verdict_annex ast)
      | AD.UnsupportedAnnex _ as a -> a
    in

    let flatten_qcref_opt = function
      | None -> None
      | Some qcr -> Some (flatten_qcref qcr)
    in

    let flatten_data_port {AD.name; AD.dir; AD.dtype; AD.properties } =
      { name; dir; AD.dtype = flatten_qcref_opt dtype; properties}
    in

    let flatten_system_type {AD.name; AD.ports; AD.annexes} =
      {name;
       AD.ports = List.map flatten_data_port ports;
       AD.annexes = List.map flatten_annex annexes;
      }
    in

    let flatten_comp_type = function
      | AD.SystemType (pos, st) ->
        AD.SystemType (pos, flatten_system_type st)
      | AD.DataType _ as dt -> dt
    in

    let flatten_subcomponent {AD.name; AD.type_ref; AD.properties } =
      {name;
       AD.type_ref = flatten_qcref_opt type_ref;
       properties;
      }
    in

    let flatten_system_impl {AD.name; AD.subcomponents; AD.connections; AD.annexes } =
      {name;
       AD.subcomponents = List.map flatten_subcomponent subcomponents;
       connections;
       AD.annexes = List.map flatten_annex annexes;
      }
    in

    let flatten_data_impl {AD.name; AD.subcomponents} =
      {name;
       AD.subcomponents = List.map flatten_subcomponent subcomponents;
      }
    in

    let flatten_comp_impl = function
      | AD.SystemImpl (pos, si) ->
        AD.SystemImpl (pos, flatten_system_impl si)
      | AD.DataImpl (pos, di) ->
        AD.DataImpl (pos, flatten_data_impl di)
    in

    let flatten_classifier = function
      | AD.ComponentType ct ->
        AD.ComponentType (flatten_comp_type ct)
      | AD.ComponentImpl ci ->
        AD.ComponentImpl (flatten_comp_impl ci)
    in

    let flatten_pkg_sec = function
      | None -> None
      | Some {AD.imported_units; AD.classifiers; AD.annex_libs } -> (
        Some {AD.imported_units = remove_mu imported_units;
              AD.classifiers = List.map flatten_classifier classifiers;
              AD.annex_libs = List.map flatten_annex annex_libs
             }
      )
    in

    let flatten_pkg { AD.name; AD.public_sec; AD.private_sec } =
      { name;
        AD.public_sec = flatten_pkg_sec public_sec;
        AD.private_sec = flatten_pkg_sec private_sec;
      }
    in

    match um with
    | AD.AADLPackage (pos, pkg) -> (
      let imported_units =
        AD.get_imported_units um
        |> PkgNameSet.of_list
      in
      let inter = PkgNameSet.inter package_names imported_units in
      if PkgNameSet.is_empty inter then um
      else (
        AD.AADLPackage (pos, flatten_pkg pkg)
      )
    )
    | AD.PropertySet _ -> assert false
  in

  let rec merge_agree_annex pos' ast' acc = function
    | [] -> List.rev (AD.AGREEAnnex (pos', ast') :: acc)
    | AD.AGREEAnnex (pos, ast) :: tl -> (
      List.rev_append
        ((AD.AGREEAnnex (pos, List.append ast' ast)) :: acc) tl
    )
    | annex :: tl -> (
      merge_agree_annex pos' ast' (annex :: acc) tl
    )
  in

  let rec merge_verdict_annex pos' ast' acc = function
    | [] -> List.rev (AD.VerdictAnnex (pos', ast') :: acc)
    | AD.VerdictAnnex (pos, ast) :: tl ->
      (List.rev_append
         ((AD.VerdictAnnex
             (pos, List.append ast' ast)) :: acc) tl)
    | annex :: tl ->
      merge_verdict_annex pos' ast' (annex :: acc) tl
  in 

  let merge_unsupported_annex annex name libs =
    let is_same_unsupported = function
      | AD.UnsupportedAnnex (_, id) when id = name -> true
      | _ -> false
    in
    match List.find_opt is_same_unsupported libs with
    | None -> annex :: libs
    | Some _ -> libs
  in

  let merge_annex_libs libs1 libs2 =
    let merge_annex libs = function
      | AD.AGREEAnnex (pos, ast) ->
        merge_agree_annex pos ast [] libs
      | AD.VerdictAnnex (pos, ast) ->
        merge_verdict_annex pos ast [] libs
      | (AD.UnsupportedAnnex (_,name)) as annex ->
        merge_unsupported_annex annex name libs
    in
    List.fold_left merge_annex libs1 libs2
  in

  let merge_pkg_sec pkg_sec1 pkg_sec2 =
    match pkg_sec1, pkg_sec2 with
    | None, sec -> sec
    | sec, None -> sec
    | Some (sec1: AD.package_section), Some (sec2:AD.package_section) -> (
      let sec =
        {AD.imported_units =
           List.sort_uniq CommonAstTypes.compare_pnames
             (List.rev_append sec1.AD.imported_units sec2.AD.imported_units);
         AD.classifiers =
           List.append sec2.AD.classifiers sec1.AD.classifiers;
         AD.annex_libs =
           merge_annex_libs sec1.AD.annex_libs sec2.AD.annex_libs;
        }
      in
      Some sec
    )
  in

  let merge_package {AD.name; AD.public_sec; AD.private_sec} (pkg2: AD.aadl_package) =
    {name;
     AD.public_sec = merge_pkg_sec public_sec pkg2.AD.public_sec;
     AD.private_sec = merge_pkg_sec private_sec pkg2.AD.private_sec;
    }
  in

  let merge_model_units mu1 mu2 =
    match mu1, mu2 with
    | AD.AADLPackage (pos1, pkg1), AD.AADLPackage (pos2, pkg2) -> (
      AD.AADLPackage (pos1, merge_package pkg1 pkg2)
    )
    | _ -> assert false
  in

  match aadl_packages with
  | [] -> property_sets
  | main_pkg :: packages -> (
    let main_pkg =
      let merge main_pkg pkg =
        let pkg = flatten_names pkg in
        merge_model_units main_pkg pkg
      in
      List.fold_left merge (flatten_names main_pkg) packages
    in
    main_pkg :: property_sets
  )

