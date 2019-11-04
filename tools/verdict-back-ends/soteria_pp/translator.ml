(* 
Authors: Heber Herencia-Zapana, Kit Siu
Date: 25/03/2019

Updates: 4/18/2019, Kit Siu, added function to generate cutset report using PrintBox.
         9/27/2019, Heber Herencia-zapana, added functions to generate defense profiles
Need to be done: 
[DONE] 1. report_gen_likelihoodCutImp: Improve the human readable part 
[DONE] 2. csv_gen_likelihoodCutImp: 
[DONE] 3. csv_gen_forUserOutput:
 
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
(* - *)

(*from l_comp_dep we get input_flows,output_flows, attacks and attack_formulas (outputs-inputs dependences)*)
let (compType_C,inputPort_C,outputPort_C,inputCIA_C,outputCIA_C)=
("CompType","InputPort","OutputPort","InputCIA","OutputCIA");;

let comp compName l_comp_dep = compInfo compName compType_C l_comp_dep;;
let comp_filter compName out cia l_comp_dep = compInfo cia outputCIA_C (compInfo out outputPort_C (comp compName l_comp_dep));;
let clean l = List.filter l ~f:(fun x->x<>"");;
let compInput name l_comp_dep= clean (comp_find inputPort_C (comp name l_comp_dep));;
let compOutput name l_comp_dep= comp_find outputPort_C (comp name l_comp_dep);;
let compAttacks name l_comp_dep= comp_find inputCIA_C (comp name l_comp_dep);;
let compAttacksOut name l_comp_dep= comp_find outputCIA_C (comp name l_comp_dep);;
let compOut_In name out cia l_comp_dep= let f x tag =  List.Assoc.find_exn x tag ~equal:(=) in
                              let l = comp_filter name out cia l_comp_dep in
                              List.map l ~f:(fun x-> if (f x inputPort_C) <> "" 
                                                  then [ f x inputPort_C ; f x inputCIA_C ] else []);;
                                                  


(*from l_attack we get attack_events*)
let (compType_A,c_A,i_A,a_A,capec_A,likeli_A)=
("CompType","Confidentiality","Integrity","Availability","CAPEC","LikelihoodOfSuccess");;
let compAttack name l_attack = compInfo name compType_A l_attack;;
let attack_cia name cia l_attack = 
                          let f x tag = List.Assoc.find_exn x tag ~equal:(=) in
                          let fun_cia x = if f x c_A = cia || f x i_A = cia || f x a_A= cia 
                                          then [f x capec_A] else [] in
                          List.map (compAttack name l_attack)  ~f:fun_cia;;
let attack_events name l_attack = let f_capec x = List.Assoc.find_exn x capec_A ~equal:(=) in
                         List.map (compAttack name l_attack) ~f:f_capec ;;
let attack_info name l_attack= let f_info x = float_of_string (List.Assoc.find_exn x likeli_A ~equal:(=)) in
                         List.map (compAttack name l_attack) ~f:f_info ;;                                                   
(* - *)


(* from l_defense we get defense_events, defense_rigors and defense_profiles*)
let(compType_D,capec_D,c_D,i_D,a_D,dal_D)=("CompType","CAPEC","Confidentiality","Integrity","Availability","DAL");;
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
     let defenseDalUAux = List.map ldefenseDAL (fun x->(String.split_on_chars (fst x) ~on:[';'], snd x)) in 
     let clean x =  List.dedup_and_sort ~compare:compare (List.concat x) in 
     clean (List.map defenseDalUAux (fun x->List.map (fst x) (fun y->(y,snd x))));;
     
let defenseEvents  name defTag l_defense = 
     let def_dal = defenseEventsRigorsAUX name defTag l_defense in
     List.map def_dal (fun x->fst x);;
let defenseRigors  name defTag l_defense = 
     let def_dal = defenseEventsRigorsAUX name defTag l_defense in
     List.map def_dal (fun x->if (snd x) = "null" then 0 else int_of_string (snd x) );;

let defenseProfileAux name  defType l_defense =
         let capec x = fst x in 
         let defenList x = snd (snd x) in
         List.concat(List.map (filterDefense name  defType l_defense) 
                    ~f:(fun x-> [(capec x, defenList x)]));;

let defenseProfile name  defType l_defense =
      let capec x = fst x in
      let andOrAuxSplit x = List.map (snd x)(fun x-> String.split_on_chars x ~on:[';']) in
      let andOr x = Or (List.map (andOrAuxSplit x) (fun y -> And (List.map y (fun x1-> D[x1])))) in
      List.concat (List.map (defenseProfileAux name  defType l_defense) 
                  (fun x->if (andOrAuxSplit x =[]) then [] else [(capec x, andOr x)]))
    ;;                    

(**)

(*from l_arch we get instances, connections*)
let (ins_Arc,comp_Arc,portName_Arc,desComp_Arc,destPort_Arc)=
    ("SrcCompInstance","SrcCompType","SrcPortName","DestCompInstance","DestPortName");;
let ins arch =  let f x tag = List.Assoc.find_exn x tag ~equal:(=) in
                List.dedup_and_sort ~compare:compare (List.map arch ~f:(fun x-> makeInstance ~i:(f x ins_Arc) ~c:(f x comp_Arc )  ()));;

let connectionsArch arch = let f x tag = List.Assoc.find_exn x tag ~equal:(=) in
                       let compIns x = f x ins_Arc in
                       let compPort x = f x portName_Arc in
                       let destCompIns x = f x desComp_Arc in
                       let destCompPort x = f x destPort_Arc in     
                       List.map arch ~f:(fun x->((destCompIns x, destCompPort x),(compIns x,compPort x)));;
let list_comp arch = let f x tag = List.Assoc.find_exn x tag ~equal:(=) in 
                    List.dedup_and_sort ~compare:compare (List.map arch ~f:(fun x-> f x comp_Arc));;                        
(* - *)


(*from l_mission we get components,instances, connections, and top_attack*)
let (missionReq_Id,cyberReqId_M,compOutputDependency_M,missionImpactCIA_M,compInstanceDependency_M,c_M,i_M,a_M)=
    ("MissionReqId","CyberReqId","CompOutputDependency","MissionImpactCIA","CompInstanceDependency","Confidentiality","Integrity","Availability");;
let compMission name l_mission =  compInfo name cyberReqId_M l_mission;;
let compInputMission name l_mission= comp_find compOutputDependency_M (compMission name l_mission);;
let missionImpact name l_mission= comp_find missionImpactCIA_M (compMission name l_mission);;
let compOut_InMission name cia l_mission=  let f x tag = List.Assoc.find_exn x tag ~equal:(=) in
                              let out x = f x compOutputDependency_M in 
                              let cia_c x = f x c_M in
                              let cia_i x = f x i_M in
                              let cia_a x = f x a_M in
                              let compMissionCIA name cia l_mission= compInfo cia missionImpactCIA_M (compMission name l_mission) in 
                              List.concat(List.map (compMissionCIA name cia l_mission) ~f:(fun x-> noEmptyList
                                                                 [(if (cia_c x <> "") then [out x;cia_c x] else []);
                                                                   if (cia_i x <> "") then [out x;cia_i x] else [];
                                                                   if (cia_a x <> "") then [out x;cia_a x] else []]));;
let compAttacksMission name l_mission= let f x tag = List.Assoc.find_exn x tag ~equal:(=) in
                              let cia_c x = f x c_M in
                              let cia_i x = f x i_M in
                              let cia_a x = f x a_M in
                              let clean l = l |> List.dedup_and_sort ~compare:compare |> List.concat|>(fun x->List.filter x ~f:(fun y->y<> "")) in 
                               clean (List.map (compMission name l_mission) ~f:(fun x->[ if (cia_c x <> "") then  cia_c x else "";
                                                                   if (cia_i x <> "") then  cia_i x else "";
                                                                   if (cia_a x <> "") then  cia_a x else ""]));;
let compInstOut name l_mission=  let f x tag = List.Assoc.find_exn x tag ~equal:(=) in
                        let compIns x = f x compInstanceDependency_M in
                        let compOut x = f x compOutputDependency_M in
                        List.map (compMission name l_mission) ~f:(fun x->((name,compOut x),(compIns x,compOut x)));;                                                                  
let missionIds l_mission  =  comp_find cyberReqId_M l_mission;;
let missionReqId l_mission = comp_find missionReq_Id l_mission;;

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
                                
let formula name l_comp_dep  l_attack = 
                      let formula_Or_A name out cia =  Or (List.map (formula_aux name out cia l_comp_dep l_attack) ~f:(fun x-> A x)) in
                      let formula_type name out cia =  ([out; cia],formula_Or_A name out cia) in
                      let formulaOut name out = List.map (compAttacksOut name l_comp_dep) ~f:(fun x-> formula_type name out x) in
                      (*let clean l = List.filter l ~f:(fun x->  snd x <> Or []) in*)
                      (List.concat (List.map (compOutput name l_comp_dep) ~f:(fun x->formulaOut name x)));;
                       
let gen_Comp name defType l_comp_dep l_attack l_defense = genComp name (compInput name l_comp_dep) (compOutput name l_comp_dep) [] [] [] [] 
                            (List.filter (List.dedup_and_sort ~compare:compare (List.append (compAttacks name l_comp_dep ) (compAttacksOut name l_comp_dep))) ~f:(fun x -> x <> "") )
                            (attack_events name l_attack) (attack_info name l_attack)
                            (formula name l_comp_dep l_attack) 
                            (defenseEvents name defType l_defense) 
                            (defenseRigors name defType l_defense)
                            (defenseProfile name  defType l_defense);;
(* - *)

(*generate component mission*)
let changeName  = (fun x->match x with "Availability"->"loa"|"Integrity"->"loi"|x->x);;
let formulaMission name l_mission =  
                              let formulaCIA name cia l_mission=(["out";changeName cia],Or( List.map (compOut_InMission name cia l_mission) ~f:(fun x->A x)))in
                              List.map  (missionImpact name l_mission) ~f:(fun x-> formulaCIA name x l_mission);;

let gen_CompMission name l_mission= genComp name (compInputMission name l_mission)  ["out"] [] [] [] [] 
                            (compAttacksMission name l_mission) [] []
                            (formulaMission name l_mission) [] [] [];;
                            
let topAttack cyberReqId l_mission= (cyberReqId,A["out";changeName (List.hd_exn(missionImpact cyberReqId l_mission))]);;                             

(**)     
let genModel ins conn fault attack = 
    { instances = ins;
      connections = conn;
      top_fault = fault;
      top_attack = attack 
      };;
let instances cyberReqId l_arch = (makeInstance ~i:cyberReqId ~c:cyberReqId ())::(ins l_arch);;
let connections cyberReqId l_arch l_mission= List.append (connectionsArch l_arch) (compInstOut cyberReqId l_mission);;
let gen_model cyberReqId l_arch l_mission= genModel (instances cyberReqId l_arch) (connections cyberReqId l_arch l_mission)
                                   ("", F["";""]) (topAttack cyberReqId l_mission);;             
 
(**)
let library cyberReqId defType l_comp_dep l_attack l_arch l_mission l_defense = 
                       let components = List.map (list_comp l_arch) ~f:(fun x -> gen_Comp x defType l_comp_dep l_attack l_defense) in
                         List.append components [gen_CompMission  cyberReqId l_mission];;

(*ApplicableDefense and ImpProperty*)

let libraries_threatConditions deftype compDepen attack arch mission defense =
              List.map (missionIds mission)
             ~f:(fun x->((x,deftype),((library x deftype compDepen attack arch mission defense), gen_model x arch mission))) ;;

(* Delete
let checkLibraries deftype l_comp_dep l_attack l_arch l_mission l_defense  = 
          List.map (libraries_threatConditions deftype l_comp_dep l_attack l_arch l_mission l_defense) 
                   ~f:(fun x->(fst x,checkLibrary( fst (snd x))));;
                   
let checkModels deftype l_comp_dep l_attack l_arch l_mission l_defense=
          let librariesThreats = libraries_threatConditions deftype l_comp_dep l_attack l_arch l_mission l_defense in
          let library x = (fst (snd x)) in 
          let threatCondition x = (snd(snd x)) in 
          List.map librariesThreats ~f:(fun x->(fst x,checkModel (library x) (threatCondition x)));;           

let tc_adtrees deftype l_comp_dep l_attack l_arch l_mission l_defense arch_ch mission_ch =
         let librariesThreats = libraries_threatConditions deftype l_comp_dep l_attack l_arch l_mission l_defense arch_ch mission_ch in
         let library x = (fst (snd x)) in 
         let threatCondition x = (snd(snd x)) in
         List.map librariesThreats ~f:(fun x->(fst x, model_to_adtree (library x) (threatCondition x) ));;

let model l_mission = 
  let f l = List.Assoc.find_exn l "ModelVersion" ~equal:(=) in
   List.hd_exn (List.dedup_and_sort ~compare:compare (List.map (mainListtag l_mission) ~f:(fun x-> f x) ));;
  
 
let adtrees_files defType l_comp_dep l_attack l_arch l_mission l_defense arch_ch mission_ch =
  let tc_adtreeList = tc_adtrees defType l_comp_dep l_attack l_arch l_mission l_defense arch_ch mission_ch in
  let missionID tree = (fst (fst tree)) in
  let defenseType tree =  (snd (fst tree)) in
  (* let fullADTree tree = dot_gen_show_direct_adtree_file ~rend:
        "svg" ("modelUnderTest/"^(model l_mission)^(missionID tree)^"_"^(defenseType tree)^"fullADTree") (snd tree)  in 
  *)
  let simpAdTree tree = dot_gen_show_adtree_file  
        ((model l_mission)^(missionID tree)^"_"^(defenseType tree)^"simplifiedADTree") (snd tree) in
        (*("modelUnderTest/"^(model l_mission)^(missionID tree)^"_"^(defenseType tree)^"simplifiedADTree") (snd tree) in *)
   [ (* List.map tc_adtreeList (fun x-> fullADTree x); *)
   List.map tc_adtreeList ~f:(fun x->simpAdTree x)];;        

let arch_files deftype l_comp_dep l_attack l_arch l_mission l_defense=
         let librariesThreats = libraries_threatConditions deftype l_comp_dep l_attack l_arch l_mission l_defense in
         let library x = (fst (snd x)) in 
         let threatCondition x = (snd(snd x)) in
         let arch x = dot_gen_show_funct_file 
	         (*(library x) (threatCondition x) ("modelUnderTest/"^(model l_mission)^(fst(fst x))^"_"^(snd(fst x))^"Arch" ) in *)
	         (library x) (threatCondition x) ((model l_mission)^(fst(fst x))^"_"^(snd(fst x))^"Arch" ) in
         List.map  librariesThreats ~f:(fun x->arch x);;

let analysis deftype l_comp_dep l_attack l_arch l_mission l_defense = 
    [ checkLibraries deftype l_comp_dep l_attack l_arch l_mission l_defense ;
      checkModels deftype l_comp_dep l_attack l_arch l_mission l_defense ];;
*)    
(* translate mission severity to level of risk *)
let severity2risk severity =
  match severity with
  | "Catastrophic" -> "1e-09"
  | "Hazardous"    -> "1e-07"
  | "Major"        -> "1e-05"
  | "Minor"        -> "1e-03"
  | _              -> "1"    ;;

(* Heber function that saves to a .ml file the library and the model as an artifact for the end-user *)
let saveLibraryAndModelToFile filename lib mdl =
      let oc = Out_channel.create filename in
      print_filename oc filename;
      print_library oc lib;
      print_model oc mdl;
      Out_channel.close oc ;;
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
     AVar (a,b)-> [("comp",a);("attack",b)]
     |_->[];;

let takeDefenseAnd l = 
  let concat_And_Or op l =List.fold (List.tl_exn l) ~init:(List.hd_exn l) ~f:(fun x y -> x^" "^ op^" " ^y) in
  let clean x =  List.dedup_and_sort ~compare:compare x in 
   match l with 
   |ANot (AVar (_,b))->[b]
   |DSum l -> clean( List.map l (fun x->"("^(concat_And_Or "and" (List.map l ~f:(fun x->get_defense x)))^")" ))
   |_->["No support"] ;;  
let takeDefenseAndOr l =
  let concat_And_Or op l =List.fold (List.tl_exn l) ~init:(List.hd_exn l) ~f:(fun x y -> x^" "^ op^" " ^y) in
  let clean x =  List.dedup_and_sort ~compare:compare x in 
   match l with 
   |ANot (AVar (_,b))->[[b]]
   |DPro l ->  List.map l (fun x-> takeDefenseAnd x)
   |DSum l -> clean( List.map l (fun x->["("^(concat_And_Or "and" (List.map l ~f:(fun x->get_defense x)))^")" ]))
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
      |_-> [] in
    let (cut,pro1,_) = cutSet in
    List.append [("prob",(string_of_float pro1))] (cutSetToView cut);;

let cutSetsList cutSetList = List.map cutSetList ~f:(fun x-> cutSets x);;

let concat_And_Or op l =List.fold (List.tl_exn l) ~init:(List.hd_exn l) ~f:(fun x y -> x^" "^ op^" " ^y) ;;

let compAttackDefense name  attack defType l_defense = 
   let listDefn = defenseProfileAux name  defType l_defense in
   let listcapecDefenAux = List.Assoc.find_exn (listDefn) attack ~equal:(=) in 
   let listcapecDefen = List.map (listcapecDefenAux) (fun x->String.split_on_chars x ~on:[';']) in
   let listAND = List.map (listcapecDefen) (fun x-> ("("^ (concat_And_Or "and" x))^")") in
   concat_And_Or "or" listAND;;

let cybReqAttackDefenseProb l_librariesThreats =
(*let l_librariesThreats = libraries_threatConditions deftype comp_dep attack arch mission defense in*)
   List.map l_librariesThreats ~f:(fun x -> 
                   let ((cyberReqIDStr,defenseTypeStr), (lib,mdl)) = x in 
                   let tc_adtree = model_to_adtree lib mdl in
                      ((cyberReqIDStr,defenseTypeStr), (likelihoodCut tc_adtree, cutSetsList (likelihoodCutImp tc_adtree) )));;                  

let xml_gen filename listCutsetInfo mission = (* defType l_defense = *)
    let oc = Out_channel.create filename in
    let lReqs_of_mission missionID mission = missionIds (compInfo missionID missionReq_Id mission) in
    let cutset_of_Mission missionID mission lcutsets = List.filter lcutsets ~f:(fun x->List.mem (lReqs_of_mission missionID mission) (fst (fst x))  ~equal:(=) ) in
    let getval l tag =  List.Assoc.find_exn l tag ~equal:(=) in 
    let cybReq_Severity lmission =  List.map lmission ~f:(fun x->(getval x "CyberReqId", getval x "Severity")) in
    let getSeverity_Of_cybReq req lmission = getval (cybReq_Severity lmission) req in 
    let reqId l = (fst(fst l)) in let defenseType l = (snd(fst l))in
    let reqPro l = (string_of_float (fst(snd l))) in         
    let probCompAttackDefense l = List.iter (snd(snd l)) 
             ~f:(fun x-> cutSet oc (getval x "prob") (getval x "comp") (getval x "attack") 
             (*(compAttackDefense (getval x "comp")  (getval x "attack") defType l_defense)*)
             (getval x "defense" )) in
    let cyberReq_of_Mission listReq  = List.iter   listReq ~f:(fun x->
          fprintf oc "\t<Requirement label=  \"%s\" defenseType= \"%s\" computed_p= \"%s\" acceptable_p = \"%s\" >\n" (reqId x)(defenseType x) (reqPro x) 
                         (severity2risk(getSeverity_Of_cybReq (reqId x) mission));
          probCompAttackDefense x;
          fprintf oc "\t</Requirement > \n" ) in
    fprintf oc "<?xml version=\"1.0\"?>\n";
    fprintf oc "<Results xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n";
    List.iter (missionReqId mission)  ~f:(fun x-> fprintf oc "<Mission label=  \"%s\" >\n" x;
                                                  cyberReq_of_Mission (cutset_of_Mission x mission listCutsetInfo) ;
                                                  fprintf oc "</Mission > \n");
    fprintf oc "</Results>\n";
     Out_channel.close oc;;
   

(* This function returns the following tuple given the CyberReqID: (modelVersion, cyberReq, risk) 
   where cyberReq is the textual cyber requirement returned as a string  
   and risk is the severity, converted to risk returned as a string 
   and modelVersion is the ModelVersion string from mission.csv 
*)      
let get_CyberReqText_Risk mission_al cyberReqID =
  let al = List.find_exn mission_al ~f:(fun x -> cyberReqID = (List.Assoc.find_exn x ~equal:(=) "CyberReqId")) in
     ( List.Assoc.find_exn al ~equal:(=) "ModelVersion",
       List.Assoc.find_exn al ~equal:(=) "CyberReq", 
       severity2risk (List.Assoc.find_exn al ~equal:(=) "Severity") ) ;;

(* Analyze function - calls model_to_adtree and generates the artifacts *)
let analyze deftype comp_dep_ch attack_ch arch_ch mission_ch defense_ch fpath =
   let compDepen =  mainListtag comp_dep_ch  
   and attack    = mainListtag attack_ch 
   and arch = mainListtag arch_ch 
   and mission = mainListtag mission_ch 
   and defense = mainListtag defense_ch in

   (* check if there's anything to analyze *)
   if List.is_empty mission 
   then Format.printf 
        "Info: mission.csv is empty. No cyber resilient requirements to analyze@." ;
   
   let l_librariesThreats = libraries_threatConditions deftype compDepen attack arch mission defense in
   List.iter l_librariesThreats ~f:(fun x -> 
   let ((cyberReqIDStr,defenseTypeStr), (lib,mdl)) = x in
      let tc_adtree = model_to_adtree lib mdl 
      and (modelVersion, cyberReqText, risk) = get_CyberReqText_Risk mission cyberReqIDStr in  
      (*.xml file *)
      xml_gen (fpath^deftype^".xml") (cybReqAttackDefenseProb l_librariesThreats) mission (* deftype defense *);
      (* .ml file for reference *)    
      saveLibraryAndModelToFile (fpath ^ modelVersion ^ "-" ^ cyberReqIDStr ^ "-" ^ defenseTypeStr ^ ".ml") lib mdl ;
      (* cutset metric file, in printbox format *)    
      saveCutSetsToFile ~cyberReqID:(cyberReqIDStr) ~risk:(risk) ~header:("header.txt") (fpath ^ modelVersion ^ "-" ^ cyberReqIDStr ^ "-" ^ defenseTypeStr ^ ".txt") tc_adtree ;
      (* adtree visualizations *)    
      dot_gen_show_adtree_file (fpath ^ modelVersion ^ "-" ^ cyberReqIDStr ^ "-" ^ defenseTypeStr) tc_adtree ;
      (* user output csv files *)    
      csv_gen_likelihoodCutImp ~risk:(risk) (fpath ^ modelVersion ^ "-" ^ cyberReqIDStr ^ "-" ^ defenseTypeStr ^ "-componentsAttacked.csv") tc_adtree ;
      csv_gen_forUserOutput ~risk:(risk) cyberReqText (fpath ^ modelVersion ^ "-" ^ cyberReqIDStr ^ "-" ^ defenseTypeStr ^ "-userOutput.csv") tc_adtree ;
     )
;;

(* Entry function to perform analysis and generate artifacts *)
let analyzeArchitecture fpath =

  (* -- step 1 -- Verify that the file path exists *)
  if not(Sys.is_directory fpath = `Yes)
  then raise (Error "analyzeArchtecture: Path does not exist.")
  else 

    (* -- step 2 -- Check that the needed csv files exists *)
    if not(Sys.file_exists (fpath ^ "/MBAS-Files/ReadIn/Mission.csv" ) = `Yes ) 
    then raise( Error "analyzeArchtecture: Mission.csv does not exist in <Path>/MBAS-Files/ReadIn");
  
    if not(Sys.file_exists (fpath ^ "/MBAS-Files/ReadIn/CompDep.csv" ) = `Yes ) 
    then raise( Error "analyzeArchtecture: CompDep.csv does not exist in <Path>/MBAS-Files/ReadIn");
    
    if not(Sys.file_exists (fpath ^ "/MBAS-Files/ReadIn/ScnArch.csv" ) = `Yes ) 
    then raise( Error "analyzeArchtecture: ScnArch.csv does not exist in <Path>/MBAS-Files/ReadIn");
  
    if not(Sys.file_exists (fpath ^ "/MBAS-Files/WrittenOut/Defenses.csv" ) = `Yes ) 
    then raise( Error "analyzeArchtecture: Defenses.csv does not exist in <Path>/MBAS-Files/WrittenOut");
  
    if not(Sys.file_exists (fpath ^ "/MBAS-Files/WrittenOut/CAPEC.csv" ) = `Yes ) 
    then raise( Error "analyzeArchtecture: CAPEC.csv does not exist in <Path>/MBAS-Files/WrittenOut");
  
    (* -- step 3 -- Load data from the csv files and begin analysis*)
    let mission  = In_channel.read_lines (fpath ^ "MBAS-Files/ReadIn/Mission.csv")
    and comp_dep = In_channel.read_lines (fpath ^ "MBAS-Files/ReadIn/CompDep.csv")
    and arch = In_channel.read_lines (fpath ^ "MBAS-Files/ReadIn/ScnArch.csv")
    and defense = In_channel.read_lines (fpath ^ "MBAS-Files/WrittenOut/Defenses.csv")
    and attack =  In_channel.read_lines (fpath ^ "MBAS-Files/WrittenOut/CAPEC.csv") in
    List.map ["ApplicableDefense";"ImplProperty"] ~f:(fun x-> analyze x comp_dep attack arch mission defense fpath);
;;


let do_arch_analysis comp_dep attack arch mission defense fpath =
  List.iter ["ApplicableDefense";"ImplProperty"] ~f:(fun x-> analyze x comp_dep attack arch mission defense fpath);  
;;
    
