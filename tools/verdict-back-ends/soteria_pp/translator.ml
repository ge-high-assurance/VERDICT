(* 
Authors: Heber Herencia-Zapana, Kit Siu
Date: 25/03/2019

Updates: 4/18/2019, Kit Siu, added function to generate cutset report using PrintBox.
         9/27/2019, Heber Herencia-zapana, added functions to generate defense profiles
         11/05/2019, Kit Siu, added functions to fill in safety information
         11/15/2019, Kit Siu, added functions to deal with hierarchy
*)

(**
	The top-level functions for the translation from AADL to .csv files are as follows:
	
	- {b do_arch_analysis} : the top-level function to call to build the library and model
	(in memory) and to do both the safety analysis and the security analysis. This function
	takes the 7 required files read-in as list of lines using In_channel.read_lines. This
	function also requires the output file path to be specified.
	
	- {b libraries_threatConditions} : this function generates a list with the following
	tuple: ((reqID,defenseType), (lib,mdl)) where cyberReqID is a string with the 
	requirement identifier, defenseType is either the applicable defense properties or 
	the implemented defense properties (see Defenses.csv for the header names used)
	and the lib and mdl are the corresponding library and model for the requirement ID and
	the defenseType.
	
	- {b mainListtag} : this function takes a list of lines (from In_channel.read_lines) and 
	creates an association list.
	
	Some commands for debugging: one way to debug these functions is to execute the following:
	# 
	# let compDepen = mainListtag (In_channel.read_lines "test/CompDep.csv");;
	# let compSafe = mainListtag (In_channel.read_lines "test/CompSaf.csv");;
	# let attack = mainListtag (In_channel.read_lines "test/CAPEC.csv");;
	# let events = mainListtag (In_channel.read_lines "test/Events.csv");;
	# let arch = mainListtag (In_channel.read_lines "test/ScnArch.csv");;
	# let mission = mainListtag (In_channel.read_lines "test/Mission.csv");;
	# let defense = mainListtag (In_channel.read_lines "test/Defenses.csv");;
	#
	# let deftype = "ApplicableDefenseProperties";;
	#
	# let l_librariesThreats = libraries_threatConditions deftype compDepen compSafe attack events arch mission defense;;
	# let ((reqIDStr, defenseTypeStr), (lib,mdl)) = List.hd_exn l_librariesThreats;;
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
let(compType_D, capec_D, c_D,               i_D,         a_D,            applProps_D,                   implProps_D,      dal_D)=
   ("CompType", "CAPEC", "Confidentiality", "Integrity", "Availability", "ApplicableDefenseProperties", "ImplProperties", "DAL");;
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

let defenseProfileAux name defType l_defense l_defense2nist =
    let capec x = fst x  
    and defenList x = snd (snd x) in
    let cp = List.concat (List.map (filterDefense name  defType l_defense) ~f:(fun x-> [(capec x, defenList x)])) in
    let cpsplit = List.map cp ~f:(fun (c,pl) -> (c, (List.map pl ~f:(fun p -> String.split_on_chars ~on:[';'] p)))) in
    let cpconverted = List.map cpsplit ~f:(fun (c,pl) -> (c, List.map pl ~f:(fun p -> convertProp2Profile p l_defense2nist))) in
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

let defenseProfile name defType l_defense l_defense2nist =
   let compDefensesList = defenseProfileAux name defType l_defense l_defense2nist in
   postProcessDefenseProfile (processDL compDefensesList);;

(* - * - * - *)

(* from l_events we get basic_events and event_info *)
let (eventsH_CompType, eventsH_Event, eventsH_Probability) =
	("Comp",           "Event",       "Probability");;
		
let compEvents name l_events = comp_find eventsH_Event (compInfo name eventsH_CompType l_events);;
let compEventsInfo name l_events = List.map (comp_find eventsH_Probability (compInfo name eventsH_CompType l_events)) ~f:(fun x -> (float_of_string x, 1.0));;

(* - * - * - *)

(* from l_comp_saf we get faults and fault_formulas *)
let (compsafH_CompType, compsafH_IOrE,      compsafH_InputIA, compsafH_Out, compsafH_OutputIA) =
	("Comp",            "InputPortOrEvent", "InputIAOrEvent", "OutputPort", "OutputIA");;

let compFaults name l_comp_saf = comp_find2 compsafH_InputIA (comp name l_comp_saf);;

let compsaf_filter compName out ia l_comp_saf = compInfo ia compsafH_OutputIA (compInfo out compsafH_Out (comp compName l_comp_saf));;
let compsafOut_In name out ia l_comp_saf = 
	let f x tag =  List.Assoc.find_exn x tag ~equal:(=) in
    let l = compsaf_filter name out ia l_comp_saf in
    	List.map l ~f:(fun x -> if (f x compsafH_IOrE) <> "" 
        						then [ f x compsafH_IOrE ; f x compsafH_InputIA ] else []);;

let formulaSafe_OrAnd listElement =
	match listElement with
	 [e] -> F[e]
	|[inList;iaList] -> 
		let inList_split = String.split_on_chars inList ~on:[';'] 
		and iaList_split = String.split_on_chars iaList ~on:[';'] 
		in
		let fList = (List.map2_exn inList_split iaList_split ~f:(fun i ia -> F (List.append [i] [ia]))) in
		And fList ;;

let formulaSafe_aux name out ia l_comp_saf = 
	let l = noEmptyList (compsafOut_In name out ia l_comp_saf)in
	let l2 = List.map l ~f:(fun x -> List.filter x ~f:(fun e -> e<>"")) in
	Or (List.map l2 ~f:(fun x -> formulaSafe_OrAnd x));;

let formulaSafe name coutputs l_arch l_comp_saf =
    let formula_type name out ia = ([out; ia], formulaSafe_aux name out ia l_comp_saf) in
    let formulaOut name out = List.map ["Integrity";"Availability"] ~f:(fun x-> formula_type name out x) in
    (List.concat (List.map coutputs ~f:(fun x->formulaOut name x)));; 

(* - * - * - *)

(* from l_arch we get lib component names, lib component inputs & outputs, mdl instances, mdl connections *)
let (srcIns_Arc,        srcType_Arc, srcImpl_Arc, srcPortName_Arc, srcPortType_Arc, desIns_Arc,         desType_Arc, desImpl_Arc, desPortName_Arc, desPortType_Arc )=
    ("SrcCompInstance", "SrcComp",   "SrcImpl",   "SrcPortName",   "SrcPortType",   "DestCompInstance", "DestComp",  "DestImpl",  "DestPortName",  "DestPortType");;

let compInputArch name l_arch = (* by design, lib component inputs are all DestComp DestPortNames *)
   let f x tag = List.Assoc.find_exn x tag ~equal:(=) in
   let cinfo = compInfo name desType_Arc l_arch in
   List.dedup_and_sort ~compare:compare (List.map cinfo ~f:(fun x -> f x desPortName_Arc)) ;;

let compOutputArch name l_arch = (* by design, lib component outputs are all SrcComp SrcPortNames *)
   let f x tag = List.Assoc.find_exn x tag ~equal:(=) in
   let cinfo = compInfo name srcType_Arc l_arch in
   List.dedup_and_sort ~compare:compare (List.map cinfo ~f:(fun x -> f x srcPortName_Arc)) ;;

let instancesArch arch = 
   let f x tag = List.Assoc.find_exn x tag ~equal:(=) in
   List.dedup_and_sort ~compare:compare (List.map arch ~f:(fun x-> makeInstance ~i:(f x srcIns_Arc) ~c:(f x srcType_Arc ) ()));;

let connectionsArch arch = 
   let f x tag = List.Assoc.find_exn x tag ~equal:(=) in
   let compIns x = f x srcIns_Arc in
   let compPort x = f x srcPortName_Arc in
   let destCompIns x = f x desIns_Arc in
   let destCompPort x = f x desPortName_Arc in     
   List.map arch ~f:(fun x->((destCompIns x, destCompPort x),(compIns x,compPort x)));;

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
   - if in Arch a des port is of type "out", then make a new line where input port is the org name with "_dotI" (i.e., dot In) and output port is the org name
compType_C, inputPort_C, outputPort_C, inputCIA_C, outputCIA_C
*)
let rec makeCompDepen_subList arch cdL =
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
            ("in","in")   -> let compA_list = compAttacks srcCompType cdL in
                             (List.append (makeCompDepen_subList tl cdL) 
                                         (List.map compA_list ~f:(fun a -> [(compType_C, srcCompType);
                                                                            (inputPort_C, (String.chop_suffix_exn ~suffix:"_dotO" srcPortName));
                                                                            (inputCIA_C, a);
                                                                            (outputPort_C, srcPortName);
                                                                            (outputCIA_C, a)])))            
          | ("out","out") -> let compA_list = compAttacks desCompType cdL in
                             (List.append (makeCompDepen_subList tl cdL) 
                                         (List.map compA_list ~f:(fun a -> [(compType_C, desCompType);
                                                                            (inputPort_C, desPortName);
                                                                            (inputCIA_C, a);
                                                                            (outputPort_C, (String.chop_suffix_exn ~suffix:"_dotI" desPortName));
                                                                            (outputCIA_C, a)])))       
          | _             -> makeCompDepen_subList tl cdL)
    | [] -> [] ;;

