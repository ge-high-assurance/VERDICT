(* 

Copyright @ 2020 General Electric Company. All Rights Reserved.

Authors: Heber Herencia-Zapana, Kit Siu
Date: 25/03/2019

Updates: 4/18/2019, Kit Siu, added function to generate cutset report using PrintBox.
         9/27/2019, Heber Herencia-zapana, added functions to generate defense profiles
         11/05/2019, Kit Siu, added functions to fill in safety information
         11/15/2019, Kit Siu, added functions to deal with hierarchy
         5/12/2020, Kit Siu, using xml library
         5/18/2020, Heber Herencia-Zapana, added  cyber relations inferring
         5/22/2020, Heber Herencia-Zapana, added safety relations inferring
         5/26/2020, Heber Herencia-Zapana,  safety/cyber Availability dependes on availability only
         8/4/2020, Kit Siu, defense properties on connections generates new components to 
                            represent the connection, with relevant CAPECs and defenses 
                            moved from the vulnerable component to the new connection component          
*)

(**
	The top-level functions for the translation from AADL to .csv files are as follows:
	
	- {b do_arch_analysis} : the top-level function to call to build the library and model
	(in memory) and to do both the safety analysis and the security analysis. This function
	takes the 7 required files read-in as list of lines using In_channel.read_lines. This
	function also requires the output file path to be specified.
	
	- {b libraries_threatConditions} : this function generates a list with the following
	tuple: ((missionID,reqID,defenseType), (lib,mdl)) where missionID is a string with the
	mission identifier, reqID is a string with the requirement identifier, 
	defenseType is either the applicable defense properties or the implemented defense properties 
	(see Defenses.csv for the header names used) and the lib and mdl are the corresponding 
	library and model for the requirement ID and the defenseType.
	
	- {b mainListtag} : this function takes a list of lines (from In_channel.read_lines) and 
	creates an association list.
	
	Some commands for debugging: one way to debug these functions is to execute the following:
	# 
	# let compDepen = mainListtag (In_channel.read_lines "test/CompDep.csv");;
	# let compSafe = mainListtag (In_channel.read_lines "test/CompSaf.csv");;
	# let attack = mainListtag (In_channel.read_lines "test/CAPEC.csv");;
	# let events = mainListtag (In_channel.read_lines "test/Events.csv");;
	# let arch = mainListtag (In_channel.read_lines "test/ScnConnections.csv");;
	# let mission = mainListtag (In_channel.read_lines "test/Mission.csv");;
	# let defense = mainListtag (In_channel.read_lines "test/Defenses.csv");;
	#
	# let deftype = "ApplicableDefenseProperties";;
	#
	# let l_librariesThreats = libraries_threatConditions deftype compDepen compSafe attack events arch mission defense;;
    # let (missionReqIDStr, holdme) = List.hd_exn l_librariesThreats;;
    # let ((reqIDStr,defenseTypeStr), (lib,mdl)) = List.hd_exn holdme;;
	# 
	# checkLibrary lib;;
	# checkModel lib mdl;;
	
*)

open Core ;;
open FaultTree ;;
open AttackDefenseTree ;;
open Qualitative ;;
open Quantitative ;;
open Modeling ;;
open Validation ;;
open TreeSynthesis ;;
open Visualization ;;
open TranslatorPrint ;;

exception Error_csv2soteria of string ;;

let noEmptyList l = List.filter l ~f:(fun x->x<>[]);;
let listgen_aux list = List.map list ~f:(fun x-> String.filter  x ~f:(fun x-> x<>'\"'));;
let list_of_list_gen_aux l = List.map l ~f:(fun x-> listgen_aux x);;
let list_sameDimension l = List.filter l ~f:(fun x-> List.length (List.hd_exn l)=List.length x);;
let listgenerator l = list_sameDimension
                      (list_of_list_gen_aux
                      (List.map (List.map l ~f:(fun x->String.split_on_chars x ~on:[','])) ~f:(fun x-> List.map x ~f:(fun y->String.strip y))));;
let mainList list = List.tl_exn (List.map list ~f:(fun x-> (List.zip_exn (List.hd_exn list)) x));;
let mainListtag file = mainList (listgenerator file);;

(*filter comp tag *)
let compInfo comp tag list = List.filter list ~f:(fun x-> List.mem x (tag,comp) ~equal:(=));;
let comp_find tag list = List.dedup_and_sort ~compare:compare (List.map list ~f:(fun x-> List.Assoc.find_exn x tag ~equal:(=)));;
(* use comp_find2 if list has semi-colon separated elements, like for conjuncted elements *)
let comp_find2 tag list = 
	let tagList = List.map list ~f:(fun x -> List.Assoc.find_exn x tag ~equal:(=)) in
	let tagList_split_flattened = List.concat (List.map tagList ~f:(fun x -> String.split_on_chars x ~on:[';'])) in
	List.dedup_and_sort ~compare:(compare) tagList_split_flattened;;
	
(* - * - * - *)

(* from l_comp_dep we get attacks and attack_formulas (outputs-inputs dependences)*)
let (compType_C, inputPort_C, outputPort_C, inputCIA_C, outputCIA_C)=
    ("Comp",     "InputPort", "OutputPort", "InputCIA", "OutputCIA");;

let comp compName l_comp_dep = compInfo compName compType_C l_comp_dep;;
let comp_filter compName out cia l_comp_dep = compInfo cia outputCIA_C (compInfo out outputPort_C (comp compName l_comp_dep));;
let clean l = List.filter l ~f:(fun x->x<>"");;
let compAttacks name l_comp_dep= comp_find2 inputCIA_C (comp name l_comp_dep);;
let compAttacksOut name l_comp_dep= comp_find outputCIA_C (comp name l_comp_dep);;
let compOut_In name out cia l_comp_dep= let f x tag =  List.Assoc.find_exn x tag ~equal:(=) in
                              let l = comp_filter name out cia l_comp_dep in
                              List.map l ~f:(fun x-> if (f x inputPort_C) <> "" 
                                                  then [ f x inputPort_C ; f x inputCIA_C ] else []);;

(* - * - * - *)
                                                  
(* from l_attack we get attack_events*)
let (compType_A, c_A,               i_A,         a_A,            capec_A, likeli_A)=
    ("CompType", "Confidentiality", "Integrity", "Availability", "CAPEC", "LikelihoodOfSuccess");;
let compAttack name l_attack = compInfo name compType_A l_attack;;
let attack_cia name cia l_attack = 
                          let f x tag = List.Assoc.find_exn x tag ~equal:(=) in
                          let fun_cia x = if f x c_A = cia || f x i_A = cia || f x a_A= cia 
                                          then [f x capec_A] else [] in
                          List.map (compAttack name l_attack)  ~f:fun_cia;;
(* TODO: zip and unzip *)
let attack_events name l_attack = let f_capec x = List.Assoc.find_exn x capec_A ~equal:(=) in
                         List.map (compAttack name l_attack) ~f:f_capec ;;
                         
let attack_info name l_attack= let f_info x = float_of_string (List.Assoc.find_exn x likeli_A ~equal:(=)) in
                         List.map (compAttack name l_attack) ~f:f_info ;;                                                   

(* there could be duplicate attack events. Assuming that each CAPECs in l_attack 
   will each have the same likelihoods, the best way to eliminate duplicates is to 
   zip the attack events and the info lists, dedup, then unzip *)
   
let makeAttackList_AttackInfoList name l_attack =
   let aL = List.zip_exn (attack_events name l_attack) (attack_info name l_attack) in
   List.unzip (List.dedup_and_sort ~compare:compare aL) ;;

(* - * - * - *)

(* from l_defense we get defense_events, defense_rigors and defense_profiles*)
let(compType_D, compInst_D,  capec_D, c_D,               i_D,         a_D,            applProps_D,                   implProps_D,      dal_D)=
   ("CompType", "CompInst",  "CAPEC", "Confidentiality", "Integrity", "Availability", "ApplicableDefenseProperties", "ImplProperties", "DAL");;
let compDefense name l_defense = compInfo name compType_D l_defense;;
let compDefenseCapec name capec l_defense = compInfo capec capec_D (compDefense name l_defense);;
let listCapec name l_defense= let capecType x  = List.Assoc.find_exn x capec_D ~equal:(=) in
                      List.dedup_and_sort ~compare:compare (List.map (compDefense name l_defense) ~f:(fun x->capecType x));;
let compDefenseCapecCIA name capec cia ciaTag l_defense = compInfo cia ciaTag (compDefenseCapec name capec l_defense);;
let filterDefenseCapecCIA name capec cia  defType l_defense =
                 let defenseType x tag = List.Assoc.find_exn x tag ~equal:(=) in
                 let defenseListC = compDefenseCapecCIA name capec cia c_D l_defense in
                 let defenseListI = compDefenseCapecCIA name capec cia i_D l_defense in
                 let defenseListA = compDefenseCapecCIA name capec cia a_D l_defense in
                 let defenCIA lDef  = List.map lDef ~f:(fun x->defenseType x defType) in
                 let filterNULL l =l|>List.dedup_and_sort ~compare:compare |>(fun x->List.filter x ~f:(fun x-> x <> "null")) in
                 let clean l = l |> (fun x->List.filter x ~f:(fun x-> x <> []))|> List.hd_exn in
                 clean
                 [if defenseListC =[] then [] else [(cia,filterNULL(defenCIA defenseListC ))];
                  if defenseListI =[] then [] else [(cia,filterNULL(defenCIA defenseListI ))];
                  if defenseListA =[] then [] else [(cia,filterNULL(defenCIA defenseListA ))]];;
