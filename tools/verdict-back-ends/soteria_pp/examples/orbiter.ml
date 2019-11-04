(* orbiter.ml - to demonstrate loops *)

let library = 
 [ 
	{ name =  "MainEngine"; 
	  input_flows  = ["in"]; 
	  output_flows  = ["out"; ]; 
	  faults = ["loa"; "loi";]; 
	  basic_events = ["sys_fl";]; 
	  event_info = [(1e-07, 1.0);]; 
	  fault_formulas = [
         (["out";"loa"], Or[ F["sys_fl"]; F["in"; "loa"] ]); 
         (["out";"loi"], Or[ F["sys_fl"]; F["in"; "loi"] ]); 
	  ]; 
	  attacks = ["loa"; "loi"; ]; 
	  attack_events = ["sys_atck"; ]; 
	  attack_info = [1e-07; ]; 
	  attack_formulas = [
         (["out";"loa"], Or[ A["sys_atck"]; A["in"; "loa"] ]); 
         (["out";"loi"], Or[ A["sys_atck"]; A["in"; "loa"] ]); 
      ]; 
	  defense_events   =[]; 
 	  defense_rigors   =[]; 
	  defense_profiles =[]; 
	}; 
	{ name =  "Orbiter"; 
	  input_flows  = ["in"]; 
	  output_flows  = ["out"; ]; 
	  faults = ["loa"; "loi";]; 
	  basic_events = ["sys_fl";]; 
	  event_info = [(1e-07, 1.0);]; 
	  fault_formulas = [
         (["out";"loa"], Or[ F["sys_fl"]; F["in"; "loa"] ]); 
         (["out";"loi"], Or[ F["sys_fl"]; F["in"; "loi"] ]); 
	  ]; 
	  attacks = ["loa"; "loi"; ]; 
	  attack_events = ["sys_atck"; ]; 
	  attack_info = [1e-07; ]; 
	  attack_formulas = [
         (["out";"loa"], Or[ A["sys_atck"]; A["in"; "loa"] ]); 
         (["out";"loi"], Or[ A["sys_atck"]; A["in"; "loa"] ]); 
      ]; 
	  defense_events   =[]; 
	  defense_rigors   =[]; 
	  defense_profiles =[]; 
	}; 
];;

let model = 
{instances = [ 
	makeInstance "mainEngine"  "MainEngine" (); 
	makeInstance "orbiter"  "Orbiter" (); 
	];
 connections = [
 	(("orbiter", "in"),("mainEngine", "out"));
 	(("mainEngine", "in"),("orbiter", "out"));
 	];
 top_fault = ("orbiter", F["out"; "loa"]);
 top_attack = ("orbiter", A["out"; "loa"]);
 };;
 
[checkLibrary_componentUnique library;
 checkLibrary_nonEmptyFaults library;
 checkLibrary_nonEmptyAttacks library;
 checkLibrary_disjointInputFlowsandBasicEvents library;
 checkLibrary_disjointInputFlowsandAttackEvents library;
 checkLibrary_listsAreConsistentLengths library;
 checkLibrary_allOutputFaultsHaveFormulas library;
 checkLibrary_faultformulasMakeSense library;
 checkLibrary_attackformulasMakeSense library;
 checkLibrary_defenseformulasMakeSense library;
 checkModel_instanceNameUnique model;
 checkModel_cnameInstanceIsDefinedInLibrary model library;
 checkModel_exposureOfBasicIsDefinedInLibrary model library;
 checkModel_validConnections model library;
 checkModel_inputFlowUnique model;
];;

(*
dot_gen_show_ph_file ~rend:"pdf" model "orbiter_phy.gv";;
dot_gen_show_funct_file ~rend:"pdf" library model "orbiter_func.gv";;
*)

let orbiter_ftree = model_to_ftree library model ;;
let orbiter_ftree_cs = cutsets orbiter_ftree;;
let orbiter_prob = probErrorCut orbiter_ftree;;
let orbiter_probImp = probErrorCutImp orbiter_ftree;;
dot_gen_show_direct_tree_file ~rend: "pdf" "orbiter-fullFTree.gv"  orbiter_ftree ;;
dot_gen_show_tree_file ~rend: "pdf" "orbiter-simplifiedFTree.gv"  orbiter_ftree ;;

let orbiter_adtree = model_to_adtree library model ;;
let orbiter_adtree_cs = cutsets_ad orbiter_adtree;;
let orbiter_likelihood = likelihoodCut orbiter_adtree;;
let orbiter_likelihoodImp = likelihoodCutImp orbiter_adtree;;
dot_gen_show_direct_adtree_file ~rend: "pdf" "orbiter-fullADTree.gv"  orbiter_adtree ;;
dot_gen_show_adtree_file ~rend: "pdf" "orbiter-simplifiedADTree.gv"  orbiter_adtree ;;