let massageCompDepen compDepen arch =
   let newCDs = makeCompDepen_subList arch compDepen in
   List.append compDepen newCDs ;;

(* - * - * - *)

(* from l_mission we get components, instances, connections, and top_attack*)
let (modelVersion_M, missionReqId_M,  reqType_M, cyberReqId_M, cyberReq_M, compOutputDependency_M, missionImpactCIA_M, severity_M, compInstanceDependency_M, cia_M)=
    ("ModelVersion", "MissionReqId", "ReqType", "CyberReqId", "CyberReq", "CompOutputDependency", "MissionImpactCIA", "Severity", "CompInstanceDependency", "DependentCompOutputCIA");;

let compMission name l_mission = compInfo name cyberReqId_M l_mission;;
let compReqType name l_mission = List.hd_exn( List.dedup_and_sort ~compare:compare (comp_find reqType_M (compMission name l_mission)));;
let compInputMission name l_mission= comp_find compOutputDependency_M (compMission name l_mission);;
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
	| _ -> raise (Error "conjuncAtckFltList exception");;

let listConjuncAtckFltList str1 str2 rType =
	let split_str1 = String.split_on_chars str1 ~on:[';'] 
	and split_str2 = String.split_on_chars str2 ~on:[';'] in
	conjuncAtckFltList (List.zip_exn split_str1 split_str2) rType;;

