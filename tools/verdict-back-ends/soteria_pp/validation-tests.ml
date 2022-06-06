(* 

Copyright © 2019-2020 General Electric Company and United States Government as represented 
by the Administrator of the National Aeronautics and Space Administration.  All Rights Reserved.

Authors: Kit Siu, Hongwei Liao, Mike Noorman
Date: 2017-12-15

Updates: 9/10/2018, Kit Siu, modified to work with models that includes attack-defense.

*)

let library_good = [

    {name	         = "Sensor";
    input_flows      = [];
    output_flows     = ["out1"]; 
    faults	         = ["loa_flt"];
    basic_events     = ["senLoaFlt"];
    event_info	     = [(1e-7, 1.0)];
    fault_formulas	 = [(["out1"; "loa_flt"], F["senLoaFlt"]); ]; 
    attacks  	     = ["loi_atck"];
    attack_events    = ["senLoiAtck"];
    attack_info      = [ 1.0 ];
    attack_formulas  = [(["out1"; "loi_atck"], A["senLoiAtck"]); ];
    defense_events   = [];
    defense_profiles = [];
    defense_rigors   = []; 
    };

    {name	         = "MissionPlanner";
    input_flows      = ["in1"];
    output_flows     = ["out1"]; 
    faults	         = ["loa_flt"];
    basic_events     = ["mpLoaFlt"];
    event_info	     = [(1e-7, 1.0)];
    fault_formulas   = [(["out1"; "loa_flt"], Or[F["in1"; "loa_flt"]; F["mpLoaFlt"]]); ];
    attacks  	     = ["loi_atck"];
    attack_events    = ["mpLoiAtck"];
    attack_info      = [ 1.0 ];
    attack_formulas	 = [(["out1"; "loi_atck"], Or[A["in1"; "loi_atck"]; A["mpLoiAtck"]]); ];
    defense_events   = ["physicalSecurity1"; "physicalSecurity2";];
    defense_profiles = [("mpLoiAtck", And[ D["physicalSecurity1"]; D["physicalSecurity2"] ])] ;
    defense_rigors   = [ 2; 3 ]; 
    };

    {name	         = "Display";
    input_flows      = ["in1"];
    output_flows     = ["out1"]; 
    faults	         = ["loa_flt"];
    basic_events     = ["dispLoaFlt"];
    event_info	     = [(1e-7, 1.0)];
    fault_formulas   = [(["out1"; "loa_flt"], Or[F["in1"; "loa_flt"]; F["dispLoaFlt"]]); ]; 
    attacks  	     = ["loi_atck"];
    attack_events    = ["dispLoiAtck"];
    attack_info      = [ 1.0 ];
    attack_formulas	 = [(["out1"; "loi_atck"], Or[A["in1"; "loi_atck"]; A["dispLoiAtck"]]); ];
    defense_events   = [];
    defense_profiles = [];
    defense_rigors   = []; 
    };
    
];;  

