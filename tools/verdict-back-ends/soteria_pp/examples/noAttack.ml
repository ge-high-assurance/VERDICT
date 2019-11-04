#use "top.ml";; 

let library = 
 [ 
	{ name             = "a"; 
	  input_flows      = [ ]; 
	  output_flows     = ["out1"; ]; 
	  faults           = ["A"; "I"]; 
	  basic_events     = ["loa_event"; "ued_event"]; 
	  event_info       = [(1e-5, 1.0); (1e-6, 1.0)]; 
	  fault_formulas   = [(["out1"; "I"], Or[ ] );
	                      (["out1"; "A"], Or[ ] );
	                      ]; 
	  attacks          = ["A"; "I";]; 
	  attack_events    = [ ]; 
	  attack_info      = [ ]; 
	  attack_formulas  = [(["out1"; "I"], Or[ ] ); 
                          (["out1"; "A"], Or[ ] ); 
                          ]; 
	  defense_events   = [ ]; 
	  defense_rigors   = [ ];
	  defense_profiles = [ ];
	}; 
];;

[checkLibrary_componentUnique library;
 checkLibrary_nonEmptyFaults library;
 checkLibrary_nonEmptyAttacks library;
 checkLibrary_disjointInputFlowsandBasicEvents library;
 checkLibrary_disjointInputFlowsandAttackEvents library;
 checkLibrary_listsAreConsistentLengths library;
 checkLibrary_allOutputFaultsHaveFormulas library;
 checkLibrary_faultformulasMakeSense library;
 checkLibrary_attackformulasMakeSense library;
 checkLibrary_defenseformulasMakeSense library];;
 
 (* first initial architecture   *)
let scenario0 = 
{ instances = [
     makeInstance "a_impl" "a" (); 
     ];
  connections = [  
     (* (inport), (outport) *)
     ]; 
  top_fault  = ("a_impl", F["out1"; "I"]); 
  top_attack = ("a_impl", A["out1"; "I"])
};;

[checkModel_instanceNameUnique scenario0; 
 checkModel_cnameInstanceIsDefinedInLibrary scenario0 library; 
 checkModel_exposureOfBasicIsDefinedInLibrary scenario0 library; 
 checkModel_validConnections scenario0 library; 
 checkModel_inputFlowUnique scenario0 ];;

(* visualize the architecture *)
dot_gen_show_funct_file ~rend:"jpg" library scenario0 "noAttack-func" ;;

(* convert to FTree *)
let scenario_ftree = model_to_ftree library scenario0 ;; 
dot_gen_show_tree_file ~rend: "jpg" "noAttack-simplifiedFTree.gv" scenario_ftree ;;
cutsets scenario_ftree ;;
probErrorCut scenario_ftree ;;

(* convert to ADTree *)
let scenario_adtree = model_to_adtree library scenario0 ;; 
dot_gen_show_adtree_file ~rend: "jpg" "noAttack-simplifiedADTree.gv" scenario_adtree ;;
cutsets_ad scenario_adtree;;
likelihoodCut scenario_adtree ;;