let compOut_InMission name cia l_mission =
	let missionImpact_list = compMissionCIA name cia l_mission in
	let outputs_list = List.map missionImpact_list ~f:(fun x -> List.Assoc.find_exn x compOutputDependency_M ~equal:(=)) in
	let outputscia_list = List.map missionImpact_list ~f:(fun x -> List.Assoc.find_exn x cia_M ~equal:(=)) in
	let rType = compReqType name l_mission in
	List.map2_exn outputs_list outputscia_list ~f:(fun outputStr ciaStr -> listConjuncAtckFltList outputStr ciaStr rType);;

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

let missionIds l_mission  =  comp_find cyberReqId_M l_mission;;
let missionReqId l_mission = comp_find missionReqId_M l_mission;;

(* - * - * - *)
(* - * - * - *)

(*Independent of Header*)
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
(*generate component for component name *)
let formula_aux name out cia l_comp_dep l_attack =  
                               let compCIA name cia = noEmptyList (attack_cia name cia l_attack)in
                                noEmptyList (List.concat [compCIA name cia;compOut_In name out cia l_comp_dep]);;   
                                
let formula_OrAnd_A listElement =
	match listElement with
	 [a] -> A[a]
	|[iList;ciaList] -> 
		let iList_split = String.split_on_chars iList ~on:[';'] 
		and ciaList_split = String.split_on_chars ciaList ~on:[';'] 
		in
		let aList = (List.map2_exn iList_split ciaList_split ~f:(fun i cia -> A (List.append [i] [cia]))) in
		And aList ;;

let formula name coutputs l_arch l_comp_dep l_attack = 
    (*let formula_Or_A name out cia =  Or (List.map (formula_aux name out cia l_comp_dep l_attack) ~f:(fun x-> A x)) in*)
	let formula_Or_A name out cia = Or (List.map (formula_aux name out cia l_comp_dep l_attack) ~f:(fun x -> formula_OrAnd_A x)) in
    let formula_type name out cia =  ([out; cia],formula_Or_A name out cia) in
    (*let formulaOut name out = List.map (compAttacksOut name l_comp_dep) ~f:(fun x-> formula_type name out x) in *)
    let formulaOut name out = List.map ["Confidentiality";"Integrity";"Availability"] ~f:(fun x-> formula_type name out x) in
    (*let clean l = List.filter l ~f:(fun x->  snd x <> Or []) in*)
    (List.concat (List.map coutputs ~f:(fun x->formulaOut name x)));;
                       
