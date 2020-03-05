(* 

Copyright © 2019 United States Government as represented by the Administrator of the 
National Aeronautics and Space Administration.  All Rights Reserved.

Author: Pete Manolios
Date: 2017-12-15

*)

(**
   FAULT TREES

   Fault trees are represented with the {i ftree} type and monotone Boolean
   formulas are represented with the {i pexp} type.
*)

(** A fault tree is either: 
    a leaf containing a failure rate and exposure time OR
    an (n-ary) operator along with a list of fault trees. **)
type 'a ftree = 
  | Leaf of 'a * float * float 
  | SUM of 'a ftree list 
  | PRO of 'a ftree list ;; 

(** Type definitions for monotone Boolean formulas. *)
type 'a pexp = 
  | TRUE
  | FALSE
  | Var of 'a 
  | Sum of 'a pexp list
  | Pro of 'a pexp list
;;

