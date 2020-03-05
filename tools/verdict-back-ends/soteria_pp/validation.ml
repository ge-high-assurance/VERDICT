(* 

Copyright © 2019-2020 General Electric Company and United States Government as represented 
by the Administrator of the National Aeronautics and Space Administration.  All Rights Reserved.

Authors: Kit Siu, Hongwei Liao, Mike Noorman, Heber Herencia-zapana
Date: 2017-12-15

Updates: 9/10/2018, Kit Siu, modified to work with models that includes attack-defense.
         4/17/2019, Heber Herencia-zapana, added functions that call all the library & model checks.

*)

(**
 
Validation checks are provided to assist the end-user in building component
library and models created following the modeling constructs defined for fault
tree and attack-defense tree. Checks include those for the component library
only, for the model only, and for the model relative to the library.

Validation checks for component library only:

   - {b checkLibrary_componentUnique} : A library of components is a list of
   components, such that no two components have the same name.

   - {b checkLibrary_nonEmptyFaults} : All components must support at least one
   fault, otherwise there is nothing to analyze.

   - {b checkLibrary_nonEmptyAttacks} : All components must support at least one
   attack, otherwise there is nothing to analyze.

   - {b checkLibrary_disjointInputFlowsandBasicEvents} : Check that the names
   used for input flows and basic events are disjoint.

   - {b checkLibrary_disjointInputFlowsandAttackEvents} : Check that the names
   used for input flows and attack events are disjoint.

   - {b checkLibrary_listsAreConsistentLengths} : Check that the lists are all
   of consistent lengths.

   - {b checkLibrary_allOutputFaultsHaveFormulas} : All flows have a formula
   defined.

   - {b checkLibrary_faultformulasMakeSense} : Check that the fault formulas
   make sense.

   - {b checkLibrary_attackformulasMakeSense} : Check that the attack formulas
   make sense.

   - {b checkLibrary_defenseformulasMakeSense} : Check that the defense formulas
   make sense.

Validation checks for a model only:

   - {b checkModel_inputFlowUnique} : Check that input flows have exactly 1
   instance connected to them.

   - {b checkModel_instanceNameUnique} : All instance names are disjoint.

Validation checks for a model relative to a library:

   - {b checkModel_validConnections} : The connection information is a list of
   pairs consisting of an input flow and an instance.

   - {b checkModel_cnameInstanceIsDefinedInLibrary} : All c_names correspond to
   actual components in our component library.

   - {b checkModel_exposureOfBasicIsDefinedInLibrary} : Elements of the form (a,
   x) in exposures of an instance make sense.

Functions to run all the validation:

   - {b checkLibrary} : Performs all of the library checks.

   - {b checkModel} : Performs all of the model checks.

*)


open Core ;;
open Modeling ;;
open Qualitative ;;


(** A function that checks that all components in a library support at least one flow. Otherwise there is nothing to analyze. 
*)
let rec checkLibrary_nonEmptyFaults library = 
    match library with
	| [] -> Ok "checkLibrary_nonEmptyFaults: pass" 
	| hd::tl -> 
		if (hd.faults <> []) then checkLibrary_nonEmptyFaults tl
		else Error ("Faults not defined for library component " ^ hd.name);		
	;;

(** A function that checks that all components in a library support at least one flow. Otherwise there is nothing to analyze. 
*)
let rec checkLibrary_nonEmptyAttacks library = 
    match library with
	| [] -> Ok "checkLibrary_nonEmptyAttacks: pass" 
	| hd::tl -> 
		if (hd.attacks <> []) then checkLibrary_nonEmptyAttacks tl
		else Error ("Attacks not defined for library component " ^ hd.name);		
	;;

(**/**)
(* Extract the component names from library, store to a list *)
let rec extractCompName library =
      match library with
      | [] -> []
      | hd::tl -> hd.name :: extractCompName tl ;;

(**/**)

(** A function that checks that a library of components is a list of components, such that no two components have the same name.
*) 
 
let checkLibrary_componentUnique library = 
  let dup = List.find_a_dup compare (extractCompName library) in
  match dup with
  | None -> Ok "checkLibrary_componentUnique: pass"
  | Some x -> Error ("Library component not unique - " ^ x);;

