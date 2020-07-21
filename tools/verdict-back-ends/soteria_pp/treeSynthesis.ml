(* 

Copyright © 2019-2020 General Electric Company and United States Government as represented 
by the Administrator of the National Aeronautics and Space Administration.  All Rights Reserved.

Author: Pete Manolios
Date: 2017-12-15

Updates: 8/16/2018, Kit Siu, modified to work with models that includes attack-defense.

*)

(**
   Trees should not be constructed manually by typical users. Instead,
   they should be synthesized from libraries of components and
   models. Components are represented with the {i component} type; component
   instances are represented with the {i instance} type; component libraries are
   just a list of components; and models are represented with the {i model}
   type. Here is an example of a library. It is just a list of components.

   {[
   let nasa_handbook_lib =
   [ {name            = "System"; 
      input_flows     = ["in"];
      output_flows    = ["out"];
      faults          = ["fault"]; 
      basic_events    = ["sys_fl"];
      event_info      = [(1.e-6, 1.)];
      fault_formulas  = [(["out"; "fault"], Or [F ["sys_fl"]; F ["in"; "fault"]])];
      attacks         = ["attack"]; 
	  attack_events   = ["sys_atck"]; 
	  attack_info     = [1e-07]; 
	  attack_formulas = [(["out"; "attack"], Or [A["sys_atck"]; A["in"; "attack"]])]; 
	  defense_events  = []; 
	  defense_profiles =[]; 
	  defense_rigors   = []; 
      }];; 
   ]}

   Here is an example of a model. It is a list of component instances along with
   connection information and the identification of a top-level fault.

   {[
   let nasa_handbook_model =
   { instances   =  [ makeInstance "Orbiter" "System" ();
                      makeInstance "Main Engine" "System" (); ];
     connections =  [ (("Orbiter", "in"), ("Main Engine", "out"));
                      (("Main Engine", "in"), ("Orbiter", "out")); ];
     top_fault   =  ("Orbiter", F["out"; "fault"]);
     top_attack  =  ("Orbiter", A["out"; "attack"]); 
   } ;;
   ]}

   The top-level functions for modeling systems are:

   - {b model_to_ftree}: a function that given a library of components and a
   model synthesizes a fault tree for the top-level event of the model. The
   fault tree synthesized can then be analyzed using the fault tree top-level
   functions.
   
   - {b model_to_adtree}: a function that given a library of components and a
   model synthesizes an attack-defense tree for the top-level event of the model. The
   attack-defense tree synthesized can then be analyzed using the attack-defense tree 
   top-level functions.
   
*)

open Core ;;
open FaultTree ;;
open AttackDefenseTree ;;
open Qualitative ;;
open Quantitative ;;
open Modeling ;;