let gen_Comp name defType l_arch l_comp_dep l_comp_saf l_attack l_defense l_defense2nist l_events = 
	(* Below calls the function genComp which creates a lib comp with the following fields filled in *)
	let coutputs = (compOutputArch name l_arch) 
	and (attacksList, infoList) = (makeAttackList_AttackInfoList name l_attack)
	and (eventsList, rigorsList) = (makeDefenseList_DefenseRigorsList name defType l_defense l_defense2nist) in
	genComp (*name*)            name 
			(*input_flows*)     (compInputArch name l_arch) 
			(*output_flows*)    coutputs
			(*faults*)          ["Integrity";"Availability"] (*(List.filter (List.dedup_and_sort ~compare:compare (List.append (compFaults name l_comp_saf) (compFaultsOut name l_comp_saf))) ~f:(fun x -> x <> "") )*)
			(*basic_events*)    (compEvents name l_events) 
			(*event_info*)      (compEventsInfo name l_events) 
			(*fault_formulas*)  (formulaSafe name coutputs l_arch l_comp_saf)
			(*attacks*)         ["Confidentiality";"Integrity";"Availability"] (*(List.filter (List.dedup_and_sort ~compare:compare (List.append (compAttacks name l_comp_dep ) (compAttacksOut name l_comp_dep))) ~f:(fun x -> x <> "") ) *)
			(*attack_events*)   attacksList (* (attack_events name l_attack) *)
			(*attack_info*)     infoList    (* (attack_info name l_attack) *)
			(*attack_formula*)  (formula name coutputs l_arch l_comp_dep l_attack) 
			(*defense_events*)  eventsList  (* (defenseEvents name defType l_defense) *)
			(*defense_rigors*)  rigorsList  (* (defenseRigors name defType l_defense) *)
			(*defense_profiles*)(defenseProfile name defType l_defense l_defense2nist);;
(* - *)

(*generate component mission*)
let changeName  = (fun x -> match x with 
                               "Availability"->"loa"
                              |"AVAILABILITY"->"loa"
                              |"Integrity"   ->"loi"
                              |"INTEGRITY"   ->"loi"
                              |x->x);;
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
                            
let topAttack cyberReqId l_mission= (cyberReqId, A["out";changeName (List.hd_exn(missionImpact cyberReqId l_mission))]);;                             
let topFault cyberReqId l_mission= (cyberReqId, F["out";changeName (List.hd_exn(missionImpact cyberReqId l_mission))]);;                             

(**)     
let genModel ins conn fault attack = 
    { instances = ins;
      connections = conn;
      top_fault = fault;
      top_attack = attack 
      };;
let instances cyberReqId l_arch = (makeInstance ~i:cyberReqId ~c:cyberReqId ())::(instancesArch l_arch);;
let connections cyberReqId l_arch l_mission= List.append (connectionsArch l_arch) (compInstOut cyberReqId l_mission);;
let gen_model cyberReqId l_arch l_mission = 
	let rType = compReqType cyberReqId l_mission in
	let (fault,attack) = 
	    match rType with
    	| "Cyber" -> (("", F["";""]),(topAttack cyberReqId l_mission))
    	| "Safety" -> ((topFault cyberReqId l_mission), ("", A["";""]))
    	| _ -> raise (Error "gen_model exception: req is neither CyberReq nor SafetyReq") 
    in
	genModel (instances cyberReqId l_arch) (connections cyberReqId l_arch l_mission) fault attack;;
 
(**)
let gen_library cyberReqId defType l_comp_dep l_comp_saf l_attack l_events l_arch l_defense l_defense2nist l_mission = 
    let components = List.map (list_comp l_arch) ~f:(fun x -> gen_Comp x defType l_arch l_comp_dep l_comp_saf l_attack l_defense l_defense2nist l_events) in
    let rType = compReqType cyberReqId l_mission in
    match rType with
    | "Cyber" -> List.append components [gen_CompMission cyberReqId l_mission]
    | "Safety" -> List.append components [gen_CompSafety cyberReqId l_mission]
    | _ -> raise (Error "gen_library exception: req is neither CyberReq nor SafetyReq");;