(**/**)
let fm2fl f_h =      (* cformula to flow *)
 match f_h with
 | ([],_) -> ""
 | (_::[],_) -> ""
 | (_::_::_::_, _) -> ""
 | ([_;str2],_) -> str2 ;;


(* get more info to print for debug *)
let fm2flout f_h =      (* cformula to flow - output *)
 match f_h with
 | ([],_) -> ""
 | (_::[],_) -> ""
 | (_::_::_::_, _) -> ""
 | ([str1;_],_) -> str1;;

(* function to check the validity of F *)
let rec confirmValidF myF inflowL basicL refL ciaL = (* string list formula -> bool list *)
 let subsume a b = List.for_all a ~f:(fun x -> List.mem b x ~equal:(=)) in
 match myF with
 | F[]       -> [false]
 | F[a]      -> if subsume [a] (List.append basicL refL) then [true] else [false]
 | F[_;a]    -> if subsume [a] ciaL then [true] else [false]
 | F[a;_]    -> if subsume [a] inflowL  then [true] else [false] 
 | F(_::_::_::_) -> [false]
 | A(_)      -> [false]
 | D(_)      -> [false]
 | Not(_)    -> [false]
 | And(a)    -> List.concat (List.map a ~f:(fun x -> confirmValidF x inflowL basicL refL ciaL))
 | Or(a)     -> List.concat (List.map a ~f:(fun x -> confirmValidF x inflowL basicL refL ciaL))
 | N_of(_,a) -> List.concat (List.map a ~f:(fun x -> confirmValidF x inflowL basicL refL ciaL)) ;; 
  
(* function to check the validity of A *)
let rec confirmValidA myA inflowL basicL refL ciaL = (* string list formula -> bool list *)
 let subsume a b = List.for_all a ~f:(fun x -> List.mem b x ~equal:(=)) in
 match myA with
 | A[]       -> [false]
 | A[a]      -> if subsume [a] (List.append basicL refL) then [true] else [false]
 | A[_;a]    -> if subsume [a] ciaL then [true] else [false]
 | A[a;_]    -> if subsume [a] inflowL  then [true] else [false] 
 | A(_::_::_::_) -> [false]
 | F(_)      -> [false]
 | D(_)      -> [false]
 | Not(_)    -> [false]
 | And(a)    -> List.concat (List.map a ~f:(fun x -> confirmValidA x inflowL basicL refL ciaL))
 | Or(a)     -> List.concat (List.map a ~f:(fun x -> confirmValidA x inflowL basicL refL ciaL))
 | N_of(_,a) -> List.concat (List.map a ~f:(fun x -> confirmValidA x inflowL basicL refL ciaL)) ;; 
  
(* function to check the validity of A *)
let rec confirmValidD myD basicL = (* string list formula -> bool list *)
 let subsume a b = List.for_all a ~f:(fun x -> List.mem b x ~equal:(=)) in
 match myD with
 | A[]       -> [false]
 | F(_)      -> [false]
 | D[a]      -> if subsume [a] basicL then [true] else [false]
 | Not(_)    -> [false]
 | And(a)    -> List.concat (List.map a ~f:(fun x -> confirmValidD x basicL))
 | Or(a)     -> List.concat (List.map a ~f:(fun x -> confirmValidD x basicL))
 | N_of(_,a) -> List.concat (List.map a ~f:(fun x -> confirmValidD x basicL)) ;; 
  
(* function to collect all the formula references *)
let rec collectFormulaReferences fmL =
 match fmL with 
 | [] -> []
 | hd::tl -> let (fm_ref, _) = hd in 
   List.hd_exn fm_ref :: (collectFormulaReferences tl) ;;

let rec checkCFormList_formulasMakeSense fmList inflowL basicL refL ciaL comp  =
 match fmList with
 | [] -> "pass"
 | hd::tl -> (* formula by formula *)
   let (_,myF) = hd in
   let validityList = confirmValidF myF inflowL basicL refL ciaL in
   if (List.fold ~init:true ~f:(&&) validityList)
   then checkCFormList_formulasMakeSense tl inflowL basicL refL ciaL comp 
   else "Invalid formula in component " ^ comp ^ ". Check fault formula [" ^ (fm2flout hd) ^ "," ^ (fm2fl hd) ^ "]";;

let rec checkAFormList_formulasMakeSense aList inflowL basicL refL ciaL comp  =
 match aList with
 | [] -> "pass"
 | hd::tl -> (* formula by formula *)
   let (_,myF) = hd in
   let validityList = confirmValidA myF inflowL basicL refL ciaL in
   if (List.fold ~init:true ~f:(&&) validityList)
   then checkAFormList_formulasMakeSense tl inflowL basicL refL ciaL comp 
   else "Invalid formula in component " ^ comp ^ ". Check attack formula [" ^ (fm2flout hd) ^ "," ^ (fm2fl hd) ^ "]";;

let rec checkDFormList_formulasMakeSense dList basicL refL comp  =
 let subsume a b = List.for_all a ~f:(fun x -> List.mem b x ~equal:(=)) in
 match dList with
 | [] -> "pass"
 | hd::tl -> (* formula by formula *)
   let (myE,myF) = hd in
   let validityList = confirmValidD myF basicL 
   and validAttackEvent = subsume [myE] refL in
   if (List.fold ~init:true ~f:(&&) validityList) && validAttackEvent
   then checkDFormList_formulasMakeSense tl basicL refL comp 
   else "Invalid formula in component " ^ comp ^ ". Check defense formula " ^ myE ;;

(**/**)

