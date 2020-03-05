(* 

Copyright © 2020 General Electric Company. All Rights Reserved.

Author: Kit Siu
Date: 2018-05-09

*)

(**
   ATTACK-DEFENSE TREES

   Attack-defense trees are represented with the {i adtree} type and Boolean
   formulas are represented with the {i adexp} type.
*)

(** A defense tree is either: 
    a leaf containing a defense and design rigor OR
    an (n-ary) operator along with a list of defense trees. **)
type 'a dtree = 
  | DLeaf of 'a * int
  | DSUM of 'a dtree list 
  | DPRO of 'a dtree list ;; 

(** An attack-defense tree is either: 
    a leaf containing an attack and likelihood of success OR
    an (n-ary) operator along with a list of attack trees
    a combination operator along with a tuple of attack trees and defense trees. **)
type 'a adtree = 
  | ALeaf of 'a * float
  | ASUM of 'a adtree list 
  | APRO of 'a adtree list 
  | C of 'a adtree * 'a dtree ;; 

(** Type definitions for Boolean formulas. *)
type 'a adexp = 
  | ATRUE
  | AFALSE
  | AVar of 'a 
  | ANot of 'a adexp
  | ASum of 'a adexp list
  | APro of 'a adexp list
  | DSum of 'a adexp list
  | DPro of 'a adexp list
;;