(**/**)
(** Given a list of formulas, find the one corresponding to x. *)
let find_form1 fs x =
  let (_, res) = List.find_exn fs ~f:(fun (a, _) -> a=x) in
  res;;

let expand_N_of j f =
  let c = choosei j f in
  let cf = List.map c ~f:(fun x -> And(x)) in
  if (List.length cf = 1)
  then List.hd_exn cf
  else Or(cf) ;;

let rec exp_i i_n b =
  match b with
    | F(x) -> F(i_n, x)
    | A(x) -> A(i_n, x)
    | D(x) -> D(i_n, x)
    | And(y) -> And(List.map y ~f:(fun x -> exp_i i_n x))
    | Or(y) -> Or(List.map y ~f:(fun x -> exp_i i_n x))
    | N_of(j, fl) -> exp_i i_n (expand_N_of j fl);;


(* I need a function to expand a formula so that it is in terms of the
   component inputs *)

let rec expand_c_form fs f c =
  match f with
    | F[x] ->
      if (List.mem c.basic_events x ~equal:(=))
      then f
      else expand_c_form fs (find_form1 fs [x]) c
    | F[i;flt] ->
      if (List.mem c.input_flows i ~equal:(=)) && (List.mem c.faults flt ~equal:(=))
      then f
      else expand_c_form fs (find_form1 fs [i; flt]) c
    | F(l) ->
      expand_c_form fs (find_form1 fs l) c
    | A[x] ->
      if (List.mem c.attack_events x ~equal:(=))
      then f
      else expand_c_form fs (find_form1 fs [x]) c
    | A[i;atck] ->
      if (List.mem c.input_flows i ~equal:(=)) && (List.mem c.attacks atck ~equal:(=))
      then f
      else expand_c_form fs (find_form1 fs [i; atck]) c
    | A(l) ->
      expand_c_form fs (find_form1 fs l) c
    | D[x] ->
      if (List.mem c.defense_events x ~equal:(=))
      then f
      else expand_c_form fs (find_form1 fs [x]) c
    | D(l) ->
      expand_c_form fs (find_form1 fs l) c
    | And(y) ->
      And(List.map y ~f:(fun x -> expand_c_form fs x c))
    | Or(y) ->
      Or(List.map y ~f:(fun x -> expand_c_form fs x c)) 
    | N_of(j, fl) ->
      N_of(j, List.map fl ~f:(fun x -> expand_c_form fs x c)) ;;

let rec f_to_pexp f =
  match f with
    | F(a) -> Var(a)
    | A(_) -> raise (Error "f_to_pexp: A() not allowed in fault formula")
    | D(_) -> raise (Error "f_to_pexp: D() not allowed in fault formula")
    | Not(_) -> raise (Error "f_to_pexp: Not() not allowed in fault formula")
    | And(l) -> Pro(List.map l ~f:f_to_pexp)
    | Or(l)  -> Sum(List.map l ~f:f_to_pexp)
    | N_of(j, f) -> f_to_pexp (expand_N_of j f);;

let rec f_to_adexp f =
  match f with
    | F(_) -> raise (Error "f_to_adexp: F() not allowed in attack formula")
    | A(a) -> AVar(a)
    | D(a) -> ANot( AVar(a) )
    | Not(f) -> ANot( f_to_adexp f )
    | And(l) -> APro(List.map l ~f:f_to_adexp)
    | Or(l)  -> ASum(List.map l ~f:f_to_adexp)
    | N_of(j, f) -> f_to_adexp (expand_N_of j f);;

let rec pexp_to_f f =
  match f with
    | TRUE -> And[]
    | FALSE -> Or[]
    | Var(a) -> F(a)
    | Sum(l) -> Or(List.map l ~f:pexp_to_f)
    | Pro(l) -> And(List.map l ~f:pexp_to_f) ;;

let rec adexp_to_f f =
  match f with
    | ATRUE   -> And[]
    | AFALSE  -> Or[]
    | AVar(a) -> A(a)
    | ASum(l) -> Or(List.map l ~f:adexp_to_f)
    | APro(l) -> And(List.map l ~f:adexp_to_f)
    | DSum(l) -> Or(List.map l ~f:adexp_to_f)
    | DPro(l) -> And(List.map l ~f:adexp_to_f)
    | ANot(x)  -> 
       match x with
         ATRUE   -> adexp_to_f AFALSE
       | AFALSE  -> adexp_to_f ATRUE
       | AVar(a) -> Not(D(a))
       | ANot(x) -> adexp_to_f x  (* eliminate double negation *)
       | ASum(x) -> adexp_to_f (APro (List.map x ~f:(fun x -> ANot x))) (* DeMorgan's *)
       | APro(x) -> adexp_to_f (ASum (List.map x ~f:(fun x -> ANot x))) (* DeMorgan's *)
       | DSum(x) -> adexp_to_f (DPro (List.map x ~f:(fun x -> ANot x))) (* DeMorgan's *)
       | DPro(x) -> adexp_to_f (DSum (List.map x ~f:(fun x -> ANot x))) (* DeMorgan's *)
;;

let fnnsimp f =
  let p = f_to_pexp f in
  let s = nnsimp p in
  pexp_to_f s ;;

let fnnsimp_ad f =
  let ad = f_to_adexp f in
  let s = nnsimp_ad ad in
  adexp_to_f s ;;

let find_form fs f c =
  let b = find_form1 fs f in
  fnnsimp (expand_c_form fs b c) ;;

let find_form_ad fs f c =
  let b = find_form1 fs f in
  fnnsimp_ad (expand_c_form fs b c) ;;

let find_form_f fs f c =
  fnnsimp (expand_c_form fs f c) ;;

let find_form_f_ad fs f c =
  fnnsimp_ad (expand_c_form fs f c) ;;

let get_i_name instances name =
  List.find_exn instances ~f:(fun x -> x.i_name = name) ;;

let get_c_name library name =
  List.find_exn  library  ~f:(fun x -> x.name = name);;

let filter_v l bound connections inst acc =
  List.filter l
    ~f:(fun z -> match z with
      | F[i;flt] ->
	let input =
	  List.find_exn connections ~f:(fun (d,_) -> d=(inst.i_name, i)) in
	let (_, (n, o)) = input in
	let nform = [o; flt] in
	let idx = (n, nform) in
	let v = if List.Assoc.mem acc idx ~equal:(=)
	  then (List.Assoc.find_exn acc idx ~equal:(=)) 
	  else 0 in
	if v > bound then false else true
      | A[i;atck] ->
	let input =
	  List.find_exn connections ~f:(fun (d,_) -> d=(inst.i_name, i)) in
	let (_, (n, o)) = input in
	let nform = [o; atck] in
	let idx = (n, nform) in
	let v = if List.Assoc.mem acc idx ~equal:(=)
	  then (List.Assoc.find_exn acc idx ~equal:(=)) 
	  else 0 in
	if v > bound then false else true
      | _ -> true)
;;

(* This function filters out Or[] and And[] from the formula list *)
let filter_exp l = 
  List.filter l
    ~f:(fun z -> match z with
      | Or[] -> false           (* <-- false here means don't keep *)
      | And[] -> false          (* <-- false here means don't keep *)
      | _ -> true)              (* <-- true here means keep everything else *)
;;

let add_to_acc_expand_form i acc =
  let mem = List.Assoc.mem acc i ~equal:(=) in
  let v = if mem
    then (List.Assoc.find_exn acc i ~equal:(=)) 
    else 0 in
  if mem
  then (i, v + 1)::(List.Assoc.remove acc i ~equal:(=))
  else (i, 1)::acc ;;

(* expand_form only for defense formula, given an instance input name 
   used by expand_form *)
let rec expand_form_df inst_i_name df =
  match df with
    | D[x]   -> D[inst_i_name; x]
    | And(x) -> And( List.map x ~f:(fun y -> expand_form_df inst_i_name y) )
    | Or(x)  -> Or( List.map x ~f:(fun y -> expand_form_df inst_i_name y) )
    | _      -> raise (Error "expand_form_df: unexpected formula");;

let match_a_to_d ans inst_i_name x =
   match ans with 
      None -> A[inst_i_name; x] 
    | Some (_, df) -> And[ A[inst_i_name; x]; Not(expand_form_df inst_i_name df) ];;

let rec expand_form library instances connections f inst c acc =
  match f with
    | F[x] -> F[inst.i_name; x]
    | A[x] -> 
        let ans = List.find c.defense_profiles ~f:(fun (event, _) -> x = event) in
        (* if None, then there's no defense for this attack 
           else, match up the attack with the defense formula *)
           match_a_to_d ans inst.i_name x
    | D(_) -> raise (Error "expand_form: D")
    | F[i;flt] -> 
      let input = List.find_exn connections ~f:(fun (d,_) -> d=(inst.i_name, i)) in
      let (_, (n, o)) = input in
      let nform = [o; flt] in
      let ni = get_i_name instances n in
      let nacc = add_to_acc_expand_form (inst.i_name, [i; flt]) acc in
      let (_, v) = List.hd_exn nacc in
      if v>1
      then Or[ ] (* used to be: raise (Error "expand_form: cycle in fault formulas detected") *)
      else cons_form library instances connections (F nform) ni nacc
    | A[i;atck] -> 
      let input = List.find_exn connections ~f:(fun (d,_) -> d=(inst.i_name, i)) in
      let (_, (n, o)) = input in
      let nform = [o; atck] in
      let ni = get_i_name instances n in
      let nacc = add_to_acc_expand_form (inst.i_name, [i; atck]) acc in
      let (_, v) = List.hd_exn nacc in
      if v>1
      then Or[ ] (* used to be: then raise (Error "expand_form: cycle in attack formulas detected") *)
      else cons_form_ad library instances connections (A nform) ni nacc
    | F(_) -> raise (Error "expand_form: F")
    | A(_) -> raise (Error "expand_form: A")
    | Or(l) ->
      Or(filter_exp
	   (List.map
	      (filter_v l 0 connections inst acc)
	      ~f:(fun x -> expand_form library instances connections x inst c acc)))
    | And(l) ->
      And(filter_exp
	    (List.map
	       (filter_v l 0 connections inst acc)
	       ~f:(fun x -> expand_form library instances connections x inst c acc)))
    | N_of(_, _) ->
      raise (Error "expand_form: n_of")
and
    cons_form library instances connections form inst acc =
  let cn = inst.c_name in
  let c = get_c_name library cn in
  let fs = c.fault_formulas in
  let f = find_form_f fs form c in
  expand_form library instances connections f inst c 
    (match form with
      | F(l) -> add_to_acc_expand_form (inst.i_name, l) acc
      | _ -> acc) 
and
    cons_form_ad library instances connections form inst acc =
  let cn = inst.c_name in
  let c = get_c_name library cn in
  let fs = c.attack_formulas in
  let f = find_form_f_ad fs form c in
  expand_form library instances connections f inst c 
    (match form with
      | A(l) -> add_to_acc_expand_form (inst.i_name, l) acc
      | _ -> acc)      
;;

let rec get_c_ind ?(ind = 0) b c  =
  match c with
    | [] -> -1
    | e::x -> if (e=b)
      then ind
      else get_c_ind ~ind:(ind + 1) b x;;

let findList b l default =
  if (List.exists l ~f:(fun (d,_) -> d=b) )
  then let (_, f) = List.find_exn l ~f:(fun (d,_) -> d=b) in
       f
  else default ;;

let rec cons_tree library instances f =
  match f with
    | F[a; b] ->
      let i = get_i_name instances a in
      let cn = i.c_name in
      let c = get_c_name library cn in
      let be = get_c_ind b c.basic_events in
      let (l, t) = List.nth_exn c.event_info be in
      let exp =  findList b i.exposures t in
      let lam =  findList b i.lambdas l in
      Leaf((a,b), lam, exp)
    | A(_)   -> raise (Error "cons_tree: A() not allowed in fault formula")
    | D(_)   -> raise (Error "cons_tree: D() not allowed in fault formula")
    | Not(_) -> raise (Error "cons_tree: Not() not allowed in fault formula")
    | Or(l)  -> SUM (List.map l ~f:(fun x -> cons_tree library instances x))
    | And(l) -> PRO (List.map l ~f:(fun x -> cons_tree library instances x))
    | N_of(j, fl) -> cons_tree library instances (expand_N_of j fl);;

let rec cons_dtree library instances f =
  match f with
    | F(_) -> raise (Error "cons_dtree: F() not allowed in defense formula")
    | A(_) -> raise (Error "cons_dtree: A() not allowed in defense formula")
    | D[a; b] ->
      let i = get_i_name instances a in
      let cn = i.c_name in
      let c = get_c_name library cn in
      let de = get_c_ind b c.defense_events in
      let rigor = List.nth_exn c.defense_rigors de in
      DLeaf((a,b), rigor)
    | N_of(j, fl) -> cons_dtree library instances (expand_N_of j fl)
    | Or(l) -> DSUM (List.map l ~f:(fun x -> cons_dtree library instances x))
    | And(l) -> DPRO (List.map l ~f:(fun x -> cons_dtree library instances x)) ;;

let rec cons_adtree library instances f =
  match f with
    | F(_) -> raise (Error "cons_adtree: F() not allowed in attack formula")
    | A[a; b] ->
      let i = get_i_name instances a in
      let cn = i.c_name in
      let c = get_c_name library cn in
      let be = get_c_ind b c.attack_events in
      let likelihood = List.nth_exn c.attack_info be in
      ALeaf((a,b), likelihood)
    | N_of(j, fl) -> cons_adtree library instances (expand_N_of j fl)
    | Or(l) -> ASUM (List.map l ~f:(fun x -> cons_adtree library instances x))
    | And[af; Not(df)] -> 
       let d = cons_dtree library instances df
       and a = cons_adtree library instances af
       in  C( a, d )
    | And(l) -> APRO (List.map l ~f:(fun x -> cons_adtree library instances x)) ;;

(**/**)
(** Given a library of components and a model, synthesize the fault tree
    corresponding to the top-level event of the model. *)