let library_bad = [

    {name	         = "Sensor";
    input_flows      = [];
    output_flows     = ["out1"]; 
    faults	         = [];
    basic_events     = ["senLoaFlt"];
    event_info	     = [(1e-7, 1.0)];
    fault_formulas	 = [(["out1"; "loa_flt"], F["senLoaFlt"]); ]; 
    attacks  	     = [];
    attack_events    = ["senLoiAtck"];
    attack_info      = [ 1.0 ];
    attack_formulas  = [(["out1"; "loi_atck"], A["senLoiAtck"]); ];
    defense_events   = [];
    defense_profiles = [];
    defense_rigors   = []; 
    };

    {name	         = "MissionPlanner";
    input_flows      = ["in1"];
    output_flows     = ["out1"]; 
    faults	         = ["loa_flt"];
    basic_events     = ["mpLoaFlt"];
    event_info	     = [(1e-7, 1.0)];
    fault_formulas   = [(["out1"; "loa_flt"], Or[F["in1"; "loa_flt"]; F["mpLoaFlt"]]); ];
    attacks  	     = ["loi_atck"];
    attack_events    = ["mpLoiAtck"];
    attack_info      = [ 1.0 ];
    attack_formulas	 = [(["out1"; "loi_atck"], Or[A["in1"; "loi_atck"]; A["mpLoiAtck"]]); ];
    defense_events   = ["physicalSecurity1"; "physicalSecurity2";];
    defense_profiles = [("mpLoiAtck", And[ D["physicalSecurity1"]; D["physicalSecurity2"] ])] ;
    defense_rigors   = [ 2; 3 ]; 
    };

    {name	         = "Display";
    input_flows      = ["dispLoa"];
    output_flows     = ["out1"]; 
    faults	         = ["loa_flt"];
    basic_events     = ["dispLoa"];
    event_info	     = [(1e-7, 1.0)];
    fault_formulas   = [(["out"; "loa_flt"], Or[F["in1"; "loa_flt"]; F["dispLoaFlt"]]); ]; 
    attacks  	     = ["dispLoa"];
    attack_events    = ["dispLoiAtck"];
    attack_info      = [ 1.0 ];
    attack_formulas	 = [(["out1"; "loi_atck"], Or[A["in1"; "loi_atck"]; A["dispLoiAtck"]]); ];
    defense_events   = [];
    defense_profiles = [] ;
    defense_rigors   = []; 
    };

    {name	         = "Display";
    input_flows      = ["in1"];
    output_flows     = ["out1"]; 
    faults	         = ["loa_flt"];
    basic_events     = ["dispLoaFlt"];
    event_info	     = [(1e-7, 1.0)];
    fault_formulas   = [(["out1"; "loa_flt"], Or[F["in1"; "loa_flt"]; F["dispLoaFlt"]]); ]; 
    attacks  	     = ["loi_atck"];
    attack_events    = ["dispLoiAtck"];
    attack_info      = [ 1.0 ];
    attack_formulas	 = [(["out1"; "loi_atck"], Or[A["in1"; "loi_atck"]; A["dispLoiAtck"]]); ];
    defense_events   = [];
    defense_profiles = [];
    defense_rigors   = []; 
    };
    
];;  

let library_bad1 =
  [ {name	         = "Sensor";
    input_flows      = [];
    output_flows     = ["out1"]; 
    faults	         = [];
    basic_events     = ["senLoaFlt"];
    event_info	     = [];
    fault_formulas	 = [(["out1"; "loa_flt"], F["senLoiFlt"]);]; 
    attacks  	     = [];
    attack_events    = ["senLoiAtck"];
    attack_info      = [ 1.0 ];
    attack_formulas  = [(["out1"; "loi_atck"], A["senLoaAtck"]); ];
    defense_events   = ["physicalSecurity1"; "physicalSecurity2";];
    defense_profiles = [("senLoiAtck", And[ D["physicalSecurity3"]; D["physicalSecurity2"] ])] ;
    defense_rigors   = [ 2; 3 ]; 
    };
  ];;

let library_bad2 =
[ 	{name            = "Sensor";
    input_flows      = [];
    output_flows     = ["out1"]; 
    faults           = ["ued"; "loa"; "ued_or_loa"];
    basic_events     = ["sen_flt_ued";"sen_flt_loa"];
    event_info       = [(1.0e-6, 1.0); (1.0e-5, 1.0);];
    fault_formulas   = [(["out";"ued"], F["sen_flt_ued"]);
                        (["out";"loa"], F["sen_flt_loa"])];
    attacks  	     = [];
    attack_events    = [];
    attack_info      = [];
    attack_formulas  = [];
    defense_events   = [];
    defense_profiles = [] ; 
    defense_rigors   = []; 
    };
];;

let library_bad3 =
[ 	{name            = "Sensor";
    input_flows      = [];
    output_flows     = ["out1"]; 
    faults           = [];
    basic_events     = [];
    event_info       = [];
    fault_formulas   = [];
    attacks  	     = ["ued"; "loa"; ];
    attack_events    = ["sen_ued";"sen_loa"];
    attack_info      = [1.0;];
    attack_formulas  = [(["out";"ued"], F["sen_ued"]);
                        (["out";"loa"], F["sen_loa"])];
    defense_events   = [];
    defense_profiles = []; 
    defense_rigors   = []; 
    };
];;