(* iterate through the following: ApplicableDefenseProperties and ImplProperties*)

let libraries_threatConditions deftype compDepen compSafe attack events arch mission defense defense2nist =
   let arch_prime = massageArch arch in
   let compDepen_prime = massageCompDepen compDepen arch_prime in
   List.map (missionIds mission)
        ~f:(fun x->((x,deftype),((gen_library x deftype compDepen_prime compSafe attack events arch_prime defense defense2nist mission), 
                                  gen_model x arch_prime mission))) ;;

    
(* translate mission severity to level of risk *)
let severity2risk severity =
  match severity with
  | "Catastrophic" -> "1e-09"
  | "Hazardous"    -> "1e-07"
  | "Major"        -> "1e-05"
  | "Minor"        -> "1e-03"
  | _              -> "1"    ;;

(* function that saves to a .ml file the library and the model as an artifact for the end-user *)
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

let cutSets cutSet =     
  let cutSetToView cOpe = 
      match cOpe with 
      | APro (h::tl) -> List.append (takeAvar h) (takeDefense (List.hd_exn tl))
      | AVar avar -> List.append (takeAvar (AVar avar)) ["defense", ""]
      | _ -> List.append (takeAvar AFALSE) ["defense",""] in
    let (cut,pro1,_) = cutSet in
    List.append [("prob",(string_of_float pro1))] (cutSetToView cut);;

let cutSetsList cutSetList = List.map cutSetList ~f:(fun x-> cutSets x);;

let concat_And_Or op l =List.fold (List.tl_exn l) ~init:(List.hd_exn l) ~f:(fun x y -> x^" "^ op^" " ^y) ;;

(*let compAttackDefense name  attack defType l_defense l_defense2nist = 
   let listDefn = defenseProfileAux name defType l_defense l_defense2nist in
   let listcapecDefenAux = List.Assoc.find_exn (listDefn) attack ~equal:(=) in 
   let listcapecDefen = List.map (listcapecDefenAux) ~f:(fun x->String.split_on_chars x ~on:[';']) in
   let listAND = List.map (listcapecDefen) ~f:(fun x-> ("("^ (concat_And_Or "and" x))^")") in
   concat_And_Or "or" listAND;;
*)

let cybReqAttackDefenseProb cyberReqIDStr defenseTypeStr lib mdl =
   let tc_adtree = model_to_adtree lib mdl in
      ((cyberReqIDStr,defenseTypeStr), (likelihoodCut tc_adtree, cutSetsList (likelihoodCutImp tc_adtree) ));;                  


let xml_gen filename_ch listCutsetInfo mission = (* defType l_defense = *)
    let lReqs_of_mission missionID mission = missionIds (compInfo missionID missionReqId_M mission) in
    let cutset_of_Mission missionID mission lcutsets = List.filter lcutsets ~f:(fun x->List.mem (lReqs_of_mission missionID mission) (fst (fst x))  ~equal:(=) ) in
    let getval l tag =  List.Assoc.find_exn l tag ~equal:(=) in 
    let cybReq_Severity lmission =  List.map lmission ~f:(fun x->(getval x cyberReqId_M, getval x severity_M)) in
    let getSeverity_Of_cybReq req lmission = getval (cybReq_Severity lmission) req in 
    let reqId l = (fst(fst l)) in let defenseType l = (snd(fst l))in
    let reqPro l = (string_of_float (fst(snd l))) in         
    let probCompAttackDefense l = List.iter (snd(snd l)) 
             ~f:(fun x-> cutSet filename_ch (getval x "prob") (getval x "comp") (getval x "attack") 
             (*(compAttackDefense (getval x "comp")  (getval x "attack") defType l_defense)*)
             (getval x "defense" )) in
    let cyberReq_of_Mission listReq  = List.iter   listReq ~f:(fun x->
          fprintf filename_ch "\t<Requirement label=  \"%s\" defenseType= \"%s\" computed_p= \"%s\" acceptable_p = \"%s\" >\n" (reqId x)(defenseType x) (reqPro x) 
                         (severity2risk(getSeverity_Of_cybReq (reqId x) mission));
          probCompAttackDefense x;
          fprintf filename_ch "\t</Requirement > \n" ) in
    List.iter (missionReqId mission)  ~f:(fun x-> fprintf filename_ch "<Mission label=  \"%s\" >\n" x;
                                                  cyberReq_of_Mission (cutset_of_Mission x mission listCutsetInfo) ;
                                                  fprintf filename_ch "</Mission > \n");