(** A function that checks that the fault formulas of all library components make sense.  
fault formulas include formula of the form F [c;b], 
where c is a subset of the variables in the union of input_flows, basic events, and c,
or F [c] where c can be defined as a local variable within the list of formulas. 
*)
let rec checkLibrary_faultformulasMakeSense l =
 match l with
 | [] -> Ok "checkLibrary_faultformulasMakeSense: pass"
 | hd::tl -> (* component by component *)
   let fm = hd.fault_formulas in
   let iFL = hd.input_flows 
   and bL = hd.basic_events
   and rL = (collectFormulaReferences fm)
   and ciaL = hd.faults
   and comp = hd.name in (* MN - comp name passed for debug printing*)
   let msg = (checkCFormList_formulasMakeSense fm iFL bL rL ciaL comp) in
   if msg = "pass"
   then checkLibrary_faultformulasMakeSense tl 
   else Error msg ;;

(** A function that checks that the attack formulas of all library components make sense.  
Attack formulas include formula of the form A [c;b]  
where c is a subset of the variables in the union of input_flows, attack events, and c, 
or A [c], where c can be defined as a local variable within the list of formulas. 
*)
let rec checkLibrary_attackformulasMakeSense l =
 match l with
 | [] -> Ok "checkLibrary_attackformulasMakeSense: pass"
 | hd::tl -> (* component by component *)
   let fm = hd.attack_formulas in
   let iFL = hd.input_flows 
   and bL = hd.attack_events
   and rL = (collectFormulaReferences fm)
   and ciaL = hd.attacks
   and comp = hd.name in (* MN - comp name passed for debug printing*)
   let msg = (checkAFormList_formulasMakeSense fm iFL bL rL ciaL comp) in
   if msg = "pass"
   then checkLibrary_attackformulasMakeSense tl 
   else Error msg ;;

(** A function that checks that the defense formulas of all library components make sense.  
Defense formulas include formula of the form D [a], 
where a is a subset of the variables in the defense events
*)
let rec checkLibrary_defenseformulasMakeSense l =
 match l with
 | [] -> Ok "checkLibrary_defenseformulasMakeSense: pass"
 | hd::tl -> (* component by component *)
   let fm = hd.defense_profiles
   and bL = hd.defense_events
   and rL = hd.attack_events
   and comp = hd.name in (* MN - comp name passed for debug printing*)
   let msg = (checkDFormList_formulasMakeSense fm bL rL comp) in
   if msg = "pass"
   then checkLibrary_defenseformulasMakeSense tl 
   else Error msg ;;


(** A function that checks that the library component names used for input flows and basic events are disjoint. 
*)
let rec checkLibrary_disjointInputFlowsandBasicEvents l =
	match l with
	| [] -> Ok "checkLibrary_disjointInputFlowsandBasicEvents: pass"
	| hd::tl -> (* component by component *)
		let iUb = List.append hd.input_flows hd.basic_events in (* may not work anymore*)
		if (iUb = []) || (List.length iUb) = (List.length (removeDups iUb))
		then checkLibrary_disjointInputFlowsandBasicEvents tl
		else Error ("Names used for input_flows and basic_events are not disjoint in component " ^ hd.name);;

