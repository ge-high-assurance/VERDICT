(* 

Copyright © 2020 General Electric Company. All Rights Reserved.

Authors: Kit Siu
Date: 2020-07-17

*)

(* --------------- TEST CASES ------------------ *)

(* ---------- *)
(* [A]<->[B], where neither A nor B have attacks *) 
let test_AB () = 

let library = 
 [ 	{ name            =  "A"; 
	  input_flows     = ["in" ;]; 
	  output_flows    = ["out";]; 
	  faults          = ["loa";]; 
	  basic_events    = [ ]; 
	  event_info      = [ ]; 
	  fault_formulas  = [(["out";"loa"], Or[ F["in"; "loa"] ]);]; 
	  attacks         = ["loa"; ]; 
	  attack_events   = ["a_atck" ]; 
	  attack_info     = [1.]; 
	  attack_formulas = [ (["out";"loa"], Or[ A["in"; "loa"] ]); ]; 
	  defense_events  = [ ]; 
 	  defense_rigors  = [ ]; 
	  defense_profiles= [ ]; 
	}; 
	{ name            =  "B"; 
	  input_flows     = ["in" ;]; 
	  output_flows    = ["out";]; 
	  faults          = ["loa";]; 
	  basic_events    = [ ]; 
	  event_info      = [ ]; 
	  fault_formulas  = [(["out";"loa"], Or[ F["in"; "loa"] ]);]; 
	  attacks         = ["loa"; ]; 
	  attack_events   = [ ]; 
	  attack_info     = [ ]; 
	  attack_formulas = [ (["out";"loa"], Or[ A["in"; "loa"]; ]); ]; 
	  defense_events  =[ ]; 
 	  defense_rigors  =[ ]; 
	  defense_profiles=[ ]; 
	}; 
]

and model = {
 instances = [   makeInstance "a"  "A" (); 
                 makeInstance "b"  "B" (); ];
 connections = [ (("b", "in"),("a", "out"));
                 (("a", "in"),("b", "out")); ];
 top_fault = ("a", F["out"; "loa"]);
 top_attack = ("a", A["out"; "loa"]);
 }

in

let cycle_adtree = model_to_adtree library model  (* <-- ASUM [] *)
in
let cycle_cutset = cutsets_ad cycle_adtree        (* <-- AFALSE *)

in 

assert( cycle_adtree = ASUM [] );
assert( cycle_cutset = AFALSE );

true
;;