let model_to_ftree library model =
  let i = model.instances in
  let c = model.connections in
  let (name, top) = model.top_fault in
  let inst = get_i_name i name in
  let cf = cons_form library i c top inst [] in
  cons_tree library i cf ;;

(**/**)
(** Given a library of components and a model, synthesize the attack-defense tree
    corresponding to the top-level event of the model. *)
let model_to_adtree library model =
  let i = model.instances in
  let c = model.connections in
  let (name, top) = model.top_attack in
  let inst = get_i_name i name in
  let cf = cons_form_ad library i c top inst [] in
  cons_adtree library i cf
  ;;

(* This is code that helps convert libraries written using the previous
   version of the modeling language to libraries for the current modeling
   language. This is done by updating formulas.
*)

let rec update_comp_form flt f iflows =
  match f with
    | F[a] -> if (List.mem iflows a ~equal:(=))
      then F[a; flt]
      else f
    | Or(l) ->
      Or(List.map
	   l
	   ~f:(fun x -> update_comp_form flt x iflows))
    | And(l) ->
      And(List.map
	    l
	    ~f:(fun x -> update_comp_form flt x iflows))
    |N_of(i, l) ->
      N_of(i,(List.map
		l
		~f:(fun x -> update_comp_form flt x iflows))) 
    | _ -> f;;

let update_comp_forms fs iflows =
  List.map
    fs
    ~f:(fun (d, f) ->
      match d with
	| [_; flt] -> (d, update_comp_form flt f iflows)
	| _ -> (d, f))
;;

let update_component c =
  {name = c.name;
   input_flows = c.input_flows;
   output_flows = c.output_flows;
   faults = c.faults;
   basic_events = c.basic_events;
   event_info = c.event_info;
   fault_formulas = update_comp_forms c.fault_formulas c.input_flows;
   attacks = [];
   attack_events = [];
   attack_info = [];
   attack_formulas = [];
   defense_events = [];
   defense_rigors = [];
   defense_profiles = [];
   } ;;
   
let update_library l = List.map l ~f:update_component ;;

