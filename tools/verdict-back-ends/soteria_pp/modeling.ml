(* 

Author: Pete Manolios
Date: 2017-12-15

Updates: 8/23/2018, Kit Siu, extended formula to handle attack-defense.

*)

(**
   Trees should not be constructed manually by typical users. Instead,
   they should be synthesized from libraries of components and
   models. Components are represented with the {i component} type; component
   instances are represented with the {i instance} type; component libraries are
   just a list of components; and models are represented with the {i model}
   type. Here is an example of a library. It is just a list of components.

   {[
   let nasa_handbook_lib =
   [ {name             = "System"; 
      input_flows      = ["in"];
      output_flows     = ["out"];
      faults           = ["fault"]; 
      basic_events     = ["sys_fl"];
      event_info       = [(1.e-6, 1.)];
      fault_formulas   = [(["out"; "fault"], Or [F ["sys_fl"]; F ["in"; "fault"]])];
      attacks          = ["attack"]; 
	  attack_events    = ["sys_atck"]; 
	  attack_info      = [1e-07]; 
	  attack_formulas  = [(["out"; "attack"], Or [A["sys_atck"]; A["in"; "attack"]])]; 
	  defense_events   = []; 
	  defense_profiles = []; 
	  defense_rigors   = []; 
      }];; 
   ]}

   Here is an example of a model. It is a list of component instances along with
   connection information and the identification of a top-level fault.

   {[
   let nasa_handbook_model =
   { instances   =  [ makeInstance "Orbiter" "System" ();
                      makeInstance "Main Engine" "System" (); ];
     connections =  [ (("Orbiter", "in"), ("Main Engine", "out"));
                      (("Main Engine", "in"), ("Orbiter", "out")); ];
     top_fault   =  ("Orbiter", F["out"; "fault"]);
     top_attack  =  ("Orbiter", A["out"; "attack"]); 
   } ;;
   ]}
   
*)

(** Type definition for a formula used in cformulas. *)
type 'a formula =
    F of 'a                  (* atomic fault formula *)
  | A of 'a                  (* atomic attack formula *)
  | D of 'a                  (* atomic defense formula *)
  | Not of 'a formula        (* combine attack formula w/ defense formula *)
  | And of 'a formula list       (* arbitrary-arity conjunction *)
  | Or  of 'a formula list       (* arbitrary-arity disjunction *)
  | N_of of int * ('a formula list);; (* n of m *)

(** Type definition for a cformula defining output flows. *)

type 'a cformula =
    (string list) * 'a formula ;;

(** Type definition for a dformula defining defense formula. *)

type 'a dformula =
    (string) * 'a formula ;;

(** Type definition for a component. *)
type component =
    {name             : string;
     input_flows      : string list;
     output_flows     : string list;
     faults           : string list;
     basic_events     : string list;
     event_info       : (float * float) list;
     fault_formulas   : (string list) cformula list;
     attacks          : string list;
     attack_events    : string list;
     attack_info      : float list;
     attack_formulas  : (string list) cformula list;
     defense_events   : string list;
     defense_rigors   : int list;
     defense_profiles : (string list) dformula list;
    } ;;

(** Type definition for an instance of a component. *)
type instance =
    {i_name: string;
     c_name: string;
     exposures: (string * float) list;
     lambdas: (string * float) list;
    } ;;

(** Function for making instances of components. *)
let makeInstance  ?(t = []) ?(l = []) ~i ~c () =
  {i_name = i; c_name = c; exposures = t; lambdas = l} ;;

(** Type definition of a model *)
type model =
    {instances  : instance list;
     connections: ((string * string) * (string * string)) list;
     top_fault  : (string * (string list) formula);
     top_attack : (string * (string list) formula);
    };;