(** A function that checks that the library component names used for input flows and attack events are disjoint. 
*)
let rec checkLibrary_disjointInputFlowsandAttackEvents l =
	match l with
	| [] -> Ok "checkLibrary_disjointInputFlowsandAttackEvents: pass"
	| hd::tl -> (* component by component *)
		let iUb = List.append hd.input_flows hd.basic_events in
		if (iUb = []) || (List.length iUb) = (List.length (removeDups iUb))
		then checkLibrary_disjointInputFlowsandAttackEvents tl
		else Error ("Names used for input_flows and attack_events are not disjoint in component " ^ hd.name);;

(**/**)
let rec cf2flist cf = (* list of formula flows -> string list *)
  match cf with
  | [] -> []
  | hd::tl ->
    match hd with
    | ([str1;str2],_) -> List.append ([String.concat ~sep:"-" [str1; str2]]) (cf2flist tl) 
    | ([],_)          -> cf2flist tl 
    | (_::[], _)      -> cf2flist tl
    | (_::_::_::_, _) -> cf2flist tl ;;
(* double recursion needed here, faults2outfaultslist and outfaults2outfaultslist *)

let rec faults2outfaultslist out_str faults =
	match faults with
	| [] -> []
	| f_hd::f_tl -> List.append ([String.concat ~sep:"-" [out_str; f_hd]]) (faults2outfaultslist out_str f_tl) ;;

let rec outfaults2outfaultslist out faults = (* list of formula flows -> string list *)
	match out with
	| [] -> []
	| out_hd::out_tl ->	
		List.append (faults2outfaultslist out_hd faults) (outfaults2outfaultslist out_tl faults) ;;			
(**/**)

(** A function that checks that all library component flows have a formula defined.
*)
let rec checkLibrary_allOutputFaultsHaveFormulas l =
  match l with
  | [] -> Ok "checkLibrary_allOutputFaultsHaveFormulas: pass"
  | hd::tl -> (* component by component *)
    if ( List.sort ~compare:(fun x y -> ~- (Pervasives.compare x y)) (outfaults2outfaultslist hd.output_flows hd.faults)  
			= List.sort ~compare:(fun x y -> ~- (Pervasives.compare x y)) (cf2flist hd.fault_formulas) )
    then checkLibrary_allOutputFaultsHaveFormulas tl
    else Error ("Not all output faults have formulas, check component " ^ (hd.name));;


(** A function that checks that the lists within each library component are all of consistent lengths, 
e.g., the length of basic_events is the same as the length of event_info,
the length of attack_events is the same as the length of attack_info,
the length of defense_events is the same as the length of defense_rigors,
Also, the length of faults * output_flows is the length of fault formulas,
the length of attacks * output_flows is the length of attack formulas.
*)		
let rec checkLibrary_listsAreConsistentLengths l =
	match l with
	| [] -> Ok "checkLibrary_listsAreConsistentLengths: pass"
	| hd::tl -> (* component by component *)
		let b_l1 = List.length hd.basic_events = List.length hd.event_info
		and b_l2 = List.length hd.faults * List.length hd.output_flows = List.length hd.fault_formulas
		and b_l3 = List.length hd.attack_events = List.length hd.attack_info
		and b_l4 = List.length hd.attacks * List.length hd.output_flows = List.length hd.attack_formulas
		and b_l5 = List.length hd.defense_events = List.length hd.defense_rigors
		in if b_l1 && b_l2 && b_l3 && b_l4 && b_l5
		then checkLibrary_listsAreConsistentLengths tl
		else if not(b_l1) then Error ("Basic events and event info are of inconsistent lengths in component " ^ hd.name)
		else if not(b_l2) then Error ("Faults and fault formulas lists are of inconsistent lengths in component " ^ hd.name)
		else if not(b_l3) then Error ("Attack events and attack info are of inconsistent lengths in component " ^ hd.name)
		else if not(b_l4) then Error ("Attacks and attack formulas lists are of inconsistent lengths in component " ^ hd.name)
		else if not(b_l5) then Error ("Defense events and defense rigors are of inconsistent lengths in component " ^ hd.name)
		else Error ("Basic events & event info, faults & fault formulas, attack events & attack info, attacks & attack formulas are of inconsistent lengths in component " ^ hd.name)
		;;

(**/**)
(* Extract "input flow" tuple from the "connection" list of a model, store to a list *)
let rec extractInputFlow connectionList =
      match connectionList with
      | [] -> []
      | hd::tl ->
            let ((receiver, address), _) = hd in
            (receiver, address) :: extractInputFlow tl
      ;;

(* Remove duplicate items from the sorted list*)
let rec deleteDuplicate sortedList =
      match sortedList with
      | [] -> []
      | x :: [] -> x :: []
      | x :: y :: rest ->
            if x = y then deleteDuplicate (y :: rest)
            else x :: deleteDuplicate (y :: rest);;
(**/**)

