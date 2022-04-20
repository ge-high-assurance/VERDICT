(* 

Copyright © 2020 General Electric Company. All Rights Reserved.

Authors: Kit Siu
Date: 2019-02-12

Updates: 

*)

let myDtree_and = DPRO [ DLeaf (("a1","d1"), 7); DLeaf (("a2","d2"), 9); DLeaf (("a3","d3"), 7) ];;
let myDtree_or = DSUM [ DLeaf (("a1","d1"), 7); DLeaf (("a2","d2"), 9); DLeaf (("a3","d3"), 7) ];;
let myADtree_wDtree_and = C ( ALeaf (("a1","d1"), 1.0), myDtree_and );;
let myADtree_wDtree_or = C ( ALeaf (("a1","d1"), 1.0), myDtree_or );;


(* assuranceCalcApprox *)

let test_assuranceCalcApprox () =
  assert( assuranceCalcApprox myDtree_and = 7);  (* <- if you need all the defenses, then pick out the weakest link *)
  assert( assuranceCalcApprox myDtree_or = 9); (* <- if you only need one of the defenses, then focus on the most rigorous *)
  true
;;

test_assuranceCalcApprox ();;


(* likelihoodCalcApprox *)

let test_likelihoodCalcApprox () =
  assert( likelihoodCalcApprox myADtree_wDtree_and = 1e-7 );
  assert( likelihoodCalcApprox myADtree_wDtree_or = 1e-9 );
  true
;;

test_likelihoodCalcApprox ();;


(* eventLikelihoodsAuxDt *)

let test_eventLikelihoodsAuxDt () =
  assert( eventLikelihoodsAuxDt myDtree_and = [(("a1","d1"), 1e-07); (("a2","d2"), 1e-09); (("a3","d3"), 1e-07)] );
  assert( eventLikelihoodsAuxDt myDtree_or = [(("a1","d1"), 1e-07); (("a2","d2"), 1e-09); (("a3","d3"), 1e-07)] );
  true
;;

test_eventLikelihoodsAuxDt ();;


(* eventLikelihoodsAux *)

let test_eventLikelihoodsAux () =
  assert( eventLikelihoodsAux myADtree_wDtree_and = [(("a1","d1"), 1.); (("a1","d1"), 1e-07); (("a2","d2"), 1e-09); (("a3","d3"), 1e-07)] );
  assert( eventLikelihoodsAux myADtree_wDtree_or = [(("a1","d1"), 1.); (("a1","d1"), 1e-07); (("a2","d2"), 1e-09); (("a3","d3"), 1e-07)] );
  true
;;

test_eventLikelihoodsAux ();;


(* likelihoodCutSOP *)

let test_likelihoodCutSOP () =
   (* test the Dtree with ORed defenses -- expect lowest likelihood of success 
      because if you only need one of the defenses, then focus on the most rigorous *)
   assert( likelihoodCutSOP [(sop_ad (formulaOfADTree myADtree_wDtree_or))] (eventLikelihoodsAux myADtree_wDtree_or) = [1e-09] );

   (* test the Dtree with ANDed defenses -- expect higher likelihood of success 
      because if you need all the defenses, then pick out the weakest link *)
   assert( likelihoodCutSOP [(sop_ad (formulaOfADTree myADtree_wDtree_and))] (eventLikelihoodsAux myADtree_wDtree_and) = [1.] );
   true
;;

test_likelihoodCutSOP ();;