let library_bad4 =
[ 	{name            = "Sensor";
    input_flows      = [];
    output_flows     = ["out1"]; 
    faults           = [];
    basic_events     = [];
    event_info       = [];
    fault_formulas   = [];
    attacks  	     = ["ued"; "loa"; "loc"];
    attack_events    = ["sen_ued";"sen_loa"];
    attack_info      = [1.0; 1.0];
    attack_formulas  = [(["out";"ued"], F["sen_ued"]);
                        (["out";"loa"], F["sen_loa"])];
    defense_events   = [];
    defense_profiles = []; 
    defense_rigors   = []; 
    };
];;

let library_bad5 =
[ 	{name            = "Sensor";
    input_flows      = [];
    output_flows     = ["out1"]; 
    faults           = [];
    basic_events     = [];
    event_info       = [];
    fault_formulas   = [];
    attacks  	     = ["ued"; "loa"];
    attack_events    = ["sen_ued";"sen_loa"];
    attack_info      = [ 1.0; 1.0;];
    attack_formulas  = [(["out";"ued"], F["sen_ued"]);
                        (["out";"loa"], F["sen_loa"])];
    defense_events   = ["physicalSecurity"];
    defense_profiles = [("sen_loa", D["physicalSecurity"])] ; 
    defense_rigors   = [7; 5]; 
    };
];;

(* ----- MODEL ----- *)
let gar_001 =
{ instances =
	[makeInstance "sen" "Sensor" ();
 	 makeInstance "mp" "MissionPlanner" ();
 	 makeInstance "disp" "Display" ();
  ];
  connections = 
  	[ (("mp", "in1"), ("sen", "out1"));
  	  (("disp", "in1"), ("mp", "out1"));
  	];
  top_fault = ("disp", F["out1";"loa_flt"]);
  top_attack = ("disp", A["out1";"loi_atck"]);
};;

let gar_bad =
  { instances =
	[makeInstance ~i: "sen" ~c:"Sensor" ~t:[("sen_flt_ued", 5.0)] ();
 	 makeInstance "sen" "MissionPlanner" ();
 	 makeInstance "disp" "Display" ();
 	 makeInstance "disp2" "Display2" ();
  ];
  connections = 
  	[ (("disp2", "in1"), ("mp", "out1"));
  	  (("mp", "in1"), ("sen", "out1"));
  	  (("disp2", "in1"), ("mp", "out1"));
  	];
  top_fault = ("disp", F["out1";"loa_flt"]);
  top_attack = ("disp", A["out1";"loi_atck"]);
  } ;;

let gar_badc1 =
  { instances = gar_001.instances;
    connections = 
  	[ (("nosuch", "in1"), ("sen", "out1"));
  	  (("disp", "in1"), ("mp", "out1"));
  	];
  top_fault = gar_001.top_fault;
  top_attack = gar_001.top_fault;
  } ;;

let gar_badc2 =
  { instances = gar_001.instances;
    connections = 
  	[ (("mp", "in1"), ("sen", "out1"));
  	  (("disp", "in1"), ("nosuch", "out1"));
  	];
    top_fault = gar_001.top_fault;
    top_attack = gar_001.top_fault;
  } ;;

let gar_badc3 =
  { instances = gar_001.instances;
    connections = 
  	[ (("mp", "in1"), ("sen", "out1"));
  	  (("disp", "nosuch"), ("mp", "out1"));
  	];
    top_fault = gar_001.top_fault;
    top_attack = gar_001.top_fault;
  } ;;

let gar_badc4 =
  { instances = gar_001.instances;
    connections = 
  	[ (("mp", "in1"), ("sen", "nosuch"));
  	  (("disp", "in1"), ("mp", "out1"));
  	];
    top_fault = gar_001.top_fault;
    top_attack = gar_001.top_fault;
  } ;;


