(* 

Copyright © 2019-2020 General Electric Company and United States Government as represented 
by the Administrator of the National Aeronautics and Space Administration.  All Rights Reserved.

Author: Pete Manolios
Date: 2017-12-15

Updates: 5/10/2018, Kit Siu, added severityCalc and supporting measures for ADT
         7/23/2018, Kit Siu, added likelihood calculations (formerly severityCalc)
         5/23/2022, Chris Alexander, Multi-Rule cutset likelihoods to include unimplemented defenses
*)

(**
   The top-level functions for the quantitative analysis of fault trees are as follows.

   - {b probErrorCut} : a function that given a fault tree, computes the
   top-level probability of a fault.

   - {b probErrorCutImp} : a function that given a fault tree, computes
   importance measures.
   
   If cutsets are available, the next two functions can compute probabilities
   and importance metrics without having to recompute them.
   
   - {b probErrorCutC} : a function that given a fault tree and cutsets,
   computes the top-level probability of a fault.

   - {b probErrorCutCImp} : a function that given a fault tree and cutsets,
   computes importance measures.

   These functions compute the likelihood of attack using the cutsets.
   
   - {b likelihoodCut} : a function that given an attack-defense tree and cutsets,
   computes the top-level likelihood of an attack.

   - {b likelihoodCutImp} : a function that given an attack-defense tree and cutsets,
   computes the importance measures.

*)

open Core ;;
open FaultTree ;;
open AttackDefenseTree ;;
open Qualitative ;;

exception Error of string ;;

(**/**)
(** A function to compute the probability of failure using an approximate
    method. The probability of failure is lambda * exposure time, which for
    reasonable values of lamba, exposure time is a good approximation.  Also, the
    assumption is that all events are independent and for sums, I use addition,
    another approximation. Notice that these approximations are conservative, so
    that I always get a probability that is >= the true probability.

    I do not handle the case where the same event appears in the fault tree. To
    deal with that, one has to use the cut set methods, which appear below.
*)
let rec probErrorApprox t =
  match t with
    | Leaf (_, l, e) -> l *. e
    | SUM (tree) ->
      List.fold_left (List.map tree ~f:probErrorApprox) ~init:0. ~f:( +. ) 
    | PRO (tree) ->
      List.fold_left (List.map tree ~f:probErrorApprox) ~init:1. ~f:( *. )  
;;

(** A function to compute the probability of failure using an exact method. The
    probability of failure is
    
    1-e^(-lambda * exposure time). 

    Also, the assumption is that all events are independent and for sums, I use
    the correct formula. Since I'm assuming independence, I can simplify the
    inclusion/exclusion formula as follows:

    P[Sum E] = 1-P[Pro E'] = 1 - *_{i in 1..n}P[ei']
    = 1 - *_{i in 1..n}(1-P[ei]) 
*)
let probDisjunction l =
  1. -. (List.fold_left 
	   (List.map ~f:(fun x -> 1. -. x) l)
	   ~init:1.
	   ~f:( *. )) ;;
  
let rec probError t =
  match t with
    | Leaf (_, l, e) -> 1.0 -. (exp (-. (l *. e)))
    | SUM (tree) ->
      probDisjunction (List.map tree ~f:probError)
    | PRO (tree) ->
      List.fold_left (List.map tree ~f:probError) ~init:1. ~f:( *. )  
;;

let rec eventProbsAux t =
  match t with
    | Leaf (var, _, _) -> [ (var, (probError t)) ] 
    | SUM s -> List.concat (List.map s ~f:eventProbsAux)
    | PRO p -> List.concat (List.map p ~f:eventProbsAux)
;;

(* There can be repeated events *)
let eventProbs t = removeDups (eventProbsAux t) ;;

let rec nexte e n =
  match e with
    | [] -> []
    | [a] -> if a=n then [] else [a+1]
    | a::x -> if a=n then
	let v = nexte x (n-1) in
	match v with
	  | [] -> []
	  | a::_ -> (a+1)::v
      else (a+1)::x ;;

let rec chooseiA ?(acc = []) e n =
  let e1 = nexte e n in
  if e1=[] then List.rev (e::acc)
  else chooseiA ~acc:(e::acc) e1 n ;;

let choosei i s =
  let l = List.length s in
  if (i<1 || i>l) then raise (Invalid_argument "i>l in choosei")
  else let e = List.rev (List.concat (chooseiA [0] (i-1))) in
       let c = chooseiA e (l-1) in
       List.map c ~f:(fun x -> (List.map x ~f:(fun y -> List.nth_exn s y))) ;;

let chooseiC i c = 
  let x = choosei i c in
  List.map x ~f:(fun y -> removeDups (List.concat y));;

let computeSopProb l pa =
  List.fold_left 
  (List.map l
     ~f:(fun y ->
       List.fold_left (List.map y ~f:(fun x -> List.Assoc.find_exn pa x ~equal:(=)) ) ~init:1. ~f:( *. ) )
  )
  ~init:0. ~f:( +. )
;;

let rec chs n k =
  if k=1 then (float n)
  else let n1 = (float (n+1)) in
       let k1 = (float k) in
       let t = (n1 -. k1) /. k1 in
       (chs n (k-1)) *. t ;;

(** This is the largest number of elements a subset of a cutset can contain. If
    we exceed this number, we stop calculating the probabilities and therefore
    return an interval which contains the true probability. *)
let max_choose_size = 100000. ;;

let rec probCutSop l pa len i m ans1 ans2 =
  if (chs len i) > max_choose_size then
    let a1 = min ans1 ans2 in
    let a2 = max ans1 ans2 in
    (a1, a2) 
  else if i=len then
    let ans = ans2 +. (m *. (computeSopProb (chooseiC i l) pa)) in
    (ans, ans)
  else let c = computeSopProb (chooseiC i l) pa in
       probCutSop l pa len (i+1) (-1. *. m) ans2 (ans2 +. (m *. c)) ;;

let probErrorCutCalc cs pa =
  match cs with
    | Var v ->
      let a = (List.Assoc.find_exn pa v ~equal:(=)) in (a, a)
    | Sum s ->
      let l = (List.map s ~f:(fun x -> match x with
	| Var v -> [v]
	| Pro p -> (List.map p ~f:(fun y -> match y with
	    | Var v -> v
	    | _ -> raise (Error "probErrorCutCalc 1 exception")) )
	| _ -> raise (Error "probErrorCutCalc 2 exception"))
		 ) in
      let len = List.length l in
      probCutSop l pa len 1 1. 0. 0.
    | Pro p ->
      let p1 = List.map p
	~f:(fun x -> match x with
	    Var v -> v
	  | _ ->  raise (Invalid_argument "In pro p of probErrorCutCalc"))
	 in
      let a = List.fold_left (List.map p1 ~f:(fun x -> List.Assoc.find_exn pa x ~equal:(=))) ~init:1. ~f:( *. ) in
      (a, a)
    | FALSE -> (0.,0.)
    | _ -> raise (Invalid_argument "In probErrorCutCalc")
;;

(**/**)

