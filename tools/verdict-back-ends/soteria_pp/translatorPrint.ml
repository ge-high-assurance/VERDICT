(* 

Copyright © 2020 General Electric Company. All Rights Reserved.

Authors: Kit Siu
Date: 2019/04/02

Updates:
 
*)

(**
 
Print functions are provided to generate a readable file containing the library and model.
The file can be kept as an artifact. This file can also be saved in .ml format to be 
read by OCaml.

   - {b print_library} : Prints a library of components.
   
   - {b print_model} : Prints a model.
   
   - {b print_filename} : Simply prints the given filename.

*)

open Core ;;
open Modeling ;;

(**/**)

(* function to print a list of strings *)
let print_string_list oc l = 
   fprintf oc "["; 
   List.iter l ~f:(fun k -> fprintf oc (" \"%s\";") k);
   fprintf oc " ]";;   

(* function to print a list of floats *)
let print_float_list oc l = 
   fprintf oc "["; 
   List.iter l ~f:(fun k-> fprintf oc " %e;" k); 
   fprintf oc " ]";;

(* function to print a list of (float, float) *)
let print_float_float_list oc l = 
   fprintf oc "["; 
   List.iter l ~f:(fun (k1,k2) -> fprintf oc "(%e, %e);" k1 k2); 
   fprintf oc " ]";;

(* function to print a list of integers *)
let print_int_list oc l = 
   fprintf oc "["; 
   List.iter l ~f:(fun k-> fprintf oc " %d;" k);
   fprintf oc " ]";;
   
(* function to print a list of (string, float) *)
let print_string_float_list oc l = 
   fprintf oc "["; 
   List.iter l ~f:(fun (s,k) -> fprintf oc "(\"%s\", %e);" s k); 
   fprintf oc " ]";;

(* function to print string list of formulas;
   see definition for type cformula in Modeling *)
let rec print_cformula oc cf =
   match cf with
   | F s -> fprintf oc "F"; (print_string_list oc s)
   | A s -> fprintf oc "A"; (print_string_list oc s)
   | D s -> fprintf oc "D"; (print_string_list oc s)
   | Not(s)  -> fprintf oc "Not( "; (print_cformula oc s); fprintf oc ")" 
   | And cfl -> fprintf oc "And[ "; List.iter cfl ~f:(fun cf -> print_cformula oc cf; fprintf oc "; "); fprintf oc "]"
   | Or cfl  -> fprintf oc "Or[ "; List.iter cfl ~f:(fun cf -> print_cformula oc cf; fprintf oc "; "); fprintf oc "]" 
   | N_of(i, cfl) -> fprintf oc "N_of( %d, [ " i; List.iter cfl ~f:(fun cf -> print_cformula oc cf; fprintf oc "; "); fprintf oc "])" ;;

(* function to print fault / attack formulas *)
let print_formula_list oc l = 
   fprintf oc "["; 
   List.iter l ~f:(fun x -> let (sl, cf) = x in
      fprintf oc "\n\t\t("; print_string_list oc sl; fprintf oc ", "; print_cformula oc cf; fprintf oc "); "; );
   fprintf oc " ]";;

(* function to print defense profile, which is slightly different from fault/attack formulas *)
let print_dformula_list oc l =
   fprintf oc "[";
   List.iter l ~f:(fun x -> let (capec, df) = x in
      fprintf oc "\n\t\t( \"%s\", " capec; print_cformula oc df; fprintf oc "); "; );
   fprintf oc " ]";;

(* function to print components;
   see definition for type component in Modeling *)
let print_component oc comp = 
     fprintf oc "\t{ name             = "; fprintf oc "\"%s\"; \n" comp.name ; 
     fprintf oc "\t  input_flows      = "; print_string_list oc comp.input_flows; fprintf oc "; \n" ;
     fprintf oc "\t  output_flows     = "; print_string_list oc comp.output_flows; fprintf oc "; \n" ;
     fprintf oc "\t  faults           = "; print_string_list oc comp.faults; fprintf oc "; \n";
     fprintf oc "\t  basic_events     = "; print_string_list oc comp.basic_events; fprintf oc "; \n";
     fprintf oc "\t  event_info       = "; print_float_float_list oc comp.event_info; fprintf oc "; \n";
     fprintf oc "\t  fault_formulas   = "; print_formula_list oc comp.fault_formulas; fprintf oc "; \n";
     fprintf oc "\t  attacks          = "; print_string_list oc comp.attacks; fprintf oc "; \n";
     fprintf oc "\t  attack_events    = "; print_string_list oc comp.attack_events; fprintf oc "; \n";
     fprintf oc "\t  attack_info      = "; print_float_list oc comp.attack_info; fprintf oc "; \n";
     fprintf oc "\t  attack_formulas  = "; print_formula_list oc comp.attack_formulas; fprintf oc "; \n";
     fprintf oc "\t  defense_events   = "; print_string_list oc comp.defense_events; fprintf oc "; \n";
     fprintf oc "\t  defense_rigors   = "; print_int_list oc comp.defense_rigors; fprintf oc "; \n";
     fprintf oc "\t  defense_profiles = "; print_dformula_list oc comp.defense_profiles; fprintf oc "; \n";
     fprintf oc "\t}; \n"
;;

(**/**)