let filterCIA name capec l_defense =
                 let ciaComp x tag = List.Assoc.find_exn x tag ~equal:(=) in 
                 let comp_C = List.map (compDefenseCapec name capec l_defense ) ~f:(fun x->ciaComp x c_D) in
                 let comp_I = List.map (compDefenseCapec name capec l_defense) ~f:(fun x->ciaComp x i_D) in
                 let comp_A = List.map (compDefenseCapec name capec l_defense ) ~f:(fun x->ciaComp x a_D) in 
                 let clean l = l|>(fun x-> List.filter x ~f:(fun x-> x <> "null"))|>List.dedup_and_sort ~compare:compare in
                 clean (List.concat [comp_C;comp_I;comp_A]);;
let filterDefense name  defType l_defense =
                   let filterCIA name capec = List.map (filterCIA name capec l_defense) 
                     ~f:(fun xcia-> filterDefenseCapecCIA name capec xcia  defType l_defense) in
                   let clean l = l|> List.concat |> List.hd_exn in   
                   List.map (listCapec name l_defense) ~f:(fun xcapec->(xcapec,clean(filterCIA name xcapec)));;

let defenseEventsRigors name defTag l_defense = 
                   let f x tag = List.Assoc.find_exn x tag ~equal:(=) in
                   List.dedup_and_sort ~compare:compare (List.map (compDefense name l_defense) ~f:(fun x->(f x defTag,f x dal_D)));;
                                                                                                                                                                                        
(*Heber defense profiles *)
let defenseEventsRigorsAUX name defTag l_defense = 
     let ldefenseDAL = defenseEventsRigors name defTag l_defense in
     let defenseDalUAux = List.map ldefenseDAL ~f:(fun x->(String.split_on_chars (fst x) ~on:[';'], 
                                                           String.split_on_chars (snd x) ~on:[';'])) in 
     let ldefenseDalU = List.concat (List.map defenseDalUAux ~f:(fun (x,y) -> List.zip_exn x y)) in
     List.filter (List.dedup_and_sort ~compare:compare ldefenseDalU) ~f:(fun x -> x <> ("null","null"));;
     
let defenseEvents name defTag l_defense = 
     let def_dal = defenseEventsRigorsAUX name defTag l_defense in
     List.map def_dal ~f:(fun x->fst x);;

let defenseRigors name defTag l_defense = 
     let def_dal = defenseEventsRigorsAUX name defTag l_defense in
     List.map def_dal ~f:(fun x->if (snd x) = "null" then 0 else int_of_string (snd x) );;

(* - * - * - *)

(* from l_defense2nist (along with l_defense) we get defense_profiles*)
let(dprop_D,           dprofile_D)=
   ("DefenseProperty", "NISTProfile");;

let propInfo prop tag list = List.filter list ~f:(fun x-> List.mem x (tag,prop) ~equal:(=)) ;;

let rec makeNIST2DAL_assocList aL l_defense2nist =
   match aL with
      hd::tl -> (let (p,dal) = hd in 
                   let d2nAList = List.concat( propInfo p dprop_D l_defense2nist ) in
                       let nistprofile = List.Assoc.find d2nAList ~equal:(=) dprofile_D in
                          let pp = match nistprofile with
                                     | Some x -> x     (* if found, return the nist profile *)
                                     | None -> p  in   (* otherwise, return the property name *)
                          (pp,dal) :: makeNIST2DAL_assocList tl l_defense2nist)
    | [] -> [];;

let makeDefenseList_DefenseRigorsList comp deftype defense defense2nist =
 let aL = List.zip_exn (defenseEvents comp deftype defense) (defenseRigors comp deftype defense) in
 let f a = 
  (let (p,dal) = a in 
   let strList = String.split_on_chars ~on:[';'] p in
    List.map strList ~f:(fun x -> (x,dal)) ) in  (* this line replicates the defense with the defense DAL *)
  List.unzip (List.concat (List.map (makeNIST2DAL_assocList aL defense2nist) ~f:(fun x -> f x)));;

let rec convertProp2Profile l l_defense2nist =
   match l with
       hd::tl -> 
         (let aL = List.concat (propInfo hd dprop_D l_defense2nist) in
            (let nistprofile = List.Assoc.find aL ~equal:(=) dprofile_D in
               match nistprofile with
                 | Some x -> x (* if found, return the nist profile *)
                 | None -> hd  (* otherwise, return the property name *)
            ) 
         ) :: (convertProp2Profile tl l_defense2nist)
     | [] -> [];;

let defenseProfileAux name defType l_defense  =
    let capec x = fst x  
    and defenList x = snd (snd x) in
    let cp = List.concat (List.map (filterDefense name  defType l_defense) ~f:(fun x-> [(capec x, defenList x)])) in
    let cpsplit = List.map cp ~f:(fun (c,pl) -> (c, (List.map pl ~f:(fun p -> String.split_on_chars ~on:[';'] p)))) in
    let cpconverted = cpsplit in 
    List.map cpconverted ~f:(fun (c,pl) -> (c, List.map pl ~f:(fun l -> List.filter l ~f:(fun x -> x<>"null"))))
    ;;   

(* this function gets rid of the empty And in a list like this 
   [And [D ["deviceAuthentication"]]; And [D ["heterogeneity"]]; And []] *)
let rec eliminateEmptyAnd l =
   match l with
       hd::tl -> (match hd with
                     And[] -> eliminateEmptyAnd tl
                   | _     -> hd::eliminateEmptyAnd tl )
     | [] -> [];;

let rec processDL dl =
   let capec x = fst x
   and defenList x = snd x 
   and andList l = String.split_on_chars ~on:[';'] (String.concat ~sep:";" l)
   and andProfile l = And( List.map l ~f:(fun x -> D[x]))       (* list of strings get ANDed together *) 
   in      
   match dl with
       hd::tl -> (capec hd, Or( eliminateEmptyAnd (
                                   List.map (defenList hd) ~f:(fun x -> andProfile 
                                                                         (let p = andList x in match p with [""] -> [] | _ -> p )
                                ))
                  )) :: processDL tl
     | [] -> [];;
     
(* need to post process the list of defense profiles because 
   if the profile is equal to Or[And[]] then that means there is no defense 
   so eliminate it from the list *)
let rec postProcessDefenseProfile dL =
   match dL with
   hd::tl -> let (c, p) = hd in 
      (match p with 
           Or [] -> postProcessDefenseProfile tl
         | _     -> (c,p) :: postProcessDefenseProfile tl)
   |[] -> [];;

let defenseProfile name defType l_defense =
   let compDefensesList = defenseProfileAux name defType l_defense in
   postProcessDefenseProfile (processDL compDefensesList);;


(* - * - * - *)

(* from l_events we get basic_events and event_info *)
let (eventsH_CompType, eventsH_Event, eventsH_Probability) =
	("Comp",           "Event",       "Probability");;
		
let compEvents name l_events = comp_find eventsH_Event (compInfo name eventsH_CompType l_events);;

(* compEventsInfo must take list of compEvents to construct the list of probabilities in the same order as compEvents *)
let rec compEventsInfo name l_events l_compEvents = 
   match l_compEvents with
   | hd::tl -> 
       (let searchList = compInfo name eventsH_CompType l_events in
        let searchSubList = compInfo hd eventsH_Event searchList in
        let tempList = (comp_find eventsH_Probability searchSubList) in
        List.append (List.map tempList ~f:(fun x -> (float_of_string x,1.0))) (compEventsInfo name l_events tl) )
   | [] -> [];;


(* - * - * - *)

(* from l_comp_saf we get faults and fault_formulas *)
let (compType_S, inputOrEvent_S,     inputIAE_S,       outputPort_S, outputCIA_S) =
	("Comp",     "InputPortOrEvent", "InputIAOrEvent", "OutputPort", "OutputIA");;

let compFaults name l_comp_saf = comp_find2 inputIAE_S (comp name l_comp_saf);;

let compsaf_filter compName out ia l_comp_saf = compInfo ia outputCIA_S (compInfo out outputPort_S (comp compName l_comp_saf));;

let compsafOut_In name out ia l_comp_saf = 
	let f x tag =  List.Assoc.find_exn x tag ~equal:(=) in
    let l = compsaf_filter name out ia l_comp_saf in
    	List.map l ~f:(fun x -> if (f x inputOrEvent_S) <> "" 
        						then (match (f x inputIAE_S) with
        						         "happens" -> [ f x inputOrEvent_S ; ]
        						       | _         -> [ f x inputOrEvent_S ; f x inputIAE_S ])
                                else []);;

let rec passOnlyAllowed l_formulas allowedList =
   match l_formulas with
       hd::tl -> ( match hd with
                     | F[e;iae] -> if (List.exists allowedList ~f:(fun x -> x=e)) then F[e;iae] :: (passOnlyAllowed tl allowedList)
                                   else passOnlyAllowed tl allowedList
                     | A[e;cia] -> if (List.exists allowedList ~f:(fun x -> x=e)) then A[e;cia] :: (passOnlyAllowed tl allowedList)
                                   else passOnlyAllowed tl allowedList
                     | _ -> raise (Error_csv2soteria "passOnlyAllowed exception") )
     | [] -> [];;

let formulaSafe_And listElement cinputs cevents =
	match listElement with
	 [e] -> F[e]
	|[inList;iaeList] -> 
		let inList_split = String.split_on_chars inList ~on:[';']   (* inport list *)
		and iaeList_split = String.split_on_chars iaeList ~on:[';'] (* I/A/Event list *)
		in
		(* zip the list of inputs and IA/E *)
		let fList = (List.map2_exn inList_split iaeList_split ~f:(fun i iae -> F (List.append [i] [iae]))) 
		in
		(* remove any F[*] that are not either inputs or events in the architecture *)
        let fList_allowed = 
           (let allowedList = List.append cinputs cevents in
            passOnlyAllowed fList allowedList)
        in
		(* remove the text "happens"; turn F[e;"happens"] into F[e] *)
		let fList_rmHappens = List.map fList_allowed ~f:(fun x -> match x with F[e;"happens"] -> F[e] | _ -> x) 
		in
		And fList_rmHappens
	 |_ -> And[];;
	 