(* checkLibrary_nonEmptyFaults *)

let test_checkLibrary_nonEmptyFaults () =
  assert( checkLibrary_nonEmptyFaults library_good = Ok "checkLibrary_nonEmptyFaults: pass");
  assert( checkLibrary_nonEmptyFaults library_bad = Error "Faults not defined for library component Sensor");
  true;;

test_checkLibrary_nonEmptyFaults ();;


(* checkLibrary_nonEmptyAttacks *)

let test_checkLibrary_nonEmptyAttacks () =
  assert( checkLibrary_nonEmptyAttacks library_good = Ok "checkLibrary_nonEmptyAttacks: pass");
  assert( checkLibrary_nonEmptyAttacks library_bad = Error "Attacks not defined for library component Sensor");
  true;;

test_checkLibrary_nonEmptyAttacks ();;


(* checkLibrary_componentUnique *)

let test_checkLibrary_componentUnique () =
  assert( checkLibrary_componentUnique library_good = Ok "checkLibrary_componentUnique: pass");
  assert( checkLibrary_componentUnique library_bad = Error "Library component not unique - Display");
  true;;

test_checkLibrary_componentUnique ();;


(* checkLibrary_disjointInputFlowsandBasicEvents *)

let test_checkLibrary_disjointInputFlowsandBasicEvents () =
  assert( checkLibrary_disjointInputFlowsandBasicEvents library_good = Ok "checkLibrary_disjointInputFlowsandBasicEvents: pass");
  assert( checkLibrary_disjointInputFlowsandBasicEvents library_bad = Error "Names used for input_flows and basic_events are not disjoint in component Display");
  true;;

test_checkLibrary_disjointInputFlowsandBasicEvents ();;


(* checkLibrary_disjointInputFlowsandAttackEvents *)

let test_checkLibrary_disjointInputFlowsandAttackEvents () =
  assert( checkLibrary_disjointInputFlowsandAttackEvents library_good = Ok "checkLibrary_disjointInputFlowsandAttackEvents: pass");
  assert( checkLibrary_disjointInputFlowsandAttackEvents library_bad = Error "Names used for input_flows and attack_events are not disjoint in component Display");
  true;;

test_checkLibrary_disjointInputFlowsandAttackEvents ();;


(* checkLibrary_listsAreConsistentLengths *)

let test_checkLibrary_listsAreConsistentLengths () =
  assert( checkLibrary_listsAreConsistentLengths library_good = Ok "checkLibrary_listsAreConsistentLengths: pass");
  assert( checkLibrary_listsAreConsistentLengths library_bad1 = Error "Basic events and event info are of inconsistent lengths in component Sensor");
  assert( checkLibrary_listsAreConsistentLengths library_bad2 = Error "Faults and fault formulas lists are of inconsistent lengths in component Sensor");
  assert( checkLibrary_listsAreConsistentLengths library_bad3 = Error "Attack events and attack info are of inconsistent lengths in component Sensor");
  assert( checkLibrary_listsAreConsistentLengths library_bad4 = Error "Attacks and attack formulas lists are of inconsistent lengths in component Sensor");
  assert( checkLibrary_listsAreConsistentLengths library_bad5 = Error "Defense events and defense rigors are of inconsistent lengths in component Sensor");
  true;;

test_checkLibrary_listsAreConsistentLengths ();;


(* checkLibrary_allOutputFaultsHaveFormulas *)

let test_checkLibrary_allOutputFaultsHaveFormulas () =
  assert( checkLibrary_allOutputFaultsHaveFormulas library_good = Ok "checkLibrary_allOutputFaultsHaveFormulas: pass");
  assert( checkLibrary_allOutputFaultsHaveFormulas library_bad = Error "Not all output faults have formulas, check component Sensor");
  true;;

test_checkLibrary_allOutputFaultsHaveFormulas ();;


(* checkLibrary_faultformulasMakeSense *)