(** A function that prints a library, which is a list of components. 
Use Out_channel.create to generate a out channel, oc. 
*)
let print_library oc lib =
     fprintf oc "let library = [\n"; 
     List.iter lib ~f:(fun x -> print_component oc x);
     fprintf oc "];;\n"
;;

(**/**)

(* function to print instances;
   see definition for type instances in Modeling *)
let print_instances oc i = 
     fprintf oc "\t\t{ i_name = "; fprintf oc "\"%s\"; " i.i_name ; 
     fprintf oc "c_name = "; fprintf oc "\"%s\"; " i.c_name ;
     fprintf oc "exposures = "; print_string_float_list oc i.exposures; fprintf oc "; " ;
     fprintf oc "lambdas = "; print_string_float_list oc i.lambdas; fprintf oc "; " ;
     fprintf oc " }; \n"
;;
(**/**)

(* A function that prints a model. 
Use Out_channel.create to generate a out channel, oc.*)
let print_model oc mdl = 
     fprintf oc "let model = \n"; 
     fprintf oc "\t{ instances = [\n"; List.iter mdl.instances ~f:(fun i -> print_instances oc i); fprintf oc "\t\t]; \n" ;
     fprintf oc "\t  connections = [\n"; List.iter mdl.connections ~f:(fun ((c1,i),(c2,o)) -> fprintf oc "\t\t((\"%s\", \"%s\"), (\"%s\", \"%s\"));\n" c1 i c2 o); fprintf oc "\t\t]; \n" ;
     fprintf oc "\t  top_fault  = "; let (s,f) = mdl.top_fault in fprintf oc "(\"%s\", " s; (print_cformula oc f); fprintf oc "); \n";
     fprintf oc "\t  top_attack = "; let (s,f) = mdl.top_attack in fprintf oc "(\"%s\", " s; (print_cformula oc f); fprintf oc "); \n";
     fprintf oc "\t};; \n"
;;


(* A function that prints the giving filename. 
Use Out_channel.create to generate a out channel, oc.*)
let print_filename oc fn = 
     fprintf oc "(* filename: %s *)\n\n" fn;
;; 

(* Prints xml formatted data for an attack-defense cut set *)
let fprintf_cutSet_attack oc comp attack = 
fprintf oc "\t       <Component name=\"%s\" capec=\"%s\"> \n" comp attack;
fprintf oc "\t       </Component> \n";;     

let fprintf_cutSet_defense oc comp profile = 
fprintf oc "\t       <Component name=\"%s\" profile=\"%s\"> \n" comp profile;
fprintf oc "\t       </Component> \n";;     

let fprintf_cutSet oc cutSetlikelihood comp_aList comp_dList = 
   let getval l tag =  List.Assoc.find_exn l tag ~equal:(=) in
   fprintf oc "\t  <Cutset likelihood=\"%s\"> \n" cutSetlikelihood;
   fprintf oc "\t    <Attack> \n";
   List.iter comp_aList ~f:(fun x -> fprintf_cutSet_attack oc (getval x "comp") (getval x "capec"));
   fprintf oc "\t    </Attack> \n";
   fprintf oc "\t    <Defense> \n";
   List.iter comp_dList ~f:(fun x -> fprintf_cutSet_defense oc (getval x "comp") (getval x "profile"));
   fprintf oc "\t    </Defense> \n";
   fprintf oc "\t  </Cutset> \n";;
 
 
(* Prints xml formatted data for an fault cut set *)
let fprintf_cutSetSafety_aux oc comp event = 
fprintf oc "\t     <Component name=\"%s\"> \n" comp;
fprintf oc "\t       <Event name=\"%s\"> \n" event;
fprintf oc "\t       </Event>\n";
fprintf oc "\t     </Component> \n";;     

let fprintf_cutSetSafety oc cutSetprobability comp_event_List = 
   let getval l tag =  List.Assoc.find_exn l tag ~equal:(=) in
   fprintf oc "\t  <Cutset probability=\"%s\"> \n" cutSetprobability;
   List.iter comp_event_List ~f:(fun x -> fprintf_cutSetSafety_aux oc (getval x "comp") (getval x "event"));
   fprintf oc "\t  </Cutset> \n";;