let formulaSafe_aux name out cinputs cevents ia l_comp_saf = 
	let l = noEmptyList (compsafOut_In name out ia l_comp_saf)in
	let l2 = List.map l ~f:(fun x -> List.filter x ~f:(fun e -> e<>"")) in
	let l3 = List.map l2 ~f:(fun x -> formulaSafe_And x cinputs cevents) in
	Or (eliminateEmptyAnd l3);;

let formulaSafe name coutputs cinputs cevents l_comp_saf iaList =
    let formula_type name out ia = ([out; ia], formulaSafe_aux name out cinputs cevents ia l_comp_saf) in
    let formulaOut name out = List.map iaList ~f:(fun x-> formula_type name out x) in
    (List.concat (List.map coutputs ~f:(fun x->formulaOut name x)));; 

(* - * - * - *)

(* from l_arch we get lib component names, lib component inputs & outputs, mdl instances, mdl connections *)
let (connName_Arc,     srcIns_Arc,        srcType_Arc, srcImpl_Arc, srcPortName_Arc, srcPortType_Arc, desIns_Arc,         desType_Arc, desImpl_Arc, desPortName_Arc, desPortType_Arc )=
    ("ConnectionName", "SrcCompInstance", "SrcComp",   "SrcImpl",   "SrcPortName",   "SrcPortType",   "DestCompInstance", "DestComp",  "DestImpl",  "DestPortName",  "DestPortType");;

let compInputArch name l_arch = (* by design, lib component inputs are all DestComp DestPortNames *)
   let f x tag = List.Assoc.find_exn x tag ~equal:(=) in
   let cinfo = compInfo name desType_Arc l_arch in
   List.dedup_and_sort ~compare:compare (List.map cinfo ~f:(fun x -> f x desPortName_Arc)) ;;

let compOutputArch name l_arch = (* by design, lib component outputs are all SrcComp SrcPortNames *)
   let f x tag = List.Assoc.find_exn x tag ~equal:(=) in
   let cinfo = compInfo name srcType_Arc l_arch in
   List.dedup_and_sort ~compare:compare (List.map cinfo ~f:(fun x -> f x srcPortName_Arc)) ;;

let instancesArch l_arch = 
   let f x tag = List.Assoc.find_exn x tag ~equal:(=) in
   List.dedup_and_sort ~compare:compare (List.map l_arch ~f:(fun x-> makeInstance ~i:(f x srcIns_Arc) ~c:(f x srcType_Arc ) ()));;

(* instantiate "Connection" as components the ones that appear in Defenses.csv  *)
let instancesConn l_defense = 
   let f x tag = List.Assoc.find_exn x tag ~equal:(=) in
   let l_defense_Connection = compInfo "Connection" compType_D l_defense in
   List.dedup_and_sort ~compare:compare (List.map l_defense_Connection ~f:(fun x-> makeInstance ~i:(f x compInst_D) ~c:"Connection" ()));;

let connectionsArch arch = 
   let f x tag = List.Assoc.find_exn x tag ~equal:(=) in
   let compIns x = f x srcIns_Arc in
   let compPort x = f x srcPortName_Arc in
   let destCompIns x = f x desIns_Arc in
   let destCompPort x = f x desPortName_Arc in     
   List.map arch ~f:(fun x->((destCompIns x, destCompPort x),(compIns x,compPort x)));;

let listConnName l_defense = 
   let connCompInst x  = List.Assoc.find_exn x compInst_D ~equal:(=) in
   let l_defense_Connection = compInfo "Connection" compType_D l_defense in
   List.dedup_and_sort ~compare:compare (List.map l_defense_Connection ~f:(fun x->connCompInst x));;

(* insert connection components by replacing the original connections from arch *)
let rec replace_connectionArch_with_connectionConn connections connNameList l_arch l_defense = 
   let f aL tag = List.Assoc.find_exn aL tag ~equal:(=) in
   let compIns aL = f aL srcIns_Arc 
   and compPort aL = f aL srcPortName_Arc 
   and destCompIns aL = f aL desIns_Arc 
   and destCompPort aL = f aL desPortName_Arc in
   match connNameList with
      hd::tl ->
         (* get the association list that has to do with the connName *)
         let connInfo = List.concat (compInfo hd connName_Arc l_arch) in
         (* original connection *)
         let destComp = destCompIns connInfo
         and destPort = destCompPort connInfo
         and srcComp = compIns connInfo
         and srcPort = compPort connInfo in
         let orgConn = ((destComp, destPort),(srcComp, srcPort))
         (* new connection list *)
         and newConnL = [((hd, "in"),(srcComp, srcPort)); ((destComp, destPort),(hd, "out"))] in
         let filteredConnections = List.filter connections ~f:(fun x->x<>orgConn) in
         replace_connectionArch_with_connectionConn (List.append filteredConnections (newConnL)) tl l_arch l_defense
      | [] -> connections ;;

let list_comp arch = 
   let f x tag = List.Assoc.find_exn x tag ~equal:(=) in 
   let compTypeList = List.dedup_and_sort ~compare:compare (List.map arch ~f:(fun x-> f x srcType_Arc)) in
   List.filter compTypeList ~f:(fun x-> x<>"") ;; 
   
(* To deal with hierarchy, we need to post process l_arch. Make an association list using l_arch
   with the following info: ins, type. (Ins is the assoc list key because it is unique.)
   Then use this assoc list to fill in the "holes" in l_arch 
*)  
let rec makeInsTypeAssocList arch aL =
	match arch with
	| hd::tl -> 
	   let ins = List.Assoc.find_exn hd ~equal:(=) srcIns_Arc
	   and typ = List.Assoc.find_exn hd ~equal:(=) srcType_Arc
	   and imp = List.Assoc.find_exn hd ~equal:(=) srcImpl_Arc in
	   (match ins with
	   "" -> makeInsTypeAssocList tl aL
	   |_ -> let new_AL = (match typ with
	                "" -> List.Assoc.add aL ~equal:(=) ins (List.hd_exn (String.split_on_chars imp ~on:['.']))
	                |_ -> List.Assoc.add aL ~equal:(=) ins typ) in
	         makeInsTypeAssocList tl new_AL)
	| [] -> aL ;;

(* This function massages the Arch by filling in the following information:
   - if Ins is blank, then fill in with info from the assoc list
   - if Typ is blank, then fill in with Impl info 
*)
let rec massageArch_fillIns_fillType arch aL insH impH typH=
    match arch with
    | hd::tl -> 
       (let ins = List.Assoc.find_exn hd ~equal:(=) insH
        and imp = List.Assoc.find_exn hd ~equal:(=) impH
        and typ = List.Assoc.find_exn hd ~equal:(=) typH in
       (match (ins, typ) with
            ("",_) -> let x = List.Assoc.find aL ~equal:(=) typ in
                      (match x with
                           Some x_ins -> List.Assoc.add hd ~equal:(=) insH x_ins
                         | None -> hd)
	        |(_,"") -> List.Assoc.add hd ~equal:(=) typH (List.hd_exn (String.split_on_chars imp ~on:['.']))
	        | _ -> hd) :: massageArch_fillIns_fillType tl aL insH impH typH)
	| [] -> [];;
	
(* This function massages the Arch by modifying port names:
   - if a source port is of type "in", then mark the port name with "_dotO" (i.e., dot Out)
   - if a destination port is of type "out", then mark the port name with "_dotI" (i.e., dot In)
   Must also modify CompDepen in the same way because the lib input/output names come from there.
*)
let rec massageArch_modPortNames arch =
    match arch with
    | hd::tl ->
       (let srcPortType = List.Assoc.find_exn hd ~equal:(=) srcPortType_Arc
        and srcPortName = List.Assoc.find_exn hd ~equal:(=) srcPortName_Arc
        and desPortType = List.Assoc.find_exn hd ~equal:(=) desPortType_Arc
        and desPortName = List.Assoc.find_exn hd ~equal:(=) desPortName_Arc
        in
        match (srcPortType,desPortType) with
            ("in","in")   -> List.Assoc.add hd ~equal:(=) srcPortName_Arc (srcPortName ^ "_dotO");
          | ("out","out") -> List.Assoc.add hd ~equal:(=) desPortName_Arc (desPortName ^ "_dotI")
          | _             -> hd) :: massageArch_modPortNames tl 
    | [] -> [] ;;

let massageArch arch =
   let typeInsAL = List.Assoc.inverse (makeInsTypeAssocList arch []) in
   let tempArchS = massageArch_fillIns_fillType arch typeInsAL srcIns_Arc srcImpl_Arc srcType_Arc in (* for src *)
   let tempArchD = massageArch_fillIns_fillType tempArchS typeInsAL desIns_Arc desImpl_Arc desType_Arc in (* for des *)
   massageArch_modPortNames tempArchD ;;

(* This function generates a sublist of CompDepen based on Arch to support hierarchy:
   - if in Arch a src port is of type "in", then make a new line where input port is the org name and output is the org name with "_dotO" (i.e., dot Out)
   - if in Arch a des port is of type "out", then make a new line where input port is the org name with "_dotI" (i.e., dot In) and output port is the org name.
*)
let rec makeFormula_subList arch cdL ciaList compType_header input_header inputCIA_header output_header outputCIA_header =
    match arch with
    | hd::tl ->
       (let srcPortType = List.Assoc.find_exn hd ~equal:(=) srcPortType_Arc
        and srcPortName = List.Assoc.find_exn hd ~equal:(=) srcPortName_Arc
        and srcCompType = List.Assoc.find_exn hd ~equal:(=) srcType_Arc 
        and desPortType = List.Assoc.find_exn hd ~equal:(=) desPortType_Arc
        and desPortName = List.Assoc.find_exn hd ~equal:(=) desPortName_Arc
        and desCompType = List.Assoc.find_exn hd ~equal:(=) desType_Arc 
        in
        match (srcPortType,desPortType) with
            ("in","in")   -> (List.append (makeFormula_subList tl cdL ciaList compType_header input_header inputCIA_header output_header outputCIA_header) 
                                         (List.map ciaList ~f:(fun a -> [(compType_header, srcCompType);
                                                                         (input_header, (String.chop_suffix_exn ~suffix:"_dotO" srcPortName));
                                                                         (inputCIA_header, a);
                                                                         (output_header, srcPortName);
                                                                         (outputCIA_header, a)])))            
          | ("out","out") -> (List.append (makeFormula_subList tl cdL ciaList compType_header input_header inputCIA_header output_header outputCIA_header) 
                                         (List.map ciaList ~f:(fun a -> [(compType_header, desCompType);
                                                                         (input_header, desPortName);
                                                                         (inputCIA_header, a);
                                                                         (output_header, (String.chop_suffix_exn ~suffix:"_dotI" desPortName));
                                                                         (outputCIA_header, a)])))       
          | _             -> makeFormula_subList tl cdL ciaList compType_header input_header inputCIA_header output_header outputCIA_header)
    | [] -> [] ;;