let test_checkLibrary_faultformulasMakeSense () =
  assert( checkLibrary_faultformulasMakeSense library_good = Ok "checkLibrary_faultformulasMakeSense: pass");
  assert( checkLibrary_faultformulasMakeSense library_bad1 = Error("Invalid formula in component Sensor. Check fault formula [out1,loa_flt]") );
  true;;

test_checkLibrary_faultformulasMakeSense ();;


(* checkLibrary_attackformulasMakeSense *)

let test_checkLibrary_attackformulasMakeSense () =
  assert( checkLibrary_attackformulasMakeSense library_good = Ok "checkLibrary_attackformulasMakeSense: pass");
  assert( checkLibrary_attackformulasMakeSense library_bad1 = Error("Invalid formula in component Sensor. Check attack formula [out1,loi_atck]") );
  true;;

test_checkLibrary_attackformulasMakeSense ();;


(* checkLibrary_defenseformulasMakeSense *)

let test_checkLibrary_defenseformulasMakeSense () =
  assert( checkLibrary_defenseformulasMakeSense library_good = Ok "checkLibrary_defenseformulasMakeSense: pass");
  assert( checkLibrary_defenseformulasMakeSense library_bad1 = Error("Invalid formula in component Sensor. Check defense formula senLoiAtck") );
  true;;

test_checkLibrary_defenseformulasMakeSense ();;


(* checkModel_instanceNameUnique  *)

let test_checkModel_instanceNameUnique  () =
  assert( checkModel_instanceNameUnique gar_001 = Ok "checkModel_instanceNameUnique: pass");
  assert( checkModel_instanceNameUnique gar_bad = Error ("Model instance names are not unique") );
  true;;

test_checkModel_instanceNameUnique  ();;


(* checkModel_inputFlowUnique  *)

let test_checkModel_inputFlowUnique  () =
  assert( checkModel_inputFlowUnique gar_001 = Ok "checkModel_inputFlowUnique: pass");
  assert( checkModel_inputFlowUnique gar_bad = Error ("One of the input_flows in the model has more than one connections made to it.") );
  true;;

test_checkModel_inputFlowUnique  ();;


(* checkModel_cnameInstanceIsDefinedInLibrary *)

let test_checkModel_cnameInstanceIsDefinedInLibrary  () =
  assert( checkModel_cnameInstanceIsDefinedInLibrary gar_001 library_good = Ok "checkModel_cnameInstanceIsDefinedInLibrary: pass");
  assert( checkModel_cnameInstanceIsDefinedInLibrary gar_bad library_good = Error ("Invalid Component: this instantiation references a component that is not in the library: Display2") );
  true;;

test_checkModel_cnameInstanceIsDefinedInLibrary ();;


(* checkModel_exposureOfBasicIsDefinedInLibrary *)

let test_checkModel_exposureOfBasicIsDefinedInLibrary  () =
  assert( checkModel_exposureOfBasicIsDefinedInLibrary gar_001 library_good = Ok "checkModel_exposureOfBasicIsDefinedInLibrary: pass");
  assert( checkModel_exposureOfBasicIsDefinedInLibrary gar_bad library_good = Error ("Model attempts to change an invalid basic_event of a library component") );
  true;;

test_checkModel_exposureOfBasicIsDefinedInLibrary ();;


(* checkModel_validConnections *)

let test_checkModel_validConnections  () =
  assert( checkModel_validConnections gar_001 library_good = Ok "checkModel_validConnections: pass");
  assert( checkModel_validConnections gar_badc1 library_good = Error("Invalid connection: this is not an instance of a component in the model: nosuch") );
  assert( checkModel_validConnections gar_badc2 library_good = Error("Invalid connection: this is not an instance of a component in the model: nosuch") );
  assert( checkModel_validConnections gar_badc3 library_good = Error("Invalid connection: this is not a valid component input from the library: (disp, nosuch)") );
  assert( checkModel_validConnections gar_badc4 library_good = Error("Invalid connection: this is not a valid component input from the library: (sen, nosuch)") );
  assert( checkModel_validConnections gar_bad library_good = Error("Invalid connection: this instantiation references a component that is not in the library: disp2") );
  true;;

test_checkModel_validConnections  () ;;