(** A function that given a fault tree and cutsets, computes the top-level
    probability of a fault. *)
let probErrorCutC t cutset =
  let probAlist = eventProbs t in
  probErrorCutCalc cutset probAlist ;;

(** A function that given a fault tree, computes the top-level probability of a
    fault. *)
let probErrorCut t =
  let cutset = cutsets t in
  probErrorCutC t cutset ;;

(** A function that given a fault tree and cutsets, computes importance measures. *)
let probErrorCutCImp t cutset =
  let probAlist = eventProbs t in
  let (plo, phi) = probErrorCutCalc cutset probAlist in
  let p = (plo +. phi) /. 2. in
  let cs = match cutset with
      Var _ -> [cutset]
    | Sum s -> s
    | FALSE -> [FALSE]
    | _ -> raise (Error "probErrorCutImp exception") in 
  let cerr = List.map cs ~f:(fun x -> probErrorCutCalc x probAlist) in
  let c = List.map2_exn cs cerr
    ~f:(fun x y -> let (p1, _) = y in
		let imp = p1 /. p in 
		(x, p1, imp))
    in
  List.sort ~compare:(fun x y -> let (_, _, xi) = x in
			     let (_, _, yi) = y in
			     -(compare xi yi))
    c ;;

(** A function that given a fault tree, computes importance measures. *)
let probErrorCutImp t =
  let cutset = cutsets t in
  probErrorCutCImp t cutset ;;