;;
   

(* This function returns the following tuple given the CyberReqID: (modelVersion, cyberReq, risk) 
   where cyberReq is the textual cyber requirement returned as a string  
   and risk is the severity, converted to risk returned as a string 
   and modelVersion is the ModelVersion string from mission.csv 
*)      
let get_CyberReqText_Risk mission_al cyberReqID =
  let al = List.find_exn mission_al ~f:(fun x -> cyberReqID = (List.Assoc.find_exn x ~equal:(=) cyberReqId_M)) in
     ( List.Assoc.find_exn al ~equal:(=) modelVersion_M,
       List.Assoc.find_exn al ~equal:(=) cyberReq_M, 
       severity2risk (List.Assoc.find_exn al ~equal:(=) severity_M) ) ;;

(* Analyze function - calls model_to_adtree and generates the artifacts *)
let analyze deftype comp_dep_ch comp_saf_ch attack_ch events_ch arch_ch mission_ch defense_ch defense2nist_ch fpath =
   let compDepen =  mainListtag comp_dep_ch  
   and compSafe = mainListtag comp_saf_ch
   and attack    = mainListtag attack_ch
   and events = mainListtag events_ch 
   and arch = mainListtag arch_ch 
   and mission = mainListtag mission_ch 
   and defense = mainListtag defense_ch 
   and defense2nist = mainListtag defense2nist_ch
   and xml_oc = Out_channel.create (fpath^deftype^".xml")  
   in

   (* check if there's anything to analyze *)
   if List.is_empty mission 
   then Format.printf 
        "Info: mission.csv is empty. No requirements to analyze@." ;
   
   let l_librariesThreats = libraries_threatConditions deftype compDepen compSafe attack events arch mission defense defense2nist
   in

   (* start the xml formatted results file *)
   fprintf xml_oc "<?xml version=\"1.0\"?>\n";
   fprintf xml_oc "<Results xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n";

   List.iter l_librariesThreats ~f:(fun x -> 
      let ((cyberReqIDStr,defenseTypeStr), (lib,mdl)) = x in
         (* -- extract some text info -- *)
         let (modelVersion, cyberReqText, risk) = get_CyberReqText_Risk mission cyberReqIDStr in  
         (* -- save .ml file of the lib and mdl for reference -- *)    
         saveLibraryAndModelToFile (fpath ^ modelVersion ^ "-" ^ cyberReqIDStr ^ "-" ^ defenseTypeStr ^ ".ml") lib mdl ;
         (* -- determine whether to perform cyber or safety analysis -- *)
         let rType = compReqType cyberReqIDStr mission in
          	match rType with
         	| "Cyber" -> let t = model_to_adtree lib mdl in
               (* cutset metric file, in printbox format *)    
               saveADCutSetsToFile ~cyberReqID:(cyberReqIDStr) ~risk:(risk) ~header:("header.txt") (fpath ^ modelVersion ^ "-" ^ cyberReqIDStr ^ "-" ^ defenseTypeStr ^ ".txt") t ;
               (* tree visualizations *)    
               dot_gen_show_adtree_file (fpath ^ modelVersion ^ "-" ^ cyberReqIDStr ^ "-" ^ defenseTypeStr) t ;
               (*.xml file *)
               xml_gen xml_oc [(cybReqAttackDefenseProb cyberReqIDStr defenseTypeStr lib mdl)] mission;
    	    | "Safety" -> let t = model_to_ftree lib mdl in
               (* tree visualizations *)    
               dot_gen_show_tree_file (fpath ^ modelVersion ^ "-" ^ cyberReqIDStr ^ "-" ^ defenseTypeStr) t ;
            | _ -> raise (Error "analyze exception: req is neither CyberReq nor SafetyReq")
      );
    
    (* complete the xml file  *)
    fprintf xml_oc "</Results>\n";
    Out_channel.close xml_oc    
;;

(* iterate through the following: ApplicableDefenseProperties and ImplProperties*)

let do_arch_analysis comp_dep comp_saf attack events arch mission defense defense2nist fpath =
  List.iter [applProps_D; implProps_D] ~f:(fun x-> analyze x comp_dep comp_saf attack events arch mission defense defense2nist fpath);  
;;
    