(** A function that checks that in the connections of a model, input flows have at most 1 instance connected to them.
*)
let checkModel_inputFlowUnique model = 
  let inputFlowList = extractInputFlow model.connections in
  (* Sort the list *)
  let sortedList = List.sort ~compare:(fun x y -> ~- (Pervasives.compare x y)) inputFlowList in  
  let uniqueList = deleteDuplicate sortedList in
	if (List.length inputFlowList) = (List.length uniqueList)
	then Ok "checkModel_inputFlowUnique: pass"
	else Error "One of the input_flows in the model has more than one connections made to it.";;

(**/**)
(* Extract the "i_names" from the list of "instances" of a "model", store to a list *)
let rec extractIName instanceList = 
	match instanceList with
	| [] -> [] 
	| hd::tl -> 
		hd.i_name :: extractIName tl
	;;

(* Remove duplicate items from the sorted list*)
let rec deleteDuplicate sortedList = 
	match sortedList with 
	| [] -> [] 
	| x :: [] -> x :: [] 
	| x :: y :: rest -> 
		if x = y then deleteDuplicate (y :: rest) 
		else x :: deleteDuplicate (y :: rest);;
(**/**)

(**	A function that checks that in a model all instance names are disjoint.  
*)
let checkModel_instanceNameUnique model = 
  let iNameList = extractIName model.instances in
  (* Sort the list *)
  let sortedList = List.sort ~compare:(fun x y -> ~- (Pervasives.compare x y)) iNameList in	
  let uniqueList = deleteDuplicate sortedList in
	if (List.length iNameList) = (List.length uniqueList)
	then Ok "checkModel_instanceNameUnique: pass"
	else Error "Model instance names are not unique";;

(**/**)
let rec checkModel_validConnections_rec c i lib =
	match c with
	| [] -> Ok "checkModel_validConnections: pass"
	| hd::tl -> 
		let ((receiver, raddress), (sender, saddress)) = hd in
		(* --- check (receiver, raddress) duple --- *)
		(* get the instance *)
		let foundInst = [try List.find_exn i ~f:(fun x -> x.i_name = receiver) with Not_found -> (makeInstance ~i:"nil" ~c:"" ())] in
 		(* get the component from the library  *)
 		let foundComp = [try List.find_exn lib ~f:(fun x -> x.name = (List.hd_exn foundInst).c_name) with Not_found -> 
 			{name="nil"; input_flows=[]; output_flows=[]; 
 			 faults=[]; basic_events=[]; event_info=[]; fault_formulas=[];
 			 attacks=[]; attack_events=[]; attack_info=[]; attack_formulas=[];
 			 defense_events=[]; defense_profiles=[]; defense_rigors=[]; } ] in
		(* get the address *)
		let foundAddr = [try List.find_exn (List.hd_exn foundComp).input_flows ~f:(fun x -> x = raddress) with Not_found -> "nil"] and 
		(* --- check (sender, saddress) duple --- *)
		(* get the instance *) 
			foundSndr = [try List.find_exn i ~f:(fun x -> x.i_name = sender) with Not_found -> (makeInstance ~i:"nil" ~c:"" ())] in
 		(* get the component from the library *)
 		let foundSomp = [try List.find_exn lib ~f:(fun x -> x.name = (List.hd_exn foundSndr).c_name) with Not_found -> 
 			{name="nil"; input_flows=[]; output_flows=[]; 
 			 faults=[]; basic_events=[]; event_info=[]; fault_formulas=[];
 			 attacks=[]; attack_events=[]; attack_info=[]; attack_formulas=[];
 			 defense_events=[]; defense_profiles=[]; defense_rigors=[]; } ] in
		(* get the address *)
		let foundSddr = [try List.find_exn (List.hd_exn foundSomp).output_flows ~f:(fun x -> x = saddress) with Not_found -> "nil"] in

		if (List.hd_exn foundInst).i_name = "nil" then Error ("Invalid connection: this is not an instance of a component in the model: " ^ receiver)
		else if (List.hd_exn foundComp).name = "nil" then Error ("Invalid connection: this instantiation references a component that is not in the library: " ^ receiver)
		else if (List.hd_exn foundAddr) = "nil" then Error ("Invalid connection: this is not a valid component input from the library: (" ^ receiver ^ ", " ^ raddress ^ ")")
		else if (List.hd_exn foundSndr).i_name = "nil" then Error ("Invalid connection: this is not an instance of a component in the model: " ^ sender)
		else if (List.hd_exn foundSomp).name = "nil" then Error ("Invalid connection: this instantiation references a component that is not in the library: " ^ sender)
		else if (List.hd_exn foundSddr) = "nil" then Error ("Invalid connection: this is not a valid component input from the library: (" ^ sender ^ ", " ^ saddress ^ ")")
		else checkModel_validConnections_rec tl i lib ;;
(**/**)

