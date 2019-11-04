 let mycomp = 
    {name	        = "MissionPlanner";
    input_flows     = ["iE6"; "iE11"; "iE12"];
    output_flows    = ["oE6"; "oE11"; "oE12"]; 
    faults	        = ["loi_flt"; "loa_flt"];
    basic_events    = ["mpLoiFlt"; "mpLoaFlt";];
    event_info	    = [(1.0, 0.0); (1e-7, 0.0)];
    fault_formulas	= [(["oE6";  "loi_flt"], Or[ F["mpLoiFlt"]; F["iE11";"loi_flt"]; F["iE12";"loi_flt"]; F["iE6";"loi_flt"] ]); 
                       (["oE11"; "loi_flt"], Or[ F["mpLoiFlt"]; F["iE6";"loi_flt"] ]);
                       (["oE12"; "loi_flt"], Or[ F["mpLoiFlt"]; F["iE6";"loi_flt"] ]);
                       (["oE6";  "loa_flt"], Or[ F["mpLoaFlt"]; F["iE11";"loa_flt"]; F["iE6";"loa_flt"] ]);
                       (["oE11"; "loa_flt"], Or[ F["mpLoaFlt"]; F["iE6";"loa_flt"] ]);
                       (["oE12"; "loa_flt"], Or[ F["mpLoaFlt"]; F["iE6";"loa_flt"] ]); ];
    attacks  	    = ["loi_atck"; "loa_atck"];
    attack_events   = ["mpLoiAtck"; "mpLoaAtck"];
    attack_info     = [ 1.0; 1.0 ];
    attack_formulas = [(["oE6";  "loi_atck"], Or[ A["mpLoiAtck"]; A["iE11";"loi_atck"]; A["iE12";"loi_atck"]; A["iE6";"loi_atck"] ]);
                       (["oE11"; "loi_atck"], Or[ A["mpLoiAtck"]; A["iE6";"loi_atck"] ]);
                       (["oE12"; "loi_atck"], Or[ A["mpLoiAtck"]; A["iE6";"loi_atck"] ] );
                       (["oE6";  "loa_atck"], Or[ A["mpLoaAtck"]; A["iE11";"loa_atck"]; A["iE6";"loa_atck"] ]);
                       (["oE11"; "loa_atck"], Or[ A["mpLoaAtck"]; A["iE6";"loa_atck"] ] );
                       (["oE12"; "loa_atck"], Or[ A["mpLoaAtck"]; A["iE6";"loa_atck"] ] ) ];
    defense_events  = ["physicalSecurity"];
    defense_rigors    = [ 7 ]; 
    defense_profiles = [ ("loi_atck", D["physicalSecurity"]) ] ;
    };;
    
let myLib = [

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
    defense_rigors     = []; 
    defense_profiles = [] ;
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
    defense_rigors   = [ 2; 3 ]; 
    defense_profiles = [("mpLoiAtck", And[ D["physicalSecurity1"]; D["physicalSecurity2"] ])] ;
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
    defense_rigors   = []; 
    defense_profiles = [] ;
    };
    
];;  

(* ----- MODEL ----- *)
let myUAV =
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

(* ----- test out the tree functions ----- *)
let mytree = model_to_adtree myLib myUAV;;
dot_gen_show_direct_adtree_file "sample_ADComp_fullTree.svg" mytree;;
dot_gen_show_adtree_file "sample_ADComp_simplifiedTree.svg" mytree;;
cutsets_ad mytree;;
likelihoodCutImp mytree;;