(* massages the CompDepen file, calling makeFormula_subList with the headers from CompDepen *)
let massageCompDepen compDepen arch ciaList =
   let newCDs = makeFormula_subList arch compDepen ciaList compType_C inputPort_C inputCIA_C outputPort_C outputCIA_C in
   List.append compDepen newCDs ;;

(* massages the CompSafe file, calling makeFormula_subList with the headers from CompSafe *)
let massageCompSafe compSafe arch iaList =
   let newCSs = makeFormula_subList arch compSafe iaList compType_S inputOrEvent_S inputIAE_S outputPort_S outputCIA_S in
   List.append compSafe newCSs ;;


(* - * - * - *)

(* from l_mission we get components, instances, connections, and top_attack*)
let (modelVersion_M, missionReqId_M,  reqType_M, reqId_M, req_M, compOutputDependency_M, missionImpactCIA_M, severity_M, compInstanceDependency_M, cia_M)=
    ("ModelVersion", "MissionReqId", "ReqType", "ReqId",  "Req", "CompOutputDependency", "MissionImpactCIA", "Severity", "CompInstanceDependency", "DependentCompOutputCIA");;

let compMission name l_mission = compInfo name reqId_M l_mission;;
let compReqType name l_mission = List.hd_exn( List.dedup_and_sort ~compare:compare (comp_find reqType_M (compMission name l_mission)));;

let compInputMission name l_mission = 
   let rawInputList = comp_find compOutputDependency_M (compMission name l_mission) in
   (* remove duplicates from the concatenated, parsed list of inputs *)
   List.dedup_and_sort ~compare:(compare) 
    (List.concat (List.map rawInputList ~f:(fun iStr -> String.split_on_chars iStr ~on:[';'])));;

let missionImpact name l_mission= comp_find missionImpactCIA_M (compMission name l_mission);;
let compMissionCIA name cia l_mission= compInfo cia missionImpactCIA_M (compMission name l_mission) ;;

let rec makeAttackList l =
	match l with
	  hd::tl -> let (port,cia) = hd in A[port;cia]::makeAttackList tl
	| [] -> [];;

let rec makeFaultList l =
	match l with
	  hd::tl -> let (port,cia) = hd in F[port;cia]::makeFaultList tl
	| [] -> [];;

let conjuncAtckFltList l rType = 
	match rType with
	  "Cyber" -> And (makeAttackList l)
	| "Safety" -> And (makeFaultList l) 
	| _ -> raise (Error_csv2soteria "conjuncAtckFltList exception");;

let listConjuncAtckFltList str1 str2 rType =
	let split_str1 = String.split_on_chars str1 ~on:[';'] 
	and split_str2 = String.split_on_chars str2 ~on:[';'] in
	conjuncAtckFltList (List.zip_exn split_str1 split_str2) rType;;