(**/**)
(** A generalization of majority. Instead of 2-of-3, this is
   n of L, where L is a list of nodes and n is <= len.L *)
let n_of_L n l =
  let ch = choosei n l in
  let s = List.map ch ~f:(fun x -> PRO x) in 
  if n=1 then (SUM l)
  else (SUM s) ;;

let lFromErr err = log(1. /. (1. -. err)) ;;

(* AD-Tree *)

(** A function to compute the risk of an attack. 
    The likelihood of success of an attack is 1. 
    The likelihood of failure of a defense is 1e^(-DAL), 
    where DAL = design assurance level.
    
    The assumption is that all events are independent. This function does
    not simplify the tree formula to remove duplicate events, for example, so 
    should only be considered as an approximation for now.
*)
  
(** function for adding a list of floats
*)
let gAdd l = min 1. (List.fold l ~init:0. ~f:(+.));;

(** function for multiplying a list of floats
*)
let gMult l = List.fold l ~init:1. ~f:( *.);;

(** function for negation 
*)
let gNot lop = 10. ** (-. float(lop));;

(** function for max 
*)
let gMax l = List.fold l ~init:(List.hd_exn l) ~f:(max);;

(** function for min 
*)
let gMin l = List.fold l ~init:(List.hd_exn l) ~f:(min);;

(** A function to compute the severity of an attack given an attack-defense tree 
*)  
let rec assuranceCalcApprox dt =
  match dt with
    | DLeaf (_, dal) -> dal
    | DSUM (dt) -> gMax (List.map dt ~f:assuranceCalcApprox)
    | DPRO (dt) -> gMin (List.map dt ~f:assuranceCalcApprox)
;;
  
let rec likelihoodCalcApprox adt =
  match adt with
    | ALeaf (_, likelihood) -> likelihood (*1.0*) 
    | ASUM (tree) -> gMax (List.map tree ~f:likelihoodCalcApprox)
    | APRO (tree) -> gMin (List.map tree ~f:likelihoodCalcApprox)
    | C (atree, dtree) -> gMin [likelihoodCalcApprox atree; gNot (assuranceCalcApprox dtree)]
;;

(** This function converts the DALs into a list of (var, likelihoods). *)
let rec eventLikelihoodsAuxDt dt =
  match dt with
    | DLeaf( var, _) -> [ (var, gNot (assuranceCalcApprox dt) ) ]
    | DSUM s -> List.concat (List.map s ~f:eventLikelihoodsAuxDt)
    | DPRO p -> List.concat (List.map p ~f:eventLikelihoodsAuxDt)
;;

let rec eventLikelihoodsAux t =
  match t with
    | ALeaf (var, _) -> [ (var, (likelihoodCalcApprox t)) ] 
    | ASUM s -> List.concat (List.map s ~f:eventLikelihoodsAux)
    | APRO p -> List.concat (List.map p ~f:eventLikelihoodsAux)
    | C (atree, dtree) -> List.concat [ eventLikelihoodsAux atree;
                                        eventLikelihoodsAuxDt dtree ]
;;

(*  Given a list of SOPs, create a list of likelihoods for each SOP term
*)
let rec likelihoodCutSOP sop la =
  match sop with
    | [] -> []
    | hd::tl -> let prod = match hd with
                             | AVar (a,d,_)         -> List.Assoc.find_exn la (a,d) ~equal:(=)
                             | ANot ( AVar (a,d,_)) -> List.Assoc.find_exn la (a,d) ~equal:(=)
                             | APro le      -> gMin (likelihoodCutSOP le la)
                             | DPro le      -> gMin (likelihoodCutSOP le la) (* min on d-AND gate b/c assoc list is in terms of likelihood *)
                             | DSum le      -> gMax (likelihoodCutSOP le la) (* allow SOP to include defense Sum gates *)
                             | _            -> raise (Error "likelihoodCutSOP exception")
                in prod :: likelihoodCutSOP tl la
;;

(*  This function uses the cutset list and the association list to calculate the 
    top-level likelihood of success.
*)
let likelihoodCutCalc cs la =
  match cs with
    | AVar (a,d,_)          -> let a = (List.Assoc.find_exn la (a,d) ~equal:(=)) in a
    | ANot ( AVar (a,d,_))  -> let a = (List.Assoc.find_exn la (a,d) ~equal:(=)) in a
    | ASum s         -> let l = (likelihoodCutSOP s la) in (gMax l)
    | APro p         -> gMin (likelihoodCutSOP p la) 
    | DSum s         -> let l = (likelihoodCutSOP s la) in (gMax l)  (* max on d-OR gate b/c assoc list is in terms of likelihood *)
    | DPro p         -> gMin (likelihoodCutSOP p la)                 (* min on d-AND gate b/c assoc list is in terms of likelihood *)
    | AFALSE         -> 0.
    | ATRUE          -> 1.
    | _ -> raise (Invalid_argument "In likelihoodCutCalc")
;;

(*  This function produces an association list of (event, likelihood) tuples.
    There could be repeated events, so remove the duplicates.
*)
let eventLikelihoods t = removeDups (eventLikelihoodsAux t) ;;

(** A function that given an attack-defense tree and cutsets, computes the top-level
    likelihood of success. *)
let likelihoodCutC t cutset =
  let likeliAlist = eventLikelihoods t in
  likelihoodCutCalc cutset likeliAlist ;;

(** A function that given an attack-defense tree, computes the top-level likelihood of
    success. *)
let likelihoodCut t =
  let cutset = cutsets_ad t in
  likelihoodCutC t cutset ;;

(** A function that given an attack-defense tree and cutsets, computes importance measures. *)
let likelihoodCutCImp t cutset =

  let likeAlist = eventLikelihoods t in (* list of cutset likelihoods *)
  let a = likelihoodCutCalc cutset likeAlist in (* summed likelihood of all cutsets *)
  let cs = match cutset with
             | AVar _ -> [cutset]
             | AFALSE -> [AFALSE]
             | ATRUE  -> [ATRUE]
             | ASum s -> s
             | APro s -> s  (* for the case where the cutset contains C(a,d) *)
             | _ -> raise (Error "likelihoodCutCImp exception") 
             in

  let clikeli = List.map cs ~f:(fun x -> likelihoodCutCalc x likeAlist) in (* list of likelihoods *)

  let c = List.map2_exn cs clikeli ~f:(fun x y ->
            let a1 = y in
            let imp = a1 /. a in
            (x, a1, imp)) in

  List.sort ~compare:(fun x y -> (* sort cutsets by likelihood *)
            let (_, _, xi) = x in let (_, _, yi) = y in -(compare xi yi)) c;;

(** A function that given an attack-defense tree, computes importance measures. *)
let likelihoodCutImp t =
  let cutset = cutsets_ad t in
  likelihoodCutCImp t cutset ;;

