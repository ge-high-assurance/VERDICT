(* sample UAV components to demonstrate both faults and attacks *)
#use "top.ml";; 

let library = 
 [ 
	{ name             = "GPS"; 
	  input_flows      = []; 
	  output_flows     = ["GPS_dir"; "GPS_pos"; ]; 
	  faults           = ["loa"; "ued"]; 
	  basic_events     = ["LoA"; "Ued"]; 
	  event_info       = [(1e-7, 1.0); (1e-7, 1.0)]; 
	  fault_formulas   = [(["GPS_dir"; "loa"], F["LoA"]);
	                      (["GPS_pos"; "loa"], F["LoA"]);
	                      (["GPS_dir"; "ued"], F["Ued"]);
	                      (["GPS_pos"; "ued"], F["Ued"])]; 
	  attacks          = ["loa"; "loi"; ]; 
	  attack_events    = ["CAPEC-148"; "CAPEC-601"; ]; 
	  attack_info      = [1.0; 1.0; ]; 
	  attack_formulas  = [(["GPS_dir"; "loa"], A["CAPEC-601"]); 
                          (["GPS_dir"; "loi"], A["CAPEC-148"]); 
                          (["GPS_pos"; "loa"], A["CAPEC-601"]); 
                          (["GPS_pos"; "loi"], A["CAPEC-148"]); ]; 
	  defense_events   = ["heterogeneity"; "wirelessLinkProtection";]; 
	  defense_rigors   = [7; 7;];
	  defense_profiles = [("CAPEC-148", D["heterogeneity"];);
	                      ("CAPEC-601", D["wirelessLinkProtection"]) ];
	}; 
	
	{ name             = "PositionEstimator"; 
	  input_flows      = ["GPS_dir"; "GPS_pos"; ]; 
	  output_flows     = ["currentDir"; "currentPos"; ]; 
	  faults           = ["loa"; "ued"]; 
	  basic_events     = ["LoA"; "Ued"]; 
	  event_info       = [(1e-7, 1.0); (1e-7, 1.0)]; 
	  fault_formulas   = [(["currentDir"; "loa"], Or[ F["LoA"]; F["GPS_dir";"loa"] ]);
	                      (["currentPos"; "loa"], Or[ F["LoA"]; F["GPS_pos";"loa"] ]);
	                      (["currentDir"; "ued"], Or[ F["Ued"]; F["GPS_dir";"ued"] ]);
	                      (["currentPos"; "ued"], Or[ F["Ued"]; F["GPS_pos";"ued"] ])]; 
	  attacks          = ["loa"; "loi"; ]; 
	  attack_events    = []; 
	  attack_info      = []; 
	  attack_formulas  = [(["currentDir"; "loa"], A["GPS_dir"; "loa"];); 
                          (["currentDir"; "loi"], A["GPS_dir"; "loi"];); 
                          (["currentPos"; "loa"], A["GPS_pos"; "loa"];); 
                          (["currentPos"; "loi"], A["GPS_pos"; "loi"];); ]; 
	  defense_events   = []; 
	  defense_profiles = [];
	  defense_rigors   = [];
	}; 
	
	{ name             = "Navigator"; 
	  input_flows      = ["currentDir"; "currentPos"; "waypoint"; ]; 
	  output_flows     = ["dest_reached"; "move"; ]; 
	  faults           = ["loa"; "ued"]; 
	  basic_events     = ["LoA"; "Ued"]; 
	  event_info       = [(1e-7, 1.0); (1e-7, 1.0)]; 
	  fault_formulas   = [(["dest_reached"; "loa"], Or[ F["LoA"]; F["currentDir";"loa"]; F["currentPos";"loa"]; F["waypoint";"loa"] ]);
	                      (["move"; "loa"], Or[ F["LoA"]; F["currentDir";"loa"]; F["currentPos";"loa"]; F["waypoint";"loa"] ]);
	                      (["dest_reached"; "ued"], Or[ F["Ued"]; F["currentDir";"ued"]; F["currentPos";"ued"]; F["waypoint";"ued"] ] );
	                      (["move"; "ued"], Or[ F["Ued"]; F["currentDir";"ued"]; F["currentPos";"ued"]; F["waypoint";"ued"] ])]; 
	  attacks          = ["loa"; "loi"; ]; 
	  attack_events    = []; 
	  attack_info      = []; 
	  attack_formulas  = [(["dest_reached";"loa"], Or[ A["currentDir"; "loa"]; A["currentPos"; "loa"]; A["waypoint"; "loa"]; ]); 
                          (["dest_reached";"loi"], Or[ A["currentDir"; "loi"]; A["waypoint"; "loi"]; ]); 
                          (["move";"loa"], Or[ A["currentDir"; "loa"]; A["currentPos"; "loa"]; A["waypoint"; "loa"]; ]); 
                          (["move";"loi"], Or[ A["currentDir"; "loi"]; A["currentPos"; "loi"]; A["waypoint"; "loi"]; ]); ];
	  defense_events   = []; 
	  defense_profiles = [];
	  defense_rigors   = [];
	}; 
	
	{ name             = "MissionPlanner"; 
	  input_flows      = []; 
	  output_flows     = ["tasks"; ]; 
	  faults           = ["loa"; "ued"]; 
	  basic_events     = ["LoA"; "Ued"]; 
	  event_info       = [(1e-7, 1.0); (1e-7, 1.0)]; 
	  fault_formulas   = [(["tasks"; "loa"], F["LoA"]);
	                      (["tasks"; "ued"], F["Ued"]) ];
	  attacks          = ["loa"; "loi"; ]; 
	  attack_events    = []; 
	  attack_info      = []; 
	  attack_formulas  = [(["tasks"; "loa"], Or[ ]); 
                          (["tasks"; "loi"], Or[ ]); ]; 
	  defense_events   = []; 
	  defense_profiles = [];
	  defense_rigors   = [];
	}; 

	{ name             = "FlightController"; 
	  input_flows      = ["dest_reached"; "tasks"; ]; 
	  output_flows     = ["payloadCmd"; "waypoint"; ]; 
	  faults           = ["loa"; "ued"]; 
	  basic_events     = ["LoA"; "Ued"]; 
	  event_info       = [(1e-7, 1.0); (1e-7, 1.0)]; 
	  fault_formulas   = [(["payloadCmd";"loa"], Or[ F["LoA"]; F["dest_reached";"loa"]; 
	                                                 F["tasks";"loa"] ]);
	                      (["waypoint";  "loa"], Or[ F["LoA"]; F["dest_reached";"loa"]; 
	                                                 F["tasks";"loa"] ]);
	                      (["payloadCmd";"ued"], Or[ F["Ued"]; F["dest_reached";"ued"]; 
	                                                 F["tasks";"ued"] ] );
	                      (["waypoint";  "ued"], Or[ F["Ued"]; F["dest_reached";"ued"]; 
	                                                 F["tasks";"ued"] ])]; 
	  attacks          = ["loa"; "loi"; ]; 
	  attack_events    = ["FlightControllerLoa"; "FlightControllerLoi"; ]; 
	  attack_info      = [1.000000e-09; 1.000000e-09; ]; 
	  attack_formulas  = [(["payloadCmd";"loa"], Or[ A["dest_reached"; "loa"]; A["tasks"; "loa"]; ]); 
                          (["payloadCmd";"loi"], Or[ A["dest_reached"; "loi"]; A["tasks"; "loi"]; ]); 
                          (["waypoint"; "loa"],  Or[ A["dest_reached"; "loa"]; A["tasks"; "loa"]; ]); 
                          (["waypoint";"loi"],   Or[ A["dest_reached"; "loi"]; A["tasks"; "loi"]; ]); ]; 
	  defense_events   = []; 
	  defense_profiles = [];
	  defense_rigors   = [];
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

let scenario = 
{ instances = [
   makeInstance "flight_controller" "FlightController" (); 
   makeInstance "position_estimator" "PositionEstimator" ();
   makeInstance "gps" "GPS" (); 
   makeInstance "mission_planner" "MissionPlanner" (); 
   makeInstance "navigator" "Navigator" ();  ];
  connections = [ 
   (( "flight_controller" , "dest_reached" ),( "navigator" , "dest_reached" )); 
   (( "flight_controller" , "tasks" ),( "mission_planner" , "tasks" )); 
   (( "navigator" , "currentPos" ),( "position_estimator" , "currentPos" )); 
   (( "position_estimator" , "GPS_dir" ),( "gps" , "GPS_dir" )); 
   (( "position_estimator" , "GPS_pos" ),( "gps" , "GPS_pos" )); 
   (( "navigator" , "waypoint" ),( "flight_controller" , "waypoint" )); 
   (( "navigator" , "currentDir" ),( "position_estimator" , "currentDir" ));  ]; 
  top_fault  = ("flight_controller", F["payloadCmd"; "ued"]); 
  top_attack = ("flight_controller", A["payloadCmd"; "loi"])
};;

[checkModel_instanceNameUnique scenario; 
 checkModel_cnameInstanceIsDefinedInLibrary scenario library; 
 checkModel_exposureOfBasicIsDefinedInLibrary scenario library; 
 checkModel_validConnections scenario library; 
 checkModel_inputFlowUnique scenario ];;
 
(* visualize the architecture *)
dot_gen_show_funct_file ~rend:"pdf" library scenario "sample_FADComp-func" ;;

(* convert to FTree *)
let scenario_ftree = model_to_ftree library scenario ;; 
dot_gen_show_tree_file ~rend: "pdf" "sample_FADComp-func-simplifiedFTree.gv" scenario_ftree ;;

(* convert to ADTree *)
let scenario_adtree = model_to_adtree library scenario ;; 
dot_gen_show_adtree_file ~rend: "pdf" "sample_FADComp-func-simplifiedADTree.gv" scenario_adtree ;;