let compOut_InMission name cia l_mission =
	let missionImpact_list = compMissionCIA name cia l_mission in
	let outputs_list = List.map missionImpact_list ~f:(fun x -> List.Assoc.find_exn x compOutputDependency_M ~equal:(=)) in
	(* Raise an error if outputs_list is empty, because then there's nothing to analyze. *)
    match outputs_list with
        [""] -> raise (Error_csv2soteria "compOut_InMission exception: no output to analyze")
       | _ -> (let outputscia_list = List.map missionImpact_list ~f:(fun x -> List.Assoc.find_exn x cia_M ~equal:(=)) in
	           let rType = compReqType name l_mission in
	           List.map2_exn outputs_list outputscia_list ~f:(fun outputStr ciaStr -> listConjuncAtckFltList outputStr ciaStr rType) );;

let compAtckFltMission name cia l_mission =
	let missionImpact_list = compMissionCIA name cia l_mission in
	let outputscia_list = List.map missionImpact_list ~f:(fun x -> List.Assoc.find_exn x cia_M ~equal:(=)) in
	let outputscia_split_flattened_list = List.concat (List.map outputscia_list ~f:(fun x -> String.split_on_chars x ~on:[';'])) in
	List.dedup_and_sort ~compare:(compare) outputscia_split_flattened_list;;

let compInstOut name l_mission =
	let f x tag = List.Assoc.find_exn x tag ~equal:(=) in
	let compInsList x = String.split_on_chars (f x compInstanceDependency_M) ~on:[';'] in
	let compOutList x = String.split_on_chars (f x compOutputDependency_M) ~on:[';'] in
	List.dedup_and_sort ~compare:(compare) (  (* flatten list and remove duplicates *)
		List.concat(
			List.map (compMission name l_mission) 
			~f:(fun x -> List.map2_exn (compInsList x) (compOutList x) ~f:(fun inst o -> ((name, o),(inst,o)))))) ;;

let missionIds l_mission  =  comp_find reqId_M l_mission;;
let missionReqId l_mission = comp_find missionReqId_M l_mission;;

(* - * - * - *)
(* - * - * - *)

(*Independent of Header*)
(*generate component for component name *)
let genComp name inp out fault b_events e_info f_formula attacks a_event a_info a_formulas d_events d_rigors d_profiles =
     {name            = name;
     input_flows      = inp;
     output_flows     = out;
     faults           = fault;
     basic_events     = b_events;
     event_info       = e_info;
     fault_formulas   = f_formula;
     attacks          = attacks;
     attack_events    = a_event;
     attack_info      = a_info;
     attack_formulas  = a_formulas;
     defense_events   = d_events;
     defense_rigors   = d_rigors;
     defense_profiles = d_profiles};;
	 
let formula_And listElement cinputs =
	match listElement with
	 [a] -> A[a]
	|[iList;ciaList] -> 
		let iList_split = String.split_on_chars iList ~on:[';'] 
		and ciaList_split = String.split_on_chars ciaList ~on:[';'] 
		in
		(* zip the list of inputs and CIA *)
		let aList = (List.map2_exn iList_split ciaList_split ~f:(fun i cia -> A (List.append [i] [cia]))) 
		in
		(* remove any A[*] that are not either inputs or events in the architecture *)
        let aList_allowed = passOnlyAllowed aList cinputs
        in
		And aList_allowed 
	 | _ -> And[] ;;

let fullAttackList name cia l_attack =
   List.dedup_and_sort ~compare:compare (noEmptyList (attack_cia name cia l_attack));;

let elimAttackList name cia l_attack = 
   List.concat (noEmptyList (List.dedup_and_sort ~compare:compare (attack_cia name cia l_attack)));;
   
let filterList fullListList elimList =
   List.filter fullListList ~f:(fun x -> not(List.mem elimList (List.hd_exn x) ~equal:(=)));;

(* given a list of Connections with defenses, list (destination comp, destination port name) *)
let makeConnDestCompList connNameList l_arch =
	List.map connNameList ~f:(fun x -> let connInfo = List.concat (compInfo x connName_Arc l_arch) in
	   (List.Assoc.find_exn connInfo desType_Arc ~equal:(=), List.Assoc.find_exn connInfo desPortName_Arc ~equal:(=)));;

let formula_aux name out cinputs cia l_comp_dep l_attack l_defense l_arch =  
   let l = fullAttackList name cia l_attack in                             (* <-- full attack list *)
   let lprime = filterList l (elimAttackList "Connection" cia l_attack) in (* <-- attack list with CAPECs on connections eliminated *)
   let l_compInCIA = compOut_In name out cia l_comp_dep in                 (* <-- [inp; cia] list *)
   let l_match = List.map l_compInCIA ~f:(fun i -> match i with            (* <-- [true; false] list that shows if an input is part of the elimination list b/c it's already in the connection *)
        [inp;_] -> List.exists (makeConnDestCompList (listConnName l_defense) l_arch) ~f:(fun m -> m=(name,inp)) 
      | _     -> false) in 
   let use_lprime = match l_match with                                     (* <-- fold the list into either T/F bool *)
        [] -> false
      | _  -> List.fold l_match ~init:true ~f:(&&) in
   let l2 = match use_lprime with
        true  -> noEmptyList(List.concat [lprime; l_compInCIA])            (* <-- if true, use the attack list with connection attack eliminated *)
      | false -> noEmptyList (List.concat [l; l_compInCIA]) in             (* <-- if false, use the full attack list *)
   let l3 = List.map l2 ~f:(fun x -> formula_And x cinputs) in
   Or (eliminateEmptyAnd l3);;

let formula name coutputs cinputs l_comp_dep l_attack l_defense l_arch ciaList = 
    let formula_type name out cia =  ([out; cia],formula_aux name out cinputs cia l_comp_dep l_attack l_defense l_arch) in
    let formulaOut name out = List.map ciaList ~f:(fun x-> formula_type name out x) in
    (List.concat (List.map coutputs ~f:(fun x->formulaOut name x)));;


(* # listConnName defense;;
- : Core.String.t Core.List.t = ["c14a"; "c16"; "i2"; "i3"] *)
(* # makeConnDestCompList (listConnName defense) arch;;
- : (Core.String.t * Core.String.t) Core.List.t =
[("Connector", "bus_in"); ("Radio", "comm_in"); ("GPS", "satellite0_pos"); ("GPS", "satellite1_pos")] *)
(* # compOut_In "Radio" "comm_out" "Integrity" compDepen;;
- : Core.String.t list Core.List.t = [["comm_in"; "Integrity"]] *)

let formulaConn_aux name cinputs cia l_attack =
	let l = List.dedup_and_sort ~compare:compare (noEmptyList (attack_cia name cia l_attack)) in 
	let l2 = noEmptyList (List.concat [l; [["in"; cia]]]) in    (* <-- using hardcoded "in" for Connection input *)
	let l3 = List.map l2 ~f:(fun x -> formula_And x cinputs) in
	Or (eliminateEmptyAnd l3);;

let formulaConn name coutputs cinputs l_attack ciaList  =
    let formula_type name out cia = ([out; cia], formulaConn_aux name cinputs cia l_attack) in
    let formulaOut name out = List.map ciaList ~f:(fun x -> formula_type name out x) in
    (List.concat (List.map coutputs ~f:(fun x->formulaOut name x)));;

(*Inferring cyber relation *)

let elemExtractVal l ltag = 
     let extracTags l ltag = List.map ltag ~f:(fun x->List.Assoc.find_exn l x ~equal:(=))  in
     List.dedup_and_sort ~compare:compare (List.map l ~f:(fun x-> extracTags x ltag));; 
     
let ele_l_nol1 l l1 =
    let clean l = List.concat l in  
    clean(List.map l ~f:(fun x->if (List.mem l1 x ~equal:(=) = true) then [] else [x]));; 

(*components without cyber relations, components with implementation are not considered*)
let compArchImpleAux l =
   let srcPortType = List.Assoc.find_exn l ~equal:(=) srcPortType_Arc
        and desPortType = List.Assoc.find_exn l ~equal:(=) desPortType_Arc
        and srcPortName = List.Assoc.find_exn l ~equal:(=) srcType_Arc
        and srcImpl     = List.Assoc.find_exn l ~equal:(=) srcImpl_Arc
        and desImpl     = List.Assoc.find_exn l ~equal:(=) desImpl_Arc
        and desType     = List.Assoc.find_exn l ~equal:(=) desType_Arc
   in 
     match (srcImpl <> "",srcPortType,desPortType,desImpl <>"") with
       (true,"in","in",_)   -> srcPortName
       | (_,"out","out",true) -> desType
       | _             -> ""  ;;
   
let compArchImple l = 
    let clean l = List.dedup_and_sort ~compare:compare (List.filter l ~f:(fun x->x<>"")) in 
    clean (List.map l ~f:(fun x->compArchImpleAux x));;

let compCyberInfe l_arch l_compDepen =
    let noInferredComp = List.append (compArchImple l_arch) (List.concat (elemExtractVal l_compDepen  [compType_C])) in
    ele_l_nol1 (List.concat(elemExtractVal (l_arch)  [srcType_Arc])) noInferredComp;;


let formulaInfer_aux name cia out linputs l_attack= 
    let l = List.concat (noEmptyList (attack_cia name cia l_attack)) in
    let lattack = List.map l ~f:(fun x->A[x]) in
    let confidIntegAvail = List.map linputs ~f:(fun x->A[x;cia]) in
    (*Heber let availability = List.concat (List.map linputs (fun x->[A[x;cia];A[x;"Integrity"]])) in  
    ([out; cia], Or (List.concat [lattack; if (cia = "Availability") then availability else confidInteg])) *)
    ([out; cia], Or (List.concat [lattack; confidIntegAvail]));;
    
let formulaInfer name coutputs cinputs l_attack ciaList = 
    let formulaOut name out = List.map ciaList ~f:(fun x-> formulaInfer_aux name x out cinputs l_attack) in
    (List.concat (List.map coutputs ~f:(fun x->formulaOut name x)));;

(*inferring safety relations*) 
   
let formulaInferSafe  coutputs cinputs =
    (*Heber let availa= List.concat (List.map cinputs (fun x-> [F[x;"Availability"];F[x;"Integrity"]])) in *)
    let availa= List.map cinputs ~f:(fun x-> F[x;"Availability"]) in
    let integ = List.map cinputs ~f:(fun x-> F[x;"Integrity"]) in 
    List.concat(List.map coutputs ~f:(fun x-> [([x;"Availability"] , Or availa );([x;"Integrity"], Or integ)]));;

 let eventsNoOutput lcompSafe = 
     let extract l tag = (List.Assoc.find_exn l tag ~equal:(=)) in
     let cond x = ((extract x outputPort_S) = "null" && (extract x inputIAE_S)= "happens") in
     let result x = [extract x compType_S;extract x inputOrEvent_S ;extract x outputCIA_S] in
     let clean l = List.filter l ~f:(fun x->x<>[])in 
     clean( List.map lcompSafe ~f:(fun x->if cond x then (result x) else [])) ;;

let eventsInfer name ia lcompSafe =  
    let comp l = List.nth_exn l 0 in
    let inAvail l = List.nth_exn l 2 in
    let clean l = List.filter l ~f:(fun x->x<>F[]) in 
    clean (List.map (eventsNoOutput lcompSafe) ~f:(fun x->if (comp x = name) && (inAvail x = ia) 
                                             then F [List.nth_exn x 1] else (F []))) ;;
 
let formulaInferSafe_aux1 name out cinputs cevents ia l_comp_saf = 
	let l = noEmptyList (compsafOut_In name out ia l_comp_saf)in
	let l2 = List.map l ~f:(fun x -> List.filter x ~f:(fun e -> e<>"")) in
	let l3 = List.map l2 ~f:(fun x -> formulaSafe_And x cinputs cevents) in
	Or (List.dedup_and_sort ~compare:compare (List.append (eventsInfer name ia l_comp_saf) (eliminateEmptyAnd l3)));;

let formulaInferSafe1 name coutputs cinputs cevents l_comp_saf iaList =
    let formula_type name out ia = ([out; ia], formulaInferSafe_aux1 name out cinputs cevents ia l_comp_saf) in
    let formulaOut name out = List.map iaList ~f:(fun x-> formula_type name out x) in
    (List.concat (List.map coutputs ~f:(fun x->formulaOut name x)));; 

(**)

let gen_Comp name defType l_arch l_comp_dep l_comp_saf l_attack l_defense 
             l_events ciaList iaList infeCyber infeSafe comNoCyberRe comNoSafeRe = 
	(* Below calls the function genComp which creates a lib comp with the following fields filled in *)
	let coutputs = (compOutputArch name l_arch) 
	and cinputs = (compInputArch name l_arch)
	and cevents = (compEvents name l_events)
	and (attacksList, infoList) = (makeAttackList_AttackInfoList name l_attack)
	in 
	genComp (*name*)            name 
			(*input_flows*)     cinputs 
			(*output_flows*)    coutputs
			(*faults*)          iaList 
			(*basic_events*)    cevents 
			(*event_info*)      (compEventsInfo name l_events cevents) 
			(*fault_formulas*)  (if infeSafe  (*&& List.mem comNoSafeRe name ~equal:(=)*)
			                     then ( if List.mem comNoSafeRe name ~equal:(=)
			                            then (formulaInferSafe  coutputs cinputs)
			                            else formulaInferSafe1 name coutputs cinputs cevents l_comp_saf iaList)
			                     else (formulaSafe name coutputs cinputs cevents l_comp_saf iaList))
			(*attacks*)         ciaList 
			(*attack_events*)   attacksList (* (attack_events name l_attack) *)
			(*attack_info*)     infoList    (* (attack_info name l_attack) *)
			(*attack_formula*)  (if (infeCyber && List.mem comNoCyberRe name ~equal:(=))
			                        then (formulaInfer name coutputs cinputs l_attack ciaList)
			                        else (formula name coutputs cinputs l_comp_dep l_attack l_defense l_arch ciaList)) 
			(*defense_events*)  (defenseEvents name defType l_defense)   (* eventsList *)
			(*defense_rigors*)  (defenseRigors name defType l_defense)   (* rigorsList *)  
			(*defense_profiles*)(defenseProfile name defType l_defense);;
(* - *)

let gen_ConnComp name defType l_attack l_defense ciaList = 
	(* Below calls the function genComp which creates a lib comp with the following fields filled in *)
	let cinputs = ["in"]
	and coutputs = ["out"] 
	and (attacksList, infoList) = (makeAttackList_AttackInfoList name l_attack)
	in 
	genComp (*name*)            name 
			(*input_flows*)     cinputs  (* <-- always 1 input called "in" *)
			(*output_flows*)    coutputs (* <-- always 1 output called "out" *)
			(*faults*)          [ ]      (* <-- nothing to do with safety; leave empty *)
			(*basic_events*)    [ ]      (* <-- nothing to do with safety; leave empty *)
			(*event_info*)      [ ]      (* <-- nothing to do with safety; leave empty *)
			(*fault_formulas*)  [ ]      (* <-- nothing to do with safety; leave empty *)
			(*attacks*)         ciaList 
			(*attack_events*)   attacksList 
			(*attack_info*)     infoList    
			(*attack_formula*)  (formulaConn name coutputs cinputs l_attack ciaList) 
			(*defense_events*)  (defenseEvents name defType l_defense)   (* eventsList *)
			(*defense_rigors*)  (defenseRigors name defType l_defense)   (* rigorsList *)  
			(*defense_profiles*)(defenseProfile name defType l_defense);;
(* - *)

(*generate component mission*)
let changeName  = (fun x -> match x with 
                               "Availability"->"loa"
                              |"AVAILABILITY"->"loa"
                              |"Integrity"   ->"loi"
                              |"INTEGRITY"   ->"loi"
                              |x->"lossOf");;
let formulaMission name l_mission =  
	let formulaCIA name cia l_mission=(["out";changeName cia],Or(compOut_InMission name cia l_mission))in
    List.map (missionImpact name l_mission) ~f:(fun x-> formulaCIA name x l_mission);;

let gen_CompMission name l_mission = 
	(* Below calls the function genComp which creates a top-level (dummy) lib comp for attacks
	   The only fields of interests are output_flows (with a default name), attacks, 
	   and attack_formulas. *)
	genComp (*name*)            name 
			(*input_flows*)     (compInputMission name l_mission)
			(*output_flows*)    ["out"]
			(*faults*)          []
			(*basic_events*)    []
			(*event_info*)      []
			(*fault_formulas*)  [] 
			(*attacks*)         (List.concat (List.map (missionImpact name l_mission) ~f:(fun x-> compAtckFltMission name x l_mission))) 
			(*attack_events*)   []
			(*attack_info*)     []
			(*attack_formula*)  (formulaMission name l_mission)
			(*defense_events*)  []
			(*defense_rigors*)  []
			(*defense_profiles*)[];;

let gen_CompSafety name l_mission = 
	(* Below calls the function genComp which creates a top-level (dummy) lib comp for safety
	   The only fields of interests are output_flows (with a default name), faults, 
	   and fault_formulas. *)
	genComp (*name*)            name 
			(*input_flows*)     (compInputMission name l_mission)
			(*output_flows*)    ["out"]
			(*faults*)          (List.concat (List.map (missionImpact name l_mission) ~f:(fun x-> compAtckFltMission name x l_mission))) 
			(*basic_events*)    []
			(*event_info*)      []
			(*fault_formulas*)  (formulaMission name l_mission) 
			(*attacks*)         []
			(*attack_events*)   []
			(*attack_info*)     []
			(*attack_formula*)  []
			(*defense_events*)  []
			(*defense_rigors*)  []
			(*defense_profiles*)[];;
                            
let topAttack reqId l_mission= (reqId, A["out";changeName (List.hd_exn(missionImpact reqId l_mission))]);;                             
let topFault reqId l_mission= (reqId, F["out";changeName (List.hd_exn(missionImpact reqId l_mission))]);;                             

(**)     
let genModel ins conn fault attack = 
    { instances = ins;
      connections = conn;
      top_fault = fault;
      top_attack = attack 
      };;
      
let instances reqId l_arch l_defense = 
    let i1 = makeInstance ~i:reqId ~c:reqId ()
    and i2 = instancesArch l_arch
    and i3 = instancesConn l_defense 
    in 
    List.append (i1::i2) i3 ;;

let connections reqId l_arch l_defense l_mission = 
	List.append (replace_connectionArch_with_connectionConn (connectionsArch l_arch) (listConnName l_defense) l_arch l_defense) 
	            (compInstOut reqId l_mission);;

let gen_model reqId l_arch l_defense l_mission = 
	let rType = compReqType reqId l_mission in
	let (fault,attack) = 
	    match rType with
    	| "Cyber" -> (("", F["";""]),(topAttack reqId l_mission))
    	| "Safety" -> ((topFault reqId l_mission), ("", A["";""]))
    	| _ -> raise (Error_csv2soteria "gen_model exception: req is neither CyberReq nor SafetyReq") 
    in
	genModel (instances reqId l_arch l_defense) (connections reqId l_arch l_defense l_mission) fault attack;;
 
(**)
let gen_library reqId defType l_comp_dep l_comp_saf l_attack l_events l_arch l_defense l_mission ciaList iaList infeCyber infeSafe= 
    let comNoCyberRe = compCyberInfe l_arch l_comp_dep in
	let comNoSafeRe  = compCyberInfe l_arch l_comp_saf in
    let components = List.map (list_comp l_arch) ~f:(fun x -> gen_Comp x defType l_arch l_comp_dep l_comp_saf l_attack 
                                   l_defense l_events ciaList iaList infeCyber infeSafe comNoCyberRe comNoSafeRe)
    and connComponent = gen_ConnComp "Connection" defType l_attack l_defense ciaList in
    let rType = compReqType reqId l_mission in
    match rType with
    | "Cyber" -> List.append (connComponent::components) [gen_CompMission reqId l_mission]
    | "Safety" -> List.append components [gen_CompSafety reqId l_mission]
    | _ -> raise (Error_csv2soteria "gen_library exception: req is neither CyberReq nor SafetyReq");;


(* iterate through the following: ApplicableDefenseProperties and ImplProperties*)

let libraries_threatConditions deftype compDepen compSafe attack events arch mission defense defense2nist infeCyber infeSafe =
   let ciaList = ["Confidentiality";"Integrity";"Availability"] (* <-- these can be hardcoded because these are the only options allowed in the VERDICT annex; *)
   and iaList = ["Integrity";"Availability"]  (* <-- these can be hardcoded because these are the only options allowed in the VERDICT annex; *)
   and arch_prime = massageArch arch 
   in
   let compDepen_prime = massageCompDepen compDepen arch_prime ciaList
   and compSafe_prime = massageCompSafe compSafe arch_prime iaList
   in 
   List.map (missionReqId mission) ~f:(fun y ->
       (* find what reqIDs are under this missionReqID and iterate through those *)
       let reqL = List.dedup_and_sort ~compare:(compare) (List.map (compInfo y missionReqId_M mission) ~f:(fun x -> List.Assoc.find_exn x ~equal:(=) reqId_M)) in
	   (y, List.map reqL ~f:(fun x->
	      (x,deftype),((gen_library x deftype compDepen_prime compSafe_prime attack events arch_prime defense mission ciaList iaList infeCyber infeSafe), 
                        gen_model x arch_prime defense mission)
          )
       )
   ) ;;

    
(* translate mission severity to level of risk 
   if a probability is given, then just print that number *)
let severity2risk severity =
  match severity with
  | "Catastrophic" -> "1e-09"
  | "Hazardous"    -> "1e-07"
  | "Major"        -> "1e-05"
  | "Minor"        -> "1e-03"
  | "None"         -> "1"
  | prob           -> prob    ;;

(* function that saves the library and model (to .ml file) for debugging *)
let saveLibraryAndModelToFile filename lib mdl =
      let oc = Out_channel.create filename in
      print_filename oc filename;
      print_library oc lib;
      print_model oc mdl;
      Out_channel.close oc ;;

(**)

(* function that generate xml file *)
let all_not l = 
  let allNot l =
    match l with 
    ANot (AVar _) -> true
    |_-> false in
  List.for_all l ~f:(fun x->allNot x);;
 
let get_defense l = 
   match l with 
   ANot(AVar (_,b))->b
   |_->"";;
     
let takeAvar aVar = 
   match aVar with
       AVar (a,b) -> [("comp",a);("attack",b)]
     | _          -> [("comp","");("attack","")];;

let takeDefenseAnd l = 
  let concat_And_Or op l =List.fold (List.tl_exn l) ~init:(List.hd_exn l) ~f:(fun x y -> x^" "^ op^" " ^y) in
  let clean x =  List.dedup_and_sort ~compare:compare x in 
   match l with 
   |ANot (AVar (_,b))->[b]
   |DSum l -> clean( List.map l ~f:(fun x->"("^(concat_And_Or "and" (List.map l ~f:(fun x->get_defense x)))^")" ))
   |_->["No support"] ;;  

let takeDefenseAndOr l =
  let concat_And_Or op l =List.fold (List.tl_exn l) ~init:(List.hd_exn l) ~f:(fun x y -> x^" "^ op^" " ^y) in
  let clean x =  List.dedup_and_sort ~compare:compare x in 
   match l with 
   |ANot (AVar (_,b))->[[b]]
   |DPro l ->  List.map l ~f:(fun x-> takeDefenseAnd x)
   |DSum l -> clean( List.map l ~f:(fun x->["("^(concat_And_Or "and" (List.map l ~f:(fun x->get_defense x)))^")" ]))
   |_->[["No support"]] ;;  

let takeDefense l = 
   let clean x =  List.dedup_and_sort ~compare:compare x in  
   let concat_And_Or op l =List.fold (List.tl_exn l) ~init:(List.hd_exn l) ~f:(fun x y -> x^" "^ op^" " ^y) in
   clean ([("defense",concat_And_Or "or" (List.concat (takeDefenseAndOr l)))]);;

let takeDefense2 l = 
   let concat_And_Or op l =List.fold (List.tl_exn l) ~init:(List.hd_exn l) ~f:(fun x y -> x^" "^ op^" " ^y) in
   concat_And_Or "or" (List.concat (takeDefenseAndOr l));;

let cutSets cutSet =     
  let cutSetToView cOpe = 
      match cOpe with 
      | APro (h::tl) -> List.append (takeAvar h) (takeDefense (List.hd_exn tl))
      | AVar avar -> List.append (takeAvar (AVar avar)) ["defense", ""]
      | _ -> List.append (takeAvar AFALSE) ["defense",""] in
    let (cut,pro1,_) = cutSet in
    List.append [("prob",(string_of_float pro1))] (cutSetToView cut);;

let concat_And_Or op l =List.fold (List.tl_exn l) ~init:(List.hd_exn l) ~f:(fun x y -> x^" "^ op^" " ^y) ;;

let rec attacksToView cOpe = 
      match cOpe with 
      | APro (h::tl) -> List.append (attacksToView h) (attacksToView (APro tl))
      | AVar (c,a) -> [[("comp",c);("capec",a)]]
      | _ -> [] ;;

let rec defenseCompToView cOpe = 
      match cOpe with 
      | ANot( AVar (c,_) ) -> c
      | DSum l -> defenseCompToView (List.hd_exn l)
      | DPro l -> defenseCompToView (List.hd_exn l)
      | _ -> "" ;;

let convertProp2Profile_single dp l_defense2nist =
    let aL = List.concat (propInfo dp dprop_D l_defense2nist) in
       let nistprofile = List.Assoc.find aL ~equal:(=) dprofile_D in
          match nistprofile with
            | Some x -> x (* if found, return the nist profile *)
            | None -> ""  (* otherwise, return the an empty string *) ;;

let propCut2nistCut cOpe defense2nist = 
      match cOpe with 
      | ANot( AVar (c,d) ) -> 
         let dNISTlist = String.split_on_chars (convertProp2Profile_single d defense2nist) ~on:[';']
         in List.map dNISTlist ~f:(fun x -> ANot( AVar (c, x) ))       
      | _ -> [] ;;

let rec defenseToView cOpe = 
      match cOpe with 
      | APro (h::tl) -> List.append (defenseToView h) (defenseToView (APro tl))
      | ANot( AVar (c,d) ) -> 
            [[("comp",c);
              ("suggested",d);]]
      | DSum l -> 
            [[("comp",(defenseCompToView (DSum l))); 
              ("suggested",takeDefense2 (DSum l)); ]]
      | DPro l -> 
            [[("comp",(defenseCompToView (DPro l))); 
              ("suggested",takeDefense2 (DPro l)); ]]
      | _ -> [] ;;   

let rec defenseProfileTranslator cOpe l_defense2nist =
      match cOpe with
      | ANot( AVar (c,d) ) -> DSum (propCut2nistCut (ANot( AVar (c,d) )) l_defense2nist ) (* <-- same as DSum because it's the DeMorgan of the a product of NISTs *)
      | DSum l -> DSum (List.map l ~f:(fun x -> defenseProfileTranslator x l_defense2nist) ) 
      | DPro l -> DPro (List.map l ~f:(fun x -> defenseProfileTranslator x l_defense2nist) ) 
      | _ -> AFALSE ;;

let rec defenseToView2 cOpe l_defense2nist = 
      match cOpe with 
      | APro (h::tl) -> List.append (defenseToView2 h l_defense2nist) (defenseToView2 (APro tl) l_defense2nist)
      | ANot( AVar (c,d) ) -> 
            [[("comp",c);
              ("suggested",d);
              ("profile", takeDefense2 (ssfc_ad (defenseProfileTranslator (ANot(AVar(c,d))) l_defense2nist))) ]]
      | DSum l -> 
            [[("comp",(defenseCompToView (DSum l))); 
              ("suggested", takeDefense2 (DSum l));
              ("profile", takeDefense2 (ssfc_ad (defenseProfileTranslator (DSum l) l_defense2nist))) ]]
      | DPro l -> 
            [[("comp",(defenseCompToView (DPro l))); 
              ("suggested", takeDefense2 (DPro l));
              ("profile", takeDefense2 (ssfc_ad (defenseProfileTranslator (DPro l) l_defense2nist))) ]] 
      | _ -> [] ;;
      
let make_cutSetTuples cs l_defense2nist =     
   let (cut,pro1,_) = cs in
   ( ("prob",(string_of_float pro1) ), 
      ("attacks", attacksToView cut),
      ("defense", defenseToView2 cut l_defense2nist )
   );;

let cutSetsList csList l_defense2nist = List.map csList ~f:(fun x-> make_cutSetTuples x l_defense2nist);;

let rec cutSetsSafetyToView cOpe = 
      match cOpe with 
      | Var (a,b) -> [[("comp",a);("event",b)]]
      | Pro (h::tl) -> List.append (cutSetsSafetyToView h) (cutSetsSafetyToView (Pro tl))
      | _ -> [] ;;

let cutSetsSafety cutSet =     
    let (cut,pro1,_) = cutSet in
    (("prob",(string_of_float pro1)), (cutSetsSafetyToView cut));;

let cutSetsSafetyList cutSetList = List.map cutSetList ~f:(fun x-> cutSetsSafety x);;

let xmlBuilder_cutSet_attack comp attack = 
   Xml.Element ("Component",[("comp",comp);("attack",attack)],[]) ;;

let xmlBuilder_cutSet_defense comp suggested profile = 
   Xml.Element ("Component",[("comp",comp);("suggested",suggested);("profile",profile)],[]) ;;

let xmlBuilder_cutSet cutSetlikelihood comp_aList comp_dList = 
   let getval l tag =  List.Assoc.find_exn l tag ~equal:(=) 
   in
   let myds_AttackComp = List.map comp_aList ~f:(fun x -> xmlBuilder_cutSet_attack (getval x "comp") (getval x "capec")) 
   and myds_DefenseComp = List.map comp_dList ~f:(fun x -> xmlBuilder_cutSet_defense (getval x "comp") (getval x "suggested") (getval x "profile")) 
   in
   let myds_Attack = Xml.Element ("Attack",[], myds_AttackComp)
   and myds_Defense = Xml.Element ("Defense",[], myds_DefenseComp)
   in
   Xml.Element ("Cutset",[("likelihood",cutSetlikelihood)], [myds_Attack;myds_Defense]) ;;

let xmlBuilder_Requirement reqIDStr defenseTypeStr tc_adtree l_mission l_defense2nist = 
   (* list of cutsets to print *)
   let infoList = cutSetsList (likelihoodCutImp tc_adtree) l_defense2nist
   and likely = likelihoodCut tc_adtree in
   (* internal functions *)
   let getval l tag =  List.Assoc.find_exn l tag ~equal:(=) in
   let cybReq_Severity l_mission =  List.map l_mission ~f:(fun x->(getval x reqId_M, getval x severity_M)) in
   let getSeverity_Of_cybReq req l_mission = getval (cybReq_Severity l_mission) req 
   in
   let ds_Cutset = List.map infoList ~f:(fun ((_,p),(_,aL),(_,dL)) -> xmlBuilder_cutSet p aL dL)
   in
   Xml.Element ("Requirement",[("label",reqIDStr);
                               ("defenseType",defenseTypeStr);
                               ("computed_p",(string_of_float likely));
                               ("acceptable_p", (severity2risk(getSeverity_Of_cybReq reqIDStr l_mission)))], ds_Cutset) ;;

let xmlBuilder_Event event = 
   Xml.Element ("Event",[("name",event)],[]) ;;

let xmlBuilder_Component_safety comp event = 
   Xml.Element ("Component",[("name",comp)], [xmlBuilder_Event event]) ;;

let xmlBuilder_cutSet_safety cutSetprobability comp_event_List = 
   let getval l tag =  List.Assoc.find_exn l tag ~equal:(=) 
   in
   let ds_Comp_safety = List.map comp_event_List ~f:(fun x -> xmlBuilder_Component_safety  (getval x "comp") (getval x "event"));
   in
   Xml.Element ("Cutset",[("probability",cutSetprobability)], ds_Comp_safety) ;;

let xmlBuilder_Requirement_safety reqIDStr defenseTypeStr tc_ftree l_mission = 
   (* list of cutsets to print *)
   let infoList = cutSetsSafetyList (probErrorCutImp tc_ftree) 
   and (pr,_) = probErrorCut tc_ftree in
   (* internal functions *)
   let getval l tag =  List.Assoc.find_exn l tag ~equal:(=) in
   let cybReq_Safety l_mission =  List.map l_mission ~f:(fun x->(getval x reqId_M, getval x severity_M)) in
   let getSafety_Of_cybReq req l_mission = getval (cybReq_Safety l_mission) req 
   in
   let ds_Cutset = List.map infoList ~f:(fun ((_,p),l) -> xmlBuilder_cutSet_safety p l)
   in
   Xml.Element ("Requirement",[("label",reqIDStr);
                               ("defenseType",defenseTypeStr);
                               ("computed_p",(string_of_float pr));
                               ("acceptable_p", (getSafety_Of_cybReq reqIDStr l_mission))], ds_Cutset) ;;

let xmlBuilder_RequirementList cyberORsafety l_libmdl mission l_defense2nist =
   match cyberORsafety with
   | "cyber" -> List.map l_libmdl ~f:(fun x ->
      (let ((reqIDStr, defenseTypeStr), (lib, mdl)) = x in
         let t = model_to_adtree lib mdl in  
         (* -- print requirement info into .xml file -- *)
         xmlBuilder_Requirement reqIDStr defenseTypeStr t mission l_defense2nist)) 
   | "safety" -> List.map l_libmdl ~f:(fun x ->
      (let ((reqIDStr, defenseTypeStr), (lib, mdl)) = x in
         let t = model_to_ftree lib mdl in  
         (* -- print requirement info into .xml file -- *)
         xmlBuilder_Requirement_safety reqIDStr defenseTypeStr t mission )) 
   | _ -> raise (Error_csv2soteria "xmlBuilder_RequirementList exception: req is neither CyberReq nor SafetyReq") ;;
        
let xmlBuilder_MissionList cyberORsafety l_librariesThreats mission l_defense2nist =
   List.map l_librariesThreats ~f:(fun mID -> 
      (let (missionReqIdStr, l_libmdl) = mID in
         Xml.Element ("Mission", [("label",missionReqIdStr)], xmlBuilder_RequirementList cyberORsafety l_libmdl mission l_defense2nist) ) ) ;;

let xml_gen filename_ch reqIDStr defenseTypeStr tc_adtree l_mission l_defense2nist = 
   (* list of cutsets to print *)
   let infoList = cutSetsList (likelihoodCutImp tc_adtree) l_defense2nist
   and likely = likelihoodCut tc_adtree in
   (* internal functions *)
   let getval l tag =  List.Assoc.find_exn l tag ~equal:(=) in
   let cybReq_Severity l_mission =  List.map l_mission ~f:(fun x->(getval x reqId_M, getval x severity_M)) in
   let getSeverity_Of_cybReq req l_mission = getval (cybReq_Severity l_mission) req 
   in
   fprintf filename_ch "defenseType=\"%s\" computed_p=\"%s\" acceptable_p =\"%s\" >\n" 
                        (defenseTypeStr) (string_of_float likely) (severity2risk(getSeverity_Of_cybReq reqIDStr l_mission));
   List.iter infoList ~f:(fun ((_,p),(_,aL),(_,dL)) -> fprintf_cutSet filename_ch p aL dL);
;;

let xml_gen_safety filename_ch reqIDStr defenseTypeStr tc_ftree l_mission = 
   (* list of cutsets to print *)
   let infoList = cutSetsSafetyList (probErrorCutImp tc_ftree) 
   and (pr,_) = probErrorCut tc_ftree in
   (* internal functions *)
   let getval l tag =  List.Assoc.find_exn l tag ~equal:(=) in
   let cybReq_Safety l_mission =  List.map l_mission ~f:(fun x->(getval x reqId_M, getval x severity_M)) in
   let getSafety_Of_cybReq req l_mission = getval (cybReq_Safety l_mission) req 
   in
   fprintf filename_ch "defenseType=\"%s\" computed_p=\"%s\" acceptable_p =\"%s\" >\n" 
                       (defenseTypeStr) (string_of_float pr) (getSafety_Of_cybReq reqIDStr l_mission);
   List.iter infoList ~f:(fun ((_,p),l) -> fprintf_cutSetSafety filename_ch p l );
;;
   
(* This function returns the following tuple given the CyberReqID: (modelVersion, cyberReq, risk) 
   where cyberReq is the textual cyber requirement returned as a string  
   and risk is the severity, converted to risk returned as a string 
   and modelVersion is the ModelVersion string from mission.csv 
*)      
let get_CyberReqText_Risk mission_al cyberReqID =
  let al = List.find_exn mission_al ~f:(fun x -> cyberReqID = (List.Assoc.find_exn x ~equal:(=) reqId_M)) in
     ( List.Assoc.find_exn al ~equal:(=) modelVersion_M,
       List.Assoc.find_exn al ~equal:(=) req_M, 
       severity2risk (List.Assoc.find_exn al ~equal:(=) severity_M) ) ;;
       
(* *)
let rec extractCyberReq l_libmdl l_mission = 
   match l_libmdl with
      | hd::tl -> let ((reqIDStr, defenseTypeStr), _) = hd in
                  let rType = compReqType reqIDStr l_mission in
          	         (match rType with  
          	         | "Cyber" -> hd :: extractCyberReq tl l_mission
          	         | _ -> extractCyberReq tl l_mission)
      | [] -> [];;

let rec extractSafetyReq l_libmdl l_mission = 
   match l_libmdl with
      | hd::tl -> let ((reqIDStr, defenseTypeStr), _) = hd in
                  let rType = compReqType reqIDStr l_mission in
          	         (match rType with  
          	         | "Safety" -> hd :: extractSafetyReq tl l_mission
          	         | _ -> extractSafetyReq tl l_mission)
      | [] -> [];;

let rec removeMissionsWithNoReq l_librariesThreats =
   match l_librariesThreats with
     | hd::tl -> let (_, l_libmdl) = hd in
         (match l_libmdl with
         | [] -> removeMissionsWithNoReq tl
         | _  -> hd::removeMissionsWithNoReq tl)
     | [] -> [];;

(* *)

       
(* function to rename the "ConnectionName" so that it is a concatenation of <ConnectionName><SrcImpl><SrcComp> *)
let renameConnectionName l_arch =
   List.map l_arch ~f:(fun aL ->
      let connName = List.Assoc.find_exn aL ~equal:(=) connName_Arc
      and srcImpl = List.Assoc.find_exn aL ~equal:(=) srcImpl_Arc
      and srcComp = List.Assoc.find_exn aL ~equal:(=) srcType_Arc in
      let newConnName = connName ^ srcImpl ^ srcComp in
      (connName_Arc, newConnName)::(List.filter aL ~f:(fun (tag, _) -> tag<>connName_Arc)) );;
       

(* Analyze function - Calls model_to_adtree and model_to_ftree. Generates the artifacts *)
let do_arch_analysis ?(save_dot_ml=false) comp_dep_ch comp_saf_ch attack_ch events_ch arch_ch mission_ch defense_ch defense2nist_ch fpath infeCyber infeSafe =
   let compDepen    = mainListtag comp_dep_ch  
   and compSafe     = mainListtag comp_saf_ch
   and attack       = mainListtag attack_ch    
   and events       = mainListtag events_ch 
   and arch         = renameConnectionName (mainListtag arch_ch) (* <-- rename the "ConnectionName" so that it is a concatenation of <ConnectionName><SrcImpl><SrcComp> *)
   and mission      = mainListtag mission_ch 
   and defense      = mainListtag defense_ch 
   and defense2nist = mainListtag defense2nist_ch in
   
   (* iterate through the following: ApplicableDefenseProperties and ImplProperties*)
   List.iter [applProps_D; implProps_D] ~f:(fun deftype ->
   
     (* check if there's anything to analyze *)
     if List.is_empty mission 
     then Format.printf 
          "Info: mission.csv is empty. No requirements to analyze@." ;   
     let l_librariesThreats = libraries_threatConditions deftype compDepen compSafe attack events arch mission defense defense2nist infeCyber infeSafe
  
     in
     
     (* separate into lists based on Cyber reqs and Safety reqs, because they will be handled differently *)
     let l_librariesThreats_Cyber = 
        removeMissionsWithNoReq (
        List.map l_librariesThreats ~f:(fun mID -> let (missionReqIdStr, l_libmdl) = mID in
              (missionReqIdStr, extractCyberReq l_libmdl mission) ) )
     and l_librariesThreats_Safety = 
        removeMissionsWithNoReq (
        List.map l_librariesThreats ~f:(fun mID -> let (missionReqIdStr, l_libmdl) = mID in
              (missionReqIdStr, extractSafetyReq l_libmdl mission) ) )

     in
  
     (* [CYBER] iterate through the list of Cyber reqs *)
     (* start the xml formatted results file *)
     List.iter l_librariesThreats_Cyber ~f:(fun mID -> 
        let (missionReqIdStr, l_libmdl) = mID in
        (* iterate through the list of Cyber requirements under each mission ID *)
        List.iter l_libmdl ~f:(fun x -> 
           (let ((reqIDStr, defenseTypeStr), (lib, mdl)) = x in
              let t = model_to_adtree lib mdl 
              (* -- extract some text info -- *)
              and (modelVersion, cyberReqText, risk) = get_CyberReqText_Risk mission reqIDStr in  
              (* -- save .ml file of the lib and mdl, for debugging purposes -- *)    
              if save_dot_ml then saveLibraryAndModelToFile (fpath ^ modelVersion ^ "-" ^ reqIDStr ^ "-" ^ defenseTypeStr ^ ".ml") lib mdl ;
              (* -- cutset metric file, in printbox format -- *)    
              saveADCutSetsToFile ~cyberReqID:(reqIDStr) ~risk:(risk) ~header:("header.txt") (fpath ^ modelVersion ^ "-" ^ reqIDStr ^ "-" ^ defenseTypeStr ^ ".txt") t ;
              (* -- save .svg tree visualizations -- *)    
              dot_gen_show_adtree_file (fpath ^ modelVersion ^ "-" ^ reqIDStr ^ "-" ^ defenseTypeStr) t ;
           )
        );
     );
     (* fill xml datastruct, print datastruct to an xml file, and close the xml file  *)
     let ds_MissionList = xmlBuilder_MissionList "cyber" l_librariesThreats_Cyber mission defense2nist in
     let ds = Xml.Element("Results",[("xmlns:xsi","http://www.w3.org/2001/XMLSchema-instance")], ds_MissionList) in
     let xml_str = Xml.to_string_fmt ds in
     let xml_oc = Out_channel.create (fpath^deftype^".xml") in
     fprintf xml_oc "<?xml version=\"1.0\"?>\n";
     fprintf xml_oc "%s" xml_str;
     Out_channel.close xml_oc; 
        

     (* [SAFETY] iterate through the list of Safety reqs *)
     List.iter l_librariesThreats_Safety ~f:(fun mID -> 
        let (missionReqIdStr, l_libmdl) = mID in
        (* iterate through the list of Safety requirements under each mission ID *)
        List.iter l_libmdl ~f:(fun x -> 
           (let ((reqIDStr, defenseTypeStr), (lib, mdl)) = x in
              let t = model_to_ftree lib mdl
              (* -- extract some text info -- *)
              and (modelVersion, safetyReqText, risk) = get_CyberReqText_Risk mission reqIDStr in  
              (* -- save .ml file of the lib and mdl, for debugging purposes -- *)    
              if save_dot_ml then saveLibraryAndModelToFile (fpath ^ modelVersion ^ "-" ^ reqIDStr ^ "-" ^ defenseTypeStr ^ ".ml") lib mdl ;
              (* -- cutset metric file, in printbox format -- *)    
              saveCutSetsToFile ~reqID:(reqIDStr) ~risk:(risk) ~header:("header.txt") (fpath ^ modelVersion ^ "-" ^ reqIDStr ^ "-" ^ defenseTypeStr ^ "-safety.txt") t ;
              (* -- save .svg tree visualizations -- *)    
              dot_gen_show_tree_file (fpath ^ modelVersion ^ "-" ^ reqIDStr ^ "-" ^ defenseTypeStr) t ;
           )
        )
     );
     (* fill xml datastruct, print datastruct to an xml file, and close the xml file  *)
     let ds_MissionList = xmlBuilder_MissionList "safety" l_librariesThreats_Safety mission defense2nist in
     let ds = Xml.Element("Results",[("xmlns:xsi","http://www.w3.org/2001/XMLSchema-instance")], ds_MissionList) in
     let xml_str = Xml.to_string_fmt ds in
     let xml_safety_oc = Out_channel.create (fpath^deftype^"-safety.xml") in
     fprintf xml_safety_oc "<?xml version=\"1.0\"?>\n";
     fprintf xml_safety_oc "%s" xml_str;
     Out_channel.close xml_safety_oc; 

  )
;;