(** A function that checks that the model connections, relative to a library, is a list of pairs consisting of an input flow and an instance.
*)
let checkModel_validConnections m lib = 
	let c = m.connections and i = m.instances in
	checkModel_validConnections_rec c i lib;;


(**/**)
exception Model_InstanceNameNotDefinedInLibrary;;

(* list of name from library *)
let rec library2namelist l = 
	match l with
	| [] -> []
	| hd::tl -> hd.name :: library2namelist tl ;;
(**/**)

(** A function that checks that all in a model, relative to a library, the model c_names correspond to actual components in the component library.
*)
let rec checkModel_cnameInstanceIsDefinedInLibrary m l =
	(* build a list of names from library *)
	let nl = library2namelist l in
	match m.instances with
	| [] -> Ok "checkModel_cnameInstanceIsDefinedInLibrary: pass"
	| hd::tl -> (* go through instance by instance *)
		if (List.mem nl hd.c_name ~equal:(=))
		then let newm = {instances=tl; connections=m.connections; top_fault=m.top_fault; top_attack=m.top_attack} in
		checkModel_cnameInstanceIsDefinedInLibrary newm l 
		else Error ("Invalid Component: this instantiation references a component that is not in the library: " ^ hd.c_name)
	;;

(**/**)
exception Model_ChangingExposureOfBasicEventNotDefinedInLibrary;;

let rec library2eventlist l = (* list of basic events from library *)
	match l with
	| [] -> []
	| hd::tl -> List.append hd.basic_events (library2eventlist tl) ;;

let rec exposures2explist l = (* list of exposures *)
	match l with
	| [] -> []
	| hd::tl -> 
		let (en, _) = hd in en::(exposures2explist tl) ;;
(**/**)

(** A function that checks that the model elements, relative to a library, of the form (a, x) in exposures of an instance make sense, i.e., there is a basic event named a in the component being instantiated.
*)
let rec checkModel_exposureOfBasicIsDefinedInLibrary m l =
	(* build a list of basic events from library *)
	let nl = library2eventlist l in
	match m.instances with
	| [] -> Ok "checkModel_exposureOfBasicIsDefinedInLibrary: pass"
	(* go through instance by instance *)
	| hd::tl -> 
		let el = exposures2explist hd.exposures in
		let subsume a b = List.for_all a ~f:(fun x -> List.mem b x ~equal:(=)) in
		if (subsume el nl)
		then let newm = {instances=tl; connections=m.connections; top_fault=m.top_fault; top_attack=m.top_attack} in
		checkModel_exposureOfBasicIsDefinedInLibrary newm l 
		else Error "Model attempts to change an invalid basic_event of a library component"
	;;

(** A function that calls all of the library checks
*)
let checkLibrary library = [
  checkLibrary_componentUnique library;
  checkLibrary_nonEmptyFaults library;
  checkLibrary_nonEmptyAttacks library;
  checkLibrary_disjointInputFlowsandBasicEvents library;
  checkLibrary_disjointInputFlowsandAttackEvents library;
  checkLibrary_listsAreConsistentLengths library;
  checkLibrary_allOutputFaultsHaveFormulas library;
  checkLibrary_faultformulasMakeSense library;
  checkLibrary_attackformulasMakeSense library;
  checkLibrary_defenseformulasMakeSense library ];;
 
(** A function that calls all of the model checks
*)
let checkModel library model = [
  checkModel_instanceNameUnique model ; 
  checkModel_cnameInstanceIsDefinedInLibrary model library; 
  checkModel_exposureOfBasicIsDefinedInLibrary model library; 
  checkModel_validConnections model library; 
  checkModel_inputFlowUnique model ];; 