(* -------- *)
(* [A]<->[B], where A has attack and B doesn't *) 
let test_AB_a_atck () =

let library = 
 [ 	{ name            =  "A"; 
	  input_flows     = ["in" ;]; 
	  output_flows    = ["out";]; 
	  faults          = ["loa";]; 
	  basic_events    = [ ]; 
	  event_info      = [ ]; 
	  fault_formulas  = [(["out";"loa"], Or[ F["in"; "loa"] ]);]; 
	  attacks         = ["loa"; ]; 
	  attack_events   = ["a_atck" ]; 
	  attack_info     = [1.]; 
	  attack_formulas = [ (["out";"loa"], Or[ A["a_atck"]; A["in"; "loa"] ]); ]; 
	  defense_events  = [ ]; 
 	  defense_rigors  = [ ]; 
	  defense_profiles= [ ]; 
	}; 
	{ name            =  "B"; 
	  input_flows     = ["in" ;]; 
	  output_flows    = ["out";]; 
	  faults          = ["loa";]; 
	  basic_events    = [ ]; 
	  event_info      = [ ]; 
	  fault_formulas  = [(["out";"loa"], Or[ F["in"; "loa"] ]);]; 
	  attacks         = ["loa"; ]; 
	  attack_events   = [ ]; 
	  attack_info     = [ ]; 
	  attack_formulas = [ (["out";"loa"], Or[ A["in"; "loa"]; ]); ]; 
	  defense_events  =[ ]; 
 	  defense_rigors  =[ ]; 
	  defense_profiles=[ ]; 
	}; 
]

and model = {
 instances = [   makeInstance "a"  "A" (); 
                 makeInstance "b"  "B" (); ];
 connections = [ (("b", "in"),("a", "out"));
                 (("a", "in"),("b", "out")); ];
 top_fault = ("a", F["out"; "loa"]);
 top_attack = ("a", A["out"; "loa"]);
 }

in

let cycle_adtree = model_to_adtree library model  (* <-- ASUM [ALeaf (("a", "a_atck"), 1.); ASUM [ALeaf (("a", "a_atck"), 1.)]] *)
in
let cycle_cutset = cutsets_ad cycle_adtree        (* <-- AVar ("a", "a_atck") *)

in

assert( cycle_adtree = ASUM [ALeaf (("a", "a_atck"), 1.); ASUM [ALeaf (("a", "a_atck"), 1.)]] );
assert( cycle_cutset = AVar ("a", "a_atck") );

true
;;

test_AB_a_atck ();;

(* -------- *)
(* [A]<->[B], where B has attack and A doesn't *) 
let test_AB_b_atck () =

let library = 
 [ 	{ name            =  "A"; 
	  input_flows     = ["in" ;]; 
	  output_flows    = ["out";]; 
	  faults          = ["loa";]; 
	  basic_events    = [ ]; 
	  event_info      = [ ]; 
	  fault_formulas  = [(["out";"loa"], Or[ F["in"; "loa"] ]);]; 
	  attacks         = ["loa"; ]; 
	  attack_events   = ["a_atck" ]; 
	  attack_info     = [1.]; 
	  attack_formulas = [ (["out";"loa"], Or[ A["in"; "loa"] ]); ]; 
	  defense_events  = [ ]; 
 	  defense_rigors  = [ ]; 
	  defense_profiles= [ ]; 
	}; 
	{ name            =  "B"; 
	  input_flows     = ["in" ;]; 
	  output_flows    = ["out";]; 
	  faults          = ["loa";]; 
	  basic_events    = [ ]; 
	  event_info      = [ ]; 
	  fault_formulas  = [(["out";"loa"], Or[ F["in"; "loa"] ]);]; 
	  attacks         = ["loa"; ]; 
	  attack_events   = ["b_atck" ]; 
	  attack_info     = [1. ]; 
	  attack_formulas = [ (["out";"loa"], Or[ A["b_atck"]; A["in"; "loa"]; ]); ]; 
	  defense_events  =[ ]; 
 	  defense_rigors  =[ ]; 
	  defense_profiles=[ ]; 
	}; 
]

and model = {
 instances = [   makeInstance "a"  "A" (); 
                 makeInstance "b"  "B" (); ];
 connections = [ (("b", "in"),("a", "out"));
                 (("a", "in"),("b", "out")); ];
 top_fault = ("a", F["out"; "loa"]);
 top_attack = ("a", A["out"; "loa"]);
 }

in

let cycle_adtree = model_to_adtree library model (* <-- ASUM [ALeaf (("b", "b_atck"), 1.)] *)
in
let cycle_cutset = cutsets_ad cycle_adtree       (* <-- AVar ("b", "b_atck") *)

in

assert( cycle_adtree = ASUM [ALeaf (("b", "b_atck"), 1.)]);
assert( cycle_cutset = AVar ("b", "b_atck"););

true
;;

test_AB_b_atck ();;

(* -------- *)
(* [A]<->[C]->[A]<->[B], where B has attack and A doesn't *) 
let test_ACAB () =

let library = 
 [ 	{ name            =  "A"; 
	  input_flows     = ["in" ;]; 
	  output_flows    = ["out";]; 
	  faults          = ["loa";]; 
	  basic_events    = [ ]; 
	  event_info      = [ ]; 
	  fault_formulas  = [(["out";"loa"], Or[ F["in"; "loa"] ]);]; 
	  attacks         = ["loa"; ]; 
	  attack_events   = ["a_atck" ]; 
	  attack_info     = [1.]; 
	  attack_formulas = [ (["out";"loa"], Or[ A["in"; "loa"] ]); ]; 
	  defense_events  = [ ]; 
 	  defense_rigors  = [ ]; 
	  defense_profiles= [ ]; 
	}; 
	{ name            =  "B"; 
	  input_flows     = ["in" ;]; 
	  output_flows    = ["out";]; 
	  faults          = ["loa";]; 
	  basic_events    = [ ]; 
	  event_info      = [ ]; 
	  fault_formulas  = [(["out";"loa"], Or[ F["in"; "loa"] ]);]; 
	  attacks         = ["loa"; ]; 
	  attack_events   = ["b_atck" ]; 
	  attack_info     = [1. ]; 
	  attack_formulas = [ (["out";"loa"], Or[ A["b_atck"]; A["in"; "loa"]; ]); ]; 
	  defense_events  =[ ]; 
 	  defense_rigors  =[ ]; 
	  defense_profiles=[ ]; 
	}; 
	{ name            =  "C"; 
	  input_flows     = ["in1";"in2";]; 
	  output_flows    = ["out";]; 
	  faults          = ["loa";]; 
	  basic_events    = [ ]; 
	  event_info      = [ ]; 
	  fault_formulas  = [(["out";"loa"], Or[ F["in1"; "loa"] ]);]; 
	  attacks         = ["loa"; ]; 
	  attack_events   = ["c_atck" ]; 
	  attack_info     = [1. ]; 
	  attack_formulas = [ (["out";"loa"], Or[ A["in1"; "loa"]; A["in2"; "loa"]; ]); ]; 
	  defense_events  =[ ]; 
 	  defense_rigors  =[ ]; 
	  defense_profiles=[ ]; 
	}; 
]

and model = {
 instances = [   makeInstance "a1"  "A" (); 
                 makeInstance "c1"  "C" (); 
                 makeInstance "a2"  "A" (); 
                 makeInstance "b2"  "B" (); ];
 connections = [ (("c1", "in1"),("a1", "out"));
                 (("a1", "in"),("c1", "out")); 
                 (("c1", "in2"),("a2", "out"));
                 (("a2", "in"),("b2", "out"));
                 (("b2", "in"),("a2", "out")); ];
 top_fault = ("a1", F["out"; "loa"]);
 top_attack = ("a1", A["out"; "loa"]);
 }

in

let cycle_adtree = model_to_adtree library model (* <-- ASUM [ASUM [ALeaf (("b2", "b_atck"), 1.)]] *)
in
let cycle_cutset = cutsets_ad cycle_adtree (* <-- AVar ("b2", "b_atck") *)

in

assert( cycle_adtree = ASUM [ASUM [ALeaf (("b2", "b_atck"), 1.)]] );
assert( cycle_cutset = AVar ("b2", "b_atck")  );

true

;;

test_ACAB ();;

(* -------- *)
(* [A]<->[C]<->[B], where C has attack and A & B don't, and output is C *) 
let test_ACB () =

let library = 
 [ 	{ name            =  "A"; 
	  input_flows     = ["in" ;]; 
	  output_flows    = ["out";]; 
	  faults          = ["loa";]; 
	  basic_events    = [ ]; 
	  event_info      = [ ]; 
	  fault_formulas  = [(["out";"loa"], Or[ F["in"; "loa"] ]);]; 
	  attacks         = ["loa"; ]; 
	  attack_events   = ["a_atck" ]; 
	  attack_info     = [1.]; 
	  attack_formulas = [ (["out";"loa"], Or[ A["in"; "loa"] ]); ]; 
	  defense_events  = [ ]; 
 	  defense_rigors  = [ ]; 
	  defense_profiles= [ ]; 
	}; 
	{ name            =  "B"; 
	  input_flows     = ["in" ;]; 
	  output_flows    = ["out";]; 
	  faults          = ["loa";]; 
	  basic_events    = [ ]; 
	  event_info      = [ ]; 
	  fault_formulas  = [(["out";"loa"], Or[ F["in"; "loa"] ]);]; 
	  attacks         = ["loa"; ]; 
	  attack_events   = ["b_atck" ]; 
	  attack_info     = [1. ]; 
	  attack_formulas = [ (["out";"loa"], Or[ A["in"; "loa"]; ]); ]; 
	  defense_events  =[ ]; 
 	  defense_rigors  =[ ]; 
	  defense_profiles=[ ]; 
	}; 
	{ name            =  "C"; 
	  input_flows     = ["in1";"in2";]; 
	  output_flows    = ["out";]; 
	  faults          = ["loa";]; 
	  basic_events    = [ ]; 
	  event_info      = [ ]; 
	  fault_formulas  = [(["out";"loa"], Or[ F["in1"; "loa"] ]);]; 
	  attacks         = ["loa"; ]; 
	  attack_events   = ["c_atck" ]; 
	  attack_info     = [1. ]; 
	  attack_formulas = [ (["out";"loa"], Or[ A["c_atck"]; A["in1"; "loa"]; A["in2"; "loa"]; ]); ]; 
	  defense_events  =[ ]; 
 	  defense_rigors  =[ ]; 
	  defense_profiles=[ ]; 
	}; 
]

and model = {
 instances = [   makeInstance "a"  "A" (); 
                 makeInstance "b"  "B" (); 
                 makeInstance "c"  "C" (); ];
 connections = [ (("c", "in1"),("a", "out"));
                 (("a", "in"),("c", "out")); 
                 (("c", "in2"),("b", "out"));
                 (("b", "in"),("c", "out")); ];
 top_fault = ("c", F["out"; "loa"]);
 top_attack = ("c", A["out"; "loa"]);
 }

in

let cycle_adtree = model_to_adtree library model (* <--   
 ASUM
   [ALeaf (("c", "c_atck"), 1.);
    ASUM [ALeaf (("c", "c_atck"), 1.); ASUM [ALeaf (("c", "c_atck"), 1.)]];
    ASUM [ALeaf (("c", "c_atck"), 1.); ASUM [ALeaf (("c", "c_atck"), 1.)]]] *)
in
let cycle_cutset = cutsets_ad cycle_adtree (* <-- AVar ("c", "c_atck") *)

in

assert( cycle_adtree = 
 ASUM
   [ALeaf (("c", "c_atck"), 1.);
    ASUM [ALeaf (("c", "c_atck"), 1.); ASUM [ALeaf (("c", "c_atck"), 1.)]];
    ASUM [ALeaf (("c", "c_atck"), 1.); ASUM [ALeaf (("c", "c_atck"), 1.)]]] );
assert( cycle_cutset = AVar ("c", "c_atck") );

true 
;;

test_ACB ();;


(* -------- *)
(* [A]<->[B]->[C]                                     *)
(* [A]<->[B]---^ , where C has attack and A & B don't *) 
let test_ABABC () =

let library = 
 [ 	{ name            =  "A"; 
	  input_flows     = ["in" ;]; 
	  output_flows    = ["out";]; 
	  faults          = ["loa";]; 
	  basic_events    = [ ]; 
	  event_info      = [ ]; 
	  fault_formulas  = [(["out";"loa"], Or[ F["in"; "loa"] ]);]; 
	  attacks         = ["loa"; ]; 
	  attack_events   = ["a_atck" ]; 
	  attack_info     = [1.]; 
	  attack_formulas = [ (["out";"loa"], Or[ A["in"; "loa"] ]); ]; 
	  defense_events  = [ ]; 
 	  defense_rigors  = [ ]; 
	  defense_profiles= [ ]; 
	}; 
	{ name            =  "B"; 
	  input_flows     = ["in" ;]; 
	  output_flows    = ["out";]; 
	  faults          = ["loa";]; 
	  basic_events    = [ ]; 
	  event_info      = [ ]; 
	  fault_formulas  = [(["out";"loa"], Or[ F["in"; "loa"] ]);]; 
	  attacks         = ["loa"; ]; 
	  attack_events   = ["b_atck" ]; 
	  attack_info     = [1. ]; 
	  attack_formulas = [ (["out";"loa"], Or[ A["in"; "loa"]; ]); ]; 
	  defense_events  =[ ]; 
 	  defense_rigors  =[ ]; 
	  defense_profiles=[ ]; 
	}; 
	{ name            =  "C"; 
	  input_flows     = ["in1";"in2";]; 
	  output_flows    = ["out";]; 
	  faults          = ["loa";]; 
	  basic_events    = [ ]; 
	  event_info      = [ ]; 
	  fault_formulas  = [(["out";"loa"], Or[ F["in1"; "loa"] ]);]; 
	  attacks         = ["loa"; ]; 
	  attack_events   = ["c_atck" ]; 
	  attack_info     = [1. ]; 
	  attack_formulas = [ (["out";"loa"], Or[ A["c_atck"]; A["in1"; "loa"]; A["in2"; "loa"]; ]); ]; 
	  defense_events  =[ ]; 
 	  defense_rigors  =[ ]; 
	  defense_profiles=[ ]; 
	}; 
]

and model = {
 instances = [   makeInstance "a1"  "A" (); 
                 makeInstance "b1"  "B" (); 
                 makeInstance "a2"  "A" (); 
                 makeInstance "b2"  "B" (); 
                 makeInstance "c"  "C" (); ];
 connections = [ (("b1", "in"),("a1", "out"));
                 (("a1", "in"),("b1", "out")); 
                 (("b2", "in"),("a2", "out"));
                 (("a2", "in"),("b2", "out"));
                 (("c", "in1"),("a1", "out"));
                 (("c", "in2"),("a2", "out")); ];
 top_fault = ("c", F["out"; "loa"]);
 top_attack = ("c", A["out"; "loa"]);
 }

in

let cycle_adtree = model_to_adtree library model (* <-- ASUM [ALeaf (("c", "c_atck"), 1.)] *)
in
let cycle_cutset = cutsets_ad cycle_adtree       (* <-- AVar ("c", "c_atck") *)

in

assert( cycle_adtree = ASUM [ALeaf (("c", "c_atck"), 1.)] );
assert( cycle_cutset = AVar ("c", "c_atck") );

true
;;

test_ABABC ();;
